package com.example.soundenchancement

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.audiofx.Equalizer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Holds the user-configurable EQ parameters.
 * Default values match the original hard-coded constants.
 *
 * @param baseLevel   Overall boost strength in millibels (default 700).
 * @param multipliers Per-band multipliers indexed 0-7, corresponding to the
 *                    frequency buckets ≤60, ≤120, ≤250, ≤500, ≤2000,
 *                    ≤4000, ≤8000, and >8000 Hz respectively.
 */
data class EqConfig(
    val baseLevel: Int = 700,
    val multipliers: DoubleArray = doubleArrayOf(1.10, 0.90, 0.70, 0.40, 0.40, 0.45, 0.70, 0.80)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EqConfig) return false
        return baseLevel == other.baseLevel && multipliers.contentEquals(other.multipliers)
    }
    override fun hashCode(): Int = 31 * baseLevel + multipliers.contentHashCode()
}

/**
 * Calculates the equalizer band level for a given frequency and base level.
 * Logic is unchanged from the original implementation.
 *
 * @param freqHz    Center frequency of the band in Hz (not milliHz).
 * @param baseLevel Desired base boost strength in millibels (e.g. 700).
 * @param minLevel  Minimum band level supported by the device equalizer (millibels).
 * @param maxLevel  Maximum band level supported by the device equalizer (millibels).
 * @param config    EQ configuration carrying the per-band multipliers.
 * @return          Band level clamped to [minLevel, maxLevel].
 */
fun calculateBandLevel(
    freqHz: Double,
    baseLevel: Int,
    minLevel: Int,
    maxLevel: Int,
    config: EqConfig = EqConfig()
): Short {
    val m = config.multipliers
    val boost = when {
        freqHz <= 60   -> baseLevel * m[0]
        freqHz <= 120  -> baseLevel * m[1]
        freqHz <= 250  -> baseLevel * m[2]
        freqHz <= 500  -> baseLevel * m[3]
        freqHz <= 2000 -> baseLevel * m[4]
        freqHz <= 4000 -> baseLevel * m[5]
        freqHz <= 8000 -> baseLevel * m[6]
        else           -> baseLevel * m[7]
    }

    val range = maxLevel - minLevel
    val scaled = (Math.pow(boost / 1000.0, 1.2) * range).toInt() + minLevel
    return scaled.coerceIn(minLevel, maxLevel).toShort()
}

class AudioEffectService : Service() {

    private val binder = LocalBinder()

    // `internal` so instrumented tests can read it via the binder.
    internal var equalizer: Equalizer? = null

    /** Reflects whether the equalizer is currently processing audio. */
    var isEqEnabled: Boolean = false
        private set

    inner class LocalBinder : android.os.Binder() {
        fun getService(): AudioEffectService = this@AudioEffectService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        applyConfig(EqConfig())                      // start with defaults
        Log.d("AudioBoostService", "Dynamic Bass Enabled")
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Applies [config] to the equalizer and ensures it is enabled.
     * Safe to call at any time; always rebuilds band levels from scratch.
     */
    fun applyConfig(config: EqConfig) {
        enableProfessionalDynamicBass(config)
    }

    /**
     * Disables the equalizer effect without releasing it.
     * Band levels are preserved so [enableEq] restores sound instantly.
     */
    fun disableEq() {
        equalizer?.enabled = false
        isEqEnabled = false
        Log.d("AudioBoostService", "Equalizer disabled")
    }

    /**
     * Re-enables the equalizer effect with the previously applied band levels.
     */
    fun enableEq() {
        equalizer?.enabled = true
        isEqEnabled = true
        Log.d("AudioBoostService", "Equalizer re-enabled")
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun enableProfessionalDynamicBass(config: EqConfig) {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, 0)
            equalizer?.enabled = true
            isEqEnabled = true

            equalizer?.let { eq ->
                val numberOfBands = eq.numberOfBands
                val (minLevel, maxLevel) = eq.bandLevelRange

                for (i in 0 until numberOfBands) {
                    val freqHz = eq.getCenterFreq(i.toShort()) / 1000.0
                    val level = calculateBandLevel(
                        freqHz,
                        config.baseLevel,
                        minLevel.toInt(),
                        maxLevel.toInt(),
                        config
                    )
                    eq.setBandLevel(i.toShort(), level)
                }
            }

            Log.d("AudioBoostService", "EQ applied: baseLevel=${config.baseLevel}")
        } catch (e: Exception) {
            Log.e("AudioBoostService", "Error applying EQ", e)
        }
    }

    override fun onDestroy() {
        try {
            equalizer?.release()
            equalizer = null
            isEqEnabled = false
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