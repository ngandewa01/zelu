package com.nagesh.zelu

import kotlin.math.exp
import kotlin.math.ln

/**
 * A small Naive Bayes that says what kind of SMS a message is (spend, credit, fee, reminder, ignore).
 * Counts come from a plain text file; your answers on the Model check screen add to them. No Android code here.
 */
class SmsModel(private val docs: IntArray, private val counts: HashMap<String, IntArray>) {
    companion object {
        val CLASSES = listOf("spend", "credit", "fee", "reminder", "ignore")
        private const val ALPHA = 0.5
        private val LINK = Regex("""https?://\S+|\b\w+\.(com|in|io)/\S*""")
        private val PHONE = Regex("""\b1800\d*\w*""")
        private val NUM = Regex("""x{1,}\d+|\*\d+|\b\d[\d,.]*\b""")
        private val OTHER = Regex("""[^a-z@% ]""")

        /** Words and word pairs; numbers, account digits, links and phone numbers become placeholders. */
        fun tokens(text: String): List<String> {
            var t = text.lowercase()
            t = LINK.replace(t, " url ")
            t = PHONE.replace(t, " phone ")
            t = NUM.replace(t, " num ")
            t = OTHER.replace(t, " ")
            val w = t.split(' ').filter { it.isNotEmpty() }
            return w + w.zipWithNext { a, b -> a + "_" + b }
        }

        /** Reads the "#docs\tn\tn..." line then "token\tn\tn..." lines. */
        fun parse(text: String): SmsModel {
            var docs = IntArray(CLASSES.size)
            val counts = HashMap<String, IntArray>()
            for (line in text.lineSequence()) {
                if (line.isBlank()) continue
                val p = line.split('\t')
                if (p.size < CLASSES.size + 1) continue
                val nums = IntArray(CLASSES.size) { p[it + 1].toIntOrNull() ?: 0 }
                if (p[0] == "#docs") docs = nums else counts[p[0]] = nums
            }
            return SmsModel(docs, counts)
        }
    }

    private val totals = IntArray(CLASSES.size)
    init { for (v in counts.values) for (i in totals.indices) totals[i] += v[i] }

    /** Probability of each class for this message. */
    fun predict(text: String): Map<String, Double> {
        val bag = HashMap<String, Int>()
        for (t in tokens(text)) bag[t] = (bag[t] ?: 0) + 1
        val v = counts.size.toDouble()
        val n = docs.sum().toDouble().coerceAtLeast(1.0)
        val lj = DoubleArray(CLASSES.size) { c ->
            var s = ln((docs[c].toDouble().coerceAtLeast(1.0)) / n)
            for ((t, k) in bag) { val row = counts[t] ?: continue; s += k * ln((row[c] + ALPHA) / (totals[c] + ALPHA * v)) }
            s
        }
        val m = lj.max()
        val e = DoubleArray(lj.size) { exp(lj[it] - m) }
        val z = e.sum()
        return CLASSES.indices.associate { CLASSES[it] to e[it] / z }
    }

    fun best(text: String): Pair<String, Double> = predict(text).maxByOrNull { it.value }!!.let { it.key to it.value }

    /** Your answer teaches it: this message's words count towards [label]. */
    fun learn(text: String, label: String) = learnTokens(tokens(text), label)

    fun learnTokens(tk: List<String>, label: String) {
        val c = CLASSES.indexOf(label); if (c < 0) return
        docs[c]++
        for (t in tk) { val row = counts.getOrPut(t) { IntArray(CLASSES.size) }; row[c]++; totals[c]++ }
    }
}
