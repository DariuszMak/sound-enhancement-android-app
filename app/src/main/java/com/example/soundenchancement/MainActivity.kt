package com.example.soundenchancement

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var isBassActive = false

    // ── Service binding ───────────────────────────────────────────────────────
    private var audioService: AudioEffectService? = null
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

    // ── Slider references ─────────────────────────────────────────────────────
    private lateinit var sliderBaseLevel: SeekBar
    private lateinit var labelBaseLevel: TextView

    private val bandSliders   = arrayOfNulls<SeekBar>(8)
    private val bandLabels    = arrayOfNulls<TextView>(8)

    /** Frequency-bucket names shown next to each slider. */
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

    /** Default multipliers — identical to the original hard-coded values. */
    private val defaultMultipliers = doubleArrayOf(1.10, 0.90, 0.70, 0.40, 0.40, 0.45, 0.70, 0.80)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ── Status widgets ────────────────────────────────────────────────────
        val startButton = findViewById<Button>(R.id.btnStart)
        val stopButton  = findViewById<Button>(R.id.btnStop)
        val statusDot   = findViewById<TextView>(R.id.statusDot)
        val statusLabel = findViewById<TextView>(R.id.statusLabel)

        fun updateStatus() {
            if (isBassActive) {
                statusDot.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
                statusLabel.text = "Sound effect: ON"
            } else {
                statusDot.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                statusLabel.text = "Sound effect: OFF"
            }
        }

        // Start service immediately (same as before)
        startService(Intent(this, AudioEffectService::class.java))
        isBassActive = true
        updateStatus()

        startButton.setOnClickListener {
            startService(Intent(this, AudioEffectService::class.java))
            isBassActive = true
            updateStatus()
        }

        stopButton.setOnClickListener {
            stopService(Intent(this, AudioEffectService::class.java))
            isBassActive = false
            updateStatus()
        }

        // ── Sliders setup ─────────────────────────────────────────────────────
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

        // Live label updates
        sliderBaseLevel.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                labelBaseLevel.text = "Base Level: $progress mB"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        for (i in 0 until 8) {
            val index = i
            bandSliders[i]?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val multiplier = progress / 100.0
                    bandLabels[index]?.text = "${bandNames[index]}: ${"%.2f".format(multiplier)}×"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        // ── Apply button ──────────────────────────────────────────────────────
        findViewById<Button>(R.id.btnApply).setOnClickListener {
            val config = buildConfigFromSliders()
            if (serviceBound) {
                audioService?.applyConfig(config)
            } else {
                // If not bound yet, restart the service; it will pick up defaults
                // on next bind. For a running service, binding is preferred.
                startService(Intent(this, AudioEffectService::class.java))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, AudioEffectService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Reads the current slider positions and constructs an [EqConfig]. */
    private fun buildConfigFromSliders(): EqConfig {
        val baseLevel = sliderBaseLevel.progress
        val multipliers = DoubleArray(8) { i ->
            (bandSliders[i]?.progress ?: (defaultMultipliers[i] * 100).toInt()) / 100.0
        }
        return EqConfig(baseLevel = baseLevel, multipliers = multipliers)
    }
}