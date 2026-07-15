package com.example.data

import kotlinx.coroutines.flow.Flow

class MacroRepository(private val macroDao: MacroDao) {
    val allMacros: Flow<List<Macro>> = macroDao.getAllMacros()
    val allLogs: Flow<List<TerminalLog>> = macroDao.getAllLogs()

    suspend fun getMacroById(id: Int): Macro? = macroDao.getMacroById(id)

    suspend fun insertMacro(macro: Macro) {
        macroDao.insertMacro(macro)
    }

    suspend fun updateMacro(macro: Macro) {
        macroDao.updateMacro(macro)
    }

    suspend fun deleteMacro(macro: Macro) {
        macroDao.deleteMacro(macro)
    }

    suspend fun insertLog(log: TerminalLog) {
        macroDao.insertLog(log)
    }

    suspend fun clearLogs() {
        macroDao.clearLogs()
    }
}
