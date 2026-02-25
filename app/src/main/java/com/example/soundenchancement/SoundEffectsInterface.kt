package com.example.soundenchancement

interface IBassBoost {
    val roundedStrength: Int
    var enabled: Boolean
}

interface ILoudnessEnhancer {
    var targetGain: Int
    var enabled: Boolean
}