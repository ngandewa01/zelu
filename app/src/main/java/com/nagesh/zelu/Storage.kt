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

    // Bump when parser/classifier changes so history rebuilds on next open.
    const val PARSER_VERSION = 8

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
        val rows = base.values.map { r ->
            var row = r
            val payeeKey = (r.counterparty ?: "").lowercase().trim()
            if (payeeKey.isNotEmpty() && rules.containsKey(payeeKey)) {
                val (k, c) = rules.getValue(payeeKey)
                row = row.copy(kind = k, category = c)
            }
            overrides[r.id]?.let { (k, c) -> row = row.copy(kind = k, category = c) }
            row
        }.sortedByDescending { it.date }
        dedupBillers(rows)
    }

    // Collapse duplicates:
    //  - Billers (e.g. Jio) send two confirmation SMS for one payment: same
    //    date + amount + known merchant + direction.
    //  - Loan EMIs arrive as repeated NACH notices: keep one per month per loan.
    private fun dedupBillers(rows: List<Row>): List<Row> {
        val seenBrand = HashSet<String>()
        val seenEmi = HashSet<String>()
        return rows.filter { r ->
            val cp = r.counterparty
            when {
                r.category == "EMI" && cp != null ->
                    seenEmi.add("${r.date.take(7)}|$cp|${r.amount}")
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

    fun setOverride(context: Context, id: String, kind: String, category: String) = synchronized(lock) {
        val kv = readKv(context, OVERRIDE_FILE).toMutableMap()
        kv[id] = kind to category
        writeKv(context, OVERRIDE_FILE, kv)
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
        file(context, TXN_FILE).delete(); Unit
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
