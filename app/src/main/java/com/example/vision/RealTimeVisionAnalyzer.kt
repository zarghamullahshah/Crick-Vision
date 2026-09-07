package com.example.vision

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.example.inference.BallTracker
import com.example.model.BowlingSpeed
import com.example.model.Decision
import com.example.model.DecisionType
import com.example.model.Detection
import com.example.model.PitchCalibration
import com.example.model.PoseLandmarks
import com.example.rules.BoundaryRule
import com.example.rules.LbwHeuristic
import com.example.rules.NoBallRule
import com.example.rules.WideRule
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Real-time computer vision frame analyzer for live CameraX feeds and video files.
 * Performs real optical motion segmentation, ball centroid localization,
 * speed estimation using physical pitch coordinates, and automated umpire decision evaluation.
 */
class RealTimeVisionAnalyzer(
    private var calibration: PitchCalibration
) {
    private val ballTracker = BallTracker()
    private val footCreaseDetector = BowlerFootCreaseDetector(calibration.poppingCrease, calibration.stumpBase)
    private val noBallRule = NoBallRule(calibration.poppingCrease, calibration.stumpBase)
    private val wideRule = WideRule(calibration.wideLine)
    private val boundaryRule = BoundaryRule(listOf(calibration.pitchCorners[3], calibration.pitchCorners[2]))
    private val lbwHeuristic = LbwHeuristic()

    // Motion tracking buffer (downscaled luminance grid)
    private var prevGrid: ByteArray? = null
    private var gridWidth = 120
    private var gridHeight = 90
    private var lastFrameTimestampNanos: Long = -1L

    private val trackedPoints = mutableListOf<Offset>()
    private val speedHistory = mutableListOf<Float>()
    private var releaseSpeedKmh = 0f
    private var bounceSpeedKmh = 0f
    private var impactSpeedKmh = 0f
    private var detectedBouncePoint: Offset? = null
    private var frameCounter = 0

    fun updateCalibration(newCalibration: PitchCalibration) {
        this.calibration = newCalibration
        noBallRule.updateCreaseLine(newCalibration.poppingCrease, newCalibration.stumpBase)
        footCreaseDetector.updateCalibration(newCalibration.poppingCrease, newCalibration.stumpBase)
        noBallRule.resetForNewDelivery()
        wideRule.resetForNewDelivery()
    }

    fun reset() {
        ballTracker.reset()
        noBallRule.resetForNewDelivery()
        wideRule.resetForNewDelivery()
        footCreaseDetector.updateCalibration(calibration.poppingCrease, calibration.stumpBase)
        prevGrid = null
        lastFrameTimestampNanos = -1L
        trackedPoints.clear()
        speedHistory.clear()
        releaseSpeedKmh = 0f
        bounceSpeedKmh = 0f
        impactSpeedKmh = 0f
        detectedBouncePoint = null
        frameCounter = 0
    }

    data class FrameResult(
        val detection: Detection?,
        val trajectory: List<Offset>,
        val projectedTrajectory: List<Offset>,
        val bouncePoint: Offset?,
        val speed: BowlingSpeed,
        val poseLandmarks: PoseLandmarks?,
        val decision: Decision?
    )

    /**
     * Analyzes a live camera frame from CameraX ImageProxy.
     */
    fun analyzeCameraFrame(imageProxy: ImageProxy): FrameResult? {
        val timestampNanos = imageProxy.imageInfo.timestamp
        val planes = imageProxy.planes
        if (planes.isEmpty()) {
            imageProxy.close()
            return null
        }

        val buffer = planes[0].buffer
        val width = imageProxy.width
        val height = imageProxy.height
        val rowStride = planes[0].rowStride

        // Downsample luminance to compact grid for real-time 30-60 FPS performance
        val currentGrid = extractLuminanceGrid(buffer, width, height, rowStride, gridWidth, gridHeight)

        val result = processGridDifferencing(currentGrid, timestampNanos)
        imageProxy.close()
        return result
    }

    /**
     * Analyzes a video frame bitmap.
     */
    fun analyzeVideoFrame(bitmap: Bitmap, timestampMs: Long): FrameResult {
        val timestampNanos = timestampMs * 1_000_000L
        val currentGrid = extractLuminanceGridFromBitmap(bitmap, gridWidth, gridHeight)
        return processGridDifferencing(currentGrid, timestampNanos)
    }

    private fun processGridDifferencing(
        currentGrid: ByteArray,
        timestampNanos: Long
    ): FrameResult {
        frameCounter++
        var detectedCentroid: Offset? = null
        var motionConfidence = 0f
        var detectedFoot: Offset? = null

        val previous = prevGrid
        if (previous != null && lastFrameTimestampNanos >= 0L) {
            val deltaSec = (timestampNanos - lastFrameTimestampNanos) / 1_000_000_000.0f

            // Frame differencing: find centroid of moving pixels
            var sumX = 0L
            var sumY = 0L
            var motionPixels = 0
            var lowestMotionY = 0

            for (y in 0 until gridHeight) {
                for (x in 0 until gridWidth) {
                    val idx = y * gridWidth + x
                    val currVal = currentGrid[idx].toInt() and 0xFF
                    val prevVal = previous[idx].toInt() and 0xFF
                    val diff = abs(currVal - prevVal)

                    // Motion threshold
                    if (diff > 22) {
                        sumX += x
                        sumY += y
                        motionPixels++
                        if (y > lowestMotionY) {
                            lowestMotionY = y
                        }
                    }
                }
            }

            if (motionPixels >= 8) {
                val avgX = (sumX.toFloat() / motionPixels) / gridWidth
                val avgY = (sumY.toFloat() / motionPixels) / gridHeight
                detectedCentroid = Offset(avgX, avgY)
                motionConfidence = min(0.98f, 0.60f + (motionPixels / 150f))

                // Bowler front foot estimate from lowest motion contact near crease
                val footY = (lowestMotionY.toFloat() / gridHeight)
                if (footY > calibration.poppingCrease[0].y - 0.15f) {
                    detectedFoot = Offset(avgX, footY)
                }

                // Compute real physical velocity if we have previous tracking points
                if (trackedPoints.isNotEmpty() && deltaSec > 0.005f) {
                    val lastPoint = trackedPoints.last()
                    val instantKmh = PitchPhysicsConverter.calculateRealSpeedKmh(
                        lastPoint,
                        detectedCentroid,
                        deltaSec,
                        calibration
                    )
                    if (instantKmh > 30f) {
                        speedHistory.add(instantKmh)
                        if (releaseSpeedKmh == 0f) {
                            releaseSpeedKmh = instantKmh
                        }
                    }
                }
            }
        }

        // Real-time computer vision analysis of bowler front foot relative to popping crease
        val footAnalysis = footCreaseDetector.detectFootInFrame(
            currentGrid = currentGrid,
            prevGrid = previous,
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            frameIndex = frameCounter
        )

        prevGrid = currentGrid
        lastFrameTimestampNanos = timestampNanos

        var detection: Detection? = null
        if (detectedCentroid != null) {
            ballTracker.update(detectedCentroid)
            trackedPoints.add(detectedCentroid)
            if (trackedPoints.size > 80) trackedPoints.removeAt(0)

            detection = Detection(
                label = "cricket_ball",
                boundingBox = RectF(
                    detectedCentroid.x - 0.025f,
                    detectedCentroid.y - 0.025f,
                    detectedCentroid.x + 0.025f,
                    detectedCentroid.y + 0.025f
                ),
                confidence = motionConfidence
            )
        } else if (ballTracker.isInitialized()) {
            ballTracker.predict()
            val predicted = ballTracker.currentPosition
            trackedPoints.add(predicted)
            if (trackedPoints.size > 80) trackedPoints.removeAt(0)
        }

        // Detect bounce point from vertical direction inflection
        if (trackedPoints.size >= 4 && detectedBouncePoint == null) {
            for (i in 2 until trackedPoints.size - 1) {
                val dy1 = trackedPoints[i].y - trackedPoints[i - 1].y
                val dy2 = trackedPoints[i + 1].y - trackedPoints[i].y
                if (dy1 > 0 && dy2 <= 0 && trackedPoints[i].y in 0.35f..0.75f) {
                    detectedBouncePoint = trackedPoints[i]
                    bounceSpeedKmh = speedHistory.lastOrNull() ?: (releaseSpeedKmh * 0.86f)
                    break
                }
            }
        }

        // Current speed calculation
        val currentKmh = speedHistory.lastOrNull() ?: if (releaseSpeedKmh > 0f) releaseSpeedKmh else 0f
        val bowlingSpeed = BowlingSpeed(
            currentKmh = currentKmh,
            currentMph = currentKmh * 0.621371f,
            releaseKmh = if (releaseSpeedKmh > 0f) releaseSpeedKmh else currentKmh,
            bounceKmh = bounceSpeedKmh,
            impactKmh = impactSpeedKmh,
            paceCategory = BowlingSpeed.categorizePace(max(currentKmh, releaseSpeedKmh)),
            isLive = detectedCentroid != null
        )

        val activeFootPoint = footAnalysis.footContact ?: detectedFoot
        val poseLandmarks = if (activeFootPoint != null) {
            val evaluated = if (footAnalysis.footContact != null) {
                footAnalysis
            } else {
                footCreaseDetector.evaluatePoint(activeFootPoint, frameCounter)
            }
            PoseLandmarks(
                frontFootAnkle = evaluated.footAnkle ?: activeFootPoint,
                frontFootToe = evaluated.footToe ?: activeFootPoint,
                frontFootContact = evaluated.footContact ?: activeFootPoint,
                isOverstepping = evaluated.isOverstep,
                creaseDistanceCm = evaluated.signedDistanceCm
            )
        } else null

        // Evaluate automated cricket decisions on real detected coordinates
        var evaluatedDecision: Decision? = null

        // 1. Check Overstep / No-Ball rule on bowler foot position in real-time
        if (poseLandmarks != null && poseLandmarks.isOverstepping) {
            val overstepDecision = footAnalysis.decision ?: noBallRule.evaluate(
                poseLandmarks.frontFootToe ?: poseLandmarks.frontFootAnkle ?: activeFootPoint!!,
                frameCounter
            )
            if (overstepDecision != null) {
                evaluatedDecision = overstepDecision
            }
        } else if (detectedFoot != null) {
            val noBall = noBallRule.evaluate(detectedFoot, frameCounter)
            if (noBall != null) {
                evaluatedDecision = noBall
            }
        }

        // 2. Check Wide / Bat Contact rule
        if (evaluatedDecision == null && detection != null) {
            val isAtStumps = detection.boundingBox.centerY() <= calibration.stumpBase.y + 0.05f
            if (isAtStumps) {
                val wide = wideRule.evaluate(
                    ballDetection = detection,
                    battingWrist = poseLandmarks?.battingHandWrist,
                    isAtStumpPlane = true,
                    frameIndex = frameCounter
                )
                if (wide != null) {
                    evaluatedDecision = wide
                }
            }
        }

        // 3. Check LBW Heuristic at stump plane
        if (evaluatedDecision == null && detectedBouncePoint != null && detection != null) {
            val impact = Offset(detection.boundingBox.centerX(), detection.boundingBox.centerY())
            if (impact.y <= calibration.stumpBase.y + 0.03f) {
                impactSpeedKmh = currentKmh
                val lbw = lbwHeuristic.evaluate(
                    pitchPoint = detectedBouncePoint!!,
                    estimatedImpactPoint = impact,
                    stumpBase = calibration.stumpBase,
                    tracker = ballTracker,
                    frameIndex = frameCounter
                )
                if (lbw.type == DecisionType.LBW_OUT) {
                    evaluatedDecision = lbw
                }
            }
        }

        // 4. Check Boundary Rule
        if (evaluatedDecision == null && trackedPoints.size >= 5) {
            val boundary = boundaryRule.evaluate(
                ballTrajectory = trackedPoints,
                wasAirborne = (detectedBouncePoint == null),
                frameIndex = frameCounter
            )
            if (boundary != null) {
                evaluatedDecision = boundary
            }
        }

        // Hawkeye projection line towards stumps
        val projectedTrajectory = if (trackedPoints.size >= 2) {
            val last = trackedPoints.last()
            val secondLast = trackedPoints[trackedPoints.size - 2]
            val dirX = last.x - secondLast.x
            val dirY = last.y - secondLast.y
            listOf(
                last,
                Offset(last.x + dirX * 3f, last.y + dirY * 3f)
            )
        } else emptyList()

        return FrameResult(
            detection = detection,
            trajectory = trackedPoints.toList(),
            projectedTrajectory = projectedTrajectory,
            bouncePoint = detectedBouncePoint,
            speed = bowlingSpeed,
            poseLandmarks = poseLandmarks,
            decision = evaluatedDecision
        )
    }

    private fun extractLuminanceGrid(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        targetW: Int,
        targetH: Int
    ): ByteArray {
        val out = ByteArray(targetW * targetH)
        val stepX = max(1, width / targetW)
        val stepY = max(1, height / targetH)

        for (gy in 0 until targetH) {
            val srcY = (gy * stepY).coerceAtMost(height - 1)
            val rowStart = srcY * rowStride
            for (gx in 0 until targetW) {
                val srcX = (gx * stepX).coerceAtMost(width - 1)
                val pixelIndex = rowStart + srcX
                out[gy * targetW + gx] = if (pixelIndex < buffer.limit()) {
                    buffer.get(pixelIndex)
                } else 0
            }
        }
        return out
    }

    private fun extractLuminanceGridFromBitmap(
        bitmap: Bitmap,
        targetW: Int,
        targetH: Int
    ): ByteArray {
        val out = ByteArray(targetW * targetH)
        val scaled = if (bitmap.width == targetW && bitmap.height == targetH) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, false)
        }
        val pixels = IntArray(targetW * targetH)
        scaled.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH)
        if (scaled != bitmap) {
            scaled.recycle()
        }

        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            // Standard perceptual luminance formula Y = 0.299R + 0.587G + 0.114B
            val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
            out[i] = lum.toByte()
        }
        return out
    }
}
