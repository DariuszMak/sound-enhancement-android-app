package com.example.soundenchancement

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.content.pm.ServiceInfo

class AudioBoostService : Service() {

    private val binder = LocalBinder()

    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    inner class LocalBinder : android.os.Binder() {
        fun getService(): AudioBoostService = this@AudioBoostService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        enableEffects()
    }

    private fun enableEffects() {
        try {
            loudnessEnhancer = LoudnessEnhancer(0)
            loudnessEnhancer?.setTargetGain(1000)
            loudnessEnhancer?.enabled = true

            bassBoost = BassBoost(0, 0)
            bassBoost?.setStrength(1000)
            bassBoost?.enabled = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        bassBoost?.release()
        loudnessEnhancer?.release()
        super.onDestroy()
    }

    private fun startForegroundService() {
        val channelId = "boost_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sound Booster",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sound Enhancement Active")
            .setContentText("Bass & Volume Booster Running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, notification)
        }
    }
}