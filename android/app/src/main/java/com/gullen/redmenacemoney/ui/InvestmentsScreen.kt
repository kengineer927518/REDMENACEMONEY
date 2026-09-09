package com.gullen.redmenacemoney.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gullen.redmenacemoney.MainViewModel
import com.gullen.redmenacemoney.data.Holding
import com.gullen.redmenacemoney.ui.components.LabeledMoneyField
import com.gullen.redmenacemoney.ui.components.MoneyText
import com.gullen.redmenacemoney.ui.components.SectionCard
import com.gullen.redmenacemoney.ui.components.money2
import com.gullen.redmenacemoney.ui.theme.Ink
import com.gullen.redmenacemoney.ui.theme.InkSoft
import com.gullen.redmenacemoney.ui.theme.MonoFamily
import com.gullen.redmenacemoney.ui.theme.Pine
import com.gullen.redmenacemoney.ui.theme.Rail
import com.gullen.redmenacemoney.ui.theme.Rust

@Composable
fun InvestmentsScreen(vm: MainViewModel) {
    val state = vm.state
    val inv = state.investments

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Market value (CAD)", fontSize = 12.sp, color = InkSoft)
                    MoneyText(inv.marketValueCAD, fontSize = 22.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Gain/loss", fontSize = 12.sp, color = InkSoft)
                    MoneyText(inv.holdingsGainLossCAD, fontSize = 22.sp, color = if (inv.holdingsGainLossCAD < 0) Rust else Pine)
                }
            }
        }
        item {
            SectionCard(accentColor = Rail) {
                Text("Cash & exchange rate", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                LabeledMoneyField("Cash (CAD)", inv.cashCAD) { v ->
                    vm.update { s -> s.copy(investments = s.investments.copy(cashCAD = v)) }
                }
                LabeledMoneyField("Cash (USD)", inv.cashUSD) { v ->
                    vm.update { s -> s.copy(investments = s.investments.copy(cashUSD = v)) }
                }
                LabeledMoneyField("USD → CAD rate", inv.exchangeRateUSDtoCAD) { v ->
                    vm.update { s -> s.copy(investments = s.investments.copy(exchangeRateUSDtoCAD = v)) }
                }
            }
        }
        item {
            Text("Holdings", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        }
        items(inv.holdings, key = { it.id }) { h ->
            HoldingCard(h, onChange = { updated ->
                vm.update { s -> s.copy(investments = s.investments.copy(holdings = s.investments.holdings.map { if (it.id == h.id) updated else it })) }
            }, onDelete = {
                vm.update { s -> s.copy(investments = s.investments.copy(holdings = s.investments.holdings.filter { it.id != h.id })) }
            })
        }
        item {
            Button(onClick = {
                vm.update { s -> s.copy(investments = s.investments.copy(holdings = s.investments.holdings + Holding())) }
            }, modifier = Modifier.padding(vertical = 6.dp)) { Text("+ Add holding") }
        }
    }
}

@Composable
private fun HoldingCard(h: Holding, onChange: (Holding) -> Unit, onDelete: () -> Unit) {
    SectionCard(accentColor = Rail) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            var symbol by remember(h.id) { mutableStateOf(h.symbol) }
            OutlinedTextField(
                value = symbol,
                onValueChange = { symbol = it; onChange(h.copy(symbol = it)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, fontFamily = MonoFamily)
            )
            IconButton(onClick = onDelete) { Icon(Icons.Default.Close, contentDescription = "Remove", tint = Rust) }
        }
        var name by remember(h.id) { mutableStateOf(h.name) }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; onChange(h.copy(name = it)) },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp)
        )
        LabeledMoneyField("Quantity", h.quantity) { onChange(h.copy(quantity = it)) }
        LabeledMoneyField("Last price", h.lastPrice) { onChange(h.copy(lastPrice = it)) }
        var expanded by remember { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Currency", modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink)
            androidx.compose.foundation.layout.Box {
                OutlinedButton(onClick = { expanded = true }) { Text(h.currency) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("CAD") }, onClick = { onChange(h.copy(currency = "CAD")); expanded = false })
                    DropdownMenuItem(text = { Text("USD") }, onClick = { onChange(h.copy(currency = "USD")); expanded = false })
                }
            }
        }
        LabeledMoneyField("Book cost", h.bookCost) { onChange(h.copy(bookCost = it)) }
        LabeledMoneyField("Market value", h.marketValue) { onChange(h.copy(marketValue = it)) }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Gain/loss")
            MoneyText(h.gainLoss, color = if (h.gainLoss < 0) Rust else Pine)
        }
    }
}
