package com.gullen.redmenacemoney.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gullen.redmenacemoney.MainViewModel
import com.gullen.redmenacemoney.data.AppState
import com.gullen.redmenacemoney.data.Debt
import com.gullen.redmenacemoney.data.DebtPayment
import com.gullen.redmenacemoney.data.MonthlyDebtInput
import com.gullen.redmenacemoney.data.amortize
import com.gullen.redmenacemoney.data.amortizeDebt
import com.gullen.redmenacemoney.data.findRequiredExtra
import com.gullen.redmenacemoney.data.monthlyEquivalentExtra
import com.gullen.redmenacemoney.data.newId
import com.gullen.redmenacemoney.data.simulateDebtStrategy
import com.gullen.redmenacemoney.data.todayISO
import com.gullen.redmenacemoney.data.yearsMonthsBetween
import com.gullen.redmenacemoney.ui.components.LabeledMoneyField
import com.gullen.redmenacemoney.ui.components.MoneyText
import com.gullen.redmenacemoney.ui.components.SectionCard
import com.gullen.redmenacemoney.ui.components.money
import com.gullen.redmenacemoney.ui.components.money2
import com.gullen.redmenacemoney.ui.theme.Amber
import com.gullen.redmenacemoney.ui.theme.Ink
import com.gullen.redmenacemoney.ui.theme.InkSoft
import com.gullen.redmenacemoney.ui.theme.MonoFamily
import com.gullen.redmenacemoney.ui.theme.Pine
import com.gullen.redmenacemoney.ui.theme.PineSoft
import com.gullen.redmenacemoney.ui.theme.Rust
import com.gullen.redmenacemoney.ui.theme.RustSoft
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val DEBT_LABELS = mapOf("mortgage" to "Mortgage", "heloc" to "HELOC", "loc" to "Line of Credit", "creditCard" to "Credit Card")

@Composable
fun DebtsScreen(vm: MainViewModel) {
    val state = vm.state
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                "Update the balance whenever you get a statement — it also auto-updates between visits based on your rate/payment/extra settings.",
                fontSize = 13.sp, color = InkSoft, modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        item {
            DebtCard("Mortgage", state.debts.mortgage, isMortgage = true, hasPaymentLog = true, onChange = { updated ->
                vm.update { s -> s.copy(debts = s.debts.copy(mortgage = updated)) }
            })
        }
        item {
            DebtCard("HELOC", state.debts.heloc, isMortgage = false, hasPaymentLog = false, onChange = { updated ->
                vm.update { s -> s.copy(debts = s.debts.copy(heloc = updated)) }
            })
        }
        item {
            DebtCard("Line of Credit", state.debts.loc, isMortgage = false, hasPaymentLog = true, onChange = { updated ->
                vm.update { s -> s.copy(debts = s.debts.copy(loc = updated)) }
            })
        }
        item {
            DebtCard("Credit Card", state.debts.creditCard, isMortgage = false, hasPaymentLog = false, onChange = { updated ->
                vm.update { s -> s.copy(debts = s.debts.copy(creditCard = updated)) }
            })
        }
        item { InterestCalculatorCard(state) }
        item { RangeTableCard(state) }
        item { ReverseCalculatorCard(state) }
        item { InvestOrDebtCard(state) }
        item { OptimizerCard(state) }
    }
}

