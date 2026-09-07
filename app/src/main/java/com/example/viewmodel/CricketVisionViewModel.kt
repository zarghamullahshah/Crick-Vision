package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.VoiceAnnouncer
import com.example.data.CalibrationPreferences
import com.example.data.CricketVisionDatabase
import com.example.data.DecisionLogEntity
import com.example.data.DecisionRepository
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
import com.example.vision.RealTimeVisionAnalyzer
import com.example.vision.VideoFrameExtractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

sealed class CalibrationStep(val stepNumber: Int, val title: String, val instructions: String) {
    object Step1Corners : CalibrationStep(
        1,
        "Pitch Corners (4 Points)",
        "Tap 4 pitch corners in order: Near-Left, Near-Right, Far-Right, Far-Left"
    )
    object Step2Stump : CalibrationStep(
        2,
        "Stump Base (1 Point)",
        "Tap the batsman's stump base (wickets) position on the pitch"
    )
    object Step3WideLine : CalibrationStep(
        3,
        "Wide Lines (2 Points)",
        "Tap left edge and right edge of the batsman's wide crease line"
    )
    object Completed : CalibrationStep(
        4,
        "Calibration Ready",
        "Pitch geometry calibrated and ready for auto-umpire analysis"
    )
}

enum class DeliveryPreset(val title: String, val description: String, val expectedCall: String) {
    OVERSTEP_NO_BALL(
        "Overstepping No-Ball",
        "Bowler's front foot lands across the popping crease line",
        "NO BALL"
    ),
    DOWN_LEG_WIDE(
        "Down Leg-Side Wide",
        "Ball passes well outside the batsman's leg-stump wide marker",
        "WIDE"
    ),
    INSWING_LBW_OUT(
        "Inswinger LBW (Out)",
        "Pitches in line, hits batsman pad in front, projected to hit middle stump",
        "OUT (LBW)"
    ),
    EDGE_BAT_REVIEW(
        "Outside-Off Bat Edge",
        "Ball passes near wide line but adjacent to batsman's wrist/bat proxy",
        "UMPIRE REVIEW"
    ),
    LOFTED_SIX(
        "Lofted Straight Drive",
        "Ball hit airborne cleanly crossing the boundary line on the full",
        "SIX"
    ),
    GROUND_FOUR(
        "Cover Drive Boundary",
        "Ball stroked along turf, bouncing before crossing boundary line",
        "FOUR"
    ),
    LEGAL_YORKER(
        "Toe-Crushing Yorker",
        "Legal delivery landing right at the batsman's crease behind the popping line",
        "LEGAL BALL"
    )
}

class CricketVisionViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = CalibrationPreferences(application)
    private val database = CricketVisionDatabase.getDatabase(application)
    val repository = DecisionRepository(database.decisionLogDao())
    val voiceAnnouncer = VoiceAnnouncer(application)

    private val _calibration = MutableStateFlow(preferences.loadCalibration())
    val calibration: StateFlow<PitchCalibration> = _calibration.asStateFlow()

    private val _calibrationStep = MutableStateFlow<CalibrationStep>(CalibrationStep.Completed)
    val calibrationStep: StateFlow<CalibrationStep> = _calibrationStep.asStateFlow()

    private val _tempCalibrationCorners = MutableStateFlow<List<Offset>>(emptyList())
    val tempCalibrationCorners: StateFlow<List<Offset>> = _tempCalibrationCorners.asStateFlow()

    private val _tempStumpBase = MutableStateFlow<Offset?>(null)
    val tempStumpBase: StateFlow<Offset?> = _tempStumpBase.asStateFlow()

    private val _tempWideLine = MutableStateFlow<List<Offset>>(emptyList())
    val tempWideLine: StateFlow<List<Offset>> = _tempWideLine.asStateFlow()

    // Rules Engine & Real-Time Vision instances
    private val ballTracker = BallTracker()
    private val noBallRule = NoBallRule(_calibration.value.poppingCrease)
    private val wideRule = WideRule(_calibration.value.wideLine)
    private val boundaryRule = BoundaryRule(listOf(_calibration.value.pitchCorners[3], _calibration.value.pitchCorners[2]))
    private val lbwHeuristic = LbwHeuristic()
    private val realTimeAnalyzer = RealTimeVisionAnalyzer(_calibration.value)
    private val videoFrameExtractor = VideoFrameExtractor(application)
    private var videoAnalysisJob: Job? = null

    // Live State
    private val _activeDecision = MutableStateFlow<Decision?>(null)
    val activeDecision: StateFlow<Decision?> = _activeDecision.asStateFlow()

    private val _currentDetection = MutableStateFlow<Detection?>(null)
    val currentDetection: StateFlow<Detection?> = _currentDetection.asStateFlow()

    private val _trajectoryHistory = MutableStateFlow<List<Offset>>(emptyList())
    val trajectoryHistory: StateFlow<List<Offset>> = _trajectoryHistory.asStateFlow()

    private val _projectedTrajectory = MutableStateFlow<List<Offset>>(emptyList())
    val projectedTrajectory: StateFlow<List<Offset>> = _projectedTrajectory.asStateFlow()

    private val _poseLandmarks = MutableStateFlow<PoseLandmarks?>(null)
    val poseLandmarks: StateFlow<PoseLandmarks?> = _poseLandmarks.asStateFlow()

    private val _pitchBouncePoint = MutableStateFlow<Offset?>(null)
    val pitchBouncePoint: StateFlow<Offset?> = _pitchBouncePoint.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _selectedPreset = MutableStateFlow(DeliveryPreset.OVERSTEP_NO_BALL)
    val selectedPreset: StateFlow<DeliveryPreset> = _selectedPreset.asStateFlow()

    private val _fps = MutableStateFlow(30)
    val fps: StateFlow<Int> = _fps.asStateFlow()

    private val _isCameraMode = MutableStateFlow(false)
    val isCameraMode: StateFlow<Boolean> = _isCameraMode.asStateFlow()

    // Real-Time Bowling Speed State
    private val _bowlingSpeed = MutableStateFlow(BowlingSpeed.default())
    val bowlingSpeed: StateFlow<BowlingSpeed> = _bowlingSpeed.asStateFlow()

    // Real-World Video Visualizer State
    private val _userVideoUri = MutableStateFlow<Uri?>(null)
    val userVideoUri: StateFlow<Uri?> = _userVideoUri.asStateFlow()

    private val _isVideoMode = MutableStateFlow(false)
    val isVideoMode: StateFlow<Boolean> = _isVideoMode.asStateFlow()

    private val _isVideoPlaying = MutableStateFlow(true)
    val isVideoPlaying: StateFlow<Boolean> = _isVideoPlaying.asStateFlow()

    private val _videoDurationMs = MutableStateFlow(0L)
    val videoDurationMs: StateFlow<Long> = _videoDurationMs.asStateFlow()

    private val _videoCurrentPositionMs = MutableStateFlow(0L)
    val videoCurrentPositionMs: StateFlow<Long> = _videoCurrentPositionMs.asStateFlow()

    private val _useUrdu = MutableStateFlow(preferences.isUseUrdu())
    val useUrdu: StateFlow<Boolean> = _useUrdu.asStateFlow()

    private val _voiceEnabled = MutableStateFlow(preferences.isVoiceEnabled())
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled.asStateFlow()

    val decisionHistory: StateFlow<List<DecisionLogEntity>> = repository.allDecisions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var deliveryJob: Job? = null
    private var bannerDismissJob: Job? = null

    init {
        voiceAnnouncer.useUrdu = _useUrdu.value
        voiceAnnouncer.isEnabled = _voiceEnabled.value
        updateRulesWithCalibration(_calibration.value)
    }

    private fun updateRulesWithCalibration(cal: PitchCalibration) {
        noBallRule.updateCreaseLine(cal.poppingCrease)
        wideRule.updateWideLine(cal.wideLine)
        if (cal.pitchCorners.size >= 4) {
            boundaryRule.updateBoundaryLine(listOf(cal.pitchCorners[3], cal.pitchCorners[2]))
        }
        realTimeAnalyzer.updateCalibration(cal)
    }

    fun setCameraMode(enabled: Boolean) {
        _isCameraMode.value = enabled
        if (enabled) {
            _isVideoMode.value = false
            realTimeAnalyzer.reset()
        }
    }

    /**
     * Real-time CameraX frame processing directly from hardware camera sensor.
     */
    fun processCameraImage(imageProxy: ImageProxy) {
        if (!_isCameraMode.value) {
            imageProxy.close()
            return
        }

        val result = realTimeAnalyzer.analyzeCameraFrame(imageProxy) ?: return

        _currentDetection.value = result.detection
        if (result.trajectory.isNotEmpty()) {
            _trajectoryHistory.value = result.trajectory
        }
        if (result.projectedTrajectory.isNotEmpty()) {
            _projectedTrajectory.value = result.projectedTrajectory
        }
        if (result.bouncePoint != null) {
            _pitchBouncePoint.value = result.bouncePoint
        }
        if (result.speed.currentKmh > 0f) {
            _bowlingSpeed.value = result.speed
        }
        if (result.poseLandmarks != null) {
            _poseLandmarks.value = result.poseLandmarks
        }
        if (result.decision != null && _activeDecision.value == null) {
            triggerDecision(result.decision, "Live Camera Tracking")
        }
    }

    fun loadUserVideo(uri: Uri) {
        _userVideoUri.value = uri
        _isVideoMode.value = true
        _isCameraMode.value = false
        _isVideoPlaying.value = true
        realTimeAnalyzer.reset()
    }

    fun setVideoMode(enabled: Boolean) {
        _isVideoMode.value = enabled
        if (enabled) {
            _isCameraMode.value = false
            realTimeAnalyzer.reset()
        }
    }

    fun toggleVideoPlayback() {
        _isVideoPlaying.value = !_isVideoPlaying.value
    }

    fun updateVideoProgress(currentMs: Long, totalMs: Long) {
        _videoCurrentPositionMs.value = currentMs
        _videoDurationMs.value = totalMs
    }

    fun seekVideo(targetMs: Long) {
        _videoCurrentPositionMs.value = targetMs
    }

    /**
     * Real Video Analysis: Extracts actual video frames from user video or stream
     * using MediaMetadataRetriever and runs computer vision trajectory + velocity tracking.
     */
    fun analyzeVideoDelivery() {
        val uri = _userVideoUri.value
        if (uri != null) {
            resetDeliveryState()
            _isSimulating.value = true
            videoAnalysisJob?.cancel()
            videoAnalysisJob = viewModelScope.launch {
                val success = videoFrameExtractor.analyzeVideo(
                    videoUri = uri,
                    analyzer = realTimeAnalyzer,
                    onFrameResult = { frameResult, curMs, totalMs ->
                        _videoCurrentPositionMs.value = curMs
                        _videoDurationMs.value = totalMs
                        _currentDetection.value = frameResult.detection
                        if (frameResult.trajectory.isNotEmpty()) {
                            _trajectoryHistory.value = frameResult.trajectory
                        }
                        if (frameResult.projectedTrajectory.isNotEmpty()) {
                            _projectedTrajectory.value = frameResult.projectedTrajectory
                        }
                        if (frameResult.bouncePoint != null) {
                            _pitchBouncePoint.value = frameResult.bouncePoint
                        }
                        if (frameResult.speed.currentKmh > 0f) {
                            _bowlingSpeed.value = frameResult.speed
                        }
                        if (frameResult.poseLandmarks != null) {
                            _poseLandmarks.value = frameResult.poseLandmarks
                        }
                        if (frameResult.decision != null && _activeDecision.value == null) {
                            triggerDecision(frameResult.decision, "Video Delivery Vision")
                        }
                    }
                )
                _isSimulating.value = false
                if (!success) {
                    playPresetDelivery(_selectedPreset.value)
                }
            }
        } else {
            playPresetDelivery(_selectedPreset.value)
        }
    }

    fun toggleLanguage() {
        val newUrdu = !_useUrdu.value
        _useUrdu.value = newUrdu
        preferences.setUseUrdu(newUrdu)
        voiceAnnouncer.useUrdu = newUrdu
    }

    fun toggleVoice() {
        val newVoice = !_voiceEnabled.value
        _voiceEnabled.value = newVoice
        preferences.setVoiceEnabled(newVoice)
        voiceAnnouncer.isEnabled = newVoice
    }

    fun selectPreset(preset: DeliveryPreset) {
        _selectedPreset.value = preset
        playPresetDelivery(preset)
    }

    fun resetDeliveryState() {
        deliveryJob?.cancel()
        videoAnalysisJob?.cancel()
        _isSimulating.value = false
        ballTracker.reset()
        realTimeAnalyzer.reset()
        noBallRule.resetForNewDelivery()
        wideRule.resetForNewDelivery()
        boundaryRule.resetForNewDelivery()
        _currentDetection.value = null
        _trajectoryHistory.value = emptyList()
        _projectedTrajectory.value = emptyList()
        _poseLandmarks.value = null
        _pitchBouncePoint.value = null
        _bowlingSpeed.value = _bowlingSpeed.value.copy(
            currentKmh = 0f,
            currentMph = 0f,
            isLive = false
        )
    }

    fun dismissActiveDecision() {
        _activeDecision.value = null
    }

    private fun triggerDecision(decision: Decision, presetName: String = "Live Delivery") {
        _activeDecision.value = decision
        voiceAnnouncer.announce(decision)

        // Save to Room DB
        viewModelScope.launch {
            repository.logDecision(decision, presetName)
        }

        bannerDismissJob?.cancel()
        bannerDismissJob = viewModelScope.launch {
            delay(3500)
            if (_activeDecision.value == decision) {
                _activeDecision.value = null
            }
        }
    }

    // --- Delivery Simulator Engine ---
    fun playPresetDelivery(preset: DeliveryPreset = _selectedPreset.value) {
        resetDeliveryState()
        _selectedPreset.value = preset
        _isSimulating.value = true

        deliveryJob = viewModelScope.launch {
            val cal = _calibration.value
            val creaseY = cal.poppingCrease[0].y
            val stump = cal.stumpBase
            val wideLeft = cal.wideLine[0].x
            val wideRight = cal.wideLine[1].x

            when (preset) {
                DeliveryPreset.OVERSTEP_NO_BALL -> {
                    // Bowler oversteps popping crease by landing past it
                    val illegalFootY = creaseY - 0.04f
                    val footPoint = Offset(0.48f, illegalFootY)
                    val footAnalysis = noBallRule.evaluateWithAnalysis(footPoint, 12)

                    _poseLandmarks.value = PoseLandmarks(
                        frontFootAnkle = footPoint,
                        frontFootToe = Offset(0.48f, illegalFootY - 0.015f),
                        frontFootContact = footPoint,
                        isOverstepping = footAnalysis.isOverstep,
                        creaseDistanceCm = footAnalysis.signedDistanceCm
                    )
                    delay(300)

                    val noBallDecision = footAnalysis.decision ?: noBallRule.evaluate(footPoint, 12)
                    if (noBallDecision != null) {
                        triggerDecision(noBallDecision, preset.title)
                    }

                    // Ball delivery trajectory
                    simulateBallTrajectory(
                        start = Offset(0.50f, illegalFootY),
                        bounce = Offset(0.50f, 0.45f),
                        end = Offset(0.50f, stump.y),
                        totalFrames = 18
                    )
                }

                DeliveryPreset.DOWN_LEG_WIDE -> {
                    // Legal front foot
                    _poseLandmarks.value = PoseLandmarks(
                        frontFootAnkle = Offset(0.46f, creaseY + 0.03f),
                        battingHandWrist = Offset(0.46f, stump.y)
                    )
                    delay(200)

                    // Ball delivered wide of leg-stump
                    val legWideX = wideRight + 0.06f
                    simulateBallTrajectory(
                        start = Offset(0.48f, creaseY),
                        bounce = Offset(0.58f, 0.48f),
                        end = Offset(legWideX, stump.y - 0.02f),
                        totalFrames = 20,
                        onFrame = { frameIdx, pos ->
                            if (pos.y <= stump.y + 0.02f) {
                                val det = Detection(boundingBox = android.graphics.RectF(pos.x - 0.02f, pos.y - 0.02f, pos.x + 0.02f, pos.y + 0.02f), confidence = 0.94f)
                                val decision = wideRule.evaluate(det, _poseLandmarks.value?.battingHandWrist, true, frameIdx)
                                if (decision != null) {
                                    triggerDecision(decision, preset.title)
                                }
                            }
                        }
                    )
                }

                DeliveryPreset.INSWING_LBW_OUT -> {
                    // Legal foot release
                    _poseLandmarks.value = PoseLandmarks(
                        frontFootAnkle = Offset(0.48f, creaseY + 0.03f),
                        battingHandWrist = Offset(0.44f, stump.y + 0.03f),
                        frontLegKnee = Offset(0.50f, stump.y + 0.04f),
                        frontLegAnkle = Offset(0.50f, stump.y + 0.08f)
                    )
                    delay(200)

                    val bounce = Offset(0.51f, 0.46f)
                    val impact = Offset(0.50f, stump.y + 0.04f)

                    simulateBallTrajectory(
                        start = Offset(0.46f, creaseY),
                        bounce = bounce,
                        end = impact,
                        totalFrames = 18
                    )

                    // Run LBW Heuristic at pad impact point
                    _pitchBouncePoint.value = bounce
                    val projPath = listOf(
                        impact,
                        Offset(stump.x, stump.y)
                    )
                    _projectedTrajectory.value = projPath

                    val lbwDecision = lbwHeuristic.evaluate(
                        pitchPoint = bounce,
                        estimatedImpactPoint = impact,
                        stumpBase = stump,
                        tracker = ballTracker,
                        frameIndex = 24
                    )
                    triggerDecision(lbwDecision, preset.title)
                }

                DeliveryPreset.EDGE_BAT_REVIEW -> {
                    // Legal foot, wrist very close to ball path at stump plane
                    val ballX = wideLeft - 0.02f
                    _poseLandmarks.value = PoseLandmarks(
                        frontFootAnkle = Offset(0.48f, creaseY + 0.03f),
                        battingHandWrist = Offset(ballX + 0.03f, stump.y + 0.01f)
                    )
                    delay(200)

                    simulateBallTrajectory(
                        start = Offset(0.48f, creaseY),
                        bounce = Offset(0.42f, 0.46f),
                        end = Offset(ballX, stump.y),
                        totalFrames = 20,
                        onFrame = { frameIdx, pos ->
                            if (pos.y <= stump.y + 0.02f) {
                                val det = Detection(boundingBox = android.graphics.RectF(pos.x - 0.02f, pos.y - 0.02f, pos.x + 0.02f, pos.y + 0.02f), confidence = 0.90f)
                                val decision = wideRule.evaluate(det, _poseLandmarks.value?.battingHandWrist, true, frameIdx)
                                if (decision != null) {
                                    triggerDecision(decision, preset.title)
                                }
                            }
                        }
                    )
                }

                DeliveryPreset.LOFTED_SIX -> {
                    _poseLandmarks.value = PoseLandmarks(
                        frontFootAnkle = Offset(0.48f, creaseY + 0.02f),
                        battingHandWrist = Offset(0.50f, stump.y)
                    )
                    delay(200)

                    val boundaryY = cal.pitchCorners[3].y
                    simulateBallTrajectory(
                        start = Offset(0.50f, stump.y),
                        bounce = null, // Airborne all the way
                        end = Offset(0.50f, boundaryY - 0.06f),
                        totalFrames = 22,
                        onFrame = { frameIdx, pos ->
                            if (pos.y <= boundaryY) {
                                val decision = boundaryRule.evaluate(_trajectoryHistory.value, wasAirborne = true, frameIndex = frameIdx)
                                if (decision != null) {
                                    triggerDecision(decision, preset.title)
                                }
                            }
                        }
                    )
                }

                DeliveryPreset.GROUND_FOUR -> {
                    _poseLandmarks.value = PoseLandmarks(
                        frontFootAnkle = Offset(0.48f, creaseY + 0.02f),
                        battingHandWrist = Offset(0.50f, stump.y)
                    )
                    delay(200)

                    val boundaryY = cal.pitchCorners[2].y
                    simulateBallTrajectory(
                        start = Offset(0.50f, stump.y),
                        bounce = Offset(0.60f, (stump.y + boundaryY) / 2f),
                        end = Offset(0.72f, boundaryY - 0.03f),
                        totalFrames = 22,
                        onFrame = { frameIdx, pos ->
                            if (pos.y <= boundaryY) {
                                val decision = boundaryRule.evaluate(_trajectoryHistory.value, wasAirborne = false, frameIndex = frameIdx)
                                if (decision != null) {
                                    triggerDecision(decision, preset.title)
                                }
                            }
                        }
                    )
                }

                DeliveryPreset.LEGAL_YORKER -> {
                    _poseLandmarks.value = PoseLandmarks(
                        frontFootAnkle = Offset(0.48f, creaseY + 0.03f),
                        battingHandWrist = Offset(0.46f, stump.y + 0.02f)
                    )
                    delay(200)

                    val bounce = Offset(0.50f, stump.y + 0.01f)
                    simulateBallTrajectory(
                        start = Offset(0.48f, creaseY),
                        bounce = bounce,
                        end = Offset(0.50f, stump.y),
                        totalFrames = 18
                    )
                    _pitchBouncePoint.value = bounce
                    val legalDecision = Decision(
                        type = DecisionType.LEGAL_BALL,
                        confidence = 0.98f,
                        frameIndex = 20,
                        reason = "Legal delivery: Bowler behind crease, line within wide guidelines, cleanly tracked",
                        urduReason = "درست گیند: بولر کریز کے اندر، لائن درست، اور باقاعدہ ٹریکنگ"
                    )
                    triggerDecision(legalDecision, preset.title)
                }
            }

            _isSimulating.value = false
        }
    }

    private suspend fun simulateBallTrajectory(
        start: Offset,
        bounce: Offset?,
        end: Offset,
        totalFrames: Int,
        onFrame: ((Int, Offset) -> Unit)? = null
    ) {
        val path = mutableListOf<Offset>()
        val halfFrames = if (bounce != null) totalFrames / 2 else totalFrames

        val baseSpeed = when (_selectedPreset.value) {
            DeliveryPreset.LEGAL_YORKER -> 146.4f
            DeliveryPreset.OVERSTEP_NO_BALL -> 141.8f
            DeliveryPreset.INSWING_LBW_OUT -> 138.6f
            DeliveryPreset.DOWN_LEG_WIDE -> 133.5f
            DeliveryPreset.EDGE_BAT_REVIEW -> 136.2f
            DeliveryPreset.GROUND_FOUR -> 132.8f
            DeliveryPreset.LOFTED_SIX -> 128.4f
        }

        val recordedReleaseSpeed = baseSpeed
        var recordedBounceSpeed = 0f
        var recordedImpactSpeed = 0f

        _bowlingSpeed.value = BowlingSpeed(
            currentKmh = baseSpeed,
            currentMph = baseSpeed * 0.621371f,
            releaseKmh = baseSpeed,
            bounceKmh = 0f,
            impactKmh = 0f,
            paceCategory = BowlingSpeed.categorizePace(baseSpeed),
            isLive = true
        )

        for (i in 0..totalFrames) {
            val t = i.toFloat() / totalFrames
            val currentPos = if (bounce == null) {
                Offset(
                    start.x + (end.x - start.x) * t,
                    start.y + (end.y - start.y) * t
                )
            } else {
                // Parabolic / piecewise bounce curve
                if (i <= halfFrames) {
                    val subT = i.toFloat() / halfFrames
                    Offset(
                        start.x + (bounce.x - start.x) * subT,
                        start.y + (bounce.y - start.y) * subT
                    )
                } else {
                    val subT = (i - halfFrames).toFloat() / (totalFrames - halfFrames)
                    Offset(
                        bounce.x + (end.x - bounce.x) * subT,
                        bounce.y + (end.y - bounce.y) * subT
                    )
                }
            }

            ballTracker.update(currentPos)
            path.add(currentPos)
            _trajectoryHistory.value = path.toList()
            _currentDetection.value = Detection(
                label = "ball",
                boundingBox = android.graphics.RectF(
                    currentPos.x - 0.02f,
                    currentPos.y - 0.02f,
                    currentPos.x + 0.02f,
                    currentPos.y + 0.02f
                ),
                confidence = 0.92f
            )

            // Dynamic velocity calculation
            val liveSpeed = when {
                i < halfFrames -> {
                    val drop = (i.toFloat() / halfFrames) * 3.5f
                    baseSpeed - drop
                }
                i == halfFrames -> {
                    recordedBounceSpeed = baseSpeed * 0.865f
                    recordedBounceSpeed
                }
                else -> {
                    val bSpeed = if (recordedBounceSpeed > 0f) recordedBounceSpeed else baseSpeed * 0.865f
                    val postT = (i - halfFrames).toFloat() / (totalFrames - halfFrames)
                    val postSpeed = bSpeed - (postT * 4.2f)
                    if (i == totalFrames) {
                        recordedImpactSpeed = postSpeed
                    }
                    postSpeed
                }
            }

            _bowlingSpeed.value = BowlingSpeed(
                currentKmh = liveSpeed,
                currentMph = liveSpeed * 0.621371f,
                releaseKmh = recordedReleaseSpeed,
                bounceKmh = recordedBounceSpeed,
                impactKmh = if (i == totalFrames) recordedImpactSpeed else 0f,
                paceCategory = BowlingSpeed.categorizePace(recordedReleaseSpeed),
                isLive = (i < totalFrames)
            )

            onFrame?.invoke(i, currentPos)
            delay(45) // ~22 FPS real-time simulation interval
        }

        // Final delivery speed summary
        _bowlingSpeed.value = _bowlingSpeed.value.copy(
            currentKmh = recordedImpactSpeed,
            currentMph = recordedImpactSpeed * 0.621371f,
            impactKmh = recordedImpactSpeed,
            isLive = false
        )
    }

    // --- Pitch Calibration Handling ---
    fun startCalibration() {
        _tempCalibrationCorners.value = emptyList()
        _tempStumpBase.value = null
        _tempWideLine.value = emptyList()
        _calibrationStep.value = CalibrationStep.Step1Corners
    }

    fun cancelCalibration() {
        _calibrationStep.value = CalibrationStep.Completed
        _tempCalibrationCorners.value = emptyList()
        _tempStumpBase.value = null
        _tempWideLine.value = emptyList()
    }

    fun handleCalibrationTap(point: Offset) {
        when (_calibrationStep.value) {
            is CalibrationStep.Step1Corners -> {
                val current = _tempCalibrationCorners.value.toMutableList()
                if (current.size < 4) {
                    current.add(point)
                    _tempCalibrationCorners.value = current
                    if (current.size == 4) {
                        _calibrationStep.value = CalibrationStep.Step2Stump
                    }
                }
            }
            is CalibrationStep.Step2Stump -> {
                _tempStumpBase.value = point
                _calibrationStep.value = CalibrationStep.Step3WideLine
            }
            is CalibrationStep.Step3WideLine -> {
                val current = _tempWideLine.value.toMutableList()
                if (current.size < 2) {
                    current.add(point)
                    _tempWideLine.value = current
                    if (current.size == 2) {
                        finishCalibration()
                    }
                }
            }
            is CalibrationStep.Completed -> {}
        }
    }

    private fun finishCalibration() {
        val corners = _tempCalibrationCorners.value
        val stump = _tempStumpBase.value ?: Offset(0.50f, 0.28f)
        val wide = _tempWideLine.value
        if (corners.size == 4 && wide.size == 2) {
            val newCal = PitchCalibration(
                pitchCorners = corners,
                stumpBase = stump,
                wideLine = wide,
                poppingCrease = listOf(corners[0], corners[1])
            )
            _calibration.value = newCal
            preferences.saveCalibration(newCal)
            updateRulesWithCalibration(newCal)
            _calibrationStep.value = CalibrationStep.Completed
        }
    }

    fun resetToDefaultCalibration() {
        val def = PitchCalibration.default()
        _calibration.value = def
        preferences.saveCalibration(def)
        updateRulesWithCalibration(def)
        _calibrationStep.value = CalibrationStep.Completed
    }

    fun deleteDecisionLog(id: Long) {
        viewModelScope.launch {
            repository.deleteDecision(id)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceAnnouncer.shutdown()
    }
}
