package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Macro::class, TerminalLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun macroDao(): MacroDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nexus_controller_database"
                )
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDb(database.macroDao())
                }
            }
        }

        suspend fun populateDb(dao: MacroDao) {
            // Initial seed data
            val presets = listOf(
                Macro(
                    name = "OUTLOOK WORKSPACE",
                    category = "DAILY MACRO 01",
                    targetAccount = "punith_dev_admin",
                    accessKey = "••••••••••",
                    steps = "pair_controller;verify_hid_link;load_outlook_suite;inject_security_hash;tunnel_established;launch_workspace",
                    iconType = "outlook",
                    isSystemPreset = true
                ),
                Macro(
                    name = "GITHUB COMPILER",
                    category = "DEVELOPER MACRO 02",
                    targetAccount = "git_actions_bot",
                    accessKey = "••••••••••",
                    steps = "verify_git_credentials;checkout_main_branch;apply_hotfix_stashes;compile_and_verify;push_origin_release",
                    iconType = "github",
                    isSystemPreset = true
                ),
                Macro(
                    name = "JIRA DEPLOYER",
                    category = "AUTOMATION MACRO 03",
                    targetAccount = "jira_hook_user",
                    accessKey = "••••••••••",
                    steps = "handshake_jira_api;fetch_assigned_tickets;automating_state_advance;trigger_prod_webhook",
                    iconType = "jira",
                    isSystemPreset = true
                ),
                Macro(
                    name = "SYSTEM REBOOTER",
                    category = "UTILITY MACRO 04",
                    targetAccount = "localhost_root",
                    accessKey = "••••••••••",
                    steps = "check_core_voltages;flush_system_buffers;terminate_stray_daemons;reboot_local_terminal",
                    iconType = "terminal",
                    isSystemPreset = true
                )
            )

            presets.forEach { dao.insertMacro(it) }

            // Insert a friendly initial system log
            dao.insertLog(
                TerminalLog(
                    macroName = "SYSTEM",
                    message = "Nexus Controller OS v1.0.4 loaded successfully.",
                    logType = "INFO"
                )
            )
            dao.insertLog(
                TerminalLog(
                    macroName = "BLUETOOTH",
                    message = "Bluetooth HID Active - listening on controller port.",
                    logType = "SUCCESS"
                )
            )
        }
    }
}
