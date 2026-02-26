package com.example.soundenchancement

class FakeBassBoost : IBassBoost {
    override val roundedStrength: Int = 1000
    override var enabled: Boolean = true
    override fun release() {}
}

class FakeLoudnessEnhancer : ILoudnessEnhancer {
    override var targetGain: Int = 1000
    override var enabled: Boolean = true
    override fun release() {}
}