package com.theaterphone.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
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
import kotlinx.coroutines.delay

@Composable
fun ActiveCallScreen(
    callerName: String,
    callerNumber: String,
    startTimeMs: Long,
    onEndCall: () -> Unit
) {
    var elapsed by remember { mutableStateOf("0:00") }

    LaunchedEffect(startTimeMs) {
        while (true) {
            val seconds = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
            val min = seconds / 60
            val sec = seconds % 60
            elapsed = "$min:${sec.toString().padStart(2, '0')}"
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(callerName, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        if (callerNumber.isNotEmpty()) {
            Text(callerNumber, color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(elapsed, color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CallButton(icon = Icons.Default.MicOff, label = "Mute", Color(0xFF2C2C2E)) {}
            CallButton(icon = Icons.Default.VolumeUp, label = "Speaker", Color(0xFF2C2C2E)) {}
        }

        Spacer(modifier = Modifier.height(40.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onEndCall,
                modifier = Modifier
                    .size(70.dp)
                    .background(Color.Red, CircleShape)
            ) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("End Call", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
private fun CallButton(icon: ImageVector, label: String, bgColor: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(60.dp)
                .background(bgColor, CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}
