package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Field-level check of hard and regional-bank SMS: rural (Gramin), co-operative, small-finance and payments banks,
// Dr/Cr abbreviations, odd amount and date formats, charges lines, foreign currency, and messages that must be
// ignored. Each goes through the same steps as Ingest (sender check, link check, parse, classify) and EVERY field is
// compared: direction, amount, account, payee, date, ref, kind, category. Any mismatch fails the build.
class SmsHardFormatsTest {
    private val oct18 = 1792305000000L   // 2026-10-18 12:00 IST, used when a message has no date
    private val jan1 = 1767241800000L    // 2026-01-01 10:00 IST

    // acct: exact label, or a pattern where * means "anything" (e.g. "*3076" = any bank, ends 3076)
    // payee: text the payee must contain (case ignored); "-" = must be empty
    private data class Exp(
        val dir: String, val amt: Double, val acct: String? = null, val payee: String? = null,
        val date: String? = null, val ref: String? = null, val kind: String? = null, val cat: String? = null,
    )
    private data class Case(val id: String, val sender: String, val body: String, val exp: Exp?, val received: Long = 0L, val info: Boolean = false)

    private fun c(id: String, sender: String, body: String, exp: Exp?, received: Long = 0L) = Case(id, sender, body, exp, received)
    private fun info(id: String, sender: String, body: String) = Case(id, sender, body, null, info = true)

