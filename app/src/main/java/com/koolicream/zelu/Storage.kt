package com.koolicream.zelu

import android.content.Context
import org.json.JSONObject
import java.io.File

object Storage {

    private val lock = Any()
    private const val PREFS = "zelu_prefs"
    private const val KEY_SENDERS = "bank_senders"
    private const val KEY_OWN = "own_accounts"
    private const val KEY_DATA_VERSION = "data_version"
    private const val TXN_FILE = "transactions.json"
    private const val OVERRIDE_FILE = "overrides.json"   // per-transaction decisions
    private const val RULE_FILE = "payee_rules.json"     // learned payee -> kind/category
    private const val REMINDER_FILE = "reminders.json"   // parsed bill/payment reminders
    private const val REM_DONE_FILE = "reminder_done.json" // reminder ids ticked as paid
    private const val REPAID_FILE = "lent_repaid.json"   // person -> amount repaid against money you lent
    private const val ALIAS_FILE = "payee_alias.json"   // payee name -> the name you renamed / merged it into
    private const val ACCOUNTS_FILE = "accounts.json"   // account / loan -> accepted, declined or joined into another ("duplicate"), and its type
    private const val APART_FILE = "apart.json"         // accounts you separated: Zelu does not join them by itself again
    private const val TITLE_FILE = "title_overrides.json" // transaction id -> the title you gave it
    private const val BUDGET_FILE = "category_budgets.json" // category -> monthly budget cap

    // Bump when parser/classifier changes so history rebuilds on next open.
    const val PARSER_VERSION = 71

    data class Settings(val bankSenders: String, val ownAccounts: String)

    fun dataVersion(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_DATA_VERSION, 0)

