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
    var equalizer: Equalizer? = null
        private set

    inner class LocalBinder : android.os.Binder() {
        fun getService(): AudioEffectService = this@AudioEffectService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        initEqualizer()
        Log.d("AudioEffectService", "Equalizer initialized")
    }

    fun initEqualizer(baseLevel: Int = 700) {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, 0)
            equalizer?.enabled = true

            equalizer?.let { eq ->
                val numberOfBands = eq.numberOfBands
                val (minLevel, maxLevel) = eq.bandLevelRange

                for (i in 0 until numberOfBands) {
                    val freq = eq.getCenterFreq(i.toShort()) / 1000.0

                    val boost = when {
                        freq <= 60   -> baseLevel * 1.1
                        freq <= 120  -> baseLevel * 0.9
                        freq <= 250  -> baseLevel * 0.7
                        freq <= 500  -> baseLevel * 0.4
                        freq <= 2000 -> baseLevel * 0.4
                        freq <= 4000 -> baseLevel * 0.45
                        freq <= 8000 -> baseLevel * 0.7
                        else         -> baseLevel * 0.8
                    }

                    val scaledBoost = ((boost / 1000.0).pow(1.2) * (maxLevel - minLevel)).roundToInt() + minLevel
                    val bandLevel = scaledBoost.coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                    eq.setBandLevel(i.toShort(), bandLevel)
                }
            }
        } catch (e: Exception) {
            Log.e("AudioEffectService", "Error initializing equalizer", e)
        }
    }

    /**
     * Sets a specific band level from a 0–100 slider value.
     * Maps 0 → minLevel, 100 → maxLevel of the device's equalizer range.
     */
    fun setBandFromSlider(bandIndex: Int, sliderValue: Int) {
        val eq = equalizer ?: return
        try {
            val (minLevel, maxLevel) = eq.bandLevelRange
            val mapped = (minLevel + (sliderValue / 100f) * (maxLevel - minLevel)).roundToInt()
                .coerceIn(minLevel.toInt(), maxLevel.toInt())
                .toShort()
            eq.setBandLevel(bandIndex.toShort(), mapped)
        } catch (e: Exception) {
            Log.e("AudioEffectService", "Error setting band $bandIndex", e)
        }
    }

    /**
     * Returns the current band level as a 0–100 slider value.
     */
    fun getSliderValueForBand(bandIndex: Int): Int {
        val eq = equalizer ?: return 50
        return try {
            val (minLevel, maxLevel) = eq.bandLevelRange
            val current = eq.getBandLevel(bandIndex.toShort()).toInt()
            ((current - minLevel).toFloat() / (maxLevel - minLevel) * 100).roundToInt()
                .coerceIn(0, 100)
        } catch (e: Exception) {
            50
        }
    }

    /**
     * Returns band count and center frequencies (in Hz) for display.
     */
    fun getBandInfo(): List<Pair<Int, Float>> {
        val eq = equalizer ?: return emptyList()
        return (0 until eq.numberOfBands).map { i ->
            i to eq.getCenterFreq(i.toShort()) / 1000f
        }
    }

    /**
     * Returns the device's EQ range in millibels, e.g. -1500 to 1500.
     */
    fun getBandLevelRange(): Pair<Short, Short> {
        return equalizer?.bandLevelRange ?: Pair(-1500, 1500)
    }

    fun enableEqualizer() {
        equalizer?.enabled = true
    }

    fun disableEqualizer() {
        equalizer?.enabled = false
    }

    override fun onDestroy() {
        try {
            equalizer?.release()
            equalizer = null
        } catch (e: Exception) {
            Log.e("AudioEffectService", "Error releasing Equalizer", e)
        }
        super.onDestroy()
    }

    private fun startForegroundNotification() {
        val channelId = "boost_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Bass Booster",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bass Boost Active")
            .setContentText("Boosting system bass")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, notification)
        }
    }
}