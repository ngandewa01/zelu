package com.koolicream.zelu

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import androidx.compose.ui.graphics.toArgb

// Posts a local "bills due soon" notification. Best-effort: if the notification
// permission isn't granted (Android 13+), notify() is a silent no-op.
object Notifier {
    private const val CHANNEL = "zelu_dues"
    private const val ID = 1001

    fun notifyDueSoon(context: Context, count: Int, line: String) {
        if (context is DemoContext) return
        if (count <= 0) return
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL, "Bill reminders", NotificationManager.IMPORTANCE_DEFAULT),
                )
            }
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            val pi = PendingIntent.getActivity(context, 0, launch, flags)
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(context, CHANNEL) else @Suppress("DEPRECATION") Notification.Builder(context)
            builder.setSmallIcon(R.drawable.ic_stat_zelu).setColor(accentInt(context))
                .setContentTitle(if (count == 1) "A bill is due soon" else "$count bills due soon")
                .setContentText(line)
                .setAutoCancel(true)
                .setContentIntent(pi)
            nm.notify(ID, builder.build())
        } catch (_: Exception) {
            // notifications are best-effort
        }
    }

    fun enabled(context: Context): Boolean = try {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).areNotificationsEnabled()
    } catch (_: Exception) { true }

    fun openSettings(context: Context) {
        try {
            context.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }

    fun openBatterySettings(context: Context) {
        try {
            context.startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }

    // A sample payment alert, so you can check alerts reach you without waiting for a real one.
    fun notifyTest(context: Context) {
        notifyTxn(context, Row(
            id = "test-" + System.currentTimeMillis(), date = "", paise = 45_000, direction = Dir.DEBIT,
            counterparty = "Swiggy (test)", ref = null, raw = "", source = "", account = "", kind = Kind.EXPENSE, category = "Food",
        ))
    }

    // The theme accent (as chosen in the app), for tinting the status-bar icon.
    private fun accentInt(context: Context): Int = try {
        val sp = context.getSharedPreferences("zelu_theme", Context.MODE_PRIVATE)
        val pal = PALETTES[paletteIndex(sp)]
        val sysDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val dark = if (sp.contains("dark")) sp.getBoolean("dark", false) else sysDark
        (if (dark) pal.dark else pal.light).accent.toInt()
    } catch (_: Exception) { 0xFF5C7057.toInt() }

    // "HDFC 5997" -> "HDFC (5997)", "HDFC CC 9065" -> "HDFC CC (9065)"
    private fun prettyAcct(account: String): String = account.trim().let { a ->
        val k = a.lastIndexOf(' ')
        if (k > 0) a.substring(0, k) + " (" + a.substring(k + 1) + ")" else a
    }

    // A rounded tile: the merchant's brand colour and mark, or the category colour with its emoji / initial.
    private fun tileBitmap(context: Context, r: Row): Bitmap {
        val size = (context.resources.displayMetrics.density * 64f).toInt().coerceAtLeast(96)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val name = r.counterparty?.takeIf { it.isNotBlank() } ?: r.category
        val brand = brandFor(name)
        val bg = brand?.bg?.toArgb() ?: catColor(r.category).toArgb()
        val fg = brand?.fg?.toArgb() ?: 0xFFFFFFFF.toInt()
        val label = brand?.mark ?: CAT_ICON[r.category] ?: name.trim().take(1).uppercase().ifBlank { "Z" }
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = bg
        c.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), size * 0.26f, size * 0.26f, p)
        p.color = fg
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = size * 0.5f
        c.drawText(label, size / 2f, size / 2f - (p.descent() + p.ascent()) / 2f, p)
        return bmp
    }

    // "₹2,100 left of your ₹10,000 Groceries budget" for a spend in a category you have a budget for.
    private fun budgetLine(context: Context, r: Row): Pair<String, String>? {
        if (r.kind != Kind.EXPENSE || r.direction != Dir.DEBIT) return null
        val budget = Storage.loadBudgets(context)[r.category] ?: return null
        if (budget <= 0.0) return null
        val month = r.date.take(7).ifBlank { Dates.todayMonth() }
        val spent = Storage.loadRows(context)
            .filter { it.kind == Kind.EXPENSE && it.category == r.category && it.date.startsWith(month) }.rupees()
        val left = budget - spent
        val short = if (left >= 0) money(left) + " left" else money(-left) + " over"
        val full = if (left >= 0) money(left) + " left of your " + money(budget) + " " + r.category + " budget"
        else money(-left) + " over your " + money(budget) + " " + r.category + " budget"
        return short to full
    }

    // Quiet "HDFC (5997) · ₹79,377.50 · Current balance". The account shows once, in the header.
    // One per account; a newer balance replaces the older.
    fun notifyBalance(context: Context, account: String, balance: Double) {
        if (context is DemoContext) return
        if (!NotifyPrefs.spend(context)) return
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel("zelu_balance", "Account balance", NotificationManager.IMPORTANCE_LOW),
                )
            }
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            val pi = PendingIntent.getActivity(context, 2, launch, flags)
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(context, "zelu_balance") else @Suppress("DEPRECATION") Notification.Builder(context)
            val shown = prettyAcct(account)
            val logo = bankLogoRes(account)?.let { BitmapFactory.decodeResource(context.resources, it) }
            builder.setSmallIcon(R.drawable.ic_stat_zelu)
                .setColor(accentInt(context))
                .setSubText(shown)
                .setContentTitle(moneyPaise(balance))
                .setContentText("Current balance")
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pi)
            if (logo != null) builder.setLargeIcon(logo)
            nm.notify(60000 + (account.hashCode() and 0x7fffffff) % 1000, builder.build())
        } catch (_: Exception) {
            // best-effort
        }
    }

    // One notification per new transaction: "₹450 spent at Swiggy" / "Food · ₹2,100 left" (+ account header).
    fun notifyTxn(context: Context, r: Row) {
        if (context is DemoContext) return
        if (!NotifyPrefs.spend(context)) return
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel("zelu_txn_hi", "Payments", NotificationManager.IMPORTANCE_HIGH),
                )
            }
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            val pi = PendingIntent.getActivity(context, 1, launch, flags)
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(context, "zelu_txn_hi") else @Suppress("DEPRECATION") Notification.Builder(context).setPriority(Notification.PRIORITY_HIGH)
            val amt = money(r.amount)
            val credit = r.direction == Dir.CREDIT
            val who = r.counterparty?.takeIf { it.isNotBlank() } ?: ""
            val unknown = r.category == "Unknown" && r.direction == Dir.DEBIT
            val nid = (r.id.hashCode() and 0x7fffffff) % 900000 + 2000
            if (unknown) {
                for ((i, cat) in listOf("Food", "Groceries").withIndex()) {
                    val it = Intent(context, CategorizeReceiver::class.java)
                        .putExtra("id", r.id).putExtra("payee", r.counterparty ?: "")
                        .putExtra("category", cat).putExtra("nid", nid)
                    val act = PendingIntent.getBroadcast(context, nid * 4 + i, it, flags)
                    builder.addAction(Notification.Action.Builder(null, cat, act).build())
                }
                val skip = Intent(context, CategorizeReceiver::class.java)
                    .putExtra("id", r.id).putExtra("payee", r.counterparty ?: "")
                    .putExtra("category", "Ignore").putExtra("kind", Kind.IGNORE.key).putExtra("nid", nid)
                builder.addAction(Notification.Action.Builder(null, "Not spending",
                    PendingIntent.getBroadcast(context, nid * 4 + 3, skip, flags)).build())
            }
            val title = if (credit) amt + " received" + (if (who.isNotEmpty()) " from $who" else "")
            else amt + " spent" + (if (who.isNotEmpty()) " at $who" else "")
            val bud = budgetLine(context, r)
            val line2 = when {
                unknown -> "Unknown payee. Tag it so Zelu remembers."
                bud != null -> r.category + " · " + bud.first
                else -> r.category
            }
            val acct = if (r.account.isNotBlank()) prettyAcct(r.account) else ""
            builder.setSmallIcon(R.drawable.ic_stat_zelu)
                .setColor(accentInt(context))
                .setLargeIcon(tileBitmap(context, r))
                .setContentTitle(title)
                .setContentText(line2)
                .setStyle(Notification.BigTextStyle().bigText(
                    (if (bud != null) r.category + " · " + bud.second else line2) + (if (acct.isNotEmpty()) "\n$acct" else "")))
                .setAutoCancel(true)
                .setContentIntent(pi)
            if (acct.isNotEmpty()) builder.setSubText(acct)
            nm.notify(nid, builder.build())
        } catch (_: Exception) {
            // best-effort
        }
    }
}