@Composable
private fun DebtCard(title: String, debt: Debt, isMortgage: Boolean, hasPaymentLog: Boolean, onChange: (Debt) -> Unit) {
    SectionCard(accentColor = Rust) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)

        LabeledMoneyField("Current balance", debt.balance) { onChange(debt.copy(balance = it, balanceAsOfDate = todayISO())) }
        Text(
            "As of ${runCatching { LocalDate.parse(debt.balanceAsOfDate).format(dateFmt) }.getOrDefault(debt.balanceAsOfDate)} — updates automatically between visits, or edit directly after a statement.",
            fontSize = 11.sp, color = InkSoft, modifier = Modifier.padding(bottom = 6.dp)
        )

        var rateText by remember(debt.rate) { mutableStateOf(if (debt.rate == 0.0) "" else "%.2f".format(debt.rate * 100)) }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Annual interest rate (%)", modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink)
            OutlinedTextField(
                value = rateText,
                onValueChange = { rateText = it; it.toDoubleOrNull()?.let { pct -> onChange(debt.copy(rate = pct / 100.0)) } },
                modifier = Modifier.width(100.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }

        LabeledMoneyField("Payment per period", debt.payment) { onChange(debt.copy(payment = it)) }

        if (isMortgage) {
            var expanded by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Payment frequency", modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink)
                Box {
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
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Lump-sum month (1=Jan…12=Dec)", modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink)
                OutlinedTextField(
                    value = monthText,
                    onValueChange = { monthText = it; it.toIntOrNull()?.let { m -> if (m in 1..12) onChange(debt.copy(lumpMonth = m)) } },
                    modifier = Modifier.width(70.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        val result = remember(debt) { amortizeDebt(debt, isMortgage, LocalDate.now()) }
        Spacer(Modifier.padding(top = 4.dp))
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

        if (hasPaymentLog) {
            PaymentLogSection(debt, onChange)
        }
    }
}

@Composable
private fun PaymentLogSection(debt: Debt, onChange: (Debt) -> Unit) {
    Column(Modifier.padding(top = 12.dp)) {
        Text("Log a payment", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            "For a one-time or small daily payment you actually made — reduces the balance above immediately. " +
            "(\"Extra payment\" above is different: it's recurring for future projections and doesn't touch today's balance on its own.)",
            fontSize = 11.sp, color = InkSoft, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
        )
        var amountText by remember(debt) { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                modifier = Modifier.width(120.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = MonoFamily)
            )
            Spacer(Modifier.padding(horizontal = 6.dp))
            Button(onClick = {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    val newPayments = debt.payments + DebtPayment(id = newId(), date = todayISO(), amount = amt)
                    onChange(debt.copy(balance = maxOf(0.0, debt.balance - amt), balanceAsOfDate = todayISO(), payments = newPayments))
                    amountText = ""
                }
            }) { Text("Log payment") }
        }
        if (debt.payments.isNotEmpty()) {
            Text(
                "Logged payments (total ${money(debt.payments.sumOf { it.amount })}):",
                fontSize = 11.sp, color = InkSoft, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
            debt.payments.asReversed().forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(runCatching { LocalDate.parse(p.date).format(dateFmt) }.getOrDefault(p.date), fontSize = 12.sp)
                    Text(money2(p.amount), fontSize = 12.sp, fontFamily = MonoFamily)
                    IconButton(onClick = {
                        onChange(debt.copy(balance = debt.balance + p.amount, balanceAsOfDate = todayISO(), payments = debt.payments.filter { it.id != p.id }))
                    }) { Icon(Icons.Default.Close, contentDescription = "Remove", tint = Rust) }
                }
            }
        }
    }
}

@Composable
private fun StatusBox(text: String, bg: Color) {
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

private fun getDebt(state: AppState, key: String): Debt = when (key) {
    "heloc" -> state.debts.heloc
    "loc" -> state.debts.loc
    "creditCard" -> state.debts.creditCard
    else -> state.debts.mortgage
}

@Composable
private fun DebtDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(DEBT_LABELS[selected] ?: selected) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DEBT_LABELS.forEach { (key, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onSelect(key); expanded = false })
            }
        }
    }
}

