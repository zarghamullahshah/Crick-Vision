package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DecisionLogDao {

    @Query("SELECT * FROM decision_logs ORDER BY timestamp DESC")
    fun getAllDecisions(): Flow<List<DecisionLogEntity>>

    @Query("SELECT * FROM decision_logs WHERE decisionType = :type ORDER BY timestamp DESC")
    fun getDecisionsByType(type: String): Flow<List<DecisionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: DecisionLogEntity): Long

    @Query("DELETE FROM decision_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM decision_logs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM decision_logs")
    fun getTotalCount(): Flow<Int>
}
