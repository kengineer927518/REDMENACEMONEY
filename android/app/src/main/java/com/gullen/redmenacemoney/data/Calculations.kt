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

fun amortizeDebt(debt: Debt, isMortgage: Boolean, startDate: LocalDate): AmortResult = amortize(
    balance = debt.balance,
    annualRate = debt.rate,
    payment = debt.payment,
    frequency = if (isMortgage) debt.frequency else "monthly",
    extra = debt.extra,
    lumpAmount = if (isMortgage) debt.lumpAmount else 0.0,
    lumpMonth = if (isMortgage) debt.lumpMonth else 0,
    startDate = startDate,
    maxPeriods = 1000
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

fun getCurrentValueForGoal(goal: Goal, state: AppState): Double = when (goal.track) {
    "netWorth" -> state.latestNetWorth
    "totalDebt" -> state.totalDebtNow
    "mortgage" -> state.debts.mortgage.balance
    "heloc" -> state.debts.heloc.balance
    "loc" -> state.debts.loc.balance
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
