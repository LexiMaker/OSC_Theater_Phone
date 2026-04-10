package com.theaterphone.ui.screen

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theaterphone.audio.SoundLibraryManager
import com.theaterphone.data.model.CommunicationMode
import com.theaterphone.service.OscListenerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    soundLibrary: SoundLibraryManager,
    onDismiss: () -> Unit,
    onRestartService: (port: Int, mode: CommunicationMode) -> Unit,
    onTestCall: () -> Unit,
    onTestSms: () -> Unit
) {
    val isListening by OscListenerService.isListening.collectAsState()
    val localIp by OscListenerService.localIp.collectAsState()
    val lastMessage by OscListenerService.lastMessage.collectAsState()
    val sounds by soundLibrary.sounds.collectAsState()
    val nowPlaying by soundLibrary.nowPlaying.collectAsState()

    var portText by remember { mutableStateOf(OscListenerService.port.toString()) }
    var selectedMode by remember { mutableStateOf(OscListenerService.mode) }

    // Audio file picker
    var showNameDialog by remember { mutableStateOf(false) }
    var pendingFileName by remember { mutableStateOf("") }
    var soundName by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingFileName = uri.lastPathSegment?.substringAfterLast("/")?.substringBeforeLast(".") ?: "sound"
            soundName = pendingFileName
            showNameDialog = true
            // Store URI temporarily
            soundLibrary.pendingUri = uri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                actions = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Connection
            SectionHeader("Connection")

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mode", modifier = Modifier.weight(1f))
                CommunicationMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        label = { Text(mode.displayName) },
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            SettingsRow("Protocol", if (selectedMode == CommunicationMode.OSC) "UDP" else "UDP + TCP")

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status", modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isListening) Color(0xFF30D158) else Color.Red,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isListening) "Active" else "Inactive",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            SettingsRow("IP Address", localIp)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Port", modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(100.dp),
                    singleLine = true
                )
            }

            Button(
                onClick = {
                    val p = portText.toIntOrNull() ?: 9000
                    onRestartService(p, selectedMode)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Restart Connection") }

            HorizontalDivider()

            // Commands
            SectionHeader("Commands")
            if (selectedMode == CommunicationMode.OSC) {
                CommandRef("/call", "<Name> [Number]", "Incoming call")
                CommandRef("/hangup", "", "End call")
                CommandRef("/sms", "<Sender> <Text>", "Receive SMS")
                CommandRef("/vibrate", "[single|pattern|stop]", "Vibration only")
                CommandRef("/audio", "<Name> | stop", "Play/stop audio file")
                CommandRef("/ping", "", "App responds with /pong")
            } else {
                CommandRef("call", "<Name> [Number]", "Incoming call")
                CommandRef("hangup", "", "End call")
                CommandRef("sms", "<Sender> <Text>", "Receive SMS")
                CommandRef("vibrate", "[single|pattern|stop]", "Vibration only")
                CommandRef("audio", "<Name> | stop", "Play/stop audio file")
                CommandRef("ping", "", "App responds with pong")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Use quotes for arguments with spaces:\ncall \"Mom\" \"+1 555 1234567\"",
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            HorizontalDivider()

            // Audio Library
            SectionHeader("Audio Library")
            if (sounds.isEmpty()) {
                Text("No sounds loaded", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            } else {
                sounds.forEach { sound ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sound.name, fontWeight = FontWeight.Bold)
                            Text(
                                sound.fileName.substringAfterLast(".").uppercase(),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        if (nowPlaying == sound.name) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "Playing",
                                tint = Color(0xFF30D158),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        IconButton(onClick = { soundLibrary.deleteSound(sound.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    filePickerLauncher.launch(arrayOf("audio/*"))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add Sound") }

            Text(
                "Trigger sounds via ${if (selectedMode == CommunicationMode.OSC) "/audio <name>" else "audio <name>"}. Names are case-insensitive.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            HorizontalDivider()

            // Test
            SectionHeader("Test")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onTestCall, modifier = Modifier.weight(1f)) { Text("Test Call") }
                OutlinedButton(onClick = onTestSms, modifier = Modifier.weight(1f)) { Text("Test SMS") }
            }
            if (sounds.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { soundLibrary.play(sounds.first().name) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Test Audio") }
                    if (nowPlaying != null) {
                        OutlinedButton(
                            onClick = { soundLibrary.stop() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Stop Audio") }
                    }
                }
            }

            HorizontalDivider()

            // Info
            SectionHeader("Info")
            SettingsRow("Version", "1.0.0")
            SettingsRow("Last Command", lastMessage.ifEmpty { "–" })

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Name dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Sound Name") },
            text = {
                Column {
                    Text("Enter a name for this sound. Use this name in commands to play it.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = soundName,
                        onValueChange = { soundName = it },
                        singleLine = true,
                        label = { Text("Name") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (soundName.isNotBlank()) {
                        soundLibrary.pendingUri?.let { uri ->
                            soundLibrary.importFile(uri, soundName.trim())
                        }
                        soundLibrary.pendingUri = null
                        showNameDialog = false
                        soundName = ""
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNameDialog = false
                    soundLibrary.pendingUri = null
                    soundName = ""
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun CommandRef(command: String, args: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(command, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A84FF))
            Text(args, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}
