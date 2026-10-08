package com.example

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LiveState {
    IDLE, CONNECTING, LISTENING, SPEAKING, ERROR
}

class MyraLiveSessionManager(
    private val context: Context,
    private val geminiClient: GeminiLiveClient
) {
    private val _liveState = MutableStateFlow(LiveState.IDLE)
    val liveState = _liveState.asStateFlow()

    private var trackingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun startLiveSession(apiKey: String) {
        // Connect through GeminiLiveClient which handles raw capturing and playback
        geminiClient.connect()

        // Track and map statuses reactively
        trackingJob?.cancel()
        trackingJob = scope.launch {
            launch {
                geminiClient.status.collect { status ->
                    _liveState.value = when (status) {
                        GeminiLiveStatus.DISCONNECTED -> LiveState.IDLE
                        GeminiLiveStatus.CONNECTING -> LiveState.CONNECTING
                        GeminiLiveStatus.ERROR -> LiveState.ERROR
                        GeminiLiveStatus.CONNECTED -> {
                            if (geminiClient.speakerLevel.value > 0.05f) {
                                LiveState.SPEAKING
                            } else {
                                LiveState.LISTENING
                            }
                        }
                    }
                }
            }

            // Real-time tracking to switch between speaking and listening
            launch {
                geminiClient.speakerLevel.collect { speakerLevel ->
                    if (geminiClient.status.value == GeminiLiveStatus.CONNECTED) {
                        _liveState.value = if (speakerLevel > 0.05f) {
                            LiveState.SPEAKING
                        } else {
                            LiveState.LISTENING
                        }
                    }
                }
            }
        }
    }

    fun stopLiveSession() {
        trackingJob?.cancel()
        trackingJob = null
        geminiClient.stop()
        _liveState.value = LiveState.IDLE
    }
}
