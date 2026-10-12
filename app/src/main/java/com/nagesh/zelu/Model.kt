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
)

// A stored transaction: a Txn plus its classification.
data class Row(
    val id: String,
    val date: String,
    val amount: Double,
    val direction: String,
    val counterparty: String?,
    val ref: String?,
    val raw: String,
    val source: String,
    val kind: String, // income | expense | transfer | refund
    val category: String,
)
