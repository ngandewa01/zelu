package com.koolicream.zelu

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

// 200 audited SMS (UPI, cards, credits, bills, dues, regional banks, scams and traps) put through the same steps as
// Ingest: sender check, link check, parse, reminder, classify. For each one the outcome (payment / reminder / ignored),
// direction, amount and category must match. A foreign spend with no rupee figure passes when the foreign amount matches.
class SmsEval200Test {
    private data class Case(val id: String, val sender: String, val outcome: String, val dir: String, val amt: Double?,
                            val fx: Boolean, val cats: Set<String>, val body: String)

    private fun cases(): List<Case> {
        val s = javaClass.classLoader!!.getResourceAsStream("sms_eval_200.tsv") ?: java.io.File("src/test/resources/sms_eval_200.tsv").inputStream()
        return s.bufferedReader(Charsets.UTF_8).readLines().filter { it.isNotBlank() && !it.startsWith("#") }.map { l ->
            val p = l.split('\t')
            Case(p[0], p[1], p[2], p[3], p[4].toDoubleOrNull(), p[5] == "1", p[6].split('|').filter { it.isNotEmpty() }.toSet(),
                unescape(p[7]))
        }
    }

    // The file writes a newline inside a message as a backslash followed by n, and a backslash as two.
    private fun unescape(s: String): String {
        val sb = StringBuilder(); var i = 0
        while (i < s.length) {
            val ch = s[i]
            if (ch == '\\' && i + 1 < s.length) { sb.append(if (s[i + 1] == 'n') '\n' else s[i + 1]); i += 2 } else { sb.append(ch); i++ }
        }
        return sb.toString()
    }

    private fun check(c: Case): String? {
        val ms = 1792305000000L
        val sender = Parser.isFinancialSender(c.sender)
        val link = sender && Parser.hasSuspiciousLink(c.body, c.sender)
        val t = if (sender && !link) Parser.parseSms(c.body, ms, c.sender) else null
        val rem = if (sender && !link && t == null) Parser.parseReminder(c.body, ms, c.sender) else null
        val got = when { t != null -> "txn"; rem != null -> "reminder"; else -> "ignore" }
        if (got != c.outcome) return "outcome: want ${c.outcome}, got $got"
        if (rem != null && c.amt != null && abs(rem.amount - c.amt) > 0.01) return "reminder amount: want ${c.amt}, got ${rem.amount}"
        if (t == null) return null
        if (t.direction != c.dir) return "direction: want ${c.dir}, got ${t.direction}"
        val fxOk = c.fx && c.amt != null && Parser.foreign(c.body)?.let { abs(it.amount - c.amt) < 0.01 } == true
        if (c.amt != null && abs(t.amount - c.amt) > 0.01 && !fxOk) return "amount: want ${c.amt}, got ${t.amount}"
        val r = Classifier.classify(t, emptyList())
        if (c.cats.isNotEmpty() && r.category !in c.cats) return "category: want ${c.cats}, got ${r.kind}/${r.category}"
        return null
    }

    // Messages known to be misread, listed rather than hidden. None at present.
    private val known = emptySet<String>()

    @Test
    fun all200AreReadCorrectly() {
        val all = cases()
        assertTrue("expected 200 cases, found ${all.size}", all.size == 200)
        val bad = all.mapNotNull { c -> check(c)?.let { c.id to it } }
        val unexpected = bad.filter { it.first !in known }
        assertTrue("${unexpected.size} misread:\n" + unexpected.joinToString("\n") { "${it.first}: ${it.second}" }, unexpected.isEmpty())
        val fixed = known - bad.map { it.first }.toSet()
        assertTrue("now read correctly, remove from the known list: $fixed", fixed.isEmpty())
    }
}
