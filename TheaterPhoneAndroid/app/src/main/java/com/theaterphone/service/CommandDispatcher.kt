package com.theaterphone.service

import com.theaterphone.audio.AudioService
import com.theaterphone.audio.SoundLibraryManager
import com.theaterphone.call.CallManager
import com.theaterphone.data.model.OscCommand
import com.theaterphone.sms.SmsManager

/**
 * Routes parsed commands to the appropriate managers.
 * Singleton — shared between OscListenerService and UI.
 */
object CommandDispatcher {

    var callManager: CallManager? = null
    var smsManager: SmsManager? = null
    var soundLibrary: SoundLibraryManager? = null
    var onPing: ((senderIp: String) -> Unit)? = null

    fun dispatch(command: OscCommand, senderIp: String? = null) {
        when (command) {
            is OscCommand.Ping -> {
                senderIp?.let { onPing?.invoke(it) }
            }
            is OscCommand.Call -> {
                callManager?.incomingCall(command.name, command.number)
            }
            is OscCommand.Hangup -> {
                callManager?.hangUp()
            }
            is OscCommand.Sms -> {
                smsManager?.receiveMessage(command.sender, command.text)
            }
            is OscCommand.Vibrate -> {
                when (command.mode) {
                    "pattern", "repeat" -> AudioService.startVibrationPattern()
                    "stop" -> AudioService.stopVibrationPattern()
                    else -> AudioService.vibrate()
                }
            }
            is OscCommand.Audio -> {
                soundLibrary?.play(command.name)
            }
            is OscCommand.AudioStop -> {
                soundLibrary?.stop()
            }
        }
    }
}
