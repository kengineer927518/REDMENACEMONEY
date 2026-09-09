package com.gullen.redmenacemoney.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gullen.redmenacemoney.MainViewModel
import com.gullen.redmenacemoney.data.AppState
import com.gullen.redmenacemoney.data.Debt
import com.gullen.redmenacemoney.data.amortizeDebt
import com.gullen.redmenacemoney.ui.components.LabeledMoneyField
import com.gullen.redmenacemoney.ui.components.SectionCard
import com.gullen.redmenacemoney.ui.components.money
import com.gullen.redmenacemoney.ui.components.money2
import com.gullen.redmenacemoney.ui.theme.Ink
import com.gullen.redmenacemoney.ui.theme.InkSoft
import com.gullen.redmenacemoney.ui.theme.Pine
import com.gullen.redmenacemoney.ui.theme.PineSoft
import com.gullen.redmenacemoney.ui.theme.Rust
import com.gullen.redmenacemoney.ui.theme.RustSoft
import com.gullen.redmenacemoney.data.yearsMonthsBetween
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun DebtsScreen(vm: MainViewModel) {
    val state = vm.state
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                "Update the balance whenever you get a statement. The payoff estimate recalculates from today.",
                fontSize = 13.sp, color = InkSoft, modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        item {
            DebtCard("Mortgage", state.debts.mortgage, isMortgage = true) { updated ->
                vm.update { s -> s.copy(debts = s.debts.copy(mortgage = updated)) }
            }
        }
        item {
            DebtCard("HELOC", state.debts.heloc, isMortgage = false) { updated ->
                vm.update { s -> s.copy(debts = s.debts.copy(heloc = updated)) }
            }
        }
        item {
            DebtCard("Line of Credit", state.debts.loc, isMortgage = false) { updated ->
                vm.update { s -> s.copy(debts = s.debts.copy(loc = updated)) }
            }
        }
    }
}

@Composable
private fun DebtCard(title: String, debt: Debt, isMortgage: Boolean, onChange: (Debt) -> Unit) {
    SectionCard(accentColor = Rust) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)

        LabeledMoneyField("Current balance", debt.balance) { onChange(debt.copy(balance = it)) }

        var rateText by remember(debt.rate) { mutableStateOf(if (debt.rate == 0.0) "" else "%.2f".format(debt.rate * 100)) }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Annual interest rate (%)", modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink)
            androidx.compose.material3.OutlinedTextField(
                value = rateText,
                onValueChange = { rateText = it; it.toDoubleOrNull()?.let { pct -> onChange(debt.copy(rate = pct / 100.0)) } },
                modifier = Modifier.width(100.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            )
        }

        LabeledMoneyField("Payment per period", debt.payment) { onChange(debt.copy(payment = it)) }

        if (isMortgage) {
            var expanded by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Payment frequency", modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink)
                androidx.compose.foundation.layout.Box {
                    OutlinedButton(onClick = { expanded = true }) { Text(if (debt.frequency == "biweekly") "Biweekly" else "Monthly") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Monthly") }, onClick = { onChange(debt.copy(frequency = "monthly")); expanded = false })
                        DropdownMenuItem(text = { Text("Biweekly") }, onClick = { onChange(debt.copy(frequency = "biweekly")); expanded = false })
                    }
                }
            }
        }

        LabeledMoneyField("Extra payment (per period)", debt.extra) { onChange(debt.copy(extra = it)) }

        if (isMortgage) {
            LabeledMoneyField("Annual lump-sum prepayment", debt.lumpAmount) { onChange(debt.copy(lumpAmount = it)) }
            var monthText by remember(debt.lumpMonth) { mutableStateOf(debt.lumpMonth.toString()) }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Lump-sum month (1=Jan…12=Dec)", modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink)
                androidx.compose.material3.OutlinedTextField(
                    value = monthText,
                    onValueChange = { monthText = it; it.toIntOrNull()?.let { m -> if (m in 1..12) onChange(debt.copy(lumpMonth = m)) } },
                    modifier = Modifier.width(70.dp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        }

        val result = remember(debt) { amortizeDebt(debt, isMortgage, LocalDate.now()) }
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 4.dp))
        when {
            debt.balance <= 0 -> StatusBox("Already paid off.", PineSoft)
            !result.coversInterest -> StatusBox(
                "At this payment, interest (${money2(result.schedule.firstOrNull()?.interest ?: 0.0)}/period) is more than what you're paying — this balance will grow, not shrink.",
                RustSoft
            )
            result.periodsToPayoff >= 1000 -> StatusBox("Payoff is a long way out at this rate — consider increasing the payment.", RustSoft)
            else -> {
                val ym = yearsMonthsBetween(LocalDate.now(), result.payoffDate ?: LocalDate.now())
                StatusBox(
                    "Projected payoff: ${result.payoffDate?.format(dateFmt)} (${ym.years} yrs ${ym.months} mo from today, total interest ${money(result.totalInterest)})",
                    PineSoft
                )
            }
        }
    }
}

@Composable
private fun StatusBox(text: String, bg: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .padding(10.dp)
    )
}
