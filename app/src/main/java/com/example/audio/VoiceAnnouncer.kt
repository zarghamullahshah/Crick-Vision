package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.model.Decision
import com.example.model.DecisionType
import java.util.Locale

/**
 * Text-to-Speech audio announcer for cricket umpire decisions.
 * Supports English (en-US) and Urdu (ur-PK) speech output.
 */
class VoiceAnnouncer(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isUrduSupported = false
    var isEnabled: Boolean = true
    var useUrdu: Boolean = false

    private val urduLocale = Locale("ur", "PK")

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("VoiceAnnouncer", "Error initializing TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val resultEn = engine.setLanguage(Locale.US)
                if (resultEn == TextToSpeech.LANG_MISSING_DATA || resultEn == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.getDefault())
                }

                // Check Urdu availability
                val resultUr = engine.isLanguageAvailable(urduLocale)
                isUrduSupported = resultUr >= TextToSpeech.LANG_AVAILABLE
                isInitialized = true
            }
        }
    }

    fun announce(decision: Decision, forceUrdu: Boolean = useUrdu) {
        if (!isEnabled || !isInitialized || tts == null) return

        val textToSpeak = if (forceUrdu) {
            getUrduAnnouncement(decision.type)
        } else {
            getEnglishAnnouncement(decision.type)
        }

        val targetLocale = if (forceUrdu && isUrduSupported) urduLocale else Locale.US
        tts?.language = targetLocale
        tts?.setSpeechRate(1.05f)

        // TextToSpeech.QUEUE_FLUSH ensures immediate authoritative umpire announcement
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "decision_${decision.frameIndex}")
    }

    private fun getEnglishAnnouncement(type: DecisionType): String {
        return when (type) {
            DecisionType.OVERSTEP -> "Overstep! No ball!"
            DecisionType.NO_BALL -> "No ball!"
            DecisionType.WIDE -> "Wide ball!"
            DecisionType.LBW_OUT -> "Out! LBW!"
            DecisionType.LBW_NOT_OUT -> "Not out."
            DecisionType.FOUR -> "Four runs!"
            DecisionType.SIX -> "Six runs!"
            DecisionType.UMPIRE_REVIEW -> "Umpire review, not out."
            DecisionType.LEGAL_BALL -> "Legal delivery."
        }
    }

    private fun getUrduAnnouncement(type: DecisionType): String {
        return when (type) {
            DecisionType.OVERSTEP -> "اوور سٹیپ! نو بال!"
            DecisionType.NO_BALL -> "نو بال!"
            DecisionType.WIDE -> "وائیڈ بال!"
            DecisionType.LBW_OUT -> "آؤٹ! ایل بی ڈبلیو!"
            DecisionType.LBW_NOT_OUT -> "ناٹ آؤٹ۔"
            DecisionType.FOUR -> "چار رنز!"
            DecisionType.SIX -> "چھ رنز!"
            DecisionType.UMPIRE_REVIEW -> "امپائر کا جائزہ، ناٹ آؤٹ۔"
            DecisionType.LEGAL_BALL -> "درست گیند۔"
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
