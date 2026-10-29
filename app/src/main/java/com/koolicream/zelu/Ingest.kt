package com.koolicream.zelu

import android.content.Context

object Ingest {

    data class Result(val found: Int, val added: Int, val rows: List<Row> = emptyList(),
                      val balanceHits: Map<String, Double> = emptyMap())

    // What a batch of messages holds, before anything is stored.
    private class Parsed {
        val rows = ArrayList<Row>()
        val reminders = ArrayList<Reminder>()
        val mandates = ArrayList<Mandate>()
        val balances = HashMap<String, Pair<Double, Long>>()
        val txnBalances = HashMap<String, Pair<Double, Long>>()
        val limits = HashMap<String, Pair<Double, Long>>()
        val balanceHits = LinkedHashMap<String, Double>()
    }

    // Parses and classifies raw SMS, then stores the new transactions.
    @Synchronized
    fun ingest(context: Context, raw: List<RawSms>): Result = store(context, read(context, raw), replace = false)

    /**
     * Reads the inbox afresh and replaces what was stored (a new parser version, or "Read everything again"). Payments,
     * AutoPays and bills whose message has left the inbox are read again from their stored copy (see [Rebuild]), and the
     * stored list is replaced in one save, so nothing is lost if the phone stops halfway.
     */
    @Synchronized
    fun rebuild(context: Context, inbox: List<RawSms>): Result {
        val stored = Storage.storedRows(context)
        val plan = Rebuild.plan(inbox, stored, Storage.loadMandates(context), Storage.storedReminders(context))
        Storage.migrateCreditIds(context, stored)
        val p = read(context, inbox + plan.messages)
        p.rows.addAll(plan.keep)
        return store(context, p, replace = true)
    }

    private fun store(context: Context, p: Parsed, replace: Boolean): Result {
        val added = Storage.saveRows(context, p.rows, replace)
        Storage.saveReminders(context, p.reminders, replace)
        Storage.saveMandates(context, p.mandates, replace)
        Storage.saveBalances(context, p.balances, p.txnBalances, replace)
        Storage.saveLimits(context, p.limits)
        return Result(p.rows.size, added, p.rows, p.balanceHits)
    }

