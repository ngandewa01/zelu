package com.nagesh.zelu

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
    private const val ACCOUNTS_FILE = "accounts.json"   // account / loan -> accepted or declined, and its type
    private const val TITLE_FILE = "title_overrides.json" // transaction id -> the title you gave it
    private const val BUDGET_FILE = "category_budgets.json" // category -> monthly budget cap

    // Bump when parser/classifier changes so history rebuilds on next open.
    const val PARSER_VERSION = 53

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
        // An account you marked as a duplicate of another: its payments move to that account (or, for a loan,
        // to the bank's main account, since the money left from there).
        val dups = readKv(context, ACCOUNTS_FILE).filter { it.value.first == "duplicate" }.mapValues { it.value.second }
        val titles = readKv(context, TITLE_FILE)
        // Accounts you said are a different type than Zelu guessed (say, a savings account it read as a card).
        val retype = readKv(context, ACCOUNTS_FILE).filter { it.value.first == "accepted" }
            .mapNotNull { (k, v) -> val n = relabel(k, v.second); if (n != k) k to n else null }.toMap()
        val catOff = readKv(context, CATOFF_FILE).keys
        val splits = loadSplits(context)
        val own = splitList(loadSettings(context).ownAccounts)
        val rows = base.values.map { r ->
            var row = r
            dups[r.account]?.let { t -> row = row.copy(account = if (t.contains(" Loan ")) t.substringBefore(' ') else t) }
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
        }.sortedByDescending { it.date }
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
        val canon = canonicalizeAccounts(expanded)
        val retyped = if (retype.isEmpty()) canon else canon.map { r ->
            val n = retype[r.account]
            if (n == null) r else reapply(r.copy(account = n))
        }
        val declined = readKv(context, ACCOUNTS_FILE).filter { it.value.first == "declined" }.keys
        return pairRefunds(detectSelfTransfers(dedupBillers(inferEmiSeries(retyped, overrides.keys))), loadRefundLinks(context))
            .let { all -> if (declined.isEmpty()) all else all.filter { it.account !in declined && (it.counterparty ?: "") !in declined } }
    }

    // The label an account takes once you say what it is: a savings account never carries "CC", a card always does.
    fun relabel(key: String, type: String): String = when {
        key.contains("Loan") -> key
        type == "savings" && key.contains(" CC") -> key.replace(" CC", "")
        type == "card" && !key.contains(" CC") && !key.startsWith("CC ") ->
            if (key.contains(' ')) key.substringBefore(' ') + " CC " + key.substringAfter(' ') else "$key CC"
        else -> key
    }

    // ---- accounts you confirmed (or turned down) ----  key -> (status, type)
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

    // ---- linked accounts: a debit card or duplicate label that belongs to one of your accounts ----
    // Stored in the same registry as a "duplicate", so its payments, balance and spend already count under the main account.
    fun linkedMap(context: Context): Map<String, List<String>> =
        accountRegistry(context).filter { it.value.first == "duplicate" }.entries.groupBy({ it.value.second }, { it.key })

    fun linkAccount(context: Context, key: String, target: String) =
        setAccountStatuses(context, mapOf(key to ("duplicate" to target)))

    fun unlinkAccount(context: Context, key: String): Unit = synchronized(lock) {
        val kv = readKv(context, ACCOUNTS_FILE).toMutableMap()
        kv[key] = "accepted" to (if (key.contains(" CC")) "card" else "savings")
        writeKv(context, ACCOUNTS_FILE, kv)
    }

    fun linkRejected(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet("link_rejected", emptySet()) ?: emptySet()

    fun rejectLink(context: Context, key: String, target: String) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putStringSet("link_rejected", (linkRejected(context) + "$key|$target").toSet()).apply()
    }

    fun accountsConfirmed(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("accounts_confirmed", false)

    fun setAccountsConfirmed(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("accounts_confirmed", true).apply()
    }

    // An EMI repeats: the same amount leaves the same account around the same day every month. When one
    // payment of such a series is already known as an EMI (its SMS says so), the other months of that
    // series that came without the word (and stayed Unknown / Transfer) are the same EMI. Nothing is
    // hard-coded, and a category you set yourself is never touched.
    private fun inferEmiSeries(rows: List<Row>, userSet: Set<String>): List<Row> {
        val known = rows.filter { it.category == "EMI" && it.direction == "debit" && it.account.isNotBlank() }
        if (known.isEmpty()) return rows
        fun day(d: String) = d.substring(8, 10).toIntOrNull() ?: 0
        return rows.map { c ->
            if (c.direction != "debit" || c.account.isBlank() || c.id in userSet) return@map c
            if (c.category != "Unknown" && c.category != "Transfer") return@map c
            val series = known.any { e ->
                e.account == c.account && kotlin.math.abs(e.amount - c.amount) < 0.5 &&
                    e.date.take(7) != c.date.take(7) && kotlin.math.abs(day(e.date) - day(c.date)) <= 3
            }
            if (series) c.copy(kind = "expense", category = "EMI") else c
        }
    }

    // A refund that looks like it belongs to an earlier spend (same merchant, or same amount on the
    // same account, within 120 days) is suggested to you; once you confirm it takes that spend's
    // category and nets against it. Until then (or on "No") it stays a plain "Refund" in Money in.
    private fun pairRefunds(rows: List<Row>, answers: Map<String, Boolean>): List<Row> {
        fun norm(x: String?) = (x ?: "").lowercase().filter { it.isLetterOrDigit() }
        val expenses = rows.filter { it.kind == "expense" }
        val remaining = HashMap<String, Double>().apply { expenses.forEach { put(it.id, it.amount) } }
        val paired = HashMap<String, String>()
        val suggest = ArrayList<Pair<Row, Row>>()
        for (r in rows.filter { it.kind == "refund" && it.category == "Refund" }.sortedBy { it.date }) {
            if (answers[r.id] == false) continue
            // Only suggest a pairing that is clearly the same purchase: the same amount (within 60 days),
            // or the same payee with a refund that is most of the original (within 45 days). A small
            // refund against a much bigger spend at a busy merchant (Amazon...) is too uncertain to guess.
            val lo = Dates.shiftDays(r.date, -60)
            val lo45 = Dates.shiftDays(r.date, -45)
            val rc = norm(r.counterparty)
            val best = expenses.filter { e ->
                val samePayee = rc.length >= 4 && norm(e.counterparty).let { ec -> ec.length >= 4 && (ec.contains(rc) || rc.contains(ec)) }
                val exact = kotlin.math.abs(e.amount - r.amount) < 0.5
                e.date <= r.date && e.date >= lo && (remaining[e.id] ?: 0.0) >= r.amount - 0.5 && (
                    (samePayee && (exact || (e.date >= lo45 && r.amount >= e.amount * 0.6))) ||
                        (exact && e.account.isNotBlank() && e.account == r.account))
            }.sortedWith(compareByDescending<Row> { kotlin.math.abs(it.amount - r.amount) < 0.5 }.thenByDescending { it.date })
                .firstOrNull() ?: continue
            if (answers[r.id] == true) {
                remaining[best.id] = (remaining[best.id] ?: 0.0) - r.amount
                paired[r.id] = best.category
            } else suggest.add(r to best)
        }
        Refunds.suggestions = suggest.sortedByDescending { it.first.date }
        return if (paired.isEmpty()) rows else rows.map { r -> paired[r.id]?.let { r.copy(category = it) } ?: r }
    }

    // A transfer between your OWN accounts shows up as the same amount leaving one account
    // and arriving in another within a day. Mark both as transfers, so it counts as neither
    // spending nor income. Learned automatically from your accounts — nothing to configure.
    private fun detectSelfTransfers(rows: List<Row>): List<Row> {
        val move = HashSet<String>()
        val credits = rows.filter { it.direction == "credit" && it.kind == "income" && it.account.isNotBlank() }
        val used = HashSet<String>()
        for (d in rows) {
            if (d.direction != "debit" || d.kind != "expense" || d.account.isBlank()) continue
            val match = credits.firstOrNull { c ->
                c.id !in used && c.amount == d.amount && c.account != d.account && nearDay(c.date, d.date)
            } ?: continue
            used.add(match.id); move.add(d.id); move.add(match.id)
        }
        return rows.map { if (it.id in move) it.copy(kind = "transfer", category = "Transfer") else it }
    }

    // Same day, or one day apart (a credit can post the next day).
    private fun nearDay(a: String, b: String): Boolean =
        a == b || a == Dates.shiftDays(b, 1) || a == Dates.shiftDays(b, -1)

    // Merge account labels that are the same real account. Each account number is resolved
    // to the bank it appears under MOST often, so a stray signature ("...-SBI" on an IT
    // refund credited to an HDFC account) can't split HDFC 5997 into a phantom SBI 5997.
    // Also folds "CC 6781" into "HDFC CC 6781" and "HDFC 1208" into "HDFC CC 1208".
    // One account written with different tails ("898" and "4898", or "8003" and "4xxx8003" read badly) is one account:
    // same bank, same kind, and at least three trailing digits in common.
    private fun mergeTails(rows: List<Row>): List<Row> {
        val digits = Regex("""^\d{2,}$""")
        fun split(s: String): Triple<String, Boolean, String>? {
            if (s.contains("Loan") || s.contains("FASTag")) return null
            val parts = s.trim().split(" ").filter { it.isNotEmpty() }
            val tail = parts.lastOrNull()?.takeIf { digits.matches(it) } ?: return null
            val bank = parts.firstOrNull { !it.equals("CC", true) && it != tail } ?: return null
            return Triple(bank, parts.any { it.equals("CC", true) }, tail)
        }
        val count = HashMap<Triple<String, Boolean, String>, Int>()
        for (r in rows) split(r.account)?.let { count[it] = (count[it] ?: 0) + 1 }
        fun same(a: String, b: String) = a == b || (minOf(a.length, b.length) >= 3 && a.takeLast(3) == b.takeLast(3))
        val remap = HashMap<String, String>()
        for ((key, _) in count.entries.groupBy { it.key.first to it.key.second }) {
            val tails = count.filter { it.key.first == key.first && it.key.second == key.second }.map { it.key.third to it.value }
            val used = HashSet<String>()
            for ((t, _) in tails.sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenByDescending { it.first.length })) {
                if (t in used) continue
                for ((u, _) in tails) if (u != t && u !in used && same(t, u)) {
                    used.add(u)
                    remap[key.first + (if (key.second) " CC " else " ") + u] = key.first + (if (key.second) " CC " else " ") + t
                }
                used.add(t)
            }
        }
        return if (remap.isEmpty()) rows else rows.map { r -> remap[r.account]?.let { r.copy(account = it) } ?: r }
    }

    private fun canonicalizeAccounts(rows0: List<Row>): List<Row> {
        val rows = mergeTails(rows0)
        val four = Regex("""^\d{4}$""")
        val anyDigits = Regex("""^\d{2,}$""")
        fun parse(s: String): Triple<String, Boolean, String> {
            var bank = ""; var cc = false; var last4 = ""
            val isTail = if (s.contains("Loan")) four else anyDigits
            for (p in s.trim().split(" ").filter { it.isNotEmpty() }) when {
                p.equals("CC", true) -> cc = true
                isTail.matches(p) -> last4 = p
                else -> if (bank.isEmpty()) bank = p
            }
            return Triple(bank, cc, last4)
        }
        // last4 -> (bank -> how many rows name that bank); and whether any row marked it a card.
        val bankCount = HashMap<String, HashMap<String, Int>>()
        val ccForBankLast4 = HashMap<String, Boolean>()
        // A savings account reports "Avl Bal"; a card reports "Avl Lmt". One stray message that says "card" must
        // not turn an account that keeps reporting balances into a credit card.
        val ccRows = HashMap<String, Int>()
        val balRows = HashMap<String, Int>()
        for (r in rows) {
            val (bank, cc, last4) = parse(r.account)
            if (last4.isNotEmpty() && bank.isNotEmpty()) {
                val m = bankCount.getOrPut(last4) { HashMap() }
                m[bank] = (m[bank] ?: 0) + 1
                val k = "$bank|$last4"
                if (cc) ccRows[k] = (ccRows[k] ?: 0) + 1
                else if (Parser.balanceFromTxn(r.raw) != null && Parser.limitFromTxn(r.raw) == null) balRows[k] = (balRows[k] ?: 0) + 1
            }
        }
        for ((k, n) in ccRows) ccForBankLast4[k] = n > 0 && (balRows[k] ?: 0) < n * 2
        for (k in balRows.keys) if (!ccForBankLast4.containsKey(k)) ccForBankLast4[k] = false
        // Savings accounts per bank (last4 not seen as a card) with their row counts, so a
        // bank-only label like "HDFC" (from loan EMIs) folds into the main HDFC savings a/c.
        val savingsCount = HashMap<String, HashMap<String, Int>>()
        val cardCount = HashMap<String, HashMap<String, Int>>()
        for ((l4, banks) in bankCount) {
            for ((bk, cnt) in banks) {
                if (ccForBankLast4["$bk|$l4"] == true) cardCount.getOrPut(bk) { HashMap() }[l4] = cnt
                else savingsCount.getOrPut(bk) { HashMap() }[l4] = cnt
            }
        }
        fun canonical(acct: String): String {
            val (bank0, cc0, last4) = parse(acct)
            if (last4.isEmpty()) {
                if (bank0.isNotEmpty() && !cc0) {
                    val sav = savingsCount[bank0]?.maxByOrNull { it.value }?.key
                    if (sav != null) return "$bank0 $sav"
                    // No savings for this bank — fold into its main card instead.
                    val card = cardCount[bank0]?.maxByOrNull { it.value }?.key
                    if (card != null) return "$bank0 CC $card"
                }
                return acct
            }
            val bank = bankCount[last4]?.maxByOrNull { it.value }?.key ?: bank0
            if (bank.isEmpty()) return acct
            val anyCC = ccForBankLast4["$bank|$last4"] ?: cc0
            return (bank + " " + (if (anyCC) "CC " else "") + last4).trim()
        }
        return rows.map { it.copy(account = canonical(it.account)) }
    }

    // Collapse duplicates:
    //  - Billers (e.g. Jio) send two confirmation SMS for one payment: same
    //    date + amount + known merchant + direction.
    //  - Loan EMIs arrive as repeated NACH notices: keep one per month per loan.
    private fun dedupBillers(rows: List<Row>): List<Row> {
        val seenBrand = HashSet<String>()
        // One EMI per (month, amount) — the real bank debit and the lender's "payment
        // received" confirmation are the same installment. Prefer the row that has an account.
        val emiBest = HashMap<String, Row>()
        for (r in rows) if (r.category == "EMI") {
            val k = "${r.date.take(7)}|${r.amount}"
            val cur = emiBest[k]
            if (cur == null || (cur.account.isBlank() && r.account.isNotBlank())) emiBest[k] = r
        }
        val emiKeep = emiBest.values.map { it.id }.toHashSet()
        return rows.filter { r ->
            val cp = r.counterparty
            when {
                r.category == "EMI" -> r.id in emiKeep
                cp != null && Parser.CANON_NAMES.contains(cp) ->
                    seenBrand.add("${r.date}|${r.amount}|$cp|${r.direction}")
                else -> true
            }
        }
    }

    fun saveRows(context: Context, newRows: List<Row>): Int = synchronized(lock) {
        val map = readMap(context).toMutableMap()
        var added = 0
        for (r in newRows) if (!map.containsKey(r.id)) { map[r.id] = r; added++ }
        writeMap(context, map)
        added
    }

    // ---- senders you said are your bank ----
    private const val SENDER_FILE = "trusted_senders.json"
    fun trustedSenders(context: Context): Set<String> = synchronized(lock) { readKv(context, SENDER_FILE).keys }
    fun loadTrust(context: Context) = synchronized(lock) { applyTrust(readKv(context, SENDER_FILE)) }
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
            val obj = JSONObject(f.readText()); val out = HashMap<String, List<Pair<Double, String>>>()
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
        val obj = JSONObject()
        for ((k, v) in cur) obj.put(k, org.json.JSONArray().also { arr -> v.forEach { arr.put(JSONObject().put("a", it.first).put("c", it.second)) } })
        file(context, SPLIT_FILE).writeAtomic(obj.toString())
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
    fun lastRead(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("last_read", 0L)
    fun newestBank(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("newest_bank", 0L)

    // Deletes everything Zelu keeps on this phone (files and saved settings). Theme and app lock are left alone.
    fun eraseAll(context: Context): Unit = synchronized(lock) {
        context.filesDir.listFiles()?.forEach { if (it.isFile) it.delete() }
        for (n in listOf(PREFS, "zelu_backup", "zelu_recap")) context.getSharedPreferences(n, Context.MODE_PRIVATE).edit().clear().apply()
    }

    // Where "Report a problem" emails go; change it from Menu > Help.
    const val DEFAULT_REPORT_EMAIL = "ngandewar01@gmail.com"
    fun reportEmail(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("report_email", null)?.takeIf { it.isNotBlank() } ?: DEFAULT_REPORT_EMAIL
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
            val obj = JSONObject(f.readText()); val out = ArrayList<Mandate>()
            for (k in obj.keys()) out.add(mandateFromJson(obj.getJSONObject(k)))
            out.sortedByDescending { it.lastMs }
        } catch (_: Exception) { emptyList() }
    }

    // The newest message about a mandate decides its status; older ones only fill in what is missing.
    fun saveMandates(context: Context, found: List<Mandate>): Unit = synchronized(lock) {
        val cur = loadMandates(context).associateBy { it.key }.toMutableMap()
        for (m in found.sortedBy { it.lastMs }) {
            val e = cur[m.key]
            cur[m.key] = if (e == null) m else if (m.lastMs >= e.lastMs) m.copy(
                amount = m.amount ?: e.amount, account = m.account.ifBlank { e.account }, since = minOf(e.since, m.since),
                nextDate = if (m.status == "scheduled" || m.status == "active") (m.nextDate ?: e.nextDate) else null)
            else e.copy(amount = e.amount ?: m.amount, account = e.account.ifBlank { m.account }, since = minOf(e.since, m.since))
        }
        if (found.isNotEmpty()) {
            val obj = JSONObject()
            for ((k, v) in cur) obj.put(k, mandateToJson(v))
            file(context, MANDATE_FILE).writeAtomic(obj.toString())
        }
    }

    // ---- payments you added by hand from a message Zelu couldn't read ----
    fun addManualTxn(context: Context, row: Row, smsKey: String): Unit = synchronized(lock) {
        val f = file(context, MANUAL_TXN_FILE)
        val obj = try { if (f.exists()) JSONObject(f.readText()) else JSONObject() } catch (_: Exception) { JSONObject() }
        obj.put(row.id, rowToJson(row))
        f.writeAtomic(obj.toString())
        markSmsHandled(context, smsKey)
    }

    private fun manualTxns(context: Context): Map<String, Row> {
        val f = file(context, MANUAL_TXN_FILE)
        if (!f.exists()) return emptyMap()
        return try {
            val obj = JSONObject(f.readText()); val out = HashMap<String, Row>()
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
    fun saveReminders(context: Context, newOnes: List<Reminder>): Int = synchronized(lock) {
        val map = readReminderMap(context).toMutableMap()
        var added = 0
        for (r in newOnes) if (!map.containsKey(r.id)) { map[r.id] = r; added++ }
        writeReminderMap(context, map)
        added
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
        if (f.exists()) org.json.JSONArray(f.readText()) else org.json.JSONArray()
    } catch (_: Exception) { org.json.JSONArray() }

    fun addManualReminder(context: Context, name: String, amount: Double, dueDate: String, recurring: Boolean) = synchronized(lock) {
        val arr = readManual(context)
        arr.put(JSONObject().put("id", System.currentTimeMillis().toString()).put("name", name).put("amount", amount)
            .put("due", dueDate).put("recurring", recurring))
        file(context, MANUAL_FILE).writeAtomic(arr.toString())
    }

    // id is a reminder id ("man-<id>" or "man-<id>-<yyyy-MM>").
    fun removeManualReminder(context: Context, reminderId: String) = synchronized(lock) {
        val base = reminderId.removePrefix("man-").substringBefore("-")
        val arr = readManual(context); val keep = org.json.JSONArray()
        for (i in 0 until arr.length()) if (arr.getJSONObject(i).getString("id") != base) keep.put(arr.getJSONObject(i))
        file(context, MANUAL_FILE).writeAtomic(keep.toString())
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
            val o = JSONObject(f.readText())
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
        file(context, REPAID_FILE).writeAtomic(o.toString())
    }

    // ---- per-category budgets ----
    fun loadBudgets(context: Context): Map<String, Double> = synchronized(lock) {
        val f = file(context, BUDGET_FILE)
        if (!f.exists()) return emptyMap()
        try {
            val o = JSONObject(f.readText())
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
        file(context, BUDGET_FILE).writeAtomic(o.toString())
    }

    // ---- latest known bank balance per account (account -> amount, receivedMs) ----
    // Source "b" = a balance-only alert (authoritative); "t" = tail of a debit/credit alert,
    // used only for accounts that never send a balance-only alert.
    private fun readBalFile(context: Context): MutableMap<String, Triple<Double, Long, String>> {
        val f = file(context, "balances.json")
        val m = LinkedHashMap<String, Triple<Double, Long, String>>()
        if (!f.exists()) return m
        try {
            val o = JSONObject(f.readText())
            val ks = o.keys()
            while (ks.hasNext()) {
                val k = ks.next(); val e = o.getJSONObject(k)
                m[k] = Triple(e.getDouble("a"), e.getLong("d"), e.optString("s", "b"))
            }
        } catch (_: Exception) {}
        return m
    }

    fun loadBalances(context: Context): Map<String, Pair<Double, Long>> = loadBalancesRaw(context).let { m ->
        val d = hiddenKeys(context); if (d.isEmpty()) m else m.filterKeys { it !in d }
    }

    private fun loadBalancesRaw(context: Context): Map<String, Pair<Double, Long>> = synchronized(lock) {
        readBalFile(context).mapValues { it.value.first to it.value.second }
    }

    fun saveBalances(context: Context, fresh: Map<String, Pair<Double, Long>>, source: String): Unit = synchronized(lock) {
        if (fresh.isEmpty()) return@synchronized
        val cur = readBalFile(context)
        for ((k, v) in fresh) {
            val c = cur[k]
            // The newest figure wins, whichever kind of SMS it came from.
            val take = c == null || v.second >= c.second
            if (take) cur[k] = Triple(v.first, v.second, source)
        }
        val o = JSONObject()
        for ((k, v) in cur) o.put(k, JSONObject().put("a", v.first).put("d", v.second).put("s", v.third))
        file(context, "balances.json").writeAtomic(o.toString())
    }

    // ---- latest available credit limit per card account ----
    fun loadLimits(context: Context): Map<String, Pair<Double, Long>> = loadLimitsRaw(context).let { m ->
        val d = hiddenKeys(context); if (d.isEmpty()) m else m.filterKeys { k -> k.substringBefore("#total") !in d }
    }

    private fun loadLimitsRaw(context: Context): Map<String, Pair<Double, Long>> = synchronized(lock) {
        val f = file(context, "limits.json")
        if (!f.exists()) return emptyMap()
        try {
            val o = JSONObject(f.readText())
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
        file(context, "limits.json").writeAtomic(o.toString())
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
        file(context, "limits.json").writeAtomic(o.toString())
    }

    // ---- refund links: refund id -> true (same spend, net it) / false (not related) ----
    fun loadRefundLinks(context: Context): Map<String, Boolean> = synchronized(lock) {
        val f = file(context, "refund_links.json")
        if (!f.exists()) return emptyMap()
        try {
            val o = JSONObject(f.readText()); val m = HashMap<String, Boolean>()
            val ks = o.keys(); while (ks.hasNext()) { val k = ks.next(); m[k] = o.getBoolean(k) }
            m
        } catch (_: Exception) { emptyMap() }
    }

    fun setRefundLink(context: Context, refundId: String, yes: Boolean): Unit = synchronized(lock) {
        val cur = loadRefundLinks(context).toMutableMap(); cur[refundId] = yes
        val o = JSONObject(); for ((k, v) in cur) o.put(k, v)
        file(context, "refund_links.json").writeAtomic(o.toString())
    }

    // ---- savings plan inputs (SIP, EMI, goal...) ----
    fun loadPlan(context: Context): Map<String, String> = synchronized(lock) {
        val f = file(context, "plan.json")
        if (!f.exists()) return emptyMap()
        try {
            val o = JSONObject(f.readText()); val m = HashMap<String, String>()
            val ks = o.keys(); while (ks.hasNext()) { val k = ks.next(); m[k] = o.getString(k) }
            m
        } catch (_: Exception) { emptyMap() }
    }

    fun savePlan(context: Context, m: Map<String, String>): Unit = synchronized(lock) {
        val o = JSONObject(); for ((k, v) in m) o.put(k, v)
        file(context, "plan.json").writeAtomic(o.toString())
    }

    // ---- per-card preferences: billing cycle day + monthly budget ----
    fun loadCardPrefs(context: Context): Map<String, Pair<Int, Double>> = synchronized(lock) {
        val f = file(context, "card_prefs.json")
        if (!f.exists()) return emptyMap()
        try {
            val o = JSONObject(f.readText())
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
        file(context, "card_prefs.json").writeAtomic(o.toString())
    }

    // ---- overall monthly spending budget ----
    fun loadMonthlyBudget(context: Context): Double = synchronized(lock) {
        val f = file(context, "monthly_budget.txt")
        if (!f.exists()) 0.0 else f.readText().trim().toDoubleOrNull()?.takeIf { it >= 0 } ?: 0.0
    }

    fun saveMonthlyBudget(context: Context, amount: Double): Unit = synchronized(lock) {
        file(context, "monthly_budget.txt").writeAtomic(amount.toString())
    }

    // ---- user-made categories (name -> emoji + colour) ----
    fun loadCustomCats(context: Context): Map<String, Pair<String, Long>> = synchronized(lock) {
        val f = file(context, "custom_categories.json")
        if (!f.exists()) return emptyMap()
        try {
            val o = JSONObject(f.readText())
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
        file(context, "custom_categories.json").writeAtomic(o.toString())
    }

    // ---- fixed monthly outflow plan (name -> typical monthly amount) ----
    fun loadFixed(context: Context): Map<String, Double> = synchronized(lock) {
        val f = file(context, "fixed_outflow.json")
        if (!f.exists()) return emptyMap()
        try {
            val o = JSONObject(f.readText())
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
        file(context, "fixed_outflow.json").writeAtomic(o.toString())
    }

    // Explicit paid(true)/unpaid(false) override per reminder. An explicit entry always
    // wins over the auto rules (past-month / matched-debit), so unmark works from any state.
    fun setReminderDone(context: Context, id: String, done: Boolean) = synchronized(lock) {
        val m = loadReminderState(context).toMutableMap()
        m[id] = done
        val o = JSONObject()
        for ((k, v) in m) o.put(k, v)
        file(context, REM_DONE_FILE).writeAtomic(o.toString())
    }

    fun loadReminderState(context: Context): Map<String, Boolean> = synchronized(lock) {
        val f = file(context, REM_DONE_FILE)
        if (!f.exists()) return emptyMap()
        val txt = f.readText()
        return try {
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
        } catch (_: Exception) { emptyMap() }
    }

    private fun readReminderMap(context: Context): Map<String, Reminder> {
        val f = file(context, REMINDER_FILE)
        if (!f.exists()) return emptyMap()
        return try {
            val obj = JSONObject(f.readText())
            val out = HashMap<String, Reminder>()
            val keys = obj.keys()
            while (keys.hasNext()) { val k = keys.next(); out[k] = reminderFromJson(obj.getJSONObject(k)) }
            out
        } catch (_: Exception) { emptyMap() }
    }

    private fun writeReminderMap(context: Context, map: Map<String, Reminder>) {
        val obj = JSONObject()
        for ((k, r) in map) obj.put(k, reminderToJson(r))
        file(context, REMINDER_FILE).writeAtomic(obj.toString())
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
    private val UNDO_FILES = listOf(OVERRIDE_FILE, RULE_FILE, ALIAS_FILE, TITLE_FILE, "refund_links.json", ACCOUNTS_FILE)

    fun snapshot(context: Context): Map<String, String?> = synchronized(lock) {
        UNDO_FILES.associateWith { n -> val f = file(context, n); if (f.exists()) f.readText() else null }
    }

    fun restoreSnapshot(context: Context, snap: Map<String, String?>): Unit = synchronized(lock) {
        for ((n, t) in snap) { val f = file(context, n); if (t == null) f.delete() else f.writeAtomic(t) }
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

    fun clear(context: Context) = synchronized(lock) {
        file(context, TXN_FILE).delete()
        file(context, MANDATE_FILE).delete() // mandates are re-read from the messages too
        file(context, REMINDER_FILE).delete() // reminders re-parse too; done ticks are kept
        dropBankOnlyBalances(context) // balances that came from loan messages were wrong; real ones return on re-read
        Unit
    }

    private fun dropBankOnlyBalances(context: Context) {
        try {
            val f = file(context, "balances.json")
            if (!f.exists()) return
            val o = JSONObject(f.readText()); val keep = JSONObject()
            val ks = o.keys()
            while (ks.hasNext()) { val k = ks.next(); if (k.trim().split(" ").any { it.length in 3..4 && it.all(Char::isDigit) }) keep.put(k, o.get(k)) }
            f.writeAtomic(keep.toString())
        } catch (_: Exception) {}
    }

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

    private fun parseMap(f: File): Map<String, Row> {
        val obj = JSONObject(f.readText())
        val out = HashMap<String, Row>()
        val keys = obj.keys()
        while (keys.hasNext()) { val k = keys.next(); out[k] = rowFromJson(obj.getJSONObject(k)) }
        return out
    }

    // If the transaction file can't be read, fall back to the copy kept from the previous save, and keep
    // the damaged file aside rather than letting the next save overwrite it with an almost empty list.
    private fun readMap(context: Context): Map<String, Row> {
        val f = file(context, TXN_FILE)
        if (!f.exists()) return emptyMap()
        return try { parseMap(f) } catch (_: Exception) {
            val bak = File(f.parentFile, TXN_FILE + ".bak")
            try { f.copyTo(File(f.parentFile, TXN_FILE + ".corrupt"), overwrite = true) } catch (_: Exception) {}
            try { if (bak.exists()) parseMap(bak) else emptyMap() } catch (_: Exception) { emptyMap() }
        }
    }

    private fun writeMap(context: Context, map: Map<String, Row>) {
        val obj = JSONObject()
        for ((k, r) in map) obj.put(k, rowToJson(r))
        val f = file(context, TXN_FILE)
        // Keep the previous version as a safety copy before replacing it.
        if (f.exists() && f.length() > 2) try { f.copyTo(File(f.parentFile, TXN_FILE + ".bak"), overwrite = true) } catch (_: Exception) {}
        f.writeAtomic(obj.toString())
    }

    // ---- key/value files (id -> "kind|category") ----
    private fun readKv(context: Context, name: String): Map<String, Pair<String, String>> {
        val f = file(context, name)
        if (!f.exists()) return emptyMap()
        return try {
            val obj = JSONObject(f.readText())
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
        file(context, name).writeAtomic(obj.toString())
    }

    private fun rowToJson(r: Row): JSONObject = JSONObject().apply {
        put("id", r.id); put("date", r.date); put("amount", r.amount)
        put("direction", r.direction)
        put("counterparty", r.counterparty ?: JSONObject.NULL)
        put("ref", r.ref ?: JSONObject.NULL)
        put("raw", r.raw); put("source", r.source); put("account", r.account)
        put("kind", r.kind); put("category", r.category)
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
    )
}
