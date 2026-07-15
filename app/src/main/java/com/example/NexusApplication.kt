package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.MacroRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class NexusApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { MacroRepository(database.macroDao()) }
}
