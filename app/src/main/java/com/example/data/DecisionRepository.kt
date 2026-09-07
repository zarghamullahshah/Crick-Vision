package com.example.data

import com.example.model.Decision
import kotlinx.coroutines.flow.Flow

class DecisionRepository(private val dao: DecisionLogDao) {

    val allDecisions: Flow<List<DecisionLogEntity>> = dao.getAllDecisions()
    val totalCount: Flow<Int> = dao.getTotalCount()

    fun getDecisionsByType(type: String): Flow<List<DecisionLogEntity>> = dao.getDecisionsByType(type)

    suspend fun logDecision(decision: Decision, presetName: String = "Live Delivery"): Long {
        val entity = DecisionLogEntity(
            decisionType = decision.type.name,
            confidence = decision.confidence,
            frameIndex = decision.frameIndex,
            reason = decision.reason,
            urduReason = decision.urduReason,
            timestamp = decision.timestamp,
            deliveryPreset = presetName
        )
        return dao.insertDecision(entity)
    }

    suspend fun deleteDecision(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }
}
