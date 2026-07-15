package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "terminal_logs")
data class TerminalLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val macroName: String, // Macro name or "SYSTEM" / "BLUETOOTH"
    val message: String,
    val logType: String // "INFO", "SUCCESS", "ERROR", "INPUT"
)
