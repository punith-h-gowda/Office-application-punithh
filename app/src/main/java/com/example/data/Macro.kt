package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "macros")
data class Macro(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val targetAccount: String,
    val accessKey: String,
    val steps: String, // Semicolon-separated list of actions
    val iconType: String, // "outlook", "github", "jira", "terminal", "system"
    val isSystemPreset: Boolean = false
) {
    val stepsList: List<String>
        get() = steps.split(";").map { it.trim() }.filter { it.isNotEmpty() }
}
