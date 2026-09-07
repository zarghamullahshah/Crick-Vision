package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.nativeCanvas
import com.example.model.BowlingSpeed
import com.example.model.Detection
import com.example.model.PitchCalibration
import com.example.model.PoseLandmarks
import com.example.ui.theme.NaturalCreaseChalk
import com.example.ui.theme.NaturalGreen
import com.example.ui.theme.NaturalOchre
import com.example.ui.theme.NaturalRed
import com.example.ui.theme.NaturalRedLight
import com.example.ui.theme.NaturalSage
import com.example.ui.theme.NaturalSageLight
import com.example.ui.theme.NaturalSandDark
import com.example.ui.theme.NaturalSlateText
import com.example.ui.theme.NaturalStumpWood
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
fun PitchCanvasOverlay(
    calibration: PitchCalibration,
    ballDetection: Detection?,
    trajectoryHistory: List<Offset>,
    projectedTrajectory: List<Offset>,
    poseLandmarks: PoseLandmarks?,
    pitchBouncePoint: Offset?,
    bowlingSpeed: BowlingSpeed? = null,
    showCalibrationLines: Boolean = true,
    showHawkeyeProjection: Boolean = true,
    onTapPitch: ((Offset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (onTapPitch != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val normalized = Offset(offset.x / size.width, offset.y / size.height)
                            onTapPitch(normalized)
                        }
                    }
                } else Modifier
            )
    ) {
        val w = size.width
        val h = size.height

        // 1. Draw Calibrated Pitch Polygon
        if (showCalibrationLines && calibration.pitchCorners.size == 4) {
            val c = calibration.pitchCorners.map { Offset(it.x * w, it.y * h) }
            val pitchPath = Path().apply {
                moveTo(c[0].x, c[0].y)
                lineTo(c[1].x, c[1].y)
                lineTo(c[2].x, c[2].y)
                lineTo(c[3].x, c[3].y)
                close()
            }

            // Turf fill
            drawPath(
                path = pitchPath,
                color = NaturalSage.copy(alpha = 0.18f)
            )

            // Pitch boundary lines
            drawPath(
                path = pitchPath,
                color = NaturalCreaseChalk.copy(alpha = 0.85f),
                style = Stroke(width = 2.5f)
            )

            // Draw Popping Crease (Bowler release line)
            val pCrease = calibration.poppingCrease.map { Offset(it.x * w, it.y * h) }
            if (pCrease.size >= 2) {
                val isOverstep = poseLandmarks?.isOverstepping == true
                if (isOverstep) {
                    // Striking glowing red alert border highlighting the breached line
                    drawLine(
                        color = NaturalRed.copy(alpha = 0.40f),
                        start = pCrease[0],
                        end = pCrease[1],
                        strokeWidth = 14f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = NaturalRed,
                        start = pCrease[0],
                        end = pCrease[1],
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                } else {
                    drawLine(
                        color = NaturalCreaseChalk,
                        start = pCrease[0],
                        end = pCrease[1],
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw Wide Lines at batsman end
            val wLines = calibration.wideLine.map { Offset(it.x * w, it.y * h) }
            if (wLines.size >= 2) {
                // Left wide line guideline
                drawLine(
                    color = NaturalOchre.copy(alpha = 0.9f),
                    start = Offset(wLines[0].x, wLines[0].y - 30f),
                    end = Offset(wLines[0].x, wLines[0].y + 30f),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                // Right wide line guideline
                drawLine(
                    color = NaturalOchre.copy(alpha = 0.9f),
                    start = Offset(wLines[1].x, wLines[1].y - 30f),
                    end = Offset(wLines[1].x, wLines[1].y + 30f),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // Draw Stump Base (Wickets)
            val stumpPt = Offset(calibration.stumpBase.x * w, calibration.stumpBase.y * h)
            drawStumps(stumpPt)
        }

        // 2. Draw Trajectory History Trail
        if (trajectoryHistory.size > 1) {
            for (i in 0 until trajectoryHistory.size - 1) {
                val p1 = Offset(trajectoryHistory[i].x * w, trajectoryHistory[i].y * h)
                val p2 = Offset(trajectoryHistory[i + 1].x * w, trajectoryHistory[i + 1].y * h)
                val alpha = (i.toFloat() / trajectoryHistory.size).coerceIn(0.25f, 1.0f)

                drawLine(
                    color = NaturalSage.copy(alpha = alpha),
                    start = p1,
                    end = p2,
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }

        // 3. Draw Pitch Bounce Point
        pitchBouncePoint?.let { bounce ->
            val pt = Offset(bounce.x * w, bounce.y * h)
            drawCircle(
                color = NaturalOchre,
                radius = 12f,
                center = pt
            )
            drawCircle(
                color = NaturalSandDark,
                radius = 16f,
                center = pt,
                style = Stroke(width = 2.5f)
            )
        }

        // 4. Draw Hawkeye Projected Trajectory
        if (showHawkeyeProjection && projectedTrajectory.size > 1) {
            for (i in 0 until projectedTrajectory.size - 1) {
                val p1 = Offset(projectedTrajectory[i].x * w, projectedTrajectory[i].y * h)
                val p2 = Offset(projectedTrajectory[i + 1].x * w, projectedTrajectory[i + 1].y * h)

                drawLine(
                    color = NaturalRed,
                    start = p1,
                    end = p2,
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                    cap = StrokeCap.Round
                )
            }
        }

        // 5. Draw Detected Ball
        ballDetection?.let { det ->
            val bx = det.boundingBox
            val rectLeft = bx.left * w
            val rectTop = bx.top * h
            val rectWidth = bx.width() * w
            val rectHeight = bx.height() * h

            // Bounding box
            drawRect(
                color = NaturalSage,
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectWidth, rectHeight),
                style = Stroke(width = 2.5f)
            )

            // Ball Center Circle
            val center = Offset(det.center.x * w, det.center.y * h)
            drawCircle(
                color = NaturalRed,
                radius = 9f,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 9f,
                center = center,
                style = Stroke(width = 2f)
            )
        }

        // 6. Draw Pose Landmarks (Bowler Front Foot with Overstep Distance & Batsman Wrist/Knee)
        poseLandmarks?.let { pose ->
            val footPoint = pose.frontFootContact ?: pose.frontFootAnkle
            if (footPoint != null) {
                val pt = Offset(footPoint.x * w, footPoint.y * h)
                val isOverstep = pose.isOverstepping
                val footAccentColor = if (isOverstep) NaturalRed else NaturalSage

                // Draw foot landing reticle
                drawCircle(color = footAccentColor.copy(alpha = 0.25f), radius = 20f, center = pt)
                drawCircle(color = footAccentColor, radius = 9f, center = pt)
                drawCircle(color = Color.White, radius = 13f, center = pt, style = Stroke(width = 2.5f))

                // Toe marker if available
                pose.frontFootToe?.let { toe ->
                    val toePt = Offset(toe.x * w, toe.y * h)
                    drawCircle(color = footAccentColor, radius = 5f, center = toePt)
                    drawLine(
                        color = footAccentColor.copy(alpha = 0.7f),
                        start = pt,
                        end = toePt,
                        strokeWidth = 2.5f
                    )
                }

                // Perpendicular projection line to popping crease
                if (calibration.poppingCrease.size >= 2) {
                    val p1 = Offset(calibration.poppingCrease[0].x * w, calibration.poppingCrease[0].y * h)
                    val p2 = Offset(calibration.poppingCrease[1].x * w, calibration.poppingCrease[1].y * h)
                    val creaseDir = Offset(p2.x - p1.x, p2.y - p1.y)
                    val creaseLen = sqrt(creaseDir.x * creaseDir.x + creaseDir.y * creaseDir.y).coerceAtLeast(1f)
                    val u = Offset(creaseDir.x / creaseLen, creaseDir.y / creaseLen)
                    val v = Offset(pt.x - p1.x, pt.y - p1.y)
                    val projLen = (v.x * u.x + v.y * u.y).coerceIn(0f, creaseLen)
                    val creaseIntersect = Offset(p1.x + u.x * projLen, p1.y + u.y * projLen)

                    drawLine(
                        color = footAccentColor,
                        start = pt,
                        end = creaseIntersect,
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                // Measurement Badge pill on canvas
                val marginCm = abs(pose.creaseDistanceCm)
                val marginStr = String.format(Locale.US, "%.1f", marginCm)
                val badgeText = if (isOverstep) "OVERSTEP +${marginStr}cm" else "LEGAL -${marginStr}cm"

                val bgPaint = android.graphics.Paint().apply {
                    color = if (isOverstep) android.graphics.Color.argb(245, 255, 235, 235) else android.graphics.Color.argb(245, 235, 248, 235)
                    style = android.graphics.Paint.Style.FILL
                    isAntiAlias = true
                }
                val borderPaint = android.graphics.Paint().apply {
                    color = if (isOverstep) android.graphics.Color.argb(255, 186, 26, 26) else android.graphics.Color.argb(255, 75, 99, 66)
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 3f
                    isAntiAlias = true
                }
                val textPaint = android.graphics.Paint().apply {
                    color = if (isOverstep) android.graphics.Color.argb(255, 186, 26, 26) else android.graphics.Color.argb(255, 30, 50, 25)
                    textSize = 24f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    isAntiAlias = true
                }

                val textW = textPaint.measureText(badgeText)
                val pillLeft = (pt.x + 18f).coerceAtMost(w - textW - 28f)
                val pillTop = pt.y - 18f
                val pillRect = android.graphics.RectF(pillLeft, pillTop, pillLeft + textW + 24f, pillTop + 34f)
                drawContext.canvas.nativeCanvas.drawRoundRect(pillRect, 8f, 8f, bgPaint)
                drawContext.canvas.nativeCanvas.drawRoundRect(pillRect, 8f, 8f, borderPaint)
                drawContext.canvas.nativeCanvas.drawText(badgeText, pillLeft + 12f, pillTop + 24f, textPaint)
            }

            pose.battingHandWrist?.let { wrist ->
                val pt = Offset(wrist.x * w, wrist.y * h)
                drawCircle(color = NaturalSlateText, radius = 8f, center = pt)
                drawCircle(color = Color.White, radius = 12f, center = pt, style = Stroke(width = 2f))
            }
            pose.frontLegKnee?.let { knee ->
                val pt = Offset(knee.x * w, knee.y * h)
                drawCircle(color = NaturalSage, radius = 8f, center = pt)
            }
        }

        // 7. Draw Velocity Badges on Pitch Canvas (Release, Bounce, Impact)
        bowlingSpeed?.let { speed ->
            if (trajectoryHistory.isNotEmpty()) {
                val bgPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(230, 252, 248, 244)
                    style = android.graphics.Paint.Style.FILL
                    isAntiAlias = true
                }
                val borderPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(200, 75, 99, 66)
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 3f
                    isAntiAlias = true
                }
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(255, 28, 27, 23)
                    textSize = 28f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    isAntiAlias = true
                }

                // A. Release Speed Tag
                if (speed.releaseKmh > 0f) {
                    val releasePos = trajectoryHistory.first()
                    val rx = releasePos.x * w
                    val ry = releasePos.y * h
                    val label = "${speed.formattedReleaseKmh()} km/h"
                    val textWidth = textPaint.measureText(label)
                    val rectF = android.graphics.RectF(
                        rx + 14f,
                        ry - 20f,
                        rx + textWidth + 38f,
                        ry + 20f
                    )
                    drawContext.canvas.nativeCanvas.drawRoundRect(rectF, 12f, 12f, bgPaint)
                    drawContext.canvas.nativeCanvas.drawRoundRect(rectF, 12f, 12f, borderPaint)
                    drawContext.canvas.nativeCanvas.drawText(label, rx + 26f, ry + 9f, textPaint)
                }

                // B. Bounce Speed Tag
                if (speed.bounceKmh > 0f && pitchBouncePoint != null) {
                    val bx = pitchBouncePoint.x * w
                    val by = pitchBouncePoint.y * h
                    val label = "${speed.formattedBounceKmh()} km/h"
                    val textWidth = textPaint.measureText(label)
                    val rectF = android.graphics.RectF(
                        bx + 14f,
                        by - 20f,
                        bx + textWidth + 38f,
                        by + 20f
                    )
                    drawContext.canvas.nativeCanvas.drawRoundRect(rectF, 12f, 12f, bgPaint)
                    drawContext.canvas.nativeCanvas.drawRoundRect(rectF, 12f, 12f, borderPaint)
                    drawContext.canvas.nativeCanvas.drawText(label, bx + 26f, by + 9f, textPaint)
                }

                // C. Impact Speed Tag (Near Stumps)
                if (speed.impactKmh > 0f && trajectoryHistory.size > 10) {
                    val impactPos = trajectoryHistory.last()
                    val ix = impactPos.x * w
                    val iy = impactPos.y * h
                    val label = "${speed.formattedImpactKmh()} km/h"
                    val textWidth = textPaint.measureText(label)
                    val rectF = android.graphics.RectF(
                        ix - textWidth - 38f,
                        iy - 20f,
                        ix - 14f,
                        iy + 20f
                    )
                    drawContext.canvas.nativeCanvas.drawRoundRect(rectF, 12f, 12f, bgPaint)
                    drawContext.canvas.nativeCanvas.drawRoundRect(rectF, 12f, 12f, borderPaint)
                    drawContext.canvas.nativeCanvas.drawText(label, ix - textWidth - 26f, iy + 9f, textPaint)
                }
            }
        }
    }
}

private fun DrawScope.drawStumps(base: Offset) {
    val stumpHeight = 44f
    val stumpSpacing = 12f

    // 3 Stumps
    for (i in -1..1) {
        val sx = base.x + (i * stumpSpacing)
        drawLine(
            color = NaturalStumpWood,
            start = Offset(sx, base.y),
            end = Offset(sx, base.y - stumpHeight),
            strokeWidth = 4.5f,
            cap = StrokeCap.Round
        )
    }

    // Bails
    drawLine(
        color = NaturalSandDark,
        start = Offset(base.x - stumpSpacing - 4f, base.y - stumpHeight),
        end = Offset(base.x + stumpSpacing + 4f, base.y - stumpHeight),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
}
