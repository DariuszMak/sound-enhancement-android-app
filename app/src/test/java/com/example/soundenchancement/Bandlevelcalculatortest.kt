package com.example.soundenhancement

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random


class BandLevelCalculatorTest {


    private val minLevel = -1500
    private val maxLevel = 1500
    private val baseLevel = 700
    private val bandFrequencies = doubleArrayOf(60.0, 230.0, 910.0, 3600.0, 14000.0)

    @Test
    fun result_neverExceedsMaxLevel() {
        val testFrequencies =
            listOf(30.0, 60.0, 120.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
        for (freq in testFrequencies) {
            val level = calculateBandLevel(freq, baseLevel, minLevel, maxLevel)
            assertTrue("Band at ${freq}Hz exceeded maxLevel: $level", level <= maxLevel)
        }
    }

    @Test
    fun result_neverBelowMinLevel() {
        val testFrequencies =
            listOf(30.0, 60.0, 120.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
        for (freq in testFrequencies) {
            val level = calculateBandLevel(freq, baseLevel, minLevel, maxLevel)
            assertTrue("Band at ${freq}Hz was below minLevel: $level", level >= minLevel)
        }
    }

    @Test
    fun subBass_strongerThan_midrange() {
        val subBass = calculateBandLevel(40.0, baseLevel, minLevel, maxLevel)
        val midrange = calculateBandLevel(1000.0, baseLevel, minLevel, maxLevel)
        assertTrue(
            "Sub-bass (40 Hz) should be louder than midrange (1 kHz): $subBass vs $midrange",
            subBass > midrange
        )
    }

    @Test
    fun bass_strongerThan_midrange() {
        val bass = calculateBandLevel(80.0, baseLevel, minLevel, maxLevel)
        val midrange = calculateBandLevel(500.0, baseLevel, minLevel, maxLevel)
        assertTrue(
            "Bass (80 Hz) should be louder than midrange (500 Hz): $bass vs $midrange",
            bass > midrange
        )
    }

    @Test
    fun exactBoundary_60Hz_usesSubBassMultiplier() {

        val at60 = calculateBandLevel(60.0, baseLevel, minLevel, maxLevel)
        val at61 = calculateBandLevel(61.0, baseLevel, minLevel, maxLevel)
        assertTrue(
            "60 Hz (sub-bass branch) should be >= 61 Hz (bass branch): $at60 vs $at61",
            at60 >= at61
        )
    }

    @Test
    fun zeroBaseLevel_returnsMinLevel() {

        val level = calculateBandLevel(1000.0, 0, minLevel, maxLevel)
        assertEquals("Zero baseLevel should yield minLevel", minLevel.toShort(), level)
    }

    @Test
    fun symmetricRange_minEqualsNegativeMax() {

        val level = calculateBandLevel(60.0, baseLevel, -1500, 1500)
        assertTrue(level in -1500..1500)
    }

    @Test
    fun highFreq_strongerThan_midrange() {
        val highFreq = calculateBandLevel(16000.0, baseLevel, minLevel, maxLevel)
        val midrange = calculateBandLevel(1000.0, baseLevel, minLevel, maxLevel)
        assertTrue(
            "High freq (16 kHz) should be louder than midrange (1 kHz): $highFreq vs $midrange",
            highFreq > midrange
        )
    }

    @Test
    fun sameInputs_alwaysReturnSameOutput() {
        val first = calculateBandLevel(440.0, baseLevel, minLevel, maxLevel)
        val second = calculateBandLevel(440.0, baseLevel, minLevel, maxLevel)
        assertEquals("calculateBandLevel must be deterministic", first, second)
    }

    @Test
    fun bass_different_values_for_same_frequency() {
        val midRangeLowerLevel = calculateBandLevel(500.0, baseLevel - 1, minLevel, maxLevel)
        val midRangeHigherLevel = calculateBandLevel(500.0, baseLevel, minLevel, maxLevel)
        assertTrue(
            "Lower mid range shold be more silent higher midrange (500 Hz): $midRangeLowerLevel vs $midRangeHigherLevel",
            midRangeLowerLevel < midRangeHigherLevel
        )
    }

    @Test
    fun calculateBandLevels_returnsOneLevelPerFrequency() {
        val levels = calculateBandLevels(bandFrequencies, EqConfig(), minLevel, maxLevel)
        assertEquals(bandFrequencies.size, levels.size)
    }

    @Test
    fun calculateBandLevels_matchesSingleBandCalculation() {
        val config = EqConfig()
        val levels = calculateBandLevels(bandFrequencies, config, minLevel, maxLevel)
        for (i in bandFrequencies.indices) {
            val expected =
                calculateBandLevel(bandFrequencies[i], config.baseLevel, minLevel, maxLevel, config)
            assertEquals("Band $i mismatch", expected, levels[i])
        }
    }

    @Test
    fun calculateBandLevels_allLevelsStayWithinRangeForRandomConfigs() {
        val random = Random(42)
        repeat(500) {
            val config = EqConfig(
                baseLevel = random.nextInt(1501),
                multipliers = DoubleArray(5) { random.nextInt(201) / 100.0 }
            )
            val levels = calculateBandLevels(bandFrequencies, config, minLevel, maxLevel)
            for (level in levels) {
                assertTrue("Level $level out of range for $config", level in minLevel..maxLevel)
            }
        }
    }

    @Test
    fun calculateBandLevels_resultDoesNotDependOnPreviousCalls() {
        val random = Random(7)
        val target = EqConfig(baseLevel = 900, multipliers = doubleArrayOf(1.8, 0.2, 1.1, 0.4, 1.9))
        val fresh = calculateBandLevels(bandFrequencies, target, minLevel, maxLevel)

        repeat(200) {
            val noise = EqConfig(
                baseLevel = random.nextInt(1501),
                multipliers = DoubleArray(5) { random.nextInt(201) / 100.0 }
            )
            calculateBandLevels(bandFrequencies, noise, minLevel, maxLevel)
        }

        val afterNoise = calculateBandLevels(bandFrequencies, target, minLevel, maxLevel)
        assertArrayEquals(fresh, afterNoise)
    }

    @Test
    fun baseLevelSlider_singleStepNeverCausesLoudnessJump() {
        val multipliers = doubleArrayOf(2.0, 2.0, 2.0, 2.0, 2.0)
        for (freq in bandFrequencies) {
            var previous = calculateBandLevel(
                freq, 0, minLevel, maxLevel, EqConfig(0, multipliers)
            ).toInt()
            for (base in 1..1500) {
                val current = calculateBandLevel(
                    freq, base, minLevel, maxLevel, EqConfig(base, multipliers)
                ).toInt()
                assertTrue(
                    "Jump of ${current - previous} mB at ${freq}Hz base=$base",
                    current >= previous && current - previous <= 20
                )
                previous = current
            }
        }
    }

    @Test
    fun bandSlider_singleStepNeverCausesLoudnessJump() {
        for (band in 0 until 5) {
            val freq = bandFrequencies[band]
            var previous = Int.MIN_VALUE
            for (progress in 0..200) {
                val multipliers = DoubleArray(5) { 1.0 }
                multipliers[band] = progress / 100.0
                val current = calculateBandLevel(
                    freq, 1500, minLevel, maxLevel, EqConfig(1500, multipliers)
                ).toInt()
                if (previous != Int.MIN_VALUE) {
                    assertTrue(
                        "Jump of ${current - previous} mB in band $band progress=$progress",
                        current >= previous && current - previous <= 100
                    )
                }
                previous = current
            }
        }
    }
}
