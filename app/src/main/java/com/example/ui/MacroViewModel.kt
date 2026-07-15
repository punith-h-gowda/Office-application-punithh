package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Macro
import com.example.data.MacroRepository
import com.example.data.TerminalLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MacroViewModel(private val repository: MacroRepository) : ViewModel() {

    // Reactive streams from database
    val macros: StateFlow<List<Macro>> = repository.allMacros
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val logs: StateFlow<List<TerminalLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI state
    private val _bluetoothConnected = MutableStateFlow(true)
    val bluetoothConnected: StateFlow<Boolean> = _bluetoothConnected.asStateFlow()

    private val _runningMacroId = MutableStateFlow<Int?>(null)
    val runningMacroId: StateFlow<Int?> = _runningMacroId.asStateFlow()

    private val _currentMacroIndex = MutableStateFlow(0)
    val currentMacroIndex: StateFlow<Int> = _currentMacroIndex.asStateFlow()

    private val _currentTab = MutableStateFlow("Macros") // "Macros", "Keys", "Terminal"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    fun setBluetoothConnected(connected: Boolean) {
        _bluetoothConnected.value = connected
        viewModelScope.launch(Dispatchers.IO) {
            val status = if (connected) "Active - listening on port" else "Disconnected"
            repository.insertLog(
                TerminalLog(
                    macroName = "BLUETOOTH",
                    message = "Bluetooth HID State changed: $status.",
                    logType = if (connected) "SUCCESS" else "ERROR"
                )
            )
        }
    }

    fun setMacroIndex(index: Int) {
        _currentMacroIndex.value = index
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun deleteMacro(macro: Macro) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMacro(macro)
            repository.insertLog(
                TerminalLog(
                    macroName = "SYSTEM",
                    message = "Deleted macro '${macro.name}' from storage.",
                    logType = "INFO"
                )
            )
            // Adjust index if needed
            val currentListSize = macros.value.size
            if (_currentMacroIndex.value >= currentListSize - 1 && currentListSize > 1) {
                _currentMacroIndex.value = currentListSize - 2
            } else {
                _currentMacroIndex.value = 0
            }
        }
    }

    fun insertMacro(name: String, category: String, targetAccount: String, accessKey: String, steps: String, iconType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val macro = Macro(
                name = name.uppercase().trim(),
                category = category.uppercase().trim(),
                targetAccount = targetAccount.trim(),
                accessKey = accessKey.trim(),
                steps = steps.trim(),
                iconType = iconType.lowercase().trim()
            )
            repository.insertMacro(macro)
            repository.insertLog(
                TerminalLog(
                    macroName = "SYSTEM",
                    message = "Successfully stored new macro '${macro.name}'.",
                    logType = "SUCCESS"
                )
            )
        }
    }

    fun updateMacro(macro: Macro) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMacro(macro)
            repository.insertLog(
                TerminalLog(
                    macroName = "SYSTEM",
                    message = "Updated macro profile for '${macro.name}'.",
                    logType = "INFO"
                )
            )
        }
    }

    fun sendKeystroke(key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLog(
                TerminalLog(
                    macroName = "KEYBOARD",
                    message = "HID Out: Typed key '$key'",
                    logType = "SUCCESS"
                )
            )
        }
    }

    fun sendRawKeystrokeSequence(sequence: String) {
        if (sequence.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLog(
                TerminalLog(
                    macroName = "KEYBOARD",
                    message = "HID Out: Mimicking raw sequence: \"$sequence\"",
                    logType = "SUCCESS"
                )
            )
        }
    }

    fun clearTerminalLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLogs()
        }
    }

    fun executeTerminalCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLog(TerminalLog(macroName = "USER", message = trimmed, logType = "INPUT"))
            
            val parts = trimmed.split(" ")
            val baseCmd = parts[0].lowercase()

            when (baseCmd) {
                "clear", "cls" -> {
                    repository.clearLogs()
                }
                "help" -> {
                    repository.insertLog(TerminalLog(macroName = "HELP", message = "Available commands: help, list, run <index>, clear, status, disconnect, connect", logType = "INFO"))
                }
                "list" -> {
                    val mList = macros.value
                    if (mList.isEmpty()) {
                        repository.insertLog(TerminalLog(macroName = "HELP", message = "No macros configured.", logType = "ERROR"))
                    } else {
                        mList.forEachIndexed { i, m ->
                            repository.insertLog(TerminalLog(macroName = "HELP", message = "[$i] ${m.name} (${m.category})", logType = "INFO"))
                        }
                    }
                }
                "status" -> {
                    val bt = if (_bluetoothConnected.value) "ACTIVE" else "INACTIVE"
                    repository.insertLog(TerminalLog(macroName = "SYSTEM", message = "Bluetooth HID: $bt. Configured macros: ${macros.value.size}.", logType = "INFO"))
                }
                "disconnect" -> {
                    _bluetoothConnected.value = false
                    repository.insertLog(TerminalLog(macroName = "BLUETOOTH", message = "Disconnected.", logType = "ERROR"))
                }
                "connect" -> {
                    _bluetoothConnected.value = true
                    repository.insertLog(TerminalLog(macroName = "BLUETOOTH", message = "Active - listening on port.", logType = "SUCCESS"))
                }
                "run" -> {
                    if (parts.size < 2) {
                        repository.insertLog(TerminalLog(macroName = "ERROR", message = "Usage: run <macro_index>", logType = "ERROR"))
                    } else {
                        val index = parts[1].toIntOrNull()
                        val mList = macros.value
                        if (index != null && index >= 0 && index < mList.size) {
                            runMacro(mList[index])
                        } else {
                            repository.insertLog(TerminalLog(macroName = "ERROR", message = "Invalid index.", logType = "ERROR"))
                        }
                    }
                }
                else -> {
                    repository.insertLog(TerminalLog(macroName = "SYSTEM", message = "Command not found: $trimmed. Type 'help' for commands.", logType = "ERROR"))
                }
            }
        }
    }

    fun runMacro(macro: Macro) {
        if (_runningMacroId.value != null) return // Already running something

        if (!_bluetoothConnected.value) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.insertLog(
                    TerminalLog(
                        macroName = "ERROR",
                        message = "Cannot run macro. Bluetooth HID is disconnected!",
                        logType = "ERROR"
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _runningMacroId.value = macro.id
            
            // Log started
            repository.insertLog(
                TerminalLog(
                    macroName = macro.name,
                    message = ">>> INITIATING MACRO: ${macro.name} [Account: ${macro.targetAccount}]",
                    logType = "INFO"
                )
            )
            
            val steps = macro.stepsList
            for (step in steps) {
                delay(700) // Simulating physical typing or protocol exchange delay
                val stepFormatted = step.replace("_", " ").uppercase()
                repository.insertLog(
                    TerminalLog(
                        macroName = macro.name,
                        message = "Executing step: $stepFormatted...",
                        logType = "INFO"
                    )
                )
                
                delay(600)
                val successMessage = when(step) {
                    "pair_controller" -> "Bluetooth socket negotiated (Port RFCOMM-2)."
                    "verify_hid_link" -> "Keyboard/HID report descriptor validated."
                    "load_outlook_suite" -> "Target Outlook process located (PID 9422)."
                    "inject_security_hash" -> "Security payload injected successfully."
                    "tunnel_established" -> "SSH Tunnel active on local port 8080."
                    "launch_workspace" -> "Outlook workspace launched on primary display."
                    "verify_git_credentials" -> "OAuth2 Token confirmed with Github."
                    "checkout_main_branch" -> "Checked out main. Local HEAD is up to date."
                    "apply_hotfix_stashes" -> "Stash pop complete. 0 conflicts."
                    "compile_and_verify" -> "Build completed in 14.2s. 0 errors."
                    "push_origin_release" -> "Push success. Remote commit ID: d23fa8c."
                    "handshake_jira_api" -> "Jira API Handshake OK (HTTP 200)."
                    "fetch_assigned_tickets" -> "Fetched 4 in-progress tickets."
                    "automating_state_advance" -> "Ticket DEV-104 updated to 'Review'."
                    "trigger_prod_webhook" -> "Deployment webhook triggered successfully."
                    "check_core_voltages" -> "CPU Voltages stable (1.2V). Temp 42C."
                    "flush_system_buffers" -> "Disk buffers synced to primary drive."
                    "terminate_stray_daemons" -> "Terminated 3 orphan processes."
                    "reboot_local_terminal" -> "SIGHUP broadcast sent. Environment clean."
                    else -> "Action '$stepFormatted' complete."
                }
                
                repository.insertLog(
                    TerminalLog(
                        macroName = macro.name,
                        message = "✓ $successMessage",
                        logType = "SUCCESS"
                    )
                )
            }
            
            delay(500)
            repository.insertLog(
                TerminalLog(
                    macroName = macro.name,
                    message = ">>> MACRO COMPLETED SUCCESSFULLY.",
                    logType = "SUCCESS"
                )
            )
            _runningMacroId.value = null
        }
    }
}

class MacroViewModelFactory(private val repository: MacroRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MacroViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MacroViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
