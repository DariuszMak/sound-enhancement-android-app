package com.example.soundenchancement

interface IBassBoost {
    val roundedStrength: Int
    var enabled: Boolean
    fun release()
}

interface ILoudnessEnhancer {
    var targetGain: Int
    var enabled: Boolean
    fun release()
}