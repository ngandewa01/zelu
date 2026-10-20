package com.nagesh.zelu

import android.content.Context
import java.io.File

/**
 * Lab only. Runs the small learning model next to Zelu's rules on your own messages, keeps what you say each message
 * really is, and counts which of the two was right. Nothing here changes any payment, balance or account.
 */
object ModelCheck {
    val NAMES = mapOf(
        "spend" to "Money spent", "credit" to "Money received", "fee" to "Bank fee",
        "reminder" to "Reminder, not paid yet", "ignore" to "Not a payment", "cardbill" to "Card bill payment",
    )

    /** The answers you can give: the model's five kinds, plus a card bill payment (money moving between your own accounts). */
    val CHOICES = SmsModel.CLASSES + "cardbill"

    /**
     * The model has no card-bill kind: card-bill messages use the same words as ordinary payments, and a sixth kind
     * pulled dozens of ordinary payments into it. So for the model a card bill payment is its money side: money
     * arriving on the card is money received, money leaving the bank account is money spent.
     */
    private val CARD_SIDE = Regex("""\b(?:credited to your (?:credit )?card|received (?:on|towards) your\b.{0,40}\bcard|payment (?:received|credited)\b.{0,60}\bcard|towards your\b.{0,40}\bcredit card\b.{0,40}\breceived)""", RegexOption.IGNORE_CASE)
    fun cardSide(body: String, date: Long, address: String): String {
        val t = Parser.parseSms(body, date, address)
        return if (t != null) (if (t.direction == "credit") "credit" else "spend")
            else if (CARD_SIDE.containsMatchIn(body)) "credit" else "spend"
    }

    class Item(val key: String, val sender: String, val date: Long, val body: String, val rules: String, val model: String, val conf: Double) {
        /** The rules' answer in the model's kinds: a card bill payment counts as its money side. */
        val rulesSide: String get() = if (rules == "cardbill") cardSide(body, date, sender) else rules
    }
    class Result(val differ: List<Item>, val sample: List<Item>, val scanned: Int, val agree: Int)
    class Tally(val n: Int, val rulesRight: Int, val modelRight: Int)

    private var cached: SmsModel? = null
    private fun learnFile(c: Context) = File(c.filesDir, "model_learn.tsv")
    private fun verdictFile(c: Context) = File(c.filesDir, "model_verdicts.tsv")

    @Synchronized
    private fun model(c: Context): SmsModel = cached ?: run {
        val m = SmsModel.parse(c.assets.open("sms_model.tsv").bufferedReader().use { it.readText() })
        runCatching { learnFile(c).takeIf { it.exists() }?.forEachLine { l ->
            val p = l.split('\t'); if (p.size == 2) m.learnTokens(p[1].split(' ').filter { it.isNotEmpty() }, p[0])
        } }
        m.also { cached = it }
    }

    /** What Zelu's rules make of a message: the model's five kinds, or a card bill payment. */
    fun rulesLabel(body: String, date: Long, address: String): String {
        val t = Parser.parseSms(body, date, address)
        return when {
            t != null && Classifier.classify(t, emptyList()).category == "Card bill" -> "cardbill"
            t != null -> if (t.direction == "credit") "credit" else "spend"
            Parser.parseReminder(body, date, address) != null -> "reminder"
            else -> "ignore"
        }
    }

    private fun answered(c: Context): Set<String> =
        verdictFile(c).takeIf { it.exists() }?.readLines()?.map { it.substringBefore('\t') }?.toSet() ?: emptySet()

    fun scan(c: Context): Result {
        Storage.loadTrust(c)
        val done = answered(c)
        val m = model(c)
        val all = try { SmsReader.readInbox(c, 4000) } catch (_: Exception) { emptyList() }
        val items = ArrayList<Item>()
        for (s in all) {
            if (!Parser.isFinancialSender(s.address) || Parser.hasSuspiciousLink(s.body, s.address)) continue
            val (label, conf) = m.best(s.body)
            items.add(Item(Ingest.smsKey(s), s.address, s.date, s.body, rulesLabel(s.body, s.date, s.address), label, conf))
        }
        val left = items.filter { it.key !in done }
        val differ = left.filter { it.rulesSide != it.model }.sortedByDescending { it.date }
        val sample = left.filter { it.rulesSide == it.model }.shuffled(java.util.Random(items.size.toLong())).take(25)
        return Result(differ, sample, items.size, items.count { it.rulesSide == it.model })
    }

    /** You say what the message really is: it is counted, and the model learns from it. */
    @Synchronized
    fun answer(c: Context, it: Item, truth: String, mode: String) {
        // For a card bill payment the model learns, and is judged on, its money side.
        val learn = if (truth == "cardbill") cardSide(it.body, it.date, it.sender) else truth
        verdictFile(c).appendText("${it.key}\t$truth\t${it.rules}\t${it.model}\t$mode\t$learn\n")
        val tk = SmsModel.tokens(it.body)
        learnFile(c).appendText("$learn\t${tk.joinToString(" ")}\n")
        model(c).learnTokens(tk, learn)
    }

    /** Honest score = random sample (both agree or not, picked without looking); the other is only where they differ. */
    fun tally(c: Context): Pair<Tally, Tally> {
        val rows = verdictFile(c).takeIf { it.exists() }?.readLines()?.map { it.split('\t') }?.filter { it.size >= 5 } ?: emptyList()
        // Rules are right on a card bill payment only when they file it as one; the model, when it gets its money side.
        fun t(mode: String) = rows.filter { it[4] == mode }.let { r ->
            Tally(r.size, r.count { it[1] == it[2] }, r.count { (it.getOrNull(5) ?: it[1]) == it[3] })
        }
        return t("sample") to t("differ")
    }
}
