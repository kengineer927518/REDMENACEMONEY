package com.gullen.redmenacemoney.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gullen.redmenacemoney.MainViewModel
import com.gullen.redmenacemoney.data.computeSmithManeuver
import com.gullen.redmenacemoney.ui.components.LabeledMoneyField
import com.gullen.redmenacemoney.ui.components.MoneyText
import com.gullen.redmenacemoney.ui.components.SectionCard
import com.gullen.redmenacemoney.ui.components.money
import com.gullen.redmenacemoney.ui.theme.InkSoft
import com.gullen.redmenacemoney.ui.theme.Pine
import com.gullen.redmenacemoney.ui.theme.Rust

@Composable
fun SmithManeuverScreen(vm: MainViewModel) {
    val state = vm.state
    val result = computeSmithManeuver(state)

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                "Models re-borrowing your mortgage's principal paydown from the HELOC and investing it. " +
                "This is a projection based on the assumptions below, not a guarantee, and isn't tax or investment advice.",
                fontSize = 13.sp, color = InkSoft, modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        item {
            SectionCard {
                Text(
                    "Assumes a readvanceable mortgage (like the CIBC Home Power Plan), HELOC interest paid " +
                    "out of pocket each period rather than capitalized, and that the deductibility of that " +
                    "interest holds up under CRA's rules for your situation — worth confirming with an accountant.",
                    fontSize = 12.sp, color = InkSoft
                )
            }
        }
        item {
            SectionCard {
                Text("Assumptions", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                var returnText by androidx.compose.runtime.remember(state.smithManeuver.investmentReturnRate) {
                    androidx.compose.runtime.mutableStateOf("%.2f".format(state.smithManeuver.investmentReturnRate * 100))
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("Assumed annual investment return (%)", modifier = Modifier.weight(1f), fontSize = 13.sp)
                    androidx.compose.material3.OutlinedTextField(
                        value = returnText,
                        onValueChange = { returnText = it; it.toDoubleOrNull()?.let { pct -> vm.update { s -> s.copy(smithManeuver = s.smithManeuver.copy(investmentReturnRate = pct/100)) } } },
                        modifier = Modifier.width(90.dp),
                        singleLine = true
                    )
                }
                var taxText by androidx.compose.runtime.remember(state.smithManeuver.marginalTaxRate) {
                    androidx.compose.runtime.mutableStateOf("%.2f".format(state.smithManeuver.marginalTaxRate * 100))
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("Marginal tax rate for refund estimate (%)", modifier = Modifier.weight(1f), fontSize = 13.sp)
                    androidx.compose.material3.OutlinedTextField(
                        value = taxText,
                        onValueChange = { taxText = it; it.toDoubleOrNull()?.let { pct -> vm.update { s -> s.copy(smithManeuver = s.smithManeuver.copy(marginalTaxRate = pct/100)) } } },
                        modifier = Modifier.width(90.dp),
                        singleLine = true
                    )
                }
            }
        }
        if (result == null) {
            item { Text("Your mortgage balance is $0 — nothing to model.", color = InkSoft, fontSize = 13.sp) }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                        Text("Projected portfolio at payoff", fontSize = 12.sp, color = InkSoft)
                        MoneyText(result.portfolioValue, fontSize = 20.sp)
                    }
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                        Text("HELOC investment loan at payoff", fontSize = 12.sp, color = InkSoft)
                        MoneyText(result.helocLoanBalance, fontSize = 20.sp)
                    }
                }
            }
            item {
                val net = result.portfolioValue - result.helocLoanBalance
                SectionCard {
                    Text("Over the life of the mortgage", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total principal re-borrowed & invested"); MoneyText(result.cumulativeInvested)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total HELOC interest paid (tax-deductible)"); MoneyText(result.cumulativeInterest)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estimated cumulative tax refund"); MoneyText(result.taxRefund)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net position (portfolio − HELOC loan)", fontWeight = FontWeight.Bold)
                        MoneyText(net, color = if (net >= 0) Pine else Rust)
                    }
                }
            }
            item {
                SectionCard {
                    Text("Year by year", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
                    result.yearly.forEach { y ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Year ${y.year}", fontSize = 12.sp)
                            Text("${money(y.portfolioValue)} vs ${money(y.helocLoanBalance)}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
