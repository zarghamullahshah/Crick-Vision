package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BowlingSpeedOverlay
import com.example.ui.components.DecisionBanner
import com.example.ui.components.PitchCanvasOverlay
import com.example.ui.theme.NaturalBackground
import com.example.ui.theme.NaturalCardWhite
import com.example.ui.theme.NaturalOchre
import com.example.ui.theme.NaturalOchreLight
import com.example.ui.theme.NaturalPitchDeep
import com.example.ui.theme.NaturalPitchGreen
import com.example.ui.theme.NaturalPitchLight
import com.example.ui.theme.NaturalRed
import com.example.ui.theme.NaturalRedBorder
import com.example.ui.theme.NaturalRedLight
import com.example.ui.theme.NaturalSage
import com.example.ui.theme.NaturalSageBorder
import com.example.ui.theme.NaturalSageLight
import com.example.ui.theme.NaturalSand
import com.example.ui.theme.NaturalSandBorder
import com.example.ui.theme.NaturalSlate
import com.example.ui.theme.NaturalSlateBorder
import com.example.ui.theme.NaturalSlateText
import com.example.ui.theme.NaturalSurface
import com.example.ui.theme.NaturalTextMuted
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary
import com.example.viewmodel.CricketVisionViewModel
import com.example.viewmodel.DeliveryPreset
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun LiveMatchScreen(
    viewModel: CricketVisionViewModel,
    onNavigateCalibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val calibration by viewModel.calibration.collectAsStateWithLifecycle()
    val activeDecision by viewModel.activeDecision.collectAsStateWithLifecycle()
    val currentDetection by viewModel.currentDetection.collectAsStateWithLifecycle()
    val trajectoryHistory by viewModel.trajectoryHistory.collectAsStateWithLifecycle()
    val projectedTrajectory by viewModel.projectedTrajectory.collectAsStateWithLifecycle()
    val poseLandmarks by viewModel.poseLandmarks.collectAsStateWithLifecycle()
    val pitchBouncePoint by viewModel.pitchBouncePoint.collectAsStateWithLifecycle()
    val isSimulating by viewModel.isSimulating.collectAsStateWithLifecycle()
    val selectedPreset by viewModel.selectedPreset.collectAsStateWithLifecycle()
    val isCameraMode by viewModel.isCameraMode.collectAsStateWithLifecycle()
    val bowlingSpeed by viewModel.bowlingSpeed.collectAsStateWithLifecycle()
    val userVideoUri by viewModel.userVideoUri.collectAsStateWithLifecycle()
    val isVideoMode by viewModel.isVideoMode.collectAsStateWithLifecycle()
    val isVideoPlaying by viewModel.isVideoPlaying.collectAsStateWithLifecycle()
    val videoDurationMs by viewModel.videoDurationMs.collectAsStateWithLifecycle()
    val videoCurrentPositionMs by viewModel.videoCurrentPositionMs.collectAsStateWithLifecycle()
    val useUrdu by viewModel.useUrdu.collectAsStateWithLifecycle()
    val voiceEnabled by viewModel.voiceEnabled.collectAsStateWithLifecycle()
    val fps by viewModel.fps.collectAsStateWithLifecycle()

    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionRatio by remember { mutableFloatStateOf(0f) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4") }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            viewModel.setCameraMode(true)
        }
    }

    // Modern Zero-Permission Photo/Video Picker (Complies 100% with Google Play Policy)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.loadUserVideo(uri)
        }
    }

    // Periodic updater for video current playback position
    LaunchedEffect(isVideoMode, isVideoPlaying, userVideoUri) {
        while (isVideoMode && userVideoUri != null) {
            videoViewInstance?.let { vView ->
                if (vView.isPlaying && !isScrubbing) {
                    val cur = vView.currentPosition.toLong()
                    val dur = vView.duration.toLong().coerceAtLeast(1L)
                    viewModel.updateVideoProgress(cur, dur)
                }
            }
            delay(250)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NaturalBackground)
    ) {
        // 1. Media Feed Layer: Camera Preview, Video Playback, OR Virtual Grass Pitch Canvas
        when {
            isVideoMode -> {
                if (userVideoUri != null) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(userVideoUri)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    viewModel.updateVideoProgress(0L, duration.toLong().coerceAtLeast(1L))
                                    if (isVideoPlaying) start()
                                }
                                videoViewInstance = this
                            }
                        },
                        update = { vView ->
                            videoViewInstance = vView
                            if (isVideoPlaying && !vView.isPlaying && !isScrubbing) {
                                vView.start()
                            } else if (!isVideoPlaying && vView.isPlaying) {
                                vView.pause()
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("match_video_view")
                    )
                } else {
                    // Empty state for Video Mode: Prompt user to pick any match video
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(NaturalSurface, NaturalSandBorder.copy(alpha = 0.5f), NaturalSurface)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = NaturalCardWhite),
                            border = BorderStroke(1.dp, NaturalSandBorder),
                            modifier = Modifier
                                .padding(32.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(NaturalSageLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VideoLibrary,
                                        contentDescription = "Video Visualizer",
                                        tint = NaturalSage,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Import Any Cricket Match Video",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalTextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Choose any real-world bowling video, nets clip, or match replay to visualize Hawkeye trajectory, measure bowling velocity, and test auto-umpire decisions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NaturalTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Quick Load Sample Video Button
                                Button(
                                    onClick = {
                                        viewModel.loadUserVideo(Uri.parse(PRESET_SAMPLE_VIDEOS[0].url))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NaturalSage,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("load_sample_video_button")
                                ) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = "Load Sample")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("⚡ LOAD SAMPLE TEST VIDEO", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Secondary Action Buttons (Select from Device / Paste URL)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            videoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, NaturalSandBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("pick_match_video_button")
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = "Device Video", tint = NaturalTextPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("DEVICE FILE", color = NaturalTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { showUrlDialog = true },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, NaturalSandBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("enter_video_url_button")
                                    ) {
                                        Icon(Icons.Default.Link, contentDescription = "Paste URL", tint = NaturalTextPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("PASTE URL", color = NaturalTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Sample Presets Chips Row
                                Text(
                                    text = "OR CHOOSE A SAMPLE VIDEO:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NaturalTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PRESET_SAMPLE_VIDEOS.forEachIndexed { index, sample ->
                                        Surface(
                                            color = NaturalSurface,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, NaturalSandBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.loadUserVideo(Uri.parse(sample.url))
                                                }
                                                .testTag("preset_sample_video_$index")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Play",
                                                    tint = NaturalSage,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = sample.title,
                                                        color = NaturalTextPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = sample.subtitle,
                                                        color = NaturalTextSecondary,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            isCameraMode && hasCameraPermission -> {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                                .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                        viewModel.processCameraImage(imageProxy)
                                    }
                                }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                            } catch (e: Exception) {
                                Log.e("LiveMatchScreen", "Camera binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                // Virtual Pitch Background in rich natural grass gradient
                Box(
                    modifier = Modifier
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
                )
            }
        }

        // 2. Interactive Pitch Canvas Overlay (Draws pitch, ball trajectory, bounce point, and speed markers)
        PitchCanvasOverlay(
            calibration = calibration,
            ballDetection = currentDetection,
            trajectoryHistory = trajectoryHistory,
            projectedTrajectory = projectedTrajectory,
            poseLandmarks = poseLandmarks,
            pitchBouncePoint = pitchBouncePoint,
            bowlingSpeed = bowlingSpeed,
            showCalibrationLines = true,
            showHawkeyeProjection = true,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Floating Real-Time Bowling Velocity Overlay (Sweeping Speedometer Canvas)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 112.dp, end = 12.dp)
        ) {
            BowlingSpeedOverlay(
                speed = bowlingSpeed,
                isExpandedDefault = true
            )
        }

        // 4. Top Header & Mode Switcher Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Row 1: Status HUD & Quick Actions (Language, Voice, Calibration)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tracking Status HUD Badge
                Surface(
                    color = NaturalCardWhite.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, NaturalSandBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isSimulating) NaturalOchre else NaturalSage)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSimulating) {
                                if (isVideoMode) "ANALYZING VIDEO FRAMES" else "BALL IN FLIGHT"
                            } else if (isCameraMode) {
                                "CAMERA VISION TRACKING"
                            } else if (isVideoMode) {
                                "VIDEO VISION READY"
                            } else {
                                "VISION READY"
                            },
                            color = NaturalTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${fps}fps",
                            color = NaturalSage,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Action icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Language Switcher (EN / UR)
                    Surface(
                        color = NaturalCardWhite.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NaturalSandBorder),
                        modifier = Modifier
                            .clickable { viewModel.toggleLanguage() }
                            .testTag("toggle_language_button")
                    ) {
                        Text(
                            text = if (useUrdu) "اردو" else "EN",
                            color = if (useUrdu) NaturalSage else NaturalTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Voice Mute / Unmute
                    IconButton(
                        onClick = { viewModel.toggleVoice() },
                        modifier = Modifier
                            .size(34.dp)
                            .background(NaturalCardWhite.copy(alpha = 0.94f), CircleShape)
                            .border(1.dp, NaturalSandBorder, CircleShape)
                            .testTag("toggle_voice_button")
                    ) {
                        Icon(
                            imageVector = if (voiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Voice toggle",
                            tint = if (voiceEnabled) NaturalSage else NaturalTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Calibration Settings Shortcut
                    IconButton(
                        onClick = onNavigateCalibration,
                        modifier = Modifier
                            .size(34.dp)
                            .background(NaturalCardWhite.copy(alpha = 0.94f), CircleShape)
                            .border(1.dp, NaturalSandBorder, CircleShape)
                            .testTag("calibration_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Calibration",
                            tint = NaturalSage,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Source Segmented Selector (Virtual Pitch, Camera Live, Video Visualizer)
            Surface(
                color = NaturalCardWhite.copy(alpha = 0.96f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NaturalSandBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Virtual Pitch Tab
                    val isVirtual = !isCameraMode && !isVideoMode
                    FeedSourcePill(
                        title = "Virtual Pitch",
                        icon = Icons.Default.Grass,
                        isSelected = isVirtual,
                        tag = "source_tab_virtual",
                        onClick = {
                            viewModel.setCameraMode(false)
                            viewModel.setVideoMode(false)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Live Camera Tab
                    FeedSourcePill(
                        title = "Live Camera",
                        icon = Icons.Default.CameraAlt,
                        isSelected = isCameraMode && !isVideoMode,
                        tag = "source_tab_camera",
                        onClick = {
                            if (!hasCameraPermission) {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                viewModel.setCameraMode(true)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Video Visualizer Tab
                    FeedSourcePill(
                        title = "Match Video",
                        icon = Icons.Default.VideoLibrary,
                        isSelected = isVideoMode,
                        tag = "source_tab_video",
                        onClick = {
                            viewModel.setVideoMode(true)
                            if (userVideoUri == null) {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Decision Banner (Top animated pop-down overlay)
        DecisionBanner(
            decision = activeDecision,
            isUrdu = useUrdu,
            onDismiss = { viewModel.dismissActiveDecision() },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Real-Time Popping Crease Status Badge (visible during foot detection when banner dismissed)
        if (activeDecision == null && (poseLandmarks?.frontFootAnkle != null || poseLandmarks?.frontFootContact != null)) {
            val isOverstep = poseLandmarks?.isOverstepping == true
            val marginCm = kotlin.math.abs(poseLandmarks?.creaseDistanceCm ?: 0f)
            val marginStr = String.format(Locale.US, "%.1f", marginCm)

            Surface(
                color = if (isOverstep) NaturalRedLight else NaturalSageLight,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, if (isOverstep) NaturalRedBorder else NaturalSageBorder),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 118.dp)
                    .testTag("crease_overstep_alert_badge")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isOverstep) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = "Crease alert",
                        tint = if (isOverstep) NaturalRed else NaturalSage,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOverstep) {
                            if (useUrdu) "اوور سٹیپ الرٹ! کریز سے +${marginStr} سینٹی میٹر آگے"
                            else "OVERSTEP ALERT: +${marginStr} cm past crease"
                        } else {
                            if (useUrdu) "کریز کلیئر: -${marginStr} سینٹی میٹر پیچھے"
                            else "CREASE CLEAR: -${marginStr} cm behind line"
                        },
                        color = if (isOverstep) NaturalRed else NaturalSage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 6. Bottom Control Deck: Video Scrub Controls (if in video mode) + Presets & Action Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            NaturalBackground.copy(alpha = 0.88f),
                            NaturalBackground
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Video Playback & Scrub Bar (Active when user loaded a video)
            if (isVideoMode && userVideoUri != null) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NaturalCardWhite.copy(alpha = 0.96f)),
                    border = BorderStroke(1.dp, NaturalSandBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("video_scrub_controls_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play / Pause button
                        IconButton(
                            onClick = {
                                viewModel.toggleVideoPlayback()
                                videoViewInstance?.let { vView ->
                                    if (vView.isPlaying) vView.pause() else vView.start()
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(NaturalSageLight, CircleShape)
                                .testTag("video_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play or Pause",
                                tint = NaturalSage,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Video Scrub Slider
                        val progressFraction = if (videoDurationMs > 0L) {
                            (videoCurrentPositionMs.toFloat() / videoDurationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        val displayFraction = if (isScrubbing) scrubPositionRatio else progressFraction

                        Slider(
                            value = displayFraction,
                            onValueChange = { ratio ->
                                isScrubbing = true
                                scrubPositionRatio = ratio
                            },
                            onValueChangeFinished = {
                                isScrubbing = false
                                if (videoDurationMs > 0L) {
                                    val targetMs = (scrubPositionRatio * videoDurationMs).toLong()
                                    videoViewInstance?.seekTo(targetMs.toInt())
                                    viewModel.seekVideo(targetMs)
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = NaturalSage,
                                activeTrackColor = NaturalSage,
                                inactiveTrackColor = NaturalSandBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                                .testTag("video_scrub_slider")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Timestamp Readout
                        Text(
                            text = "${formatDurationMs(videoCurrentPositionMs)} / ${formatDurationMs(videoDurationMs)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NaturalTextSecondary
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // URL Stream button
                        IconButton(
                            onClick = { showUrlDialog = true },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("video_bar_url_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Stream URL",
                                tint = NaturalTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Pick New Video button
                        IconButton(
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("change_video_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Change Video",
                                tint = NaturalTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Scenario Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeliveryPreset.entries.forEach { preset ->
                    val isSelected = selectedPreset == preset
                    Surface(
                        color = if (isSelected) NaturalSageLight else NaturalCardWhite,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NaturalSage else NaturalSandBorder
                        ),
                        modifier = Modifier
                            .clickable { viewModel.selectPreset(preset) }
                            .testTag("preset_${preset.name.lowercase(Locale.ROOT)}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = preset.title,
                                color = if (isSelected) NaturalSage else NaturalTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (isSelected) NaturalSage else NaturalSurface,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = preset.expectedCall,
                                    color = if (isSelected) Color.White else NaturalTextSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Bowl / Deliver Action Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = NaturalCardWhite),
                border = BorderStroke(1.dp, NaturalSandBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isVideoMode -> "Real Video Motion Tracker"
                                isCameraMode -> "Live Camera Optical Engine"
                                else -> selectedPreset.title
                            },
                            color = NaturalTextPrimary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = when {
                                isVideoMode -> "Optical motion differencing & physical velocity estimation on video frames"
                                isCameraMode -> "Live CameraX 60 FPS motion & trajectory analysis on pitch"
                                else -> selectedPreset.description
                            },
                            color = NaturalTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.resetDeliveryState() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(NaturalSurface, CircleShape)
                                .border(1.dp, NaturalSandBorder, CircleShape)
                                .testTag("reset_delivery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Delivery",
                                tint = NaturalTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (isVideoMode) {
                                    viewModel.analyzeVideoDelivery()
                                } else {
                                    viewModel.playPresetDelivery()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NaturalSage,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isSimulating,
                            modifier = Modifier.testTag("bowl_delivery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsCricket,
                                contentDescription = "Bowl",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSimulating) "TRACKING..." else if (isVideoMode) "ANALYZE VIDEO" else "BOWL BALL",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 7. Stream Video from URL Dialog
        if (showUrlDialog) {
            AlertDialog(
                onDismissRequest = { showUrlDialog = false },
                title = {
                    Text(
                        text = "Stream Video from URL",
                        fontWeight = FontWeight.Bold,
                        color = NaturalTextPrimary
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter any direct MP4 video URL to visualize bowling speed, pitch trajectory, and Hawkeye projection:",
                            style = MaterialTheme.typography.bodySmall,
                            color = NaturalTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { customUrlInput = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NaturalSage,
                                unfocusedBorderColor = NaturalSandBorder,
                                focusedTextColor = NaturalTextPrimary,
                                unfocusedTextColor = NaturalTextPrimary
                            ),
                            placeholder = { Text("https://.../cricket.mp4", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("video_url_input_field")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tip: Supports standard MP4, WebM, and RTSP video streams over HTTP/HTTPS.",
                            fontSize = 10.sp,
                            color = NaturalTextSecondary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = customUrlInput.trim()
                            if (trimmed.isNotBlank()) {
                                viewModel.loadUserVideo(Uri.parse(trimmed))
                            }
                            showUrlDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NaturalSage),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_video_url_button")
                    ) {
                        Text("STREAM VIDEO", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showUrlDialog = false },
                        modifier = Modifier.testTag("cancel_video_url_button")
                    ) {
                        Text("CANCEL", color = NaturalTextSecondary)
                    }
                },
                containerColor = NaturalCardWhite,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

data class PresetSampleVideo(
    val title: String,
    val subtitle: String,
    val url: String
)

val PRESET_SAMPLE_VIDEOS = listOf(
    PresetSampleVideo(
        title = "Pace Delivery Action",
        subtitle = "High speed bowling run-up & seam release (Sample MP4)",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
    ),
    PresetSampleVideo(
        title = "Stumps Impact Replay",
        subtitle = "Crease delivery line & pad collision (Sample MP4)",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
    ),
    PresetSampleVideo(
        title = "Nets Motion Tracking",
        subtitle = "Fast bowler motion & pitch landing (Sample MP4)",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"
    )
)

@Composable
private fun FeedSourcePill(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) NaturalSage else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.White else NaturalTextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else NaturalTextPrimary,
                maxLines = 1
            )
        }
    }
}

private fun formatDurationMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
