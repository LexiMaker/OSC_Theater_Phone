package com.theaterphone.data.model

sealed class CallPhase {
    data object Inactive : CallPhase()
    data class Ringing(val callerName: String, val callerNumber: String) : CallPhase()
    data class Active(val callerName: String, val callerNumber: String, val startTimeMs: Long) : CallPhase()
    data object Ended : CallPhase()
}
