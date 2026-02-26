package com.example.soundenchancement

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class AudioEffectService : Service() {

    companion object {
        var bassBoostFactory: () -> IBassBoost = { RealBassBoost() }
    }

    private val binder = LocalBinder()
    private var bassBoost: IBassBoost? = null

    inner class LocalBinder : android.os.Binder() {
        fun getService(): AudioEffectService = this@AudioEffectService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        enableBassBoost()
        Log.d("AudioBoostService", "Global Bass Boost Enabled")
    }

    private fun enableBassBoost() {
        try {
            bassBoost = bassBoostFactory()
            bassBoost?.strength = 1000
            bassBoost?.enabled = true
        } catch (e: Exception) {
            Log.e("AudioBoostService", "BassBoost not supported", e)
        }
    }

    override fun onDestroy() {
        bassBoost?.release()
        bassBoost = null
        super.onDestroy()
    }

    private fun startForegroundService() {
        val channelId = "boost_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Bass Booster",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bass Boost Active")
            .setContentText("Boosting system bass")
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
}