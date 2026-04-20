package com.theaterphone.call

import android.content.Context
import com.theaterphone.audio.AudioService
import com.theaterphone.data.model.CallPhase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CallManager {

    private val _phase = MutableStateFlow<CallPhase>(CallPhase.Inactive)
    val phase: StateFlow<CallPhase> = _phase

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var resetJob: Job? = null
    var appContext: Context? = null

    fun incomingCall(name: String, number: String = "") {
        _phase.value = CallPhase.Ringing(callerName = name, callerNumber = number)
        appContext?.let { ctx ->
            CallNotificationHelper.showIncomingCall(ctx, name, number)
        }
    }

    fun acceptCall() {
        val current = _phase.value
        if (current is CallPhase.Ringing) {
            appContext?.let { CallNotificationHelper.cancelNotification(it) }
            _phase.value = CallPhase.Active(
                callerName = current.callerName,
                callerNumber = current.callerNumber,
                startTimeMs = System.currentTimeMillis()
            )
        }
    }

    /** User declines before answering. */
    fun declineCall() = terminate(playTone = false)

    /** User ends the active call manually. */
    fun endCall() = terminate(playTone = false)

    /** OSC `/hangup` — plays disconnect tone. */
    fun hangUp() = terminate(playTone = true)

    private fun terminate(playTone: Boolean) {
        appContext?.let { CallNotificationHelper.cancelNotification(it) }
        if (playTone) AudioService.playEndCallTone()
        _phase.value = CallPhase.Ended
        scheduleReset()
    }

    private fun scheduleReset() {
        resetJob?.cancel()
        resetJob = scope.launch {
            delay(1500)
            if (_phase.value is CallPhase.Ended) {
                _phase.value = CallPhase.Inactive
            }
        }
    }
}
