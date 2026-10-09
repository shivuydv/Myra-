package com.example

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

class GeminiMultimodalClient {
    private val tag = "GeminiMultimodalClient"

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status = _status.asStateFlow()

    private val _textStream = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val textStream = _textStream.asSharedFlow()

    private val _audioStream = MutableSharedFlow<ByteArray>(extraBufferCapacity = 256)
    val audioStream = _audioStream.asSharedFlow()

    private val _interruptedStream = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val interruptedStream = _interruptedStream.asSharedFlow()

    private var okHttpClient: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Establishes a real-time WebSocket connection to the Gemini Multimodal Live API
     * using the API key loaded fromBuildConfig (which is sourced from the environment variables).
     */
    fun connect() {
        if (_status.value == ConnectionStatus.CONNECTED || _status.value == ConnectionStatus.CONNECTING) {
            Log.d(tag, "Client is already connecting or connected.")
            return
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(tag, "API Key is missing or invalid in BuildConfig.")
            _status.value = ConnectionStatus.ERROR
            return
        }

        _status.value = ConnectionStatus.CONNECTING

        okHttpClient = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(0, TimeUnit.MILLISECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .build()

        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = okHttpClient?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "WebSocket connection successfully opened.")
                _status.value = ConnectionStatus.CONNECTED
                sendSetupFrame(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseServerMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket connection closing: $reason")
                disconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "WebSocket connection failure", t)
                disconnect(isError = true)
            }
        })
    }

    /**
     * Sends the initial setup frame to the Gemini Live session configuring the model,
     * voice modalities, and options.
     */
    private fun sendSetupFrame(ws: WebSocket) {
        try {
            val setupObj = JSONObject().apply {
                put("setup", JSONObject().apply {
                    put("model", "models/gemini-2.0-flash-exp")
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().apply {
                            put("AUDIO")
                        })
                    })
                })
            }
            ws.send(setupObj.toString())
            Log.d(tag, "Setup frame sent successfully.")
        } catch (e: Exception) {
            Log.e(tag, "Failed to compile setup frame", e)
        }
    }

    /**
     * Parse incoming messages from the Gemini Live API WebSocket stream.
     */
    private fun parseServerMessage(text: String) {
        try {
            val json = JSONObject(text)
            if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")
                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("text")) {
                                val transcript = part.getString("text")
                                scope.launch { _textStream.emit(transcript) }
                            }
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val dataStr = inlineData.getString("data")
                                val audioBytes = Base64.decode(dataStr, Base64.DEFAULT)
                                scope.launch { _audioStream.emit(audioBytes) }
                            }
                        }
                    }
                }
                if (serverContent.has("interrupted")) {
                    scope.launch { _interruptedStream.emit(Unit) }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing server message", e)
        }
    }

    /**
     * Sends a raw PCM audio chunk to the Gemini Multimodal Live API.
     */
    fun sendAudioChunk(pcmData: ByteArray, length: Int) {
        if (_status.value != ConnectionStatus.CONNECTED) {
            Log.w(tag, "Attempted to send audio chunk while disconnected.")
            return
        }
        try {
            val base64 = Base64.encodeToString(pcmData, 0, length, Base64.NO_WRAP)
            val realtimeInput = JSONObject().apply {
                put("realtimeInput", JSONObject().apply {
                    put("mediaChunks", JSONArray().apply {
                        put(JSONObject().apply {
                            put("mimeType", "audio/pcm;rate=16000")
                            put("data", base64)
                        })
                    })
                })
            }
            webSocket?.send(realtimeInput.toString())
        } catch (e: Exception) {
            Log.e(tag, "Error sending media chunk", e)
        }
    }

    /**
     * Disconnects and releases the WebSocket connection.
     */
    fun disconnect(isError: Boolean = false) {
        _status.value = if (isError) ConnectionStatus.ERROR else ConnectionStatus.DISCONNECTED
        try {
            webSocket?.close(1000, "Disconnected")
        } catch (e: Exception) {
            // ignore
        }
        webSocket = null
        okHttpClient = null
        Log.d(tag, "Client disconnected successfully.")
    }
}
