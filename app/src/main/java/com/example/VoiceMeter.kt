package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

@Composable
fun RealtimeVoiceMeter(
    micLevel: Float,      // 0f to 1f
    speakerLevel: Float,  // 0f to 1f
    status: GeminiLiveStatus,
    modifier: Modifier = Modifier
) {
    // Calculate dB value from RMS levels (0f to 1f)
    // dB = 20 * log10(level)
    val micDb = remember(micLevel) {
        if (micLevel <= 0.001f) -60f else (20f * kotlin.math.log10(micLevel)).coerceIn(-60f, 0f)
    }
    val speakerDb = remember(speakerLevel) {
        if (speakerLevel <= 0.001f) -60f else (20f * kotlin.math.log10(speakerLevel)).coerceIn(-60f, 0f)
    }

    val activeLevel = if (speakerLevel > micLevel) speakerLevel else micLevel
    val activeDb = if (speakerLevel > micLevel) speakerDb else micDb
    val isAssistantSpeaking = speakerLevel > micLevel && speakerLevel > 0.01f
    val isUserSpeaking = micLevel >= speakerLevel && micLevel > 0.01f

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val waveColor = when {
        status != GeminiLiveStatus.CONNECTED -> MaterialTheme.colorScheme.outlineVariant
        isAssistantSpeaking -> secondaryColor
        isUserSpeaking -> primaryColor
        else -> MaterialTheme.colorScheme.outline
    }

    val infiniteTransition = rememberInfiniteTransition(label = "voiceMeter")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Multi-Bar Waveform Amplitude meter
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val currentPhase = if (status == GeminiLiveStatus.CONNECTED) phaseShift else 0f
                val barCount = 15
                val barSpacing = 10.dp.toPx()
                val barWidth = 6.dp.toPx()
                val totalWidth = (barCount * barWidth) + ((barCount - 1) * barSpacing)
                val startX = (size.width - totalWidth) / 2f
                val centerY = size.height / 2f

                for (i in 0 until barCount) {
                    // Smooth Gaussian weight so center bars jump higher
                    val centerDist = abs(i - (barCount / 2))
                    val weight = max(0.15f, 1f - (centerDist.toFloat() / (barCount / 2f)))

                    // Add dynamic sine shifting using currentPhase
                    val sinWave = sin(currentPhase + (i * 0.4f)) * 0.15f
                    val animatedAmp = if (status == GeminiLiveStatus.CONNECTED) {
                        (activeLevel * weight) + abs(sinWave) * 0.05f
                    } else {
                        // Gentle static wave for standby mode (consumes 0 recompositions)
                        abs(sin(0f + (i * 0.3f))) * 0.05f
                    }

                    val maxBarHeight = size.height * 0.85f
                    val barHeight = (animatedAmp * maxBarHeight).coerceIn(8.dp.toPx(), size.height)

                    val x = startX + i * (barWidth + barSpacing)
                    val y = centerY - (barHeight / 2f)

                    val gradientBrush = Brush.verticalGradient(
                        colors = listOf(
                            waveColor,
                            waveColor.copy(alpha = 0.5f),
                            waveColor.copy(alpha = 0.15f)
                        ),
                        startY = y,
                        endY = y + barHeight
                    )

                    drawRoundRect(
                        brush = gradientBrush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // HUD Dashboard label for live decibels
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val indicatorIcon = when {
                status != GeminiLiveStatus.CONNECTED -> Icons.Filled.Adjust
                isAssistantSpeaking -> Icons.Filled.VolumeUp
                isUserSpeaking -> Icons.Filled.Mic
                else -> Icons.Filled.Adjust
            }

            Icon(
                imageVector = indicatorIcon,
                contentDescription = null,
                tint = waveColor,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = when {
                    status != GeminiLiveStatus.CONNECTED -> "STANDBY"
                    isUserSpeaking -> "USER SPEAKING"
                    isAssistantSpeaking -> "ASSISTANT TALKING"
                    else -> "LISTENING"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = if (status == GeminiLiveStatus.CONNECTED) "${activeDb.toInt()} dB" else "--- dB",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = waveColor
            )
        }
    }
}