    private val cases = listOf(
        // ===== Regional rural (Gramin / Grameena) banks =====
        c("mgb-upi-debit", "VM-MGBBNK-S", "Your A/c No. XXXXXX3076 is debited by Rs.500.00 on 18-10-2026 through UPI Ref No 629412345678. Avl Bal Rs.12,345.67 -Maharashtra Gramin Bank",
            Exp("debit", 500.0, "MGB 3076", date = "2026-10-18", ref = "629412345678", kind = "expense")),
        c("tgb-upi-credit", "VM-TGBANK-S", "Dear Customer, Rs.1,200.00 credited to your A/c XXXX5521 on 18/10/2026 by UPI from SRINIVAS RAO (Ref 629423456789). -Telangana Grameena Bank",
            Exp("credit", 1200.0, "TGB 5521", "srinivas", "2026-10-18", "629423456789", "transfer")),
        c("apgvb-atm", "VM-APGVBK-S", "A/c XX7712 debited Rs 2,000.00 on 18-10-26 for ATM WDL at KURNOOL. Bal Rs 5,430.00 -APGVB",
            Exp("debit", 2000.0, "APGB 7712", date = "2026-10-18", kind = "expense", cat = "ATM / Cash")),
        c("kerala-gramin-pension", "JM-KGBANK-S", "Your SB A/c **4410 is credited by Rs.15,000.00 on 18-10-2026 by NEFT from KERALA STATE ELECTRICITY BOARD-PENSION. Kerala Gramin Bank",
            Exp("credit", 15000.0, "KERALA GRAMIN 4410", date = "2026-10-18", kind = "income")),
        c("baroda-up-medical", "VM-BUPGB-S", "Rs.750.00 debited from A/c XX8834 on 18-10-2026 towards UPI/629434567890/KANPUR MEDICAL STORE. Avl bal Rs.2,100.00 -Baroda UP Bank",
            Exp("debit", 750.0, "BARODA UP 8834", "kanpur medical", "2026-10-18", kind = "expense", cat = "Health")),
        c("vkgb-cr", "VM-VKGBNK-S", "VKGB: A/c XX6620 Cr by Rs.3,000.00 on 18-10-26 through IMPS from SANTOSH PATIL. Avl Bal Rs.8,250.00",
            Exp("credit", 3000.0, "VKGB 6620", "santosh", "2026-10-18", kind = "transfer")),
        // ===== Co-operative banks =====
        c("saraswat-imps", "AX-SARSWT-S", "Dear Customer, your A/c no XXXX9031 has been debited with INR 4,500.00 on 18-Oct-2026 towards IMPS/629445678901/RAVI ELECTRICALS. Saraswat Bank",
            Exp("debit", 4500.0, "SARASWAT 9031", "ravi electricals", "2026-10-18", kind = "expense")),
        c("cosmos-pos-dmart", "VM-COSMOS-S", "INR 1,350.00 debited to A/c XXXX6612 on 18/10/26 for POS txn at D MART PUNE. Avl Bal INR 20,410.00 - Cosmos Bank",
            Exp("debit", 1350.0, "COSMOS 6612", "dmart", "2026-10-18", kind = "expense", cat = "Groceries")),
        c("tjsb-salary", "VM-TJSBNK-S", "A/c XX2290 credited with Rs.25,000.00 on 18-10-2026 through NEFT from TATA CONSULTANCY SERVICES. TJSB Sahakari Bank",
            Exp("credit", 25000.0, "TJSB 2290", "tata consultancy", "2026-10-18", kind = "income")),
        c("svc-debit-card", "VM-SVCBNK-S", "Rs.899.00 spent on your SVC Bank Debit Card XX3301 at AMAZON on 18-10-2026",
            Exp("debit", 899.0, "SVC*", "amazon", "2026-10-18", kind = "expense", cat = "Shopping")),
        c("abhyudaya-upi", "VM-ABHYUD-S", "Your Abhyudaya Bank A/c XX7104 is debited for Rs.210.00 on 18-10-2026 by UPI to ZEPTO MARKETPLACE. Ref 629456789013",
            Exp("debit", 210.0, "ABHYUDAYA 7104", "zepto", "2026-10-18", "629456789013", "expense", "Groceries")),
        // ===== Small finance & payments banks =====
        c("au-upi-swiggy", "VM-AUBANK-S", "Rs 2,500.00 debited from your AU Bank A/c XX1100 on 18-10-26 via UPI to swiggy@icici. UPI Ref 629456789012",
            Exp("debit", 2500.0, "AU 1100", "swiggy", "2026-10-18", "629456789012", "expense", "Food")),
        c("equitas-ola", "VM-EQUTAS-S", "INR 600.00 debited from A/c XX4455 on 18-10-2026 towards UPI/629467890123/OLA. Equitas Small Finance Bank",
            Exp("debit", 600.0, "EQUITAS 4455", "ola", "2026-10-18", kind = "expense", cat = "Transport")),
        c("ujjivan-jio", "VM-UJJIVN-S", "Your Ujjivan SFB A/c XX7781 is debited for Rs.1,000.00 on 18/10/2026 by UPI-629478901234-JIO PREPAID. Bal Rs.3,200.00",
            Exp("debit", 1000.0, "*7781", "jio", "2026-10-18", kind = "expense", cat = "Bills")),
        c("ippb-pm-kisan", "VM-IPBMSG-S", "Your IPPB A/c XXXXXX3342 has been credited with Rs.2,000.00 on 18-10-2026 towards PM KISAN. Avl Bal Rs.2,450.00",
            Exp("credit", 2000.0, "IPPB 3342", date = "2026-10-18", kind = "income")),
        c("fino-cash-wdl", "VM-FINOPB-S", "Rs.1,500.00 withdrawn from your Fino Payments Bank A/c XX9087 at FINO MERCHANT POINT on 18-10-26. Bal Rs.300.00",
            Exp("debit", 1500.0, "FINO 9087", date = "2026-10-18", kind = "expense", cat = "ATM / Cash")),
        c("jana-credit", "VM-JANASF-S", "Rs.4,200.00 credited to your Jana Small Finance Bank A/c XX5110 on 18-10-2026 from ANIL KUMAR via IMPS 629489012346",
            Exp("credit", 4200.0, "JANA 5110", "anil kumar", "2026-10-18", kind = "transfer")),
        // ===== Regional private banks =====
        c("karnataka-bank", "VM-KBLBNK-S", "Your A/c XX1234 debited Rs.320.00 on 18-10-26 via UPI to rapido@ybl Ref 629489012345. -Karnataka Bank",
            Exp("debit", 320.0, "*1234", "rapido", "2026-10-18", "629489012345", "expense", "Transport")),
        c("south-indian-bank", "VM-SIBSMS-S", "Your A/c X7788 is debited for Rs.1,050.00 on 18-10-26 towards UPI/629490123456/BIGBASKET. Bal: Rs.9,950.00 -South Indian Bank",
            Exp("debit", 1050.0, "*7788", "bigbasket", "2026-10-18", kind = "expense", cat = "Groceries")),
        c("kvb-rent", "VM-KVBANK-S", "Rs.12,200.00 debited from your KVB A/c XX6655 on 18/10/2026 for NEFT to LANDLORD RENT OCT. Avl Bal Rs.18,000.00",
            Exp("debit", 12200.0, "KVB 6655", date = "2026-10-18", kind = "expense", cat = "Rent")),
        c("cub-imps-credit", "VM-CUBANK-S", "Dear Customer, INR 5,000.00 credited to your A/c XXXX1209 on 18-10-26 by IMPS from MURUGAN K. Ref 629501234567. -City Union Bank",
            Exp("credit", 5000.0, "CUB 1209", "murugan", "2026-10-18", "629501234567", "transfer")),
        c("tmb-pos-food", "VM-TMBANK-S", "Rs.450.00 Debited from A/c XX3377 on 18-10-2026 at POS SARAVANA BHAVAN CHENNAI. Avl Bal Rs.7,600.00 - TMB",
            Exp("debit", 450.0, "TMB 3377", "saravana", "2026-10-18", kind = "expense", cat = "Food")),
        c("jkbank-power", "VM-JKBANK-S", "Your A/c XXXX5520 debited by INR 1,250.00 on 18-10-2026 via UPI ref 629512345678 to JKPDD ELECTRICITY. Avl Bal INR 11,200.00 -J&K Bank",
            Exp("debit", 1250.0, "JKB 5520", "jkpdd", "2026-10-18", "629512345678", "expense", "Bills")),
        c("dcb-atm", "VM-DCBBNK-S", "INR 3,000.00 debited from DCB Bank A/c XX8899 on 18-10-26 for ATM withdrawal at MUMBAI",
            Exp("debit", 3000.0, "DCB 8899", date = "2026-10-18", kind = "expense", cat = "ATM / Cash")),
        c("hdfc-card-withdrawn", "AX-HDFCBK-S", "Withdrawn Rs.10000 From HDFC Bank Card x1208 At +HAFEEZPET On 2026-04-03:08:40:38 Bal Rs.747635 Not You? Call 18002586161/SMS BLOCK DC 1208 to 7308080808",
            Exp("debit", 10000.0, null, date = "2026-04-03", kind = "expense", cat = "ATM / Cash")),
        c("hdfc-bigtree", "AX-HDFCBK-S", "Sent Rs.452.28 From HDFC Bank A/C *5997 To BIGTREE ENTERTAINMENT PRI On 31/10/26 Ref 235440724631 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808",
            Exp("debit", 452.28, "HDFC 5997", "bigtree", "2026-10-31", "235440724631", "expense", "Entertainment")),
        c("bom-msedcl", "VM-MAHABK-S", "Your A/c No XX4567 is debited with Rs.1,800.00 on 18-10-2026 by UPI-629523456789-MSEDCL. Avl Bal Rs 6,700.00 -Bank of Maharashtra",
            Exp("debit", 1800.0, "BOM 4567", "msedcl", "2026-10-18", kind = "expense", cat = "Bills")),
        c("indian-bank-p2p", "VM-INDBNK-S", "Your a/c no. XXXXXXXX7765 is debited for Rs.250.00 on 18-10-2026 and credited to a/c no. XXXXXXXX1122 (UPI Ref no 629534567890)",
            Exp("debit", 250.0, "INDIAN 7765", date = "2026-10-18", ref = "629534567890")),
        c("central-recharge", "VM-CENTBK-S", "Rs.600.00 debited from a/c **5544 on 18-10-26 by UPI to PHONEPE RECHARGE (Ref 629545678901). Central Bank of India",
            Exp("debit", 600.0, "CENTRAL 5544", date = "2026-10-18", ref = "629545678901", kind = "expense", cat = "Bills")),
        c("uco-dr-abbrev", "VM-UCOBNK-S", "A/c XX9988 Dr INR 1,100.00 on 18-10-26 Info: UPI/629556789012/LIC OF INDIA. Bal INR 2,345.00 -UCO Bank",
            Exp("debit", 1100.0, "UCO 9988", "lic", "2026-10-18", kind = "expense", cat = "Bills")),
        c("iob-cr-abbrev", "VM-IOBCHN-S", "Your A/c XXXX3322 is Cr with Rs.7,500.00 on 18-10-26 by NEFT from ARUN KUMAR. Bal Rs.9,800.00 -IOB",
            Exp("credit", 7500.0, "IOB 3322", "arun", "2026-10-18", kind = "transfer")),
        c("psb-debit", "VM-PSBANK-S", "Punjab & Sind Bank: A/c XX2211 debited INR 2,750.00 on 18-10-2026 for UPI/629567890124/DOMINOS PIZZA. Bal INR 5,000.00",
            Exp("debit", 2750.0, "PSB 2211", "domino", "2026-10-18", kind = "expense", cat = "Food")),
        // ===== Hard formats on big banks =====
        c("lakh-commas-rtgs", "AX-HDFCBK-S", "Rs.1,23,456.78 debited from A/c XX5997 on 18-10-26 towards RTGS to SHARMA BUILDERS. Avl bal Rs.2,00,000.00",
            Exp("debit", 123456.78, "HDFC 5997", "sharma builders", "2026-10-18")),
        c("inr-dot", "AX-HDFCBK-S", "INR.500.00 debited from A/c XX5997 on 18-10-26 for UPI txn to blinkit@hdfcbank Ref 629578901235",
            Exp("debit", 500.0, "HDFC 5997", "blinkit", "2026-10-18", "629578901235", "expense", "Groceries")),
        c("slash-dash-amount", "VM-SBIINB-S", "Rs 1500/- debited from your a/c XX1234 on 18-10-26 for UPI txn to AUTO DRIVER RAJU. Ref 629589012346 -SBI",
            Exp("debit", 1500.0, "SBI 1234", "raju", "2026-10-18", "629589012346")),
        c("charges-gst", "AX-HDFCBK-S", "Rs.5,000.00 transferred via IMPS from A/c XX5997 to RAMESH on 18-10-26. Charges Rs.5.90 incl GST. Ref 629590123457",
            Exp("debit", 5000.0, "HDFC 5997", "ramesh", "2026-10-18", "629590123457")),
        c("balance-first", "AX-HDFCBK-S", "Avl Bal Rs.10,000.00 after debit of Rs.250.00 from A/c XX5997 on 18-10-26 for UPI to blinkit",
            Exp("debit", 250.0, "HDFC 5997", "blinkit", "2026-10-18", kind = "expense", cat = "Groceries")),
        c("named-month-date", "AD-ICICIT-S", "ICICI Bank Acct XX345 debited for Rs 99.00 on 05 Oct 2026; SPOTIFY credited. UPI:629601234568.",
            Exp("debit", 99.0, "ICICI 345", "spotify", "2026-10-05", kind = "expense", cat = "Entertainment")),
        c("iso-date", "VK-AXISBK-S", "INR 1,999.00 spent on Axis Bank Card XX4455 on 2026-10-07 at DECATHLON. Avl Lmt INR 1,10,000",
            Exp("debit", 1999.0, "AXIS CC 4455", "decathlon", "2026-10-07", kind = "expense", cat = "Shopping")),
        c("dotted-date", "VM-PNBSMS-S", "A/c XX0987 debited INR 400.00 on 09.10.2026 UPI/629612345679/APOLLO PHARMACY. Bal INR 9,800.00 -PNB",
            Exp("debit", 400.0, "PNB 0987", "apollo", "2026-10-09", kind = "expense", cat = "Health")),
        c("year-boundary", "AX-HDFCBK-S", "Rs.2,000.00 spent on HDFC Bank Card x1291 at TAJ HOTELS on 31-12-25:23:40:11. Avl Lmt: Rs.1,30,000.00",
            Exp("debit", 2000.0, "HDFC CC 1291", "taj", "2025-12-31", kind = "expense"), received = jan1),
        c("spaced-card-number", "AX-HDFCBK-S", "Txn of INR 1,000.00 done on HDFC Bank Credit Card XXXX XXXX XXXX 1291 at FLIPKART on 18-10-26",
            Exp("debit", 1000.0, "HDFC CC 1291", "flipkart", "2026-10-18", kind = "expense", cat = "Shopping")),
        c("account-ending", "VM-SBIINB-S", "Dear Customer, Rs. 1,000 has been debited from your account ending 1234 on 18-10-26 towards UPI to swiggy. -SBI",
            Exp("debit", 1000.0, "SBI 1234", "swiggy", "2026-10-18", kind = "expense", cat = "Food")),
        c("own-vs-beneficiary", "AX-HDFCBK-S", "A/c XX5997 debited Rs.2,000.00 on 18-10-26 for IMPS to A/c XX1234 (MAHESH JOSHI). Ref 629623456780",
            Exp("debit", 2000.0, "HDFC 5997", "mahesh", "2026-10-18", "629623456780", "expense", "Unknown")),
        c("bank-to-cc-bill", "AX-HDFCBK-S", "Rs.10,000.00 debited from A/c XX5997 towards your ICICI Bank Credit Card XX9012 bill payment on 18-10-26",
            Exp("debit", 10000.0, "HDFC 5997", date = "2026-10-18", kind = "transfer", cat = "Card bill")),
        c("card-reversal", "AX-HDFCBK-S", "Rs.1,299.00 reversed to your HDFC Bank Card x1291 on 18-10-26 for NETFLIX",
            Exp("credit", 1299.0, "HDFC CC 1291", "netflix", "2026-10-18", kind = "refund")),
        c("failed-txn-credit", "AX-HDFCBK-S", "Rs.500.00 credited to A/c XX5997 on 18-10-26 against amount debited for failed UPI txn Ref 629634567891",
            Exp("credit", 500.0, "HDFC 5997", date = "2026-10-18", ref = "629634567891", kind = "refund")),
        c("cash-deposit-cdm", "AX-HDFCBK-S", "Rs.20,000.00 deposited in A/c XX5997 at CDM HSR LAYOUT on 18-10-26. Avl bal Rs.90,000.00",
            Exp("credit", 20000.0, "HDFC 5997", date = "2026-10-18")),
        c("sms-charges", "AX-HDFCBK-S", "Rs.17.70 debited from A/c XX5997 on 18-10-26 towards SMS alert charges for Jul-Sep incl GST",
            Exp("debit", 17.7, "HDFC 5997", date = "2026-10-18", kind = "expense", cat = "Bank charges")),
        // Card fees written as "levied"/"charged" with no "debited", one of them also saying "please pay your dues".
        c("card-late-fee-levied", "AD-AXISBK-S", "Late payment charges of INR 1,200.00 + GST have been levied on your Axis Bank Credit Card XX2290 on 08-10-26. Please pay your dues to avoid further charges.",
            Exp("debit", 1200.0, "*2290", date = "2026-10-08", kind = "expense", cat = "Bank charges")),
        c("card-annual-fee-charged", "VK-KOTAKB-S", "Annual membership fee of Rs.999 + GST has been charged on your Kotak Credit Card XX5521. It will reflect in your next statement.",
            Exp("debit", 999.0, "*5521", kind = "expense", cat = "Bank charges")),
        c("min-balance-charges", "VM-SBIINB-S", "Dear Customer, Rs.354.00 has been debited from your A/c XX6612 towards non-maintenance of minimum average balance charges for Sep-26. -SBI",
            Exp("debit", 354.0, "*6612", kind = "expense", cat = "Bank charges")),
        c("atm-charges", "JD-ICICIT-S", "Dear Customer, ATM transaction charges of Rs 23.60 (incl GST) debited to your ICICI Bank Acct XX219 on 05-Oct-26 for exceeding free transactions.",
            Exp("debit", 23.6, "*219", date = "2026-10-05", kind = "expense", cat = "Bank charges")),
        // Cash from a machine is named "ATM withdrawal", not after the machine's code.
        c("atm-name", "VK-KOTAKB-T", "Rs 5000.00 withdrawn from Kotak Bank a/c XX3381 using Debit Card XX7710 at ATM S1BW000123 THANE on 18-10-2026. Avl bal Rs 23,418.77. Not you? Call 18602662666 - Kotak Bank",
            Exp("debit", 5000.0, "*3381", "ATM withdrawal", "2026-10-18", kind = "expense", cat = "ATM / Cash")),
        // A card swipe at Amazon Pay is shopping, not a wallet top-up.
        c("card-amazon-pay", "JD-ICICIT-S", "INR 2,349.00 spent using ICICI Bank Card XX1291 on 17-Oct-26 on AMAZON PAY IN. Avl Limit: INR 1,84,560.40. If not you, call 1800 2662/SMS BLOCK 1291 to 9215676766.",
            Exp("debit", 2349.0, "*1291", date = "2026-10-17", kind = "expense", cat = "Shopping")),
        c("fee-will-be-levied", "AD-AXISBK-S", "Late payment charges will be levied if the minimum due of INR 920 on your Axis Bank Credit Card XX2290 is not paid by 05-11-26.", null),
        c("fee-waived", "VK-KOTAKB-S", "Good news! The annual fee of Rs.999 on your Kotak Credit Card XX5521 has been waived as you crossed the spend target.", null),
        c("atm-charges-may-apply", "BZ-YESBNK-S", "INR 2,000.00 withdrawn at ATM S1AW0123 from your YES BANK A/c XX7712 on 18-OCT-2026. ATM charges may apply beyond free limit. Avl Bal INR 34,880.21",
            Exp("debit", 2000.0, "*7712", date = "2026-10-18", kind = "expense", cat = "ATM / Cash")),
        c("emi-conversion-offer", "AD-ICICIT-S", "Convert your recent purchase of Rs 7,900 on ICICI Bank Card XX8003 into easy EMIs at 13% p.a. Give a missed call to 9924667667", null),
        c("imps-to-person-masked", "AX-HDFCBK-S", "IMPS Rs.3,000.00 debited from A/c XX5997 to A/c XX4567 (SURESH PATIL) on 19-10-26. IMPS Ref No 629311112222",
            Exp("debit", 3000.0, "HDFC 5997", "suresh", "2026-10-19", "629311112222", "expense", "Unknown")),
        c("fd-booking", "AX-HDFCBK-S", "Rs.50,000.00 debited from A/c XX5997 on 19-10-26 towards Fixed Deposit No XX9988 booked via NetBanking.",
            Exp("debit", 50000.0, "HDFC 5997", date = "2026-10-19", kind = "invest")),
        // Banks write FDs short: "AMT TRF TO FD A.C NO...", and "Term Deposit". Both are investments, not spending or income.
        c("fd-trf-short", "VM-SHANBK-S", "Dear Customer, Chq No.000000 of Rs.500000.00 has been Debited from A/c.xxxx2653 on 24-Dec-2024. Thru AMT TRF TO FD A.C NO200440631000296 L CHEQ NO 347450 @ [200440631000296] Bal: Rs.4110.71 - SHANBK",
            Exp("debit", 500000.0, "2653", date = "2024-12-24", kind = "invest")),
        c("term-deposit-booking", "AX-HDFCBK-S", "Rs.25,000.00 debited from A/c XX5997 on 19-10-26 for Term Deposit booking via NetBanking.",
            Exp("debit", 25000.0, "HDFC 5997", date = "2026-10-19", kind = "invest")),
        // ===== Must be ignored =====
        c("pre-debit-notice", "AX-HDFCBK-S", "Pre-debit notification: Rs.649.00 will be debited on 22-10-26 for NETFLIX via e-mandate from HDFC Bank Card x1291.", null),
        c("zero-verification", "AX-HDFCBK-S", "Your HDFC Bank Card x1291 was used for a txn of Rs.0.00 for card verification at GOOGLE on 18-10-26", null),
        c("promo-credited", "AX-HDFCBK-S", "Get Rs.500 credited to your account on your first UPI payment with HDFC Bank! T&C apply.", null),
        c("pre-auth-block", "AX-HDFCBK-S", "Rs.10,000.00 has been blocked on your HDFC Bank Card x1291 as pre-authorization at TAJ HOTELS. The final amount will be charged at checkout.", null),
        // Tester reports: a payee name that starts with a digit, Axis's "WhatsApp BAL to <number>" footer, and an
        // overdraft warning asking for a minimum credit (no money moved).
        c("axis-upi-digit-payee", "VK-AXISBK-S", "INR 21445.00 debited\nA/c no. XX9116\n08-10-26, 20:42:21\nUPI/P2M/628163353261/101society\nNot you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank",
            Exp("debit", 21445.0, "*9116", "101society", "2026-10-08", kind = "expense", cat = "Home services")),
        c("axis-imps-whatsapp-footer", "VK-AXISBK-S", "Debit INR 50000.00\nAxis Bank A/c XX9116\n23-10-26 12:51:30\nIMPS/P2A/629612194143/ZERO\nWhatsApp BAL to 917036165000\nNot You? SMS BLOCKALL CustID to 919951860002",
            Exp("debit", 50000.0, "*9116", "zero", "2026-10-23")),
        c("society-rwa", "VK-AXISBK-S", "INR 3500.00 debited\nA/c no. XX9116\n05-10-26, 09:12:40\nUPI/P2M/629711112222/SECTOR 21 RWA\nNot you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank",
            Exp("debit", 3500.0, "*9116", "rwa", "2026-10-05", kind = "expense", cat = "Home services")),
        c("society-owners-assn", "VK-AXISBK-S", "INR 6200.00 debited\nA/c no. XX9116\n06-10-26, 10:02:11\nUPI/P2M/629722223333/GREEN VALLEY OWNERS ASSN\nNot you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank",
            Exp("debit", 6200.0, "*9116", "green valley", "2026-10-06", kind = "expense", cat = "Home services")),
        c("icici-od-min-credit", "AX-ICICIT-S", "Credits in ICICI Bank OD XX173 did not cover interest debited in last 90 days & may be tagged as Out of Order. Credit min Rs 72554.34 at https://icici.co/ICICIT/HREcMA", null),
        c("nach-bounce", "VK-AXISBK-S", "Your NACH debit of Rs.15,432.00 towards BAJAJ FINANCE could not be processed due to insufficient balance. Return charges Rs.590 will be levied.", null),
        c("cheque-book", "AX-HDFCBK-S", "Your request for a cheque book for A/c XX5997 has been received and will be dispatched in 7 working days.", null),
        c("loan-sanctioned", "AX-HDFCBK-S", "Congratulations! Your personal loan of Rs.5,00,000 has been sanctioned. Disbursal will happen shortly.", null),
        c("upi-collect-request", "VM-SBIUPI-S", "SURESH has sent a collect request of Rs 1,200.00 on SBI UPI. Approve only if you know the person.", null),
        c("txn-under-process", "AX-HDFCBK-S", "Your IMPS transfer of Rs.4,000.00 to RAMESH is under process. You will be notified once it is completed.", null),
        c("debit-card-blocked", "AX-HDFCBK-S", "Your HDFC Bank Debit Card XX5555 has been blocked as requested. To issue a new card, call 18002586161.", null),
        c("min-balance-warning", "VM-SBIINB-S", "Your A/c X1234 balance is below the minimum required Rs.3,000. Maintain it to avoid charges. -SBI", null),
        c("regional-promo", "VM-MGBBNK-S", "Maharashtra Gramin Bank: Gold loan at 8.9% p.a. up to Rs.10,00,000. Visit your nearest branch.", null),
        // ===== Real payments whose last line reads like an OTP warning, an offer or a reminder =====
        c("debit-card-details-footer", "AX-HDFCBK-S", "Rs.500.00 debited from A/c XX5997 on 18-10-26 to VPA abc@ybl. Do not share your card details or OTP with anyone.",
            Exp("debit", 500.0, "HDFC 5997", date = "2026-10-18", kind = "expense")),
        c("spend-cashback-later", "AX-ICICIB-S", "Rs.1,200 spent on ICICI Bank Card XX1234 at AMAZON on 18-Oct-26. Cashback of Rs.60 will be credited within 3 days.",
            Exp("debit", 1200.0, "ICICI*1234", "amazon", "2026-10-18", kind = "expense", cat = "Shopping")),
        c("debit-next-due-date", "AX-HDFCBK-S", "Rs.899 debited from A/c XX5997 for NETFLIX on 18-10-26. Next due date 18-11-26.",
            Exp("debit", 899.0, "HDFC 5997", "netflix", "2026-10-18", kind = "expense", cat = "Entertainment")),
        c("emi-paid-next-emi", "VM-HDFCBK-S", "EMI of Rs.10,000 for Loan A/c XX4488 has been debited from A/c XX5997 on 05-10-26. Next EMI of Rs.10,000 will be debited on 05-11-26.",
            Exp("debit", 10000.0, "HDFC 5997", date = "2026-10-05")),
        c("refund-to-debit-card", "AX-HDFCBK-S", "Your HDFC Bank Debit Card XX3308 has been credited with Rs.1,200.00 towards refund from AMAZON on 18-10-26.",
            Exp("credit", 1200.0, date = "2026-10-18")),
        c("debit-toll-free-footer", "AX-HDFCBK-S", "Rs.2,499 debited from A/c XX5997 to MYNTRA on 18-10-26. Not you? Call toll free 18002586161.",
            Exp("debit", 2499.0, "HDFC 5997", "myntra", "2026-10-18", kind = "expense", cat = "Shopping")),
        // Still not payments: the debit has not happened yet.
        c("emi-only-upcoming", "VM-HDFCBK-S", "Your EMI of Rs.10,000 for Loan A/c XX4488 will be debited from A/c XX5997 on 05-11-26. Please keep sufficient balance.", null),
        c("otp-for-debit", "AX-HDFCBK-S", "OTP for txn of Rs.2,000.00 at AMAZON on HDFC Bank Card XX1234 is 482913. Do not share it with anyone.", null),
        // ===== Not scored: there is no single right answer; the outcome is just recorded =====
        // ===== Foreign currency and loans =====
        c("foreign-currency", "AX-HDFCBK-S", "USD 25.00 spent on HDFC Bank Card x1291 at NETFLIX.COM on 18-10-26. Avl Lmt: Rs.1,35,000.00",
            Exp("debit", 2100.0, "HDFC CC 1291", "netflix", "2026-10-18", kind = "expense", cat = "Entertainment")),
        c("loan-disbursed", "AX-HDFCBK-S", "Rs.5,00,000.00 disbursed and credited to your A/c XX5997 on 18-10-26 for Loan A/c 7766.",
            Exp("credit", 500000.0, "HDFC 5997", date = "2026-10-18", kind = "transfer", cat = "Loan")),
    )

