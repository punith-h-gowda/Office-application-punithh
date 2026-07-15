package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MacroDao {
    @Query("SELECT * FROM macros ORDER BY id ASC")
    fun getAllMacros(): Flow<List<Macro>>

    @Query("SELECT * FROM macros WHERE id = :id LIMIT 1")
    suspend fun getMacroById(id: Int): Macro?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: Macro)

    @Update
    suspend fun updateMacro(macro: Macro)

    @Delete
    suspend fun deleteMacro(macro: Macro)

    @Query("SELECT * FROM terminal_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<TerminalLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TerminalLog)

    @Query("DELETE FROM terminal_logs")
    suspend fun clearLogs()
}
