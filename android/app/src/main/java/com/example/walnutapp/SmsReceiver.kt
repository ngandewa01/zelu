package com.example.walnutapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import org.json.JSONArray
import org.json.JSONObject

// Declared in AndroidManifest.xml, so it runs even when the app is closed.
// It queues incoming SMS; the JavaScript side drains the queue when the app opens.
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        // Long SMS arrive as several parts from the same sender; join them.
        val grouped = parts.groupBy { it.originatingAddress.orEmpty() }

        synchronized(LOCK) {
            val prefs = context.getSharedPreferences(QUEUE_PREFS, Context.MODE_PRIVATE)
            val queue = JSONArray(prefs.getString("items", "[]") ?: "[]")
            for ((address, msgs) in grouped) {
                val obj = JSONObject()
                obj.put("address", address)
                obj.put("body", msgs.joinToString("") { it.messageBody.orEmpty() })
                obj.put("date", msgs.first().timestampMillis)
                queue.put(obj)
            }
            prefs.edit().putString("items", queue.toString()).apply()
        }
    }

    companion object {
        const val QUEUE_PREFS = "sms_queue"
        val LOCK = Any()
    }
}
