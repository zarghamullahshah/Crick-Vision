package com.example.rules

import androidx.compose.ui.geometry.Offset
import com.example.inference.BallTracker
import com.example.model.Decision
import com.example.model.DecisionType
import kotlin.math.abs

/**
 * Heuristic assistant for Leg-Before-Wicket (LBW) appeals.
 *
 * NOTE: This is a single-camera optical heuristic, not multi-camera Hawk-Eye physics.
 * Impact point is approximated from batsman's front-leg knee/ankle pose landmarks.
 * An explicit 0.85 penalty factor is applied to confidence to guard against incorrect calls.
 */
class LbwHeuristic(
    private val lineMargin: Float = 0.15f,     // Normalized width around stump line
    private val stumpHalfWidth: Float = 0.045f // Normalized half-width of the 3 stumps
) {
    fun evaluate(
        pitchPoint: Offset,
        estimatedImpactPoint: Offset,
        stumpBase: Offset,
        tracker: BallTracker,
        frameIndex: Int
    ): Decision {
        // 1. Pitching in line check
        val distPitchFromStumpLine = abs(pitchPoint.x - stumpBase.x)
        val isPitchingInLine = distPitchFromStumpLine <= lineMargin
        val pitchScore = (1f - (distPitchFromStumpLine / lineMargin)).coerceIn(0f, 1f)

        // 2. Impact in line check
        val distImpactFromStumpLine = abs(estimatedImpactPoint.x - stumpBase.x)
        val isImpactInLine = distImpactFromStumpLine <= lineMargin
        val impactScore = (1f - (distImpactFromStumpLine / lineMargin)).coerceIn(0f, 1f)

        // 3. Hitting stumps check (Kalman straight-line projection to stumpBase.y)
        val projectedStumpX = tracker.projectXAtY(stumpBase.y)
        val distProjectedFromStumpCenter = abs(projectedStumpX - stumpBase.x)
        val isHittingStumps = distProjectedFromStumpCenter <= stumpHalfWidth
        val hittingScore = (1f - (distProjectedFromStumpCenter / (stumpHalfWidth * 2f))).coerceIn(0f, 1f)

        // 4. Compute confidence with 0.85 penalty for pose-based impact approximation
        val rawConfidence = (pitchScore + impactScore + hittingScore) / 3f
        val computedConfidence = rawConfidence * 0.85f

        val conditionsPass = isPitchingInLine && isImpactInLine && isHittingStumps

        val confidenceThreshold = 0.70f

        val (decisionType, englishReason, urduReason) = when {
            conditionsPass && computedConfidence >= confidenceThreshold -> {
                Triple(
                    DecisionType.LBW_OUT,
                    "OUT! Pitching in-line, impact in-line, and projected to hit stumps (Impact estimated from pose)",
                    "آؤٹ! گیند لائن میں گری، پیڈ سے رابطہ لائن میں ہوا، اور وکٹوں میں لگ رہی ہے"
                )
            }
            !conditionsPass && computedConfidence >= 0.65f -> {
                val failReason = when {
                    !isPitchingInLine -> "Pitching outside line (${if (pitchPoint.x < stumpBase.x) "leg" else "off"} side)"
                    !isImpactInLine -> "Impact outside line (${if (estimatedImpactPoint.x < stumpBase.x) "leg" else "off"} side)"
                    else -> "Missing stumps on trajectory projection"
                }
                val failUrdu = when {
                    !isPitchingInLine -> "گیند اسٹمپ لائن سے باہر پچ ہوئی"
                    !isImpactInLine -> "پیڈ کا رابطہ لائن سے باہر ہوا"
                    else -> "گیند اسٹمپس سے باہر نکل رہی ہے"
                }
                Triple(
                    DecisionType.LBW_NOT_OUT,
                    "NOT OUT. $failReason",
                    "ناٹ آؤٹ۔ $failUrdu"
                )
            }
            else -> {
                val detail = buildString {
                    append("Pitch: ").append(if (isPitchingInLine) "In-line" else "Outside")
                    append(", Impact: ").append(if (isImpactInLine) "In-line" else "Outside")
                    append(", Stumps: ").append(if (isHittingStumps) "Hitting" else "Missing")
                }
                Triple(
                    DecisionType.UMPIRE_REVIEW,
                    "UMPIRE REVIEW. Insufficient margin for conclusive automated call ($detail)",
                    "امپائر کا جائزہ۔ حتمی فیصلے کے لیے پیمائش غیر یقینی ہے"
                )
            }
        }

        return Decision(
            type = decisionType,
            confidence = computedConfidence,
            frameIndex = frameIndex,
            reason = englishReason,
            urduReason = urduReason
        )
    }
}
