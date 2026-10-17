package com.nagesh.zelu

import android.content.Context

object Ingest {

    data class Result(val found: Int, val added: Int, val rows: List<Row> = emptyList(),
                      val balanceHits: Map<String, Double> = emptyMap())

    // Parses and classifies raw SMS, then stores the new transactions.
    @Synchronized
    fun ingest(context: Context, raw: List<RawSms>): Result {
        val settings = Storage.loadSettings(context)
        val own = Storage.splitList(settings.ownAccounts)

        val rows = ArrayList<Row>()
        val reminders = ArrayList<Reminder>()
        val balances = HashMap<String, Pair<Double, Long>>()
        val txnBalances = HashMap<String, Pair<Double, Long>>()
        val limits = HashMap<String, Pair<Double, Long>>()
        val balanceHits = LinkedHashMap<String, Double>()
        for (msg in raw) {
            // 1) Trust the sender first: accept only bank/financial sender IDs
            //    (HDFCBK, ICICIB, SBIINB…). Personal numbers, astrology apps,
            //    lottery and fake-KYC senders are not banks — drop before parsing.
            if (!Parser.isFinancialSender(msg.address)) continue
            // 2) A genuine bank alert never carries a link shortener or a throwaway
            //    short URL. If it does, the message is spoofed / phishing — ignore it.
            if (Parser.hasSuspiciousLink(msg.body)) continue

            Parser.cardLimit(msg.body, msg.address)?.let { (acct, amt, isTotal) ->
                val key = if (isTotal) "$acct#total" else acct
                val cur = limits[key]
                if (cur == null || msg.date >= cur.second) limits[key] = amt to msg.date
            }
            val txn = Parser.parseSms(msg.body, msg.date, msg.address)
            if (txn == null) {
                // Balance-only alert -> remember the latest balance per account.
                val bal = Parser.parseBalance(msg.body, msg.date, msg.address)
                if (bal != null) {
                    val cur = balances[bal.first]
                    if (cur == null || bal.third >= cur.second) balances[bal.first] = bal.second to bal.third
                    balanceHits[bal.first] = bal.second
                    continue
                }
                // Not a completed payment — it may be a bill/EMI reminder for the Dues page.
                Parser.parseReminder(msg.body, msg.date, msg.address)?.let { reminders.add(it) }
                continue
            }
            if (txn.account.isNotBlank() && !txn.account.contains(" CC")) {
                Parser.balanceFromTxn(msg.body)?.let { b ->
                    val cur = txnBalances[txn.account]
                    if (cur == null || msg.date >= cur.second) txnBalances[txn.account] = b to msg.date
                }
            }
            if (txn.account.contains(" CC")) {
                Parser.limitFromTxn(msg.body)?.let { l ->
                    val cur = limits[txn.account]
                    if (cur == null || msg.date >= cur.second) limits[txn.account] = l to msg.date
                }
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
        Storage.saveBalances(context, balances, "b")
        Storage.saveBalances(context, txnBalances, "t")
        Storage.saveLimits(context, limits)
        return Result(rows.size, added, rows, balanceHits)
    }

    private val MONEYISH = Regex("""(?i)(rs\.?|inr|₹)\s*[\d,]+""")
    private val ACTIONISH = Regex("""(?i)\b(debit(ed)?|credit(ed)?|paid|sent|spent|withdraw(n|al)?|received|deposit(ed)?|transferred|purchase|txn|upi)\b""")
    private val NOT_TXN = Regex("""(?i)\b(otp|one time password|offer|cashback offer|apply now|pre-?approved|click)\b""")

    // Bank-looking messages that mention money but that Zelu could not turn into a transaction, balance
    // or reminder. These are the ones worth reporting so the reader can be taught about them.
    fun unreadable(context: Context, limit: Int = 40): List<RawSms> {
        val all = try { SmsReader.readInbox(context, 3000) } catch (_: Exception) { return emptyList() }
        val out = ArrayList<RawSms>()
        for (m in all) {
            if (out.size >= limit) break
            if (!Parser.isFinancialSender(m.address) || Parser.hasSuspiciousLink(m.body)) continue
            if (!MONEYISH.containsMatchIn(m.body) || !ACTIONISH.containsMatchIn(m.body) || NOT_TXN.containsMatchIn(m.body)) continue
            if (Parser.parseSms(m.body, m.date, m.address) != null) continue
            if (Parser.parseBalance(m.body, m.date, m.address) != null) continue
            if (Parser.parseReminder(m.body, m.date, m.address) != null) continue
            out.add(m)
        }
        return out
    }
}
