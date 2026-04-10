package com.theaterphone.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.Timer
import java.util.TimerTask

/**
 * Handles vibration and call-end tone.
 * Port of iOS AudioService.
 */
object AudioService {

    private var appContext: Context? = null
    private var vibrationTimer: Timer? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getVibrator(): Vibrator? {
        val ctx = appContext ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrate() {
        val vibrator = getVibrator() ?: return
        val effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    }

    fun startVibrationPattern() {
        stopVibrationPattern()
        vibrate()
        vibrationTimer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() { vibrate() }
            }, 1500, 1500)
        }
    }

    fun stopVibrationPattern() {
        vibrationTimer?.cancel()
        vibrationTimer = null
        getVibrator()?.cancel()
    }

    fun playEndCallTone() {
        val ctx = appContext ?: return
        try {
            // Use system notification sound as disconnect tone
            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                // ToneGenerator alternative: use a short beep
            }
            // Play a short beep using ToneGenerator
            val toneGen = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_NOTIFICATION, 80
            )
            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 300)
            mediaPlayer.release()
        } catch (e: Exception) {
            // Fallback: just vibrate briefly
            val vibrator = getVibrator()
            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
