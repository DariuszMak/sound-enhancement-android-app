package com.example.soundenhancement

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Random
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AudioEffectServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private fun bindService(): AudioEffectService {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val binder = serviceRule.bindService(Intent(context, AudioEffectService::class.java))
        return (binder as AudioEffectService.LocalBinder).getService()
    }

    private fun randomConfig(random: Random) = EqConfig(
        baseLevel = random.nextInt(1501),
        multipliers = DoubleArray(5) { random.nextInt(201) / 100.0 }
    )

    private fun expectedLevels(service: AudioEffectService, config: EqConfig): List<Short> {
        val eq = service.equalizer!!
        val (minLevel, maxLevel) = eq.bandLevelRange
        val freqs =
            DoubleArray(eq.numberOfBands.toInt()) { eq.getCenterFreq(it.toShort()) / 1000.0 }
        return calculateBandLevels(freqs, config, minLevel.toInt(), maxLevel.toInt()).toList()
    }

    private fun currentLevels(service: AudioEffectService): List<Short> {
        val eq = service.equalizer!!
        return (0 until eq.numberOfBands.toInt()).map { eq.getBandLevel(it.toShort()) }
    }


    @Test
    fun serviceStartsAndAppliesEqualizer() {
        val service = bindService()
        assertNotNull(service)

        val eq = service.equalizer
        assertNotNull(eq)
        assertTrue("Equalizer should be enabled on start", eq!!.enabled)

        val numberOfBands = eq.numberOfBands
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


    @Test
    fun disableEq_turnsOffEqualizerEffect() {
        val service = bindService()
        assertTrue("Should start enabled", service.isEqEnabled)

        service.disableEq()

        assertFalse("isEqEnabled should be false after disableEq()", service.isEqEnabled)
        assertFalse(
            "Equalizer.enabled should be false after disableEq()",
            service.equalizer?.enabled ?: true
        )
    }


    @Test
    fun enableEq_restoresEqualizerAfterDisable() {
        val service = bindService()


        val eq = service.equalizer!!
        val levelsBefore = (0 until eq.numberOfBands).map { eq.getBandLevel(it.toShort()) }

        service.disableEq()
        assertFalse(service.isEqEnabled)

        service.enableEq()

        assertTrue("isEqEnabled should be true after enableEq()", service.isEqEnabled)
        assertTrue(
            "Equalizer.enabled should be true after enableEq()",
            service.equalizer?.enabled ?: false
        )


        val levelsAfter = (0 until eq.numberOfBands).map { eq.getBandLevel(it.toShort()) }
        assertEquals("Band levels should not change on re-enable", levelsBefore, levelsAfter)
    }


    @Test
    fun applyConfig_updatesBandLevels() {
        val service = bindService()
        val eq = service.equalizer!!

        val before = (0 until eq.numberOfBands).map { eq.getBandLevel(it.toShort()) }


        val newConfig = EqConfig(
            baseLevel = 1400,
            multipliers = DoubleArray(5) { 2.0 }
        )
        service.applyConfig(newConfig)

        val after = (0 until service.equalizer!!.numberOfBands)
            .map { service.equalizer!!.getBandLevel(it.toShort()) }

        assertNotEquals(
            "Band levels should change when a new config is applied",
            before, after
        )
    }


    @Test
    fun applyConfig_whileDisabled_doesNotEnableEq() {
        val service = bindService()
        service.disableEq()
        assertFalse(service.isEqEnabled)





        service.applyConfig(EqConfig())


        assertTrue(
            "applyConfig always re-enables the equalizer; Activity must guard calls",
            service.isEqEnabled
        )
    }


    @Test
    fun isEqEnabled_tracksMultipleToggleCycles() {
        val service = bindService()

        repeat(3) {
            service.disableEq()
            assertFalse("Cycle ${it + 1}: should be disabled", service.isEqEnabled)
            service.enableEq()
            assertTrue("Cycle ${it + 1}: should be enabled", service.isEqEnabled)
        }
    }

    @Test
    fun applyConfig_reusesTheSameEqualizerInstance() {
        val service = bindService()
        val first = service.equalizer

        repeat(20) { service.applyConfig(randomConfig(Random(it.toLong()))) }

        assertSame("Equalizer must not be recreated on config change", first, service.equalizer)
    }

    @Test
    fun applyConfig_leavesEqualizerEnabled() {
        val service = bindService()

        service.applyConfig(EqConfig(baseLevel = 1500, multipliers = DoubleArray(5) { 2.0 }))

        assertTrue(service.isEqEnabled)
        assertTrue(service.equalizer!!.enabled)
    }

    @Test
    fun rapidApplyConfig_endsWithLevelsOfLastConfig() {
        val service = bindService()
        val random = Random(123)
        var last = EqConfig()

        repeat(100) {
            last = randomConfig(random)
            service.applyConfig(last)
        }

        assertEquals(expectedLevels(service, last), currentLevels(service))
        assertTrue(service.isEqEnabled)
        assertTrue(service.equalizer!!.enabled)
    }

    @Test
    fun rapidApplyConfig_levelsAlwaysWithinBandRange() {
        val service = bindService()
        val random = Random(99)

        repeat(50) {
            service.applyConfig(randomConfig(random))
            val eq = service.equalizer!!
            val (minLevel, maxLevel) = eq.bandLevelRange
            for (level in currentLevels(service)) {
                assertTrue("Level $level out of range", level in minLevel..maxLevel)
            }
        }
    }

    @Test
    fun concurrentApplyConfig_keepsSingleConsistentEqualizer() {
        val service = bindService()
        val first = service.equalizer
        val threads = 4
        val executor = Executors.newFixedThreadPool(threads)
        val done = CountDownLatch(threads)
        val configs = (0 until threads).map { randomConfig(Random(it.toLong())) }

        for (t in 0 until threads) {
            executor.execute {
                repeat(25) { service.applyConfig(configs[t]) }
                done.countDown()
            }
        }

        assertTrue(done.await(30, TimeUnit.SECONDS))
        executor.shutdown()

        assertSame(first, service.equalizer)
        assertTrue(service.isEqEnabled)
        assertTrue(service.equalizer!!.enabled)
        val levels = currentLevels(service)
        assertTrue(
            "Final levels must match exactly one of the applied configs",
            configs.any { expectedLevels(service, it) == levels }
        )
    }
}
