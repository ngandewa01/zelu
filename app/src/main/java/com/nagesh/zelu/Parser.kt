package com.nagesh.zelu

import java.text.SimpleDateFormat
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

// Parses bank / UPI SMS alerts into transactions.
// Patterns are deliberately loose because bank SMS formats vary.
object Parser {

    private val AMOUNT = Regex("""(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val DEBIT = Regex("""\b(debited|debit|sent|paid|spent|withdrawn|purchase)\b""", RegexOption.IGNORE_CASE)
    private val CREDIT = Regex("""\b(credited|credit|received|deposited|refunded?)\b""", RegexOption.IGNORE_CASE)
    val REFUND = Regex("""\brefund""", RegexOption.IGNORE_CASE)

    // OTPs, offers and loan promos contain amounts too; they are not transactions.
    private val SKIP = Regex("""\b(otp|one[- ]time password|do not share|offer|pre-?approved|loan offer|will be debited|request(ed)? (money|rs|inr))\b""", RegexOption.IGNORE_CASE)

    // Amounts described as a balance / limit / due are NOT the transaction amount.
    private val BALANCE_LABEL = Regex("""(bal(ance)?|avbl|avl|available|limit|o/s|outstanding|due|mandate|min(imum)? (amt|amount|due))[^.]{0,12}$""", RegexOption.IGNORE_CASE)

    private val REF = Regex("""\b(?:UPI\s*Ref(?:erence)?(?:\s*No\.?)?|RRN|Ref(?:\s*No\.?)?)\s*[:#-]?\s*(\d{6,})""", RegexOption.IGNORE_CASE)
    private val DATE = Regex("""\b(\d{1,2})[-/](\d{1,2})[-/](\d{2}|\d{4})\b""")
    private val CP = Regex(
        """\b(?:to|from|by|at)\s+(?:VPA\s+)?([\w.\-]+@[\w.\-]+|[A-Za-z][A-Za-z0-9 .&'\-]{1,40}?)(?=\s+(?:on|upi|ref|via|of|rs|inr)\b|[.,;]|$)""",
        RegexOption.IGNORE_CASE,
    )
    // Fallback merchant: NAME right after the amount, e.g. "INR2100.76 TOIT BREWPUB."
    private val MERCHANT_AFTER = Regex("""(?:Rs\.?|INR|₹)\s*[\d,]+(?:\.\d{1,2})?\s+(?:to\s+|at\s+)?([A-Za-z][A-Za-z0-9 .&'\-]{1,40}?)(?=\s+(?:on|upi|ref|via|avbl|available|bal)\b|[.,;]|$)""", RegexOption.IGNORE_CASE)
    private val WS = Regex("""\s+""")

    private val IST: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    fun parseSms(body: String, receivedMs: Long?, source: String): Txn? {
        val text = WS.replace(body, " ").trim()
        if (text.isEmpty() || SKIP.containsMatchIn(text)) return null

        // Direction, from the earliest transaction keyword present.
        val d = DEBIT.find(text)
        val c = CREDIT.find(text)
        val direction: String
        val keywordIndex: Int
        when {
            d != null && (c == null || d.range.first < c.range.first) -> {
                direction = "debit"; keywordIndex = d.range.first
            }
            c != null -> {
                direction = "credit"; keywordIndex = c.range.first
            }
            else -> return null
        }

        // Pick the amount nearest the transaction keyword, skipping balance/limit figures.
        val amounts = AMOUNT.findAll(text).toList()
        if (amounts.isEmpty()) return null
        val notBalance = amounts.filter { m ->
            val pre = text.substring(0, m.range.first)
            !BALANCE_LABEL.containsMatchIn(pre)
        }
        val pool = if (notBalance.isNotEmpty()) notBalance else amounts
        val chosen = pool.minByOrNull { abs(it.range.first - keywordIndex) } ?: pool.first()
        val amount = chosen.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null

        val date = parseDate(text, receivedMs)
        val ref = REF.find(text)?.groupValues?.get(1)
        var counterparty = CP.find(text)?.groupValues?.get(1)?.trim()
        if (counterparty.isNullOrBlank()) {
            counterparty = MERCHANT_AFTER.find(text)?.groupValues?.get(1)?.trim()
        }
        if (counterparty.isNullOrBlank()) counterparty = null

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
            h1 = (h1 xor code) * -1640531535
            h2 = (h2 xor code) * 1597334677
        }
        h1 = (h1 xor (h1 ushr 16)) * -2048144789 xor ((h2 xor (h2 ushr 13)) * -1028477387)
        h2 = (h2 xor (h2 ushr 16)) * -2048144789 xor ((h1 xor (h1 ushr 13)) * -1028477387)
        val value = 4294967296L * (2097151 and h2).toLong() + (h1.toLong() and 0xFFFFFFFFL)
        return value.toString(36)
    }
}
