package com.gullen.redmenacemoney.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Serializable
data class HistoryEntry(val timestamp: Long, val data: String)

/** Shape of a holdings-only update file: just an "investments" section, nothing else. */
@Serializable
data class InvestmentsOnlyImport(val investments: Investments)

private val FULL_BACKUP_KEYS = listOf("assumptions", "paycheque", "budgetIncome", "budgetExpense", "debts", "netWorthEntries", "goals")

/**
 * Same shape as ON TRACK's Store.kt: SharedPreferences + JSON strings, no database.
 * Autosaves on every change; a rolling backup history (capped, snapshotted at most every
 * few minutes) means a save is never the only copy of your data.
 */
class FinanceStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun load(): AppState {
        val raw = prefs.getString(KEY_STATE, null) ?: return AppState()
        return try {
            json.decodeFromString<AppState>(raw)
        } catch (e: Exception) {
            AppState()
        }
    }

    fun save(state: AppState) {
        prefs.edit().putString(KEY_STATE, json.encodeToString(state)).apply()
    }

    fun exportBackupJson(state: AppState): String = json.encodeToString(state)

    /** Returns null if the file doesn't parse as a valid AppState, so the UI can show an error. */
    fun parseBackupJson(raw: String): AppState? = try {
        json.decodeFromString<AppState>(raw)
    } catch (e: Exception) {
        null
    }

    /** True if the file has any of the core sections (a normal full backup); false for a partial file. */
    fun isFullBackup(raw: String): Boolean = try {
        val keys = json.parseToJsonElement(raw).jsonObject.keys
        FULL_BACKUP_KEYS.any { it in keys }
    } catch (e: Exception) {
        true // not valid JSON — let the normal parser report the failure
    }

    /** Parses a holdings-only update file; null if it doesn't contain a readable investments section. */
    fun parseInvestmentsOnly(raw: String): Investments? = try {
        json.decodeFromString<InvestmentsOnlyImport>(raw).investments
    } catch (e: Exception) {
        null
    }

    fun loadHistory(): List<HistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try { json.decodeFromString<List<HistoryEntry>>(raw) } catch (e: Exception) { emptyList() }
    }

    private fun saveHistory(history: List<HistoryEntry>) {
        prefs.edit().putString(KEY_HISTORY, json.encodeToString(history)).apply()
    }

    /** Snapshots the given state into history, unless the last snapshot was too recent. */
    fun maybeSnapshotHistory(state: AppState) {
        val history = loadHistory().toMutableList()
        val last = history.lastOrNull()
        val now = System.currentTimeMillis()
        if (last == null || now - last.timestamp > HISTORY_MIN_INTERVAL_MS) {
            history.add(HistoryEntry(now, json.encodeToString(state)))
            while (history.size > HISTORY_MAX) history.removeAt(0)
            saveHistory(history)
        }
    }

    /** Restores a past snapshot, first safety-snapshotting whatever's current so the restore itself is reversible. */
    fun restoreFromHistory(timestamp: Long, currentState: AppState): AppState? {
        val history = loadHistory().toMutableList()
        val entry = history.find { it.timestamp == timestamp } ?: return null
        history.add(HistoryEntry(System.currentTimeMillis(), json.encodeToString(currentState)))
        while (history.size > HISTORY_MAX) history.removeAt(0)
        saveHistory(history)
        return try { json.decodeFromString<AppState>(entry.data) } catch (e: Exception) { null }
    }

    companion object {
        private const val PREFS_NAME = "redmenace_prefs"
        private const val KEY_STATE = "finance_state"
        private const val KEY_HISTORY = "finance_state_history"
        private const val HISTORY_MAX = 20
        private const val HISTORY_MIN_INTERVAL_MS = 5 * 60 * 1000L
    }
}
