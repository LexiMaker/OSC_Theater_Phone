package com.theaterphone.data.model

sealed class OscCommand {
    data object Ping : OscCommand()
    data class Call(val name: String, val number: String = "") : OscCommand()
    data object Hangup : OscCommand()
    data class Sms(val sender: String, val text: String) : OscCommand()
    data class Vibrate(val mode: String = "single") : OscCommand()
    data class Audio(val name: String) : OscCommand()
    data object AudioStop : OscCommand()
}
