package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object MyraCommandBus {
    private val _commands = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val commands = _commands.asSharedFlow()

    fun send(command: String) {
        if (command.isNotBlank()) {
            _commands.tryEmit(command.trim())
        }
    }
}

object MyraResultBus {
    private val _results = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val results = _results.asSharedFlow()

    fun send(result: String) {
        if (result.isNotBlank()) {
            _results.tryEmit(result.trim())
        }
    }
}

object MyraConfirmationBus {
    private val _responses = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val responses = _responses.asSharedFlow()

    fun send(response: String) {
        if (response.isNotBlank()) {
            _responses.tryEmit(response.trim())
        }
    }
}

enum class MyraVoiceState {
    IDLE, LISTENING, PROCESSING, SPEAKING, ERROR
}

class MyraVoiceCapture(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    fun start(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition available नहीं है.")
            return
        }

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}

                override fun onError(error: Int) {
                    onError("Voice recognition error: $error")
                }

                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()

                    if (!text.isNullOrBlank()) {
                        onResult(text)
                    } else {
                        onError("कुछ सुनाई नहीं दिया.")
                    }
                }
            }
        )

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        }

        recognizer?.startListening(intent)
    }

    fun stop() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}

class MyraWakeService : Service() {

    companion object {
        const val CHANNEL_ID = "myra_wake"
        const val NOTIFICATION_ID = 1001

        @Volatile
        private var running = false

        fun isRunning() = running
    }

    private lateinit var core: MyraCore
    private lateinit var voiceCapture: MyraVoiceCapture

    override fun onCreate() {
        super.onCreate()

        running = true
        core = MyraCore(applicationContext)
        voiceCapture = MyraVoiceCapture(this)

        createChannel()
        startForeground(
            NOTIFICATION_ID,
            notification()
        )
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "MYRA Assistant",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    private fun notification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MYRA")
            .setContentText("Assistant active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun processBackground(command: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = core.process(command)
            MyraResultBus.send(result)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        voiceCapture.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

object MyraServiceManager {

    fun start(context: Context) {
        if (MyraWakeService.isRunning()) return

        val intent = Intent(context, MyraWakeService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, MyraWakeService::class.java))
    }
}
