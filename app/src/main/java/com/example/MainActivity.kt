package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var myraCore: MyraCore
    private lateinit var geminiClient: GeminiLiveClient
    private lateinit var permissionManager: MyraPermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        myraCore = MyraCore(applicationContext)
        geminiClient = GeminiLiveClient(applicationContext, myraCore)
        permissionManager = MyraPermissionManager(this)

        val secureStore = MyraSecureStore(applicationContext)

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf("main") }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    when (currentScreen) {
                        "settings" -> {
                            SettingsScreen(
                                secureStore = secureStore,
                                permissionManager = permissionManager,
                                onBack = { currentScreen = "main" },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        else -> {
                            MyraLiveScreen(
                                myraCore = myraCore,
                                geminiClient = geminiClient,
                                permissionManager = permissionManager,
                                onOpenSettings = { currentScreen = "settings" },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        geminiClient.stop()
    }
}
