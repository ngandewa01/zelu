package com.koolicream.zelu

// Search for the menu that forgives typos ("notifcation") and knows everyday words ("pin" finds App lock,
// "excel" finds Export). Pure, so it is unit tested.
object MenuSearch {
    // Everyday word -> words the menu itself uses.
    private val SYNONYMS: Map<String, List<String>> = mapOf(
        "pin" to listOf("lock"), "password" to listOf("lock", "password"), "fingerprint" to listOf("lock"),
        "security" to listOf("lock"), "secure" to listOf("lock"), "biometric" to listOf("lock"),
        "delete" to listOf("erase"), "wipe" to listOf("erase"), "clear" to listOf("erase"),
        "excel" to listOf("export", "csv"), "sheet" to listOf("export", "csv"), "spreadsheet" to listOf("export", "csv"),
        "download" to listOf("export"),
        "salary" to listOf("income", "categories"), "earning" to listOf("income"),
        "notification" to listOf("alerts", "alert", "summary", "reminders"), "notify" to listOf("alerts", "alert"),
        "night" to listOf("dark"), "nite" to listOf("dark"), "colour" to listOf("theme", "colours"), "color" to listOf("theme", "colours"),
        "sms" to listOf("messages"), "inbox" to listOf("messages"),
        "remind" to listOf("reminders", "remind"), "due" to listOf("bill", "reminders"), "dues" to listOf("bill", "reminders"),
        "limit" to listOf("budget"), "sip" to listOf("savings", "investments"), "mutual" to listOf("investments"),
        "refresh" to listOf("check"), "sync" to listOf("check"), "scan" to listOf("check", "read"), "update" to listOf("check"),
        "import" to listOf("restore"), "save" to listOf("back"), "backup" to listOf("back", "backup"),
        "contact" to listOf("report"), "bug" to listOf("report"), "feedback" to listOf("report"),
        "battery" to listOf("running"), "demo" to listOf("sample"),
    )
    private val FILLER = setOf("a", "an", "the", "at", "in", "on", "of", "to", "for", "my", "me", "i", "and", "how", "do", "is")

    fun tokens(query: String): List<String> =
        query.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() && it !in FILLER }

    /** How many of the query's words this text matches (0 = none). */
    fun score(tokens: List<String>, text: String): Int {
        val words = text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        val joined = words.joinToString(" ")
        return tokens.count { t -> candidates(t).any { c -> matches(c, words, joined) } }
    }

    // The word itself, plus what it stands for, also when it is misspelt ("notifcation" -> notification's words).
    private fun candidates(t: String): Set<String> {
        val out = mutableSetOf(t)
        SYNONYMS.forEach { (k, v) -> if (close(t, k)) { out += k; out += v } }
        return out
    }

    private fun matches(t: String, words: List<String>, joined: String): Boolean {
        if (t.length >= 3 && joined.contains(t)) return true
        return words.any { w -> w == t || (t.length >= 3 && w.startsWith(t)) || close(t, w) ||
            (t.length >= 4 && w.length > t.length && close(t, w.take(t.length))) }
    }

    // Close enough to be a typo: one slip in a short word, two in a long one.
    private fun close(a: String, b: String): Boolean {
        if (a == b) return true
        val allowed = when { a.length <= 3 -> 0; a.length <= 6 -> 1; else -> 2 }
        if (allowed == 0 || kotlin.math.abs(a.length - b.length) > allowed) return false
        return distance(a, b) <= allowed
    }

    private fun distance(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1)
            cur[0] = i
            for (j in 1..b.length) {
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            }
            prev = cur
        }
        return prev[b.length]
    }
}
