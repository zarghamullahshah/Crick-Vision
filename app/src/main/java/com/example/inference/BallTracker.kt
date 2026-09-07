package com.example.inference

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/**
 * 2D Constant-Velocity Kalman Filter for Cricket Ball Tracking.
 * State vector: [x, y, vx, vy]
 */
class BallTracker(
    private val dt: Float = 1.0f,
    private val processNoise: Float = 0.005f,
    private val measurementNoise: Float = 0.02f
) {
    // State: [x, y, vx, vy]
    private var x = 0f
    private var y = 0f
    private var vx = 0f
    private var vy = 0f

    // Error covariance matrix diagonal estimates
    private var pX = 0.1f
    private var pY = 0.1f
    private var pVx = 1.0f
    private var pVy = 1.0f

    private var initialized = false
    private val _trajectoryHistory = mutableListOf<Offset>()
    val trajectoryHistory: List<Offset> get() = _trajectoryHistory

    val currentPosition: Offset
        get() = Offset(x, y)

    val velocity: Offset
        get() = Offset(vx, vy)

    fun isInitialized(): Boolean = initialized

    fun reset() {
        initialized = false
        x = 0f
        y = 0f
        vx = 0f
        vy = 0f
        pX = 0.1f
        pY = 0.1f
        pVx = 1.0f
        pVy = 1.0f
        _trajectoryHistory.clear()
    }

    /**
     * Advances state by one frame using constant velocity prediction model:
     * x' = x + vx * dt
     * y' = y + vy * dt
     */
    fun predict() {
        if (!initialized) return

        x += vx * dt
        y += vy * dt

        // Increase covariance due to process noise
        pX += (pVx * dt * dt) + processNoise
        pY += (pVy * dt * dt) + processNoise
        pVx += processNoise
        pVy += processNoise

        _trajectoryHistory.add(Offset(x, y))
        if (_trajectoryHistory.size > 150) {
            _trajectoryHistory.removeAt(0)
        }
    }

    /**
     * Corrects state with new visual detection measurement.
     */
    fun update(measuredPosition: Offset) {
        if (!initialized) {
            x = measuredPosition.x
            y = measuredPosition.y
            vx = 0f
            vy = 0f
            initialized = true
            _trajectoryHistory.add(measuredPosition)
            return
        }

        // Kalman gain K = P / (P + R)
        val kX = pX / (pX + measurementNoise)
        val kY = pY / (pY + measurementNoise)

        // Innovation
        val residualX = measuredPosition.x - x
        val residualY = measuredPosition.y - y

        // Update state
        x += kX * residualX
        y += kY * residualY

        // Velocity innovation estimate
        vx += (kX / dt) * residualX * 0.5f
        vy += (kY / dt) * residualY * 0.5f

        // Update covariance P = (1 - K) * P
        pX *= (1f - kX)
        pY *= (1f - kY)
    }

    /**
     * Projects the ball's path forward in a straight line for [framesAhead] frames.
     * Used for LBW straight-line projection to the stump plane.
     */
    fun predictPositionAt(framesAhead: Int): Offset {
        val projX = x + (vx * dt * framesAhead)
        val projY = y + (vy * dt * framesAhead)
        return Offset(projX, projY)
    }

    /**
     * Calculates projected X position when ball reaches a specific target Y (e.g. stump line).
     */
    fun projectXAtY(targetY: Float): Float {
        if (abs(vy) < 0.0001f) return x
        val framesToReach = (targetY - y) / vy
        return x + (vx * framesToReach)
    }

    /**
     * Finds the bounce / pitch point in the trajectory (local inflection where vertical velocity swings).
     */
    fun findPitchBouncePoint(): Offset? {
        if (_trajectoryHistory.size < 5) return null
        var maxPoint = _trajectoryHistory[0]
        // In screen coords, higher y or inflection point indicates ground pitch
        for (i in 1 until _trajectoryHistory.size - 1) {
            val curr = _trajectoryHistory[i]
            val prev = _trajectoryHistory[i - 1]
            val next = _trajectoryHistory[i + 1]
            // Inflection point in Y
            if ((curr.y >= prev.y && curr.y >= next.y) || (curr.y <= prev.y && curr.y <= next.y)) {
                maxPoint = curr
            }
        }
        return maxPoint
    }
}
