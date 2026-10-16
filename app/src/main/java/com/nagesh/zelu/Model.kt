package com.nagesh.zelu

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
)

// Refunds that look like they belong to an earlier spend, waiting for a Yes/No. (refund, spend)
object Refunds {
    @Volatile var suggestions: List<Pair<Row, Row>> = emptyList()
}
