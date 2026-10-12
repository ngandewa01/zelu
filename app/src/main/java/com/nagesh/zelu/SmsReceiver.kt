package com.nagesh.zelu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

// Captures new SMS as they arrive, even when the app is closed, and stores
// any that parse as transactions.
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return

        // A multipart SMS arrives as several parts of the same message; join them.
        val body = StringBuilder()
        var address = ""
        var date = System.currentTimeMillis()
        for (p in parts) {
            body.append(p.messageBody ?: "")
            p.originatingAddress?.let { address = it }
            date = p.timestampMillis
        }

        val raw = RawSms(address, body.toString(), date)
        try {
            Ingest.ingest(context.applicationContext, listOf(raw))
        } catch (_: Exception) {
            // Never crash on an incoming SMS.
        }
    }
}
