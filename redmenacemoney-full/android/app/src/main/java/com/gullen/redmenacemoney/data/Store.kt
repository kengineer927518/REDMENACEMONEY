package com.gullen.redmenacemoney.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Same shape as ON TRACK's Store.kt: SharedPreferences + JSON strings, no database.
 * exportBackupJson/parseBackupJson mirror CrewStore's backup/restore pattern so the
 * Dashboard's Export/Import buttons can hand a raw JSON string straight to/from a file.
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

    companion object {
        private const val PREFS_NAME = "redmenace_prefs"
        private const val KEY_STATE = "finance_state"
    }
}
