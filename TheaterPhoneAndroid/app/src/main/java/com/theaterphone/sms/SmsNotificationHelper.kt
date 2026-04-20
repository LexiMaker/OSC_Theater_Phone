package com.theaterphone.sms

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.theaterphone.ui.MainActivity
import java.util.concurrent.atomic.AtomicInteger

object SmsNotificationHelper {

    const val EXTRA_OPEN_SMS = "open_sms"

    private const val CHANNEL_ID = "sms_messages"
    private val notificationId = AtomicInteger(200)
    @Volatile private var channelCreated = false

    fun showSmsNotification(context: Context, sender: String, text: String) {
        ensureChannel(context)

        val id = notificationId.getAndIncrement()
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_SMS, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(sender)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .build()

        context.notificationManager().notify(id, notification)
    }

    private fun ensureChannel(context: Context) {
        if (channelCreated) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SMS Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming SMS notifications"
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.notificationManager().createNotificationChannel(channel)
        channelCreated = true
    }
}

internal fun Context.notificationManager(): NotificationManager =
    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
