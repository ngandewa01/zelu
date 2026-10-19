package com.nagesh.zelu

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * One optional "Sponsored" card on Home. The text and link come from a small JSON file the app owner hosts
 * (BuildConfig.SPONSOR_URL). Downloading it sends no identifier, cookie or personal data. No ad network is involved.
 */
object Sponsor {
    class Card(val id: String, val title: String, val body: String, val cta: String, val url: String)

    private const val FILE = "sponsor_cache.json"
    private const val REFRESH_MS = 12 * 3_600_000L
    private const val QUIET_DAYS = 7L          // nothing is shown during the first week after install
    private const val MAX_BYTES = 16_384

    private fun prefs(context: Context) = context.getSharedPreferences("zelu_sponsor", Context.MODE_PRIVATE)

    /** The card to show now, from the saved copy of the file; null when there is none, it is hidden, or it is too soon. */
    fun current(context: Context): Card? {
        if (BuildConfig.SPONSOR_URL.isBlank()) return null
        val installed = try { context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime } catch (_: Exception) { 0L }
        if (installed > 0 && System.currentTimeMillis() - installed < QUIET_DAYS * 86_400_000L) return null
        val text = try { java.io.File(context.filesDir, FILE).takeIf { it.exists() }?.readText() } catch (_: Exception) { null } ?: return null
        return pick(text, Dates.today(), prefs(context).getStringSet("hidden", emptySet()) ?: emptySet())
    }

    /** Downloads the file if the saved copy is older than 12 hours. Quietly does nothing on any problem. Call off the main thread. */
    fun refresh(context: Context) {
        val address = BuildConfig.SPONSOR_URL
        if (!address.startsWith("https://")) return
        val p = prefs(context)
        val now = System.currentTimeMillis()
        if (now - p.getLong("fetched", 0L) < REFRESH_MS) return
        try {
            val c = URL(address).openConnection() as HttpURLConnection
            c.connectTimeout = 5_000; c.readTimeout = 5_000
            c.instanceFollowRedirects = true
            c.useCaches = false
            c.setRequestProperty("User-Agent", "Zelu")
            if (c.responseCode != 200) { c.disconnect(); return }
            val bytes = c.inputStream.use { it.readBytes() }
            c.disconnect()
            if (bytes.size > MAX_BYTES) return
            val text = String(bytes, Charsets.UTF_8)
            JSONObject(text)                                   // must be valid before it replaces the saved copy
            java.io.File(context.filesDir, FILE).writeAtomic(text)
            p.edit().putLong("fetched", now).apply()
        } catch (_: Exception) { }
    }

    fun hide(context: Context, id: String) {
        val p = prefs(context)
        val set = (p.getStringSet("hidden", emptySet()) ?: emptySet()).toMutableSet().apply { add(id) }
        p.edit().putStringSet("hidden", set).apply()
    }

    fun open(context: Context, card: Card) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(card.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { }
    }

    /** First card in the file that is switched on, inside its dates, not hidden, and has a safe link. */
    internal fun pick(text: String, today: String, hidden: Set<String>): Card? = try {
        val arr = JSONObject(text).optJSONArray("cards")
        var found: Card? = null
        if (arr != null) for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id").take(60)
            val url = o.optString("url")
            val title = o.optString("title").trim().take(60)
            if (id.isBlank() || title.isBlank() || !o.optBoolean("enabled", true) || id in hidden || !url.startsWith("https://")) continue
            val from = o.optString("from"); val until = o.optString("until")
            if (from.isNotBlank() && today < from) continue
            if (until.isNotBlank() && today > until) continue
            found = Card(id, title, o.optString("body").trim().take(140), o.optString("cta").trim().take(24).ifBlank { "Learn more" }, url)
            break
        }
        found
    } catch (_: Exception) { null }
}
