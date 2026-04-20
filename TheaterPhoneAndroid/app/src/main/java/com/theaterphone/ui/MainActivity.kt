package com.theaterphone.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.theaterphone.audio.AudioService
import com.theaterphone.audio.SoundLibraryManager
import com.theaterphone.call.CallManager
import com.theaterphone.data.model.CallPhase
import com.theaterphone.service.CommandDispatcher
import com.theaterphone.service.OscListenerService
import com.theaterphone.sms.SmsManager
import com.theaterphone.sms.SmsNotificationHelper
import com.theaterphone.ui.screen.*
import com.theaterphone.ui.theme.TheaterPhoneTheme

class MainActivity : ComponentActivity() {

    private val callManager = CallManager()
    private val smsManager = SmsManager()
    private lateinit var soundLibrary: SoundLibraryManager

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        soundLibrary = SoundLibraryManager(this)

        callManager.appContext = this
        smsManager.appContext = this
        AudioService.init(this)

        CommandDispatcher.callManager = callManager
        CommandDispatcher.smsManager = smsManager
        CommandDispatcher.soundLibrary = soundLibrary

        requestPermissions()
        startOscService()

        handleIntent(intent)

        setContent {
            TheaterPhoneTheme {
                MainContent(
                    callManager = callManager,
                    smsManager = smsManager,
                    soundLibrary = soundLibrary,
                    onRestartService = { port, mode ->
                        OscListenerService.port = port
                        OscListenerService.mode = mode
                        startOscService()
                    },
                    onTestCall = {
                        callManager.incomingCall("Mom", "+1 555 1234567")
                    },
                    onTestSms = {
                        smsManager.receiveMessage("Max", "Where are you? The show is about to start!")
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(SmsNotificationHelper.EXTRA_OPEN_SMS, false) == true) {
            smsManager.openConversation()
        }
    }

    private fun startOscService() {
        val intent = Intent(this, OscListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

@Composable
fun MainContent(
    callManager: CallManager,
    smsManager: SmsManager,
    soundLibrary: SoundLibraryManager,
    onRestartService: (port: Int, mode: com.theaterphone.data.model.CommunicationMode) -> Unit,
    onTestCall: () -> Unit,
    onTestSms: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    val callPhase by callManager.phase.collectAsState()
    val messages by smsManager.messages.collectAsState()
    val isShowingSms by smsManager.isShowingConversation.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            showSettings -> {
                SettingsScreen(
                    soundLibrary = soundLibrary,
                    onDismiss = { showSettings = false },
                    onRestartService = onRestartService,
                    onTestCall = {
                        showSettings = false
                        onTestCall()
                    },
                    onTestSms = {
                        showSettings = false
                        onTestSms()
                    }
                )
            }
            isShowingSms -> {
                SmsConversationScreen(
                    messages = messages,
                    onBack = { smsManager.closeConversation() }
                )
            }
            else -> {
                // Lock screen as default
                LockScreen()

                // Call overlays
                AnimatedVisibility(
                    visible = callPhase is CallPhase.Active,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val phase = callPhase
                    if (phase is CallPhase.Active) {
                        ActiveCallScreen(
                            callerName = phase.callerName,
                            callerNumber = phase.callerNumber,
                            startTimeMs = phase.startTimeMs,
                            onEndCall = { callManager.endCall() }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = callPhase is CallPhase.Ended,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    CallEndedScreen()
                }

                // Settings gear
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