@Composable
private fun InterestCalculatorCard(state: AppState) {
    var debtKey by remember { mutableStateOf("mortgage") }
    var extraText by remember { mutableStateOf("0") }
    val extraTest = extraText.toDoubleOrNull() ?: 0.0
    val debt = getDebt(state, debtKey)
    val isMortgage = debtKey == "mortgage"

    SectionCard(accentColor = Pine) {
        Text("Interest savings calculator", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(
            "Pick a debt and test an extra payment to see how much interest it would actually save.",
            fontSize = 12.sp, color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Debt", modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink)
            DebtDropdown(debtKey) { debtKey = it }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Extra payment to test (per period)", modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink)
            OutlinedTextField(
                value = extraText,
                onValueChange = { extraText = it },
                modifier = Modifier.width(110.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
        if (debt.balance <= 0) {
            Text("This debt is already paid off.", fontSize = 12.sp, color = InkSoft, modifier = Modifier.padding(top = 8.dp))
        } else {
            val current = amortizeDebt(debt, isMortgage, LocalDate.now())
            val withExtra = amortizeDebt(debt.copy(extra = debt.extra + extraTest), isMortgage, LocalDate.now())
            val interestSaved = current.totalInterest - withExtra.totalInterest
            val periodsSaved = current.periodsToPayoff - withExtra.periodsToPayoff
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total interest, current plan", fontSize = 13.sp); Text(money(current.totalInterest), fontFamily = MonoFamily, fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("With ${money2(extraTest)} extra", fontSize = 13.sp); Text(money(withExtra.totalInterest), fontFamily = MonoFamily, fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Interest saved", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(money(interestSaved), fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (interestSaved > 0) Pine else Ink)
            }
            if (periodsSaved > 0) {
                Text("Paid off $periodsSaved fewer periods from now.", fontSize = 12.sp, color = InkSoft, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun RangeTableCard(state: AppState) {
    var debtKey by remember { mutableStateOf("mortgage") }
    val debt = getDebt(state, debtKey)
    val isMortgage = debtKey == "mortgage"

    SectionCard(accentColor = Pine) {
        Text("Compare several extra-payment amounts at once", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Debt", modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink)
            DebtDropdown(debtKey) { debtKey = it }
        }
        if (debt.balance <= 0) {
            Text("This debt is already paid off.", fontSize = 12.sp, color = InkSoft)
        } else {
            val amounts = listOf(0.0, 50.0, 100.0, 200.0, 300.0, 500.0, 1000.0)
            val rows = amounts.map { extra ->
                val r = amortizeDebt(debt.copy(extra = debt.extra + extra), isMortgage, LocalDate.now(), maxPeriods = 1200)
                Triple(extra, r.payoffDate, r.totalInterest)
            }
            val baseInterest = rows.first().third
            rows.forEach { (extra, payoffDate, totalInterest) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (extra == 0.0) "(none)" else money2(extra), fontSize = 12.sp, fontFamily = MonoFamily)
                    Text(payoffDate?.format(dateFmt) ?: "—", fontSize = 12.sp, fontFamily = MonoFamily)
                    Text(money(totalInterest), fontSize = 12.sp, fontFamily = MonoFamily)
                    Text(money(baseInterest - totalInterest), fontSize = 12.sp, fontFamily = MonoFamily, color = Pine)
                }
            }
        }
    }
}

@Composable
private fun ReverseCalculatorCard(state: AppState) {
    var debtKey by remember { mutableStateOf("mortgage") }
    var targetDateText by remember { mutableStateOf("") }
    val debt = getDebt(state, debtKey)
    val isMortgage = debtKey == "mortgage"

    SectionCard(accentColor = Amber) {
        Text("Work backward from a target date", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(
            "Pick when you want to be debt-free on this one, and this tells you the extra payment per period needed.",
            fontSize = 12.sp, color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Debt", modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink)
            DebtDropdown(debtKey) { debtKey = it }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Target date (YYYY-MM-DD)", modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink)
            OutlinedTextField(
                value = targetDateText,
                onValueChange = { targetDateText = it },
                modifier = Modifier.width(140.dp),
                singleLine = true
            )
        }
        when {
            debt.balance <= 0 -> Text("This debt is already paid off.", fontSize = 12.sp, color = InkSoft, modifier = Modifier.padding(top = 8.dp))
            targetDateText.length != 10 -> Text("Enter a target date above.", fontSize = 12.sp, color = InkSoft, modifier = Modifier.padding(top = 8.dp))
            else -> {
                val targetDate = runCatching { LocalDate.parse(targetDateText) }.getOrNull()
                if (targetDate == null || !targetDate.isAfter(LocalDate.now())) {
                    Text("Enter a valid future date.", fontSize = 12.sp, color = InkSoft, modifier = Modifier.padding(top = 8.dp))
                } else {
                    val requiredExtra = findRequiredExtra(debt, isMortgage, targetDate, 1000)
                    val check = amortizeDebt(debt.copy(extra = requiredExtra), isMortgage, LocalDate.now())
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Required extra per period", fontSize = 13.sp); Text(money2(requiredExtra), fontFamily = MonoFamily, fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Projected payoff at that pace", fontSize = 13.sp); Text(check.payoffDate?.format(dateFmt) ?: "—", fontFamily = MonoFamily, fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total interest at that pace", fontSize = 13.sp); Text(money(check.totalInterest), fontFamily = MonoFamily, fontSize = 13.sp)
                    }
                    if (check.payoffDate != null && check.payoffDate.isAfter(targetDate)) {
                        StatusBox("Even paying it off as fast as possible, the earliest realistic payoff is ${check.payoffDate.format(dateFmt)} — that target isn't reachable for this balance.", RustSoft)
                    }
                }
            }
        }
    }
}

@Composable
private fun InvestOrDebtCard(state: AppState) {
    var extraText by remember { mutableStateOf("0") }
    var returnText by remember { mutableStateOf("6") }
    var yearsText by remember { mutableStateOf("10") }

    val debts = listOf(
        MonthlyDebtInput("mortgage", state.debts.mortgage.balance, state.debts.mortgage.rate, 0.0),
        MonthlyDebtInput("heloc", state.debts.heloc.balance, state.debts.heloc.rate, 0.0),
        MonthlyDebtInput("loc", state.debts.loc.balance, state.debts.loc.rate, 0.0),
        MonthlyDebtInput("creditCard", state.debts.creditCard.balance, state.debts.creditCard.rate, 0.0)
    ).filter { it.balance > 0 }

    SectionCard(accentColor = Amber) {
        Text("Invest or pay down debt?", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(
            "Compares putting extra money toward your highest-rate debt (a guaranteed \"return\" equal to that rate) against investing it, over the same stretch of time.",
            fontSize = 12.sp, color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Extra $/month available", modifier = Modifier.weight(1f), fontSize = 13.sp)
            OutlinedTextField(value = extraText, onValueChange = { extraText = it }, modifier = Modifier.width(100.dp), singleLine = true)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Assumed annual investment return (%)", modifier = Modifier.weight(1f), fontSize = 13.sp)
            OutlinedTextField(value = returnText, onValueChange = { returnText = it }, modifier = Modifier.width(100.dp), singleLine = true)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Years to compare over", modifier = Modifier.weight(1f), fontSize = 13.sp)
            OutlinedTextField(value = yearsText, onValueChange = { yearsText = it }, modifier = Modifier.width(100.dp), singleLine = true)
        }
        if (debts.isEmpty()) {
            Text("No debt balances — might as well invest it.", fontSize = 12.sp, color = InkSoft, modifier = Modifier.padding(top = 8.dp))
        } else {
            val extra = extraText.toDoubleOrNull() ?: 0.0
            val assumedReturn = (returnText.toDoubleOrNull() ?: 0.0) / 100
            val years = yearsText.toIntOrNull() ?: 0
            val months = years * 12
            val highestRateDebt = debts.maxByOrNull { it.rate }!!

            var investValue = 0.0
            val investMonthlyRate = assumedReturn / 12
            repeat(months) { investValue = investValue * (1 + investMonthlyRate) + extra }

            var debtSavedValue = 0.0
            val debtMonthlyRate = highestRateDebt.rate / 12
            repeat(months) { debtSavedValue = debtSavedValue * (1 + debtMonthlyRate) + extra }

            val winnerIsDebt = debtSavedValue > investValue
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Paying down ${DEBT_LABELS[highestRateDebt.key]} (${"%.2f".format(highestRateDebt.rate*100)}%)", fontSize = 12.sp, modifier = Modifier.weight(1f))
                MoneyText(debtSavedValue, fontSize = 14.sp)
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Investing instead (${"%.1f".format(assumedReturn*100)}%)", fontSize = 12.sp, modifier = Modifier.weight(1f))
                MoneyText(investValue, fontSize = 14.sp)
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (winnerIsDebt) "Paying down debt wins by" else "Investing wins by", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                MoneyText(kotlin.math.abs(debtSavedValue - investValue), color = Pine, fontSize = 14.sp)
            }
            Text(
                "Paying down debt always \"returns\" exactly its interest rate; real investment returns aren't guaranteed and can be negative in any given year.",
                fontSize = 11.sp, color = InkSoft, modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun OptimizerCard(state: AppState) {
    var addExtraText by remember { mutableStateOf("0") }
    var lumpNowText by remember { mutableStateOf("0") }
    val currentExtra = monthlyEquivalentExtra(state.debts)
    val addExtra = addExtraText.toDoubleOrNull() ?: 0.0
    val lumpNow = lumpNowText.toDoubleOrNull() ?: 0.0
    val totalPool = currentExtra + addExtra

    val d = state.debts
    val anyBalance = d.mortgage.balance > 0 || d.heloc.balance > 0 || d.loc.balance > 0 || d.creditCard.balance > 0

    SectionCard(accentColor = Pine) {
        Text("Pay the least interest — allocation optimizer", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(
            "Compares your current extra-payment split against funneling the same total extra dollars at the highest-rate debt first (avalanche) or smallest balance first (snowball). Normalized to monthly; mortgage lump-sum spread evenly across the year.",
            fontSize = 11.sp, color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Your current extra/month, all debts", modifier = Modifier.weight(1f), fontSize = 13.sp)
            Text(money2(currentExtra), fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Add hypothetical extra ($/month)", modifier = Modifier.weight(1f), fontSize = 13.sp)
            OutlinedTextField(value = addExtraText, onValueChange = { addExtraText = it }, modifier = Modifier.width(100.dp), singleLine = true)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("One-time lump sum available now", modifier = Modifier.weight(1f), fontSize = 13.sp)
            OutlinedTextField(value = lumpNowText, onValueChange = { lumpNowText = it }, modifier = Modifier.width(100.dp), singleLine = true)
        }

        if (!anyBalance) {
            Text("No debt balances to optimize.", fontSize = 12.sp, color = InkSoft, modifier = Modifier.padding(top = 8.dp))
        } else {
            val baselineInterest = amortizeDebtTotal(d.mortgage, true) + amortizeDebtTotal(d.heloc, false) +
                amortizeDebtTotal(d.loc, false) + amortizeDebtTotal(d.creditCard, false)

            val monthlyDebts = listOf(
                MonthlyDebtInput("mortgage", d.mortgage.balance, d.mortgage.rate, if (d.mortgage.frequency == "biweekly") d.mortgage.payment * 26 / 12 else d.mortgage.payment),
                MonthlyDebtInput("heloc", d.heloc.balance, d.heloc.rate, d.heloc.payment),
                MonthlyDebtInput("loc", d.loc.balance, d.loc.rate, d.loc.payment),
                MonthlyDebtInput("creditCard", d.creditCard.balance, d.creditCard.rate, d.creditCard.payment)
            ).filter { it.balance > 0 }

            val avalanche = simulateDebtStrategy(monthlyDebts, totalPool, "avalanche", lumpNow, 1200)
            val snowball = simulateDebtStrategy(monthlyDebts, totalPool, "snowball", lumpNow, 1200)
            val avalancheOrder = monthlyDebts.sortedByDescending { it.rate }
            val snowballOrder = monthlyDebts.sortedBy { it.balance }
            val savedVsSnowball = snowball.totalInterest - avalanche.totalInterest

            if (lumpNow > 0) {
                val target = avalancheOrder.first()
                StatusBox("If you have ${money(lumpNow)} to put toward debt right now, the least-interest move is putting it all on ${DEBT_LABELS[target.key]} (your highest rate at ${"%.2f".format(target.rate*100)}%).", PineSoft)
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Current split (as configured)", fontSize = 12.sp); Text(money(baselineInterest), fontFamily = MonoFamily, fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Avalanche (highest rate first) — least interest", fontSize = 12.sp); Text(money(avalanche.totalInterest), fontFamily = MonoFamily, fontSize = 12.sp, color = Pine)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Snowball (smallest balance first) — motivating", fontSize = 12.sp); Text(money(snowball.totalInterest), fontFamily = MonoFamily, fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Interest saved: current → avalanche", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(money(baselineInterest - avalanche.totalInterest), fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Pine)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("What snowball's motivation costs vs. avalanche", fontSize = 12.sp)
                Text(money(savedVsSnowball), fontFamily = MonoFamily, fontSize = 12.sp, color = Rust)
            }
            Text(
                "Avalanche order: " + avalancheOrder.joinToString(" → ") { "${DEBT_LABELS[it.key]} (${"%.2f".format(it.rate*100)}%)" },
                fontSize = 11.sp, color = InkSoft, modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Snowball order: " + snowballOrder.joinToString(" → ") { "${DEBT_LABELS[it.key]} (${money(it.balance)})" },
                fontSize = 11.sp, color = InkSoft
            )
        }
    }
}

private fun amortizeDebtTotal(debt: Debt, isMortgage: Boolean): Double {
    if (debt.balance <= 0) return 0.0
    return amortizeDebt(debt, isMortgage, LocalDate.now(), maxPeriods = 1200).totalInterest
}
