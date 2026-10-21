package com.koolicream.zelu

import org.junit.Assert.assertTrue
import org.junit.Test

// End-to-end check of 44 realistic SMS through the same steps as Ingest: sender check, link check, parse, classify.
// Every message must come out with the right direction, amount, kind and category (or be ignored). The build fails
// otherwise, and the failure lists each message that was read wrongly.
class SmsPipelineTest {
    private val received = 1792300000000L // 2026-10-18 IST

    private data class Case(val id: String, val sender: String, val body: String, val expect: String)
    // expect: "ignore" | "debit <amount> <kind>/<category>" | "credit <amount> <kind>/<category>" (category "*" = any)

    private val cases = listOf(
        // --- UPI / card debits, banks and formats beyond the regression set ---
        Case("hdfc-upi-multiline", "AX-HDFCBK-S", "Sent Rs.250.00\nFrom HDFC Bank A/C *5997\nTo BLINKIT\nOn 18/10/26\nRef 629112345678\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808", "debit 250 expense/Groceries"),
        Case("sbi-upi-swiggy", "VM-SBIUPI-S", "Dear UPI user A/C X1234 debited by 150.0 on date 18Oct26 trf to SWIGGY Refno 629123456789 If not u? call -1800111109 for other services-18001234-SBI", "debit 150 expense/Food"),
        Case("sbi-pos-dmart", "VM-SBIINB-S", "Your A/C XXXXX1234 Debited INR 2,499.00 on 17/10/26 -Transferred to DMART. Avl Balance INR 12,345.00-SBI", "debit 2499 expense/Groceries"),
        Case("icici-upi-zepto", "AD-ICICIT-S", "ICICI Bank Acct XX345 debited for Rs 120.00 on 18-Oct-26; ZEPTO MARKETPLACE credited. UPI:629234567890. Call 18002662 for dispute. SMS BLOCK 345 to 9215676766.", "debit 120 expense/Groceries"),
        Case("axis-upi-rapido", "VK-AXISBK-S", "INR 89.00 debited\nA/c no. XX7788\n18-10-26, 09:15:22\nUPI/P2M/629345678901/RAPIDO\nNot you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank", "debit 89 expense/Transport"),
        Case("kotak-upi-link", "AD-KOTAKB-S", "Sent Rs.45.00 from Kotak Bank AC X9988 to paytmqr1234@paytm on 18-10-26.UPI Ref 629456789012. Not you, https://kotak.com/KBANKT/Fraud", "debit 45 expense/*"),
        Case("paytm-bank-chai", "JD-PAYTMB-S", "Paid Rs.60 to Chai Point from Paytm Payments Bank a/c XX4321. UPI Ref: 629567890123. Not you? Call 01204456456", "debit 60 expense/Food"),
        Case("airtel-bank-ola", "AD-AIRBNK-S", "Rs.500.00 debited from your Airtel Payments Bank a/c for UPI txn to ola@axisbank Ref no 629678901234", "debit 500 expense/Transport"),
        Case("yes-instamart", "VM-YESBNK-S", "INR 1,200.00 debited from YES BANK A/c XX5566 on 18-OCT-2026 towards UPI/629789012345/SWIGGY INSTAMART. Avl Bal INR 3,400.00", "debit 1200 expense/Groceries"),
        Case("indusind-bescom", "VM-INDUSB-S", "Your IndusInd Bank A/C 1234XXXXX5678 has been debited for INR 2,000 towards UPI/629890123456/BESCOM on 18/10/2026. Avl bal INR 9,000", "debit 2000 expense/Bills"),
        Case("federal-jio", "VM-FEDBNK-S", "Rs 300.00 debited from your A/c XX9900 to VPA jio@axisbank on 18-10-2026 UPI Ref 629901234567 -Federal Bank", "debit 300 expense/Bills"),
        Case("hdfc-cc-netflix", "AX-HDFCBK-S", "Rs.1,299.00 spent on HDFC Bank Credit Card XX1291 at NETFLIX ENTERTAINMENT on 2026-10-18:20:15:44. Avl Lmt: Rs.1,40,000.00", "debit 1299 expense/Entertainment"),
        Case("sbicard-link", "AD-SBICRD-S", "Rs.3,450.00 spent on your SBI Credit Card ending 7788 at RELIANCE DIGITAL on 18/10/26. Trxn. not done by you? Report at https://sbicard.com/Dispute", "debit 3450 expense/Shopping"),
        Case("axis-cc-zomato", "VK-AXISBK-S", "Spent INR 550 Axis Bank Card no. XX4455 18-10-26 13:01:22 IST ZOMATO Avl Lmt INR 1,18,670 Not you? SMS BLOCK 4455 to 919951860002", "debit 550 expense/Food"),
        Case("icici-cc-emi-footer", "JM-ICICIT-S", "Rs 7,900.00 spent on ICICI Bank Card XX8003 on 17-Oct-26 at SRI SAI KANCHI . Avl Lmt: Rs 7,12,100.00. To dispute, call 18002662/SMS BLOCK 8003 to 9215676766. To convert this txn to EMI give a missed call on 9924667667. Know more about EMI conversion at https://icici.co/ICICIT/j/2894b29c", "debit 7900 expense/Shopping"),
        Case("icici-cc-amazonpay", "AD-ICICIT-S", "INR 799.00 spent using ICICI Bank Card XX9012 on 18-Oct-26 on AMAZON PAY IN. Avl Limit: INR 2,10,000.00. If not you, call 1800 2662/SMS BLOCK 9012 to 9215676766", "debit 799 expense/*"),
        Case("hdfc-atm", "AX-HDFCBK-S", "Rs.5,000.00 withdrawn from A/c XX5997 at ATM S1AW000123 HSR LAYOUT on 18-10-26. Avl bal Rs.70,000", "debit 5000 expense/ATM / Cash"),
        Case("nach-bajaj-emi", "VK-AXISBK-S", "Your A/c XX7788 has been debited with INR 15,432.00 towards NACH-DR-BAJAJ FINANCE LTD on 05-10-2026", "debit 15432 expense/EMI"),
        Case("sip-zerodha", "AX-HDFCBK-S", "Rs.5,000.00 debited from A/c XX5997 on 05-10-26 towards ACH D- ZERODHA BROKING mutual fund SIP", "debit 5000 invest/*"),
        Case("cred-card-bill", "AX-HDFCBK-S", "Rs.25,000.00 debited from HDFC Bank A/c XX5997 to VPA cred.club@axisbank on 10-10-26 UPI Ref 628812345678", "debit 25000 transfer/Card bill"),
        Case("rent-to-person", "AX-HDFCBK-S", "Sent Rs.18,000.00 From HDFC Bank A/C *5997 To RAJESH KUMAR On 01/10/26 Ref 628723456789 Not You? Call 18002586161", "debit 18000 transfer/*"),
        Case("bbps-electricity", "AX-HDFCBK-S", "Payment of Rs.1,850.00 for BESCOM electricity bill received via HDFC Bank BillPay on 15-10-2026. Ref 628634567890", "debit 1850 expense/Bills"),
        Case("hdfc-cc-fuel", "AX-HDFCBK-S", "Rs.2,000.00 spent on HDFC Bank Card x1291 at HPCL FUEL STATION BLR on 16-10-2026. Avl Lmt: Rs.1,38,000.00", "debit 2000 expense/Fuel"),
        Case("insurance-premium", "AX-HDFCBK-S", "Rs 22,500.00 debited from a/c XX5997 towards HDFC LIFE premium policy 12345 on 12-10-26", "debit 22500 expense/*"),
        Case("fastag-toll", "VM-ICICIT-S", "Rs.135.00 has been deducted from your ICICI Bank FASTag for toll at NELAMANGALA TOLL PLAZA on 18-10-26. Avl bal Rs.450.00", "debit 135 expense/Transport"),
        Case("lowercase-rs", "AX-HDFCBK-S", "rs.99 debited from a/c xx5997 on 18-10-26 for upi/629111122223/rapido", "debit 99 expense/Transport"),
        Case("pnb-pos-medical", "VM-PNBSMS-S", "Your A/c XX0987 is debited for Rs.640.00 on 18-10-2026 at APOLLO PHARMACY POS. Bal Rs.8,100.00 -PNB", "debit 640 expense/Health"),
        // --- credits ---
        Case("salary-neft", "AX-HDFCBK-S", "Rs.1,25,000.00 credited to HDFC Bank A/c XX5997 on 01-10-26 by NEFT from ACME TECHNOLOGIES PVT LTD SALARY OCT", "credit 125000 income/*"),
        Case("card-refund", "AX-HDFCBK-S", "Refund of Rs.499.00 from AMAZON credited to your HDFC Bank Card x1291 on 14-10-26", "credit 499 refund/*"),
        Case("interest", "AX-HDFCBK-S", "Interest of Rs.1,234.00 credited to your SB A/c XX5997 on 30-09-26", "credit 1234 income/*"),
        Case("upi-reversal", "AX-HDFCBK-S", "Rs.300.00 reversed to your a/c XX5997 for failed UPI txn 628745678901 on 12-10-26", "credit 300 refund/*"),
        Case("cc-bill-received", "AD-SBICRD-S", "We have received payment of Rs.15,000.00 towards your SBI Credit Card ending 7788 on 08-10-26. Thank you.", "credit 15000 transfer/Card bill"),
        // --- must be ignored ---
        Case("otp", "AX-HDFCBK-S", "123456 is OTP for txn of INR 2,000.00 at FLIPKART on HDFC Bank card ending 1291. Valid till 10:15. Do not share OTP for security reasons", "ignore"),
        Case("declined", "AX-HDFCBK-S", "Your transaction of Rs.500.00 at SWIGGY using HDFC Bank Card x1291 has been declined due to insufficient limit", "ignore"),
        Case("loan-promo", "AX-HDFCBK-S", "Get a Personal Loan of up to Rs.5,00,000 pre-approved at 10.5% p.a. Apply now: hdfcbk.io/abc", "ignore"),
        Case("collect-request", "AD-GPAYUP-S", "merchant@ybl has requested money Rs.1,500 from you on Google Pay. Approve only if you know them.", "ignore"),
        Case("statement", "AX-HDFCBK-S", "Your HDFC Bank Credit Card statement for Oct-26 has been generated. Total due Rs.12,345.00, Min due Rs.620.00, due by 05-11-26", "ignore"),
        Case("future-autodebit", "AX-HDFCBK-S", "Your Netflix subscription of Rs.649 will be auto-debited on 20-10-26 from HDFC Bank Card x1291", "ignore"),
        Case("phish-bitly", "VK-HDFCBK-S", "Rs.4,999.00 debited from your HDFC a/c. If not you, update KYC at http://bit.ly/hdfc-kyc immediately or account will be blocked", "ignore"),
        Case("personal-number", "+919876543210", "Sent you Rs 500 for dinner, check your account", "ignore"),
        Case("balance-only", "AX-HDFCBK-S", "Available Bal in HDFC Bank A/c XX5997 as on 18-10-26 is INR 79,186.50. Cheques are subject to clearing", "ignore"),
        Case("nbfc-offer", "AD-BAJAJF-S", "Congrats! Rs.2,00,000 pre-approved loan offer waiting for you. Disbursal in 10 mins.", "ignore"),
        Case("mandate-registered", "AX-HDFCBK-S", "E-mandate registered successfully for Rs.15,000 on your HDFC Bank A/c XX5997 for BAJAJ FINANCE. First debit on 05-11-26", "ignore"),
        Case("cashback-offer", "AX-PAYTMB-S", "Win cashback up to Rs.100 on your next UPI payment! Pay now on Paytm.", "ignore"),
    )

