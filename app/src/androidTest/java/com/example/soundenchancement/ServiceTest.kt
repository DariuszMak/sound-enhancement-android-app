package com.example.soundenchancement

import android.content.Context
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.ServiceTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioBoostServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    @Test
    fun serviceStartsAndInitializesEffects() {

        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, AudioBoostService::class.java)

        val binder = serviceRule.bindService(intent)
        val service = (binder as AudioBoostService.LocalBinder).getService()

        assertNotNull(service)

        val bassBoostField =
            AudioBoostService::class.java.getDeclaredField("bassBoost")
        bassBoostField.isAccessible = true
        val bassBoost = bassBoostField.get(service) as BassBoost?

        assertNotNull(bassBoost)
        assertTrue((bassBoost?.roundedStrength?.toInt() ?: 0) > 0)
        assertTrue(bassBoost?.enabled ?: false)

        val loudnessField =
            AudioBoostService::class.java.getDeclaredField("loudnessEnhancer")
        loudnessField.isAccessible = true
        val loudnessEnhancer = loudnessField.get(service) as LoudnessEnhancer?

        assertNotNull(loudnessEnhancer)
        assertEquals(1000, loudnessEnhancer?.targetGain)
        assertTrue(loudnessEnhancer?.enabled ?: false)
    }
}