package com.gullen.redmenacemoney.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gullen.redmenacemoney.MainViewModel
import com.gullen.redmenacemoney.ui.theme.Amber
import com.gullen.redmenacemoney.ui.theme.AmberSoft
import com.gullen.redmenacemoney.ui.theme.Ink
import com.gullen.redmenacemoney.ui.theme.Rail

private enum class Tab(val label: String) {
    DASHBOARD("Dashboard"),
    PAYCHEQUE("Paycheque"),
    BUDGET("Budget"),
    DEBTS("Debts"),
    INVESTMENTS("Investments"),
    NET_WORTH("Net Worth"),
    VACATIONS("Vacations"),
    GOALS("Goals")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedMenaceApp(vm: MainViewModel) {
    var selected by remember { mutableStateOf(Tab.DASHBOARD) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Closest Android equivalent to a browser's "leave without saving?" prompt:
    // intercept the system back button when there are unsaved changes.
    BackHandler(enabled = vm.dirty) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Unsaved changes") },
            text = { Text("You have changes that haven't been saved yet. Save before leaving?") },
            confirmButton = {
                TextButton(onClick = { vm.save(); showExitDialog = false }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { vm.undo(); showExitDialog = false }) { Text("Discard") }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Red Menace Money", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Rail,
                        titleContentColor = Color.White
                    )
                )
                Surface(color = Rail) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        Tab.entries.forEach { tab ->
                            val isSelected = tab == selected
                            Text(
                                text = tab.label,
                                color = if (isSelected) Color.White else Color(0xFFB7C4CA),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .then(Modifier.clickable { selected = tab })
                            )
                        }
                    }
                }
                if (vm.dirty) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(AmberSoft).padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Unsaved changes", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                        Row {
                            OutlinedButton(onClick = { vm.undo() }) { Text("Undo") }
                            androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = 4.dp))
                            Button(onClick = { vm.save() }) { Text("Save") }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
            when (selected) {
                Tab.DASHBOARD -> DashboardScreen(vm)
                Tab.PAYCHEQUE -> PaycheckScreen(vm)
                Tab.BUDGET -> BudgetScreen(vm)
                Tab.DEBTS -> DebtsScreen(vm)
                Tab.INVESTMENTS -> InvestmentsScreen(vm)
                Tab.NET_WORTH -> NetWorthScreen(vm)
                Tab.VACATIONS -> VacationsScreen(vm)
                Tab.GOALS -> GoalsScreen(vm)
            }
        }
    }
}
