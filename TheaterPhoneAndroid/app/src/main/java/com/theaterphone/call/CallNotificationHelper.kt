package com.theaterphone.call

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.theaterphone.sms.notificationManager

object CallNotificationHelper {

    const val EXTRA_CALLER_NAME = "caller_name"
    const val EXTRA_CALLER_NUMBER = "caller_number"

    private const val CHANNEL_ID = "incoming_call"
    private const val NOTIFICATION_ID = 100
    private val RING_PATTERN = longArrayOf(0, 1000, 500, 1000, 500, 1000)

    @Volatile private var channelCreated = false

    fun showIncomingCall(context: Context, callerName: String, callerNumber: String) {
        ensureChannel(context)

        val fullScreenIntent = Intent(context, IncomingCallActivity::class.java).apply {
            putExtra(EXTRA_CALLER_NAME, callerName)
            putExtra(EXTRA_CALLER_NUMBER, callerNumber)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPending = PendingIntent.getActivity(
            context, 0, fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Incoming Call")
            .setContentText(callerName)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPending, true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
            .setVibrate(RING_PATTERN)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        context.notificationManager().notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification(context: Context) {
        context.notificationManager().cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel(context: Context) {
        if (channelCreated) return
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Full-screen incoming call notifications"
            setSound(ringtoneUri, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            vibrationPattern = RING_PATTERN
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        context.notificationManager().createNotificationChannel(channel)
        channelCreated = true
    }
}
