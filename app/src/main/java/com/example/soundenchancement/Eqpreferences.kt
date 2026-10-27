package com.example.soundenhancement

import android.content.Context
import android.content.SharedPreferences


class EqPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)


    fun saveBaseLevel(progress: Int) =
        prefs.edit().putInt(KEY_BASE_LEVEL, progress).apply()

    fun saveBandProgress(band: Int, progress: Int) =
        prefs.edit().putInt(bandKey(band), progress).apply()

    fun saveIsActive(active: Boolean) =
        prefs.edit().putBoolean(KEY_IS_ACTIVE, active).apply()

    fun saveCurrentPreset(index: Int) =
        prefs.edit().putInt(KEY_CURRENT_PRESET, index).apply()

    fun loadBaseLevel(): Int =
        prefs.getInt(KEY_BASE_LEVEL, DEFAULT_BASE_LEVEL)

    fun loadBandProgress(band: Int): Int =
        prefs.getInt(bandKey(band), DEFAULT_BAND_PROGRESS[band])

    fun loadIsActive(): Boolean =
        prefs.getBoolean(KEY_IS_ACTIVE, true)

    fun loadCurrentPreset(): Int =
        prefs.getInt(KEY_CURRENT_PRESET, PRESET_HOME_HEADPHONES)

    private fun bandKey(band: Int) = "${KEY_BAND_PREFIX}$band"

    companion object {
        private const val PREF_FILE = "eq_settings"
        private const val KEY_BASE_LEVEL = "base_level"
        private const val KEY_BAND_PREFIX = "band_"
        private const val KEY_IS_ACTIVE = "is_active"
        private const val KEY_CURRENT_PRESET = "current_preset"

        const val DEFAULT_BASE_LEVEL = 700

        val DEFAULT_BAND_PROGRESS = intArrayOf(200, 200, 0, 110, 200)

        const val PRESET_HOME_HEADPHONES = 0
        const val PRESET_CAR = 1
        const val PRESET_HOME_SYSTEM = 2

        val PRESET_MULTIPLIERS = arrayOf(
            doubleArrayOf(2.00, 2.00, 0.00, 1.10, 2.00),
            doubleArrayOf(1.50, 2.00, 0.00, 1.20, 2.00),
            doubleArrayOf(1.60, 1.20, 0.00, 2.00, 2.00)
        )
    }
}