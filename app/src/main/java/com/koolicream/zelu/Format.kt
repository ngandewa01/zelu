package com.koolicream.zelu

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.foundation.layout.size
import java.util.Calendar
import java.util.Locale

// Indian digit grouping: 1,76,217 and 12,34,56,789.
fun groupIndian(n: Long): String {
    val s = kotlin.math.abs(n).toString()
    val g = if (s.length <= 3) s else s.dropLast(3).reversed().chunked(2).joinToString(",").reversed() + "," + s.takeLast(3)
    return if (n < 0) "-$g" else g
}

fun money(n: Double): String = "₹" + groupIndian(Math.round(n))

/** With paise: "₹1,76,217.50". */
fun moneyPaise(n: Double): String {
    val p = Math.round(kotlin.math.abs(n) * 100)
    return (if (n < 0) "-₹" else "₹") + groupIndian(p / 100) + "." + String.format(Locale.US, "%02d", p % 100)
}

internal val RANGE_OPTS = listOf(
    "week" to "This week", "month" to "By month", "3m" to "Last 3 months",
    "6m" to "Last 6 months", "year" to "This year", "all" to "All time", "custom" to "Custom dates",
)
internal val TYPE_OPTS = listOf(
    "all" to "Everything", "expense" to "Spending only", "income" to "Money in only",
    "ignore" to "Ignored",
)
internal val CAT_OPTS: List<Pair<String, String>> =
    listOf("All" to "All categories") +
        Classifier.CATEGORIES.keys.map { it to it } +
        listOf("Domestic help" to "Domestic help", "Unknown" to "Unknown")

internal fun pickDate(context: Context, current: String, onPick: (String) -> Unit) {
    val p = current.split("-")
    val c = Calendar.getInstance()
    try { c.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt()) } catch (_: Exception) {}
    DatePickerDialog(context, { _, y, m, d -> onPick("%04d-%02d-%02d".format(y, m + 1, d)) },
        c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
}

internal fun acctTypeLabel(t: String) = when (t) { "card" -> "Credit card"; "loan" -> "Loan"; "debit" -> "Debit card"; "fd" -> "Fixed deposit"; "rd" -> "Recurring deposit"; "other" -> "Wallet"; else -> "Savings account" }

internal fun rowsOfAccount(rows: List<Row>, key: String) = rows.filter { it.account == key || (it.counterparty ?: "") == key }

internal fun plural(n: Int, one: String, many: String) = if (n == 1) "$n $one" else "$n $many"

internal fun acctLine(type: String, count: Int) = when (type) {
    "loan" -> acctTypeLabel(type) + " · " + plural(count, "EMI", "EMIs")
    else -> acctTypeLabel(type) + " · " + plural(count, "payment", "payments")
}

internal fun agoText(ms: Long): String {
    val min = (System.currentTimeMillis() - ms) / 60_000L
    return when {
        min < 1 -> "just now"
        min < 60 -> "$min min ago"
        min < 1440 -> "${min / 60} h ago"
        else -> "${min / 1440} days ago"
    }
}

internal fun fmtPlain(n: Double): String =
    if (n == n.toLong().toDouble()) n.toLong().toString() else n.toString()

// "₹2.37 L", "₹1.20 Cr", "₹57,328": short Indian-style amounts for the plan screen.
internal fun inr(v: Double): String {
    val a = kotlin.math.abs(v)
    val t = when {
        a >= 1e7 -> String.format(Locale.US, "%.2f Cr", a / 1e7)
        a >= 1e5 -> String.format(Locale.US, "%.2f L", a / 1e5)
        else -> groupIndian(Math.round(a))
    }
    return (if (v < 0) "−₹" else "₹") + t
}

// Future value of 1 invested at the start of each month, for `months` months.
internal fun sipFactor(months: Int, monthlyRate: Double): Double =
    if (monthlyRate == 0.0) months.toDouble()
    else ((Math.pow(1 + monthlyRate, months.toDouble()) - 1) / monthlyRate) * (1 + monthlyRate)

internal fun yrMo(m: Int): String {
    val y = m / 12; val mo = m % 12
    return when { y == 0 -> "$mo mo"; mo == 0 -> "$y yr"; else -> "$y yr $mo mo" }
}

internal fun cycleYmd(idx: Int, d: Int): String {
    val y = idx / 12; val mo = idx % 12
    val cal = java.util.Calendar.getInstance().apply { clear(); set(y, mo, 1) }
    val maxd = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    return String.format(java.util.Locale.US, "%04d-%02d-%02d", y, mo + 1, minOf(d, maxd))
}

// Billing cycle [start, end] for a cycle-start day; offset 0 = the cycle containing today.
internal fun cycleRange(day: Int, offset: Int): Pair<String, String> {
    val now = java.util.Calendar.getInstance()
    val idxNow = now.get(java.util.Calendar.YEAR) * 12 + now.get(java.util.Calendar.MONTH)
    val startIdx = (if (now.get(java.util.Calendar.DAY_OF_MONTH) >= day) idxNow else idxNow - 1) + offset
    return cycleYmd(startIdx, day) to Dates.shiftDays(cycleYmd(startIdx + 1, day), -1)
}

internal fun prettyDay(ymd: String): String {
    val m = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return ymd.substring(8, 10).toInt().toString() + " " + m[ymd.substring(5, 7).toInt() - 1]
}

internal fun subtitleFor(t: Row): String = when (t.kind) {
    "expense" -> t.category
    "lent" -> "Lent"
    "borrowed" -> "Borrowed"
    "transfer" -> t.category.ifBlank { "Transfer" }
    "invest" -> "Investment"
    "ignore" -> "Ignored"
    "refund" -> if (t.category != "Refund") "Refund · ${t.category}" else "Refund"
    else -> t.kind.replaceFirstChar { it.uppercase() }
}

internal fun ordinal(n: Int): String = if (n in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }

internal val MONTH_ABBR = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
internal fun prettyDate(iso: String): String = try {
    val p = iso.split("-"); "${p[2]} ${MONTH_ABBR[p[1].toInt() - 1]} ${p[0].takeLast(2)}"
} catch (_: Exception) { iso }

internal fun accountLabel(a: String): String {
    if (a == "All") return "All accounts"
    val p = a.trim().split(" ")
    val last = p.last()
    return if (p.size > 1 && last.all { it.isDigit() })
        (if (a.contains(" CC")) "${p.first()} card ••$last" else "${p.first()} ••$last") else a
}

internal fun dayMonth(d: String) = "${d.substring(8, 10).toInt()} ${MONTH_ABBR[d.substring(5, 7).toInt() - 1]}"

internal fun weekStart(d: String): String {
    val dow = Dates.parse(d).get(java.util.Calendar.DAY_OF_WEEK) // Sun = 1
    return Dates.shiftDays(d, -((dow + 5) % 7))
}

internal fun shortAmt(v: Double): String {
    val a = kotlin.math.abs(v)
    val t = when {
        a >= 100000 -> String.format(java.util.Locale.US, "%.1fL", a / 100000)
        a >= 1000 -> String.format(java.util.Locale.US, "%.0fK", a / 1000)
        else -> String.format(java.util.Locale.US, "%.0f", a)
    }
    return if (v < 0) "-$t" else t
}

internal fun niceCeil(x: Double): Double {
    if (x <= 0) return 1000.0
    val mag = Math.pow(10.0, Math.floor(Math.log10(x)))
    return Math.ceil(x / mag * 2) / 2 * mag
}
