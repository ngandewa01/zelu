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

    // "Payment of Rs X for your <service> connection ... received" is an OUTGOING bill
    // payment (the biller received your money) even though it contains the word "received".
    // "received a payment" / "payment … has been received" means a biller/merchant received
    // YOUR money — you paid. Real income instead says "credited to your a/c".
    private val OUTGOING_BILL = Regex(
        """received a?\s*payment\b|your payment\b|payment\s+(?:of\s+)?.{0,45}?\b(?:has been|is|was)\s+received\b""",
        RegexOption.IGNORE_CASE,
    )

    // An NPS contribution ("credited to NAGESH TIER1") is money going OUT into an investment,
    // not income — flip it to a debit.
    private val INVEST_OUT = Regex("""\b(tier\s?1|tier\s?2|\bnps\b|pran)\b""", RegexOption.IGNORE_CASE)

    // FASTag / toll transactions come from the FASTag wallet, not the bank account.
    private val FASTAG = Regex("""\b(fastt?ag|toll|netc)\b""", RegexOption.IGNORE_CASE)

    // OTPs, offers and loan promos contain amounts too; they are not transactions.
    private val SKIP = Regex("""\b(otp|one[- ]time password|do not share|offer|pre-?approved|loan offer|will be debited|will be deducted|will be auto[- ]?debited|auto[- ]?debited on|request(ed)? (money|rs|inr)|due on|due date|payment is due|is due|kindly pay|please pay|pay by|pay now|pay your|overdue|statement (is )?generated|bill generated|scheduled|recharge due|has expired|expiring|payment reminder|reminder|changed from (inr|rs)|limit[^.]{0,25}(changed|revised|enhanced|increased|upgraded|updated)|(enhanced|increased|revised|upgraded)[^.]{0,15}limit|up ?to rs|cashback up to|is waiting for|save \d{1,2}%|get credit|apply now|pre-?qualified|eligible for|pay instantly|smart emi|payable in \d|bill pay|hdfcbk\.io|shrtsms|bit\.ly|tinyurl|rb\.gy|cutt\.ly|ow\.ly|is\.gd|t\.me|wa\.me|shorturl|lottery|lucky winner|have won|claim your prize|withdraw process|account (blocked|suspended)|kyc (expired|update|suspend))\b""", RegexOption.IGNORE_CASE)

    // Investment contributions (EPF/NPS/funds) — tracked separately, kept out of the account
    // filter so they don't create phantom bank accounts.
    private val INVESTMENT = Regex("""\b(epf|epfo|provident fund|passbook|uan|nps|tier\s?1|tier\s?2|pran|mutual fund|zerodha|groww|upstox|kuvera|smallcase|indmoney|demat|elss|folio|icici direct|hdfc securities|paytm money|angel ?one|ppf|nippon india|sovereign gold|etmoney|5paisa|motilal)\b""", RegexOption.IGNORE_CASE)

    // Amounts described as a balance / limit / due are NOT the transaction amount.
    private val BALANCE_LABEL = Regex("""(bal(ance)?|avbl|avl|available|limit|o/s|outstanding|due|mandate|min(imum)? (amt|amount|due))[^.]{0,12}$""", RegexOption.IGNORE_CASE)

    private val REF = Regex("""\b(?:UPI\s*Ref(?:erence)?(?:\s*No\.?)?|RRN|Ref(?:\s*No\.?)?)\s*[:#-]?\s*(\d{6,})""", RegexOption.IGNORE_CASE)
    private val BANK = Regex("""\b(HDFC|SBI|ICICI|AXIS|KOTAK|IDFC|YES ?BANK|PNB|BOB|CANARA|INDUSIND|RBL|AU ?BANK|BANDHAN|FEDERAL|IDBI|UNION|UCO|IOB|DBS|HSBC|CITI|AMEX|DCB|PAYTM|IPPB|EQUITAS|UJJIVAN|SARASWAT)\b""", RegexOption.IGNORE_CASE)

    // Bank codes inside an SMS sender id (e.g. "AD-HDFCBK", "JX-UNIONB", "CBSSBI"), matched
    // as a substring because the code runs into surrounding letters. Specific codes first.
    private val SENDER_CODES: List<Pair<String, String>> = listOf(
        "HDFC" to "HDFC", "ICIC" to "ICICI", "AXIS" to "AXIS", "KOTAK" to "KOTAK",
        "UBOI" to "UNION", "UBIN" to "UNION", "UNION" to "UNION",
        "CBSSBI" to "SBI", "ATMSBI" to "SBI", "SBIINB" to "SBI", "SBIPSG" to "SBI", "SBICRD" to "SBI", "SBI" to "SBI",
        "PNB" to "PNB", "BARODA" to "BOB", "BOBTXN" to "BOB", "CANBNK" to "CANARA", "CANARA" to "CANARA",
        "INDUS" to "INDUSIND", "RBLBNK" to "RBL", "AUBANK" to "AU", "BANDHAN" to "BANDHAN", "BANDAN" to "BANDHAN",
        "FEDBNK" to "FEDERAL", "IDBI" to "IDBI", "IDFC" to "IDFC", "YESBNK" to "YES", "YESBK" to "YES",
        "BOIIND" to "BOI", "INDBNK" to "INDIAN", "INDBK" to "INDIAN", "CENTBK" to "CENTRAL",
        "UCOBNK" to "UCO", "IOBCHN" to "IOB", "PAYTM" to "PAYTM", "PYTM" to "PAYTM",
        "DBS" to "DBS", "HSBC" to "HSBC", "CITI" to "CITI", "SCBANK" to "SCB", "AMEX" to "AMEX",
        "KVBANK" to "KVB", "KARUR" to "KVB", "RBL" to "RBL", "BOB" to "BOB", "UCO" to "UCO", "IOB" to "IOB", "BOI" to "BOI",
    )
    // A real account/card tail: exactly 4 digits anchored to a mask (XX5997, ****9065)
    // or an explicit a/c|account|card keyword. Avoids grabbing stray 3-digit numbers.
    // A masked account tail: one or more mask chars then 4 digits (*5997, **2931, XX5997).
    private val LAST4_MASK = Regex("""[xX*#]+\s*\d*(\d{4})\b""")
    private val LAST4_KW = Regex("""(?:a/?c|acct|account|card)\s*(?:no\.?|number|ending(?:\s*(?:with|in))?|is)?\s*[xX*#]*\s*\d*(\d{4})\b""", RegexOption.IGNORE_CASE)
    // Your account is the one money leaves FROM (debit) or lands in / TO (credit) — never the
    // other party's account. "debited from a/c *5997 ... to a/c **2931" -> your a/c is 5997.
    // Matches "from a/c *5997", "from HDFC Bank XX5997", etc. (a/c word optional if masked).
    private val FROM_AC = Regex("""\b(?:from|frm)\b[A-Za-z:. ]{0,22}?(?:(?:a/?c|acct|account)\s*(?:no\.?|number)?\s*[xX*#]*|[xX*#]+)\s*\d*(\d{4})\b""", RegexOption.IGNORE_CASE)
    private val TO_AC = Regex("""\b(?:to|into|in)\b[A-Za-z:. ]{0,22}?(?:(?:a/?c|acct|account)\s*(?:no\.?|number)?\s*[xX*#]*|[xX*#]+)\s*\d*(\d{4})\b""", RegexOption.IGNORE_CASE)
    // A credit card is marked by an explicit "credit card" or by a spending LIMIT (debit
    // cards / bank accounts show a balance, never a limit).
    private val IS_CARD = Regex("""\b(credit card|\bcc\b|card ending|on card|your card|avl lmt|avl limit|available limit|credit limit)\b""", RegexOption.IGNORE_CASE)
    // A debit card is linked to a bank account, not a separate credit-card bill.
    private val DEBIT_CARD = Regex("""\b(debit card|\bdc\b)\b""", RegexOption.IGNORE_CASE)
    private val AC_KEYWORD = Regex("""\b(a/?c|acct|account)\b""", RegexOption.IGNORE_CASE)
    private val DATE = Regex("""\b(\d{1,2})[-/](\d{1,2})[-/](\d{2}|\d{4})\b""")
    private val CP = Regex(
        """\b(?:to|from|by|at)\s+(?:VPA\s+)?([\w.\-]+@[\w.\-]+|[A-Za-z][A-Za-z0-9 .&'\-]{1,40}?)(?=\s+(?:on|upi|ref|via|of|rs|inr)\b|[.,;]|$)""",
        RegexOption.IGNORE_CASE,
    )
    // Fallback merchant: NAME right after the amount, e.g. "INR2100.76 TOIT BREWPUB."
    private val MERCHANT_AFTER = Regex("""(?:Rs\.?|INR|₹)\s*[\d,]+(?:\.\d{1,2})?\s+(?:to\s+|at\s+)?([A-Za-z][A-Za-z0-9 .&'\-]{1,40}?)(?=\s+(?:on|upi|ref|via|avbl|available|bal)\b|[.,;]|$)""", RegexOption.IGNORE_CASE)

    // Promotional / instructional phrases that must never be treated as a payee.
    private val CP_JUNK = Regex("""^(register|continue|enjoy|enjoying|setup|set|click|share|download|restart|your|is|was|will|now|pay|visit|call|the|update|info|dear|team|please|to|thank|get|go|use|tap|view|check|received|debited|credited|payment|paid|spent|sent|txn|transaction|amount|rs|inr|bal|balance|avl|available|successful|successfully|a/c|acct|account|report|block|dispute|verify|helpline|sms|transfer|transferred|a year|a month|year|month|months|annual|debit|debited|vide|clearing|has|have|we|hi|dear)\b""", RegexOption.IGNORE_CASE)

    // ACH / NACH mandate debits name the biller after "ACH D-" or "ACH/".
    private val ACH_PAYEE = Regex("""ACH[ /]?D?[-/ ]+([A-Za-z][A-Za-z .&]{2,40}?)[-/]""", RegexOption.IGNORE_CASE)
    // NEFT/IMPS/RTGS strings name the sender after the bank code: "NEFT Cr-BOFA0CN6215-QUALCOMM INDIA PVT LTD-…".
    private val NEFT_SENDER = Regex("""(?:NEFT|IMPS|RTGS)[^-]{0,12}[-:]\s*[A-Za-z0-9]{6,}\s*[-]\s*([A-Za-z][A-Za-z .&]{2,40}?)\s*[-]""", RegexOption.IGNORE_CASE)

    // A loan EMI debit notice (NACH / ACH / e-mandate / installment). Captured as a real
    // monthly expense even though it reads like a reminder; de-duplicated to once a month.
    private val EMI_DEBIT = Regex("""\b(nach|ach|e-?mandate|installment|instalment|loan a/?c|loan ac|emi)\b.{0,70}\b(debit|debited|deduct|deducted)\b|\b(debit|debited|deduct|deducted)\b.{0,50}\b(nach|ach|emi|loan|installment|instalment)\b""", RegexOption.IGNORE_CASE)
    private val LOAN_AC = Regex("""loan\s*a/?c(?:\s*no\.?)?\s*[xX*#]*\s*(\d{3,4})""", RegexOption.IGNORE_CASE)

    // Canonical merchant names, so "ZOMATO LTD" / "Zomato Media Pvt Ltd" group as one payee.
    private val MERCHANT_MAP: List<Pair<Regex, String>> = listOf(
        Regex("""jio\s*fiber|jiofiber|jiohome""", RegexOption.IGNORE_CASE) to "JioFiber",
        Regex("""\bjio\b""", RegexOption.IGNORE_CASE) to "Jio",
        Regex("""zomato""", RegexOption.IGNORE_CASE) to "Zomato",
        Regex("""swiggy""", RegexOption.IGNORE_CASE) to "Swiggy",
        Regex("""blinkit""", RegexOption.IGNORE_CASE) to "Blinkit",
        Regex("""zepto""", RegexOption.IGNORE_CASE) to "Zepto",
        Regex("""bigbasket|innovative retail""", RegexOption.IGNORE_CASE) to "BigBasket",
        Regex("""avenue supermart|\bdmart\b|d mart""", RegexOption.IGNORE_CASE) to "DMart",
        Regex("""q ?mart retail|\bqmart\b""", RegexOption.IGNORE_CASE) to "Q Mart",
        Regex("""pure o natural""", RegexOption.IGNORE_CASE) to "Pure O Natural",
        Regex("""eatsure""", RegexOption.IGNORE_CASE) to "EatSure",
        Regex("""jubilant food|\bdominos\b|domino's""", RegexOption.IGNORE_CASE) to "Domino's",
        Regex("""pizza ?hut""", RegexOption.IGNORE_CASE) to "Pizza Hut",
        Regex("""qualcomm""", RegexOption.IGNORE_CASE) to "Qualcomm",
        Regex("""olamoney|ola money""", RegexOption.IGNORE_CASE) to "OlaMoney",
        Regex("""nobroker""", RegexOption.IGNORE_CASE) to "NoBroker",
        Regex("""propelld""", RegexOption.IGNORE_CASE) to "Propelld",
        Regex("""tier\s?1|tier\s?2|\bnps\b|\bpran\b""", RegexOption.IGNORE_CASE) to "NPS",
        Regex("""\bepf\b|epfo|provident fund|passbook|\buan\b""", RegexOption.IGNORE_CASE) to "EPF",
        Regex("""amazon""", RegexOption.IGNORE_CASE) to "Amazon",
        Regex("""flipkart""", RegexOption.IGNORE_CASE) to "Flipkart",
        Regex("""myntra""", RegexOption.IGNORE_CASE) to "Myntra",
        Regex("""urban\s*company|urbanclap""", RegexOption.IGNORE_CASE) to "Urban Company",
        Regex("""country\s*delight""", RegexOption.IGNORE_CASE) to "Country Delight",
        Regex("""mother\s*dairy""", RegexOption.IGNORE_CASE) to "Mother Dairy",
        Regex("""netflix""", RegexOption.IGNORE_CASE) to "Netflix",
        Regex("""spotify""", RegexOption.IGNORE_CASE) to "Spotify",
        Regex("""hotstar""", RegexOption.IGNORE_CASE) to "Hotstar",
        Regex("""\bcred\b""", RegexOption.IGNORE_CASE) to "CRED",
        Regex("""\buber\b""", RegexOption.IGNORE_CASE) to "Uber",
        Regex("""\bola\b""", RegexOption.IGNORE_CASE) to "Ola",
        Regex("""rapido""", RegexOption.IGNORE_CASE) to "Rapido",
        Regex("""snabbit""", RegexOption.IGNORE_CASE) to "Snabbit",
        Regex("""\bairtel\b""", RegexOption.IGNORE_CASE) to "Airtel",
    )

    // Canonical names, used to collapse duplicate biller confirmation SMS.
    val CANON_NAMES: Set<String> = MERCHANT_MAP.map { it.second }.toSet()

    private val WS = Regex("""\s+""")

    private val IST: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    fun parseSms(body: String, receivedMs: Long?, source: String): Txn? {
        val text = WS.replace(body, " ").trim()
        // Loan EMI debit notices are kept even if they read like reminders; other reminders drop.
        val isEmiDebit = EMI_DEBIT.containsMatchIn(text)
        if (text.isEmpty() || (SKIP.containsMatchIn(text) && !isEmiDebit)) return null

        // Direction, from the earliest transaction keyword present.
        val d = DEBIT.find(text)
        val c = CREDIT.find(text)
        var direction: String
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
        // A biller confirming it received your bill payment is money going OUT.
        if (direction == "credit" && (OUTGOING_BILL.containsMatchIn(text) || INVEST_OUT.containsMatchIn(text))) direction = "debit"

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
        // Prefer a known merchant (clean, groups duplicates); then the ACH biller name;
        // then the generic "to/at NAME" and "after-amount NAME" fallbacks, junk filtered out.
        var counterparty = canonicalMerchant(text)
        if (counterparty == null) {
            counterparty = ACH_PAYEE.find(text)?.groupValues?.get(1)?.let { titleCase(it.trim()) }
        }
        if (counterparty == null) {
            counterparty = NEFT_SENDER.find(text)?.groupValues?.get(1)?.let { titleCase(it.trim()) }
        }
        // Scan all "to/from/by/at NAME" matches and take the first real one (skips "by Rs …").
        if (counterparty == null) counterparty = CP.findAll(text).firstNotNullOfOrNull { cleanCp(it.groupValues[1]) }
        if (counterparty == null) counterparty = cleanCp(MERCHANT_AFTER.find(text)?.groupValues?.get(1))

        // For a loan EMI, name the payee after the loan account ("HDFC Loan 448") so each
        // loan is distinct in the ledger — without the loan number polluting the account filter.
        if (isEmiDebit) {
            LOAN_AC.find(text)?.groupValues?.get(1)?.let { ln ->
                val bk = BANK.find(text)?.value?.uppercase()?.replace(" ", "") ?: ""
                counterparty = (if (bk.isNotEmpty()) "$bk " else "") + "Loan $ln"
            }
        }

        val account = accountOf(text, source, direction)

        val id = if (ref != null) "ref-$ref"
        else "sms-" + hash("$date|$amount|$direction|$text")

        return Txn(id, date, amount, direction, counterparty, ref, text, source, account)
    }

    // Best-effort account label, e.g. "HDFC 5997" or "HDFC CC 1291".
    private fun accountOf(text: String, source: String, direction: String): String {
        var bank = BANK.find(text)?.value?.uppercase()?.replace(" ", "") ?: ""
        if (bank == "YESBANK") bank = "YES"
        if (bank.isEmpty()) {
            // Decode the bank from the sender id (code runs into letters, so match substring).
            val src = source.uppercase()
            bank = SENDER_CODES.firstOrNull { src.contains(it.first) }?.second ?: ""
        }
        // Investments aren't bank accounts you spend from — keep them out of the account filter.
        if (INVESTMENT.containsMatchIn(text)) return ""
        // FASTag / toll debits come from the FASTag wallet, not the bank account.
        if (FASTAG.containsMatchIn(text)) return if (bank.isNotEmpty()) "$bank FASTag" else "FASTag"
        // Prefer the YOUR-side account: "from" on a debit, "to" on a credit.
        val directional = when (direction) {
            "debit" -> FROM_AC.find(text)?.groupValues?.get(1)
            "credit" -> TO_AC.find(text)?.groupValues?.get(1)
            else -> null
        }
        var last4 = directional
            ?: LAST4_MASK.find(text)?.groupValues?.get(1)
            ?: LAST4_KW.find(text)?.groupValues?.get(1) ?: ""
        // A debit card is the bank account, not a separate credit-card bill.
        val debitCard = DEBIT_CARD.containsMatchIn(text)
        // When a debit card shows only its card number (no a/c), attribute it to the bank
        // account itself (which then folds into the main savings) rather than the card number.
        if (debitCard && !AC_KEYWORD.containsMatchIn(text)) last4 = ""
        val isCard = IS_CARD.containsMatchIn(text) && !debitCard
        return when {
            bank.isNotEmpty() && last4.isNotEmpty() -> "$bank ${if (isCard) "CC " else ""}$last4"
            // A card with no readable number still stays a card, so it won't merge into savings.
            bank.isNotEmpty() -> bank + if (isCard) " CC" else ""
            last4.isNotEmpty() -> (if (isCard) "CC " else "") + last4
            else -> ""
        }
    }

    private fun canonicalMerchant(text: String): String? =
        MERCHANT_MAP.firstOrNull { it.first.containsMatchIn(text) }?.second

    private val SALUTATION = Regex("""^(m/?s|mr|mrs|ms|dr|shri|smt|sri)\.?\s+""", RegexOption.IGNORE_CASE)

    private fun cleanCp(raw: String?): String? {
        val s = (raw?.trim()?.trim('.', ',', ';', ':')?.trim() ?: return null).replace(SALUTATION, "")
        if (s.length < 2) return null
        if (CP_JUNK.containsMatchIn(s)) return null
        // A bank/account/card string (bank name plus digits) is your own account, not a payee.
        if (BANK.containsMatchIn(s) && s.any { it.isDigit() }) return null
        return s
    }

    private fun titleCase(s: String): String =
        s.lowercase().split(" ").joinToString(" ") { w ->
            if (w.isEmpty()) w else w.replaceFirstChar { it.uppercase() }
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
