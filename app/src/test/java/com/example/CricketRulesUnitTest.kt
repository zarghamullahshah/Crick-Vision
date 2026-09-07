package com.example

import androidx.compose.ui.geometry.Offset
import com.example.inference.BallTracker
import com.example.model.DecisionType
import com.example.model.Detection
import com.example.rules.BoundaryRule
import com.example.rules.LbwHeuristic
import com.example.rules.NoBallRule
import com.example.rules.WideRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CricketRulesUnitTest {

    @Test
    fun testNoBallCreaseCrossing() {
        val creaseLine = listOf(Offset(0.18f, 0.88f), Offset(0.82f, 0.88f))
        val rule = NoBallRule(creaseLine)

        // Legal delivery behind crease
        val legalDecision = rule.evaluate(Offset(0.50f, 0.92f), frameIndex = 1)
        assertNull("Legal foot position behind crease should return null", legalDecision)

        // Reset for next delivery
        rule.resetForNewDelivery()

        // Overstepping illegal delivery (bowler stepped past 0.88f line)
        val noBallDecision = rule.evaluate(Offset(0.50f, 0.84f), frameIndex = 2)
        assertNotNull("Overstepping should return an OVERSTEP decision", noBallDecision)
        assertEquals(DecisionType.OVERSTEP, noBallDecision?.type)
        assertTrue("Confidence should be high", (noBallDecision?.confidence ?: 0f) >= 0.95f)
    }

    @Test
    fun testWideDeliveryAndBatProximityReview() {
        val wideLine = listOf(Offset(0.36f, 0.28f), Offset(0.64f, 0.28f))
        val rule = WideRule(wideLine)

        // 1. Clear wide with no bat nearby
        val wideBallDetection = Detection(
            label = "ball",
            boundingBox = android.graphics.RectF(0.70f, 0.27f, 0.74f, 0.29f),
            confidence = 0.95f
        )
        val wideDecision = rule.evaluate(
            ballDetection = wideBallDetection,
            battingWrist = Offset(0.50f, 0.28f), // Far from 0.72f ball
            isAtStumpPlane = true,
            frameIndex = 5
        )
        assertNotNull(wideDecision)
        assertEquals(DecisionType.WIDE, wideDecision?.type)

        // 2. Ball near wide line but adjacent to batsman wrist (bat proximity review)
        rule.resetForNewDelivery()
        val edgeBallDetection = Detection(
            label = "ball",
            boundingBox = android.graphics.RectF(0.65f, 0.27f, 0.69f, 0.29f), // Center ~0.67
            confidence = 0.92f
        )
        val reviewDecision = rule.evaluate(
            ballDetection = edgeBallDetection,
            battingWrist = Offset(0.68f, 0.28f), // Within 0.08 threshold
            isAtStumpPlane = true,
            frameIndex = 6
        )
        assertNotNull(reviewDecision)
        assertEquals(DecisionType.UMPIRE_REVIEW, reviewDecision?.type)
    }

    @Test
    fun testLbwHeuristicOutVerdict() {
        val stumpBase = Offset(0.50f, 0.26f)
        val heuristic = LbwHeuristic()

        val tracker = BallTracker()
        tracker.update(Offset(0.50f, 0.70f))
        tracker.update(Offset(0.50f, 0.50f))
        tracker.update(Offset(0.50f, 0.30f))

        val pitchBounce = Offset(0.50f, 0.45f) // Perfectly in line
        val padImpact = Offset(0.50f, 0.28f)   // In line with stumps

        val decision = heuristic.evaluate(
            pitchPoint = pitchBounce,
            estimatedImpactPoint = padImpact,
            stumpBase = stumpBase,
            tracker = tracker,
            frameIndex = 15
        )

        assertEquals(DecisionType.LBW_OUT, decision.type)
        assertTrue("Confidence should be above 0.70 threshold", decision.confidence >= 0.70f)
    }

    @Test
    fun testBallTrackerKalmanPrediction() {
        val tracker = BallTracker()
        tracker.update(Offset(0.50f, 0.80f))
        tracker.update(Offset(0.50f, 0.70f))
        tracker.update(Offset(0.50f, 0.60f))

        // Projecting to target stump line Y = 0.26
        val projectedX = tracker.projectXAtY(0.26f)
        assertEquals(0.50f, projectedX, 0.05f)
    }

    @Test
    fun testBoundaryRuleFourAndSix() {
        val boundaryLine = listOf(Offset(0.32f, 0.28f), Offset(0.68f, 0.28f))
        val rule = BoundaryRule(boundaryLine)

        val trajectory = listOf(
            Offset(0.50f, 0.50f),
            Offset(0.50f, 0.35f),
            Offset(0.50f, 0.24f) // Past 0.28 line
        )

        val sixDecision = rule.evaluate(trajectory, wasAirborne = true, frameIndex = 20)
        assertNotNull(sixDecision)
        assertEquals(DecisionType.SIX, sixDecision?.type)

        rule.resetForNewDelivery()
        val fourDecision = rule.evaluate(trajectory, wasAirborne = false, frameIndex = 22)
        assertNotNull(fourDecision)
        assertEquals(DecisionType.FOUR, fourDecision?.type)
    }
}
