package com.nagesh.zelu

import android.content.Context
import org.json.JSONObject
import java.io.File

object Storage {

    private val lock = Any()
    private const val PREFS = "zelu_prefs"
    private const val KEY_SENDERS = "bank_senders"
    private const val KEY_OWN = "own_accounts"
    private const val TXN_FILE = "transactions.json"

    data class Settings(val bankSenders: String, val ownAccounts: String)

    fun loadSettings(context: Context): Settings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Settings(
            p.getString(KEY_SENDERS, "") ?: "",
            p.getString(KEY_OWN, "") ?: "",
        )
    }

    fun saveSettings(context: Context, bankSenders: String, ownAccounts: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SENDERS, bankSenders)
            .putString(KEY_OWN, ownAccounts)
            .apply()
    }

    fun loadRows(context: Context): List<Row> = synchronized(lock) {
        readMap(context).values.sortedByDescending { it.date }
    }

    // Returns how many rows were new. Existing ids are kept, so repeat imports are safe.
    fun saveRows(context: Context, newRows: List<Row>): Int = synchronized(lock) {
        val map = readMap(context).toMutableMap()
        var added = 0
        for (r in newRows) {
            if (!map.containsKey(r.id)) {
                map[r.id] = r
                added++
            }
        }
        writeMap(context, map)
        added
    }

    fun splitList(s: String): List<String> =
        s.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    // Deletes all stored transactions (settings are kept). Used to rebuild
    // history after the parser improves.
    fun clear(context: Context) = synchronized(lock) {
        file(context).delete()
        Unit
    }

    private fun file(context: Context) = File(context.filesDir, TXN_FILE)

    private fun readMap(context: Context): Map<String, Row> {
        val f = file(context)
        if (!f.exists()) return emptyMap()
        return try {
            val obj = JSONObject(f.readText())
            val out = HashMap<String, Row>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = rowFromJson(obj.getJSONObject(k))
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun writeMap(context: Context, map: Map<String, Row>) {
        val obj = JSONObject()
        for ((k, r) in map) obj.put(k, rowToJson(r))
        file(context).writeText(obj.toString())
    }

    private fun rowToJson(r: Row): JSONObject = JSONObject().apply {
        put("id", r.id)
        put("date", r.date)
        put("amount", r.amount)
        put("direction", r.direction)
        put("counterparty", r.counterparty ?: JSONObject.NULL)
        put("ref", r.ref ?: JSONObject.NULL)
        put("raw", r.raw)
        put("source", r.source)
        put("kind", r.kind)
        put("category", r.category)
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
        kind = o.getString("kind"),
        category = o.getString("category"),
    )
}
