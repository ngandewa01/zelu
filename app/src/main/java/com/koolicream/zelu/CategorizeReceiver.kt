package com.koolicream.zelu

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Handles the category buttons on a transaction notification: tags that transaction
// and remembers the payee so future ones are categorised automatically.
class CategorizeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val cat = intent.getStringExtra("category") ?: return
        val payee = intent.getStringExtra("payee") ?: ""
        val kind = intent.getStringExtra("kind") ?: "expense"
        val nid = intent.getIntExtra("nid", 0)
        inBackground {
            Storage.setOverride(context, id, kind, cat)
            if (payee.isNotBlank()) Storage.setPayeeRule(context, payee, kind, cat)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(nid)
        }
    }
}