    fun setDataVersion(context: Context, v: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DATA_VERSION, v).apply()
    }

    fun loadSettings(context: Context): Settings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Settings(p.getString(KEY_SENDERS, "") ?: "", p.getString(KEY_OWN, "") ?: "")
    }

    fun saveSettings(context: Context, bankSenders: String, ownAccounts: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_SENDERS, bankSenders).putString(KEY_OWN, ownAccounts).apply()
    }

    // Load transactions with learned payee rules and per-transaction overrides applied.
    // Working out every payment's final category from years of messages is the slow part, so the result is kept
    // until something it depends on changes (any saved file, or the own-accounts setting).
    private var cacheKey: String? = null
    private var cacheRows: List<Row> = emptyList()
    private var cacheRefunds: List<Pair<Row, Row>> = emptyList()

    private fun dataKey(context: Context): String {
        val sb = StringBuilder().append(SafeFileStats.writes).append('|')
        context.filesDir.listFiles()?.filter { it.isFile && (it.name.endsWith(".json") || it.name.endsWith(".txt")) }
            ?.sortedBy { it.name }?.forEach { sb.append(it.name).append(':').append(it.length()).append(':').append(it.lastModified()).append(';') }
        sb.append(loadSettings(context).ownAccounts)
        return sb.toString()
    }

    fun loadRows(context: Context): List<Row> = synchronized(lock) {
        val key = dataKey(context)
        if (key == cacheKey) { Refunds.suggestions = cacheRefunds; return@synchronized cacheRows }
        val rows = computeRows(context)
        cacheRows = rows; cacheRefunds = Refunds.suggestions; cacheKey = key
        rows
    }

    private fun computeRows(context: Context): List<Row> {
        val base = readMap(context) + manualTxns(context)
        val overrides = readKv(context, OVERRIDE_FILE)
        val rules = readKv(context, RULE_FILE)
        val aliases = readKv(context, ALIAS_FILE)
        fun resolve(n: String?): String? {
            var cur = n ?: return null
            var steps = 0
            while (steps < 6) {
                val next = aliases[cur.lowercase().trim()]?.first?.takeIf { it.isNotBlank() } ?: break
                if (next.equals(cur, ignoreCase = true)) break
                cur = next; steps++
            }
            return cur
        }
        val reg = readKv(context, ACCOUNTS_FILE)
        // An account you joined into another on the Your accounts screen: its payments move to that account (or, for a
        // loan, to the bank's main account, since the money left from there). Applied once the labels are final, below.
        val dups = reg.filter { it.value.first == "duplicate" }.mapValues { it.value.second }
        val apart = readKv(context, APART_FILE).keys
        val titles = readKv(context, TITLE_FILE)
        // Accounts you said are a different type than Zelu guessed (say, a savings account it read as a card).
        val retype = reg.filter { it.value.first == "accepted" }
            .mapNotNull { (k, v) -> val n = relabel(k, v.second); if (n != k) k to n else null }.toMap()
        val catOff = readKv(context, CATOFF_FILE).keys
        val splits = loadSplits(context)
        val own = splitList(loadSettings(context).ownAccounts)
        // The same account written with different digit counts is joined here, unless you decided otherwise about that
        // label: you separated it, removed it, joined it yourself, or said it is not a savings account.
        val keep = apart + reg.filter { it.value.first != "accepted" || (it.value.second.isNotBlank() && it.value.second != "savings") }.keys
        val auto = Analytics.autoMerge(base.values.groupingBy { it.account }.eachCount()).filterKeys { it !in keep }
        autoMap = auto
        val rawStats = if (auto.isEmpty()) emptyMap() else AccountBook.stats(base.values.filter { it.account in auto })
        autoPartCache = auto.mapNotNull { (old, main) -> rawStats[old]?.let { old to (main to it) } }.toMap()
        val rows = base.values.map { r ->
            var row = auto[r.account]?.let { r.copy(account = it) } ?: r
            // A title you typed replaces the one read from the SMS, and the transaction is classified again under it.
            titles[r.id]?.first?.takeIf { it.isNotBlank() }?.let { t ->
                val c = Classifier.classify(Txn(r.id, r.date, r.amount, r.direction, t, r.ref, r.raw, r.source, r.account), own)
                row = row.copy(counterparty = t, kind = c.kind, category = c.category)
            }
            if (titles[r.id] == null && aliases.isNotEmpty()) { val n = resolve(r.counterparty); if (n != r.counterparty) row = row.copy(counterparty = n) }
            val payeeKey = (row.counterparty ?: "").lowercase().trim()
            if (payeeKey.isNotEmpty() && rules.containsKey(payeeKey)) {
                val (k, c) = rules.getValue(payeeKey)
                row = row.copy(kind = k, category = c)
            }
            overrides[r.id]?.let { (k, c) -> row = row.copy(kind = k, category = c) }
            Classifier.ALIAS[row.category]?.let { row = row.copy(category = it) }
            if (row.kind == "expense" && row.category in catOff) row = row.copy(kind = "ignore")
            row
        }.newestFirst()
        // A payment you split becomes several rows that add up to it, one per category.
        val expanded = if (splits.isEmpty()) rows else rows.flatMap { r ->
            val parts = splits[r.id]
            if (parts == null || r.kind != "expense" || Math.abs(parts.sumOf { it.first } - r.amount) > 0.5) listOf(r)
            else parts.mapIndexed { i, pc ->
                val pid = r.id + "#" + (i + 1)
                var part = r.copy(id = pid, amount = pc.first, category = pc.second)
                overrides[pid]?.let { (k, c) -> part = part.copy(kind = k, category = c) }
                part
            }
        }
        // A card/savings change also changes how its credits read (a card payment is a transfer; salary is income).
        fun reapply(r0: Row): Row {
            if (r0.id.contains('#') || overrides.containsKey(r0.id)) return r0
            val c = Classifier.classify(Txn(r0.id, r0.date, r0.amount, r0.direction, r0.counterparty, r0.ref, r0.raw, r0.source, r0.account), own)
            var row = r0.copy(kind = c.kind, category = c.category)
            val pk = (row.counterparty ?: "").lowercase().trim()
            if (pk.isNotEmpty() && rules.containsKey(pk)) { val (k, cat) = rules.getValue(pk); row = row.copy(kind = k, category = cat) }
            Classifier.ALIAS[row.category]?.let { row = row.copy(category = it) }
            if (row.kind == "expense" && row.category in catOff) row = row.copy(kind = "ignore")
            return row
        }
        val canon = Reconcile.canonicalizeAccounts(expanded, apart)
        val retyped = if (retype.isEmpty()) canon else canon.map { r ->
            val n = retype[r.account]
            if (n == null) r else reapply(r.copy(account = n))
        }
        // Every label before anything you joined is applied, for the Your accounts screen.
        labelStatsCache = AccountBook.stats(retyped)
        val joined = if (dups.isEmpty()) retyped else Reconcile.joinAccounts(retyped, dups)
        cardNetAuto = AccountBook.networks(joined)
        val declined = reg.filter { it.value.first == "declined" }.keys
        val userSet = overrides.keys
        val paired = Reconcile.pairRefunds(
            Reconcile.detectSelfTransfers(Reconcile.dedupBillers(Reconcile.inferEmiSeries(joined, userSet)), userSet),
            loadRefundLinks(context))
        Refunds.suggestions = paired.suggestions
        val processed = paired.rows
        loanStatsCache = AccountBook.loanStats(processed) + loansFromNotices(context, processed)
        return (if (declined.isEmpty()) processed else processed.filter { it.account !in declined && (it.counterparty ?: "") !in declined }).newestFirst()
    }

    // The label an account takes once you say what it is: a savings account never carries "CC", a card always does.
    fun relabel(key: String, type: String): String = AccountBook.relabel(key, type)

    // ---- accounts you confirmed (or turned down) ----  key -> (status, type)
    // The network (Visa, Mastercard, RuPay...) of each credit card: read from its messages, and what you picked wins.
    @Volatile private var cardNetAuto: Map<String, String> = emptyMap()
    fun cardNetworks(context: Context): Map<String, String> = synchronized(lock) { cardNetAuto + readKv(context, "card_networks.json").mapValues { it.value.first } }
    fun setCardNetwork(context: Context, account: String, network: String?) = synchronized(lock) {
        val kv = readKv(context, "card_networks.json").toMutableMap()
        if (network == null) kv.remove(account) else kv[account] = network to ""
        writeKv(context, "card_networks.json", kv)
    }

    fun accountRegistry(context: Context): Map<String, Pair<String, String>> = synchronized(lock) { readKv(context, ACCOUNTS_FILE) }

    fun declinedKeys(context: Context): Set<String> =
        accountRegistry(context).filter { it.value.first == "declined" }.keys

    // Declined and duplicate accounts both disappear from balances and limits.
    private fun hiddenKeys(context: Context): Set<String> =
        accountRegistry(context).filter { it.value.first != "accepted" }.keys

    fun setAccountStatuses(context: Context, decisions: Map<String, Pair<String, String>>): Unit = synchronized(lock) {
        val kv = readKv(context, ACCOUNTS_FILE).toMutableMap()
        kv.putAll(decisions)
        writeKv(context, ACCOUNTS_FILE, kv)
    }

    fun accountsConfirmed(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("accounts_confirmed", false)

    fun setAccountsConfirmed(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("accounts_confirmed", true).apply()
    }

    // Adds payments not stored yet; with replace (a rebuild) the stored list becomes exactly these. Returns how many
    // were not stored before. A transaction file that can't be read is never overwritten by an ordinary save.
    fun saveRows(context: Context, newRows: List<Row>, replace: Boolean = false): Int = synchronized(lock) {
        val old = readMapOrNull(context)
        if (old == null && !replace) return@synchronized 0
        val map = if (replace || old == null) LinkedHashMap<String, Row>() else old.toMutableMap()
        var added = 0
        for (r in newRows) if (!map.containsKey(r.id)) { map[r.id] = r; if (old?.containsKey(r.id) != true) added++ }
        writeMap(context, map)
        added
    }

    /** The payments as stored, before any of your decisions are applied (what a rebuild reads again). */
    fun storedRows(context: Context): Collection<Row> = synchronized(lock) { readMap(context).values }

    /** Reminders read from messages, as stored (not the ones you added yourself). */
    fun storedReminders(context: Context): Collection<Reminder> = synchronized(lock) { readReminderMap(context).values }

    // Payment IDs from a reference number used to be the same for money in and money out ("ref-629112345678"), so a
    // reversal replaced the payment it undid. Money in now has its own ID ("...-cr"). Your decisions about a payment
    // stored under the old ID move to the new one. Safe to run again: only stored credits with the old ID are renamed.
    fun migrateCreditIds(context: Context, rows: Collection<Row>): Unit = synchronized(lock) {
        val renames = Rebuild.creditRenames(rows)
        if (renames.isEmpty()) return@synchronized
        fun <V> moved(m: Map<String, V>): Map<String, V> = m.mapKeys { Rebuild.renameKey(it.key, renames) }
        for (name in listOf(OVERRIDE_FILE, TITLE_FILE, "dup_ok.json")) {
            val kv = readKv(context, name)
            if (kv.isNotEmpty()) writeKv(context, name, moved(kv))
        }
        val splits = loadSplits(context)
        if (splits.isNotEmpty()) writeSplits(context, moved(splits))
        val links = loadRefundLinks(context)
        if (links.isNotEmpty()) writeRefundLinks(context, moved(links))
    }

    // ---- senders you said are your bank ----
    private const val SENDER_FILE = "trusted_senders.json"
    fun trustedSenders(context: Context): Set<String> = synchronized(lock) { readKv(context, SENDER_FILE).keys }
    fun loadTrust(context: Context) = synchronized(lock) {
        applyTrust(readKv(context, SENDER_FILE))
        Parser.taughtBanks = readKv(context, SENDER_BANK_FILE).mapValues { it.value.first }
    }
    private const val SENDER_BANK_FILE = "sender_banks.json"   // sender id -> the bank you told Zelu it belongs to
    private fun applyTrust(kv: Map<String, Pair<String, String>>) {
        Parser.trusted = kv.keys.toSet()
        Parser.walletSenders = kv.filter { it.value.first == "wallet" }.keys.toSet()
        Parser.otherSenders = kv.filter { it.value.first == "other" }.keys.toSet()
    }
    // type: "bank" (read as an account), "wallet" (spends not counted again) or "other" (read, with no account).
    fun trustSender(context: Context, address: String, type: String = "bank"): Unit = synchronized(lock) {
        val kv = readKv(context, SENDER_FILE).toMutableMap()
        kv[address.uppercase()] = type to ""
        writeKv(context, SENDER_FILE, kv)
        applyTrust(kv)
    }

    // ---- categories you don't want counted as spending ----
    // Loans you have paid off: name -> the day you marked it closed. They stay on Home, frozen.
    private const val CLOSEDLOAN_FILE = "closed_loans.json"
    fun closedLoans(context: Context): Map<String, String> = synchronized(lock) { readKv(context, CLOSEDLOAN_FILE).mapValues { it.value.first } }
    fun setLoanClosed(context: Context, name: String, closed: Boolean) = synchronized(lock) {
        val kv = readKv(context, CLOSEDLOAN_FILE).toMutableMap()
        if (closed) kv[name] = Dates.today() to "" else kv.remove(name)
        writeKv(context, CLOSEDLOAN_FILE, kv)
    }
    private const val CATOFF_FILE = "category_off.json"
    fun categoriesOff(context: Context): Set<String> = synchronized(lock) { readKv(context, CATOFF_FILE).keys }
    fun setCategoryCounts(context: Context, category: String, counts: Boolean): Unit = synchronized(lock) {
        val kv = readKv(context, CATOFF_FILE).toMutableMap()
        if (counts) kv.remove(category) else kv[category] = "off" to ""
        writeKv(context, CATOFF_FILE, kv)
    }

    // ---- one payment split across categories ----  id -> [(amount, category)]
    private const val SPLIT_FILE = "splits.json"
    fun loadSplits(context: Context): Map<String, List<Pair<Double, String>>> = synchronized(lock) {
        val f = file(context, SPLIT_FILE)
        if (!f.exists()) emptyMap() else try {
            val obj = f.readWithBackup { JSONObject(it) }; val out = HashMap<String, List<Pair<Double, String>>>()
            for (k in obj.keys()) {
                val arr = obj.getJSONArray(k)
                out[k] = (0 until arr.length()).map { i -> arr.getJSONObject(i).let { it.getDouble("a") to it.getString("c") } }
            }
            out
        } catch (_: Exception) { emptyMap() }
    }
    fun setSplit(context: Context, id: String, parts: List<Pair<Double, String>>?): Unit = synchronized(lock) {
        val cur = loadSplits(context).toMutableMap()
        if (parts == null) cur.remove(id) else cur[id] = parts
        writeSplits(context, cur)
    }

    private fun writeSplits(context: Context, splits: Map<String, List<Pair<Double, String>>>) {
        val obj = JSONObject()
        for ((k, v) in splits) obj.put(k, org.json.JSONArray().also { arr -> v.forEach { arr.put(JSONObject().put("a", it.first).put("c", it.second)) } })
        file(context, SPLIT_FILE).writeKeepingBackup(obj.toString())
    }

    // Pairs of payments you said are really two payments ("Keep both"), so they are not flagged again.
    fun dupOk(context: Context): Set<String> = synchronized(lock) { readKv(context, "dup_ok.json").keys }
    fun markDupOk(context: Context, key: String): Unit = synchronized(lock) {
        val kv = readKv(context, "dup_ok.json").toMutableMap()
        kv[key] = "1" to ""
        writeKv(context, "dup_ok.json", kv)
    }

    // "Do it later" on the refund question hides that whole card for a day, then it comes back.
    fun refundSnoozed(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("refund_later_until", 0L) > System.currentTimeMillis()
    fun snoozeRefund(context: Context, hours: Int = 24) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("refund_later_until", System.currentTimeMillis() + hours * 3_600_000L).apply()
    }

    // How many missed messages you have already seen, so the menu can show a dot when new ones appear.
    fun missedSeen(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("missed_seen", 0)
    fun setMissedSeen(context: Context, n: Int) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("missed_seen", n).apply() }

    // When messages were last read, and the date of the newest bank message found.
    fun setReadStamp(context: Context, readAt: Long, newestBank: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("last_read", readAt).putLong("newest_bank", newestBank).apply()
    }
    // A bank message arrived (even with the app closed): remember its time, for the "last SMS" line on Home.
    fun noteBankSms(context: Context, ms: Long) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (ms > sp.getLong("newest_bank", 0L)) sp.edit().putLong("newest_bank", ms).apply()
    }
    fun lastRead(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("last_read", 0L)
    fun newestBank(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("newest_bank", 0L)

    // Deletes everything Zelu keeps on this phone (files and saved settings). Theme and app lock are left alone.
    fun eraseAll(context: Context): Unit = synchronized(lock) {
        context.filesDir.listFiles()?.forEach { if (it.isFile) it.delete() }
        for (n in listOf(PREFS, "zelu_backup", "zelu_recap")) context.getSharedPreferences(n, Context.MODE_PRIVATE).edit().clear().apply()
    }

    // Where "Report a problem" emails go; change it from Menu > Help.
    const val DEFAULT_REPORT_EMAIL = "koolicream@gmail.com"
    fun reportEmail(context: Context): String {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Earlier versions saved the report address on the phone; forget it once so everyone uses the current one.
        if (!p.getBoolean("report_email_reset_v2", false)) p.edit().remove("report_email").putBoolean("report_email_reset_v2", true).apply()
        return p.getString("report_email", null)?.takeIf { it.isNotBlank() } ?: DEFAULT_REPORT_EMAIL
    }
    fun setReportEmail(context: Context, email: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("report_email", email.trim()).apply()
    }

    // Accounts you said "not mine" to come back and are asked about again.
    fun forgetDeclined(context: Context): Int = synchronized(lock) {
        val kv = readKv(context, ACCOUNTS_FILE).toMutableMap()
        val n = kv.count { it.value.first == "declined" }
        kv.entries.removeAll { it.value.first == "declined" }
        writeKv(context, ACCOUNTS_FILE, kv)
        n
    }

    // ---- UPI AutoPay / mandates (Dues page) ----
    private const val MANDATE_FILE = "mandates.json"
    private const val MANUAL_TXN_FILE = "manual_txns.json"
    private const val HANDLED_FILE = "handled_sms.json"

    private fun mandateToJson(m: Mandate) = JSONObject().put("key", m.key).put("merchant", m.merchant)
        .put("amount", m.amount ?: JSONObject.NULL).put("account", m.account).put("status", m.status)
        .put("next", m.nextDate ?: JSONObject.NULL).put("since", m.since).put("last", m.lastMs)
        .put("raw", m.raw).put("source", m.source)

    private fun mandateFromJson(o: JSONObject) = Mandate(
        o.getString("key"), o.getString("merchant"), if (o.isNull("amount")) null else o.getDouble("amount"),
        o.optString("account"), o.getString("status"), if (o.isNull("next")) null else o.getString("next"),
        o.optLong("since"), o.optLong("last"), o.optString("raw"), o.optString("source"))

    fun loadMandates(context: Context): List<Mandate> = synchronized(lock) {
        val f = file(context, MANDATE_FILE)
        if (!f.exists()) emptyList() else try {
            val obj = f.readWithBackup { JSONObject(it) }; val out = ArrayList<Mandate>()
            for (k in obj.keys()) out.add(mandateFromJson(obj.getJSONObject(k)))
            out.sortedByDescending { it.lastMs }
        } catch (_: Exception) { emptyList() }
    }

    // The newest message about a mandate decides its status; older ones only fill in what is missing.
    fun saveMandates(context: Context, found: List<Mandate>, replace: Boolean = false): Unit = synchronized(lock) {
        val cur = if (replace) HashMap<String, Mandate>() else loadMandates(context).associateBy { it.key }.toMutableMap()
        for (m in found.sortedBy { it.lastMs }) {
            val e = cur[m.key]
            cur[m.key] = if (e == null) m else if (m.lastMs >= e.lastMs) m.copy(
                amount = m.amount ?: e.amount, account = m.account.ifBlank { e.account }, since = minOf(e.since, m.since),
                nextDate = if (m.status == "scheduled" || m.status == "active") (m.nextDate ?: e.nextDate) else null)
            else e.copy(amount = e.amount ?: m.amount, account = e.account.ifBlank { m.account }, since = minOf(e.since, m.since))
        }
        if (found.isNotEmpty() || replace) {
            val obj = JSONObject()
            for ((k, v) in cur) obj.put(k, mandateToJson(v))
            file(context, MANDATE_FILE).writeKeepingBackup(obj.toString())
        }
    }

    // ---- payments you added by hand from a message Zelu couldn't read ----
    fun addManualTxn(context: Context, row: Row, smsKey: String): Unit = synchronized(lock) {
        val f = file(context, MANUAL_TXN_FILE)
        val obj = try { if (f.exists()) f.readWithBackup { JSONObject(it) } else JSONObject() } catch (_: Exception) { JSONObject() }
        obj.put(row.id, rowToJson(row))
        f.writeKeepingBackup(obj.toString())
        markSmsHandled(context, smsKey)
    }

    private fun manualTxns(context: Context): Map<String, Row> {
        val f = file(context, MANUAL_TXN_FILE)
        if (!f.exists()) return emptyMap()
        return try {
            val obj = f.readWithBackup { JSONObject(it) }; val out = HashMap<String, Row>()
            for (k in obj.keys()) out[k] = rowFromJson(obj.getJSONObject(k))
            out
        } catch (_: Exception) { emptyMap() }
    }

    // A message you added as a payment, or told Zelu to stop listing.
    fun markSmsHandled(context: Context, key: String): Unit = synchronized(lock) {
        val kv = readKv(context, HANDLED_FILE).toMutableMap()
        kv[key] = "1" to ""
        writeKv(context, HANDLED_FILE, kv)
    }

    fun handledSms(context: Context): Set<String> = synchronized(lock) { readKv(context, HANDLED_FILE).keys }

    // ---- reminders (Dues page) ----
    fun saveReminders(context: Context, newOnes: List<Reminder>, replace: Boolean = false): Int = synchronized(lock) {
        val map = if (replace) HashMap<String, Reminder>() else readReminderMap(context).toMutableMap()
        var added = 0
        for (r in newOnes) if (!map.containsKey(r.id)) { map[r.id] = r; added++ }
        writeReminderMap(context, map)
        added
    }

    // A loan that only has payment notices (a "pay your dues" link) and no EMI debit yet is still a loan of yours.
    private fun loansFromNotices(context: Context, rows: List<Row>): Map<String, AccountBook.Stat> {
        val have = rows.mapNotNull { it.counterparty }.toSet()
        return readReminderMap(context).values
            .filter { r -> r.counterparty != null && r.counterparty !in have && Regex(""".*\bLoan \d{3,4}$""").matches(r.counterparty) }
            .groupBy { it.counterparty!! }
            .mapValues { e -> val l = e.value.maxByOrNull { it.dueDate }!!; AccountBook.Stat(0, l.dueDate, l.amount, l.raw) }
    }

    fun loadReminders(context: Context): List<Reminder> = synchronized(lock) {
        val state = loadReminderState(context)
        val sorted = (readReminderMap(context).values + manualReminders(context))
            .map { it.copy(done = state[it.id] ?: false) }
            .sortedBy { it.dueDate }
        // Collapse true duplicates: same payee + amount whose due dates are within 3 days
        // (the same bill arriving as two SMS). Keeps the earliest.
        val out = ArrayList<Reminder>()
        for (r in sorted) {
            val key = (r.counterparty ?: "").lowercase().trim()
            val dup = out.any { d ->
                (d.counterparty ?: "").lowercase().trim() == key &&
                    kotlin.math.abs(d.amount - r.amount) < 1.0 &&
                    (-3..3).any { Dates.shiftDays(d.dueDate, it) == r.dueDate }
            }
            if (!dup) out.add(r)
        }
        out
    }

    // ---- bills you add yourself (rent, school fee...) — kept apart so an SMS re-read never removes them ----
    private const val MANUAL_FILE = "manual_reminders.json"

    private fun readManual(context: Context): org.json.JSONArray = try {
        val f = file(context, MANUAL_FILE)
        if (f.exists()) f.readWithBackup { org.json.JSONArray(it) } else org.json.JSONArray()
    } catch (_: Exception) { org.json.JSONArray() }

    fun addManualReminder(context: Context, name: String, amount: Double, dueDate: String, recurring: Boolean) = synchronized(lock) {
        val arr = readManual(context)
        arr.put(JSONObject().put("id", System.currentTimeMillis().toString()).put("name", name).put("amount", amount)
            .put("due", dueDate).put("recurring", recurring))
        file(context, MANUAL_FILE).writeKeepingBackup(arr.toString())
    }

    // id is a reminder id ("man-<id>" or "man-<id>-<yyyy-MM>").
    fun removeManualReminder(context: Context, reminderId: String) = synchronized(lock) {
        val base = reminderId.removePrefix("man-").substringBefore("-")
        val arr = readManual(context); val keep = org.json.JSONArray()
        for (i in 0 until arr.length()) if (arr.getJSONObject(i).getString("id") != base) keep.put(arr.getJSONObject(i))
        file(context, MANUAL_FILE).writeKeepingBackup(keep.toString())
    }

    private fun manualReminders(context: Context): List<Reminder> {
        val arr = readManual(context)
        val out = ArrayList<Reminder>()
        val thisMonth = Dates.todayMonth()
        val lastMonth = Dates.shiftMonth(thisMonth, 1)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.getString("id"); val due = o.getString("due"); val name = o.getString("name")
            val amt = o.getDouble("amount"); val rec = o.optBoolean("recurring", false)
            fun make(rid: String, d: String) = Reminder(rid, d, amt, null, name, "", rec, "Added by you", "manual")
            if (!rec) { out.add(make("man-$id", due)); continue }
            val day = due.substring(8, 10).toIntOrNull() ?: 1
            var ym = due.take(7)
            var guard = 0
            while (ym <= lastMonth && guard++ < 60) {
                if (ym >= Dates.shiftMonth(thisMonth, -3)) {
                    val d = minOf(day, Dates.daysInMonth(ym))
                    out.add(make("man-$id-$ym", "$ym-" + d.toString().padStart(2, '0')))
                }
                ym = Dates.shiftMonth(ym, 1)
            }
        }
        return out
    }

    // ---- lending repayments ----  person key (lowercased counterparty) -> amount repaid
    fun loadRepaid(context: Context): Map<String, Double> = synchronized(lock) {
        val f = file(context, REPAID_FILE)
        if (!f.exists()) return emptyMap()
        try {
            val o = f.readWithBackup { JSONObject(it) }
            val m = HashMap<String, Double>()
            val ks = o.keys()
            while (ks.hasNext()) { val k = ks.next(); m[k] = o.getDouble(k) }
            m
        } catch (_: Exception) { emptyMap() }
    }

    fun setRepaid(context: Context, personKey: String, amount: Double) = synchronized(lock) {
        val cur = loadRepaid(context).toMutableMap()
        if (amount <= 0.0) cur.remove(personKey) else cur[personKey] = amount
        val o = JSONObject()
        for ((k, v) in cur) o.put(k, v)
        file(context, REPAID_FILE).writeKeepingBackup(o.toString())
    }

    // ---- per-category budgets ----
    fun loadBudgets(context: Context): Map<String, Double> = synchronized(lock) {
        val f = file(context, BUDGET_FILE)
        if (!f.exists()) return emptyMap()
        try {
            val o = f.readWithBackup { JSONObject(it) }
            val m = HashMap<String, Double>()
            val ks = o.keys()
            while (ks.hasNext()) { val k = ks.next(); m[k] = o.getDouble(k) }
            m
        } catch (_: Exception) { emptyMap() }
    }

    fun setBudget(context: Context, category: String, amount: Double) = synchronized(lock) {
        val cur = loadBudgets(context).toMutableMap()
        if (amount <= 0.0) cur.remove(category) else cur[category] = amount
        val o = JSONObject()
        for ((k, v) in cur) o.put(k, v)
        file(context, BUDGET_FILE).writeKeepingBackup(o.toString())
    }

    // ---- latest known bank balance per account (account -> amount, receivedMs) ----
    // Source "b" = a balance-only alert (authoritative); "t" = tail of a debit/credit alert,
    // used only for accounts that never send a balance-only alert.
    private fun readBalFile(context: Context): MutableMap<String, Triple<Double, Long, String>> {
        val f = file(context, "balances.json")
        val m = LinkedHashMap<String, Triple<Double, Long, String>>()
        if (!f.exists()) return m
        try {
            val o = f.readWithBackup { JSONObject(it) }
            val ks = o.keys()
            while (ks.hasNext()) {
                val k = ks.next(); val e = o.getJSONObject(k)
                m[k] = Triple(e.getDouble("a"), e.getLong("d"), e.optString("s", "b"))
            }
        } catch (_: Exception) {}
        return m
    }

    // Old account label -> the account it was joined to (see Analytics.autoMerge). Set whenever the rows are computed.
    @Volatile private var autoMap: Map<String, String> = emptyMap()
    // Set whenever the rows are computed, for the Your accounts screen: Zelu's own joins (old label -> main, its payments),
    // payments per label before your joins, and EMIs per loan.
    @Volatile private var autoPartCache: Map<String, Pair<String, AccountBook.Stat>> = emptyMap()
    @Volatile private var labelStatsCache: Map<String, AccountBook.Stat> = emptyMap()
    @Volatile private var loanStatsCache: Map<String, AccountBook.Stat> = emptyMap()

    /** Every account for the Your accounts screen, with what is joined into each. */
    fun accountBook(context: Context): List<AccountBook.Acct> {
        loadRows(context)                                   // makes sure the caches below are current
        return AccountBook.build(labelStatsCache, autoPartCache, loanStatsCache, accountRegistry(context),
            loadBalances(context), loadLimits(context))
    }

    /** Save on an account's sheet (see [AccountBook.plan]). */
    fun applyAccountPlan(context: Context, plan: AccountBook.Plan): Unit = synchronized(lock) {
        val kv = readKv(context, ACCOUNTS_FILE).toMutableMap()
        kv.putAll(plan.registry)
        writeKv(context, ACCOUNTS_FILE, kv)
        val apart = readKv(context, APART_FILE).toMutableMap()
        plan.apartAdd.forEach { apart[it] = "1" to "" }
        plan.apartRemove.forEach { apart.remove(it) }
        writeKv(context, APART_FILE, apart)
        learnSenderBanks(context, plan.registry)
    }

    // A number with no bank name that you join to an account of a bank: its sender belongs to that bank, so the next
    // messages from it are read under that bank without asking.
    private fun learnSenderBanks(context: Context, reg: Map<String, Pair<String, String>>) {
        val joins = reg.filter { it.value.first == "duplicate" && AccountBook.bankOf(it.key) == null && AccountBook.bankOf(it.value.second) != null }
        if (joins.isEmpty()) return
        val rows = readMap(context).values
        val learned = readKv(context, SENDER_BANK_FILE).toMutableMap()
        for ((label, v) in joins) {
            val bank = AccountBook.bankOf(v.second)!!.uppercase()
            val cores = rows.filter { it.account == label && it.source.isNotBlank() }.map { Parser.senderCore(it.source) }.filter { it.length >= 3 }.toSet()
            for (c in cores) learned[c] = bank to ""
        }
        writeKv(context, SENDER_BANK_FILE, learned)
        Parser.taughtBanks = learned.mapValues { it.value.first }
    }

    /** Continue on the screen: accounts shown and not removed are yours. */
    fun confirmShownAccounts(context: Context): Unit = synchronized(lock) {
        val plan = AccountBook.confirmPlan(accountBook(context), accountRegistry(context))
        if (plan.isNotEmpty()) setAccountStatuses(context, plan)
    }

    /** "Not my account": removes the account and its payments from Zelu (the screen asks first). */
    fun declineAccount(context: Context, key: String, type: String): Unit = synchronized(lock) {
        val kv = readKv(context, ACCOUNTS_FILE).toMutableMap()
        kv[key] = "declined" to type
        // What was joined into it comes back as its own accounts.
        for ((k, v) in kv.toMap()) if (v.first == "duplicate" && v.second == key) kv[k] = "accepted" to AccountBook.guessType(k)
        writeKv(context, ACCOUNTS_FILE, kv)
    }

    /** "This is my account" on a removed account. */
    fun restoreAccount(context: Context, key: String): Unit = synchronized(lock) {
        val kv = readKv(context, ACCOUNTS_FILE).toMutableMap()
        kv[key] = "accepted" to (kv[key]?.second?.takeIf { it.isNotBlank() } ?: AccountBook.guessType(key))
        writeKv(context, ACCOUNTS_FILE, kv)
    }

    fun loadBalances(context: Context): Map<String, Pair<Double, Long>> {
        loadRows(context)                                   // makes sure autoMap is current
        val auto = autoMap
        val raw = loadBalancesRaw(context)
        val m = if (auto.isEmpty()) raw else {
            val joined = LinkedHashMap<String, Pair<Double, Long>>()
            for ((k, v) in raw) {
                val t = auto[k] ?: k
                val cur = joined[t]
                if (cur == null || v.second >= cur.second) joined[t] = v
            }
            joined
        }
        val joined = accountRegistry(context).filter { it.value.first == "duplicate" && !it.value.second.contains(" Loan ") }
        val withJoined = if (joined.isEmpty()) m else {
            val out = LinkedHashMap(m)
            for ((k, v) in joined) {
                val b = m[k] ?: continue
                var t = v.second; var hops = 0
                while (hops++ < 8) { val n = joined[t]?.second ?: break; if (n == t) break; t = n }
                val cur = out[t]
                if (cur == null || b.second > cur.second) out[t] = b
                if (t != k) out.remove(k)   // a joined label is no longer an account of its own
            }
            out
        }
        val d = hiddenKeys(context)
        return if (d.isEmpty()) withJoined else withJoined.filterKeys { it !in d }
    }

    private fun loadBalancesRaw(context: Context): Map<String, Pair<Double, Long>> = synchronized(lock) {
        readBalFile(context).mapValues { it.value.first to it.value.second }
    }

    // alerts: from balance-only messages; tails: from the end of payment messages. With replace (a rebuild) only what
    // these messages say is kept, so a balance that no message produces any more does not linger.
    fun saveBalances(context: Context, alerts: Map<String, Pair<Double, Long>>, tails: Map<String, Pair<Double, Long>>,
                     replace: Boolean = false): Unit = synchronized(lock) {
        if (alerts.isEmpty() && tails.isEmpty() && !replace) return@synchronized
        val cur = if (replace) LinkedHashMap<String, Triple<Double, Long, String>>() else readBalFile(context)
        for ((fresh, source) in listOf(alerts to "b", tails to "t")) for ((k, v) in fresh) {
            val c = cur[k]
            // The newest figure wins, whichever kind of SMS it came from.
            val take = c == null || v.second >= c.second
            if (take) cur[k] = Triple(v.first, v.second, source)
        }
        val o = JSONObject()
        for ((k, v) in cur) o.put(k, JSONObject().put("a", v.first).put("d", v.second).put("s", v.third))
        file(context, "balances.json").writeKeepingBackup(o.toString())
    }

    // ---- latest available credit limit per card account ----
    fun loadLimits(context: Context): Map<String, Pair<Double, Long>> = loadLimitsRaw(context).let { m ->
        val d = hiddenKeys(context); if (d.isEmpty()) m else m.filterKeys { k -> k.substringBefore("#total") !in d }
    }

    private fun loadLimitsRaw(context: Context): Map<String, Pair<Double, Long>> = synchronized(lock) {
        val f = file(context, "limits.json")
        if (!f.exists()) return emptyMap()
        try {
            val o = f.readWithBackup { JSONObject(it) }
            val m = LinkedHashMap<String, Pair<Double, Long>>()
            val ks = o.keys()
            while (ks.hasNext()) { val k = ks.next(); val e = o.getJSONObject(k); m[k] = e.getDouble("a") to e.getLong("d") }
            m
        } catch (_: Exception) { emptyMap() }
    }

    // A limit you typed in because no SMS states it. A later SMS that does state it replaces it.
    fun manualLimitKeys(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet("manual_limits", emptySet()) ?: emptySet()

    fun setManualLimit(context: Context, account: String, amount: Double): Unit = synchronized(lock) {
        val cur = loadLimits(context).toMutableMap()
        cur[account] = amount to System.currentTimeMillis()
        val o = JSONObject()
        for ((k, v) in cur) o.put(k, JSONObject().put("a", v.first).put("d", v.second))
        file(context, "limits.json").writeKeepingBackup(o.toString())
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet("manual_limits", manualLimitKeys(context) + account).apply()
    }

    fun saveLimits(context: Context, fresh: Map<String, Pair<Double, Long>>): Unit = synchronized(lock) {
        if (fresh.isEmpty()) return@synchronized
        val cur = loadLimits(context).toMutableMap()
        val man = manualLimitKeys(context).toMutableSet()
        var manChanged = false
        for ((k, v) in fresh) { val c = cur[k]; if (c == null || v.second >= c.second) { cur[k] = v; if (man.remove(k)) manChanged = true } }
        if (manChanged) context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet("manual_limits", man).apply()
        val o = JSONObject()
        for ((k, v) in cur) o.put(k, JSONObject().put("a", v.first).put("d", v.second))
        file(context, "limits.json").writeKeepingBackup(o.toString())
    }

    // ---- refund links: refund id -> true (same spend, net it) / false (not related) ----
    fun loadRefundLinks(context: Context): Map<String, Boolean> = synchronized(lock) {
        val f = file(context, "refund_links.json")
        if (!f.exists()) return emptyMap()
        try {
            val o = f.readWithBackup { JSONObject(it) }; val m = HashMap<String, Boolean>()
            val ks = o.keys(); while (ks.hasNext()) { val k = ks.next(); m[k] = o.getBoolean(k) }
            m
        } catch (_: Exception) { emptyMap() }
    }

    fun setRefundLink(context: Context, refundId: String, yes: Boolean): Unit = synchronized(lock) {
        val cur = loadRefundLinks(context).toMutableMap(); cur[refundId] = yes
        writeRefundLinks(context, cur)
    }

    private fun writeRefundLinks(context: Context, links: Map<String, Boolean>) {
        val o = JSONObject(); for ((k, v) in links) o.put(k, v)
        file(context, "refund_links.json").writeKeepingBackup(o.toString())
    }

    // ---- savings plan inputs (SIP, EMI, goal...) ----
    fun loadPlan(context: Context): Map<String, String> = synchronized(lock) {
        val f = file(context, "plan.json")
        if (!f.exists()) return emptyMap()
        try {
            val o = f.readWithBackup { JSONObject(it) }; val m = HashMap<String, String>()
            val ks = o.keys(); while (ks.hasNext()) { val k = ks.next(); m[k] = o.getString(k) }
            m
        } catch (_: Exception) { emptyMap() }
    }

    fun savePlan(context: Context, m: Map<String, String>): Unit = synchronized(lock) {
        val o = JSONObject(); for ((k, v) in m) o.put(k, v)
        file(context, "plan.json").writeKeepingBackup(o.toString())
    }

    // ---- per-card preferences: billing cycle day + monthly budget ----
    fun loadCardPrefs(context: Context): Map<String, Pair<Int, Double>> = synchronized(lock) {
        val f = file(context, "card_prefs.json")
        if (!f.exists()) return emptyMap()
        try {
            val o = f.readWithBackup { JSONObject(it) }
            val m = HashMap<String, Pair<Int, Double>>()
            val ks = o.keys()
            while (ks.hasNext()) { val k = ks.next(); val e = o.getJSONObject(k); m[k] = e.optInt("d", 1) to e.optDouble("b", 0.0) }
            m
        } catch (_: Exception) { emptyMap() }
    }

    fun setCardPref(context: Context, account: String, day: Int, budget: Double): Unit = synchronized(lock) {
        val cur = loadCardPrefs(context).toMutableMap()
        cur[account] = day to budget
        val o = JSONObject()
        for ((k, v) in cur) o.put(k, JSONObject().put("d", v.first).put("b", v.second))
        file(context, "card_prefs.json").writeKeepingBackup(o.toString())
    }

    // ---- overall monthly spending budget ----
    fun loadMonthlyBudget(context: Context): Double = synchronized(lock) {
        val f = file(context, "monthly_budget.txt")
        if (!f.exists()) 0.0 else f.readText().trim().toDoubleOrNull()?.takeIf { it >= 0 } ?: 0.0
    }

    fun saveMonthlyBudget(context: Context, amount: Double): Unit = synchronized(lock) {
        file(context, "monthly_budget.txt").writeKeepingBackup(amount.toString())
    }

    // ---- user-made categories (name -> emoji + colour) ----
    fun loadCustomCats(context: Context): Map<String, Pair<String, Long>> = synchronized(lock) {
        val f = file(context, "custom_categories.json")
        if (!f.exists()) return emptyMap()
        try {
            val o = f.readWithBackup { JSONObject(it) }
            val m = LinkedHashMap<String, Pair<String, Long>>()
            val ks = o.keys()
            while (ks.hasNext()) { val k = ks.next(); val e = o.getJSONObject(k); m[k] = e.getString("e") to e.getLong("c") }
            m
        } catch (_: Exception) { emptyMap() }
    }

    fun addCustomCat(context: Context, name: String, emoji: String, color: Long): Unit = synchronized(lock) {
        val cur = loadCustomCats(context).toMutableMap()
        cur[name] = emoji to color
        val o = JSONObject()
        for ((k, v) in cur) o.put(k, JSONObject().put("e", v.first).put("c", v.second))
        file(context, "custom_categories.json").writeKeepingBackup(o.toString())
    }

    // ---- fixed monthly outflow plan (name -> typical monthly amount) ----
    fun loadFixed(context: Context): Map<String, Double> = synchronized(lock) {
        val f = file(context, "fixed_outflow.json")
        if (!f.exists()) return emptyMap()
        try {
            val o = f.readWithBackup { JSONObject(it) }
            val m = LinkedHashMap<String, Double>()
            val ks = o.keys()
            while (ks.hasNext()) { val k = ks.next(); m[k] = o.getDouble(k) }
            m
        } catch (_: Exception) { emptyMap() }
    }

    fun setFixed(context: Context, name: String, amount: Double) = synchronized(lock) {
        val cur = loadFixed(context).toMutableMap()
        if (amount <= 0.0) cur.remove(name) else cur[name] = amount
        val o = JSONObject()
        for ((k, v) in cur) o.put(k, v)
        file(context, "fixed_outflow.json").writeKeepingBackup(o.toString())
    }

    // Explicit paid(true)/unpaid(false) override per reminder. An explicit entry always
    // wins over the auto rules (past-month / matched-debit), so unmark works from any state.
    fun setReminderDone(context: Context, id: String, done: Boolean) = synchronized(lock) {
        val m = loadReminderState(context).toMutableMap()
        m[id] = done
        val o = JSONObject()
        for ((k, v) in m) o.put(k, v)
        file(context, REM_DONE_FILE).writeKeepingBackup(o.toString())
    }

    fun loadReminderState(context: Context): Map<String, Boolean> = synchronized(lock) {
        val f = file(context, REM_DONE_FILE)
        if (!f.exists()) return emptyMap()
        return try {
            f.readWithBackup { txt ->
                if (txt.trimStart().startsWith("[")) {
                    // legacy format: a JSON array of paid ids
                    val arr = org.json.JSONArray(txt)
                    (0 until arr.length()).associate { arr.getString(it) to true }
                } else {
                    val o = JSONObject(txt)
                    val m = HashMap<String, Boolean>()
                    val ks = o.keys()
                    while (ks.hasNext()) { val k = ks.next(); m[k] = o.getBoolean(k) }
                    m
                }
            }
        } catch (_: Exception) { emptyMap() }
    }

    private fun readReminderMap(context: Context): Map<String, Reminder> {
        val f = file(context, REMINDER_FILE)
        if (!f.exists()) return emptyMap()
        return try {
            val obj = f.readWithBackup { JSONObject(it) }
            val out = HashMap<String, Reminder>()
            val keys = obj.keys()
            while (keys.hasNext()) { val k = keys.next(); out[k] = reminderFromJson(obj.getJSONObject(k)) }
            out
        } catch (_: Exception) { emptyMap() }
    }

    private fun writeReminderMap(context: Context, map: Map<String, Reminder>) {
        val obj = JSONObject()
        for ((k, r) in map) obj.put(k, reminderToJson(r))
        file(context, REMINDER_FILE).writeKeepingBackup(obj.toString())
    }

    private fun reminderToJson(r: Reminder): JSONObject = JSONObject().apply {
        put("id", r.id); put("dueDate", r.dueDate); put("amount", r.amount)
        put("minAmount", r.minAmount ?: JSONObject.NULL)
        put("counterparty", r.counterparty ?: JSONObject.NULL)
        put("account", r.account); put("recurring", r.recurring)
        put("raw", r.raw); put("source", r.source)
    }

    private fun reminderFromJson(o: JSONObject): Reminder = Reminder(
        id = o.getString("id"),
        dueDate = o.getString("dueDate"),
        amount = o.getDouble("amount"),
        minAmount = if (o.isNull("minAmount")) null else o.getDouble("minAmount"),
        counterparty = if (o.isNull("counterparty")) null else o.getString("counterparty"),
        account = o.optString("account", ""),
        recurring = o.optBoolean("recurring", false),
        raw = o.optString("raw", ""),
        source = o.optString("source", ""),
    )

    fun setOverride(context: Context, id: String, kind: String, category: String) = synchronized(lock) {
        val kv = readKv(context, OVERRIDE_FILE).toMutableMap()
        kv[id] = kind to category
        writeKv(context, OVERRIDE_FILE, kv)
    }

    // Rename a payee, or merge it into another by giving it that payee's name. Rules follow the new name.
    fun setAlias(context: Context, from: String, to: String): Unit = synchronized(lock) {
        val fk = from.lowercase().trim(); val target = to.trim()
        if (fk.isEmpty()) return@synchronized
        val kv = readKv(context, ALIAS_FILE).toMutableMap()
        if (target.isEmpty() || target.equals(from.trim(), ignoreCase = true)) kv.remove(fk)
        else {
            kv[fk] = target to ""
            if (kv[target.lowercase()]?.first.equals(from.trim(), ignoreCase = true)) kv.remove(target.lowercase())
            val rules = readKv(context, RULE_FILE).toMutableMap()
            val r = rules[fk]
            if (r != null && !rules.containsKey(target.lowercase())) { rules[target.lowercase()] = r; writeKv(context, RULE_FILE, rules) }
        }
        writeKv(context, ALIAS_FILE, kv)
    }

    // Undo support: a copy of everything a category / refund / rename decision can change.
    private val UNDO_FILES = listOf(OVERRIDE_FILE, RULE_FILE, ALIAS_FILE, TITLE_FILE, "refund_links.json", ACCOUNTS_FILE, APART_FILE)

    fun snapshot(context: Context): Map<String, String?> = synchronized(lock) {
        UNDO_FILES.associateWith { n -> val f = file(context, n); if (f.exists()) f.readText() else null }
    }

    fun restoreSnapshot(context: Context, snap: Map<String, String?>): Unit = synchronized(lock) {
        for ((n, t) in snap) { val f = file(context, n); if (t == null) f.delete() else f.writeKeepingBackup(t) }
    }

    fun setTitle(context: Context, id: String, title: String): Unit = synchronized(lock) {
        val kv = readKv(context, TITLE_FILE).toMutableMap()
        if (title.isBlank()) kv.remove(id) else kv[id] = title.trim() to ""
        writeKv(context, TITLE_FILE, kv)
    }

    fun setPayeeRule(context: Context, payee: String, kind: String, category: String) = synchronized(lock) {
        val key = payee.lowercase().trim()
        if (key.isEmpty()) return@synchronized
        val kv = readKv(context, RULE_FILE).toMutableMap()
        kv[key] = kind to category
        writeKv(context, RULE_FILE, kv)
    }

    fun splitList(s: String): List<String> =
        s.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    // Accounts you chose not to show on Home.
    fun hiddenAccounts(context: Context): Set<String> =
        splitList(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("hidden_accounts", "") ?: "").toSet()

    fun setAccountHidden(context: Context, account: String, hidden: Boolean) {
        val cur = hiddenAccounts(context).toMutableSet()
        if (hidden) cur.add(account) else cur.remove(account)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("hidden_accounts", cur.joinToString(",")).apply()
    }

    // ---- transaction file ----
    private fun file(context: Context, name: String) = File(context.filesDir, name)

    // The transaction file, or null when neither it nor the copy kept from the previous save can be read (see
    // readWithBackup). A missing file is an empty list.
    private fun readMapOrNull(context: Context): Map<String, Row>? {
        val f = file(context, TXN_FILE)
        if (!f.exists()) return emptyMap()
        return try {
            f.readWithBackup { text ->
                val obj = JSONObject(text)
                val out = HashMap<String, Row>()
                val keys = obj.keys()
                while (keys.hasNext()) { val k = keys.next(); out[k] = rowFromJson(obj.getJSONObject(k)) }
                out
            }
        } catch (_: Exception) { null }
    }

    private fun readMap(context: Context): Map<String, Row> = readMapOrNull(context) ?: emptyMap()

    private fun writeMap(context: Context, map: Map<String, Row>) {
        val obj = JSONObject()
        for ((k, r) in map) obj.put(k, rowToJson(r))
        file(context, TXN_FILE).writeKeepingBackup(obj.toString())
    }

    // ---- key/value files (id -> "kind|category") ----
    private fun readKv(context: Context, name: String): Map<String, Pair<String, String>> {
        val f = file(context, name)
        if (!f.exists()) return emptyMap()
        return try {
            val obj = f.readWithBackup { JSONObject(it) }
            val out = HashMap<String, Pair<String, String>>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val parts = obj.getString(k).split("|", limit = 2)
                if (parts.size == 2) out[k] = parts[0] to parts[1]
            }
            out
        } catch (_: Exception) { emptyMap() }
    }

    private fun writeKv(context: Context, name: String, kv: Map<String, Pair<String, String>>) {
        val obj = JSONObject()
        for ((k, v) in kv) obj.put(k, v.first + "|" + v.second)
        file(context, name).writeKeepingBackup(obj.toString())
    }

    private fun rowToJson(r: Row): JSONObject = JSONObject().apply {
        put("id", r.id); put("date", r.date); put("amount", r.amount)
        put("direction", r.direction)
        put("counterparty", r.counterparty ?: JSONObject.NULL)
        put("ref", r.ref ?: JSONObject.NULL)
        put("raw", r.raw); put("source", r.source); put("account", r.account)
        put("kind", r.kind); put("category", r.category)
        if (r.ms > 0) put("ms", r.ms)
    }

    private fun rowFromJson(o: JSONObject): Row = Row(
        id = o.getString("id"),
        date = o.getString("date"),
        amount = o.getDouble("amount"),
        direction = o.getString("direction"),
        counterparty = if (o.isNull("counterparty")) null else o.getString("counterparty"),
        ref = if (o.isNull("ref")) null else o.getString("ref"),
        raw = o.optString("raw", ""),
        source = o.optString("source", ""),
        account = o.optString("account", ""),
        kind = o.getString("kind"),
        category = o.getString("category"),
        ms = o.optLong("ms", 0L),
    )
}
