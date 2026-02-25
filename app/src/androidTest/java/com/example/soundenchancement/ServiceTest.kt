package com.example.soundenchancement

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.ServiceTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioBoostServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    @Before
    fun setup() {
        AudioBoostService.effectFactory = FakeAudioEffectFactory()
    }

    @After
    fun tearDown() {
        AudioBoostService.effectFactory = AudioEffectFactory()
    }

    @Test
    fun serviceStartsAndInitializesEffects() {

        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, AudioBoostService::class.java)

        val binder = serviceRule.bindService(intent)
        val service = (binder as AudioBoostService.LocalBinder).getService()

        assertNotNull(service)

        val bassBoost = service.getBassBoost()
        assertNotNull(bassBoost)
        assertTrue(bassBoost?.roundedStrength?.toInt() ?: 0 > 0)
        assertTrue(bassBoost?.enabled ?: false)

        val loudnessEnhancer = service.getLoudnessEnhancer()
        assertNotNull(loudnessEnhancer)
        assertEquals(1000, loudnessEnhancer?.targetGain)
        assertTrue(loudnessEnhancer?.enabled ?: false)
    }
}