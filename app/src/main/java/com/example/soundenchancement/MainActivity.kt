package com.example.soundenchancement

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    // ── State ─────────────────────────────────────────────────────────────────
    internal var isBassActive = false

    // ── Service binding ───────────────────────────────────────────────────────
    internal var audioService: AudioEffectService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            audioService = (binder as AudioEffectService.LocalBinder).getService()
            serviceBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            serviceBound = false
        }
    }

    // ── Widget references (internal for test access) ──────────────────────────
    internal lateinit var statusDot: TextView
    internal lateinit var statusLabel: TextView
    internal lateinit var btnStart: Button
    internal lateinit var btnStop: Button
    internal lateinit var eqPanel: View          // the panel that gets greyed out

    internal lateinit var sliderBaseLevel: SeekBar
    internal lateinit var labelBaseLevel: TextView

    internal val bandSliders = arrayOfNulls<SeekBar>(8)
    internal val bandLabels  = arrayOfNulls<TextView>(8)

    // ── Constants ─────────────────────────────────────────────────────────────
    private val bandNames = arrayOf(
        "≤ 60 Hz (Sub-bass)",
        "≤ 120 Hz (Bass)",
        "≤ 250 Hz (Low-mid)",
        "≤ 500 Hz (Mid)",
        "≤ 2000 Hz (Upper-mid)",
        "≤ 4000 Hz (Presence)",
        "≤ 8000 Hz (Brilliance)",
        "> 8000 Hz (Air)"
    )
    private val defaultMultipliers =
        doubleArrayOf(1.10, 0.90, 0.70, 0.40, 0.40, 0.45, 0.70, 0.80)

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindWidgets()
        setupSliderListeners()

        // Start the service and mark active
        startService(Intent(this, AudioEffectService::class.java))
        isBassActive = true
        refreshUi()

        btnStart.setOnClickListener { onStartClicked() }
        btnStop.setOnClickListener  { onStopClicked()  }
    }

    override fun onStart() {
        super.onStart()
        // Bind WITHOUT auto-create: only connect to an already-running service
        val intent = Intent(this, AudioEffectService::class.java)
        bindService(intent, serviceConnection, 0 /* no BIND_AUTO_CREATE */)
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
            audioService = null
        }
    }

    // ── Button handlers ───────────────────────────────────────────────────────

    internal fun onStartClicked() {
        if (!isBassActive) {
            startService(Intent(this, AudioEffectService::class.java))
            // Re-bind since the service may have been destroyed
            bindService(
                Intent(this, AudioEffectService::class.java),
                serviceConnection,
                BIND_AUTO_CREATE
            )
            isBassActive = true
            // Apply current slider values once the service is running
            audioService?.enableEq() ?: run {
                // Service will apply defaults on onCreate; push slider values after bind
                postApplyOnBind = true
            }
            refreshUi()
        }
    }

    /** Whether to push slider config the moment the service connects. */
    private var postApplyOnBind = false

    internal fun onStopClicked() {
        if (isBassActive) {
            // Disable the EQ effect without stopping the service process.
            // stopService() alone does NOT un-apply the Equalizer — the audio
            // effect lives in the hardware session until enabled = false.
            audioService?.disableEq()
            isBassActive = false
            refreshUi()
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    /**
     * Updates status dot/label colour and enables or disables the EQ panel.
     */
    internal fun refreshUi() {
        if (isBassActive) {
            statusDot.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_green_light))
            statusLabel.text = "Sound effect: ON"
            setEqPanelEnabled(true)
        } else {
            statusDot.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_red_light))
            statusLabel.text = "Sound effect: OFF"
            setEqPanelEnabled(false)
        }
    }

    /** Recursively enables/disables all interactive children inside [eqPanel]. */
    private fun setEqPanelEnabled(enabled: Boolean) {
        eqPanel.alpha = if (enabled) 1f else 0.38f
        setGroupEnabled(eqPanel, enabled)
    }

    private fun setGroupEnabled(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) setGroupEnabled(view.getChildAt(i), enabled)
        }
    }

    // ── Slider wiring ─────────────────────────────────────────────────────────

    private fun bindWidgets() {
        statusDot   = findViewById(R.id.statusDot)
        statusLabel = findViewById(R.id.statusLabel)
        btnStart    = findViewById(R.id.btnStart)
        btnStop     = findViewById(R.id.btnStop)
        eqPanel     = findViewById(R.id.eqPanel)

        sliderBaseLevel = findViewById(R.id.sliderBaseLevel)
        labelBaseLevel  = findViewById(R.id.labelBaseLevel)

        val sliderIds = intArrayOf(
            R.id.sliderBand0, R.id.sliderBand1, R.id.sliderBand2, R.id.sliderBand3,
            R.id.sliderBand4, R.id.sliderBand5, R.id.sliderBand6, R.id.sliderBand7
        )
        val labelIds = intArrayOf(
            R.id.labelBand0, R.id.labelBand1, R.id.labelBand2, R.id.labelBand3,
            R.id.labelBand4, R.id.labelBand5, R.id.labelBand6, R.id.labelBand7
        )
        for (i in 0 until 8) {
            bandSliders[i] = findViewById(sliderIds[i])
            bandLabels[i]  = findViewById(labelIds[i])
        }
    }

    private fun setupSliderListeners() {
        // Base level slider
        sliderBaseLevel.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                labelBaseLevel.text = "Base Level: $progress mB"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            /** Apply immediately when the user lifts their finger. */
            override fun onStopTrackingTouch(sb: SeekBar?) {
                if (isBassActive) audioService?.applyConfig(buildConfigFromSliders())
            }
        })

        // Per-band sliders
        for (i in 0 until 8) {
            val index = i
            bandSliders[i]?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val multiplier = progress / 100.0
                    bandLabels[index]?.text =
                        "${bandNames[index]}: ${"%.2f".format(multiplier)}×"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                /** Apply immediately when the user lifts their finger. */
                override fun onStopTrackingTouch(sb: SeekBar?) {
                    if (isBassActive) audioService?.applyConfig(buildConfigFromSliders())
                }
            })
        }
    }

    // ── Config builder ────────────────────────────────────────────────────────

    /** Reads current slider positions and constructs an [EqConfig]. */
    internal fun buildConfigFromSliders(): EqConfig {
        val baseLevel = sliderBaseLevel.progress
        val multipliers = DoubleArray(8) { i ->
            (bandSliders[i]?.progress
                ?: (defaultMultipliers[i] * 100).toInt()) / 100.0
        }
        return EqConfig(baseLevel = baseLevel, multipliers = multipliers)
    }
}