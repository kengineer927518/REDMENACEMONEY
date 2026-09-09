package com.gullen.redmenacemoney.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.gullen.redmenacemoney.ui.theme.Rail

private enum class Tab(val label: String) {
    DASHBOARD("Dashboard"),
    PAYCHEQUE("Paycheque"),
    BUDGET("Budget"),
    DEBTS("Debts"),
    NET_WORTH("Net Worth"),
    GOALS("Goals")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedMenaceApp(vm: MainViewModel) {
    var selected by remember { mutableStateOf(Tab.DASHBOARD) }

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
                    // underline for the active tab would need per-item width measurement;
                    // bold + white-vs-grey contrast carries the "active" state clearly enough on mobile.
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
                Tab.NET_WORTH -> NetWorthScreen(vm)
                Tab.GOALS -> GoalsScreen(vm)
            }
        }
    }
}
