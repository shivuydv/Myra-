package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    secureStore: MyraSecureStore,
    permissionManager: MyraPermissionManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept hardware back button to return safely
    BackHandler {
        onBack()
    }

    // Load existing securely saved API keys
    var geminiKeyInput by remember {
        mutableStateOf(secureStore.get("CUSTOM_GEMINI_API_KEY") ?: "")
    }
    var elevenLabsKeyInput by remember {
        mutableStateOf(secureStore.get("CUSTOM_ELEVENLABS_API_KEY") ?: "")
    }

    var isGeminiVisible by remember { mutableStateOf(false) }
    var isElevenLabsVisible by remember { mutableStateOf(false) }

    var permissionStatus by remember { mutableStateOf(permissionManager.getStatus()) }

    // Periodically update permission check statuses
    LaunchedEffect(Unit) {
        while (true) {
            permissionStatus = permissionManager.getStatus()
            delay(1500)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Settings Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Settings & Permissions",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Section: Gemini API Key
        Text(
            text = "Gemini API Key",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = geminiKeyInput,
            onValueChange = { geminiKeyInput = it },
            placeholder = { Text("Gemini API Key yahan dalein") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            visualTransformation = if (isGeminiVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val icon = if (isGeminiVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                IconButton(onClick = { isGeminiVisible = !isGeminiVisible }) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Toggle Visibility",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        // Section: ElevenLabs API Key
        Text(
            text = "ElevenLabs API Key",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = elevenLabsKeyInput,
            onValueChange = { elevenLabsKeyInput = it },
            placeholder = { Text("ElevenLabs API Key yahan dalein") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            singleLine = true,
            visualTransformation = if (isElevenLabsVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val icon = if (isElevenLabsVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                IconButton(onClick = { isElevenLabsVisible = !isElevenLabsVisible }) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Toggle Visibility",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        // Section: System Permissions Section (Real & Working)
        Text(
            text = "System Permissions (Real & Working)",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

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
                SettingsPermissionRow(
                    label = "Grant Microphone Permission",
                    isGranted = permissionStatus.microphone,
                    onGrant = { permissionManager.requestMicrophone() }
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                SettingsPermissionRow(
                    label = "Enable Accessibility & Swipe Control",
                    isGranted = permissionStatus.accessibility,
                    onGrant = { permissionManager.openAccessibilitySettings() }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Save All Changes Button
        Button(
            onClick = {
                // Save both keys securely in Encrypted SharedPreferences
                val trimmedGemini = geminiKeyInput.trim()
                if (trimmedGemini.isNotBlank()) {
                    secureStore.put("CUSTOM_GEMINI_API_KEY", trimmedGemini)
                } else {
                    secureStore.remove("CUSTOM_GEMINI_API_KEY")
                }

                val trimmedEleven = elevenLabsKeyInput.trim()
                if (trimmedEleven.isNotBlank()) {
                    secureStore.put("CUSTOM_ELEVENLABS_API_KEY", trimmedEleven)
                } else {
                    secureStore.remove("CUSTOM_ELEVENLABS_API_KEY")
                }

                onBack()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Save All Changes",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SettingsPermissionRow(
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
                contentDescription = if (isGranted) "Enabled" else "Disabled",
                tint = if (isGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (!isGranted) {
            Button(
                onClick = onGrant,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                text = "Enabled",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
        }
    }
}
