package com.example.soundenchancement

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton = findViewById<Button>(R.id.btnStart)
        val stopButton = findViewById<Button>(R.id.btnStop)

        startButton.setOnClickListener {
            startService(Intent(this, AudioEffectService::class.java))
        }

        stopButton.setOnClickListener {
            stopService(Intent(this, AudioEffectService::class.java))
        }
    }
}