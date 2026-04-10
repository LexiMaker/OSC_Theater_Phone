package com.theaterphone

import android.app.Application
import com.theaterphone.audio.AudioService

class TheaterPhoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AudioService.init(this)
    }
}
