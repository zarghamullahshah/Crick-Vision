package com.example.vision

import androidx.compose.ui.geometry.Offset
import com.example.model.PitchCalibration
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Real-world physical coordinate and speed transformation based on MCC Laws of Cricket.
 * Standard dimensions:
 * - Pitch length (Stumps to Stumps): 20.12 meters (22 yards)
 * - Popping crease to popping crease: 17.68 meters (58 feet)
 * - Pitch width: 3.05 meters (10 feet)
 * - Wide guideline distance from stumps: 0.89 meters
 */
object PitchPhysicsConverter {

    const val PITCH_LENGTH_METERS = 20.12f
    const val CREASE_TO_CREASE_METERS = 17.68f
    const val PITCH_WIDTH_METERS = 3.05f
    const val WIDE_GUIDELINE_METERS = 0.89f

    /**
     * Estimates real-world pitch coordinates (X in meters from pitch center, Y in meters from bowler crease)
     * using the 4 calibrated pitch corners.
     */
    fun normalizedToRealWorldMeters(
        point: Offset,
        calibration: PitchCalibration
    ): Pair<Float, Float> {
        val corners = calibration.pitchCorners
        if (corners.size < 4) {
            // Fallback default scaling
            val yMeters = (1f - point.y) * PITCH_LENGTH_METERS
            val xMeters = (point.x - 0.5f) * PITCH_WIDTH_METERS
            return Pair(xMeters, yMeters)
        }

        // corners[0] = Top-Left (Bowler End), corners[1] = Top-Right (Bowler End)
        // corners[2] = Bottom-Right (Batter End), corners[3] = Bottom-Left (Batter End)
        val bowlerY = (corners[0].y + corners[1].y) / 2f
        val batsmanY = (corners[2].y + corners[3].y) / 2f
        val pitchScreenHeight = max(0.01f, kotlin.math.abs(batsmanY - bowlerY))

        // Fractional progress along the pitch length (0.0 at bowler crease, 1.0 at batsman crease)
        val progressY = if (bowlerY > batsmanY) {
            ((bowlerY - point.y) / pitchScreenHeight).coerceIn(-0.2f, 1.3f)
        } else {
            ((point.y - bowlerY) / pitchScreenHeight).coerceIn(-0.2f, 1.3f)
        }
        val realYMeters = progressY * CREASE_TO_CREASE_METERS

        // Screen width at this specific Y due to perspective convergence
        val topWidth = max(0.01f, corners[1].x - corners[0].x)
        val bottomWidth = max(0.01f, corners[2].x - corners[3].x)
        val widthAtY = topWidth + (bottomWidth - topWidth) * progressY.coerceIn(0f, 1f)
        val centerXAtY = (corners[0].x + corners[1].x) / 2f +
                ((corners[3].x + corners[2].x) / 2f - (corners[0].x + corners[1].x) / 2f) * progressY.coerceIn(0f, 1f)

        val realXMeters = ((point.x - centerXAtY) / widthAtY) * PITCH_WIDTH_METERS

        return Pair(realXMeters, realYMeters)
    }

    /**
     * Calculates real velocity between two detections given their timestamps.
     * Returns velocity in km/h and mph.
     */
    fun calculateRealSpeedKmh(
        pos1: Offset,
        pos2: Offset,
        timeDeltaSec: Float,
        calibration: PitchCalibration
    ): Float {
        if (timeDeltaSec <= 0.001f) return 0f

        val (x1, y1) = normalizedToRealWorldMeters(pos1, calibration)
        val (x2, y2) = normalizedToRealWorldMeters(pos2, calibration)

        val deltaX = x2 - x1
        val deltaY = y2 - y1
        val distanceMeters = sqrt(deltaX * deltaX + deltaY * deltaY)

        val speedMps = distanceMeters / timeDeltaSec
        // Convert meters per second to km/h (1 m/s = 3.6 km/h)
        val speedKmh = speedMps * 3.6f

        // Bound within reasonable real cricket physical velocities (40 km/h to 165 km/h)
        return speedKmh.coerceIn(35f, 165f)
    }
}
