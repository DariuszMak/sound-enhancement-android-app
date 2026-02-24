package com.example.soundenchancement

import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ServiceScenario
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*


@RunWith(AndroidJUnit4::class)
class AudioBoostServiceTest {

    @Test
    fun serviceStartsAndInitializesEffects() {
        // Launch the service
        val intent = Intent(ApplicationProvider.getApplicationContext(), AudioBoostService::class.java)
        ServiceScenario.launch<AudioBoostService>(intent).use { scenario ->

            scenario.onActivity { service ->
                // Check that the service is running
                assertNotNull(service)

                // Use reflection to access private properties for testing (not ideal, but works for instrumentation test)
                val bassBoostField = AudioBoostService::class.java.getDeclaredField("bassBoost")
                bassBoostField.isAccessible = true
                val bassBoost = bassBoostField.get(service) as BassBoost?
                assertNotNull(bassBoost)
                assertTrue(bassBoost?.strength?.toInt() ?: 0 > 0)
                assertTrue(bassBoost?.enabled ?: false)

                val loudnessField = AudioBoostService::class.java.getDeclaredField("loudnessEnhancer")
                loudnessField.isAccessible = true
                val loudnessEnhancer = loudnessField.get(service) as LoudnessEnhancer?
                assertNotNull(loudnessEnhancer)
                assertEquals(1000, loudnessEnhancer?.targetGain)
                assertTrue(loudnessEnhancer?.enabled ?: false)
            }
        }
    }
}