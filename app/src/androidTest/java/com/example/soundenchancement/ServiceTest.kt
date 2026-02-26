package com.example.soundenchancement

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioBoostServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    @Before
    fun setup() {
        AudioBoostService.bassBoostFactory = { FakeBassBoost() }
    }

    @After
    fun tearDown() {
        AudioBoostService.bassBoostFactory = { RealBassBoost() }
    }

    @Test
    fun serviceStartsAndEnablesBassBoost() {

        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, AudioBoostService::class.java)

        val binder = serviceRule.bindService(intent)
        val service = (binder as AudioBoostService.LocalBinder).getService()

        assertNotNull(service)

        val bassBoostField =
            AudioBoostService::class.java.getDeclaredField("bassBoost")
        bassBoostField.isAccessible = true

        val bassBoost = bassBoostField.get(service) as IBassBoost?

        assertNotNull(bassBoost)
        assertEquals(1000.toShort(), bassBoost?.strength)
        assertTrue(bassBoost?.enabled ?: false)
    }
}