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

    /**
     * Returns a success message, or null if the file couldn't be read. A file containing only an
     * "investments" section updates just the holdings and leaves everything else as it was.
     */
    fun importBackup(raw: String): String? {
        if (!store.isFullBackup(raw)) {
            val inv = store.parseInvestmentsOnly(raw) ?: return null
            state = state.copy(investments = inv)
            store.save(state)
            store.maybeSnapshotHistory(state)
            return "Imported investments only (${inv.holdings.size} holdings) — everything else was left as it was."
        }
        val parsed = store.parseBackupJson(raw) ?: return null
        state = parsed
        store.save(state)
        store.maybeSnapshotHistory(state)
        return "Imported successfully."
    }

    fun history(): List<HistoryEntry> = store.loadHistory()

    fun restoreFromHistory(timestamp: Long) {
        val restored = store.restoreFromHistory(timestamp, state) ?: return
        state = restored
        store.save(state)
    }
}
