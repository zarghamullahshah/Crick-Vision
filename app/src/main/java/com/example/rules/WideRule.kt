package com.example.rules

import androidx.compose.ui.geometry.Offset
import com.example.model.Decision
import com.example.model.DecisionType
import com.example.model.Detection
import kotlin.math.hypot

/**
 * Evaluates Wide deliveries at the batting crease plane.
 * Note: Bat position is approximated using the batsman's wrist landmark from pose detection.
 * Feature is calibrated to lower confidence than physical crease lines as per specs.
 */
class WideRule(
    private var wideLine: List<Offset>, // [wideLeft, wideRight]
    private val batProximityThreshold: Float = 0.08f
) {
    private var hasFiredThisDelivery = false

    fun updateWideLine(newWideLine: List<Offset>) {
        if (newWideLine.size >= 2) {
            wideLine = newWideLine
        }
    }

    fun resetForNewDelivery() {
        hasFiredThisDelivery = false
    }

    /**
     * Evaluates ball detection when at or passing the stump plane.
     */
    fun evaluate(
        ballDetection: Detection,
        battingWrist: Offset?,
        isAtStumpPlane: Boolean,
        frameIndex: Int
    ): Decision? {
        if (hasFiredThisDelivery || !isAtStumpPlane || wideLine.size < 2) return null

        val ballCenter = ballDetection.center
        val minX = minOf(wideLine[0].x, wideLine[1].x)
        val maxX = maxOf(wideLine[0].x, wideLine[1].x)

        val isOutsideLeft = ballCenter.x < minX
        val isOutsideRight = ballCenter.x > maxX

        if (!isOutsideLeft && !isOutsideRight) {
            return null
        }

        // Near the bat proxy check
        if (battingWrist != null) {
            val distToWrist = hypot(ballCenter.x - battingWrist.x, ballCenter.y - battingWrist.y)
            if (distToWrist < batProximityThreshold) {
                hasFiredThisDelivery = true
                return Decision(
                    type = DecisionType.UMPIRE_REVIEW,
                    confidence = 0.40f,
                    frameIndex = frameIndex,
                    reason = "Ball passed outside wide line but near bat's estimated position (contact uncertain)",
                    urduReason = "گیند وائیڈ لائن سے باہر گزری لیکن بلے کے قریب تھی، رابطہ مشکوک ہے"
                )
            }
        }

        hasFiredThisDelivery = true
        return Decision(
            type = DecisionType.WIDE,
            confidence = 0.60f, // Intentionally capped per specification
            frameIndex = frameIndex,
            reason = "Ball passed outside the wide line, no bat contact detected (approximate)",
            urduReason = "گیند وائیڈ لائن سے باہر گزری، بلے سے کوئی رابطہ نہیں پایا گیا"
        )
    }
}
