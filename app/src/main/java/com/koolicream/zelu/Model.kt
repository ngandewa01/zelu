package com.koolicream.zelu

// A raw SMS as read from the inbox or received by the broadcast receiver.
data class RawSms(
    val address: String,
    val body: String,
    val date: Long, // epoch milliseconds
)

// A parsed bank/UPI transaction.
data class Txn(
    val id: String,
    val date: String, // YYYY-MM-DD (IST)
    val amount: Double,
    val direction: String, // "debit" or "credit"
    val counterparty: String?,
    val ref: String?,
    val raw: String,
    val source: String,
    val account: String, // e.g. "HDFC 5997", "HDFC CC 1291", or "" if unknown
    val ms: Long = 0,    // when the message arrived; orders payments within a day
)

// A bill / payment reminder parsed from a "due" or "will be debited" SMS. These are NOT
// transactions (no money has left the account yet) — they live on the Dues page so you can
// see what's coming up or overdue and tick it off once paid.
data class Reminder(
    val id: String,
    val dueDate: String,       // YYYY-MM-DD
    val amount: Double,
    val minAmount: Double?,     // for credit-card bills ("Min Amt Due")
    val counterparty: String?,
    val account: String,
    val recurring: Boolean,     // repeats monthly (EMI / SIP / autopay / subscription)
    val raw: String,
    val source: String,
    val done: Boolean = false,  // ticked off as paid (applied at load time)
)

// A stored transaction: a Txn plus its classification.
// kind is one of: expense, income, refund, transfer, lent, borrowed, invest, ignore
// A UPI AutoPay / e-mandate you set up (or that was changed), built from the bank's messages.
// status: "active", "scheduled" (a debit is lined up for nextDate), "paused" or "ended".
data class Mandate(
    val key: String,
    val merchant: String,
    val amount: Double?,
    val account: String,
    val status: String,
    val nextDate: String?,
    val since: Long,
    val lastMs: Long,
    val raw: String,
    val source: String,
)

data class Row(
    val id: String,
    val date: String,
    val amount: Double,
    val direction: String,
    val counterparty: String?,
    val ref: String?,
    val raw: String,
    val source: String,
    val account: String,
    val kind: String,
    val category: String,
    val ms: Long = 0,    // when the message arrived; orders payments within a day
)

/**
 * The time of day a payment happened: the time written in its message ("at 18:27:54", "on 02-05-26 6:05 PM"),
 * and only when the message has none, the time it arrived (India time).
 */
object TxnTime {
    private val IN_TEXT = Regex("""(?i)(?<![\d:.])([01]?\d|2[0-3]):([0-5]\d)(?::([0-5]\d))?\s*([ap]\.?m\.?)?(?![\d:])""")
    private const val IST_OFFSET_MS = 19_800_000L

    /** Seconds since midnight, or -1 when there is nothing to go on. */
    fun secondsOf(r: Row): Int = fromText(r.raw) ?: fromArrival(r.ms)

    fun fromText(raw: String): Int? {
        val m = IN_TEXT.find(raw) ?: return null
        var h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        val sec = m.groupValues[3].takeIf { it.isNotEmpty() }?.toInt() ?: 0
        val ap = m.groupValues[4].lowercase().firstOrNull()
        if (ap != null) { if (h == 0 || h > 12) return null; if (ap == 'p' && h < 12) h += 12; if (ap == 'a' && h == 12) h = 0 }
        return h * 3600 + min * 60 + sec
    }

    fun fromArrival(ms: Long): Int = if (ms <= 0) -1 else (((ms + IST_OFFSET_MS) / 1000) % 86_400).toInt()

    /** "6:27 PM", or "" when the time is not known. */
    fun label(r: Row): String {
        val s = secondsOf(r)
        if (s < 0) return ""
        val h = s / 3600; val min = s % 3600 / 60
        return (if (h % 12 == 0) 12 else h % 12).toString() + ":" + min.toString().padStart(2, '0') + (if (h < 12) " AM" else " PM")
    }
}

/** Newest first: by date, then by the time of day of the payment (from its message, else when it arrived). */
val NEWEST_FIRST: Comparator<Row> = compareByDescending<Row> { it.date }
    .thenByDescending { TxnTime.secondsOf(it) }.thenByDescending { it.ms }
fun List<Row>.newestFirst(): List<Row> = sortedWith(NEWEST_FIRST)

// Refunds that look like they belong to an earlier spend, waiting for a Yes/No. (refund, spend)
object Refunds {
    // Compose state, so the card appears/disappears as soon as the list changes.
    private val state = androidx.compose.runtime.mutableStateOf<List<Pair<Row, Row>>>(emptyList())
    var suggestions: List<Pair<Row, Row>>
        get() = state.value
        set(v) { state.value = v }
}
