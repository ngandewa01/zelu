package com.nagesh.zelu

import android.content.Context
import android.provider.Telephony

object SmsReader {

    // Reads the most recent messages from the SMS inbox.
    // Every message received between two moments (milliseconds), newest first.
    fun readInboxBetween(context: Context, fromMs: Long, toMs: Long): List<RawSms> {
        val out = ArrayList<RawSms>()
        val cols = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI, cols, "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} <= ?",
            arrayOf(fromMs.toString(), toMs.toString()), "${Telephony.Sms.DATE} DESC",
        )?.use { c ->
            val ai = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bi = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val di = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (c.moveToNext()) out.add(RawSms(address = c.getString(ai) ?: "", body = c.getString(bi) ?: "", date = c.getLong(di)))
        }
        return out
    }

    fun readInbox(context: Context, limit: Int = 2000): List<RawSms> {
        val out = ArrayList<RawSms>()
        val cols = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
        )
        val sort = "${Telephony.Sms.DATE} DESC LIMIT $limit"
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI, cols, null, null, sort,
        )?.use { c ->
            val ai = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bi = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val di = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (c.moveToNext()) {
                out.add(
                    RawSms(
                        address = c.getString(ai) ?: "",
                        body = c.getString(bi) ?: "",
                        date = c.getLong(di),
                    ),
                )
            }
        }
        return out
    }
}
