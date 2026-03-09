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

    // Each entry: (seekBarId, dBLabelId, freqLabelId)
    private val bandViews = listOf(
        Triple(R.id.band0,  R.id.label0,  R.id.freq0),
        Triple(R.id.band1,  R.id.label1,  R.id.freq1),
        Triple(R.id.band2,  R.id.label2,  R.id.freq2),
        Triple(R.id.band3,  R.id.label3,  R.id.freq3),
        Triple(R.id.band4,  R.id.label4,  R.id.freq4),
        Triple(R.id.band5,  R.id.label5,  R.id.freq5),
        Triple(R.id.band6,  R.id.label6,  R.id.freq6),
        Triple(R.id.band7,  R.id.label7,  R.id.freq7),
    )

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as AudioEffectService.LocalBinder).getService()
            isBound = true
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

        val btnStart  = findViewById<Button>(R.id.btnStart)
        val btnStop   = findViewById<Button>(R.id.btnStop)

        btnStart.setOnClickListener {
            startService(Intent(this, AudioEffectService::class.java))
            bindToService()
            isEqActive = true
            updateStatus()
        }

        btnStop.setOnClickListener {
            service?.disableEqualizer()
            isEqActive = false
            updateStatus()
        }

        updateStatus()
    }

    override fun onStart() {
        super.onStart()
        // Bind if service is already running
        bindToService()
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun bindToService() {
        val intent = Intent(this, AudioEffectService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun populateBands() {
        val svc = service ?: return
        val bandInfo = svc.getBandInfo()
        val (minMb, maxMb) = svc.getBandLevelRange()

        bandViews.forEachIndexed { index, (seekId, labelId, freqId) ->
            val seekBar  = findViewById<SeekBar>(seekId)
            val dbLabel  = findViewById<TextView>(labelId)
            val freqLabel = findViewById<TextView>(freqId)

            val freqHz = bandInfo.getOrNull(index)?.second ?: return@forEachIndexed

            // Set frequency label
            freqLabel.text = formatFreq(freqHz)

            // Set initial slider position from current EQ value
            val initSlider = svc.getSliderValueForBand(index)
            seekBar.progress = initSlider
            dbLabel.text = sliderToDb(initSlider, minMb, maxMb)

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    dbLabel.text = sliderToDb(progress, minMb, maxMb)
                    if (isEqActive) {
                        service?.setBandFromSlider(index, progress)
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {
                    // Apply even if EQ was off — live preview
                    service?.setBandFromSlider(index, sb.progress)
                }
            })
        }
    }

    private fun sliderToDb(progress: Int, minMb: Short, maxMb: Short): String {
        val db = (minMb + (progress / 100f) * (maxMb - minMb)) / 100f
        val sign = if (db >= 0) "+" else ""
        return "$sign${db.roundToInt()} dB"
    }

    private fun formatFreq(hz: Float): String {
        return if (hz >= 1000f) "${(hz / 1000f).roundToInt()}k" else "${hz.roundToInt()}"
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