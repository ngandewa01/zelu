package com.koolicream.zelu

// The steps that turn stored payments into what the screens count: one account written several ways becomes one,
// accounts you joined count together, repeated EMI and biller notices count once, refunds meet their spends, and money
// moved between your own accounts is a transfer. Plain functions over rows, so they are tested without a phone.
object Reconcile {
    // Labels you joined into another account count under it. A label joined to a loan is that loan's EMI messages, and
    // the money left from the bank's main account, so it counts there.
    fun joinAccounts(rows: List<Row>, dups: Map<String, String>): List<Row> {
        fun root(k: String): String {
            var c = k; var hops = 0
            while (hops++ < 8) { val t = dups[c] ?: break; if (t == c) break; c = t }
            return c
        }
        val counts = rows.groupingBy { it.account }.eachCount()
        val mainOfBank = HashMap<String, String>()
        fun bankMain(loan: String): String {
            val bank = AccountBook.bankOf(loan) ?: return loan.substringBefore(' ')
            return mainOfBank.getOrPut(bank) {
                counts.keys.filter { AccountBook.bankOf(it) == bank && AccountBook.guessType(it) == "savings" && !dups.containsKey(it) }
                    .maxByOrNull { counts[it] ?: 0 } ?: bank
            }
        }
        val target = HashMap<String, String>()
        for (k in counts.keys) if (dups.containsKey(k)) {
            val t = root(k)
            target[k] = if (t.contains(" Loan ")) bankMain(t) else t
        }
        return if (target.isEmpty()) rows else rows.map { r -> target[r.account]?.let { r.copy(account = it) } ?: r }
    }

    // An EMI repeats: the same amount leaves the same account around the same day every month. When one
    // payment of such a series is already known as an EMI (its SMS says so), the other months of that
    // series that came without the word (and stayed Unknown / Transfer) are the same EMI. Nothing is
    // hard-coded, and a category you set yourself is never touched.
    fun inferEmiSeries(rows: List<Row>, userSet: Set<String>): List<Row> {
        val known = rows.filter { it.category == "EMI" && it.direction == Dir.DEBIT && it.account.isNotBlank() }
        if (known.isEmpty()) return rows
        fun day(d: String) = d.substring(8, 10).toIntOrNull() ?: 0
        return rows.map { c ->
            if (c.direction != Dir.DEBIT || c.account.isBlank() || c.id in userSet) return@map c
            if (c.category != "Unknown" && c.category != "Transfer") return@map c
            val series = known.any { e ->
                e.account == c.account && kotlin.math.abs(e.amount - c.amount) < 0.5 &&
                    e.date.take(7) != c.date.take(7) && kotlin.math.abs(day(e.date) - day(c.date)) <= 3
            }
            if (series) c.copy(kind = Kind.EXPENSE, category = "EMI") else c
        }
    }

    // A refund that looks like it belongs to an earlier spend (same merchant, or same amount on the
    // same account, within 120 days) is suggested to you; once you confirm it takes that spend's
    // category and nets against it. Until then (or on "No") it stays a plain "Refund" in Money in.
    // A reversal that quotes the reference of a spend on the same account is that spend coming back: it is paired
    // without asking.
    class Paired(val rows: List<Row>, val suggestions: List<Pair<Row, Row>>)

    // How soon money must come back to count as the same payment undone (a failed ATM withdrawal or card payment), with
    // a little leeway for two banks' clocks; and how far apart one credit's messages from two banks may arrive.
    private const val REVERSAL_WINDOW_S = 15 * 60
    private const val CLOCK_LEEWAY_S = 60
    private const val ECHO_WINDOW_MS = 36 * 3600_000L

    /**
     * True when [back] came within [REVERSAL_WINDOW_S] after [spend] ([CLOCK_LEEWAY_S] of leeway for clocks): by when the two messages
     * arrived, or, for payments stored without that, by the times written in them.
     */
    internal fun isQuickReversal(spend: Row, back: Row): Boolean {
        if (spend.ms > 0 && back.ms > 0) return (back.ms - spend.ms) / 1000 in -CLOCK_LEEWAY_S.toLong()..REVERSAL_WINDOW_S.toLong()
        val a = TxnTime.fromText(spend.raw) ?: return false
        val b = TxnTime.fromText(back.raw) ?: return false
        return b - a in -CLOCK_LEEWAY_S..REVERSAL_WINDOW_S
    }

