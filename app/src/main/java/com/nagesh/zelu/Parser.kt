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

    private val AMOUNT = Regex("""(?:Rs\.?|INR|₹)\s*:?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val DEBIT = Regex("""\b(debited|debit|sent|paid|spent|withdrawn|purchase)\b""", RegexOption.IGNORE_CASE)
    private val CREDIT = Regex("""\b(credited|credit|received|deposited|refunded?)\b""", RegexOption.IGNORE_CASE)
    val REFUND = Regex("""\brefund""", RegexOption.IGNORE_CASE)

    // "Payment of Rs X for your <service> connection ... received" is an OUTGOING bill
    // payment (the biller received your money) even though it contains the word "received".
    // "a biller/merchant received YOUR money" (you paid) vs real income ("credited to your a/c").
    private val RECV_PAYMENT = Regex("""received a?\s*payment\b""", RegexOption.IGNORE_CASE)
    private val YOUR_PAYMENT_RECV = Regex("""\byour payment\b.{0,60}\breceived\b""", RegexOption.IGNORE_CASE)
    private val HAS_RECEIVED = Regex("""\b(?:has been|is|was)\s+received\b""", RegexOption.IGNORE_CASE)
    private val BILLER_FOR = Regex("""\bfor your\b|\btowards your\b""", RegexOption.IGNORE_CASE)
    private val INBOUND = Regex("""\bcredited (?:to|in) your\b|\breceived in your\b|\bin your a/?c\b|\bto your a/?c\b""", RegexOption.IGNORE_CASE)

    // A payment you MADE (biller confirms receipt), not income.
    private fun isOutgoingPayment(text: String): Boolean =
        RECV_PAYMENT.containsMatchIn(text) ||
            YOUR_PAYMENT_RECV.containsMatchIn(text) ||
            (HAS_RECEIVED.containsMatchIn(text) && BILLER_FOR.containsMatchIn(text) && !INBOUND.containsMatchIn(text))

    // An NPS contribution ("credited to NAGESH TIER1") is money going OUT into an investment,
    // not income — flip it to a debit.
    private val INVEST_OUT = Regex("""\b(tier\s?1|tier\s?2|\bnps\b|pran)\b""", RegexOption.IGNORE_CASE)

    // FASTag / toll transactions come from the FASTag wallet, not the bank account.
    private val FASTAG = Regex("""\b(fastt?ag|toll|netc)\b""", RegexOption.IGNORE_CASE)

    // OTPs, offers and loan promos contain amounts too; they are not transactions.
    private val SKIP = Regex("""\b(otp|one[- ]time password|do not share|offer|pre-?approved|loan offer|will be debited|will be deducted|will be auto[- ]?debited|will be (credited|refunded|reversed)|initiated (for )?(a )?refund|refund (has been |is )?initiated|auto[- ]?debited on|request(ed)? (money|rs|inr)|due on|due date|payment is due|is due|kindly pay|please pay|pay by|pay now|pay your|overdue|statement (is )?generated|bill generated|scheduled|recharge due|has expired|expiring|payment reminder|reminder|changed from (inr|rs)|limit[^.]{0,25}(changed|revised|enhanced|increased|upgraded|updated)|(enhanced|increased|revised|upgraded)[^.]{0,15}limit|up ?to rs|cashback up to|is waiting for|save \d{1,2}%|get credit|apply now|pre-?qualified|eligible for|pay instantly|smart emi|payable in \d|hdfcbk\.io|shrtsms|bit\.ly|tinyurl|rb\.gy|cutt\.ly|ow\.ly|is\.gd|t\.me|wa\.me|shorturl|lottery|lucky winner|have won|claim your prize|withdraw process|account (blocked|suspended)|kyc (expired|update|suspend)|vvip|consult your astro|astro now|bodhi|you'?re blessed|get rs [\d,]+[ -]?code|txn declined|transaction (declined|failed|unsuccessful)|\bdeclined\b|payment (failed|declined)|could not be processed|was unsuccessful|usage disabled)\b""", RegexOption.IGNORE_CASE)

    // A debit that has NOT happened yet: a scheduled / future auto-debit, e.g.
    // "your EMI will be auto-debited on 5th". Money hasn't left the account.
    private val FUTURE_DEBIT = Regex(
        """\b(will|shall|would|to|going to) be[a-z '-]{0,18}(debit|debited|deduct|deducted)\b|\b(will|shall) (be )?(debit|deduct)\b""",
        RegexOption.IGNORE_CASE,
    )
    // A dues / reminder notice (bill due, amount payable) — not a completed payment.
    private val DUES_NOTICE = Regex(
        """\b(is|are) due\b|\bdue (on|by|date)\b|payment (is )?due|amount due|amt due|\bpayable\b|please pay|kindly pay|do pay|\bpay (now|by|before|your)\b|\brepay\b|last date to pay""",
        RegexOption.IGNORE_CASE,
    )
    // Money that has ACTUALLY left the account (past tense / completed).
    private val DEBIT_DONE = Regex(
        """\b(spent|withdrawn|purchased?|debited|deducted|paid|sent|transferred|w/?d)\b""",
        RegexOption.IGNORE_CASE,
    )

    // ---- Reminder / dues parsing (the Dues page) ----
    // Promo / OTP / phishing text that must never become a bill reminder.
    private val NOT_REMINDER = Regex(
        """\b(otp|one[- ]time password|do not share|offer|pre-?approved|loan offer|cashback|apply now|pre-?qualified|eligible for|lottery|lucky|have won|claim|kyc|vvip|astro|bodhi|declined|failed|unsuccessful|up ?to rs|save \d{1,2}%|limit[^.]{0,25}(changed|revised|enhanced|increased))\b""",
        RegexOption.IGNORE_CASE,
    )
    // An amount to ignore when picking the bill amount (a balance, limit or min-due figure).
    private val REM_SKIP_AMT = Regex(
        """(min(?:imum)?(\s*(amt|amount|due))?|avl|avbl|available|balance|\bbal\b|limit|o/s|outstanding)[^.]{0,10}$""",
        RegexOption.IGNORE_CASE,
    )
    private val MIN_AMT = Regex(
        """min(?:imum)?\s*(?:amt|amount)?\s*(?:due)?\s*[:\-]?\s*(?:Rs\.?|INR|₹)\s*:?\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE,
    )
    private val RECURRING = Regex(
        """\b(emi|sip|auto ?pay|autopay|auto[- ]?debit|e-?mandate|nach|standing instruction|mandate|subscription|monthly|every month|recurring|renewal|renews)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val DUE_KW = Regex("""\b(due(?:\s*(?:on|date|by))?|pay(?:able)?\s*by|last date(?:\s*to pay)?)\b""", RegexOption.IGNORE_CASE)
    private val DATE_NAMED = Regex("""\b(\d{1,2})[-/ ]([A-Za-z]{3,9})[-/ ]?(\d{2,4})?\b""")
    private val MONTHS = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
    )

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

    // ---- Anti-phishing gate (Ingest calls these BEFORE a message is parsed) ----
    // Step 1: is the sender a bank/financial sender ID? A genuine bank/UPI alert
    // carries a bank code (HDFCBK, ICICIB, SBIINB…) or a financial token. Personal
    // phone numbers and promo/scam senders (astrology apps, lotteries, fake KYC) do not.
    // Matched as substrings because sender headers are smooshed (e.g. "VM-HDFCBK").
    private val FIN_TOKEN = Regex("""(BANK|BNK|BK|UPI|PAYTM|CARD|FINSERV|FINANC|NBFC|CRED|FINO|INB|MUDRA|EPF|EPFO|UAN|NSDL|NPS|PFRDA|PRAN|CAMS|KFIN|CDSL|MFUT|DEPOS|INVEST|SEBI)""", RegexOption.IGNORE_CASE)

    // Senders you told Zelu are your bank (Menu > Messages > Unrecognised senders). Loaded by Storage.loadTrust.
    @Volatile var trusted: Set<String> = emptySet()
    // Of those, the ones you said are wallets (their spends are not counted again) or something else.
    @Volatile var walletSenders: Set<String> = emptySet()
    @Volatile var otherSenders: Set<String> = emptySet()

    fun isFinancialSender(address: String): Boolean {
        val a = address.uppercase()
        if (a in trusted) return true
        if (a.none { it.isLetter() }) return false          // plain phone number -> personal
        if (SENDER_CODES.any { a.contains(it.first) }) return true
        if (BANK.containsMatchIn(a)) return true
        return FIN_TOKEN.containsMatchIn(a)
    }

    // Step 2: banks never send URL shorteners or throwaway short links. A message
    // that carries one is spoofed / phishing even if the sender header looks bank-like.
    private val SHORTENER = Regex("""\b(bit\.ly|tinyurl|rb\.gy|cutt\.ly|ow\.ly|is\.gd|t\.me|wa\.me|t\.co|goo\.gl|tiny\.cc|bit\.do|rebrand\.ly|shorturl|shrtsms|hdfcbk\.io|clck\.ru|short\.gy)\b""", RegexOption.IGNORE_CASE)
    // A generic short link: scheme + a very short host + a short path code (bit.ly/aB3x).
    private val SHORT_LINK = Regex("""https?://[a-z0-9-]{1,8}\.[a-z]{2,4}/[A-Za-z0-9]{3,}""", RegexOption.IGNORE_CASE)

    // Banks' own short-link domains. ICICI ends every card alert with an icici.co/... link,
    // which SHORT_LINK would otherwise flag; links on these exact hosts are ignored.
    private val TRUSTED_LINK = Regex("""https?://(?:www\.)?(?:icici\.co|icicibank\.com)/\S*""", RegexOption.IGNORE_CASE)
    // Any other bank's own link is fine too when its name is part of the sender ID (hdfcbk.io from
    // HDFCBK, axisbk.com from AXISBK). Those are removed before the shortener check.
    private val ANY_LINK = Regex("""(?:https?://)?(?:www\.)?([a-z0-9-]+)\.[a-z]{2,4}/\S*""", RegexOption.IGNORE_CASE)

    fun hasSuspiciousLink(body: String, address: String = ""): Boolean {
        val sender = address.uppercase().filter { it.isLetterOrDigit() }
        var rest = TRUSTED_LINK.replace(body, " ")
        if (sender.isNotEmpty()) rest = ANY_LINK.replace(rest) { m ->
            val label = m.groupValues[1].uppercase().replace("-", "")
            if (label.length >= 4 && sender.contains(label)) " " else m.value
        }
        return SHORTENER.containsMatchIn(rest) || SHORT_LINK.containsMatchIn(rest)
    }

    // A real account/card tail: exactly 4 digits anchored to a mask (XX5997, ****9065)
    // or an explicit a/c|account|card keyword. Avoids grabbing stray 3-digit numbers.
    // A masked account tail: one or more mask chars then 4 digits (*5997, **2931, XX5997).
    private val LAST4_MASK = Regex("""(?<![A-Za-z0-9])[xX*#]+\s*(\d+)(?!\d)""")
    private val LAST4_KW = Regex("""(?:a/?c|acct|account|card)\s*(?:no\.?|number|ending(?:\s*(?:with|in))?|is)?\s*[xX*#]*\s*(\d+)(?!\d)""", RegexOption.IGNORE_CASE)
    // Your account is the one money leaves FROM (debit) or lands in / TO (credit) — never the
    // other party's account. "debited from a/c *5997 ... to a/c **2931" -> your a/c is 5997.
    // Matches "from a/c *5997", "from HDFC Bank XX5997", etc. (a/c word optional if masked).
    private val FROM_AC = Regex("""\b(?:from|frm)\b[A-Za-z:. ]{0,22}?(?:(?:a/?c|acct|account)\s*(?:no\.?|number)?\s*[xX*#]*|[xX*#]+)\s*(\d+)(?!\d)""", RegexOption.IGNORE_CASE)
    private val TO_AC = Regex("""\b(?:to|into|in)\b[A-Za-z:. ]{0,22}?(?:(?:a/?c|acct|account)\s*(?:no\.?|number)?\s*[xX*#]*|[xX*#]+)\s*(\d+)(?!\d)""", RegexOption.IGNORE_CASE)
    // A credit card is marked by an explicit "credit card" or by a spending LIMIT (debit
    // cards / bank accounts show a balance, never a limit).
    private val IS_CARD = Regex("""\b(credit card|\bcc\b|card ending|on card|your card|avl lmt|avl limit|available limit|credit limit)\b""", RegexOption.IGNORE_CASE)
    // A debit card is linked to a bank account, not a separate credit-card bill.
    private val DEBIT_CARD = Regex("""\b(debit card|\bdc\b)\b""", RegexOption.IGNORE_CASE)
    // Digits that are NOT an account: a mobile number ("linked to mobile number XXXXX64606"), a reference number.
    private val PHONE_NOISE = Regex("""\b(?:mobile|mob|phone|ph|contact|cell|registered)\s*(?:no\.?|number|num|#)?\s*[:.\-]?\s*\+?\d?[xX*#]*\s*[\d ]{4,}""", RegexOption.IGNORE_CASE)
    private val REF_NOISE = Regex("""\b(?:ref(?:erence)?|utr|rrn|txn|trans(?:action)?|order|id|otp)\s*(?:no\.?|number|id|#)?\s*[:.\-]?\s*[A-Za-z]{0,6}\d{5,}""", RegexOption.IGNORE_CASE)
    private val AC_KEYWORD = Regex("""\b(a/?c|acct|account)\b""", RegexOption.IGNORE_CASE)
    private val DATE = Regex("""\b(\d{1,2})[-/](\d{1,2})[-/](\d{2}|\d{4})\b""")
    private val CP = Regex(
        """\b(?:to|from|by|at)\s+(?:VPA\s+)?([\w.\-]+@[\w.\-]+|[A-Za-z][A-Za-z0-9 .&'\-]{1,40}?)(?=\s+(?:on|upi|ref|via|of|rs|inr)\b|[.,;]|$)""",
        RegexOption.IGNORE_CASE,
    )
    // Fallback merchant: NAME right after the amount, e.g. "INR2100.76 TOIT BREWPUB."
    private val MERCHANT_AFTER = Regex("""(?:Rs\.?|INR|₹)\s*:?\s*[\d,]+(?:\.\d{1,2})?\s+(?:to\s+|at\s+)?([A-Za-z][A-Za-z0-9 .&'\-]{1,40}?)(?=\s+(?:on|upi|ref|via|avbl|available|bal)\b|[.,;]|$)""", RegexOption.IGNORE_CASE)

    // Promotional / instructional phrases that must never be treated as a payee.
    private val CP_JUNK = Regex("""^(register|continue|enjoy|enjoying|setup|set|click|share|download|restart|your|is|was|will|now|pay|visit|call|the|update|info|dear|team|please|to|thank|get|go|use|tap|view|check|received|debited|credited|payment|paid|spent|sent|txn|transaction|amount|rs|inr|bal|balance|avl|available|successful|successfully|a/c|acct|account|report|block|dispute|verify|helpline|sms|transfer|transferred|a year|a month|year|month|months|annual|debit|debited|vide|clearing|has|have|we|hi|dear)\b""", RegexOption.IGNORE_CASE)

    // Union Bank style: "... by Mob Bk ref no 1896..., Fvg: NAGESH A Avl Bal ..." names the payee after Fvg.
    private val FVG_PAYEE = Regex("""\b(?:fvg|favou?ring)\s*:?\s*([A-Za-z][A-Za-z .]{1,30}?)(?=\s+(?:avl|bal|not|ref|on)\b|[.,;]|$)""", RegexOption.IGNORE_CASE)

    // ICICI account alerts name the payee between a semicolon and "credited": "...on 11-Oct-26; Akshay Medical credited."
    private val SEMI_PAYEE = Regex("""[;:]\s*([A-Za-z][A-Za-z0-9 .&'\-]{1,40}?)\s+credited\b""", RegexOption.IGNORE_CASE)
    // Bill-desk style narration: "InfoBIL*INFT*FJ27" is a bill paid through BillDesk.
    private val BILLDESK = Regex("""\b(?:infobil|billdesk|billdk)\b""", RegexOption.IGNORE_CASE)
    // A date is never a payee ("on 11-Oct-26").
    private val DATE_LIKE = Regex("""^(?:on\s+)?\d{1,2}[-/ ][A-Za-z0-9]{2,9}[-/ ]\d{2,4}$""", RegexOption.IGNORE_CASE)

    // ACH / NACH mandate debits name the biller after "ACH D-" or "ACH/".
    private val ACH_PAYEE = Regex("""ACH[ /]?D?[-/ ]+([A-Za-z][A-Za-z .&]{2,40}?)[-/]""", RegexOption.IGNORE_CASE)
    // NEFT/IMPS/RTGS strings name the sender after the bank code: "NEFT Cr-BOFA0CN6215-QUALCOMM INDIA PVT LTD-…".
    private val NEFT_SENDER = Regex("""(?:NEFT|IMPS|RTGS)[^-]{0,12}[-:]\s*[A-Za-z0-9]{6,}\s*[-]\s*([A-Za-z][A-Za-z .&]{2,40}?)\s*[-]""", RegexOption.IGNORE_CASE)

    // A loan EMI debit notice (NACH / ACH / e-mandate / installment). Captured as a real
    // monthly expense even though it reads like a reminder; de-duplicated to once a month.
    private val EMI_DEBIT = Regex("""\b(nach|ach|e-?mandate|installment|instalment|loan a/?c|loan ac|emi)\b.{0,70}\b(debit|debited|deduct|deducted)\b|\b(debit|debited|deduct|deducted)\b.{0,50}\b(nach|ach|emi|loan|installment|instalment)\b""", RegexOption.IGNORE_CASE)
    // "Your Loan A/c *0126 Credited for Rs:72894.00 ... by BRANCH": the bank took the EMI from your own
    // account and the loan account received it. That is an EMI paid (money out), never money in.
    private val LOAN_CREDIT = Regex("""loan\s*a/?c.{0,40}\b(credited|credit)\b""", RegexOption.IGNORE_CASE)
    private val LOAN_AC = Regex("""loan\s*a/?c(?:\s*no\.?)?\s*[xX*#]*\s*(\d{3,4})""", RegexOption.IGNORE_CASE)

    // Canonical merchant names, so "ZOMATO LTD" / "Zomato Media Pvt Ltd" group as one payee.
    private val MERCHANT_MAP: List<Pair<Regex, String>> = listOf(
        Regex("""jio\s*fiber|jiofiber|jiohome""", RegexOption.IGNORE_CASE) to "JioFiber",
        Regex("""\bjio\b""", RegexOption.IGNORE_CASE) to "Jio",
        Regex("""zomato""", RegexOption.IGNORE_CASE) to "Zomato",
        Regex("""swiggy|bundl""", RegexOption.IGNORE_CASE) to "Swiggy",
        Regex("""blinkit""", RegexOption.IGNORE_CASE) to "Blinkit",
        Regex("""zepto""", RegexOption.IGNORE_CASE) to "Zepto",
        Regex("""bigbasket|big ?basket|bbnow|innovative retail""", RegexOption.IGNORE_CASE) to "BigBasket",
        Regex("""avenue supermart|\bdmart\b|d mart""", RegexOption.IGNORE_CASE) to "DMart",
        Regex("""q ?mart retail|\bqmart\b""", RegexOption.IGNORE_CASE) to "Q Mart",
        Regex("""pure o natural""", RegexOption.IGNORE_CASE) to "Pure O Natural",
        Regex("""eatsure""", RegexOption.IGNORE_CASE) to "EatSure",
        Regex("""jubilant food|\bdominos\b|domino's""", RegexOption.IGNORE_CASE) to "Domino's",
        Regex("""pizza ?hut""", RegexOption.IGNORE_CASE) to "Pizza Hut",
        Regex("""\badobe\b""", RegexOption.IGNORE_CASE) to "Adobe",
        Regex("""qualcomm""", RegexOption.IGNORE_CASE) to "Qualcomm Salary",
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
        // Only count money that has actually left the account. A future auto-debit
        // ("will be debited") or a pure dues/reminder notice (no completed payment in it)
        // never counts. A completed debit that merely MENTIONS a future due date still counts.
        val future = FUTURE_DEBIT.containsMatchIn(text)
        val completed = !future && DEBIT_DONE.containsMatchIn(text)
        val duesOnly = DUES_NOTICE.containsMatchIn(text) && !completed
        // A completed EMI/NACH debit survives SKIP; a future or dues-only EMI notice does not.
        val loanCredit = LOAN_CREDIT.containsMatchIn(text) && !future && !duesOnly &&
            !text.contains("disburs", ignoreCase = true)
        val isEmiDebit = (EMI_DEBIT.containsMatchIn(text) || loanCredit) && !future && !duesOnly
        if (text.isEmpty() || (SKIP.containsMatchIn(text) && !isEmiDebit)) return null
        if (future || duesOnly) return null

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
        if (direction == "credit" && (isOutgoingPayment(text) || INVEST_OUT.containsMatchIn(text))) direction = "debit"
        if (loanCredit) direction = "debit"

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
            counterparty = FVG_PAYEE.find(text)?.groupValues?.get(1)?.let { titleCase(it.trim()) }
        }
        if (counterparty == null && BILLDESK.containsMatchIn(text)) counterparty = "BillDesk"
        if (counterparty == null) {
            counterparty = SEMI_PAYEE.find(text)?.groupValues?.get(1)?.let { cleanCp(it) }
                ?.let { if (it == it.uppercase()) titleCase(it) else it }
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

        // A loan-account credit names the loan (*0126), not your account: use the bank alone, which folds
        // into that bank's main savings account.
        val account = if (loanCredit) accountOf(text, source, direction).substringBefore(' ')
        else accountOf(text, source, direction)

        val id = if (ref != null) "ref-$ref"
        else "sms-" + hash("$date|$amount|$direction|$text")

        return Txn(id, date, amount, direction, counterparty, ref, text, source, account)
    }

    // For the "add by hand" screen: the first rupee amount in a message, and the account it names.
    fun firstAmount(body: String): Double? =
        AMOUNT.find(WS.replace(body, " "))?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

    fun accountFor(body: String, source: String, direction: String): String =
        accountOf(WS.replace(body, " ").trim(), source, direction)

    // ---- UPI AutoPay / mandates ----
    private val M_WORD = Regex("""\b(upi[- ]?(?:auto ?pay|mandate)|auto ?pay|e-?mandate|mandate)\b""", RegexOption.IGNORE_CASE)
    private val M_END = Regex("""\b(revok(?:ed|e)|cancel+ed|deleted|removed|expired|terminated|declined|rejected|failed|unsuccessful|closed)\b""", RegexOption.IGNORE_CASE)
    private val M_PAUSE = Regex("""\bpaus(?:ed|e)\b""", RegexOption.IGNORE_CASE)
    private val M_NEW = Regex("""\b(created|registered|activated|approved|set ?up|successfully|resumed|modified|accepted)\b""", RegexOption.IGNORE_CASE)
    private val M_SCHED = Regex("""\b(scheduled|will be debited|pre-?debit|upcoming)\b""", RegexOption.IGNORE_CASE)
    private val M_OTP = Regex("""\b(otp|one[- ]time password|offer|cashback|click|apply now)\b""", RegexOption.IGNORE_CASE)
    private val M_NAME = listOf(
        Regex("""towards\s+(.+?)\s*(?:auto ?pay\b|from\b|for\b|of\b|\.|,|$)""", RegexOption.IGNORE_CASE),
        Regex("""auto ?pay\s+for\s+(.+?)\s+(?:debit|of|is|has|for|from)\b""", RegexOption.IGNORE_CASE),
        Regex("""mandate\s+(?:on|for|with|to|towards)\s+(.+?)\s+(?:for|of|from|is|has|worth|rs|inr|₹)\b""", RegexOption.IGNORE_CASE),
        Regex("""merchant\s*(?:name)?\s*[:\-]\s*(.+?)(?:\.|,|\s{2}|$)""", RegexOption.IGNORE_CASE),
    )
    private val M_ON = Regex("""\bon\s*\.?\s*(\d{1,2})[-/](\d{1,2})[-/](\d{2}|\d{4})\b""", RegexOption.IGNORE_CASE)

    fun parseMandate(body: String, receivedMs: Long?, source: String): Mandate? {
        val text = WS.replace(body, " ").trim()
        if (text.isEmpty() || !M_WORD.containsMatchIn(text) || M_OTP.containsMatchIn(text)) return null
        val status = when {
            M_END.containsMatchIn(text) -> "ended"
            M_PAUSE.containsMatchIn(text) -> "paused"
            M_SCHED.containsMatchIn(text) -> "scheduled"
            M_NEW.containsMatchIn(text) -> "active"
            else -> return null
        }
        var name: String? = null
        for (r in M_NAME) {
            val g = r.find(text)?.groupValues?.get(1)?.replace("<", "")?.replace(">", "")?.trim(' ', '.', ',', ':', '-') ?: continue
            if (g.length in 2..90 && !g.equals("upi", true)) { name = g; break }
        }
        name ?: return null
        val merchant = if (name == name.uppercase()) titleCase(name) else name
        val amount = AMOUNT.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        val next = if (status == "scheduled") M_ON.find(text)?.let { m ->
            try {
                val dd = m.groupValues[1].toInt(); val mm = m.groupValues[2].toInt()
                var yy = m.groupValues[3].toInt(); if (yy < 100) yy += 2000
                val cal = GregorianCalendar(IST); cal.isLenient = false; cal.clear(); cal.set(yy, mm - 1, dd)
                fmt(cal.timeInMillis)
            } catch (_: Exception) { null }
        } else null
        val ms = receivedMs ?: System.currentTimeMillis()
        val account = accountOf(text, source, "debit")
        return Mandate(merchant.lowercase().replace(Regex("[^a-z0-9]"), ""), merchant, amount, account, if (status == "scheduled" && next == null) "active" else status, next, ms, ms, text, source)
    }

    // Parse a bill / payment reminder (a "due" or "will be debited" notice) into a Reminder.
    // Called by Ingest only when parseSms returned null, so a completed payment is never a reminder.
    fun parseReminder(body: String, receivedMs: Long?, source: String): Reminder? {
        val text = WS.replace(body, " ").trim()
        if (text.isEmpty() || NOT_REMINDER.containsMatchIn(text)) return null
        // Must read like an upcoming / due payment.
        if (!FUTURE_DEBIT.containsMatchIn(text) && !DUES_NOTICE.containsMatchIn(text)) return null

        val amounts = AMOUNT.findAll(text).toList()
        if (amounts.isEmpty()) return null
        // The bill amount: the first figure that isn't a balance / limit / min-due.
        val billAmt = amounts.firstOrNull { m ->
            !REM_SKIP_AMT.containsMatchIn(text.substring(0, m.range.first))
        } ?: amounts.first()
        val amount = billAmt.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        val minAmount = MIN_AMT.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        val dueDate = parseDueDate(text) ?: parseDate(text, receivedMs)
        val recurring = RECURRING.containsMatchIn(text)

        var cp = canonicalMerchant(text)
            ?: ACH_PAYEE.find(text)?.groupValues?.get(1)?.let { titleCase(it.trim()) }
            ?: CP.findAll(text).firstNotNullOfOrNull { cleanCp(it.groupValues[1]) }
        if (recurring) {
            LOAN_AC.find(text)?.groupValues?.get(1)?.let { ln ->
                val bk = BANK.find(text)?.value?.uppercase()?.replace(" ", "") ?: ""
                cp = (if (bk.isNotEmpty()) "$bk " else "") + "Loan $ln"
            }
        }
        val account = accountOf(text, source, "debit")
        val ref = REF.find(text)?.groupValues?.get(1)
        val id = if (ref != null) "rem-$ref" else "rem-" + hash("$dueDate|$amount|$text")
        return Reminder(id, dueDate, amount, minAmount, cp, account, recurring, text, source)
    }

    // The date following a "due on / due by / payable by / last date" keyword — numeric
    // (02/11/26) or named-month (02 Nov 26). Null if none found.
    private fun parseDueDate(text: String): String? {
        val kw = DUE_KW.find(text) ?: return null
        val after = text.substring((kw.range.last + 1).coerceAtMost(text.length)).take(32)
        DATE.find(after)?.let { m ->
            return try {
                val dd = m.groupValues[1].toInt(); val mm = m.groupValues[2].toInt()
                var yy = m.groupValues[3].toInt(); if (yy < 100) yy += 2000
                val cal = GregorianCalendar(IST); cal.isLenient = false; cal.clear(); cal.set(yy, mm - 1, dd)
                fmt(cal.timeInMillis)
            } catch (_: Exception) { null }
        }
        DATE_NAMED.find(after)?.let { m ->
            val mm = MONTHS[m.groupValues[2].lowercase().take(3)] ?: return null
            val dd = m.groupValues[1].toInt()
            var yy = m.groupValues[3].toIntOrNull() ?: fmt(System.currentTimeMillis()).substring(0, 4).toInt()
            if (yy < 100) yy += 2000
            return try {
                val cal = GregorianCalendar(IST); cal.isLenient = false; cal.clear(); cal.set(yy, mm - 1, dd)
                fmt(cal.timeInMillis)
            } catch (_: Exception) { null }
        }
        return null
    }

    // Balance-only alerts ("Available Bal in HDFC Bank A/c XX5997 ... is INR 77,617.50").
    // Returns (account label, balance, received ms) or null if this isn't a balance message.
    private val BAL_WORD = Regex("""\b(avail(?:able)?\.?\s*bal(?:ance)?|a/?c\s*bal(?:ance)?|current\s*balance|bal(?:ance)?\s*(?:is|:))""", RegexOption.IGNORE_CASE)
    private val BAL_AMT = Regex("""bal(?:ance)?.{0,90}?(?:Rs\.?|INR|₹)\s*:?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val TXN_WORD = Regex("""\b(debited|credited|spent|withdrawn|paid|sent|received|purchase|txn|transaction)\b""", RegexOption.IGNORE_CASE)

    private val TXN_BAL = Regex("""(?:avl|avail(?:able)?)\.?\s*bal(?:ance)?\.?\s*(?:is|:|-)?\s*(?:Rs\.?|INR|₹)?\s*:?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)

    // "Avl Bal Rs 32,855.00" tail on a debit/credit alert (for banks with no daily balance SMS).
    fun balanceFromTxn(body: String): Double? =
        TXN_BAL.find(body.replace("\n", " "))?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

    private val TXN_LIMIT = Regex("""(?:avl|avbl|available)\.?\s*(?:cr(?:edit)?\.?\s*)?(?:lmt|limit)\.?\s*(?:is|:|-)?\s*(?:Rs\.?|INR|₹)?\s*:?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)

    // "Avl Lmt Rs 1,20,000" on a credit-card alert.
    fun limitFromTxn(body: String): Double? =
        TXN_LIMIT.find(body.replace("\n", " "))?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

    private val CARD_DIGITS = Regex("""(?:card|ending(?:\s*with)?)\s*(?:no\.?\s*)?[\dXx*]*?(\d{4})\b""", RegexOption.IGNORE_CASE)
    private val CARD_TOTAL_LIMIT = Regex("""credit\s*limit.{0,80}?changed\s+from\s+(?:INR|Rs\.?|₹)?\s*[\d,]+(?:\.\d+)?\s+to\s+(?:INR|Rs\.?|₹)?\s*([\d,]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)

    // Any bank SMS (payment received, limit change, enquiry reply...) that states a card's limit.
    // Returns (account label, amount, isTotalLimit). Available limit beats total limit in the UI.
    fun cardLimit(body: String, source: String): Triple<String, Double, Boolean>? {
        val text = body.replace("\n", " ")
        val avail = TXN_LIMIT.find(text)?.groupValues?.get(1)
        val total = if (avail == null) CARD_TOTAL_LIMIT.find(text)?.groupValues?.get(1) else null
        val amt = (avail ?: total)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val last4 = CARD_DIGITS.find(text)?.groupValues?.get(1) ?: return null
        var bank = BANK.find(text)?.value?.uppercase()?.replace(" ", "") ?: ""
        if (bank == "YESBANK") bank = "YES"
        if (bank.isEmpty()) {
            val src = source.uppercase()
            bank = SENDER_CODES.firstOrNull { src.contains(it.first) }?.second ?: ""
        }
        if (bank.isEmpty()) return null
        return Triple("$bank CC $last4", amt, avail == null)
    }

    // A message about a LOAN account ("Your Loan A/c *0126 Credited ... Avl Bal Rs:32855"): its balance is what is
    // left of the loan, not money in any account of yours, so it must never become an account balance.
    fun isLoanAccountText(body: String): Boolean {
        val t = body.replace("\n", " ")
        return LOAN_CREDIT.containsMatchIn(t) || LOAN_AC.containsMatchIn(t)
    }

    fun parseBalance(body: String, receivedMs: Long?, source: String): Triple<String, Double, Long>? {
        val text = body.replace("\n", " ").trim()
        if (isLoanAccountText(text)) return null
        if (!BAL_WORD.containsMatchIn(text) || TXN_WORD.containsMatchIn(text)) return null
        val amt = BAL_AMT.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val acct = accountOf(text, source, "")
        if (acct.isEmpty()) return null
        return Triple(acct, amt, receivedMs ?: 0L)
    }

    // Best-effort account label, e.g. "HDFC 5997" or "HDFC CC 1291".
    private fun accountOf(rawText: String, source: String, direction: String): String {
        val text = REF_NOISE.replace(PHONE_NOISE.replace(rawText, " "), " ")
        // The sender id names the bank first; only when it doesn't, the message text does.
        val src = source.uppercase()
        var bank = SENDER_CODES.firstOrNull { src.contains(it.first) }?.second ?: ""
        if (bank.isEmpty()) bank = BANK.find(rawText)?.value?.uppercase()?.replace(" ", "") ?: ""
        if (bank == "YESBANK") bank = "YES"
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
            ?: LAST4_KW.find(text)?.groupValues?.get(1)
            ?: LAST4_MASK.find(text)?.groupValues?.get(1) ?: ""
        // A full account number is shown by its end; a masked tail is kept as the bank wrote it, whatever its length.
        if (last4.length > 6) last4 = last4.takeLast(4)
        // "Loan A/c *0126" names the LOAN's number: that is not a savings account of yours.
        LOAN_AC.find(text)?.groupValues?.get(1)?.let { ln -> if (last4.isNotEmpty() && (last4 == ln || ln.endsWith(last4) || last4.endsWith(ln))) last4 = "" }
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
        if (DATE_LIKE.matches(s) || s.matches(Regex("""(?i)^on\s+\d.*"""))) return null
        if (CP_JUNK.containsMatchIn(s)) return null
        // A bank/account/card string (bank name plus digits) is your own account, not a payee.
        if (BANK.containsMatchIn(s) && s.any { it.isDigit() }) return null
        return s
    }

    private val SUG_JUNK = setOf("rs", "inr", "upi", "call", "sms", "block", "acct", "acc", "account", "bank", "avl", "bal", "available",
        "balance", "debited", "credited", "dispute", "not", "you", "your", "info", "ref", "refund", "dear", "customer", "card", "txn",
        "transaction", "payment", "paid", "sent", "received", "transfer", "thru", "through", "via", "vide", "the", "for", "from", "with",
        "and", "total", "limit", "lmt", "convert", "missed", "know", "more", "emi", "conversion", "mob", "bk", "dont", "share", "pin", "cvv",
        "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec", "xx", "to", "on", "at", "by")
    private val CAP_SEQ = Regex("""\b([A-Z][A-Za-z]{2,}(?: [A-Z][A-Za-z]{2,}){0,3})\b""")
    private val CAMEL = Regex("""\b([a-z]+[A-Z][A-Za-z]+)\b""")
    private val VPA = Regex("""\b([A-Za-z][\w.\-]{2,})@[A-Za-z][\w.\-]*\b""")

    // Names that could be the payee, read out of the SMS, best guesses first. Offered when a title looks wrong.
    fun suggestTitles(body: String, current: String?): List<String> {
        val text = WS.replace(body, " ").trim()
        val found = ArrayList<String>()
        fun add(raw: String?) {
            var s = cleanCp(raw) ?: return
            s = s.split("*").first().trim()
            if (s.length < 3 || DATE_LIKE.matches(s)) return
            val words = s.split(" ").filter { it.isNotBlank() }
            if (words.any { it.lowercase() in SUG_JUNK } || BANK.matches(s)) return
            if (s == s.uppercase() && s.any { it.isLetter() }) s = titleCase(s)
            if (found.none { it.equals(s, ignoreCase = true) } && !s.equals(current?.trim(), ignoreCase = true)) found.add(s)
        }
        if (BILLDESK.containsMatchIn(text)) add("BillDesk")
        SEMI_PAYEE.find(text)?.let { add(it.groupValues[1]) }
        FVG_PAYEE.find(text)?.let { add(it.groupValues[1]) }
        ACH_PAYEE.find(text)?.let { add(it.groupValues[1]) }
        NEFT_SENDER.find(text)?.let { add(it.groupValues[1]) }
        CP.findAll(text).forEach { add(it.groupValues[1]) }
        VPA.findAll(text).forEach { add(it.groupValues[1]) }
        CAP_SEQ.findAll(text).forEach { add(it.groupValues[1]) }
        CAMEL.findAll(text).forEach { add(it.groupValues[1]) }
        return found.take(6)
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
