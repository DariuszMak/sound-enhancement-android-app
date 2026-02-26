package com.example.soundenchancement

class FakeBassBoost : IBassBoost {

    override var strength: Short = 1000
    override var enabled: Boolean = true

    override fun release() {}
}