    @Test
    fun everyMessageIsReadCorrectly() {
        val wrong = cases.mapNotNull { c ->
            val got = run(c)
            if (matches(c.expect, got)) null else "${c.id}: expected ${c.expect}, got $got"
        }
        assertTrue("Should be read correctly (${wrong.size} of ${cases.size} wrong):\n" + wrong.joinToString("\n"), wrong.isEmpty())
    }

    @Test
    fun mandatePayeeIsTheBillerNotTheMethod() {
        val t = Parser.parseSms(cases.first { it.id == "nach-bajaj-emi" }.body, received, "VK-AXISBK-S")!!
        val payee = t.counterparty.orEmpty()
        assertTrue("payee should name Bajaj Finance, got '$payee'", payee.contains("bajaj", ignoreCase = true) &&
            !payee.startsWith("towards", ignoreCase = true) && !payee.contains("NACH", ignoreCase = true))
    }

    private fun run(c: Case): String {
        if (!Parser.isFinancialSender(c.sender)) return "ignore(sender)"
        if (Parser.hasSuspiciousLink(c.body, c.sender)) return "ignore(link)"
        val t = try { Parser.parseSms(c.body, received, c.sender) } catch (e: Exception) { return "CRASH ${e.javaClass.simpleName}: ${e.message}" }
            ?: return "ignore(parse)"
        val r = try { Classifier.classify(t, emptyList()) } catch (e: Exception) { return "CRASH classify ${e.message}" }
        val amt = if (t.amount % 1.0 == 0.0) t.amount.toLong().toString() else t.amount.toString()
        return "${t.direction} $amt ${r.kind}/${r.category} [acct=${t.account} payee=${t.counterparty} date=${t.date}]"
    }

    private fun matches(expect: String, got: String): Boolean {
        if (expect == "ignore") return got.startsWith("ignore")
        val e = expect.split(" ", limit = 3)
        val g = got.split(" ", limit = 3)
        if (g.size < 3 || e[0] != g[0] || e[1] != g[1]) return false
        val (ek, ec) = e[2].split("/", limit = 2)
        val gkc = g[2].substringBefore(" [")
        val (gk, gc) = gkc.split("/", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        return ek == gk && (ec == "*" || ec == gc)
    }
}
