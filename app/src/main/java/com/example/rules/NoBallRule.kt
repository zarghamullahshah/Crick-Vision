package com.example.rules

import androidx.compose.ui.geometry.Offset
import com.example.model.Decision
import com.example.model.DecisionType
import com.example.vision.BowlerFootCreaseDetector
import java.util.Locale
import kotlin.math.abs

/**
 * Evaluates bowler popping crease violation for No-Ball / Overstep calls.
 * Uses the front-foot position tested against the calibrated popping crease line.
 */
class NoBallRule(
    private var creaseLine: List<Offset>, // [creaseLeft, creaseRight]
    private var stumpBase: Offset = Offset(0.50f, 0.28f)
) {
    private var hasFiredThisDelivery = false
    private val detector = BowlerFootCreaseDetector(creaseLine, stumpBase)

    fun updateCreaseLine(newCreaseLine: List<Offset>, newStumpBase: Offset = stumpBase) {
        if (newCreaseLine.size >= 2) {
            creaseLine = newCreaseLine
        }
        stumpBase = newStumpBase
        detector.updateCalibration(creaseLine, stumpBase)
    }

    fun resetForNewDelivery() {
        hasFiredThisDelivery = false
    }

    /**
     * Evaluates front foot position against popping crease line.
     * Triggers an 'Overstep' decision if crossed.
     */
    fun evaluate(frontFootAnkle: Offset, frameIndex: Int): Decision? {
        if (hasFiredThisDelivery || creaseLine.size < 2) return null

        val analysis = detector.evaluatePoint(frontFootAnkle, frameIndex)
        if (analysis.isOverstep) {
            hasFiredThisDelivery = true
            return analysis.decision ?: Decision(
                type = DecisionType.OVERSTEP,
                confidence = 0.98f,
                frameIndex = frameIndex,
                reason = "Overstep detected: Front foot crossed popping crease by ${String.format(Locale.US, "%.1f", abs(analysis.signedDistanceCm))} cm",
                urduReason = "اوور سٹیپ: بولر کا اگلا پاؤں پوپنگ کریز سے ${String.format(Locale.US, "%.1f", abs(analysis.signedDistanceCm))} سینٹی میٹر آگے نکل گیا"
            )
        }

        return null
    }

    fun evaluateWithAnalysis(frontFoot: Offset, frameIndex: Int): BowlerFootCreaseDetector.FootCreaseAnalysis {
        val analysis = detector.evaluatePoint(frontFoot, frameIndex)
        if (analysis.isOverstep && !hasFiredThisDelivery) {
            hasFiredThisDelivery = true
        }
        return analysis
    }
}