    private fun read(context: Context, raw: List<RawSms>): Parsed {
        Storage.loadTrust(context)
        val settings = Storage.loadSettings(context)
        val own = Storage.splitList(settings.ownAccounts)
        val p = Parsed()
        val rows = p.rows
        val reminders = p.reminders
        val mandates = p.mandates
        val taught = Taught.load(context)
        val balances = p.balances
        val txnBalances = p.txnBalances
        val limits = p.limits
        val balanceHits = p.balanceHits
        for (msg in raw) {
            // 1) Trust the sender first: accept only bank/financial sender IDs
            //    (HDFCBK, ICICIB, SBIINB…). Personal numbers, astrology apps,
            //    lottery and fake-KYC senders are not banks — drop before parsing.
            if (!Parser.isFinancialSender(msg.address)) continue
            // 2) A genuine bank alert never carries a link shortener or a throwaway
            //    short URL. If it does, the message is spoofed / phishing — ignore it.
            if (Parser.hasSuspiciousLink(msg.body, msg.address)) continue

            Parser.cardLimit(msg.body, msg.address)?.let { (acct, amt, isTotal) ->
                val key = if (isTotal) "$acct#total" else acct
                val cur = limits[key]
                if (cur == null || msg.date >= cur.second) limits[key] = amt to msg.date
            }
            val upSender = msg.address.uppercase()
            val sideSender = upSender in Parser.walletSenders || upSender in Parser.otherSenders
            val txn = (Parser.parseSms(msg.body, msg.date, msg.address) ?: Taught.apply(taught, msg))
                ?.let { if (sideSender) it.copy(account = "") else it }
            if (txn == null) {
                // Balance-only alert -> remember the latest balance per account.
                val bal = Parser.parseBalance(msg.body, msg.date, msg.address)
                if (bal != null) {
                    val cur = balances[bal.first]
                    if (cur == null || bal.third >= cur.second) balances[bal.first] = bal.second to bal.third
                    balanceHits[bal.first] = bal.second
                    continue
                }
                // A UPI AutoPay / mandate notice: remember it for the Dues page, and line up the next debit.
                val mand = Parser.parseMandate(msg.body, msg.date, msg.address)
                if (mand != null) {
                    mandates.add(mand)
                    val due = mand.nextDate
                    val amt = mand.amount
                    if (due != null && amt != null && due >= Dates.today()) {
                        reminders.add(Reminder("mand-" + mand.key + "-" + due, due, amt, null, mand.merchant, mand.account, true, mand.raw, mand.source))
                    }
                    continue
                }
                // Not a completed payment — it may be a bill/EMI reminder for the Dues page.
                Parser.parseReminder(msg.body, msg.date, msg.address)?.let { reminders.add(it) }
                continue
            }
            Parser.nextReminder(msg.body, msg.date, msg.address)?.let { reminders.add(it) }
            if (txn.account.isNotBlank() && !txn.account.contains(" CC") && !Parser.isLoanAccountText(msg.body)) {
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
            val c = if (upSender in Parser.walletSenders) Classifier.Result(Kind.IGNORE, "Wallet") else Classifier.classify(txn, own)
            rows.add(
                Row(
                    id = txn.id,
                    date = txn.date,
                    paise = txn.paise,
                    direction = txn.direction,
                    counterparty = txn.counterparty,
                    ref = txn.ref,
                    raw = txn.raw,
                    source = txn.source,
                    account = txn.account,
                    kind = c.kind,
                    category = c.category,
                    ms = txn.ms,
                ),
            )
        }
        return p
    }

    private val MONEYISH = Regex("""(?i)(rs\.?|inr|₹)\s*[\d,]+""")
    private val ACTIONISH = Regex("""(?i)\b(debit(ed)?|credit(ed)?|paid|sent|spent|withdraw(n|al)?|received|deposit(ed)?|transferred|purchase|txn|upi)\b""")
    private val NOT_TXN = Regex("""(?i)\b(otp|one time password|offer|cashback offer|apply now|pre-?approved|click)\b""")
    /**
     * A bank-looking message that mentions money changing hands, and that Zelu's parser does not set aside on purpose
     * (a declined or failed attempt, an OTP, a debit still to come...): worth listing when it can't be read.
     */
    internal fun looksLikeMoneyMoved(body: String, source: String): Boolean =
        MONEYISH.containsMatchIn(body) && ACTIONISH.containsMatchIn(body) && !NOT_TXN.containsMatchIn(body) &&
            !Parser.isNotAPayment(body, source)

    // Bank-looking messages that mention money but that Zelu could not turn into a transaction, balance
    // or reminder. These are the ones worth reporting so the reader can be taught about them.
    fun smsKey(m: RawSms): String = m.date.toString() + "|" + m.body.hashCode()

    fun unreadable(context: Context, limit: Int = 40): List<RawSms> {
        Storage.loadTrust(context)
        val handled = Storage.handledSms(context)
        val taught = Taught.load(context)
        val all = try { SmsReader.readInbox(context, 3000) } catch (e: Exception) { CrashLog.warn("Ingest.unreadable", e); return emptyList() }
        val out = ArrayList<RawSms>()
        for (m in all) {
            if (out.size >= limit) break
            if (!Parser.isFinancialSender(m.address) || Parser.hasSuspiciousLink(m.body, m.address)) continue
            if (!looksLikeMoneyMoved(m.body, m.address)) continue
            if (Parser.parseSms(m.body, m.date, m.address) != null) continue
            if (Parser.parseBalance(m.body, m.date, m.address) != null) continue
            if (Parser.parseReminder(m.body, m.date, m.address) != null) continue
            if (Parser.parseMandate(m.body, m.date, m.address) != null) continue
            if (smsKey(m) in handled) continue
            if (Taught.apply(taught, m) != null) continue
            out.add(m)
        }
        return out
    }

    // Senders Zelu skipped as "not a bank" although their messages read like payments.
    data class UnknownSender(val address: String, val count: Int, val samples: List<String>)

    fun unrecognisedSenders(context: Context): List<UnknownSender> {
        Storage.loadTrust(context)
        val skipped = Storage.handledSms(context)
        val all = try { SmsReader.readInbox(context, 5000) } catch (e: Exception) { CrashLog.warn("Ingest.unrecognisedSenders", e); return emptyList() }
        val by = LinkedHashMap<String, MutableList<String>>()
        for (m in all) {
            if (m.address.isBlank() || Parser.isFinancialSender(m.address)) continue
            if (("sender|" + m.address.uppercase()) in skipped) continue
            if (Parser.hasSuspiciousLink(m.body, m.address)) continue
            if (!looksLikeMoneyMoved(m.body, m.address)) continue
            if (Parser.parseSms(m.body, m.date, m.address) == null) continue
            by.getOrPut(m.address) { ArrayList() }.add(m.body)
        }
        return by.entries.map { UnknownSender(it.key, it.value.size, it.value.take(2)) }.sortedByDescending { it.count }.take(15)
    }

    // The SMS an account on the Home screen was built from, so you can judge whether it belongs there.
    data class Ref(val sender: String, val date: String, val body: String, val why: String)

    fun referencesFor(context: Context, account: String, rows: List<Row>): List<Ref> {
        Storage.loadTrust(context)
        val out = ArrayList<Ref>()
        val fmt = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.US)
        try {
            for (m in SmsReader.readInbox(context, 5000)) {
                if (out.size >= 3) break
                if (!Parser.isFinancialSender(m.address) || Parser.hasSuspiciousLink(m.body, m.address)) continue
                val txn = Parser.parseSms(m.body, m.date, m.address)
                val acct = txn?.account ?: Parser.parseBalance(m.body, m.date, m.address)?.first ?: continue
                val hasBal = Parser.balanceFromTxn(m.body) != null || txn == null
                if (acct == account && hasBal) out.add(Ref(m.address, fmt.format(java.util.Date(m.date)), m.body,
                    if (txn == null) "Balance alert" else "Balance in a transaction alert"))
            }
        } catch (e: Exception) { CrashLog.warn("Ingest.referencesFor", e) }
        rows.filter { it.account == account }.sortedByDescending { it.date }.take(2).forEach {
            out.add(Ref(it.source, it.date, it.raw, "Transaction on this account"))
        }
        return out
    }

