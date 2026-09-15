package com.gullen.redmenacemoney.data

import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

data class SchedulePeriod(
    val n: Int,
    val date: LocalDate,
    val interest: Double,
    val principal: Double,
    val lump: Double,
    val balance: Double
)

data class AmortResult(
    val schedule: List<SchedulePeriod>,
    val periodsToPayoff: Int,
    val payoffDate: LocalDate?,
    val totalInterest: Double,
    val coversInterest: Boolean
)

/**
 * Ported from the web app's amortize() function. Verified against the CIBC mortgage
 * statement figures: $105,727 @ 5.57%, $340.82 biweekly, $22,000 lump every December
 * -> 86 periods, payoff 2029-12-12, first lump applied 2026-12-02.
 */
fun amortize(
    balance: Double,
    annualRate: Double,
    payment: Double,
    frequency: String,
    extra: Double = 0.0,
    lumpAmount: Double = 0.0,
    lumpMonth: Int = 0,
    startDate: LocalDate,
    maxPeriods: Int = 1000
): AmortResult {
    val periodsPerYear = if (frequency == "biweekly") 26 else 12
    val periodRate = annualRate / periodsPerYear
    var date = startDate
    var bal = balance
    var totalInterest = 0.0
    val schedule = mutableListOf<SchedulePeriod>()
    var prevMonth: Int? = null

    for (n in 1..maxPeriods) {
        if (n > 1) date = if (frequency == "biweekly") date.plusDays(14) else date.plusMonths(1)
        if (bal <= 0) break
        val interest = bal * periodRate
        val month = date.monthValue
        val lump = if (lumpAmount > 0 && month == lumpMonth && month != prevMonth) lumpAmount else 0.0
        prevMonth = month
        val principal = payment + extra + lump - interest
        totalInterest += interest
        bal = max(0.0, bal - principal)
        schedule.add(SchedulePeriod(n, date, interest, principal, lump, bal))
        if (bal <= 0) break
    }

    return AmortResult(
        schedule = schedule,
        periodsToPayoff = schedule.size,
        payoffDate = schedule.lastOrNull()?.date,
        totalInterest = totalInterest,
        coversInterest = schedule.firstOrNull()?.let { (payment + extra) > it.interest } ?: true
    )
}

fun amortizeDebt(debt: Debt, isMortgage: Boolean, startDate: LocalDate, maxPeriods: Int = 1000): AmortResult = amortize(
    balance = debt.balance,
    annualRate = debt.rate,
    payment = debt.payment,
    frequency = if (isMortgage) debt.frequency else "monthly",
    extra = debt.extra,
    lumpAmount = if (isMortgage) debt.lumpAmount else 0.0,
    lumpMonth = if (isMortgage) debt.lumpMonth else 0,
    startDate = startDate,
    maxPeriods = maxPeriods
)

data class PaycheckOccurrence(val date: LocalDate, val gross: Double, val deduct: Double) {
    val net: Double get() = gross - deduct
}

fun generatePaycheques(pc: Paycheque, from: LocalDate, count: Int): List<PaycheckOccurrence> {
    var d = LocalDate.parse(pc.firstDate)
    while (d.isBefore(from)) d = d.plusDays(14)
    val list = mutableListOf<PaycheckOccurrence>()
    repeat(count) {
        list.add(PaycheckOccurrence(d, pc.gross, pc.deduct))
        d = d.plusDays(14)
    }
    return list
}

fun incomeForMonth(pc: Paycheque, year: Int, month: Int): Double {
    val start = LocalDate.of(year, month, 1)
    val end = start.plusMonths(1)
    var d = LocalDate.parse(pc.firstDate)
    while (d.isAfter(start)) d = d.minusDays(14)
    var total = 0.0
    while (d.isBefore(end)) {
        if (!d.isBefore(start) && d.isBefore(end)) total += (pc.gross - pc.deduct)
        d = d.plusDays(14)
    }
    return total
}

fun clamp(n: Double, lo: Double, hi: Double) = max(lo, min(hi, n))

data class YearsMonths(val years: Int, val months: Int)

fun yearsMonthsBetween(a: LocalDate, b: LocalDate): YearsMonths {
    if (b.isBefore(a)) return YearsMonths(0, 0)
    var months = (b.year - a.year) * 12 + (b.monthValue - a.monthValue)
    if (b.dayOfMonth < a.dayOfMonth) months--
    months = max(0, months)
    return YearsMonths(months / 12, months % 12)
}

val DEBT_TRACKS = listOf("mortgage", "heloc", "loc", "creditCard")

