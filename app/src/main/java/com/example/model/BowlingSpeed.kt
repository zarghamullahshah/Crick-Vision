package com.example.model

import java.util.Locale

/**
 * Real-time bowling velocity data structure tracking release speed,
 * pitch bounce speed, and impact velocity at the stumps.
 */
data class BowlingSpeed(
    val currentKmh: Float = 0f,
    val currentMph: Float = 0f,
    val releaseKmh: Float = 0f,
    val bounceKmh: Float = 0f,
    val impactKmh: Float = 0f,
    val paceCategory: String = "IDLE",
    val isLive: Boolean = false,
    val speedHistory: List<Float> = emptyList()
) {
    fun formattedCurrentKmh(): String = if (currentKmh > 0f) String.format(Locale.US, "%.1f", currentKmh) else "--.-"
    fun formattedCurrentMph(): String = if (currentMph > 0f) String.format(Locale.US, "%.1f", currentMph) else "--.-"
    fun formattedReleaseKmh(): String = if (releaseKmh > 0f) String.format(Locale.US, "%.1f", releaseKmh) else "--.-"
    fun formattedBounceKmh(): String = if (bounceKmh > 0f) String.format(Locale.US, "%.1f", bounceKmh) else "--.-"
    fun formattedImpactKmh(): String = if (impactKmh > 0f) String.format(Locale.US, "%.1f", impactKmh) else "--.-"

    companion object {
        fun default(): BowlingSpeed = BowlingSpeed(
            currentKmh = 0f,
            currentMph = 0f,
            releaseKmh = 141.2f,
            bounceKmh = 124.8f,
            impactKmh = 118.5f,
            paceCategory = "FAST BOWLER",
            isLive = false
        )

        fun categorizePace(kmh: Float): String = when {
            kmh >= 145f -> "EXPRESS FAST ⚡"
            kmh >= 130f -> "FAST BOWLER 💨"
            kmh >= 115f -> "FAST-MEDIUM 🎯"
            kmh >= 100f -> "MEDIUM PACER 🏏"
            kmh > 0f -> "SPIN / FLIGHT 🔄"
            else -> "AWAITING BOWL"
        }
    }
}
