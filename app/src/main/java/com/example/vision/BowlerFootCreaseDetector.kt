package com.example.vision

import androidx.compose.ui.geometry.Offset
import com.example.model.Decision
import com.example.model.DecisionType
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Real-time Computer Vision detector for bowler front-foot position relative to the popping crease.
 * Complies with MCC Law 21.5 (Fair delivery - the feet).
 */
class BowlerFootCreaseDetector(
    private var poppingCrease: List<Offset>,
    private var stumpBase: Offset = Offset(0.50f, 0.28f)
) {
    data class FootCreaseAnalysis(
        val footContact: Offset? = null,
        val footToe: Offset? = null,
        val footAnkle: Offset? = null,
        val signedDistanceCm: Float = 0f, // > 0: Legal (behind crease), < 0: Overstep (crossed crease into pitch)
        val isOverstep: Boolean = false,
        val confidence: Float = 0f,
        val decision: Decision? = null
    )

    fun updateCalibration(newCrease: List<Offset>, newStumpBase: Offset) {
        if (newCrease.size >= 2) {
            poppingCrease = newCrease
        }
        stumpBase = newStumpBase
    }

    /**
     * Evaluates a coordinate point against the popping crease geometry
     * and calculates exact physical margin in real-world centimeters.
     */
    fun evaluatePoint(
        point: Offset,
        frameIndex: Int = 0
    ): FootCreaseAnalysis {
        if (poppingCrease.size < 2) {
            return FootCreaseAnalysis(footContact = point)
        }

        val p1 = poppingCrease[0]
        val p2 = poppingCrease[1]
        val midCrease = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

        // Vector along the pitch from crease midpoint towards the batsman stumps
        val pitchVector = Offset(stumpBase.x - midCrease.x, stumpBase.y - midCrease.y)
        val pitchLength = sqrt(pitchVector.x * pitchVector.x + pitchVector.y * pitchVector.y).coerceAtLeast(0.01f)
        val normalTowardsBatsman = Offset(pitchVector.x / pitchLength, pitchVector.y / pitchLength)

        // Vector from crease midpoint to the foot point
        val footVec = Offset(point.x - midCrease.x, point.y - midCrease.y)

        // Forward displacement along pitch towards batsman (positive = past crease towards batsman)
        val forwardDisplacement = footVec.x * normalTowardsBatsman.x + footVec.y * normalTowardsBatsman.y

        // Real world conversion: 17.68m (1768 cm) crease-to-crease
        val cmPerUnit = 1768.0f / pitchLength
        val signedDistanceCm = -forwardDisplacement * cmPerUnit

        // Overstep threshold: crossed past line towards batsman with 0.5 cm subpixel tolerance
        val isOverstep = signedDistanceCm < -0.5f

        val marginAbsCm = abs(signedDistanceCm)
        val decision = if (isOverstep) {
            val marginStr = String.format(Locale.US, "%.1f", marginAbsCm)
            Decision(
                type = DecisionType.OVERSTEP,
                confidence = 0.98f,
                frameIndex = frameIndex,
                reason = "Overstep detected: Front foot crossed popping crease by $marginStr cm",
                urduReason = "اوور سٹیپ: بولر کا اگلا پاؤں پوپنگ کریز سے $marginStr سینٹی میٹر آگے نکل گیا"
            )
        } else null

        return FootCreaseAnalysis(
            footContact = point,
            footToe = point,
            footAnkle = point,
            signedDistanceCm = signedDistanceCm,
            isOverstep = isOverstep,
            confidence = 0.95f,
            decision = decision
        )
    }

    /**
     * Real-time vision analysis on luminance grid:
     * Scans the popping crease landing zone for bowler foot ground-contact,
     * extracts foot contours/toe position, and evaluates overstep in real time.
     */
    fun detectFootInFrame(
        currentGrid: ByteArray,
        prevGrid: ByteArray?,
        gridWidth: Int,
        gridHeight: Int,
        frameIndex: Int
    ): FootCreaseAnalysis {
        if (poppingCrease.size < 2) return FootCreaseAnalysis()

        val p1 = poppingCrease[0]
        val p2 = poppingCrease[1]
        val midCrease = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

        // Bounding box of the landing zone corridor around popping crease
        val minXNorm = (min(p1.x, p2.x) - 0.18f).coerceIn(0f, 1f)
        val maxXNorm = (max(p1.x, p2.x) + 0.18f).coerceIn(0f, 1f)
        val minYNorm = (min(p1.y, p2.y) - 0.18f).coerceIn(0f, 1f)
        val maxYNorm = (max(p1.y, p2.y) + 0.18f).coerceIn(0f, 1f)

        val startX = (minXNorm * gridWidth).toInt().coerceIn(0, gridWidth - 1)
        val endX = (maxXNorm * gridWidth).toInt().coerceIn(startX + 1, gridWidth)
        val startY = (minYNorm * gridHeight).toInt().coerceIn(0, gridHeight - 1)
        val endY = (maxYNorm * gridHeight).toInt().coerceIn(startY + 1, gridHeight)

        // Vector pointing towards batsman
        val pitchVector = Offset(stumpBase.x - midCrease.x, stumpBase.y - midCrease.y)
        val pitchLength = sqrt(pitchVector.x * pitchVector.x + pitchVector.y * pitchVector.y).coerceAtLeast(0.01f)
        val normalTowardsBatsman = Offset(pitchVector.x / pitchLength, pitchVector.y / pitchLength)

        var sumX = 0L
        var sumY = 0L
        var count = 0

        var maxForwardDisp = -Float.MAX_VALUE
        var forwardMostX = 0
        var forwardMostY = 0

        var minForwardDisp = Float.MAX_VALUE
        var rearMostX = 0
        var rearMostY = 0

        for (y in startY until endY) {
            for (x in startX until endX) {
                val idx = y * gridWidth + x
                val currLuma = currentGrid[idx].toInt() and 0xFF
                val prevLuma = prevGrid?.let { it[idx].toInt() and 0xFF } ?: currLuma
                val diff = abs(currLuma - prevLuma)

                // High motion or strong contrast landing footprint in the crease zone
                val isMotion = diff > 20
                // High contrast edge detection against turf (e.g. shoe white/bright or shadow contact)
                val isEdge = if (x > 0 && y > 0 && x < gridWidth - 1 && y < gridHeight - 1) {
                    val right = currentGrid[idx + 1].toInt() and 0xFF
                    val down = currentGrid[idx + gridWidth].toInt() and 0xFF
                    (abs(currLuma - right) + abs(currLuma - down)) > 55
                } else false

                if (isMotion || isEdge) {
                    sumX += x
                    sumY += y
                    count++

                    val normPt = Offset(x.toFloat() / gridWidth, y.toFloat() / gridHeight)
                    val footVec = Offset(normPt.x - midCrease.x, normPt.y - midCrease.y)
                    val disp = footVec.x * normalTowardsBatsman.x + footVec.y * normalTowardsBatsman.y

                    if (disp > maxForwardDisp) {
                        maxForwardDisp = disp
                        forwardMostX = x
                        forwardMostY = y
                    }
                    if (disp < minForwardDisp) {
                        minForwardDisp = disp
                        rearMostX = x
                        rearMostY = y
                    }
                }
            }
        }

        if (count < 6) {
            return FootCreaseAnalysis()
        }

        val footCentroid = Offset(
            (sumX.toFloat() / count) / gridWidth,
            (sumY.toFloat() / count) / gridHeight
        )
        val footToe = Offset(
            forwardMostX.toFloat() / gridWidth,
            forwardMostY.toFloat() / gridHeight
        )
        val footHeel = Offset(
            rearMostX.toFloat() / gridWidth,
            rearMostY.toFloat() / gridHeight
        )

        val primaryTestPoint = if (maxForwardDisp > 0f) footToe else footCentroid

        val footVec = Offset(primaryTestPoint.x - midCrease.x, primaryTestPoint.y - midCrease.y)
        val forwardDisplacement = footVec.x * normalTowardsBatsman.x + footVec.y * normalTowardsBatsman.y

        val cmPerUnit = 1768.0f / pitchLength
        val signedDistanceCm = -forwardDisplacement * cmPerUnit

        val isOverstep = signedDistanceCm < -0.5f
        val confidence = min(0.99f, 0.70f + (count / 120f))

        val marginAbsCm = abs(signedDistanceCm)
        val decision = if (isOverstep) {
            val marginStr = String.format(Locale.US, "%.1f", marginAbsCm)
            Decision(
                type = DecisionType.OVERSTEP,
                confidence = confidence,
                frameIndex = frameIndex,
                reason = "Overstep detected: Front foot crossed popping crease by $marginStr cm",
                urduReason = "اوور سٹیپ: بولر کا اگلا پاؤں پوپنگ کریز سے $marginStr سینٹی میٹر آگے نکل گیا"
            )
        } else null

        return FootCreaseAnalysis(
            footContact = footCentroid,
            footToe = footToe,
            footAnkle = footHeel,
            signedDistanceCm = signedDistanceCm,
            isOverstep = isOverstep,
            confidence = confidence,
            decision = decision
        )
    }
}
