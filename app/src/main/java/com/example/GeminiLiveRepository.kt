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

enum class GeminiLiveStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class GeminiToolCall(
    val name: String,
    val callId: String,
    val args: JSONObject
)

class GeminiLiveRepository {
    private val tag = "GeminiLiveRepository"

    private val _status = MutableStateFlow(GeminiLiveStatus.DISCONNECTED)
    val status = _status.asStateFlow()

    private val _assistantTextStream = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val assistantTextStream = _assistantTextStream.asSharedFlow()

    private val _audioStream = MutableSharedFlow<ByteArray>(extraBufferCapacity = 256)
    val audioStream = _audioStream.asSharedFlow()

    private val _toolCallStream = MutableSharedFlow<GeminiToolCall>(extraBufferCapacity = 16)
    val toolCallStream = _toolCallStream.asSharedFlow()

    private val _interruptedStream = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val interruptedStream = _interruptedStream.asSharedFlow()

    private val _latencyMs = MutableStateFlow(0)
    val latencyMs = _latencyMs.asStateFlow()

    private var lastSendTime = 0L

    private var okHttpClient: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun connect(apiKey: String) {
        if (_status.value == GeminiLiveStatus.CONNECTED || _status.value == GeminiLiveStatus.CONNECTING) return
        _status.value = GeminiLiveStatus.CONNECTING

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
                Log.d(tag, "WebSocket established successfully")
                _status.value = GeminiLiveStatus.CONNECTED
                sendSetupFrame(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseServerMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closing: $reason")
                disconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "WebSocket connection failed", t)
                disconnect(isError = true)
            }
        })
    }

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
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "You are MYRA, a super-fast, low-latency, real-time voice-to-voice Android assistant. Speak in short, conversational Hindi or Hinglish. You have access to the MYRA Device Automation tools. Use them whenever the user asks you to perform actions on their phone. Keep your voice replies quick and helpful.")
                            })
                        })
                    })
                    put("tools", JSONArray().apply {
                        put(JSONObject().apply {
                            put("functionDeclarations", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("name", "openApp")
                                    put("description", "Opens an app on the user's device. Supports: youtube, whatsapp, chrome, settings, camera.")
                                    put("parameters", JSONObject().apply {
                                        put("type", "OBJECT")
                                        put("properties", JSONObject().apply {
                                            put("name", JSONObject().apply {
                                                put("type", "STRING")
                                                put("description", "The name of the app to open.")
                                            })
                                        })
                                        put("required", JSONArray().apply { put("name") })
                                    })
                                })
                                put(JSONObject().apply {
                                    put("name", "clickText")
                                    put("description", "Clicks/Taps a specific text element visible on the screen using OCR/Accessibility.")
                                    put("parameters", JSONObject().apply {
                                        put("type", "OBJECT")
                                        put("properties", JSONObject().apply {
                                            put("text", JSONObject().apply {
                                                put("type", "STRING")
                                                put("description", "The text element to click.")
                                            })
                                        })
                                        put("required", JSONArray().apply { put("text") })
                                    })
                                })
                                put(JSONObject().apply {
                                    put("name", "typeText")
                                    put("description", "Types specific text into the active text/input field.")
                                    put("parameters", JSONObject().apply {
                                        put("type", "OBJECT")
                                        put("properties", JSONObject().apply {
                                            put("text", JSONObject().apply {
                                                put("type", "STRING")
                                                put("description", "The text content to type.")
                                            })
                                        })
                                        put("required", JSONArray().apply { put("text") })
                                    })
                                })
                                put(JSONObject().apply {
                                    put("name", "performSwipe")
                                    put("description", "Performs a swipe gesture on the screen.")
                                    put("parameters", JSONObject().apply {
                                        put("type", "OBJECT")
                                        put("properties", JSONObject().apply {
                                            put("direction", JSONObject().apply {
                                                put("type", "STRING")
                                                put("description", "The direction to swipe, either 'up' or 'down'.")
                                            })
                                        })
                                        put("required", JSONArray().apply { put("direction") })
                                    })
                                })
                                put(JSONObject().apply {
                                    put("name", "goBack")
                                    put("description", "Goes back to the previous screen (hardware back).")
                                    put("parameters", JSONObject().apply {
                                        put("type", "OBJECT")
                                        put("properties", JSONObject())
                                    })
                                })
                                put(JSONObject().apply {
                                    put("name", "controlVolume")
                                    put("description", "Adjusts the device volume.")
                                    put("parameters", JSONObject().apply {
                                        put("type", "OBJECT")
                                        put("properties", JSONObject().apply {
                                            put("action", JSONObject().apply {
                                                put("type", "STRING")
                                                put("description", "Direction to change volume: 'up' or 'down'.")
                                            })
                                        })
                                        put("required", JSONArray().apply { put("action") })
                                    })
                                })
                                put(JSONObject().apply {
                                    put("name", "toggleFlashlight")
                                    put("description", "Toggles the device flashlight on or off.")
                                    put("parameters", JSONObject().apply {
                                        put("type", "OBJECT")
                                        put("properties", JSONObject())
                                    })
                                })
                                put(JSONObject().apply {
                                    put("name", "getBatteryLevel")
                                    put("description", "Retrieves the current battery level percentage.")
                                    put("parameters", JSONObject().apply {
                                        put("type", "OBJECT")
                                        put("properties", JSONObject())
                                    })
                                })
                                put(JSONObject().apply {
                                    put("name", "makeCall")
                                    put("description", "Makes a dial phone call to a matching contact name.")
                                    put("parameters", JSONObject().apply {
                                        put("type", "OBJECT")
                                        put("properties", JSONObject().apply {
                                            put("contactName", JSONObject().apply {
                                                put("type", "STRING")
                                                put("description", "The name of the contact to call.")
                                            })
                                        })
                                        put("required", JSONArray().apply { put("contactName") })
                                    })
                                })
                            })
                        })
                    })
                })
            }
            ws.send(setupObj.toString())
        } catch (e: Exception) {
            Log.e(tag, "Failed to compile setup frame", e)
        }
    }

    private fun parseServerMessage(text: String) {
        if (lastSendTime > 0L) {
            val rtt = (System.currentTimeMillis() - lastSendTime).toInt()
            if (rtt in 10..2000) {
                _latencyMs.value = if (_latencyMs.value == 0) rtt else (_latencyMs.value * 0.7 + rtt * 0.3).toInt()
            }
        }
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
                                scope.launch { _assistantTextStream.emit(transcript) }
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
            if (json.has("toolCall")) {
                val toolCall = json.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null) {
                    for (i in 0 until functionCalls.length()) {
                        val call = functionCalls.getJSONObject(i)
                        val name = call.getString("name")
                        val id = call.getString("id")
                        val args = call.optJSONObject("args") ?: JSONObject()
                        scope.launch {
                            _toolCallStream.emit(GeminiToolCall(name, id, args))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse inbound frame", e)
        }
    }

    fun sendAudioChunk(pcmData: ByteArray, length: Int) {
        lastSendTime = System.currentTimeMillis()
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
    }

    fun sendToolResponse(name: String, id: String, success: Boolean, message: String) {
        try {
            val responseObj = JSONObject().apply {
                put("toolResponse", JSONObject().apply {
                    put("functionResponses", JSONArray().apply {
                        put(JSONObject().apply {
                            put("name", name)
                            put("id", id)
                            put("response", JSONObject().apply {
                                put("output", JSONObject().apply {
                                    put("status", if (success) "SUCCESS" else "FAILED")
                                    put("message", message)
                                })
                            })
                        })
                    })
                })
            }
            webSocket?.send(responseObj.toString())
        } catch (e: Exception) {
            Log.e(tag, "Error sending tool response", e)
        }
    }

    fun disconnect(isError: Boolean = false) {
        _status.value = if (isError) GeminiLiveStatus.ERROR else GeminiLiveStatus.DISCONNECTED
        _latencyMs.value = 0
        lastSendTime = 0L
        try {
            webSocket?.close(1000, "Disconnected")
        } catch (e: Exception) {
            // ignore
        }
        webSocket = null
        okHttpClient = null
    }
}
