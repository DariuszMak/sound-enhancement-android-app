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

    // ── Persistence ───────────────────────────────────────────────────────────
    internal lateinit var eqPrefs: EqPreferences

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
    internal lateinit var eqPanel: View

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

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        eqPrefs = EqPreferences(this)

        bindWidgets()
        restoreSliderState()      // ← set progress BEFORE attaching listeners
        setupSliderListeners()    //   so onProgressChanged fires with saved values
                                  //   and labels are correct immediately

        isBassActive = eqPrefs.loadIsActive()

        if (isBassActive) {
            startService(Intent(this, AudioEffectService::class.java))
        }
        refreshUi()

        btnStart.setOnClickListener { onStartClicked() }
        btnStop.setOnClickListener  { onStopClicked()  }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, AudioEffectService::class.java),
            serviceConnection,
            0   // no BIND_AUTO_CREATE — only attach to an already-running service
        )
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
            bindService(
                Intent(this, AudioEffectService::class.java),
                serviceConnection,
                BIND_AUTO_CREATE
            )
            isBassActive = true
            eqPrefs.saveIsActive(true)
            audioService?.enableEq()
            refreshUi()
        }
    }

    internal fun onStopClicked() {
        if (isBassActive) {
            audioService?.disableEq()
            isBassActive = false
            eqPrefs.saveIsActive(false)
            refreshUi()
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

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

    /**
     * Pushes saved progress values onto sliders BEFORE listeners are attached.
     * Setting [SeekBar.setProgress] here will later trigger [onProgressChanged]
     * inside [setupSliderListeners], which updates the labels automatically.
     */
    internal fun restoreSliderState() {
        sliderBaseLevel.progress = eqPrefs.loadBaseLevel()
        for (i in 0 until 8) {
            bandSliders[i]?.progress = eqPrefs.loadBandProgress(i)
        }
    }

    private fun setupSliderListeners() {
        sliderBaseLevel.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                labelBaseLevel.text = "Base Level: $progress mB"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                eqPrefs.saveBaseLevel(sliderBaseLevel.progress)
                if (isBassActive) audioService?.applyConfig(buildConfigFromSliders())
            }
        })

        for (i in 0 until 8) {
            val index = i
            bandSliders[i]?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val multiplier = progress / 100.0
                    bandLabels[index]?.text =
                        "${bandNames[index]}: ${"%.2f".format(multiplier)}×"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {
                    eqPrefs.saveBandProgress(index, bandSliders[index]?.progress ?: 0)
                    if (isBassActive) audioService?.applyConfig(buildConfigFromSliders())
                }
            })
        }
    }

    // ── Config builder ────────────────────────────────────────────────────────

    internal fun buildConfigFromSliders(): EqConfig {
        val baseLevel = sliderBaseLevel.progress
        val multipliers = DoubleArray(8) { i ->
            (bandSliders[i]?.progress
                ?: EqPreferences.DEFAULT_BAND_PROGRESS[i]) / 100.0
        }
        return EqConfig(baseLevel = baseLevel, multipliers = multipliers)
    }
}