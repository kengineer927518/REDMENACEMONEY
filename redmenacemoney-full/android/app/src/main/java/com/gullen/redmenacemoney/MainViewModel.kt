package com.gullen.redmenacemoney

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.gullen.redmenacemoney.data.AppState
import com.gullen.redmenacemoney.data.FinanceStore

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val store = FinanceStore(application)

    var state by mutableStateOf(store.load())
        private set

    /** All mutations flow through here so every change is saved immediately, no separate "save" step. */
    fun update(transform: (AppState) -> AppState) {
        state = transform(state)
        store.save(state)
    }

    fun exportBackup(): String = store.exportBackupJson(state)

    /** Returns true if the import succeeded. */
    fun importBackup(raw: String): Boolean {
        val parsed = store.parseBackupJson(raw) ?: return false
        state = parsed
        store.save(state)
        return true
    }
}
