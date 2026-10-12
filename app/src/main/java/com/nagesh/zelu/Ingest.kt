package com.nagesh.zelu

import android.content.Context

object Ingest {

    data class Result(val found: Int, val added: Int)

    // Parses and classifies raw SMS, then stores the new transactions.
    @Synchronized
    fun ingest(context: Context, raw: List<RawSms>): Result {
        val settings = Storage.loadSettings(context)
        val senders = Storage.splitList(settings.bankSenders).map { it.lowercase() }
        val own = Storage.splitList(settings.ownAccounts)

        val rows = ArrayList<Row>()
        for (msg in raw) {
            val address = msg.address.lowercase()
            // With bank senders configured, ignore personal chats entirely.
            if (senders.isNotEmpty() && senders.none { address.contains(it) }) continue

            val txn = Parser.parseSms(msg.body, msg.date, "phone:${msg.address}") ?: continue
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
                    kind = c.kind,
                    category = c.category,
                ),
            )
        }

        val added = Storage.saveRows(context, rows)
        return Result(rows.size, added)
    }
}
