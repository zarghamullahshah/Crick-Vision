package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Detection
import com.example.model.PoseLandmarks
import com.example.ui.components.PitchCanvasOverlay
import com.example.ui.theme.NaturalBackground
import com.example.ui.theme.NaturalBorderSubtle
import com.example.ui.theme.NaturalCardWhite
import com.example.ui.theme.NaturalOchre
import com.example.ui.theme.NaturalPitchGreen
import com.example.ui.theme.NaturalRed
import com.example.ui.theme.NaturalSage
import com.example.ui.theme.NaturalSageLight
import com.example.ui.theme.NaturalSandBorder
import com.example.ui.theme.NaturalSurface
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary
import com.example.viewmodel.CricketVisionViewModel
import com.example.vision.BowlerFootCreaseDetector
import java.util.Locale
import kotlin.math.abs

@Composable
fun DeliverySimulatorScreen(
    viewModel: CricketVisionViewModel,
    modifier: Modifier = Modifier
) {
    val calibration by viewModel.calibration.collectAsStateWithLifecycle()
    val activeDecision by viewModel.activeDecision.collectAsStateWithLifecycle()

    var bowlerFootY by remember { mutableFloatStateOf(calibration.poppingCrease[0].y + 0.02f) }
    var pitchPointX by remember { mutableFloatStateOf(calibration.stumpBase.x) }
    var impactPointX by remember { mutableFloatStateOf(calibration.stumpBase.x) }
    var isAirborne by remember { mutableStateOf(false) }
    var nearBatContact by remember { mutableStateOf(false) }

    val creaseY = calibration.poppingCrease[0].y
    val footDetector = remember(calibration) {
        BowlerFootCreaseDetector(calibration.poppingCrease, calibration.stumpBase)
    }
    val footAnalysis = footDetector.evaluatePoint(Offset(0.48f, bowlerFootY))
    val isFootOverstepping = footAnalysis.isOverstep
    val footMarginCm = abs(footAnalysis.signedDistanceCm)

    val stumpBase = calibration.stumpBase
    val isPitchInLine = abs(pitchPointX - stumpBase.x) <= 0.15f
    val isImpactInLine = abs(impactPointX - stumpBase.x) <= 0.15f
    val isHittingStumps = abs(impactPointX - stumpBase.x) <= 0.045f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NaturalBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Net Practice Lab",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = NaturalTextPrimary
            )
        )
        Text(
            text = "Interactive parameter tuning for 2D Kalman filter & rule criteria",
            style = MaterialTheme.typography.bodySmall.copy(color = NaturalTextSecondary)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Mini Pitch Visualizer
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalPitchGreen),
            border = BorderStroke(1.dp, NaturalSandBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val trajectory = listOf(
                    Offset(0.50f, bowlerFootY),
                    Offset(pitchPointX, 0.50f),
                    Offset(impactPointX, stumpBase.y)
                )

                PitchCanvasOverlay(
                    calibration = calibration,
                    ballDetection = Detection("ball", android.graphics.RectF(impactPointX - 0.02f, stumpBase.y - 0.02f, impactPointX + 0.02f, stumpBase.y + 0.02f), 0.95f),
                    trajectoryHistory = trajectory,
                    projectedTrajectory = listOf(
                        Offset(impactPointX, stumpBase.y),
                        Offset(stumpBase.x, stumpBase.y - 0.05f)
                    ),
                    poseLandmarks = PoseLandmarks(
                        frontFootAnkle = Offset(0.48f, bowlerFootY),
                        frontFootToe = Offset(0.48f, bowlerFootY - 0.015f),
                        frontFootContact = Offset(0.48f, bowlerFootY),
                        isOverstepping = isFootOverstepping,
                        creaseDistanceCm = footAnalysis.signedDistanceCm,
                        battingHandWrist = if (nearBatContact) Offset(impactPointX + 0.03f, stumpBase.y) else Offset(impactPointX + 0.20f, stumpBase.y)
                    ),
                    pitchBouncePoint = Offset(pitchPointX, 0.50f),
                    showCalibrationLines = true,
                    showHawkeyeProjection = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Control Sliders
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalCardWhite),
            border = BorderStroke(1.dp, NaturalSandBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Bowler landing foot slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bowler Front Foot Y", color = NaturalTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isFootOverstepping) {
                            "OVERSTEP (+${String.format(Locale.US, "%.1f", footMarginCm)} cm)"
                        } else {
                            "LEGAL CREASE (-${String.format(Locale.US, "%.1f", footMarginCm)} cm)"
                        },
                        color = if (isFootOverstepping) NaturalRed else NaturalSage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = bowlerFootY,
                    onValueChange = { bowlerFootY = it },
                    valueRange = (creaseY - 0.08f)..(creaseY + 0.08f),
                    colors = SliderDefaults.colors(
                        thumbColor = if (isFootOverstepping) NaturalRed else NaturalSage,
                        activeTrackColor = NaturalSage,
                        inactiveTrackColor = NaturalSageLight
                    ),
                    modifier = Modifier.testTag("slider_bowler_foot")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Ball Pitch Bounce Point X
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pitch Point X", color = NaturalTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isPitchInLine) "PITCH IN-LINE" else "OUTSIDE LINE",
                        color = if (isPitchInLine) NaturalSage else NaturalOchre,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = pitchPointX,
                    onValueChange = { pitchPointX = it },
                    valueRange = 0.20f..0.80f,
                    colors = SliderDefaults.colors(
                        thumbColor = NaturalOchre,
                        activeTrackColor = NaturalOchre
                    ),
                    modifier = Modifier.testTag("slider_pitch_x")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Impact Point X
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Impact Point X (at stumps)", color = NaturalTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isHittingStumps) "HITTING STUMPS" else "MISSING",
                        color = if (isHittingStumps) NaturalRed else NaturalSage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = impactPointX,
                    onValueChange = { impactPointX = it },
                    valueRange = 0.20f..0.80f,
                    colors = SliderDefaults.colors(
                        thumbColor = NaturalSage,
                        activeTrackColor = NaturalSage,
                        inactiveTrackColor = NaturalSageLight
                    ),
                    modifier = Modifier.testTag("slider_impact_x")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Toggles: Near Bat contact proxy & Airborne
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bat Proximity Contact Proxy", color = NaturalTextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = nearBatContact,
                        onCheckedChange = { nearBatContact = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NaturalSage,
                            checkedTrackColor = NaturalSageLight
                        ),
                        modifier = Modifier.testTag("switch_bat_contact")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Airborne Flight (for Six/Four check)", color = NaturalTextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = isAirborne,
                        onCheckedChange = { isAirborne = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NaturalSage,
                            checkedTrackColor = NaturalSageLight
                        ),
                        modifier = Modifier.testTag("switch_airborne")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Rule Breakdown Diagnostics
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalSurface),
            border = BorderStroke(1.dp, NaturalSandBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Rule Engine Diagnostics",
                    color = NaturalTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                DiagnosticRow(
                    label = "No-Ball Crease Clearance",
                    passed = !isFootOverstepping,
                    passText = "Behind Crease",
                    failText = "Overstepped Line"
                )
                DiagnosticRow(
                    label = "LBW Pitching in line",
                    passed = isPitchInLine,
                    passText = "Within +/- 0.15 margin",
                    failText = "Outside Line"
                )
                DiagnosticRow(
                    label = "LBW Impact in line",
                    passed = isImpactInLine,
                    passText = "In front of wickets",
                    failText = "Outside Line"
                )
                DiagnosticRow(
                    label = "LBW Hitting Wickets",
                    passed = isHittingStumps,
                    passText = "Direct on Stumps",
                    failText = "Missing Wickets"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Trigger Evaluation Button
        Button(
            onClick = {
                when {
                    isFootOverstepping -> viewModel.selectPreset(com.example.viewmodel.DeliveryPreset.OVERSTEP_NO_BALL)
                    isPitchInLine && isImpactInLine && isHittingStumps -> viewModel.selectPreset(com.example.viewmodel.DeliveryPreset.INSWING_LBW_OUT)
                    nearBatContact -> viewModel.selectPreset(com.example.viewmodel.DeliveryPreset.EDGE_BAT_REVIEW)
                    impactPointX < calibration.wideLine[0].x || impactPointX > calibration.wideLine[1].x -> viewModel.selectPreset(com.example.viewmodel.DeliveryPreset.DOWN_LEG_WIDE)
                    isAirborne -> viewModel.selectPreset(com.example.viewmodel.DeliveryPreset.LOFTED_SIX)
                    else -> viewModel.selectPreset(com.example.viewmodel.DeliveryPreset.LEGAL_YORKER)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = NaturalSage,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("evaluate_custom_delivery_button")
        ) {
            Icon(Icons.Default.Gavel, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "EXECUTE UMPIRE VERDICT",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, passed: Boolean, passText: String, failText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = NaturalTextSecondary, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.HighlightOff,
                contentDescription = null,
                tint = if (passed) NaturalSage else NaturalRed,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (passed) passText else failText,
                color = if (passed) NaturalSage else NaturalRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

