package com.example.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryFactDao {
    @Query("SELECT * FROM memory_facts ORDER BY timestamp DESC")
    fun getAllFacts(): Flow<List<MemoryFactEntity>>

    @Query("SELECT * FROM memory_facts WHERE category = :category ORDER BY timestamp DESC")
    fun getFactsByCategory(category: String): Flow<List<MemoryFactEntity>>

    @Query("SELECT * FROM memory_facts ORDER BY importance DESC, timestamp DESC LIMIT :limit")
    suspend fun getTopFacts(limit: Int = 20): List<MemoryFactEntity>

    @Query("SELECT * FROM memory_facts WHERE `key` LIKE '%' || :query || '%' OR `value` LIKE '%' || :query || '%'")
    suspend fun searchFacts(query: String): List<MemoryFactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFact(fact: MemoryFactEntity): Long

    @Query("DELETE FROM memory_facts WHERE id = :id")
    suspend fun deleteFact(id: Long)

    @Query("DELETE FROM memory_facts")
    suspend fun clearAll()
}
