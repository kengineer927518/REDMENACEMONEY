package com.gullen.redmenacemoney.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gullen.redmenacemoney.MainViewModel
import com.gullen.redmenacemoney.data.Vacation
import com.gullen.redmenacemoney.data.VacationItem
import com.gullen.redmenacemoney.ui.components.LabeledMoneyField
import com.gullen.redmenacemoney.ui.components.LabeledTextField
import com.gullen.redmenacemoney.ui.components.MoneyText
import com.gullen.redmenacemoney.ui.components.SectionCard
import com.gullen.redmenacemoney.ui.components.money
import com.gullen.redmenacemoney.ui.theme.Amber
import com.gullen.redmenacemoney.ui.theme.InkSoft
import com.gullen.redmenacemoney.ui.theme.Pine
import com.gullen.redmenacemoney.ui.theme.Rust

@Composable
fun VacationsScreen(vm: MainViewModel) {
    val state = vm.state

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                "Set a budget for a trip, then track what it actually costs as bookings and expenses come in.",
                fontSize = 13.sp, color = InkSoft, modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (state.vacations.isEmpty()) {
            item { Text("No vacations planned yet.", color = InkSoft, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)) }
        }
        items(state.vacations, key = { it.id }) { v ->
            VacationCard(v, onChange = { updated ->
                vm.update { s -> s.copy(vacations = s.vacations.map { if (it.id == v.id) updated else it }) }
            }, onDelete = {
                vm.update { s -> s.copy(vacations = s.vacations.filter { it.id != v.id }) }
            })
        }
        item {
            Button(onClick = {
                vm.update { s -> s.copy(vacations = s.vacations + Vacation()) }
            }) { Text("+ Add a vacation") }
        }
    }
}

@Composable
private fun VacationCard(v: Vacation, onChange: (Vacation) -> Unit, onDelete: () -> Unit) {
    SectionCard(accentColor = Amber) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            var name by remember(v.id) { mutableStateOf(v.name) }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; onChange(v.copy(name = it)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
            )
            IconButton(onClick = onDelete) { Icon(Icons.Default.Close, contentDescription = "Remove trip", tint = Rust) }
        }
        var destination by remember(v.id) { mutableStateOf(v.destination) }
        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it; onChange(v.copy(destination = it)) },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp)
        )
        var startDate by remember(v.id) { mutableStateOf(v.startDate) }
        LabeledTextField("Start date (YYYY-MM-DD)", startDate) { startDate = it; onChange(v.copy(startDate = it)) }
        var endDate by remember(v.id) { mutableStateOf(v.endDate) }
        LabeledTextField("End date (YYYY-MM-DD)", endDate) { endDate = it; onChange(v.copy(endDate = it)) }
        LabeledMoneyField("Overall budget target", v.budgetTarget) { onChange(v.copy(budgetTarget = it)) }

        Text("Costs", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
        v.items.forEach { item ->
            VacationItemRow(
                item = item,
                onChange = { updated -> onChange(v.copy(items = v.items.map { if (it.id == item.id) updated else it })) },
                onDelete = { onChange(v.copy(items = v.items.filter { it.id != item.id })) }
            )
        }
        Button(onClick = { onChange(v.copy(items = v.items + VacationItem())) }, modifier = Modifier.padding(top = 6.dp)) {
            Text("+ Add cost item")
        }

        val remaining = v.totalBudgeted - v.totalActual
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total budgeted"); MoneyText(v.totalBudgeted)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total actual"); MoneyText(v.totalActual)
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (remaining >= 0) "Remaining" else "Over budget", fontWeight = FontWeight.Bold)
            MoneyText(kotlin.math.abs(remaining), color = if (remaining < 0) Rust else Pine)
        }
        val vsTarget = v.budgetTarget - v.totalActual
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Vs. overall target (${money(v.budgetTarget)})")
            MoneyText(vsTarget, color = if (v.totalActual > v.budgetTarget) Rust else Pine)
        }
    }
}

@Composable
private fun VacationItemRow(item: VacationItem, onChange: (VacationItem) -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        var category by remember(item.id) { mutableStateOf(item.category) }
        OutlinedTextField(
            value = category,
            onValueChange = { category = it; onChange(item.copy(category = it)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp)
        )
        var budgeted by remember(item.id) { mutableStateOf(if (item.budgeted == 0.0) "" else item.budgeted.toString()) }
        OutlinedTextField(
            value = budgeted,
            onValueChange = { budgeted = it; it.toDoubleOrNull()?.let { d -> onChange(item.copy(budgeted = d)) } },
            modifier = Modifier.width(80.dp).padding(start = 4.dp),
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp)
        )
        var actual by remember(item.id) { mutableStateOf(if (item.actual == 0.0) "" else item.actual.toString()) }
        OutlinedTextField(
            value = actual,
            onValueChange = { actual = it; it.toDoubleOrNull()?.let { d -> onChange(item.copy(actual = d)) } },
            modifier = Modifier.width(80.dp).padding(start = 4.dp),
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp)
        )
        IconButton(onClick = onDelete) { Icon(Icons.Default.Close, contentDescription = "Remove item", tint = Rust) }
    }
}
