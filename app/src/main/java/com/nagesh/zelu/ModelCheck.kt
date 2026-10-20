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
        "reminder" to "Reminder, not paid yet", "ignore" to "Not a payment",
    )

    class Item(val key: String, val sender: String, val date: Long, val body: String, val rules: String, val model: String, val conf: Double)
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

    /** What Zelu's rules make of a message, in the same five kinds. */
    fun rulesLabel(body: String, date: Long, address: String): String {
        val t = Parser.parseSms(body, date, address)
        return when {
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
        val differ = left.filter { it.rules != it.model }.sortedByDescending { it.date }
        val sample = left.filter { it.rules == it.model }.shuffled(java.util.Random(items.size.toLong())).take(25)
        return Result(differ, sample, items.size, items.count { it.rules == it.model })
    }

    /** You say what the message really is: it is counted, and the model learns from it. */
    @Synchronized
    fun answer(c: Context, it: Item, truth: String, mode: String) {
        verdictFile(c).appendText("${it.key}\t$truth\t${it.rules}\t${it.model}\t$mode\n")
        val tk = SmsModel.tokens(it.body)
        learnFile(c).appendText("$truth\t${tk.joinToString(" ")}\n")
        model(c).learnTokens(tk, truth)
    }

    /** Honest score = random sample (both agree or not, picked without looking); the other is only where they differ. */
    fun tally(c: Context): Pair<Tally, Tally> {
        val rows = verdictFile(c).takeIf { it.exists() }?.readLines()?.map { it.split('\t') }?.filter { it.size >= 5 } ?: emptyList()
        fun t(mode: String) = rows.filter { it[4] == mode }.let { r -> Tally(r.size, r.count { it[1] == it[2] }, r.count { it[1] == it[3] }) }
        return t("sample") to t("differ")
    }
}
