package com.koolicream.zelu

/**
 * The "Your accounts" screen: which accounts it shows, the labels joined into each one, the accounts that could be
 * the same as the one you open, and what one Save changes. No Android code here, so it is tested on its own.
 *
 *  label     - an account as Zelu reads it from messages: "MGB 883076", "HDFC CC 9065", or a bare "76" when no bank was named
 *  part      - a label joined into another account; its payments and balance count under that account
 *  auto part - a part Zelu joined by itself (the same number written with fewer digits); you can still separate it
 *  apart     - a label you separated; Zelu does not join it by itself again
 */
object AccountBook {

    /** How many payments a label has, and its newest one. [looks]: "fd" or "rd" when its messages are mostly about a deposit. */
    data class Stat(val count: Int, val lastDate: String, val lastAmount: Double, val lastRaw: String, val looks: String? = null)

    data class Part(val key: String, val stat: Stat, val auto: Boolean)

    data class Acct(
        val key: String, val type: String, val own: Stat, val parts: List<Part>,
        val balance: Double?, val limit: Double?, val declined: Boolean,
    ) {
        /** Payments under this account. An auto part's payments are already in [own]. */
        val count: Int get() = own.count + parts.filter { !it.auto }.sumOf { it.stat.count }
        val bank: String? get() = bankOf(key)
    }

    /** What one Save writes: registry entries (label -> status, type), and labels to keep apart or no longer apart. */
    data class Plan(val main: String, val registry: Map<String, Pair<String, String>>, val apartAdd: Set<String>, val apartRemove: Set<String>)

    private val DIGITS = Regex("""^\d{2,}$""")
    private val LOAN_NAME = Regex(""".*\bLoan \d{3,4}$""")

    /** The account number at the end of a label: "HDFC CC 9065" -> "9065", "76" -> "76", "PAYTM" -> null. */
    fun numberOf(key: String): String? = key.trim().substringAfterLast(' ').takeIf { DIGITS.matches(it) }

    /** The bank in a label, or null when the label is only a number. */
    fun bankOf(key: String): String? {
        val parts = key.trim().split(" ").filter { it.isNotEmpty() }
        val words = parts.filterIndexed { i, p -> !(i == parts.size - 1 && DIGITS.matches(p)) && !p.equals("CC", true) && p != "Loan" && p != "FD" && p != "RD" && p != "DC" }
        return words.joinToString(" ").takeIf { w -> w.any { it.isLetter() } }
    }

    /** What the screen calls a label: a bare number reads "Account ending 76". */
    fun displayName(key: String): String = numberOf(key)?.takeIf { bankOf(key) == null }?.let { "Account ending $it" } ?: key

    fun guessType(key: String): String = when {
        key.contains(" Loan ") -> "loan"
        key.contains(" DC ") -> "debit"
        key.contains(" FD ") -> "fd"
        key.contains(" RD ") -> "rd"
        key.contains("FASTag", true) -> "other"
        key.contains(" CC") || key.startsWith("CC ") -> "card"
        else -> "savings"
    }

    /** The label an account takes once you say what it is: a savings account never carries "CC", a card always does. */
    fun relabel(key: String, type: String): String {
        if (key.contains("Loan") || key.contains(" DC ")) return key
        val marker = when (type) { "card" -> "CC"; "loan" -> "Loan"; "fd" -> "FD"; "rd" -> "RD"; else -> null }
        if (type == "other") return key
        val base = key.replace(" CC", "").replace(" FD", "").replace(" RD", "")
        if (marker == null) return base
        if (marker == "CC" && key.startsWith("CC ")) return key
        if (key.split(' ').contains(marker)) return key
        return if (base.contains(' ')) base.substringBefore(' ') + " $marker " + base.substringAfter(' ') else "$base $marker"
    }

    private val FD_WORDS = Regex("""\b(?:fd|fixed deposit|term deposit|tdr|stdr)\b""", RegexOption.IGNORE_CASE)
    private val RD_WORDS = Regex("""\b(?:rd|recurring deposit)\b""", RegexOption.IGNORE_CASE)

    // People do not remember which number is their FD or RD. A label whose messages are mostly about a deposit is one;
    // a savings account that once opened an FD has many other messages and stays savings.
    private fun looksOf(raws: List<String>): String? {
        val half = raws.size / 2.0
        return when {
            raws.count { RD_WORDS.containsMatchIn(it) } > half -> "rd"
            raws.count { FD_WORDS.containsMatchIn(it) } > half -> "fd"
            else -> null
        }
    }

    private fun statOf(rs: List<Row>): Stat {
        val last = rs.maxByOrNull { it.date }!!
        return Stat(rs.size, last.date, last.amount, last.raw, looksOf(rs.map { it.raw }))
    }