    data class Check(
        val bankSms: Int, val txns: Int, val balanceAlerts: Int, val reminders: Int, val unread: Int,
        val gaps: List<Pair<String, Int>>, // account -> times the running balance doesn't add up
    )

    // Reads the inbox the way the app does and reports what became what. On each account, consecutive
    // transaction alerts that state a balance must add up (previous balance +/- the amount = next balance);
    // where they don't, a message is missing between them.
    fun diagnose(context: Context): Check {
        Storage.loadTrust(context)
        var bank = 0; var txns = 0; var bal = 0; var rem = 0; var unread = 0
        val points = HashMap<String, ArrayList<Triple<Long, Double, Double>>>() // account -> (time, balance, signed amount)
        val all = try { SmsReader.readInbox(context, 5000) } catch (e: Exception) { CrashLog.warn("Ingest.diagnose", e); emptyList() }
        for (m in all) {
            if (!Parser.isFinancialSender(m.address) || Parser.hasSuspiciousLink(m.body, m.address)) continue
            bank++
            val t = Parser.parseSms(m.body, m.date, m.address)
            if (t != null) {
                txns++
                val b = Parser.balanceFromTxn(m.body)
                if (b != null && t.account.isNotBlank() && !t.account.contains(" CC") && !Parser.isLoanAccountText(m.body))
                    points.getOrPut(t.account) { ArrayList() }.add(Triple(m.date, b, if (t.direction == Dir.DEBIT) -t.amount else t.amount))
            } else if (Parser.parseBalance(m.body, m.date, m.address) != null) bal++
            else if (Parser.parseReminder(m.body, m.date, m.address) != null) rem++
            else if (looksLikeMoneyMoved(m.body, m.address)) unread++
        }
        val gaps = ArrayList<Pair<String, Int>>()
        for ((acct, list) in points) {
            val sorted = list.sortedBy { it.first }
            var n = 0
            for (k in 1 until sorted.size) if (kotlin.math.abs(sorted[k - 1].second + sorted[k].third - sorted[k].second) > 1.0) n++
            if (n > 0) gaps.add(acct to n)
        }
        return Check(bank, txns, bal, rem, unread, gaps.sortedByDescending { it.second })
    }
}
