package com.gullen.redmenacemoney.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()
fun todayISO(): String = LocalDate.now().toString() // yyyy-MM-dd

@Serializable
data class Assumptions(
    val kenDOB: String = "",
    val wifeDOB: String = "",
    val retirementDate: String = ""
)

@Serializable
data class Paycheque(
    val firstDate: String = todayISO(),
    val gross: Double = 0.0,
    val deduct: Double = 0.0
)

@Serializable
data class BudgetItem(
    val id: String = newId(),
    val name: String = "",
    val amount: Double = 0.0
)

@Serializable
data class Debt(
    val balance: Double = 0.0,
    val rate: Double = 0.0,
    val payment: Double = 0.0,
    val frequency: String = "monthly", // "monthly" | "biweekly"
    val extra: Double = 0.0,
    val lumpAmount: Double = 0.0,
    val lumpMonth: Int = 12
)

@Serializable
data class Debts(
    val mortgage: Debt = Debt(frequency = "biweekly", lumpMonth = 12),
    val heloc: Debt = Debt(),
    val loc: Debt = Debt()
)

@Serializable
data class NetWorthAssets(
    val chequing: Double = 0.0,
    val tfsa: Double = 0.0,
    val rrsp: Double = 0.0,
    val nonReg: Double = 0.0,
    val stock: Double = 0.0,
    val homeValue: Double = 0.0,
    val other: Double = 0.0
) {
    val total: Double get() = chequing + tfsa + rrsp + nonReg + stock + homeValue + other
}

@Serializable
data class NetWorthLiabilities(
    val mortgage: Double = 0.0,
    val heloc: Double = 0.0,
    val loc: Double = 0.0,
    val creditCard: Double = 0.0,
    val otherDebt: Double = 0.0
) {
    val total: Double get() = mortgage + heloc + loc + creditCard + otherDebt
}

@Serializable
data class NetWorthEntry(
    val id: String = newId(),
    val date: String = todayISO(),
    val assets: NetWorthAssets = NetWorthAssets(),
    val liabilities: NetWorthLiabilities = NetWorthLiabilities()
) {
    val netWorth: Double get() = assets.total - liabilities.total
}

// track: "netWorth" | "totalDebt" | "mortgage" | "heloc" | "loc" | "custom" | "date"
@Serializable
data class Goal(
    val id: String = newId(),
    val name: String = "",
    val track: String = "custom",
    val targetValue: Double? = null,
    val targetDate: String? = null,
    val baselineValue: Double? = null,
    val manualCurrent: Double = 0.0,
    val createdDate: String = todayISO()
)

@Serializable
data class Holding(
    val id: String = newId(),
    val symbol: String = "",
    val name: String = "",
    val quantity: Double = 0.0,
    val lastPrice: Double = 0.0,
    val currency: String = "CAD", // "CAD" | "USD"
    val bookCost: Double = 0.0,
    val marketValue: Double = 0.0
) {
    val gainLoss: Double get() = marketValue - bookCost
    val gainLossPct: Double get() = if (bookCost > 0) (gainLoss / bookCost) * 100 else 0.0
}

@Serializable
data class Investments(
    val cashCAD: Double = 0.0,
    val cashUSD: Double = 0.0,
    val exchangeRateUSDtoCAD: Double = 1.35,
    val holdings: List<Holding> = emptyList()
) {
    fun holdingValueCAD(h: Holding) = if (h.currency == "USD") h.marketValue * exchangeRateUSDtoCAD else h.marketValue
    fun holdingBookCostCAD(h: Holding) = if (h.currency == "USD") h.bookCost * exchangeRateUSDtoCAD else h.bookCost
    val marketValueCAD: Double get() = holdings.sumOf { holdingValueCAD(it) } + cashCAD + cashUSD * exchangeRateUSDtoCAD
    val bookCostCAD: Double get() = holdings.sumOf { holdingBookCostCAD(it) }
    val holdingsGainLossCAD: Double get() = holdings.sumOf { holdingValueCAD(it) - holdingBookCostCAD(it) }
}

@Serializable
data class VacationItem(
    val id: String = newId(),
    val category: String = "",
    val budgeted: Double = 0.0,
    val actual: Double = 0.0
)

@Serializable
data class Vacation(
    val id: String = newId(),
    val name: String = "New trip",
    val destination: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val budgetTarget: Double = 0.0,
    val items: List<VacationItem> = emptyList()
) {
    val totalBudgeted: Double get() = items.sumOf { it.budgeted }
    val totalActual: Double get() = items.sumOf { it.actual }
    val remaining: Double get() = totalBudgeted - totalActual
}

@Serializable
data class AppState(
    val assumptions: Assumptions = Assumptions(),
    val paycheque: Paycheque = Paycheque(),
    val budgetIncome: List<BudgetItem> = listOf(BudgetItem(name = "Other Income")),
    val budgetExpense: List<BudgetItem> = defaultExpenseCategories(),
    val debts: Debts = Debts(),
    val netWorthEntries: List<NetWorthEntry> = listOf(NetWorthEntry()),
    val investments: Investments = Investments(),
    val vacations: List<Vacation> = emptyList(),
    val goals: List<Goal> = emptyList()
) {
    val totalDebtNow: Double get() = debts.mortgage.balance + debts.heloc.balance + debts.loc.balance

    val latestNetWorth: Double get() =
        netWorthEntries.maxByOrNull { LocalDate.parse(it.date) }?.netWorth ?: 0.0
}

fun defaultExpenseCategories(): List<BudgetItem> = listOf(
    "Mortgage Payment", "HELOC Payment", "Reliance (Water Heater/Furnace Rental)",
    "Line of Credit Payment", "Greater Sudbury Utilities", "Enbridge Gas", "Cell Phone",
    "Eastlink (Internet/TV)", "Ken's Medications", "Property Tax", "WestJet (Travel)",
    "Costco Mastercard", "Home Insurance", "Groceries", "Transportation/Fuel",
    "Savings/Investment Contribution", "Discretionary/Other"
).map { BudgetItem(name = it) }
