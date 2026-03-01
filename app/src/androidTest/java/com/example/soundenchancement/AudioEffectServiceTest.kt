package com.example.soundenchancement

import android.content.Context
import android.content.Intent
import android.media.audiofx.Equalizer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioEffectServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    @Test
    fun serviceStartsAndAppliesEqualizer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, AudioEffectService::class.java)

        val binder = serviceRule.bindService(intent)
        val service = (binder as AudioEffectService.LocalBinder).getService()

        assertNotNull(service)

        val equalizerField =
            AudioEffectService::class.java.getDeclaredField("equalizer")
        equalizerField.isAccessible = true

        val eq = equalizerField.get(service) as Equalizer?
        assertNotNull(eq)
        assertTrue(eq?.enabled == true)

        val numberOfBands = eq!!.numberOfBands
        val (minLevel, maxLevel) = eq.bandLevelRange

        for (i in 0 until numberOfBands) {
            val bandLevel = eq.getBandLevel(i.toShort())
            assertTrue(
                "Band $i level $bandLevel out of range",
                bandLevel in minLevel..maxLevel
            )
        }

        val firstBand = eq.getBandLevel(0)
        val midBand = eq.getBandLevel((numberOfBands / 2).toShort())
        assertTrue("Bass band should be stronger than mid band", firstBand > midBand)
    }
}