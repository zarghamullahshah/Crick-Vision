package com.example.data

import android.content.Context
import com.example.model.PitchCalibration

class CalibrationPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("cricket_vision_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CALIBRATION_JSON = "pitch_calibration_json"
        private const val KEY_USE_URDU = "use_urdu_voice"
        private const val KEY_VOICE_ENABLED = "voice_enabled"
        private const val KEY_CONFIDENCE_THRESHOLD = "confidence_threshold"
    }

    fun saveCalibration(calibration: PitchCalibration) {
        prefs.edit().putString(KEY_CALIBRATION_JSON, calibration.toJson()).apply()
    }

    fun loadCalibration(): PitchCalibration {
        val json = prefs.getString(KEY_CALIBRATION_JSON, null)
        return if (!json.isNullOrEmpty()) {
            PitchCalibration.fromJson(json)
        } else {
            PitchCalibration.default()
        }
    }

    fun setUseUrdu(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_URDU, enabled).apply()
    }

    fun isUseUrdu(): Boolean = prefs.getBoolean(KEY_USE_URDU, false)

    fun setVoiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_ENABLED, enabled).apply()
    }

    fun isVoiceEnabled(): Boolean = prefs.getBoolean(KEY_VOICE_ENABLED, true)
}