fun getCurrentValueForGoal(goal: Goal, state: AppState): Double = when (goal.track) {
    "netWorth" -> state.latestNetWorth
    "totalDebt" -> state.totalDebtNow
    "mortgage" -> state.debts.mortgage.balance
    "heloc" -> state.debts.heloc.balance
    "loc" -> state.debts.loc.balance
    "creditCard" -> state.debts.creditCard.balance
    "custom" -> goal.manualCurrent
    else -> 0.0
}

fun goalProgress(goal: Goal, state: AppState): Double {
    if (goal.track == "date") {
        val targetDate = goal.targetDate ?: return 0.0
        val start = LocalDate.parse(goal.createdDate)
        val end = LocalDate.parse(targetDate)
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, end)
        if (totalDays <= 0) return 1.0
        val elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(start, LocalDate.now())
        return clamp(elapsedDays.toDouble() / totalDays.toDouble(), 0.0, 1.0)
    }
    val current = getCurrentValueForGoal(goal, state)
    val base = goal.baselineValue ?: return 0.0
    val target = goal.targetValue ?: return 0.0
    if (target == base) return if (current >= target) 1.0 else 0.0
    return clamp((current - base) / (target - base), 0.0, 1.0)
}

/** For a debt-payoff goal, how much the very next scheduled payment will bring the balance down. */
fun nextPaymentImpact(goal: Goal, state: AppState): Double? {
    if (goal.track !in DEBT_TRACKS) return null
    val d = when (goal.track) {
        "mortgage" -> state.debts.mortgage
        "heloc" -> state.debts.heloc
        "loc" -> state.debts.loc
        "creditCard" -> state.debts.creditCard
        else -> return null
    }
    if (d.balance <= 0) return null
    val isMortgage = goal.track == "mortgage"
    val r = amortizeDebt(d, isMortgage, LocalDate.now(), maxPeriods = 1)
    return r.schedule.firstOrNull()?.principal
}

/**
 * Runs the debt's own schedule forward from balanceAsOfDate to today, so the balance
 * reflects scheduled payments made since without needing manual entry. Returns a new
 * AppState (immutable-style, matching the rest of the app) with any debts advanced.
 */
fun autoAdvanceDebtBalances(state: AppState): AppState {
    val today = LocalDate.now()

    fun advance(debt: Debt, isMortgage: Boolean): Debt {
        if (debt.balance <= 0) return debt.copy(balanceAsOfDate = todayISO())
        val asOf = LocalDate.parse(debt.balanceAsOfDate)
        if (!asOf.isBefore(today)) return debt
        val r = amortize(
            balance = debt.balance, annualRate = debt.rate, payment = debt.payment,
            frequency = if (isMortgage) debt.frequency else "monthly",
            extra = debt.extra, lumpAmount = if (isMortgage) debt.lumpAmount else 0.0,
            lumpMonth = if (isMortgage) debt.lumpMonth else 0,
            startDate = asOf, maxPeriods = 2000
        )
        var newBalance = debt.balance
        var advanced = false
        for (p in r.schedule) {
            if (!p.date.isAfter(today)) { newBalance = p.balance; advanced = true } else break
        }
        return if (advanced) debt.copy(balance = newBalance, balanceAsOfDate = todayISO()) else debt
    }

    val newDebts = state.debts.copy(
        mortgage = advance(state.debts.mortgage, isMortgage = true),
        heloc = advance(state.debts.heloc, isMortgage = false),
        loc = advance(state.debts.loc, isMortgage = false),
        creditCard = advance(state.debts.creditCard, isMortgage = false)
    )
    return state.copy(debts = newDebts)
}

data class MonthlyDebtInput(val key: String, val balance: Double, val rate: Double, val payment: Double)
data class StrategyResult(val totalInterest: Double, val months: Int)

fun monthlyEquivalentExtra(debts: Debts): Double {
    val mortMonthlyExtra = if (debts.mortgage.frequency == "biweekly") debts.mortgage.extra * 26 / 12 else debts.mortgage.extra
    val mortLumpMonthly = debts.mortgage.lumpAmount / 12
    return mortMonthlyExtra + mortLumpMonthly + debts.heloc.extra + debts.loc.extra + debts.creditCard.extra
}

/**
 * strategy: "avalanche" (highest rate first, minimizes interest) or "snowball" (smallest
 * balance first, for motivation). lumpAmount is applied once, before any regular payments.
 */
