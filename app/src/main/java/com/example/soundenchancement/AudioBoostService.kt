package com.example.soundenchancement

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class AudioBoostService : Service() {

    companion object {
        var effectFactory: AudioEffectFactory = AudioEffectFactory()
    }

    private val binder = LocalBinder()

    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    inner class LocalBinder : android.os.Binder() {
        fun getService(): AudioBoostService = this@AudioBoostService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        enableEffects()
        startForegroundService()
        Log.d("AudioBoostService", "Service created")
    }

    private fun enableEffects() {
        try {
            val sessionId = 0

            loudnessEnhancer = effectFactory
                .createLoudnessEnhancer(sessionId)
                .apply {
                    setTargetGain(1000)
                    enabled = true
                }

            bassBoost = effectFactory
                .createBassBoost(sessionId)
                .apply {
                    setStrength(1000)
                    enabled = true
                }

        } catch (e: Exception) {
            Log.e("AudioBoostService", "Effect initialization failed", e)
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

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sound Enhancement Active")
            .setContentText("Bass & Volume Booster Running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(1, notification)
        }
    }

    // For test access (cleaner than reflection)
    fun getBassBoost(): BassBoost? = bassBoost
    fun getLoudnessEnhancer(): LoudnessEnhancer? = loudnessEnhancer
}