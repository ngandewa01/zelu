package com.nagesh.zelu

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

// Posts a local "bills due soon" notification. Best-effort: if the notification
// permission isn't granted (Android 13+), notify() is a silent no-op.
object Notifier {
    private const val CHANNEL = "zelu_dues"
    private const val ID = 1001

    fun notifyDueSoon(context: Context, count: Int, line: String) {
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
            builder.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(if (count == 1) "A bill is due soon" else "$count bills due soon")
                .setContentText(line)
                .setAutoCancel(true)
                .setContentIntent(pi)
            nm.notify(ID, builder.build())
        } catch (_: Exception) {
            // notifications are best-effort
        }
    }

    // One notification per new transaction: "₹450 spent · Swiggy" / "Food".
    fun notifyTxn(context: Context, r: Row) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel("zelu_txn", "Transactions", NotificationManager.IMPORTANCE_DEFAULT),
                )
            }
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            val pi = PendingIntent.getActivity(context, 1, launch, flags)
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(context, "zelu_txn") else @Suppress("DEPRECATION") Notification.Builder(context)
            val amt = "₹" + String.format(java.util.Locale.US, "%,.0f", r.amount)
            val verb = if (r.direction == "credit") "received" else "spent"
            val who = r.counterparty?.takeIf { it.isNotBlank() } ?: r.account
            if (r.category == "Unknown" && r.direction == "debit") {
                val nid = (r.id.hashCode() and 0x7fffffff) % 900000 + 2000
                for ((i, cat) in listOf("Food", "Groceries", "Shopping").withIndex()) {
                    val it = Intent(context, CategorizeReceiver::class.java)
                        .putExtra("id", r.id).putExtra("payee", r.counterparty ?: "")
                        .putExtra("category", cat).putExtra("nid", nid)
                    val act = PendingIntent.getBroadcast(context, nid * 4 + i, it, flags)
                    builder.addAction(Notification.Action.Builder(null, cat, act).build())
                }
            }
            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("$amt $verb" + if (who.isNotBlank()) " · $who" else "")
                .setContentText(if (r.category == "Unknown") "Unknown payee — tag it" else r.category)
                .setAutoCancel(true)
                .setContentIntent(pi)
            nm.notify((r.id.hashCode() and 0x7fffffff) % 900000 + 2000, builder.build())
        } catch (_: Exception) {
            // best-effort
        }
    }
}
