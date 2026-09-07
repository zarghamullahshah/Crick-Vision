package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import com.example.model.PitchCalibration
import com.example.vision.PitchPhysicsConverter
import com.example.vision.RealTimeVisionAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VisionEngineUnitTest {

    @Test
    fun testPhysicalPitchConversionAndSpeedCalculation() {
        val calibration = PitchCalibration.default()

        // Point at bowler crease (progress 0.0) -> ~0.0 meters from crease
        val (_, yBowler) = PitchPhysicsConverter.normalizedToRealWorldMeters(
            calibration.pitchCorners[0],
            calibration
        )
        assertTrue("Bowler crease Y should be near 0m", yBowler <= 2.0f)

        // Point at batsman stumps (progress ~1.0) -> ~17-20 meters down the pitch
        val (_, yStumps) = PitchPhysicsConverter.normalizedToRealWorldMeters(
            calibration.stumpBase,
            calibration
        )
        assertTrue("Batsman end Y should be between 14m and 22m, was $yStumps", yStumps in 14f..22f)

        // Real speed calculation: ball travelling from crease (0.50f, 0.88f) to stumps (0.50f, 0.28f) in 0.45 seconds
        // In real cricket: 18 meters in 0.45s = 40 m/s = 144 km/h
        val speedKmh = PitchPhysicsConverter.calculateRealSpeedKmh(
            pos1 = Offset(0.50f, 0.88f),
            pos2 = Offset(0.50f, 0.28f),
            timeDeltaSec = 0.45f,
            calibration = calibration
        )
        assertTrue("Calculated speed should be fast bowling velocity (120-160 km/h), was $speedKmh", speedKmh in 120f..160f)
    }

    @Test
    fun testRealTimeVisionMotionDetectionOnFrames() {
        val calibration = PitchCalibration.default()
        val analyzer = RealTimeVisionAnalyzer(calibration)

        // Create Frame 1: Blank green pitch background
        val bitmap1 = Bitmap.createBitmap(120, 90, Bitmap.Config.ARGB_8888)
        for (y in 0 until 90) {
            for (x in 0 until 120) {
                bitmap1.setPixel(x, y, Color.rgb(50, 120, 50))
            }
        }

        val result1 = analyzer.analyzeVideoFrame(bitmap1, timestampMs = 0L)
        // First frame establishes baseline background
        assertEquals(0, result1.trajectory.size)

        // Create Frame 2: Ball moves to position (40, 50) - high contrast bright white ball
        val bitmap2 = Bitmap.createBitmap(120, 90, Bitmap.Config.ARGB_8888)
        for (y in 0 until 90) {
            for (x in 0 until 120) {
                bitmap2.setPixel(x, y, Color.rgb(50, 120, 50))
            }
        }
        for (dy in -4..4) {
            for (dx in -4..4) {
                bitmap2.setPixel(40 + dx, 50 + dy, Color.WHITE)
            }
        }

        val result2 = analyzer.analyzeVideoFrame(bitmap2, timestampMs = 50L)
        assertNotNull("Should detect motion in second frame", result2.detection)
        assertTrue("Trajectory should record moving ball", result2.trajectory.isNotEmpty())

        // Create Frame 3: Ball moves to position (40, 30)
        val bitmap3 = Bitmap.createBitmap(120, 90, Bitmap.Config.ARGB_8888)
        for (y in 0 until 90) {
            for (x in 0 until 120) {
                bitmap3.setPixel(x, y, Color.rgb(50, 120, 50))
            }
        }
        for (dy in -4..4) {
            for (dx in -4..4) {
                bitmap3.setPixel(40 + dx, 30 + dy, Color.WHITE)
            }
        }

        val result3 = analyzer.analyzeVideoFrame(bitmap3, timestampMs = 100L)
        assertTrue("Should calculate physical speed between frames", result3.speed.currentKmh > 0f)
        assertTrue("Pace category should be populated", result3.speed.paceCategory.isNotBlank())
    }

    @Test
    fun testBowlerFootCreaseGeometricOverstepAndLegalDetection() {
        val calibration = PitchCalibration.default()
        val detector = com.example.vision.BowlerFootCreaseDetector(
            poppingCrease = calibration.poppingCrease,
            stumpBase = calibration.stumpBase
        )

        val creaseY = calibration.poppingCrease[0].y

        // 1. Bowler lands clearly behind popping crease (Y > creaseY in camera space)
        val legalPoint = Offset(0.48f, creaseY + 0.05f)
        val legalResult = detector.evaluatePoint(legalPoint, frameIndex = 1)
        org.junit.Assert.assertFalse("Foot behind crease must be legal", legalResult.isOverstep)
        assertTrue("Signed distance should be positive (safe margin)", legalResult.signedDistanceCm > 0f)
        org.junit.Assert.assertNull("No decision for legal delivery", legalResult.decision)

        // 2. Bowler oversteps across the popping crease towards the batsman
        val overstepPoint = Offset(0.48f, creaseY - 0.04f)
        val overstepResult = detector.evaluatePoint(overstepPoint, frameIndex = 2)
        assertTrue("Foot past crease must trigger overstep", overstepResult.isOverstep)
        assertTrue("Signed distance should be negative (breach margin)", overstepResult.signedDistanceCm < 0f)
        assertNotNull("Decision must be triggered on overstep", overstepResult.decision)
        assertEquals(com.example.model.DecisionType.OVERSTEP, overstepResult.decision?.type)
        assertEquals("OVERSTEP", overstepResult.decision?.type?.displayName)
        assertTrue("Reason should contain distance in cm", overstepResult.decision?.reason?.contains("cm") == true)
    }

    @Test
    fun testBowlerFootDetectionFromLuminanceGrid() {
        val calibration = PitchCalibration.default()
        val detector = com.example.vision.BowlerFootCreaseDetector(
            poppingCrease = calibration.poppingCrease,
            stumpBase = calibration.stumpBase
        )

        val gridW = 120
        val gridH = 90
        val grid = ByteArray(gridW * gridH) { 60.toByte() } // Grass luminance ~60

        // Create bright white shoe marking on luminance grid across popping crease
        val creaseRow = (calibration.poppingCrease[0].y * gridH).toInt()
        val footRow = creaseRow - 3 // Stepping over crease
        val footCol = 60

        for (r in (footRow - 2)..(footRow + 2)) {
            for (c in (footCol - 3)..(footCol + 3)) {
                if (r in 0 until gridH && c in 0 until gridW) {
                    grid[r * gridW + c] = 230.toByte() // Bright white shoe
                }
            }
        }

        val result = detector.detectFootInFrame(
            currentGrid = grid,
            prevGrid = null,
            gridWidth = gridW,
            gridHeight = gridH,
            frameIndex = 5
        )
        assertNotNull("Foot detector should extract foot candidate from luminance grid", result.footContact)
        assertTrue("Detected shoe crossing crease should trigger overstep", result.isOverstep)
        assertEquals(com.example.model.DecisionType.OVERSTEP, result.decision?.type)
    }
}
