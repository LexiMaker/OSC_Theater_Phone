package com.theaterphone.call

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theaterphone.data.model.CallPhase
import com.theaterphone.service.CommandDispatcher
import com.theaterphone.ui.MainActivity
import com.theaterphone.ui.theme.TheaterPhoneTheme

class IncomingCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val callerName = intent?.getStringExtra(CallNotificationHelper.EXTRA_CALLER_NAME) ?: "Unknown"
        val callerNumber = intent?.getStringExtra(CallNotificationHelper.EXTRA_CALLER_NUMBER) ?: ""

        setContent {
            val callPhase by CommandDispatcher.callManager!!.phase.collectAsState()
            LaunchedEffect(callPhase) {
                // e.g. /hangup arrives via OSC while the lock-screen UI is up
                if (callPhase !is CallPhase.Ringing) finish()
            }

            TheaterPhoneTheme {
                IncomingCallScreen(
                    callerName = callerName,
                    callerNumber = callerNumber,
                    onAccept = {
                        CommandDispatcher.callManager?.acceptCall()
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                        })
                        finish()
                    },
                    onDecline = {
                        CommandDispatcher.callManager?.declineCall()
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun IncomingCallScreen(
    callerName: String,
    callerNumber: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(120.dp))

        Text(callerName, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        if (callerNumber.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(callerNumber, color = Color.White.copy(alpha = 0.6f), fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Incoming Call...", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 80.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AnswerButton(Icons.Default.CallEnd, "Decline", Color.Red, onClick = onDecline)
            AnswerButton(Icons.Default.Call, "Accept", Color(0xFF30D158), onClick = onAccept)
        }
    }
}

@Composable
private fun AnswerButton(icon: ImageVector, label: String, bgColor: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(70.dp)
                .background(bgColor, CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
    }
}
