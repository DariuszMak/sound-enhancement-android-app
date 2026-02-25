package com.example.soundenchancement

import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer

open class AudioEffectFactory {

    open fun createLoudnessEnhancer(sessionId: Int): LoudnessEnhancer {
        return LoudnessEnhancer(sessionId)
    }

    open fun createBassBoost(sessionId: Int): BassBoost {
        return BassBoost(0, sessionId)
    }
}