    /** Payments per account label. */
    fun stats(rows: Collection<Row>): Map<String, Stat> =
        rows.filter { it.account.isNotBlank() }.groupBy { it.account }.mapValues { statOf(it.value) }

    /** EMIs per loan ("UNION Loan 0126"); a loan is named by its EMI messages, not by an account label. */
    fun loanStats(rows: Collection<Row>): Map<String, Stat> =
        rows.filter { it.category == "EMI" && LOAN_NAME.matches(it.counterparty ?: "") }.groupBy { it.counterparty!! }.mapValues { statOf(it.value) }

    /**
     * Every account with the labels joined into it.
     * [labels]: labels before anything you joined is applied. [autoParts]: label Zelu joined -> (account it joined, its payments).
     */
    fun build(
        labels: Map<String, Stat>, autoParts: Map<String, Pair<String, Stat>>, loans: Map<String, Stat>,
        registry: Map<String, Pair<String, String>>, balances: Map<String, Pair<Double, Long>>, limits: Map<String, Pair<Double, Long>>,
    ): List<Acct> {
        val dup = registry.filter { it.value.first == "duplicate" }.mapValues { it.value.second }
        val all = LinkedHashMap<String, Stat>(labels)
        for ((k, v) in loans) all.putIfAbsent(k, v)
        // An account that only has labels joined into it (no payments of its own) is still shown, with them under it.
        for ((k, t) in dup) if (all.containsKey(k) && !all.containsKey(t)) all[t] = Stat(0, "", 0.0, "")
        fun root(k: String): String {
            var c = k; var hops = 0
            while (hops++ < 8) { val t = dup[c] ?: break; if (t == c || !all.containsKey(t)) break; c = t }
            return c
        }
        val partsOf = HashMap<String, MutableList<Part>>()
        for (k in all.keys) { val r = root(k); if (r != k) partsOf.getOrPut(r) { ArrayList() }.add(Part(k, all.getValue(k), false)) }
        for ((raw, p) in autoParts) {
            if (all.containsKey(raw)) continue
            val r = root(p.first)
            if (all.containsKey(r)) partsOf.getOrPut(r) { ArrayList() }.add(Part(raw, p.second, true))
        }
        return all.keys.filter { root(it) == it }.map { k ->
            val reg = registry[k]
            val stat = all.getValue(k)
            val type = reg?.takeIf { it.first == "accepted" || it.first == "declined" }?.second?.takeIf { it.isNotBlank() }
                ?: guessType(k).let { g -> if (g == "savings") stat.looks ?: g else g }
            Acct(k, type, all.getValue(k), partsOf[k].orEmpty().sortedByDescending { it.stat.count },
                balances[k]?.first, limits[k]?.first ?: limits["$k#total"]?.first, reg?.first == "declined")
        }
    }

    /** True when one number is the end of the other ("883076" and "76"). */
    fun numberMatch(a: String, b: String): Boolean {
        val x = numberOf(a) ?: return false
        val y = numberOf(b) ?: return false
        return x.endsWith(y) || y.endsWith(x)
    }

    /**
     * Accounts that could be the same as [open]: the number matches, the same bank, or either has no bank name.
     * Cards are offered only with cards, and a loan only with labels of the same number. Best matches first.
     */
    fun similar(book: List<Acct>, open: Acct): List<Acct> {
        val ob = open.bank
        val cands = book.filter { it.key != open.key && !it.declined }
        val list = if (open.type == "debit") {
            cands.filter { it.type == "savings" && (ob == null || it.bank == null || it.bank == ob) }
        } else if (open.type == "loan") {
            cands.filter { it.type != "loan" && it.type != "card" && numberMatch(open.key, it.key) && (it.bank == null || it.bank == ob) }
        } else {
            // An FD or RD is never the same account as a savings account at that bank, unless the number says so.
            fun deposit(t: String) = t == "fd" || t == "rd"
            cands.filter { c ->
                c.type != "loan" && (c.type == "card") == (open.type == "card") &&
                    (numberMatch(open.key, c.key) || (deposit(c.type) == deposit(open.type) && (ob == null || c.bank == null || c.bank == ob)))
            }
        }
        fun rank(c: Acct) = when { numberMatch(open.key, c.key) -> 0; ob != null && c.bank == ob -> 1; else -> 2 }
        return list.sortedWith(compareBy<Acct> { rank(it) }.thenByDescending { it.count })
    }

    /** The savings account a debit card most likely belongs to: the only savings account at its bank, else none. */
    fun cardLink(book: List<Acct>, card: Acct): Acct? {
        if (card.type != "debit" || card.declined) return null
        val b = card.bank ?: return null
        return book.filter { it.type == "savings" && !it.declined && it.bank == b }.singleOrNull()
    }

