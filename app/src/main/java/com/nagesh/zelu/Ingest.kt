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
        val reminders = ArrayList<Reminder>()
        val balances = HashMap<String, Pair<Double, Long>>()
        for (msg in raw) {
            // 1) Trust the sender first: accept only bank/financial sender IDs
            //    (HDFCBK, ICICIB, SBIINB…). Personal numbers, astrology apps,
            //    lottery and fake-KYC senders are not banks — drop before parsing.
            if (!Parser.isFinancialSender(msg.address)) continue
            // 2) A genuine bank alert never carries a link shortener or a throwaway
            //    short URL. If it does, the message is spoofed / phishing — ignore it.
            if (Parser.hasSuspiciousLink(msg.body)) continue

            val txn = Parser.parseSms(msg.body, msg.date, msg.address)
            if (txn == null) {
                // Balance-only alert -> remember the latest balance per account.
                val bal = Parser.parseBalance(msg.body, msg.date, msg.address)
                if (bal != null) {
                    val cur = balances[bal.first]
                    if (cur == null || bal.third >= cur.second) balances[bal.first] = bal.second to bal.third
                    continue
                }
                // Not a completed payment — it may be a bill/EMI reminder for the Dues page.
                Parser.parseReminder(msg.body, msg.date, msg.address)?.let { reminders.add(it) }
                continue
            }
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
        Storage.saveReminders(context, reminders)
        Storage.saveBalances(context, balances)
        return Result(rows.size, added)
    }
}
