package com.gullen.redmenacemoney.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gullen.redmenacemoney.MainViewModel
import com.gullen.redmenacemoney.data.Goal
import com.gullen.redmenacemoney.data.getCurrentValueForGoal
import com.gullen.redmenacemoney.data.goalProgress
import com.gullen.redmenacemoney.data.incomeForMonth
import com.gullen.redmenacemoney.data.yearsMonthsBetween
import com.gullen.redmenacemoney.ui.components.GoalTrack
import com.gullen.redmenacemoney.ui.components.LabeledTextField
import com.gullen.redmenacemoney.ui.components.MoneyText
import com.gullen.redmenacemoney.ui.components.SectionCard
import com.gullen.redmenacemoney.ui.components.money
import com.gullen.redmenacemoney.ui.theme.Amber
import com.gullen.redmenacemoney.ui.theme.Ink
import com.gullen.redmenacemoney.ui.theme.InkSoft
import com.gullen.redmenacemoney.ui.theme.Pine
import com.gullen.redmenacemoney.ui.theme.Rust
import java.time.LocalDate

@Composable
fun DashboardScreen(vm: MainViewModel) {
    val state = vm.state
    val context = LocalContext.current
    var importStatus by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportBackup().toByteArray()) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (text != null && vm.importBackup(text)) {
                importStatus = "Imported successfully."
            } else {
                importStatus = "Couldn't read that file — make sure it's the Red Menace Money JSON export/starter file."
            }
        }
    }

    val now = LocalDate.now()
    val empIncome = incomeForMonth(state.paycheque, now.year, now.monthValue)
    val otherIncome = state.budgetIncome.sumOf { it.amount }
    val totalExpense = state.budgetExpense.sumOf { it.amount }
    val net = empIncome + otherIncome - totalExpense

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f).padding(end = 6.dp)) {
                    Text("Net worth", fontSize = 12.sp, color = InkSoft)
                    MoneyText(state.latestNetWorth, fontSize = 24.sp)
                }
                Column(Modifier.weight(1f).padding(start = 6.dp)) {
                    Text("Total debt", fontSize = 12.sp, color = InkSoft)
                    MoneyText(state.totalDebtNow, fontSize = 24.sp)
                }
            }
        }

        item {
            SectionCard {
                Text("Retirement target", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(Modifier.padding(top = 8.dp)) {
                    Column(Modifier.weight(1f)) {
                        var dateText by remember(state.assumptions.retirementDate) {
                            mutableStateOf(state.assumptions.retirementDate)
                        }
                        if (state.assumptions.retirementDate.isNotBlank()) {
                            val rd = LocalDate.parse(state.assumptions.retirementDate)
                            val ym = yearsMonthsBetween(now, rd)
                            Text("Target date: $rd")
                            Text("Time remaining: ${ym.years} yrs ${ym.months} mo")
                        } else {
                            Text("No target date set yet.", color = InkSoft)
                        }
                        LabeledTextField(
                            label = "Date (YYYY-MM-DD)",
                            value = dateText,
                            onValueChange = {
                                dateText = it
                                if (it.length == 10) {
                                    runCatching { LocalDate.parse(it) }.onSuccess { _ ->
                                        vm.update { s -> s.copy(assumptions = s.assumptions.copy(retirementDate = it)) }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        item {
            SectionCard {
                Text("This month's cash flow", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Income"); MoneyText(empIncome + otherIncome)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Expenses"); MoneyText(totalExpense)
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net", fontWeight = FontWeight.Bold)
                    MoneyText(net, color = if (net < 0) Rust else Pine)
                }
            }
        }

        item {
            Text("Goals", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        }
        if (state.goals.isEmpty()) {
            item { Text("No goals yet — add one on the Goals tab.", color = InkSoft, fontSize = 13.sp) }
        } else {
            items(state.goals) { goal -> GoalSummaryCard(goal, vm) }
        }

        item {
            SectionCard(accentColor = Amber) {
                Text("Your data", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    "Everything here lives only on this phone. Export a backup any time, or import a starter file with real numbers already filled in.",
                    fontSize = 12.sp, color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )
                Row {
                    Button(
                        onClick = { exportLauncher.launch("redmenacemoney-backup-${LocalDate.now()}.json") },
                        colors = ButtonDefaults.buttonColors()
                    ) { Text("Export") }
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = 6.dp))
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) { Text("Import") }
                }
                importStatus?.let {
                    Text(it, fontSize = 12.sp, color = if (it.startsWith("Imported")) Pine else Rust, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun GoalSummaryCard(goal: Goal, vm: MainViewModel) {
    val state = vm.state
    val progress = goalProgress(goal, state).toFloat()
    SectionCard(accentColor = Amber) {
        Text(goal.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        val currentLabel = if (goal.track == "date") {
            goal.targetDate?.let {
                val ym = yearsMonthsBetween(LocalDate.now(), LocalDate.parse(it))
                "${ym.years} yrs ${ym.months} mo remaining"
            } ?: "No date set"
        } else {
            "${money(getCurrentValueForGoal(goal, state))} of ${money(goal.targetValue ?: 0.0)} target"
        }
        Text(currentLabel, fontSize = 12.sp, color = InkSoft)
        GoalTrack(progress = progress, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        Text("${(progress * 100).toInt()}% of the way there", fontSize = 12.sp, color = InkSoft)
    }
}
