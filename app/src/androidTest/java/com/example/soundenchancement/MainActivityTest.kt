package com.example.soundenchancement

import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

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

    // ── EQ panel is enabled on launch ────────────────────────────────────────

    @Test
    fun onLaunch_eqPanelIsEnabled() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(
                    "EQ panel should be fully opaque (enabled) on launch",
                    activity.eqPanel.alpha == 1f
                )
                assertTrue(
                    "Sliders should be enabled on launch",
                    activity.sliderBaseLevel.isEnabled
                )
            }
        }
    }

    // ── Stop button greys out the EQ panel ───────────────────────────────────

    @Test
    fun stopButton_greysOutEqPanel() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnStop.performClick()

                assertEquals(
                    "EQ panel alpha should drop to 0.38 when effect is OFF",
                    0.38f, activity.eqPanel.alpha, 0.01f
                )
            }
        }
    }

    // ── Sliders are disabled after stop ──────────────────────────────────────

    @Test
    fun stopButton_disablesSlidersAndLabels() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnStop.performClick()

                assertFalse(
                    "Base-level slider should be disabled when effect is OFF",
                    activity.sliderBaseLevel.isEnabled
                )
                for (i in 0 until 8) {
                    assertFalse(
                        "Band slider $i should be disabled when effect is OFF",
                        activity.bandSliders[i]?.isEnabled ?: true
                    )
                }
            }
        }
    }

    // ── Start button re-enables the EQ panel ─────────────────────────────────

    @Test
    fun startButton_reEnablesEqPanel() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnStop.performClick()
                assertFalse(activity.isBassActive)

                activity.btnStart.performClick()

                assertTrue(activity.isBassActive)
                assertEquals(
                    "EQ panel alpha should return to 1.0 when effect is ON",
                    1f, activity.eqPanel.alpha, 0.01f
                )
                assertTrue(
                    "Base-level slider should be re-enabled",
                    activity.sliderBaseLevel.isEnabled
                )
            }
        }
    }

    // ── buildConfigFromSliders reflects slider positions ─────────────────────

    @Test
    fun buildConfigFromSliders_returnsDefaultConfig_onLaunch() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val config = activity.buildConfigFromSliders()
                assertEquals("Default baseLevel should be 700", 700, config.baseLevel)
                assertEquals("Multiplier[0] should be 1.10", 1.10, config.multipliers[0], 0.01)
                assertEquals("Multiplier[1] should be 0.90", 0.90, config.multipliers[1], 0.01)
                assertEquals("Multiplier[2] should be 0.70", 0.70, config.multipliers[2], 0.01)
                assertEquals("Multiplier[3] should be 0.40", 0.40, config.multipliers[3], 0.01)
                assertEquals("Multiplier[4] should be 0.40", 0.40, config.multipliers[4], 0.01)
                assertEquals("Multiplier[5] should be 0.45", 0.45, config.multipliers[5], 0.01)
                assertEquals("Multiplier[6] should be 0.70", 0.70, config.multipliers[6], 0.01)
                assertEquals("Multiplier[7] should be 0.80", 0.80, config.multipliers[7], 0.01)
            }
        }
    }

    @Test
    fun buildConfigFromSliders_reflectsChangedSliderPosition() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Simulate user moving the base-level slider to 1000
                activity.sliderBaseLevel.progress = 1000
                // Simulate moving band 0 to multiplier 1.50 (progress = 150)
                activity.bandSliders[0]?.progress = 150

                val config = activity.buildConfigFromSliders()
                assertEquals("baseLevel should reflect slider at 1000", 1000, config.baseLevel)
                assertEquals(
                    "multipliers[0] should reflect slider at 150 → 1.50",
                    1.50, config.multipliers[0], 0.01
                )
            }
        }
    }

    // ── Slider label updates live ─────────────────────────────────────────────

    @Test
    fun baseLevelSlider_updatesLabelOnChange() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // SeekBar.setProgress triggers onProgressChanged
                activity.sliderBaseLevel.progress = 1200
                val label = activity.labelBaseLevel.text.toString()
                assertTrue(
                    "Label should contain '1200', got: $label",
                    label.contains("1200")
                )
            }
        }
    }

    @Test
    fun bandSlider_updatesLabelOnChange() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // progress 130 → multiplier 1.30
                activity.bandSliders[2]?.progress = 130
                val label = activity.bandLabels[2]?.text.toString()
                assertTrue(
                    "Label should contain '1.30', got: $label",
                    label.contains("1.30")
                )
            }
        }
    }

    // ── isBassActive tracks button presses correctly ──────────────────────────

    @Test
    fun isBassActive_isTrueOnLaunch() {
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

    // ── Clicking stop twice does not change already-OFF state ─────────────────

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
}