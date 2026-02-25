package com.example.soundenchancement

import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import org.mockito.Mockito

class FakeAudioEffectFactory : AudioEffectFactory() {

    override fun createLoudnessEnhancer(sessionId: Int): LoudnessEnhancer {
        val mock = Mockito.mock(LoudnessEnhancer::class.java)
        Mockito.`when`(mock.targetGain).thenReturn(1000)
        Mockito.`when`(mock.enabled).thenReturn(true)
        return mock
    }

    override fun createBassBoost(sessionId: Int): BassBoost {
        val mock = Mockito.mock(BassBoost::class.java)
        Mockito.`when`(mock.roundedStrength).thenReturn(1000.toShort())
        Mockito.`when`(mock.enabled).thenReturn(true)
        return mock
    }
}