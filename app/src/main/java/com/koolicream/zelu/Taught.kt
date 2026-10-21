package com.koolicream.zelu

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// Message shapes you taught Zelu from the Missed SMS list ("Add as payment" with "same for similar messages").
// A shape is the message with its numbers blanked and the payee cut out, plus the words that sat either side
// of the payee, so later messages of the same form are read the same way.
object Taught {
    data class Pattern(val sender: String, val sig: String, val direction: String, val left: String, val right: String)

    private const val FILE = "taught.json"
    private val WS = Regex("\\s+")
    private val DIGITS = Regex("[0-9][0-9,.:/\\-]*")

    private fun senderKey(address: String): String {
        val u = address.uppercase()
        return if (u.contains('-')) u.substringAfter('-') else u
    }

    private fun norm(s: String): String = DIGITS.replace(s.lowercase(), "#")

    fun load(context: Context): List<Pattern> {
        val f = java.io.File(context.filesDir, FILE)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            val out = ArrayList<Pattern>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(Pattern(o.getString("s"), o.getString("sig"), o.getString("dir"), o.optString("l"), o.optString("r")))
            }
            out
        } catch (_: Exception) { emptyList() }
    }

    private fun save(context: Context, list: List<Pattern>) {
        val arr = JSONArray()
        for (p in list) arr.put(JSONObject().put("s", p.sender).put("sig", p.sig).put("dir", p.direction).put("l", p.left).put("r", p.right))
        java.io.File(context.filesDir, FILE).writeAtomic(arr.toString())
    }

    @Synchronized
    fun learn(context: Context, body: String, address: String, payee: String?, direction: String) {
        val text = WS.replace(body, " ").trim()
        var left = ""
        var right = ""
        var shaped = text
        val p = payee?.trim().orEmpty()
        if (p.length >= 2) {
            val i = text.indexOf(p, ignoreCase = true)
            if (i >= 0) {
                left = text.substring(0, i).trimEnd().split(" ").takeLast(2).joinToString(" ").lowercase()
                right = text.substring(i + p.length).trimStart().substringBefore(" ").lowercase()
                shaped = text.substring(0, i) + "<p>" + text.substring(i + p.length)
            }
        }
        val pat = Pattern(senderKey(address), norm(shaped), direction, left, right)
        val cur = load(context)
        if (cur.none { it.sender == pat.sender && it.sig == pat.sig }) save(context, cur + pat)
    }

    // The payment a taught shape reads out of this message, or null when none of them fit.
    fun apply(patterns: List<Pattern>, msg: RawSms): Txn? {
        if (patterns.isEmpty()) return null
        val key = senderKey(msg.address)
        val text = WS.replace(msg.body, " ").trim()
        val lower = text.lowercase()
        for (t in patterns) {
            if (t.sender != key) continue
            var payee: String? = null
            var shaped = text
            if (t.left.isNotEmpty()) {
                val i = lower.indexOf(t.left)
                if (i < 0) continue
                var s = i + t.left.length
                while (s < text.length && text[s] == ' ') s++
                val e = if (t.right.isNotEmpty()) lower.indexOf(t.right, s) else -1
                if (e <= s || e - s > 90) continue
                var pe = e
                while (pe > s && text[pe - 1] == ' ') pe--
                payee = text.substring(s, pe).trim()
                shaped = text.substring(0, s) + "<p>" + text.substring(pe)
            }
            if (norm(shaped) != t.sig) continue
            val amount = Parser.firstAmount(msg.body) ?: continue
            val id = "man-" + Ingest.smsKey(msg).hashCode().toUInt().toString(16)
            return Txn(id, Dates.fmtMs(msg.date), amount, t.direction, payee?.takeIf { it.isNotBlank() }, null, text, msg.address,
                Parser.accountFor(msg.body, msg.address, t.direction), msg.date)
        }
        return null
    }
}
