package com.nagesh.zelu

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

object Dates {
    val IST: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    private fun sdf(p: String) = SimpleDateFormat(p, Locale.US).apply { timeZone = IST }

    fun today(): String = sdf("yyyy-MM-dd").format(Date())
    fun todayMonth(): String = sdf("yyyy-MM").format(Date())
    fun dayOfMonth(): Int = GregorianCalendar(IST).get(Calendar.DAY_OF_MONTH)

    fun parse(dateStr: String): Calendar =
        GregorianCalendar(IST).apply { time = sdf("yyyy-MM-dd").parse(dateStr) ?: Date() }

    fun shiftDays(dateStr: String, delta: Int): String {
        val c = parse(dateStr); c.add(Calendar.DAY_OF_MONTH, delta); return sdf("yyyy-MM-dd").format(c.time)
    }

    // First day of the month that is (n-1) months before monthStr (yyyy-MM).
    fun firstDayMonthsAgo(monthStr: String, n: Int): String {
        val (y, mo) = monthStr.split("-").map { it.toInt() }
        val c = GregorianCalendar(IST); c.clear(); c.set(y, mo - 1, 1); c.add(Calendar.MONTH, -(n - 1))
        return sdf("yyyy-MM-dd").format(c.time)
    }

    fun daysInMonth(monthStr: String): Int {
        val (y, mo) = monthStr.split("-").map { it.toInt() }
        val c = GregorianCalendar(IST); c.clear(); c.set(y, mo - 1, 1)
        return c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun shiftMonth(monthStr: String, delta: Int): String {
        val (y, mo) = monthStr.split("-").map { it.toInt() }
        val c = GregorianCalendar(IST); c.clear(); c.set(y, mo - 1, 1); c.add(Calendar.MONTH, delta)
        return SimpleDateFormat("yyyy-MM", Locale.US).apply { timeZone = IST }.format(c.time)
    }

    fun monthLabel(monthStr: String): String {
        val (y, mo) = monthStr.split("-").map { it.toInt() }
        val names = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        return names[mo - 1] + " '" + y.toString().takeLast(2)
    }

    fun monthLong(monthStr: String): String {
        val names = arrayOf("January","February","March","April","May","June","July","August","September","October","November","December")
        return names[monthStr.split("-")[1].toInt() - 1]
    }

    // Recent months ending at todayMonth, most recent last.
    fun recentMonths(todayMonth: String, n: Int): List<String> {
        val (y, mo) = todayMonth.split("-").map { it.toInt() }
        return (0 until n).map {
            val c = GregorianCalendar(IST); c.clear(); c.set(y, mo - 1, 1); c.add(Calendar.MONTH, -(n - 1 - it))
            SimpleDateFormat("yyyy-MM", Locale.US).apply { timeZone = IST }.format(c.time)
        }
    }
}

object Analytics {

    const val BUDGET = 60000.0

    data class Filter(
        val period: String,       // week|month|3m|6m|year|all|custom
        val month: String,        // yyyy-MM (used by month)
        val customFrom: String,
        val customTo: String,
        val account: String,      // "All" or an account label
        val type: String = "all", // all|expense|income
        val category: String = "All",
    )

    // Inclusive [start, end] date range for a filter.
    fun range(f: Filter): Pair<String, String> {
        val today = Dates.today()
        return when (f.period) {
            "week" -> Dates.shiftDays(today, -6) to today
            "month" -> "${f.month}-01" to "${f.month}-%02d".format(Dates.daysInMonth(f.month))
            "3m" -> Dates.firstDayMonthsAgo(Dates.todayMonth(), 3) to today
            "6m" -> Dates.firstDayMonthsAgo(Dates.todayMonth(), 6) to today
            "year" -> "${today.take(4)}-01-01" to "${today.take(4)}-12-31"
            "all" -> "0000-01-01" to "9999-12-31"
            "custom" -> f.customFrom to f.customTo
            else -> "${f.month}-01" to "${f.month}-31"
        }
    }

    fun periodLabel(f: Filter): String = when (f.period) {
        "week" -> "the last 7 days"
        "month" -> Dates.monthLong(f.month)
        "3m" -> "the last 3 months"
        "6m" -> "the last 6 months"
        "year" -> Dates.today().take(4)
        "all" -> "all time"
        "custom" -> "${f.customFrom} to ${f.customTo}"
        else -> Dates.monthLong(f.month)
    }

    fun filter(rows: List<Row>, f: Filter): List<Row> {
        val (start, end) = range(f)
        return rows.filter { r ->
            r.date in start..end &&
                (f.account == "All" || r.account == f.account) &&
                typeOk(r, f.type) &&
                catOk(r, f.category)
        }
    }

    private fun typeOk(r: Row, type: String): Boolean = when (type) {
        "expense" -> r.kind == "expense"
        "income" -> r.kind == "income" || r.kind == "refund"
        "ignore" -> r.kind == "ignore"
        // "Everything" hides ignored rows — pick "Ignored" to see and un-ignore them.
        else -> r.kind != "ignore"
    }

    private fun catOk(r: Row, cat: String): Boolean = when (cat) {
        "All" -> true
        "Unknown" -> r.kind == "expense" && r.category == "Unknown"
        else -> r.category == cat
    }

