package com.example.soundenchancement

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private var service: AudioEffectService? = null
    private var isBound = false
    private var isEqActive = false

    // (seekBarId, dBLabelId, freqLabelId) — must match activity_main.xml
    private val bandViews = listOf(
        Triple(R.id.band0, R.id.label0, R.id.freq0),
        Triple(R.id.band1, R.id.label1, R.id.freq1),
        Triple(R.id.band2, R.id.label2, R.id.freq2),
        Triple(R.id.band3, R.id.label3, R.id.freq3),
        Triple(R.id.band4, R.id.label4, R.id.freq4),
        Triple(R.id.band5, R.id.label5, R.id.freq5),
        Triple(R.id.band6, R.id.label6, R.id.freq6),
        Triple(R.id.band7, R.id.label7, R.id.freq7),
    )

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as AudioEffectService.LocalBinder).getService()
            isBound = true
            // Service is running and bound — treat EQ as active
            isEqActive = true
            updateStatus()
            populateBands()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            // Start the service (foreground), then bind to it
            val intent = Intent(this, AudioEffectService::class.java)
            startService(intent)
            if (!isBound) {
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            } else {
                // Already bound — just re-enable EQ and refresh UI
                service?.enableEqualizer()
                isEqActive = true
                updateStatus()
                populateBands()
            }
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            service?.disableEqualizer()
            isEqActive = false
            updateStatus()
        }

        updateStatus()
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
            service = null
        }
    }

    private fun populateBands() {
        val svc = service ?: return
        val bandInfo = svc.getBandInfo()           // List<Pair<Int, Int>>  (index, freqHz)
        val (minMb, maxMb) = svc.getBandLevelRange()

        bandViews.forEachIndexed { index, (seekId, labelId, freqId) ->
            // Skip if device has fewer bands than our 8 slots
            val (_, freqHz) = bandInfo.getOrNull(index) ?: return@forEachIndexed

            val seekBar   = findViewById<SeekBar>(seekId)
            val dbLabel   = findViewById<TextView>(labelId)
            val freqLabel = findViewById<TextView>(freqId)

            // Frequency label (e.g. "60 Hz", "1k Hz")
            freqLabel.text = formatFreqHz(freqHz)

            // Set slider to current EQ value — suppress listener during init
            seekBar.setOnSeekBarChangeListener(null)
            val initProgress = svc.getSliderValueForBand(index)
            seekBar.progress = initProgress
            dbLabel.text = progressToDb(initProgress, minMb, maxMb)

            // Wire live interaction
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    dbLabel.text = progressToDb(progress, minMb, maxMb)
                    // Always apply — EQ object is always present when bound
                    service?.setBandFromSlider(index, progress)
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }
    }

    /**
     * Maps 0–100 slider progress to a dB string using the device's actual mB range.
     * e.g. range -1500..1500 mB → -15.0..+15.0 dB
     */
    private fun progressToDb(progress: Int, minMb: Short, maxMb: Short): String {
        val mb = minMb + (progress / 100f) * (maxMb - minMb)
        val db = mb / 100f
        val sign = if (db >= 0f) "+" else ""
        return "$sign${db.roundToInt()} dB"
    }

    /**
     * Format Hz value for display: below 1000 → "60 Hz", above → "1k Hz"
     */
    private fun formatFreqHz(hz: Int): String {
        return if (hz >= 1000) "${hz / 1000}k" else "$hz"
    }

    private fun updateStatus() {
        val dot   = findViewById<TextView>(R.id.statusDot)
        val label = findViewById<TextView>(R.id.statusLabel)
        if (isEqActive) {
            dot.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            label.text = "ON"
            label.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
        } else {
            dot.setTextColor(0xFF444455.toInt())
            label.text = "OFF"
            label.setTextColor(0xFF888888.toInt())
        }
    }
}