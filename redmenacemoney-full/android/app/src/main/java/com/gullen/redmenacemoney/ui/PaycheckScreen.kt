package com.gullen.redmenacemoney.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
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
import com.gullen.redmenacemoney.data.PaycheckOccurrence
import com.gullen.redmenacemoney.data.generatePaycheques
import com.gullen.redmenacemoney.data.incomeForMonth
import com.gullen.redmenacemoney.ui.components.LabeledMoneyField
import com.gullen.redmenacemoney.ui.components.LabeledTextField
import com.gullen.redmenacemoney.ui.components.MoneyText
import com.gullen.redmenacemoney.ui.components.SectionCard
import com.gullen.redmenacemoney.ui.components.money2
import com.gullen.redmenacemoney.ui.theme.InkSoft
import com.gullen.redmenacemoney.ui.theme.Pine
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val monthFmt = DateTimeFormatter.ofPattern("MMM yyyy")

@Composable
fun PaycheckScreen(vm: MainViewModel) {
    val state = vm.state
    val pc = state.paycheque

    val upcoming = remember(pc) { generatePaycheques(pc, LocalDate.now(), 6) }
    val monthsPreview = remember(pc) {
        (0..2).map { off ->
            val d = LocalDate.now().plusMonths(off.toLong())
            d.format(monthFmt) to incomeForMonth(pc, d.year, d.monthValue)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                "Set this up once — the Budget tab pulls income from here automatically, including months with 3 paycheques.",
                fontSize = 13.sp, color = InkSoft, modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        item {
            SectionCard(accentColor = Pine) {
                Text("Biweekly pay", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                var dateText by remember(pc.firstDate) { mutableStateOf(pc.firstDate) }
                LabeledTextField(
                    label = "First pay date (YYYY-MM-DD)",
                    value = dateText,
                    onValueChange = {
                        dateText = it
                        if (it.length == 10) {
                            runCatching { LocalDate.parse(it) }.onSuccess { _ ->
                                vm.update { s -> s.copy(paycheque = s.paycheque.copy(firstDate = it)) }
                            }
                        }
                    }
                )
                LabeledMoneyField("Gross pay per cheque", pc.gross) { v ->
                    vm.update { s -> s.copy(paycheque = s.paycheque.copy(gross = v)) }
                }
                LabeledMoneyField("Deductions per cheque", pc.deduct) { v ->
                    vm.update { s -> s.copy(paycheque = s.paycheque.copy(deduct = v)) }
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net pay per cheque", fontWeight = FontWeight.Bold)
                    MoneyText(pc.gross - pc.deduct)
                }
            }
        }
        item {
            SectionCard {
                Text("Next 6 paycheques", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
                upcoming.forEach { p: PaycheckOccurrence ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(p.date.format(dateFmt), fontSize = 13.sp)
                        Text(money2(p.net), fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            SectionCard {
                Text("Income by month (next 3 months)", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
                monthsPreview.forEach { (label, amount) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, fontSize = 13.sp)
                        Text(money2(amount), fontSize = 13.sp)
                    }
                }
                Text(
                    "Some months land 3 paycheques instead of 2 — that's normal for biweekly pay, not an error.",
                    fontSize = 12.sp, color = InkSoft, modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
