package com.example

import com.example.model.BowlingSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BowlingSpeedUnitTest {

    @Test
    fun testPaceCategorization() {
        assertEquals("EXPRESS FAST ⚡", BowlingSpeed.categorizePace(148.5f))
        assertEquals("FAST BOWLER 💨", BowlingSpeed.categorizePace(138.2f))
        assertEquals("FAST-MEDIUM 🎯", BowlingSpeed.categorizePace(122.0f))
        assertEquals("MEDIUM PACER 🏏", BowlingSpeed.categorizePace(106.4f))
        assertEquals("SPIN / FLIGHT 🔄", BowlingSpeed.categorizePace(88.0f))
    }

    @Test
    fun testBowlingSpeedFormatting() {
        val speed = BowlingSpeed(
            currentKmh = 143.8f,
            currentMph = 89.4f,
            releaseKmh = 143.8f,
            bounceKmh = 124.2f,
            impactKmh = 118.5f,
            paceCategory = "FAST BOWLER",
            isLive = true
        )

        assertEquals("143.8", speed.formattedCurrentKmh())
        assertEquals("89.4", speed.formattedCurrentMph())
        assertEquals("143.8", speed.formattedReleaseKmh())
        assertEquals("124.2", speed.formattedBounceKmh())
        assertEquals("118.5", speed.formattedImpactKmh())
        assertTrue(speed.isLive)
    }

    @Test
    fun testBowlingSpeedDefaultState() {
        val defaultSpeed = BowlingSpeed.default()
        assertFalse(defaultSpeed.isLive)
        assertEquals(141.2f, defaultSpeed.releaseKmh, 0.01f)
        assertEquals(124.8f, defaultSpeed.bounceKmh, 0.01f)
        assertEquals(118.5f, defaultSpeed.impactKmh, 0.01f)
    }
}
