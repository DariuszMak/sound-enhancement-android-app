package com.example.soundenchancement

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    // Clear saved prefs before every test so each starts from a known state.
    @Before
    fun clearPrefs() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("eq_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @After
    fun clearPrefsAfter() = clearPrefs()

    // ── Original launch tests ─────────────────────────────────────────────────

    @Test
    fun onLaunch_statusShouldShowOn() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val statusLabel = activity.findViewById<TextView>(R.id.statusLabel)
                assertEquals("Sound effect: ON", statusLabel.text.toString())
            }
        }
    }

    @Test
    fun onLaunch_stopButtonShouldTurnOffStatus() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnStop.performClick()
                assertEquals("Sound effect: OFF", activity.statusLabel.text.toString())
            }
        }
    }

    // ── EQ panel state ────────────────────────────────────────────────────────

    @Test
    fun onLaunch_eqPanelIsEnabled() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(1f, activity.eqPanel.alpha, 0.01f)
                assertTrue(activity.sliderBaseLevel.isEnabled)
            }
        }
    }

    @Test
    fun stopButton_greysOutEqPanel() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnStop.performClick()
                assertEquals(0.38f, activity.eqPanel.alpha, 0.01f)
            }
        }
    }

    @Test
    fun stopButton_disablesSlidersAndLabels() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnStop.performClick()
                assertFalse(activity.sliderBaseLevel.isEnabled)
                for (i in 0 until 8) {
                    assertFalse(
                        "Band slider $i should be disabled",
                        activity.bandSliders[i]?.isEnabled ?: true
                    )
                }
            }
        }
    }

    @Test
    fun startButton_reEnablesEqPanel() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnStop.performClick()
                activity.btnStart.performClick()

                assertTrue(activity.isBassActive)
                assertEquals(1f, activity.eqPanel.alpha, 0.01f)
                assertTrue(activity.sliderBaseLevel.isEnabled)
            }
        }
    }

    // ── buildConfigFromSliders ────────────────────────────────────────────────

    @Test
    fun buildConfigFromSliders_returnsDefaultConfig_onFreshLaunch() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val config = activity.buildConfigFromSliders()
                assertEquals(700, config.baseLevel)
                assertEquals(1.10, config.multipliers[0], 0.01)
                assertEquals(0.90, config.multipliers[1], 0.01)
                assertEquals(0.70, config.multipliers[2], 0.01)
                assertEquals(0.40, config.multipliers[3], 0.01)
                assertEquals(0.40, config.multipliers[4], 0.01)
                assertEquals(0.45, config.multipliers[5], 0.01)
                assertEquals(0.70, config.multipliers[6], 0.01)
                assertEquals(0.80, config.multipliers[7], 0.01)
            }
        }
    }

    @Test
    fun buildConfigFromSliders_reflectsChangedSliderPosition() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.sliderBaseLevel.progress = 1000
                activity.bandSliders[0]?.progress = 150

                val config = activity.buildConfigFromSliders()
                assertEquals(1000, config.baseLevel)
                assertEquals(1.50, config.multipliers[0], 0.01)
            }
        }
    }

    // ── Label updates ─────────────────────────────────────────────────────────

    @Test
    fun baseLevelSlider_updatesLabelOnChange() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.sliderBaseLevel.progress = 1200
                assertTrue(activity.labelBaseLevel.text.contains("1200"))
            }
        }
    }

    @Test
    fun bandSlider_updatesLabelOnChange() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.bandSliders[2]?.progress = 130
                val label = activity.bandLabels[2]?.text.toString()
                assertTrue("Expected '1.30' in label, got: $label", label.contains("1.30"))
            }
        }
    }

    // ── isBassActive tracking ─────────────────────────────────────────────────

    @Test
    fun isBassActive_isTrueOnFreshLaunch() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.isBassActive)
            }
        }
    }

    @Test
    fun isBassActive_isFalseAfterStop_TrueAfterStart() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnStop.performClick()
                assertFalse(activity.isBassActive)

                activity.btnStart.performClick()
                assertTrue(activity.isBassActive)
            }
        }
    }

    @Test
    fun doubleStop_doesNotChangeState() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnStop.performClick()
                activity.btnStop.performClick()

                assertFalse(activity.isBassActive)
                assertEquals("Sound effect: OFF", activity.statusLabel.text.toString())
            }
        }
    }

    // ── Persistence: slider values survive across Activity instances ──────────

    @Test
    fun sliderValues_areRestoredAfterRelaunch() {
        // First launch — change values
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.sliderBaseLevel.progress = 1100
                activity.bandSliders[0]?.progress = 180
                activity.bandSliders[3]?.progress = 60
                // Simulate finger-up to trigger save
                activity.eqPrefs.saveBaseLevel(1100)
                activity.eqPrefs.saveBandProgress(0, 180)
                activity.eqPrefs.saveBandProgress(3, 60)
            }
        }

        // Second launch — values should be restored
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(
                    "baseLevel slider should be restored to 1100",
                    1100, activity.sliderBaseLevel.progress
                )
                assertEquals(
                    "Band 0 slider should be restored to 180",
                    180, activity.bandSliders[0]?.progress
                )
                assertEquals(
                    "Band 3 slider should be restored to 60",
                    60, activity.bandSliders[3]?.progress
                )
            }
        }
    }

    @Test
    fun sliderLabels_reflectRestoredValues_onRelaunch() {
        // Save custom values directly via prefs
        val prefs = EqPreferences(ApplicationProvider.getApplicationContext())
        prefs.saveBaseLevel(900)
        prefs.saveBandProgress(1, 160)   // → 1.60×

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(
                    "Base level label should show 900, got: ${activity.labelBaseLevel.text}",
                    activity.labelBaseLevel.text.contains("900")
                )
                val band1Label = activity.bandLabels[1]?.text.toString()
                assertTrue(
                    "Band 1 label should show 1.60, got: $band1Label",
                    band1Label.contains("1.60")
                )
            }
        }
    }

    @Test
    fun isActiveState_isRestoredAfterRelaunch_whenOff() {
        // Save OFF state
        val prefs = EqPreferences(ApplicationProvider.getApplicationContext())
        prefs.saveIsActive(false)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(
                    "isBassActive should be false when saved state was OFF",
                    activity.isBassActive
                )
                assertEquals("Sound effect: OFF", activity.statusLabel.text.toString())
                assertEquals(0.38f, activity.eqPanel.alpha, 0.01f)
            }
        }
    }

    @Test
    fun isActiveState_isRestoredAfterRelaunch_whenOn() {
        val prefs = EqPreferences(ApplicationProvider.getApplicationContext())
        prefs.saveIsActive(true)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(
                    "isBassActive should be true when saved state was ON",
                    activity.isBassActive
                )
                assertEquals("Sound effect: ON", activity.statusLabel.text.toString())
                assertEquals(1f, activity.eqPanel.alpha, 0.01f)
            }
        }
    }

    @Test
    fun restoreSliderState_loadsAllNineSlidersFromPrefs() {
        val prefs = EqPreferences(ApplicationProvider.getApplicationContext())
        prefs.saveBaseLevel(300)
        for (i in 0 until 8) prefs.saveBandProgress(i, 50 + i * 10)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(300, activity.sliderBaseLevel.progress)
                for (i in 0 until 8) {
                    assertEquals(
                        "Band $i progress should be ${50 + i * 10}",
                        50 + i * 10,
                        activity.bandSliders[i]?.progress
                    )
                }
            }
        }
    }

    @Test
    fun buildConfigFromSliders_matchesRestoredPrefs() {
        val prefs = EqPreferences(ApplicationProvider.getApplicationContext())
        prefs.saveBaseLevel(500)
        prefs.saveBandProgress(0, 120)   // → 1.20×
        prefs.saveBandProgress(7, 95)    // → 0.95×

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val config = activity.buildConfigFromSliders()
                assertEquals(500, config.baseLevel)
                assertEquals(1.20, config.multipliers[0], 0.01)
                assertEquals(0.95, config.multipliers[7], 0.01)
            }
        }
    }
}