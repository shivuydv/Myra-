package com.example

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.testTag

@Composable
fun MyraLiveScreen(
    myraCore: MyraCore,
    geminiClient: GeminiLiveClient,
    permissionManager: MyraPermissionManager,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionManager = remember { MyraLiveSessionManager(context, geminiClient) }
    val currentState by sessionManager.liveState.collectAsStateWithLifecycle()

    val connectionStatus by geminiClient.status.collectAsStateWithLifecycle()
    val assistantTranscript by geminiClient.assistantTranscript.collectAsStateWithLifecycle()
    val lastActionLog by geminiClient.lastActionLog.collectAsStateWithLifecycle()
    val micLevel by geminiClient.micLevel.collectAsStateWithLifecycle()
    val speakerLevel by geminiClient.speakerLevel.collectAsStateWithLifecycle()
    val latencyMs by geminiClient.latencyMs.collectAsStateWithLifecycle()

    var permissionStatus by remember { mutableStateOf(permissionManager.getStatus()) }

    // Periodically check system permission statuses
    LaunchedEffect(Unit) {
        while (true) {
            permissionStatus = permissionManager.getStatus()
            kotlinx.coroutines.delay(2000)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sessionManager.stopLiveSession()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Header Row
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Face,
                        contentDescription = "MYRA Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "MYRA Live",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Open Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Zero-Delay Gemini Voice Assistant",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pulse Status indicators
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                StatusBadge(status = connectionStatus)
                if (connectionStatus == GeminiLiveStatus.CONNECTED) {
                    Spacer(modifier = Modifier.width(8.dp))
                    PulsingLatencyIndicator(latencyMs = latencyMs, status = connectionStatus)
                }
            }
        }

        // Beautiful Real-time Voice Amplitude Multi-Bar Meter
        RealtimeVoiceMeter(
            micLevel = micLevel,
            speakerLevel = speakerLevel,
            status = connectionStatus,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )

        // Session Controller Button (Start/Stop Session)
        val buttonColor = when (currentState) {
            LiveState.LISTENING -> Color(0xFF4CAF50)
            LiveState.SPEAKING -> Color(0xFF00BCD4)
            LiveState.CONNECTING -> Color(0xFFFFEB3B)
            LiveState.ERROR -> Color(0xFFF44336)
            LiveState.IDLE -> MaterialTheme.colorScheme.primary
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // State Display Label
            Text(
                text = "Status: ${currentState.name}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = when (currentState) {
                    LiveState.LISTENING -> Color(0xFF4CAF50)
                    LiveState.SPEAKING -> Color(0xFF00BCD4)
                    LiveState.CONNECTING -> Color(0xFFFFC107)
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                },
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Button(
                onClick = {
                    if (currentState == LiveState.IDLE || currentState == LiveState.ERROR) {
                        if (!permissionManager.hasMicrophone()) {
                            permissionManager.requestMicrophone()
                        } else {
                            val apiKey = BuildConfig.GEMINI_API_KEY
                            sessionManager.startLiveSession(apiKey)
                        }
                    } else {
                        sessionManager.stopLiveSession()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = if (currentState == LiveState.CONNECTING) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(6.dp, RoundedCornerShape(14.dp))
                    .testTag("start_stop_session_button")
            ) {
                Text(
                    text = if (currentState == LiveState.IDLE) "Start Live Session" else "Stop Session",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Live Action Logs & Gemini Response Screen
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Tool Logs
            if (lastActionLog.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Tool Executed",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = lastActionLog,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Real-time voice transcript bubble
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    item {
                        Text(
                            text = if (assistantTranscript.isNotEmpty()) assistantTranscript else "आपकी आवाज़ सुनने के लिए तैयार हूँ...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Bottom suggested commands list and permissions setup
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Suggestion Card
            Text(
                text = "Suggested Prompts (बोलकर देखें):",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(text = "Flashlight जलाओ")
                SuggestionChip(text = "YouTube खोलो")
                SuggestionChip(text = "Battery level बताओ")
            }

            // Permission Status & Action Box
            ExpandablePermissionPanel(
                status = permissionStatus,
                onMicReq = { permissionManager.requestMicrophone() },
                onContactsReq = { permissionManager.requestContacts() },
                onAccessibilityReq = { permissionManager.openAccessibilitySettings() }
            )
        }
    }
}

@Composable
fun StatusBadge(status: GeminiLiveStatus) {
    val (label, color) = when (status) {
        GeminiLiveStatus.CONNECTED -> "CONNECTED (Live)" to Color(0xFF4CAF50)
        GeminiLiveStatus.CONNECTING -> "CONNECTING..." to Color(0xFFFF9800)
        GeminiLiveStatus.ERROR -> "ERROR" to Color(0xFFF44336)
        GeminiLiveStatus.DISCONNECTED -> "DISCONNECTED" to Color(0xFF9E9E9E)
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = CircleShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = Modifier.padding(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
fun RowScope.SuggestionChip(text: String) {
    val tag = text.lowercase().replace(Regex("[^a-z0-9]"), "_")
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .weight(1f)
            .testTag("suggestion_chip_$tag")
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
        )
    }
}

@Composable
fun ExpandablePermissionPanel(
    status: MyraPermissionStatus,
    onMicReq: () -> Unit,
    onContactsReq: () -> Unit,
    onAccessibilityReq: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "System Integration Requirements",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionRow(
                label = "Microphone Permission",
                isGranted = status.microphone,
                onGrant = onMicReq
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionRow(
                label = "Contacts Permission (Calling)",
                isGranted = status.contacts,
                onGrant = onContactsReq
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionRow(
                label = "Accessibility Control (Automation)",
                isGranted = status.accessibility,
                onGrant = onAccessibilityReq
            )
        }
    }
}

@Composable
fun PermissionRow(
    label: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.Info,
                contentDescription = if (isGranted) "Granted" else "Required",
                tint = if (isGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (!isGranted) {
            val tag = label.lowercase()
                .replace("(", "")
                .replace(")", "")
                .trim()
                .replace(Regex("\\s+"), "_")
            Button(
                onClick = onGrant,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier
                    .height(28.dp)
                    .testTag("enable_$tag"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                text = "Enabled",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun PulsingLatencyIndicator(
    latencyMs: Int,
    status: GeminiLiveStatus,
    modifier: Modifier = Modifier
) {
    if (status != GeminiLiveStatus.CONNECTED) return

    val infiniteTransition = rememberInfiniteTransition(label = "latencyPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Latency categories based on realtime WebSocket benchmarks
    val (healthLabel, healthColor) = when {
        latencyMs <= 180 -> "Excellent" to Color(0xFF4CAF50)
        latencyMs <= 380 -> "Good" to Color(0xFFFF9800)
        else -> "Poor" to Color(0xFFF44336)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        // Pulsing glowing dot indicator
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(healthColor)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .border(BorderStroke(1.5.dp, healthColor.copy(alpha = pulseAlpha)), CircleShape)
                    .background(Color.Transparent)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$latencyMs ms ($healthLabel)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = healthColor
        )
    }
}
