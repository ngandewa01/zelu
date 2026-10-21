package com.koolicream.zelu

/**
 * Card spends in a foreign currency ("USD 25.00 spent on HDFC Bank Card x1291 at NETFLIX.COM").
 * Banks text only the foreign amount; the rupee amount (with the bank's markup) appears on the card statement.
 * Zelu books an estimate from these reference rates so totals stay close, and marks the payment as foreign.
 */
object Fx {
    /** Approximate rupees for one unit of each currency. An estimate, not the rate your bank charged. */
    val RATES: Map<String, Double> = mapOf(
        "USD" to 84.0, "EUR" to 91.0, "GBP" to 109.0, "AED" to 22.9, "SGD" to 64.0, "AUD" to 55.0,
        "CAD" to 61.0, "JPY" to 0.56, "CHF" to 97.0, "THB" to 2.4, "MYR" to 18.5, "HKD" to 10.8,
        "SAR" to 22.4, "QAR" to 23.1, "OMR" to 218.0, "KWD" to 274.0, "BHD" to 223.0, "NZD" to 50.0,
        "LKR" to 0.28, "NPR" to 0.625, "CNY" to 11.7, "ZAR" to 4.7, "IDR" to 0.0053, "VND" to 0.0033,
    )

    /** The foreign amount written in a message. [inr] is the estimate; [estimated] is false when the bank gave rupees too. */
    data class Spend(val code: String, val amount: Double, val inr: Double, val estimated: Boolean = true)

    private val AMT = Regex(
        """(?<![A-Za-z])(US\$|USD|EUR|GBP|AED|SGD|AUD|CAD|JPY|CHF|THB|MYR|HKD|SAR|QAR|OMR|KWD|BHD|NZD|LKR|NPR|CNY|ZAR|IDR|VND)\s*:?\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE,
    )

    fun find(text: String): Spend? {
        val m = AMT.find(text) ?: return null
        val code = m.groupValues[1].uppercase().let { if (it == "US$") "USD" else it }
        val amount = m.groupValues[2].replace(",", "").toDoubleOrNull()?.takeIf { it > 0 } ?: return null
        val rate = RATES[code] ?: return null
        return Spend(code, amount, Math.round(amount * rate * 100) / 100.0)
    }

    /** "USD 25.00" */
    fun label(s: Spend): String = s.code + " " + String.format(java.util.Locale.US, "%,.2f", s.amount)
}
