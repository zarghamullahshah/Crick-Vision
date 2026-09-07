package com.example.model

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset

enum class DecisionType(val displayName: String, val urduName: String) {
    OVERSTEP("OVERSTEP", "اوور سٹیپ"),
    NO_BALL("NO BALL", "نو بال"),
    WIDE("WIDE", "وائیڈ"),
    LBW_OUT("OUT (LBW)", "آؤٹ (ایل بی ڈبلیو)"),
    LBW_NOT_OUT("NOT OUT", "ناٹ آؤٹ"),
    FOUR("FOUR", "چار رنز"),
    SIX("SIX", "چھ رنز"),
    UMPIRE_REVIEW("UMPIRE REVIEW", "امپائر کا جائزہ"),
    LEGAL_BALL("LEGAL BALL", "درست گیند")
}

data class Decision(
    val type: DecisionType,
    val confidence: Float,
    val frameIndex: Int,
    val reason: String,
    val urduReason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Detection(
    val label: String = "ball",
    val boundingBox: RectF, // Normalized 0.0 - 1.0 coords
    val confidence: Float
) {
    val center: Offset
        get() = Offset(boundingBox.centerX(), boundingBox.centerY())
}

data class PoseLandmarks(
    val frontFootAnkle: Offset? = null,   // for no-ball popping crease evaluation
    val frontFootToe: Offset? = null,
    val frontFootContact: Offset? = null,
    val isOverstepping: Boolean = false,
    val creaseDistanceCm: Float = 0f,    // Positive = behind line (legal), Negative = overstepping line
    val battingHandWrist: Offset? = null, // proxy for bat position (wide rule evaluation)
    val frontLegKnee: Offset? = null,     // proxy for pad impact point (LBW heuristic)
    val frontLegAnkle: Offset? = null
)

data class FrameResult(
    val frameIndex: Int,
    val timestampMs: Long,
    val ballDetections: List<Detection>,
    val pose: PoseLandmarks? = null
)
