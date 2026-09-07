package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decision_logs")
data class DecisionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val decisionType: String,
    val confidence: Float,
    val frameIndex: Int,
    val reason: String,
    val urduReason: String,
    val timestamp: Long = System.currentTimeMillis(),
    val deliveryPreset: String = "Live Delivery"
)
