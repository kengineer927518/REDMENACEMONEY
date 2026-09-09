package com.gullen.redmenacemoney.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gullen.redmenacemoney.MainViewModel
import com.gullen.redmenacemoney.data.NetWorthEntry
import com.gullen.redmenacemoney.ui.components.LabeledMoneyField
import com.gullen.redmenacemoney.ui.components.MoneyText
import com.gullen.redmenacemoney.ui.components.SectionCard
import com.gullen.redmenacemoney.ui.theme.InkSoft
import com.gullen.redmenacemoney.ui.theme.Rust
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun NetWorthScreen(vm: MainViewModel) {
    val state = vm.state
    val sorted = state.netWorthEntries.sortedByDescending { LocalDate.parse(it.date) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                "Log a snapshot whenever you check your accounts. Mortgage/HELOC/LOC pre-fill from the Debts tab but you can adjust them for this entry.",
                fontSize = 13.sp, color = InkSoft, modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        item {
            Button(onClick = {
                val d = state.debts
                val last = state.netWorthEntries.maxByOrNull { LocalDate.parse(it.date) }
                val baseAssets = (last?.assets ?: com.gullen.redmenacemoney.data.NetWorthAssets())
                    .copy(tfsa = state.investments.marketValueCAD)
                val newEntry = NetWorthEntry(
                    date = LocalDate.now().toString(),
                    assets = baseAssets,
                    liabilities = com.gullen.redmenacemoney.data.NetWorthLiabilities(
                        mortgage = d.mortgage.balance,
                        heloc = d.heloc.balance,
                        loc = d.loc.balance,
                        creditCard = last?.liabilities?.creditCard ?: 0.0,
                        otherDebt = last?.liabilities?.otherDebt ?: 0.0
                    )
                )
                vm.update { s -> s.copy(netWorthEntries = s.netWorthEntries + newEntry) }
            }, modifier = Modifier.padding(bottom = 8.dp)) { Text("+ Log a snapshot") }
        }
        if (sorted.isEmpty()) {
            item { Text("No snapshots logged yet.", color = InkSoft, fontSize = 13.sp) }
        } else {
            items(sorted, key = { it.id }) { entry ->
                NetWorthEntryCard(entry) { updated ->
                    vm.update { s -> s.copy(netWorthEntries = s.netWorthEntries.map { if (it.id == entry.id) updated else it }) }
                }
                NetWorthEntryDeleteButton(entry) {
                    vm.update { s -> s.copy(netWorthEntries = s.netWorthEntries.filter { it.id != entry.id }) }
                }
            }
        }
    }
}

@Composable
private fun NetWorthEntryCard(entry: NetWorthEntry, onChange: (NetWorthEntry) -> Unit) {
    SectionCard {
        Text(LocalDate.parse(entry.date).format(dateFmt), fontWeight = FontWeight.Bold, fontSize = 15.sp)

        Text("Assets", fontSize = 12.sp, color = InkSoft, modifier = Modifier.padding(top = 8.dp))
        LabeledMoneyField("Chequing/Savings", entry.assets.chequing) { onChange(entry.copy(assets = entry.assets.copy(chequing = it))) }
        LabeledMoneyField("TFSA", entry.assets.tfsa) { onChange(entry.copy(assets = entry.assets.copy(tfsa = it))) }
        LabeledMoneyField("RRSP", entry.assets.rrsp) { onChange(entry.copy(assets = entry.assets.copy(rrsp = it))) }
        LabeledMoneyField("Non-Reg Investments", entry.assets.nonReg) { onChange(entry.copy(assets = entry.assets.copy(nonReg = it))) }
        LabeledMoneyField("Solium/Company Stock", entry.assets.stock) { onChange(entry.copy(assets = entry.assets.copy(stock = it))) }
        LabeledMoneyField("Home Value", entry.assets.homeValue) { onChange(entry.copy(assets = entry.assets.copy(homeValue = it))) }
        LabeledMoneyField("Other Assets", entry.assets.other) { onChange(entry.copy(assets = entry.assets.copy(other = it))) }

        Text("Liabilities", fontSize = 12.sp, color = InkSoft, modifier = Modifier.padding(top = 10.dp))
        LabeledMoneyField("Mortgage", entry.liabilities.mortgage) { onChange(entry.copy(liabilities = entry.liabilities.copy(mortgage = it))) }
        LabeledMoneyField("HELOC", entry.liabilities.heloc) { onChange(entry.copy(liabilities = entry.liabilities.copy(heloc = it))) }
        LabeledMoneyField("Line of Credit", entry.liabilities.loc) { onChange(entry.copy(liabilities = entry.liabilities.copy(loc = it))) }
        LabeledMoneyField("Credit Card", entry.liabilities.creditCard) { onChange(entry.copy(liabilities = entry.liabilities.copy(creditCard = it))) }
        LabeledMoneyField("Other Debt", entry.liabilities.otherDebt) { onChange(entry.copy(liabilities = entry.liabilities.copy(otherDebt = it))) }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Net worth", fontWeight = FontWeight.Bold)
            MoneyText(entry.netWorth)
        }
    }
}

@Composable
private fun NetWorthEntryDeleteButton(entry: NetWorthEntry, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        IconButton(onClick = onDelete) { Icon(Icons.Default.Close, contentDescription = "Remove entry", tint = Rust) }
    }
}
