package com.example.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolSettingDao {
    @Query("SELECT * FROM tool_settings")
    fun getAllSettings(): Flow<List<ToolSettingEntity>>

    @Query("SELECT * FROM tool_settings WHERE toolName = :toolName LIMIT 1")
    suspend fun getSetting(toolName: String): ToolSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: ToolSettingEntity)

    @Query("UPDATE tool_settings SET isEnabled = :enabled WHERE toolName = :toolName")
    suspend fun setEnabled(toolName: String, enabled: Boolean)
}