    data class Summary(val spent: Double, val income: Double, val net: Double, val pending: Int)

    fun summarize(list: List<Row>): Summary {
        var spent = 0.0; var income = 0.0; var pending = 0
        for (t in list) when (t.kind) {
            "expense" -> { spent += t.amount; if (t.category == "Unknown") pending++ }
            "income", "refund" -> income += t.amount
        }
        return Summary(spent, income, income - spent, pending)
    }

    fun byCategory(list: List<Row>): List<Pair<String, Double>> {
        val m = LinkedHashMap<String, Double>()
        list.filter { it.kind == "expense" }.forEach { m[it.category] = (m[it.category] ?: 0.0) + it.amount }
        return m.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }

    // Expense totals by category for one month/account — used for month-over-month deltas.
    fun categoryTotals(rows: List<Row>, month: String, account: String): Map<String, Double> {
        val m = HashMap<String, Double>()
        rows.filter { it.kind == "expense" && it.date.startsWith(month) && (account == "All" || it.account == account) }
            .forEach { m[it.category] = (m[it.category] ?: 0.0) + it.amount }
        return m
    }

    data class Payee(val name: String, val amount: Double, val count: Int, val cats: List<String>)

    fun byPayee(list: List<Row>): List<Payee> {
        val amt = LinkedHashMap<String, Double>()
        val cnt = LinkedHashMap<String, Int>()
        val cats = LinkedHashMap<String, LinkedHashSet<String>>()
        list.filter { it.kind == "expense" }.forEach { t ->
            val n = t.counterparty?.takeIf { it.isNotBlank() } ?: t.category
            amt[n] = (amt[n] ?: 0.0) + t.amount
            cnt[n] = (cnt[n] ?: 0) + 1
            cats.getOrPut(n) { LinkedHashSet() }.add(t.category)
        }
        return amt.entries.sortedByDescending { it.value }
            .map { Payee(it.key, it.value, cnt[it.key] ?: 1, cats[it.key]?.toList() ?: emptyList()) }
    }

    // Spend per day-of-month for the calendar (index 1..daysInMonth).
    fun dailyTotals(allRows: List<Row>, month: String, account: String): DoubleArray {
        val dim = Dates.daysInMonth(month)
        val arr = DoubleArray(dim + 1)
        allRows.filter { it.kind == "expense" && it.date.startsWith(month) && (account == "All" || it.account == account) }
            .forEach { val d = it.date.substring(8, 10).toInt(); if (d in 1..dim) arr[d] += it.amount }
        return arr
    }

    fun spentByAccount(list: List<Row>, accounts: List<String>): List<Pair<String, Double>> =
        accounts.filter { it != "All" }.map { a ->
            a to list.filter { it.account == a && it.kind == "expense" }.sumOf { it.amount }
        }.filter { it.second > 0 }

    fun lent(list: List<Row>) = list.filter { it.kind == "lent" }
    fun borrowed(list: List<Row>) = list.filter { it.kind == "borrowed" }
    // Investments (PF, NPS, mutual funds) — tracked on their own, not spend or income.
    fun investments(list: List<Row>) = list.filter { it.kind == "invest" }.sortedByDescending { it.date }

    data class Insight(val topCat: String, val topCatAmt: Double, val topCatPct: Int,
                       val biggestName: String, val biggestAmt: Double,
                       val overBy: Double?)

    fun insight(list: List<Row>, f: Filter): Insight? {
        val expenses = list.filter { it.kind == "expense" }
        if (expenses.isEmpty()) return null
        val cats = byCategory(list)
        val spent = expenses.sumOf { it.amount }
        val top = cats.first()
        val biggest = expenses.maxByOrNull { it.amount }!!
        val overBy = if (f.period == "month" && f.month == Dates.todayMonth()) {
            spent - BUDGET * Dates.dayOfMonth() / Dates.daysInMonth(f.month)
        } else null
        return Insight(top.first, top.second, (top.second / spent * 100).toInt(),
            biggest.counterparty?.takeIf { it.isNotBlank() } ?: biggest.category, biggest.amount, overBy)
    }

    // Top N expenses (by amount) in the list — for highlighting big spends.
    fun bigSpendIds(list: List<Row>, n: Int = 3): Set<String> =
        list.filter { it.kind == "expense" }.sortedByDescending { it.amount }.take(n).map { it.id }.toSet()

    // Cumulative spend by day for a month (for the pace chart), up to `upto`.
    fun runningTotals(allRows: List<Row>, month: String, account: String, upto: Int): DoubleArray {
        val daily = dailyTotals(allRows, month, account)
        val out = DoubleArray(upto + 1)
        var run = 0.0
        for (d in 1..upto) { run += daily[d]; out[d] = run }
        return out
    }

    // Spending + income per month for the three-month bars.
    fun monthlySpendIncome(allRows: List<Row>, months: List<String>, account: String): List<Triple<String, Double, Double>> =
        months.map { m ->
            val inM = allRows.filter { it.date.startsWith(m) && (account == "All" || it.account == account) }
            val spent = inM.filter { it.kind == "expense" }.sumOf { it.amount }
            val income = inM.filter { it.kind == "income" || it.kind == "refund" }.sumOf { it.amount }
            Triple(m, spent, income)
        }
}
