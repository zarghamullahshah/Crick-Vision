package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BowlingSpeed
import com.example.ui.theme.NaturalCardWhite
import com.example.ui.theme.NaturalCreaseChalk
import com.example.ui.theme.NaturalOchre
import com.example.ui.theme.NaturalOchreLight
import com.example.ui.theme.NaturalRed
import com.example.ui.theme.NaturalRedLight
import com.example.ui.theme.NaturalSage
import com.example.ui.theme.NaturalSageLight
import com.example.ui.theme.NaturalSandBorder
import com.example.ui.theme.NaturalSurface
import com.example.ui.theme.NaturalTextMuted
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance real-time bowling velocity overlay utilizing custom Canvas drawing
 * to render a sweeping speedometer gauge, dynamic speed tier arcs, and multi-point velocity telemetry.
 */
@Composable
fun BowlingSpeedOverlay(
    speed: BowlingSpeed,
    modifier: Modifier = Modifier,
    isExpandedDefault: Boolean = true
) {
    var isExpanded by remember { mutableStateOf(isExpandedDefault) }

    // Smooth animated angle for speedometer gauge needle & arc
    val animatedSpeed by animateFloatAsState(
        targetValue = speed.currentKmh.coerceIn(0f, 160f),
        animationSpec = tween(durationMillis = 220),
        label = "animated_speed_gauge"
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = NaturalCardWhite.copy(alpha = 0.95f)),
        border = BorderStroke(1.dp, NaturalSandBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .width(280.dp)
            .testTag("bowling_speed_overlay")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title, Live indicator & Expand/Collapse Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(NaturalSageLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Bowling Speed",
                            tint = NaturalSage,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BOWLING VELOCITY",
                                color = NaturalTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            if (speed.isLive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(NaturalRed, CircleShape)
                                )
                            }
                        }
                        Text(
                            text = speed.paceCategory,
                            color = NaturalSage,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("toggle_speed_details_button")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand speed details",
                        tint = NaturalTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Speed Display & Custom Canvas Gauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Custom Canvas Speedometer Arc
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(90.dp)
                            .testTag("speed_gauge_canvas")
                    ) {
                        val strokeWidth = 8.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val radius = diameter / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val arcSize = Size(diameter, diameter)
                        val topLeft = Offset(center.x - radius, center.y - radius)

                        val startAngle = 140f
                        val sweepAngle = 260f

                        // 1. Background Arc Track (Natural Sand / subtle neutral)
                        drawArc(
                            color = NaturalCreaseChalk.copy(alpha = 0.45f),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // 2. Active Speed Arc
                        val currentFraction = (animatedSpeed / 160f).coerceIn(0f, 1f)
                        val activeSweep = sweepAngle * currentFraction

                        val arcColor = when {
                            animatedSpeed >= 140f -> NaturalRed
                            animatedSpeed >= 120f -> NaturalOchre
                            else -> NaturalSage
                        }

                        if (activeSweep > 0f) {
                            drawArc(
                                color = arcColor,
                                startAngle = startAngle,
                                sweepAngle = activeSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        // 3. Tick Marks
                        val totalTicks = 8
                        for (i in 0..totalTicks) {
                            val tickFraction = i.toFloat() / totalTicks
                            val tickAngleDeg = startAngle + (sweepAngle * tickFraction)
                            val tickAngleRad = Math.toRadians(tickAngleDeg.toDouble())

                            val innerRadius = radius - strokeWidth - 2f
                            val outerRadius = innerRadius - 5f

                            val p1 = Offset(
                                (center.x + innerRadius * cos(tickAngleRad)).toFloat(),
                                (center.y + innerRadius * sin(tickAngleRad)).toFloat()
                            )
                            val p2 = Offset(
                                (center.x + outerRadius * cos(tickAngleRad)).toFloat(),
                                (center.y + outerRadius * sin(tickAngleRad)).toFloat()
                            )

                            drawLine(
                                color = NaturalTextMuted,
                                start = p1,
                                end = p2,
                                strokeWidth = 1.5f
                            )
                        }

                        // 4. Center Needle Pointer
                        val needleAngleDeg = startAngle + (sweepAngle * currentFraction)
                        val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
                        val needleLength = radius - 4f

                        val needleTip = Offset(
                            (center.x + needleLength * cos(needleAngleRad)).toFloat(),
                            (center.y + needleLength * sin(needleAngleRad)).toFloat()
                        )

                        drawLine(
                            color = arcColor,
                            start = center,
                            end = needleTip,
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )

                        drawCircle(
                            color = arcColor,
                            radius = 4f,
                            center = center
                        )
                    }

                    // Center Small Unit text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 18.dp)
                    ) {
                        Text(
                            text = if (speed.currentKmh > 0f) "${animatedSpeed.toInt()}" else "--",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NaturalTextPrimary
                        )
                        Text(
                            text = "KM/H",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = NaturalTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Numerical readout column
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "${speed.formattedCurrentKmh()} km/h",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = NaturalTextPrimary
                        )
                    )
                    Text(
                        text = "${speed.formattedCurrentMph()} mph",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = NaturalTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Pace category badge
                    val (badgeBg, badgeText) = when {
                        speed.currentKmh >= 140f -> Pair(NaturalRedLight, NaturalRed)
                        speed.currentKmh >= 115f -> Pair(NaturalOchreLight, NaturalOchre)
                        else -> Pair(NaturalSageLight, NaturalSage)
                    }

                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (speed.currentKmh > 0f) speed.paceCategory else "READY",
                            color = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Expandable Multi-Point Speed Breakdown (Release, Bounce, Stumps)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(NaturalSandBorder)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SpeedPhasePill(
                            phase = "RELEASE",
                            kmh = speed.formattedReleaseKmh(),
                            tag = "speed_release_stat",
                            color = NaturalSage
                        )
                        SpeedPhasePill(
                            phase = "BOUNCE",
                            kmh = speed.formattedBounceKmh(),
                            tag = "speed_bounce_stat",
                            color = NaturalOchre
                        )
                        SpeedPhasePill(
                            phase = "STUMPS",
                            kmh = speed.formattedImpactKmh(),
                            tag = "speed_impact_stat",
                            color = NaturalRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedPhasePill(
    phase: String,
    kmh: String,
    tag: String,
    color: Color
) {
    Surface(
        color = NaturalSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, NaturalSandBorder),
        modifier = Modifier.testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = phase,
                color = NaturalTextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (kmh != "--.-") "$kmh km/h" else "--",
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