    private val NETWORK = Regex("""\b(visa|master ?card|rupay|american express|amex|diners|maestro)\b""", RegexOption.IGNORE_CASE)

    /** The card network a message names, or null. */
    fun networkIn(raw: String): String? = when (NETWORK.find(raw)?.value?.lowercase()?.replace(" ", "")) {
        "visa" -> "Visa"; "mastercard" -> "Mastercard"; "rupay" -> "RuPay"
        "americanexpress", "amex" -> "Amex"; "diners" -> "Diners"; "maestro" -> "Maestro"
        else -> null
    }

    /** Credit card label -> network, from the newest message of that card that names one. */
    fun networks(rows: Collection<Row>): Map<String, String> =
        rows.filter { it.account.contains(" CC") }.sortedByDescending { it.date }.groupBy { it.account }
            .mapNotNull { (k, v) -> v.firstNotNullOfOrNull { networkIn(it.raw) }?.let { k to it } }.toMap()

    /** Which name an account keeps when several are joined: a loan, then one with a bank name, then savings, then most payments. */
    fun chooseMain(group: List<Acct>): Acct = group.sortedWith(
        compareByDescending<Acct> { if (it.type == "loan") 1 else 0 }
            .thenByDescending { if (it.bank != null) 1 else 0 }
            .thenByDescending { if (it.type == "savings") 1 else 0 }
            .thenByDescending { it.count }
            .thenBy { it.key.length }.thenBy { it.key },
    ).first()

    /**
     * Save on an account's sheet. [ticked]: the labels you ticked (parts kept, and other accounts to join).
     * [type]: the type picked on the sheet. Unticked parts become separate accounts that Zelu leaves apart.
     */
    fun plan(book: List<Acct>, registry: Map<String, Pair<String, String>>, openKey: String, ticked: Set<String>, type: String): Plan {
        val byKey = book.associateBy { it.key }
        val open = byKey[openKey] ?: return Plan(openKey, emptyMap(), emptySet(), emptySet())
        val reg = LinkedHashMap<String, Pair<String, String>>()
        val apartAdd = LinkedHashSet<String>()
        val apartRemove = LinkedHashSet<String>()
        for (p in open.parts) if (p.key !in ticked) { reg[p.key] = "accepted" to guessType(p.key); apartAdd.add(p.key) }
        val group = listOf(open) + ticked.mapNotNull { byKey[it] }.filter { it.key != openKey && !it.declined }
        val main = chooseMain(group)
        // The type picked on the sheet is for the account as a whole.
        val typeChanged = type != open.type
        val finalType = if (typeChanged) type else main.type
        val mainKey = if (typeChanged) relabel(main.key, finalType) else main.key
        if (typeChanged || registry[main.key]?.first != "accepted") reg[main.key] = "accepted" to finalType
        if (mainKey != main.key) {
            reg[mainKey] = "accepted" to finalType
            for ((k, v) in registry) if (v.first == "duplicate" && v.second == main.key && k !in reg) reg[k] = "duplicate" to mainKey
        }
        for (m in group) {
            if (m.key == main.key) continue
            reg[m.key] = "duplicate" to mainKey
            apartRemove.add(m.key)
            // Its own parts move along (an auto part follows the label it was joined to, so it needs nothing).
            for (p in m.parts) if (!p.auto && (m.key != openKey || p.key in ticked)) reg[p.key] = "duplicate" to mainKey
        }
        for (p in open.parts) if (p.key in ticked) apartRemove.add(p.key)
        return Plan(mainKey, reg, apartAdd, apartRemove)
    }

    /** Continue on the screen: every account shown and not removed is yours, so Zelu stops asking about it. */
    fun confirmPlan(book: List<Acct>, registry: Map<String, Pair<String, String>>): Map<String, Pair<String, String>> =
        book.filter { !it.declined && it.key !in registry }.associate { it.key to ("accepted" to it.type) }

    /** Accounts in screen order: banks with the most payments first, then labels with no bank name. */
    fun ordered(book: List<Acct>): Pair<List<Acct>, List<Acct>> {
        val vis = book.filter { !it.declined }
        val known = vis.filter { it.bank != null }
        val unknown = vis.filter { it.bank == null }.sortedByDescending { it.count }
        val bankPays = known.groupBy { it.bank!! }.mapValues { e -> e.value.sumOf { it.count } }
        val typeOrder = listOf("savings", "debit", "card", "loan", "fd", "rd", "other")
        fun ti(t: String) = typeOrder.indexOf(t).let { if (it < 0) typeOrder.size else it }
        return known.sortedWith(
            compareByDescending<Acct> { bankPays[it.bank] ?: 0 }.thenBy { it.bank }.thenBy { ti(it.type) }.thenByDescending { it.count },
        ) to unknown
    }
}