    @Test
    fun everyFieldIsReadCorrectly() {
        var scored = 0; var passed = 0
        val fieldMiss = LinkedHashMap<String, Int>()
        val sb = StringBuilder("\n===== HARD-FORMAT SMS =====\n")
        for (c in cases) {
            val received = if (c.received != 0L) c.received else oct18
            val stage: String
            var t: Txn? = null
            stage = when {
                !Parser.isFinancialSender(c.sender) -> "ignore(sender)"
                Parser.hasSuspiciousLink(c.body, c.sender) -> "ignore(link)"
                else -> {
                    t = try { Parser.parseSms(c.body, received, c.sender) } catch (e: Exception) { null }
                    if (t == null) "ignore(parse)" else "read"
                }
            }
            val r = t?.let { Classifier.classify(it, emptyList()) }
            val got = if (t == null) stage else
                "${t.direction.key} ${t.amount} acct=[${t.account}] payee=[${t.counterparty ?: ""}] date=${t.date} ref=${t.ref ?: ""} ${r!!.kind.key}/${r.category}"
            if (c.info) { sb.append("INFO ").append(c.id).append(" | got: ").append(got).append('\n'); continue }
            scored++
            val misses = ArrayList<String>()
            val e = c.exp
            if (e == null) {
                if (t != null) misses.add("should be ignored")
            } else if (t == null) {
                misses.add("missed ($stage)")
            } else {
                if (t.direction.key != e.dir) misses.add("dir")
                if (kotlin.math.abs(t.amount - e.amt) > 0.001) misses.add("amount")
                if (e.acct != null && !acctOk(e.acct, t.account)) misses.add("account")
                if (e.payee != null) {
                    val p = t.counterparty.orEmpty()
                    if (if (e.payee == "-") p.isNotEmpty() else !p.contains(e.payee, ignoreCase = true)) misses.add("payee")
                }
                if (e.date != null && t.date != e.date) misses.add("date")
                if (e.ref != null && t.ref != e.ref) misses.add("ref")
                if (e.kind != null && r!!.kind.key != e.kind) misses.add("kind")
                if (e.cat != null && r!!.category != e.cat) misses.add("category")
            }
            misses.forEach { fieldMiss[it.substringBefore(" (")] = (fieldMiss[it.substringBefore(" (")] ?: 0) + 1 }
            if (misses.isEmpty()) passed++
            sb.append(if (misses.isEmpty()) "PASS " else "FAIL ").append(c.id)
            if (misses.isNotEmpty()) sb.append(" [").append(misses.joinToString(",")).append("] expect=").append(c.exp?.toString() ?: "ignore")
            sb.append(" | got: ").append(got).append('\n')
        }
        sb.append("SCORE ").append(passed).append('/').append(scored).append(" | misses by field: ").append(fieldMiss).append('\n')
        sb.append("===== END =====\n")
        println(sb)
        assertEquals("Hard-format SMS read wrongly (see the report above):\n" +
            sb.lines().filter { it.startsWith("FAIL") }.joinToString("\n"), scored, passed)
    }