    fun pairRefunds(rows: List<Row>, answers: Map<String, Boolean>): Paired {
        fun norm(x: String?) = (x ?: "").lowercase().filter { it.isLetterOrDigit() }
        val expenses = rows.filter { it.kind == Kind.EXPENSE }
        val remaining = HashMap<String, Double>().apply { expenses.forEach { put(it.id, it.amount) } }
        val paired = HashMap<String, String>()
        val suggest = ArrayList<Pair<Row, Row>>()
        for (r in rows.filter { it.kind == Kind.REFUND && it.category == "Refund" }.sortedBy { it.date }) {
            if (answers[r.id] == false) continue
            val sameRef = r.ref?.takeIf { it.isNotBlank() }?.let { ref ->
                expenses.firstOrNull { e -> e.ref == ref && e.account == r.account && (remaining[e.id] ?: 0.0) >= r.amount - 0.5 }
            }
            if (sameRef != null) {
                remaining[sameRef.id] = (remaining[sameRef.id] ?: 0.0) - r.amount
                paired[r.id] = sameRef.category
                continue
            }
            // The same amount back on the same account within minutes of the spend: a failed ATM withdrawal or card
            // payment ("ATM Txn Reversed: Rs.10000" two seconds after "Withdrawn Rs.10000"). The money never left, so it
            // nets without asking.
            val quick = expenses.firstOrNull { e ->
                e.account.isNotBlank() && e.account == r.account && e.date == r.date && kotlin.math.abs(e.amount - r.amount) < 0.5 &&
                    (remaining[e.id] ?: 0.0) >= r.amount - 0.5 && isQuickReversal(e, r)
            }
            if (quick != null) {
                remaining[quick.id] = (remaining[quick.id] ?: 0.0) - r.amount
                paired[r.id] = quick.category
                continue
            }
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
        val out = if (paired.isEmpty()) rows else rows.map { r -> paired[r.id]?.let { r.copy(category = it) } ?: r }
        return Paired(out, suggest.sortedByDescending { it.first.date })
    }

    // A transfer between your OWN accounts shows up as the same amount leaving one account
    // and arriving in another within a day. Mark both as transfers, so it counts as neither
    // spending nor income. Learned automatically from your accounts — nothing to configure.
    // Both banks' messages for one such transfer often quote the same reference, which settles it whatever the payee
    // reads as. A payment you categorised yourself (userSet) is left as you set it.
    fun detectSelfTransfers(rows: List<Row>, userSet: Set<String> = emptySet()): List<Row> {
        val move = HashSet<String>()
        val used = HashSet<String>()
        val creditsByRef = rows.filter {
            it.direction == Dir.CREDIT && (it.kind == Kind.INCOME || (it.kind == Kind.TRANSFER && it.category == "Transfer")) && it.account.isNotBlank() &&
                !it.ref.isNullOrBlank() && it.id !in userSet
        }.groupBy { it.ref }
        for (d in rows) {
            if (d.direction != Dir.DEBIT || d.kind != Kind.EXPENSE || d.account.isBlank() || d.ref.isNullOrBlank() || d.id in userSet) continue
            val match = creditsByRef[d.ref]?.firstOrNull { c ->
                c.id !in used && kotlin.math.abs(c.amount - d.amount) < 0.5 && c.account != d.account
            } ?: continue
            used.add(match.id); move.add(d.id); move.add(match.id)
        }
        val credits = rows.filter { it.direction == Dir.CREDIT && it.kind == Kind.INCOME && it.account.isNotBlank() && it.id !in userSet }
        for (d in rows) {
            if (d.direction != Dir.DEBIT || d.kind != Kind.EXPENSE || d.account.isBlank() || d.id in move || d.id in userSet) continue
            val match = credits.firstOrNull { c ->
                c.id !in used && c.amount == d.amount && c.account != d.account && nearDay(c.date, d.date)
            } ?: continue
            used.add(match.id); move.add(d.id); move.add(match.id)
        }
        return rows.map { if (it.id in move) it.copy(kind = Kind.TRANSFER, category = "Transfer") else it }
    }

    private fun within3(a: String, b: String): Boolean = (-3..3).any { a == Dates.shiftDays(b, it) }

    // Same day, or one day apart (a credit can post the next day).
    private fun nearDay(a: String, b: String): Boolean =
        a == b || a == Dates.shiftDays(b, 1) || a == Dates.shiftDays(b, -1)

    // Merge account labels that are the same real account. Each account number is resolved
    // to the bank it appears under MOST often, so a stray signature ("...-SBI" on an IT
    // refund credited to an HDFC account) can't split HDFC 5997 into a phantom SBI 5997.
    // Also folds "CC 6781" into "HDFC CC 6781" and "HDFC 1208" into "HDFC CC 1208".
    // One account written with different tails ("898" and "4898", or "8003" and "4xxx8003" read badly) is one account:
    // same bank, same kind, and at least three trailing digits in common.
    fun mergeTails(rows: List<Row>, apart: Set<String> = emptySet()): List<Row> {
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
                    val from = key.first + (if (key.second) " CC " else " ") + u
                    if (from in apart) continue                 // you separated it
                    used.add(u)
                    remap[from] = key.first + (if (key.second) " CC " else " ") + t
                }
                used.add(t)
            }
        }
        return if (remap.isEmpty()) rows else rows.map { r -> remap[r.account]?.let { r.copy(account = it) } ?: r }
    }

    fun canonicalizeAccounts(rows0: List<Row>, apart: Set<String> = emptySet()): List<Row> {
        val rows = mergeTails(rows0, apart)
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
    /**
     * One credit reported by two banks: the bank that sent the money (SBI, as the income tax refund banker: "An IT Refund
     * amount of Rs 20990 ... credited to your account XXXXXXXXXX5997") and your own bank ("INR 20,990.00 deposited in HDFC
     * Bank A/c XX5997"). Once the account labels are joined (canonicalizeAccounts puts both on HDFC 5997), two credits
     * of the same amount on one account within a day, from senders of different banks, are one payment: the message from
     * the account's own bank stays. Two credits from the account's own bank are left alone.
     */
    fun dedupCrossBank(rows: List<Row>): List<Row> {
        fun bankOf(acct: String) = acct.trim().split(" ").firstOrNull()?.uppercase().orEmpty()
        fun fromOwnBank(r: Row): Boolean {
            val b = bankOf(r.account)
            return b.length >= 2 && Parser.senderCore(r.source).uppercase().contains(b)
        }
        val drop = HashSet<String>()
        val credits = rows.filter { it.direction == Dir.CREDIT && it.account.isNotBlank() && it.source.isNotBlank() }
        for ((_, group) in credits.groupBy { it.account + "|" + Math.round(it.amount * 100) }) {
            if (group.size < 2) continue
            val own = group.filter { fromOwnBank(it) }
            for (other in group) {
                if (fromOwnBank(other) || other.id in drop) continue
                val match = own.any { o ->
                    if (o.ms > 0 && other.ms > 0) kotlin.math.abs(o.ms - other.ms) <= ECHO_WINDOW_MS else nearDay(o.date, other.date)
                }
                if (match) drop.add(other.id)
            }
        }
        return if (drop.isEmpty()) rows else rows.filter { it.id !in drop }
    }

    fun dedupBillers(rows: List<Row>): List<Row> {
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
        // A biller's own "payment received" note (no account) is the same payment as your bank's debit of that
        // amount within three days. Keep the bank's row, which has the account.
        val bankDebits = rows.filter { it.direction == Dir.DEBIT && it.account.isNotBlank() }
        val billerEcho = rows.filter { r ->
            r.direction == Dir.DEBIT && r.account.isBlank() && Parser.isBillerSender(r.source.uppercase()) &&
                bankDebits.any { b -> kotlin.math.abs(b.amount - r.amount) < 0.5 && within3(b.date, r.date) }
        }.map { it.id }.toHashSet()
        return rows.filter { r ->
            val cp = r.counterparty
            when {
                r.id in billerEcho -> false
                r.category == "EMI" -> r.id in emiKeep
                cp != null && Parser.CANON_NAMES.contains(cp) ->
                    seenBrand.add("${r.date}|${r.amount}|$cp|${r.direction}")
                else -> true
            }
        }
    }

    // A bill counts as paid when a payment clearly matches it: money left the same bank between 10 days before and
    // 3 days after the due date, for the same amount (a card bill: anything from its minimum due up to the full amount).
    // Returns the matching payment, or null. A bill with no bank named is never matched, to avoid guessing.
    fun paidBy(bill: Reminder, rows: List<Row>): Row? {
        val bank = bill.account.trim().substringBefore(' ').uppercase()
        if (bank.isEmpty()) return null
        val from = Dates.shiftDays(bill.dueDate, -10)
        val to = Dates.shiftDays(bill.dueDate, 3)
        fun amountFits(a: Double) = kotlin.math.abs(a - bill.amount) <= maxOf(1.0, bill.amount * 0.01) ||
            (bill.minAmount != null && a >= bill.minAmount - 1.0 && a <= bill.amount + 1.0)
        return rows.filter { r ->
            r.direction == Dir.DEBIT && (r.kind == Kind.EXPENSE || r.kind == Kind.TRANSFER) && r.date in from..to &&
                r.account.trim().substringBefore(' ').uppercase() == bank && amountFits(r.amount)
        }.minByOrNull { kotlin.math.abs(Dates.daysBetween(bill.dueDate, it.date)) }
    }
}
