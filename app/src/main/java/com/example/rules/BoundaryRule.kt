package com.example.rules

import androidx.compose.ui.geometry.Offset
import com.example.model.Decision
import com.example.model.DecisionType

/**
 * Evaluates whether a hit ball reached or crossed the boundary line,
 * determining whether it is a FOUR, SIX, or requires UMPIRE_REVIEW.
 */
class BoundaryRule(
    private var boundaryLine: List<Offset> // Usually far corners of pitch/boundary [left, right]
) {
    private var hasFiredThisDelivery = false

    fun updateBoundaryLine(newLine: List<Offset>) {
        if (newLine.size >= 2) {
            boundaryLine = newLine
        }
    }

    fun resetForNewDelivery() {
        hasFiredThisDelivery = false
    }

    fun evaluate(
        ballTrajectory: List<Offset>,
        wasAirborne: Boolean,
        frameIndex: Int
    ): Decision? {
        if (hasFiredThisDelivery || ballTrajectory.size < 3 || boundaryLine.size < 2) return null

        val lastPos = ballTrajectory.last()
        val boundaryY = (boundaryLine[0].y + boundaryLine[1].y) / 2f
        val boundaryMinX = minOf(boundaryLine[0].x, boundaryLine[1].x)
        val boundaryMaxX = maxOf(boundaryLine[0].x, boundaryLine[1].x)

        // Check if ball crossed the boundary edge
        val hasCrossed = lastPos.y <= boundaryY && (lastPos.x in (boundaryMinX - 0.1f)..(boundaryMaxX + 0.1f))

        if (hasCrossed) {
            hasFiredThisDelivery = true
            return if (wasAirborne) {
                Decision(
                    type = DecisionType.SIX,
                    confidence = 0.80f,
                    frameIndex = frameIndex,
                    reason = "Ball crossed boundary on the full without touching the ground",
                    urduReason = "گیند زمین کو چھوئے بغیر باؤنڈری پار کر گئی (چھکا)"
                )
            } else {
                Decision(
                    type = DecisionType.FOUR,
                    confidence = 0.80f,
                    frameIndex = frameIndex,
                    reason = "Ball crossed boundary along the ground after pitching",
                    urduReason = "گیند ٹپہ کھا کر زمین کے ساتھ باؤنڈری پار کر گئی (چوکا)"
                )
            }
        }

        // Ball disappeared near boundary edge
        if (lastPos.y in (boundaryY - 0.05f)..(boundaryY + 0.05f) && ballTrajectory.size >= 8) {
            hasFiredThisDelivery = true
            return Decision(
                type = DecisionType.UMPIRE_REVIEW,
                confidence = 0.40f,
                frameIndex = frameIndex,
                reason = "Ball left frame near boundary line, boundary crossing could not be confirmed",
                urduReason = "گیند باؤنڈری لائن کے قریب فریم سے غائب ہو گئی، تصدیق ممکن نہیں"
            )
        }

        return null
    }
}
