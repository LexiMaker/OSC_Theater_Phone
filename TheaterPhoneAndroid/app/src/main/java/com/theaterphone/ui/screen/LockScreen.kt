package com.theaterphone.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theaterphone.service.OscListenerService
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/** Lock screen style view showing clock and connection status. */
@Composable
fun LockScreen() {
    val isListening by OscListenerService.isListening.collectAsState()
    val localIp by OscListenerService.localIp.collectAsState()
    val lastMessage by OscListenerService.lastMessage.collectAsState()

    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEEE, d. MMMM", Locale.getDefault()).format(now)
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Clock
            Text(
                text = currentTime,
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Thin,
                letterSpacing = 2.sp
            )
            Text(
                text = currentDate,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isListening) Color(0xFF30D158) else Color.Red,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Text(
                    text = if (isListening) "Listening" else "Inactive",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$localIp : ${OscListenerService.port}",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )

            if (lastMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = lastMessage,
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
