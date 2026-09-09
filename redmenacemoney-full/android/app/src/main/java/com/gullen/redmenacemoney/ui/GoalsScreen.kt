package com.gullen.redmenacemoney.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gullen.redmenacemoney.MainViewModel
import com.gullen.redmenacemoney.data.Goal
import com.gullen.redmenacemoney.data.getCurrentValueForGoal
import com.gullen.redmenacemoney.data.goalProgress
import com.gullen.redmenacemoney.data.newId
import com.gullen.redmenacemoney.data.todayISO
import com.gullen.redmenacemoney.data.yearsMonthsBetween
import com.gullen.redmenacemoney.ui.components.GoalTrack
import com.gullen.redmenacemoney.ui.components.LabeledMoneyField
import com.gullen.redmenacemoney.ui.components.LabeledTextField
import com.gullen.redmenacemoney.ui.components.SectionCard
import com.gullen.redmenacemoney.ui.components.money
import com.gullen.redmenacemoney.ui.theme.Amber
import com.gullen.redmenacemoney.ui.theme.InkSoft
import com.gullen.redmenacemoney.ui.theme.Rust
import java.time.LocalDate

private val TRACK_OPTIONS = listOf(
    "netWorth" to "Net worth",
    "totalDebt" to "Total debt",
    "mortgage" to "Mortgage balance",
    "heloc" to "HELOC balance",
    "loc" to "Line of Credit balance",
    "custom" to "Custom (I'll update it myself)",
    "date" to "Date only (e.g. retirement countdown)"
)

@Composable
fun GoalsScreen(vm: MainViewModel) {
    val state = vm.state
    var showForm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                "Set a target and track it against your real numbers. Linked goals update automatically; custom goals you update yourself.",
                fontSize = 13.sp, color = InkSoft, modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (state.goals.isEmpty() && !showForm) {
            item { Text("No goals yet.", color = InkSoft, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)) }
        }
        items(state.goals, key = { it.id }) { goal ->
            GoalCard(goal, state, onDelete = {
                vm.update { s -> s.copy(goals = s.goals.filter { it.id != goal.id }) }
            }, onManualUpdate = { newVal ->
                vm.update { s -> s.copy(goals = s.goals.map { if (it.id == goal.id) it.copy(manualCurrent = newVal) else it }) }
            })
        }
        item {
            if (showForm) {
                GoalForm(
                    state = state,
                    onCancel = { showForm = false },
                    onSave = { goal ->
                        vm.update { s -> s.copy(goals = s.goals + goal) }
                        showForm = false
                    }
                )
            } else {
                Button(onClick = { showForm = true }) { Text("+ Add a goal") }
            }
        }
    }
}

@Composable
private fun GoalCard(goal: Goal, state: com.gullen.redmenacemoney.data.AppState, onDelete: () -> Unit, onManualUpdate: (Double) -> Unit) {
    val progress = goalProgress(goal, state).toFloat()
    SectionCard(accentColor = Amber) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(goal.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            IconButton(onClick = onDelete) { Icon(Icons.Default.Close, contentDescription = "Delete goal", tint = Rust) }
        }
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
        if (goal.track == "custom") {
            LabeledMoneyField("Update current value", goal.manualCurrent, onManualUpdate)
        }
    }
}

@Composable
private fun GoalForm(state: com.gullen.redmenacemoney.data.AppState, onCancel: () -> Unit, onSave: (Goal) -> Unit) {
    var name by remember { mutableStateOf("") }
    var track by remember { mutableStateOf("custom") }
    var targetValueText by remember { mutableStateOf("0") }
    var currentValueText by remember { mutableStateOf("0") }
    var targetDate by remember { mutableStateOf("") }
    var trackMenuExpanded by remember { mutableStateOf(false) }

    SectionCard(accentColor = Amber) {
        Text("New goal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            singleLine = true
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Track", modifier = Modifier.padding(top = 12.dp))
            androidx.compose.foundation.layout.Box {
                OutlinedButton(onClick = { trackMenuExpanded = true }) {
                    Text(TRACK_OPTIONS.first { it.first == track }.second)
                }
                DropdownMenu(expanded = trackMenuExpanded, onDismissRequest = { trackMenuExpanded = false }) {
                    TRACK_OPTIONS.forEach { (key, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { track = key; trackMenuExpanded = false })
                    }
                }
            }
        }
        if (track != "date") {
            LabeledTextField("Target value ($)", targetValueText) { targetValueText = it }
        }
        if (track == "custom") {
            LabeledTextField("Starting value ($)", currentValueText) { currentValueText = it }
        }
        LabeledTextField("Target date (YYYY-MM-DD)", targetDate) { targetDate = it }

        Row(Modifier.padding(top = 12.dp)) {
            Button(onClick = {
                val targetVal = targetValueText.toDoubleOrNull() ?: 0.0
                val startVal = currentValueText.toDoubleOrNull() ?: 0.0
                val baseline = when {
                    track == "date" -> null
                    track == "custom" -> startVal
                    else -> getCurrentValueForGoal(Goal(track = track), state)
                }
                val goal = Goal(
                    id = newId(),
                    name = name.ifBlank { "Untitled goal" },
                    track = track,
                    targetValue = if (track == "date") null else targetVal,
                    targetDate = targetDate.ifBlank { null },
                    baselineValue = baseline,
                    manualCurrent = if (track == "custom") startVal else 0.0,
                    createdDate = todayISO()
                )
                onSave(goal)
            }) { Text("Create goal") }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = 6.dp))
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}