    @Test
    fun foreignSpendIsMarkedAndEstimated() {
        val raw = "USD 25.00 spent on HDFC Bank Card x1291 at NETFLIX.COM on 18-10-26. Avl Lmt: Rs.1,35,000.00"
        val fx = Parser.foreign(raw)
        assertNotNull("foreign spend should be detected", fx)
        assertEquals("USD", fx!!.code)
        assertEquals(25.0, fx.amount, 0.001)
        assertTrue("rupee amount is an estimate when the bank gave none", fx.estimated)
        // When the bank gives both, the rupee amount is the bank's and is not an estimate.
        val both = Parser.foreign("INR 2,150.00 spent on HDFC Bank Card x1291 at NETFLIX.COM (USD 25.00) on 18-10-26")
        assertNotNull(both); assertTrue(!both!!.estimated)
        assertEquals(2150.0, Parser.parseSms("INR 2,150.00 spent on HDFC Bank Card x1291 at NETFLIX.COM (USD 25.00) on 18-10-26", oct18, "AX-HDFCBK-S")!!.amount, 0.001)
        // An ordinary rupee payment is not foreign.
        assertTrue(Parser.foreign("Rs.250.00 debited from A/c XX5997 to BLINKIT") == null)
    }



    private fun acctOk(pattern: String, actual: String): Boolean {
        if (!pattern.contains('*')) return pattern == actual
        val rx = pattern.split('*').joinToString(".+") { Regex.escape(it) }
        return Regex("^$rx$").matches(actual)
    }
}
