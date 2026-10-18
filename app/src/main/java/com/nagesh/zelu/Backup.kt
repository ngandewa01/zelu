package com.nagesh.zelu

import android.content.Context
import org.json.JSONObject

// One-file backup of everything you have taught or saved in Zelu (transactions, categories, payee rules,
// budgets, plan, refund answers, settings, theme), so a new phone or a reinstall doesn't lose it.
object Backup {
    private val PREF_FILES = listOf("zelu_theme", "zelu_prefs")

    fun export(context: Context): String {
        val files = JSONObject()
        context.filesDir.listFiles()?.filter { it.isFile && (it.name.endsWith(".json") || it.name.endsWith(".txt")) }
            ?.forEach { files.put(it.name, it.readText()) }
        val prefs = JSONObject()
        for (name in PREF_FILES) {
            val o = JSONObject()
            for ((k, v) in context.getSharedPreferences(name, Context.MODE_PRIVATE).all) {
                val t = when (v) { is Boolean -> "b"; is Int -> "i"; is Long -> "l"; is Float -> "f"; is String -> "s"; else -> null }
                if (t != null) o.put(k, JSONObject().put("t", t).put("v", v))
            }
            prefs.put(name, o)
        }
        return JSONObject().put("app", "zelu").put("version", 1).put("files", files).put("prefs", prefs).toString()
    }

    // Returns false (and changes nothing) when the file isn't a Zelu backup.
    fun restore(context: Context, text: String): Boolean {
        val root = try { JSONObject(text) } catch (_: Exception) { return false }
        if (root.optString("app") != "zelu") return false
        val files = root.optJSONObject("files") ?: return false
        val names = files.keys().asSequence().toList()
        if (names.any { it.contains('/') || it.contains("..") || !(it.endsWith(".json") || it.endsWith(".txt")) }) return false
        try {
            for (n in names) java.io.File(context.filesDir, n).writeText(files.getString(n))
            val prefs = root.optJSONObject("prefs")
            if (prefs != null) for (name in PREF_FILES) {
                val o = prefs.optJSONObject(name) ?: continue
                val e = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit()
                for (k in o.keys()) {
                    val item = o.getJSONObject(k)
                    when (item.optString("t")) {
                        "b" -> e.putBoolean(k, item.getBoolean("v"))
                        "i" -> e.putInt(k, item.getInt("v"))
                        "l" -> e.putLong(k, item.getLong("v"))
                        "f" -> e.putFloat(k, item.getDouble("v").toFloat())
                        "s" -> e.putString(k, item.getString("v"))
                    }
                }
                e.apply()
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    // Once a week the daily check also saves a backup file to Downloads/Zelu (can be switched off in the menu).
    fun autoOn(context: Context): Boolean =
        context.getSharedPreferences("zelu_backup", Context.MODE_PRIVATE).getBoolean("on", true)

    fun setAutoOn(context: Context, on: Boolean) {
        context.getSharedPreferences("zelu_backup", Context.MODE_PRIVATE).edit().putBoolean("on", on).apply()
    }

    fun lastAuto(context: Context): Long =
        context.getSharedPreferences("zelu_backup", Context.MODE_PRIVATE).getLong("last", 0L)

    fun lastManual(context: Context): Long =
        context.getSharedPreferences("zelu_backup", Context.MODE_PRIVATE).getLong("manual", 0L)

    fun markManual(context: Context) {
        context.getSharedPreferences("zelu_backup", Context.MODE_PRIVATE).edit().putLong("manual", System.currentTimeMillis()).apply()
    }

    fun lastAny(context: Context): Long = maxOf(lastAuto(context), lastManual(context))

    fun maybeAuto(context: Context) {
        if (!autoOn(context)) return
        val now = System.currentTimeMillis()
        if (now - lastAuto(context) < 7L * 86_400_000L) return
        if (Storage.loadRows(context).isEmpty()) return
        val ok = Exporter.exportText(context, "zelu-backup-" + Dates.today() + ".json", "application/json", export(context))
        if (ok) context.getSharedPreferences("zelu_backup", Context.MODE_PRIVATE).edit().putLong("last", now).apply()
    }
}
