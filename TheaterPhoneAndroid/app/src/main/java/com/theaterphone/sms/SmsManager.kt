package com.theaterphone.sms

import android.content.Context
import com.theaterphone.data.model.SmsMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages SMS messages and conversation state.
 * Port of iOS SMSManager.
 */
class SmsManager {

    private val _messages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val messages: StateFlow<List<SmsMessage>> = _messages

    private val _isShowingConversation = MutableStateFlow(false)
    val isShowingConversation: StateFlow<Boolean> = _isShowingConversation

    var appContext: Context? = null

    fun receiveMessage(sender: String, text: String) {
        val msg = SmsMessage(sender = sender, text = text)
        _messages.value = _messages.value + msg
        appContext?.let { ctx ->
            SmsNotificationHelper.showSmsNotification(ctx, sender, text)
        }
    }

    fun openConversation() {
        _isShowingConversation.value = true
    }

    fun closeConversation() {
        _isShowingConversation.value = false
    }
}
