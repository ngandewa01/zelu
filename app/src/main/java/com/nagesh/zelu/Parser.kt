package com.nagesh.zelu

import java.text.SimpleDateFormat
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

// Parses bank / UPI SMS alerts into transactions.
// Patterns are deliberately loose because bank SMS formats vary.
object Parser {

    private val AMOUNT = Regex("""(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val DEBIT = Regex("""\b(debited|sent|paid|spent|withdrawn)\b""", RegexOption.IGNORE_CASE)
    private val CREDIT = Regex("""\b(credited|received|deposited|refunded?)\b""", RegexOption.IGNORE_CASE)
    val REFUND = Regex("""\brefund""", RegexOption.IGNORE_CASE)

    // OTPs, offers and loan promos contain amounts too; they are not transactions.
    private val SKIP = Regex("""\b(otp|one[- ]time password|do not share|offer|pre-?approved|loan)\b""", RegexOption.IGNORE_CASE)
    private val REF = Regex("""\b(?:UPI\s*Ref(?:erence)?(?:\s*No\.?)?|RRN|Ref(?:\s*No\.?)?)\s*[:#-]?\s*(\d{6,})""", RegexOption.IGNORE_CASE)
    private val DATE = Regex("""\b(\d{1,2})[-/](\d{1,2})[-/](\d{2}|\d{4})\b""")
    private val CP = Regex(
        """\b(?:to|from|by)\s+(?:VPA\s+)?([\w.\-]+@[\w.\-]+|[A-Za-z][A-Za-z .&'\-]{1,40}?)(?=\s+(?:on|UPI|Ref|via|of)\b|[.,;]|$)""",
        RegexOption.IGNORE_CASE,
    )
    private val WS = Regex("""\s+""")

    private val IST: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    fun parseSms(body: String, receivedMs: Long?, source: String): Txn? {
        val text = WS.replace(body, " ").trim()
        if (text.isEmpty() || SKIP.containsMatchIn(text)) return null

        val amt = AMOUNT.find(text) ?: return null
        val amount = amt.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null

        val d = DEBIT.find(text)
        val c = CREDIT.find(text)
        val direction: String = when {
            d != null && (c == null || d.range.first < c.range.first) -> "debit"
            c != null -> "credit"
            else -> return null
        }

        val date = parseDate(text, receivedMs)
        val ref = REF.find(text)?.groupValues?.get(1)
        val counterparty = CP.find(text)?.groupValues?.get(1)?.trim()

        val id = if (ref != null) "ref-$ref"
        else "sms-" + hash("$date|$amount|$direction|$text")

        return Txn(id, date, amount, direction, counterparty, ref, text, source)
    }

    private fun parseDate(text: String, receivedMs: Long?): String {
        val m = DATE.find(text)
        if (m != null) {
            val dd = m.groupValues[1].toInt()
            val mm = m.groupValues[2].toInt()
            var yy = m.groupValues[3].toInt()
            if (yy < 100) yy += 2000
            try {
                val cal = GregorianCalendar(IST)
                cal.isLenient = false
                cal.clear()
                cal.set(yy, mm - 1, dd)
                return fmt(cal.timeInMillis)
            } catch (_: Exception) {
                // invalid date, fall through to received time
            }
        }
        val base = receivedMs ?: System.currentTimeMillis()
        return fmt(base)
    }

    private fun fmt(ms: Long): String {
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        f.timeZone = IST
        return f.format(Date(ms))
    }

    // Non-cryptographic 32-bit string hash (cyrb53-style) to dedupe SMS without a ref.
    private fun hash(s: String): String {
        var h1 = 0xdeadbeef.toInt()
        var h2 = 0x41c6ce57
        for (ch in s) {
            val code = ch.code
            h1 = (h1 xor code) * -1640531535   // 2654435761 as signed 32-bit
            h2 = (h2 xor code) * 1597334677
        }
        h1 = (h1 xor (h1 ushr 16)) * -2048144789 xor ((h2 xor (h2 ushr 13)) * -1028477387)
        h2 = (h2 xor (h2 ushr 16)) * -2048144789 xor ((h1 xor (h1 ushr 13)) * -1028477387)
        val value = 4294967296L * (2097151 and h2).toLong() + (h1.toLong() and 0xFFFFFFFFL)
        return value.toString(36)
    }
}
