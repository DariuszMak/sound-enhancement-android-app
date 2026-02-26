package com.example.soundenchancement

import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer

class RealBassBoost(sessionId: Int) : IBassBoost {

    private val bass = BassBoost(0, sessionId)

    override val roundedStrength: Int
        get() = bass.roundedStrength.toInt()

    override var enabled: Boolean
        get() = bass.enabled
        set(value) { bass.enabled = value }

    override fun release() {
        bass.release()
    }
}

class RealLoudnessEnhancer(sessionId: Int) : ILoudnessEnhancer {

    private val loudness = LoudnessEnhancer(sessionId)

    override var targetGain: Int
        get() = loudness.targetGain.toInt()
        set(value) { loudness.setTargetGain(value) }

    override var enabled: Boolean
        get() = loudness.enabled
        set(value) { loudness.enabled = value }

    override fun release() {
        loudness.release()
    }
}