fun simulateDebtStrategy(debtList: List<MonthlyDebtInput>, extraPool: Double, strategy: String, lumpAmount: Double, maxMonths: Int): StrategyResult {
    data class Mutable(val key: String, var balance: Double, val rate: Double, val payment: Double)
    val debts = debtList.map { Mutable(it.key, it.balance, it.rate, it.payment) }.toMutableList()
    val comparator: Comparator<Mutable> = if (strategy == "snowball") compareBy { it.balance } else compareByDescending { it.rate }

    if (lumpAmount > 0) {
        var lump = lumpAmount
        for (d in debts.filter { it.balance > 0 }.sortedWith(comparator)) {
            if (lump <= 0) break
            if (d.balance <= lump) { lump -= d.balance; d.balance = 0.0 } else { d.balance -= lump; lump = 0.0 }
        }
    }

    var totalInterest = 0.0
    var month = 0
    while (debts.any { it.balance > 0.01 } && month < maxMonths) {
        month++
        var freedUp = 0.0
        for (d in debts) {
            if (d.balance <= 0) { freedUp += d.payment; continue }
            val interest = d.balance * (d.rate / 12)
            totalInterest += interest
            var principal = d.payment - interest
            if (principal < 0) principal = 0.0
            d.balance = max(0.0, d.balance - principal)
        }
        var pool = extraPool + freedUp
        for (d in debts.filter { it.balance > 0 }.sortedWith(comparator)) {
            if (pool <= 0) break
            if (d.balance <= pool) { pool -= d.balance; d.balance = 0.0 } else { d.balance -= pool; pool = 0.0 }
        }
    }
    return StrategyResult(totalInterest, month)
}

/** Binary search for the extra payment per period needed to hit a target payoff date. */
fun findRequiredExtra(debt: Debt, isMortgage: Boolean, targetDate: LocalDate, maxPeriods: Int): Double {
    var lo = 0.0
    var hi = debt.balance * 2 + 1000
    repeat(60) {
        val mid = (lo + hi) / 2
        val r = amortize(
            balance = debt.balance, annualRate = debt.rate, payment = debt.payment,
            frequency = if (isMortgage) debt.frequency else "monthly",
            extra = mid, lumpAmount = if (isMortgage) debt.lumpAmount else 0.0,
            lumpMonth = if (isMortgage) debt.lumpMonth else 0,
            startDate = LocalDate.now(), maxPeriods = maxPeriods
        )
        if (r.payoffDate != null && !r.payoffDate.isAfter(targetDate)) hi = mid else lo = mid
    }
    return hi
}

data class SmithYearSnapshot(val year: Int, val portfolioValue: Double, val helocLoanBalance: Double, val cumulativeInterest: Double, val cumulativeInvested: Double)
data class SmithResult(
    val portfolioValue: Double, val helocLoanBalance: Double, val cumulativeInterest: Double,
    val cumulativeInvested: Double, val taxRefund: Double, val yearly: List<SmithYearSnapshot>
)

/**
 * Models re-borrowing the mortgage's principal paydown from a readvanceable HELOC and
 * investing it — the core Smith Maneuver mechanic. HELOC interest is assumed paid out of
 * pocket each period (not capitalized into the loan). Returns null if there's no mortgage
 * balance to model.
 */
fun computeSmithManeuver(state: AppState): SmithResult? {
    val settings = state.smithManeuver
    val mortgage = state.debts.mortgage
    val heloc = state.debts.heloc
    if (mortgage.balance <= 0) return null

    val result = amortizeDebt(mortgage, isMortgage = true, startDate = LocalDate.now(), maxPeriods = 1000)
    val periodsPerYear = if (mortgage.frequency == "biweekly") 26 else 12
    val investPeriodRate = Math.pow(1 + settings.investmentReturnRate, 1.0 / periodsPerYear) - 1
    val helocPeriodRate = heloc.rate / periodsPerYear

    var helocLoanBalance = 0.0
    var portfolioValue = 0.0
    var cumulativeInterest = 0.0
    var cumulativeInvested = 0.0
    val yearly = mutableListOf<SmithYearSnapshot>()
    var lastYearIndex = -1

    result.schedule.forEachIndexed { idx, p ->
        val interestThisPeriod = helocLoanBalance * helocPeriodRate
        cumulativeInterest += interestThisPeriod
        portfolioValue = portfolioValue * (1 + investPeriodRate) + p.principal
        helocLoanBalance += p.principal
        cumulativeInvested += p.principal

        val yearIndex = idx / periodsPerYear
        val snapshot = SmithYearSnapshot(yearIndex + 1, portfolioValue, helocLoanBalance, cumulativeInterest, cumulativeInvested)
        if (yearIndex != lastYearIndex) { yearly.add(snapshot); lastYearIndex = yearIndex }
        else yearly[yearly.size - 1] = snapshot
    }

    return SmithResult(
        portfolioValue = portfolioValue, helocLoanBalance = helocLoanBalance,
        cumulativeInterest = cumulativeInterest, cumulativeInvested = cumulativeInvested,
        taxRefund = cumulativeInterest * settings.marginalTaxRate, yearly = yearly
    )
}
