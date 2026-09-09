package com.gullen.redmenacemoney.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.gullen.redmenacemoney.data.BudgetItem
import com.gullen.redmenacemoney.data.incomeForMonth
import com.gullen.redmenacemoney.ui.components.LabeledMoneyField
import com.gullen.redmenacemoney.ui.components.MoneyText
import com.gullen.redmenacemoney.ui.components.SectionCard
import com.gullen.redmenacemoney.ui.components.money2
import com.gullen.redmenacemoney.ui.theme.Amber
import com.gullen.redmenacemoney.ui.theme.Ink
import com.gullen.redmenacemoney.ui.theme.MonoFamily
import com.gullen.redmenacemoney.ui.theme.Pine
import com.gullen.redmenacemoney.ui.theme.Rust
import java.time.LocalDate

@Composable
fun BudgetScreen(vm: MainViewModel) {
    val state = vm.state
    val now = LocalDate.now()
    val empIncome = incomeForMonth(state.paycheque, now.year, now.monthValue)
    val otherIncome = state.budgetIncome.sumOf { it.amount }
    val totalExpense = state.budgetExpense.sumOf { it.amount }
    val totalIncome = empIncome + otherIncome
    val net = totalIncome - totalExpense

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            SectionCard(accentColor = Pine) {
                Text("Income", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Employment income (this month, from Paycheque)", modifier = Modifier.padding(end = 8.dp), fontSize = 13.sp)
                    MoneyText(empIncome)
                }
                state.budgetIncome.forEach { item ->
                    BudgetItemRow(
                        item = item,
                        onNameChange = { name -> vm.update { s -> s.copy(budgetIncome = s.budgetIncome.map { if (it.id == item.id) it.copy(name = name) else it }) } },
                        onAmountChange = { amt -> vm.update { s -> s.copy(budgetIncome = s.budgetIncome.map { if (it.id == item.id) it.copy(amount = amt) else it }) } },
                        onDelete = { vm.update { s -> s.copy(budgetIncome = s.budgetIncome.filter { it.id != item.id }) } }
                    )
                }
                OutlinedButton(onClick = {
                    vm.update { s -> s.copy(budgetIncome = s.budgetIncome + BudgetItem(name = "New income")) }
                }, modifier = Modifier.padding(top = 6.dp)) { Text("+ Add other income") }
            }
        }

        item {
            SectionCard(accentColor = Amber) {
                Text("Expenses", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                state.budgetExpense.forEach { item ->
                    BudgetItemRow(
                        item = item,
                        onNameChange = { name -> vm.update { s -> s.copy(budgetExpense = s.budgetExpense.map { if (it.id == item.id) it.copy(name = name) else it }) } },
                        onAmountChange = { amt -> vm.update { s -> s.copy(budgetExpense = s.budgetExpense.map { if (it.id == item.id) it.copy(amount = amt) else it }) } },
                        onDelete = { vm.update { s -> s.copy(budgetExpense = s.budgetExpense.filter { it.id != item.id }) } }
                    )
                }
                OutlinedButton(onClick = {
                    vm.update { s -> s.copy(budgetExpense = s.budgetExpense + BudgetItem(name = "New expense")) }
                }, modifier = Modifier.padding(top = 6.dp)) { Text("+ Add expense") }
            }
        }

        item {
            SectionCard {
                Text("Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total income"); MoneyText(totalIncome)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total expenses"); MoneyText(totalExpense)
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net cash flow", fontWeight = FontWeight.Bold)
                    MoneyText(net, color = if (net < 0) Rust else Pine)
                }
            }
        }
    }
}

@Composable
private fun BudgetItemRow(
    item: BudgetItem,
    onNameChange: (String) -> Unit,
    onAmountChange: (Double) -> Unit,
    onDelete: () -> Unit
) {
    var name by androidx.compose.runtime.remember(item.id) { androidx.compose.runtime.mutableStateOf(item.name) }
    var amountText by androidx.compose.runtime.remember(item.id) {
        androidx.compose.runtime.mutableStateOf(if (item.amount == 0.0) "" else item.amount.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() })
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; onNameChange(it) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp)
        )
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it; it.toDoubleOrNull()?.let(onAmountChange) },
            modifier = Modifier.width(100.dp).padding(start = 6.dp),
            singleLine = true,
            textStyle = TextStyle(fontFamily = MonoFamily, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Rust)
        }
    }
}
