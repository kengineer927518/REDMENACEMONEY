package com.gullen.redmenacemoney

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.gullen.redmenacemoney.data.AppState
import com.gullen.redmenacemoney.data.FinanceStore

/**
 * No autosave: every edit just marks state "dirty" in memory. Nothing touches
 * SharedPreferences until save() is called explicitly (Save button). undo()
 * reverts to whatever was last actually saved.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val store = FinanceStore(application)
    private var lastSaved: AppState = store.load()

    var state by mutableStateOf(lastSaved)
        private set

    var dirty by mutableStateOf(false)
        private set

    /** All mutations flow through here; nothing is persisted until save() is called. */
    fun update(transform: (AppState) -> AppState) {
        state = transform(state)
        dirty = true
    }

    fun save() {
        store.save(state)
        lastSaved = state
        dirty = false
    }

    fun undo() {
        state = lastSaved
        dirty = false
    }

    fun exportBackup(): String = store.exportBackupJson(state)

    /** Returns true if the import succeeded. Imported data is treated like any other edit — not saved until save() is called. */
    fun importBackup(raw: String): Boolean {
        val parsed = store.parseBackupJson(raw) ?: return false
        state = parsed
        dirty = true
        return true
    }
}
