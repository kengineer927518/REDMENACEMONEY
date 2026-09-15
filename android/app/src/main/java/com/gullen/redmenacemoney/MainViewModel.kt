package com.gullen.redmenacemoney

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.gullen.redmenacemoney.data.AppState
import com.gullen.redmenacemoney.data.FinanceStore
import com.gullen.redmenacemoney.data.HistoryEntry
import com.gullen.redmenacemoney.data.autoAdvanceDebtBalances

/**
 * Autosaves on every change (debounced), with a rolling backup history — same model as
 * the web app. No manual Save/Undo step; restoring an older snapshot is the safety net.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val store = FinanceStore(application)

    var state by mutableStateOf(run {
        val loaded = store.load()
        val advanced = autoAdvanceDebtBalances(loaded)
        if (advanced != loaded) store.save(advanced)
        advanced
    })
        private set

    /** All mutations flow through here so every change autosaves immediately. */
    fun update(transform: (AppState) -> AppState) {
        state = transform(state)
        store.save(state)
        store.maybeSnapshotHistory(state)
    }

    fun exportBackup(): String = store.exportBackupJson(state)

    /** Returns true if the import succeeded. */
    fun importBackup(raw: String): Boolean {
        val parsed = store.parseBackupJson(raw) ?: return false
        state = parsed
        store.save(state)
        store.maybeSnapshotHistory(state)
        return true
    }

    fun history(): List<HistoryEntry> = store.loadHistory()

    fun restoreFromHistory(timestamp: Long) {
        val restored = store.restoreFromHistory(timestamp, state) ?: return
        state = restored
        store.save(state)
    }
}
