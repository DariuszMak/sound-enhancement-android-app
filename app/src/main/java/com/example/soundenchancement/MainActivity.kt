package com.example.soundenchancement

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var isBassActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton = findViewById<Button>(R.id.btnStart)
        val stopButton = findViewById<Button>(R.id.btnStop)
        val statusDot = findViewById<TextView>(R.id.statusDot)
        val statusLabel = findViewById<TextView>(R.id.statusLabel)

        fun updateStatus() {
            if (isBassActive) {
                statusDot.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
                statusLabel.text = "Bass Enhancement: ON"
            } else {
                statusDot.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                statusLabel.text = "Bass Enhancement: OFF"
            }
        }

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
    }
}