package com.theaterphone.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.Timer
import java.util.TimerTask

object AudioService {

    private var vibrator: Vibrator? = null
    private var vibrationTimer: Timer? = null

    fun init(context: Context) {
        val app = context.applicationContext
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrate() {
        vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
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
        vibrator?.cancel()
    }

    fun playEndCallTone() {
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                .startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        } catch (e: Exception) {
            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
