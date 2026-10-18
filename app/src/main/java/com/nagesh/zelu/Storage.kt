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
    private const val BUDGET_FILE = "category_budgets.json" // category -> monthly budget cap

    // Bump when parser/classifier changes so history rebuilds on next open.
    const val PARSER_VERSION = 43

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
    fun loadRows(context: Context): List<Row> = synchronized(lock) {
        val base = readMap(context)
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
        val rows = base.values.map { r ->
            var row = r
            if (aliases.isNotEmpty()) { val n = resolve(r.counterparty); if (n != r.counterparty) row = row.copy(counterparty = n) }
            val payeeKey = (row.counterparty ?: "").lowercase().trim()
            if (payeeKey.isNotEmpty() && rules.containsKey(payeeKey)) {
                val (k, c) = rules.getValue(payeeKey)
                row = row.copy(kind = k, category = c)
            }
            overrides[r.id]?.let { (k, c) -> row = row.copy(kind = k, category = c) }
            Classifier.ALIAS[row.category]?.let { row = row.copy(category = it) }
            row
        }.sortedByDescending { it.date }
        pairRefunds(detectSelfTransfers(dedupBillers(inferEmiSeries(canonicalizeAccounts(rows), overrides.keys))), loadRefundLinks(context))
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
    private fun canonicalizeAccounts(rows: List<Row>): List<Row> {
        val four = Regex("""^\d{4}$""")
        fun parse(s: String): Triple<String, Boolean, String> {
            var bank = ""; var cc = false; var last4 = ""
            for (p in s.trim().split(" ").filter { it.isNotEmpty() }) when {
                p.equals("CC", true) -> cc = true
                four.matches(p) -> last4 = p
                else -> if (bank.isEmpty()) bank = p
            }
            return Triple(bank, cc, last4)
        }
        // last4 -> (bank -> how many rows name that bank); and whether any row marked it a card.
        val bankCount = HashMap<String, HashMap<String, Int>>()
        val ccForBankLast4 = HashMap<String, Boolean>()
        for (r in rows) {
            val (bank, cc, last4) = parse(r.account)
            if (last4.isNotEmpty() && bank.isNotEmpty()) {
                val m = bankCount.getOrPut(last4) { HashMap() }
                m[bank] = (m[bank] ?: 0) + 1
                val k = "$bank|$last4"
                ccForBankLast4[k] = (ccForBankLast4[k] ?: false) || cc
            }
        }
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
        val sorted = readReminderMap(context).values
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
        file(context, REPAID_FILE).writeText(o.toString())
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
        file(context, BUDGET_FILE).writeText(o.toString())
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

    fun loadBalances(context: Context): Map<String, Pair<Double, Long>> = synchronized(lock) {
        readBalFile(context).mapValues { it.value.first to it.value.second }
    }

    fun saveBalances(context: Context, fresh: Map<String, Pair<Double, Long>>, source: String): Unit = synchronized(lock) {
        if (fresh.isEmpty()) return@synchronized
        val cur = readBalFile(context)
        for ((k, v) in fresh) {
            val c = cur[k]
            val take = when {
                c == null -> true
                source == "b" && c.third == "t" -> true
                source == "t" && c.third == "b" -> false
                else -> v.second >= c.second
            }
            if (take) cur[k] = Triple(v.first, v.second, source)
        }
        val o = JSONObject()
        for ((k, v) in cur) o.put(k, JSONObject().put("a", v.first).put("d", v.second).put("s", v.third))
        file(context, "balances.json").writeText(o.toString())
    }

    // ---- latest available credit limit per card account ----
    fun loadLimits(context: Context): Map<String, Pair<Double, Long>> = synchronized(lock) {
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

    fun saveLimits(context: Context, fresh: Map<String, Pair<Double, Long>>): Unit = synchronized(lock) {
        if (fresh.isEmpty()) return@synchronized
        val cur = loadLimits(context).toMutableMap()
        for ((k, v) in fresh) { val c = cur[k]; if (c == null || v.second >= c.second) cur[k] = v }
        val o = JSONObject()
        for ((k, v) in cur) o.put(k, JSONObject().put("a", v.first).put("d", v.second))
        file(context, "limits.json").writeText(o.toString())
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
        file(context, "refund_links.json").writeText(o.toString())
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
        file(context, "plan.json").writeText(o.toString())
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
        file(context, "card_prefs.json").writeText(o.toString())
    }

    // ---- overall monthly spending budget ----
    fun loadMonthlyBudget(context: Context): Double = synchronized(lock) {
        val f = file(context, "monthly_budget.txt")
        if (!f.exists()) return 60000.0
        f.readText().trim().toDoubleOrNull()?.takeIf { it > 0 } ?: 60000.0
    }

    fun saveMonthlyBudget(context: Context, amount: Double): Unit = synchronized(lock) {
        file(context, "monthly_budget.txt").writeText(amount.toString())
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
        file(context, "custom_categories.json").writeText(o.toString())
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
        file(context, "fixed_outflow.json").writeText(o.toString())
    }

    // Explicit paid(true)/unpaid(false) override per reminder. An explicit entry always
    // wins over the auto rules (past-month / matched-debit), so unmark works from any state.
    fun setReminderDone(context: Context, id: String, done: Boolean) = synchronized(lock) {
        val m = loadReminderState(context).toMutableMap()
        m[id] = done
        val o = JSONObject()
        for ((k, v) in m) o.put(k, v)
        file(context, REM_DONE_FILE).writeText(o.toString())
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
        file(context, REMINDER_FILE).writeText(obj.toString())
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
    private val UNDO_FILES = listOf(OVERRIDE_FILE, RULE_FILE, ALIAS_FILE, "refund_links.json")

    fun snapshot(context: Context): Map<String, String?> = synchronized(lock) {
        UNDO_FILES.associateWith { n -> val f = file(context, n); if (f.exists()) f.readText() else null }
    }

    fun restoreSnapshot(context: Context, snap: Map<String, String?>): Unit = synchronized(lock) {
        for ((n, t) in snap) { val f = file(context, n); if (t == null) f.delete() else f.writeText(t) }
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
        file(context, REMINDER_FILE).delete() // reminders re-parse too; done ticks are kept
        Unit
    }

    // ---- transaction file ----
    private fun file(context: Context, name: String) = File(context.filesDir, name)

    private fun readMap(context: Context): Map<String, Row> {
        val f = file(context, TXN_FILE)
        if (!f.exists()) return emptyMap()
        return try {
            val obj = JSONObject(f.readText())
            val out = HashMap<String, Row>()
            val keys = obj.keys()
            while (keys.hasNext()) { val k = keys.next(); out[k] = rowFromJson(obj.getJSONObject(k)) }
            out
        } catch (_: Exception) { emptyMap() }
    }

    private fun writeMap(context: Context, map: Map<String, Row>) {
        val obj = JSONObject()
        for ((k, r) in map) obj.put(k, rowToJson(r))
        file(context, TXN_FILE).writeText(obj.toString())
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
        file(context, name).writeText(obj.toString())
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
