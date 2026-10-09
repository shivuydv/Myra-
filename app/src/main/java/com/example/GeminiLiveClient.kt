package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class GeminiLiveClient(
    private val context: Context,
    private val myraCore: MyraCore
) {
    private val tag = "GeminiLiveClient"

    private val repository = GeminiMultimodalClient()
    private val scope = CoroutineScope(Dispatchers.Main)

    val status = repository.status
    val latencyMs = repository.latencyMs

    private val _assistantTranscript = MutableStateFlow("")
    val assistantTranscript = _assistantTranscript.asStateFlow()

    private val _micLevel = MutableStateFlow(0f)
    val micLevel = _micLevel.asStateFlow()

    private val _speakerLevel = MutableStateFlow(0f)
    val speakerLevel = _speakerLevel.asStateFlow()

    private val _lastActionLog = MutableStateFlow("")
    val lastActionLog = _lastActionLog.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private var isRecording = false
    private var isPlaying = false

    private var recordingJob: Job? = null
    private var collectionJob: Job? = null

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        try {
            val sampleRate = 24000 // Gemini output rate is 24kHz
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ) * 4

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isPlaying = true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize AudioTrack", e)
        }
    }

    fun connect() {
        stop() // Clean up any active session, recording, or collection jobs first

        _assistantTranscript.value = "Connecting with SIYA Live..."
        _lastActionLog.value = ""

        val secureStore = MyraSecureStore(context)
        val savedKey = secureStore.get("CUSTOM_GEMINI_API_KEY")
        val apiKey = if (!savedKey.isNullOrBlank()) savedKey else BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            _assistantTranscript.value = "API Key missing. Please set GEMINI_API_KEY in the Settings or Secrets panel."
            return
        }

        // Connect repository to Gemini Multimodal Live API
        repository.connect(apiKey)

        // Launch jobs to listen to the Repository flows
        collectionJob = scope.launch {
            // Observe assistant's speech transcript streaming
            launch {
                repository.assistantTextStream.collect { text ->
                    _assistantTranscript.value = text
                }
            }

            // Observe connection status to show friendly messages
            launch {
                repository.status.collect { status ->
                    when (status) {
                        GeminiLiveStatus.ERROR -> {
                            _assistantTranscript.value = "API connection failed. Please check your internet, verify your Gemini API Key in Settings, and try starting again."
                        }
                        GeminiLiveStatus.DISCONNECTED -> {
                            _assistantTranscript.value = "Session ended. Tap 'Start Live Session' to connect with SIYA."
                        }
                        else -> { /* no-op */ }
                    }
                }
            }

            // Observe raw audio stream output from Gemini and write it to speaker AudioTrack
            launch {
                repository.audioStream.collect { bytes ->
                    audioTrack?.write(bytes, 0, bytes.size)
                    calculatePlaybackVolume(bytes)
                }
            }

            // Observe incoming tool calls from Gemini and orchestrate MYRA capabilities
            launch {
                repository.toolCallStream.collect { toolCall ->
                    handleToolExecution(toolCall.name, toolCall.callId, toolCall.args)
                }
            }

            // Observe user barge-in/interruption to immediately silence current playback
            launch {
                repository.interruptedStream.collect {
                    Log.d(tag, "User interruption detected. Silencing speaker...")
                    audioTrack?.flush()
                    _speakerLevel.value = 0f
                }
            }
        }

        // Start microphone capture and stream it through repository
        startAudioCapture()
    }

    @SuppressLint("MissingPermission")
    private fun startAudioCapture() {
        try {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufSize * 2
            )

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ByteArray(1024)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        calculateMicVolume(buffer, read)
                        // Stream mic chunk directly to repository
                        repository.sendAudioChunk(buffer, read)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error starting microphone capture", e)
        }
    }

    private fun handleToolExecution(name: String, id: String, args: JSONObject) {
        scope.launch {
            _lastActionLog.value = "Executing tool call: $name..."
            try {
                val result = when (name) {
                    "openApp" -> {
                        val appName = args.getString("name")
                        myraCore.capabilityManager.execute(MyraAction("OPEN_APP", target = appName))
                    }
                    "clickText" -> {
                        val elementText = args.getString("text")
                        myraCore.capabilityManager.execute(MyraAction("CLICK", target = elementText))
                    }
                    "typeText" -> {
                        val content = args.getString("text")
                        myraCore.capabilityManager.execute(MyraAction("TYPE", value = content))
                    }
                    "performSwipe" -> {
                        val direction = args.getString("direction")
                        myraCore.capabilityManager.execute(MyraAction("SWIPE", value = direction))
                    }
                    "goBack" -> {
                        myraCore.capabilityManager.execute(MyraAction("BACK"))
                    }
                    "controlVolume" -> {
                        val action = args.getString("action")
                        val actType = if (action.lowercase() == "up") "VOLUME_UP" else "VOLUME_DOWN"
                        myraCore.capabilityManager.execute(MyraAction(actType))
                    }
                    "toggleFlashlight" -> {
                        myraCore.capabilityManager.execute(MyraAction("FLASHLIGHT"))
                    }
                    "getBatteryLevel" -> {
                        myraCore.capabilityManager.execute(MyraAction("BATTERY"))
                    }
                    "makeCall" -> {
                        val contactName = args.getString("contactName")
                        myraCore.capabilityManager.execute(MyraAction("CALL", target = contactName))
                    }
                    else -> MyraActionResult(MyraActionStatus.FAILED, "Unknown function call: $name")
                }

                _lastActionLog.value = "Tool result: ${result.message}"
                // Stream tool outcome back to Gemini
                repository.sendToolResponse(name, id, result.success, result.message)
            } catch (e: Exception) {
                Log.e(tag, "Execution of $name tool failed", e)
                _lastActionLog.value = "Tool error: ${e.localizedMessage}"
                repository.sendToolResponse(name, id, false, e.localizedMessage ?: "Execution error")
            }
        }
    }

    private fun calculateMicVolume(buffer: ByteArray, read: Int) {
        var sum = 0.0
        for (i in 0 until read step 2) {
            if (i + 1 < read) {
                val sample = (buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xff)
                sum += sample * sample
            }
        }
        val rms = Math.sqrt(sum / (read / 2))
        val level = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
        _micLevel.value = level * 1.5f
    }

    private fun calculatePlaybackVolume(buffer: ByteArray) {
        val size = buffer.size
        var sum = 0.0
        for (i in 0 until size step 2) {
            if (i + 1 < size) {
                val sample = (buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xff)
                sum += sample * sample
            }
        }
        val rms = Math.sqrt(sum / (size / 2))
        val level = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
        _speakerLevel.value = level * 1.5f
    }

    fun stop() {
        repository.disconnect()

        _micLevel.value = 0f
        _speakerLevel.value = 0f

        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

        collectionJob?.cancel()
        collectionJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // ignore
        }
        audioRecord = null
    }
}
