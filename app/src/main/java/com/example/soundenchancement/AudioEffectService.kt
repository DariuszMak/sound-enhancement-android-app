package com.example.soundenchancement

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.audiofx.Equalizer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.math.pow
import kotlin.math.roundToInt

class AudioEffectService : Service() {

    private val binder = LocalBinder()
    private var equalizer: Equalizer? = null

    inner class LocalBinder : android.os.Binder() {
        fun getService(): AudioEffectService = this@AudioEffectService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        enableProfessionalDynamicBass()
        Log.d("AudioBoostService", "Dynamic Bass Enabled")
    }

    private fun enableProfessionalDynamicBass(baseLevel: Int = 700) {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, 0)
            equalizer?.enabled = true

            equalizer?.let { eq ->
                val numberOfBands = eq.numberOfBands
                val (minLevel, maxLevel) = eq.bandLevelRange

                for (i in 0 until numberOfBands) {
                    val freq = eq.getCenterFreq(i.toShort()) / 1000.0 // Hz

                    val boost = when {
                        freq <= 60 -> baseLevel.toDouble()                  // deep bass
                        freq <= 120 -> baseLevel * 0.75
                        freq <= 250 -> baseLevel * 0.5
                        freq <= 500 -> baseLevel * 0.25                     // low-mids
                        freq <= 2000 -> baseLevel * 0.25                     // mids (vocals)
                        freq <= 4000 -> baseLevel * 0.45                    // presence
                        freq <= 8000 -> baseLevel * 0.9                    // upper mids / percussion
                        else -> baseLevel * 1.0                              // highs for clarity/air
                    }

                    // Exponential scaling for smooth, musical sound
                    val scaledBoost = ((boost / 1000.0).pow(1.2) * (maxLevel - minLevel)).roundToInt() + minLevel
                    val bandLevelShort = scaledBoost.coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                    eq.setBandLevel(i.toShort(), bandLevelShort)
                }
            }

            Log.d("AudioBoostService", "Professional dynamic bass + enhanced clarity applied")
        } catch (e: Exception) {
            Log.e("AudioBoostService", "Error applying professional dynamic bass + clarity", e)
        }
    }

    override fun onDestroy() {
        try {
            equalizer?.release()
            equalizer = null
        } catch (e: Exception) {
            Log.e("AudioBoostService", "Error releasing Equalizer", e)
        }
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
            .setOngoing(true)
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