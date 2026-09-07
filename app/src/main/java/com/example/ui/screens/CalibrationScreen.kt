package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PitchCalibration
import com.example.ui.components.PitchCanvasOverlay
import com.example.ui.theme.NaturalBackground
import com.example.ui.theme.NaturalCardWhite
import com.example.ui.theme.NaturalPitchDeep
import com.example.ui.theme.NaturalPitchGreen
import com.example.ui.theme.NaturalPitchLight
import com.example.ui.theme.NaturalSage
import com.example.ui.theme.NaturalSageLight
import com.example.ui.theme.NaturalSandBorder
import com.example.ui.theme.NaturalSurface
import com.example.ui.theme.NaturalTextMuted
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary
import com.example.viewmodel.CalibrationStep
import com.example.viewmodel.CricketVisionViewModel

@Composable
fun CalibrationScreen(
    viewModel: CricketVisionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calibration by viewModel.calibration.collectAsStateWithLifecycle()
    val step by viewModel.calibrationStep.collectAsStateWithLifecycle()
    val tempCorners by viewModel.tempCalibrationCorners.collectAsStateWithLifecycle()
    val tempStump by viewModel.tempStumpBase.collectAsStateWithLifecycle()
    val tempWide by viewModel.tempWideLine.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (step is CalibrationStep.Completed) {
            viewModel.startCalibration()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        NaturalPitchLight,
                        NaturalPitchGreen,
                        NaturalPitchDeep
                    )
                )
            )
    ) {
        // Effective calibration for preview during tapping
        val previewCalibration = PitchCalibration(
            pitchCorners = if (tempCorners.size == 4) tempCorners else calibration.pitchCorners,
            stumpBase = tempStump ?: calibration.stumpBase,
            wideLine = if (tempWide.size == 2) tempWide else calibration.wideLine,
            poppingCrease = if (tempCorners.size >= 2) listOf(tempCorners[0], tempCorners[1]) else calibration.poppingCrease
        )

        // Pitch Tap Canvas
        PitchCanvasOverlay(
            calibration = previewCalibration,
            ballDetection = null,
            trajectoryHistory = emptyList(),
            projectedTrajectory = emptyList(),
            poseLandmarks = null,
            pitchBouncePoint = null,
            showCalibrationLines = true,
            showHawkeyeProjection = false,
            onTapPitch = { pt ->
                viewModel.handleCalibrationTap(pt)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Navigation & Step Header
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(NaturalCardWhite.copy(alpha = 0.94f), CircleShape)
                        .border(1.dp, NaturalSandBorder, CircleShape)
                        .testTag("back_from_calibration_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NaturalTextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Pitch Calibration",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = NaturalTextPrimary
                        )
                    )
                    Text(
                        text = "Align optical markers with real cricket pitch",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = NaturalTextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step Progress Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NaturalCardWhite.copy(alpha = 0.96f)),
                border = BorderStroke(1.dp, NaturalSandBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = NaturalSageLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "STEP ${step.stepNumber} OF 3",
                                color = NaturalSage,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Point count indicator
                        val statusText = when (step) {
                            is CalibrationStep.Step1Corners -> "${tempCorners.size} / 4 Corners Set"
                            is CalibrationStep.Step2Stump -> if (tempStump != null) "1 / 1 Stump Set" else "0 / 1 Tap Stump Base"
                            is CalibrationStep.Step3WideLine -> "${tempWide.size} / 2 Wide Markers"
                            is CalibrationStep.Completed -> "All Points Calibrated"
                        }

                        Text(
                            text = statusText,
                            color = NaturalSage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = step.title,
                        color = NaturalTextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = step.instructions,
                        color = NaturalTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Bottom Controls
        Card(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = NaturalCardWhite),
            border = BorderStroke(1.dp, NaturalSandBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.startCalibration() },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NaturalSandBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restart_calibration_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = NaturalTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RETRY", color = NaturalTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.resetToDefaultCalibration()
                            onNavigateBack()
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NaturalSandBorder),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("default_preset_calibration_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Standard Pitch",
                            tint = NaturalSage,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STANDARD", color = NaturalSage, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NaturalSage,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("confirm_calibration_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CONFIRM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

