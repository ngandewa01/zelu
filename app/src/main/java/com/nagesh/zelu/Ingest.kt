package com.nagesh.zelu

import android.content.Context

object Ingest {

    data class Result(val found: Int, val added: Int)

    // Parses and classifies raw SMS, then stores the new transactions.
    @Synchronized
    fun ingest(context: Context, raw: List<RawSms>): Result {
        val settings = Storage.loadSettings(context)
        val own = Storage.splitList(settings.ownAccounts)

        val rows = ArrayList<Row>()
        for (msg in raw) {
            // Bank/UPI alerts come from alphanumeric sender IDs (e.g. AX-SBIUPI);
            // personal chats come from plain phone numbers. Keep only the former,
            // so no manual bank-sender setup is needed.
            if (!msg.address.any { it.isLetter() }) continue

            val txn = Parser.parseSms(msg.body, msg.date, msg.address) ?: continue
            val c = Classifier.classify(txn, own)
            rows.add(
                Row(
                    id = txn.id,
                    date = txn.date,
                    amount = txn.amount,
                    direction = txn.direction,
                    counterparty = txn.counterparty,
                    ref = txn.ref,
                    raw = txn.raw,
                    source = txn.source,
                    account = txn.account,
                    kind = c.kind,
                    category = c.category,
                ),
            )
        }

        val added = Storage.saveRows(context, rows)
        return Result(rows.size, added)
    }
}
