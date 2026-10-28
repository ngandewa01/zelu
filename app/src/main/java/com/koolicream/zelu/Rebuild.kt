package com.koolicream.zelu

// A rebuild (a new parser version, or "Read everything again") reads every message afresh. A payment, AutoPay or bill
// whose message is no longer in the inbox (deleted, older than the read, or brought from another phone in a backup) is
// read again from the copy of the message stored with it, so a rebuild never loses it.
object Rebuild {
    private val WS = Regex("""\s+""")

    /** messages: stored copies to read along with the inbox. keep: payments to keep exactly as stored. */
    class Plan(val messages: List<RawSms>, val keep: List<Row>)

    fun plan(inbox: List<RawSms>, rows: Collection<Row>, mandates: Collection<Mandate>, reminders: Collection<Reminder>): Plan {
        val inInbox = HashSet<String>()
        for (m in inbox) { inInbox.add(Parser.normalize(m.body)); inInbox.add(WS.replace(m.body, " ").trim()) }
        val messages = ArrayList<RawSms>()
        val keep = ArrayList<Row>()
        val seen = HashSet<String>()
        fun archive(source: String, text: String, ms: Long) {
            if (text.isBlank() || text in inInbox || !seen.add(source.uppercase() + "|" + text)) return
            messages.add(RawSms(source, text, ms))
        }
        for (r in rows) {
            if (r.raw in inInbox) continue
            // A payment read with a shape you taught is named after its original message, which is gone: kept as it is.
            if (r.id.startsWith("man-")) { keep.add(r); continue }
            archive(r.source, r.raw, if (r.ms > 0) r.ms else noonOf(r.date))
        }
        for (m in mandates) archive(m.source, m.raw, m.lastMs)
        // A bill line made from an AutoPay comes back from that AutoPay; one you added yourself is stored elsewhere.
        for (r in reminders) if (!r.id.startsWith("mand-") && r.source != "manual") archive(r.source, r.raw, noonOf(r.dueDate))
        return Plan(messages.sortedByDescending { it.date }, keep)
    }

    private fun noonOf(date: String): Long = try { Dates.parse(date).timeInMillis + 12 * 3_600_000L } catch (_: Exception) { 0L }

    private val OLD_CREDIT_ID = Regex("""^ref-\d+$""")

    /** Stored credits that still carry a reference ID without the credit mark: old ID -> new ID. */
    fun creditRenames(rows: Collection<Row>): Map<String, String> =
        rows.filter { it.direction == "credit" && OLD_CREDIT_ID.matches(it.id) }.associate { it.id to it.id + Parser.CREDIT_ID_SUFFIX }

    /** A key naming payments ("id", a split part "id#2", a pair "id1|id2") with the renamed IDs. */
    fun renameKey(key: String, renames: Map<String, String>): String =
        key.split('|').joinToString("|") { part ->
            val base = part.substringBefore('#')
            renames[base]?.let { it + part.substring(base.length) } ?: part
        }
}
