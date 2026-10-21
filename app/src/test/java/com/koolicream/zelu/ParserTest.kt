package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

// A regression set of realistic bank SMS. Every message here once was (or could have been) misread;
// the build fails if a change to the parser reads any of them differently. To add a case, add one line to
// the SMS list: ok(...) for a payment that must be read, none(...) for a message that must be ignored.
class ParserTest {
    // 15 March 2026, 12:00 India time: the "received" time used when a message carries no date of its own.
    private val received = 1773556200000L

    private fun ok(src: String, body: String, dir: String, amount: Double, account: String?, payee: String?, date: String?, ref: String?) {
        val t = Parser.parseSms(body, received, src)
        assertNotNull("Should be read as a payment: $body", t)
        t!!
        assertEquals("direction: $body", dir, t.direction)
        assertEquals("amount: $body", amount, t.amount, 0.001)
        if (account != null) assertEquals("account: $body", account, t.account)
        if (payee != null) assertEquals("payee: $body", payee, t.counterparty)
        if (date != null) assertEquals("date: $body", date, t.date)
        if (ref != null) assertEquals("ref: $body", ref, t.ref)
    }

    private fun none(src: String, body: String) {
        assertNull("Should be ignored: $body", Parser.parseSms(body, received, src))
    }

    @Test
    fun readsEveryKnownBankFormat() {
        ok("AX-HDFCBK-S", "Rs.450.00 debited from a/c **5997 on 14-03-26 to VPA swiggy@icici (UPI Ref No 607412345678). Not you? Call 18002586161", "debit", 450.0, "HDFC 5997", "Swiggy", "2026-03-14", "607412345678")
        ok("AX-HDFCBK-S", "Sent Rs.1,250.00 From HDFC Bank A/C *5997 To ZOMATO On 13/03/26 Ref 607498761234 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808", "debit", 1250.0, "HDFC 5997", "Zomato", "2026-03-13", "607498761234")
        ok("AX-HDFCBK-S", "Update! INR 50,000.00 deposited in HDFC Bank A/c XX5997 on 10-MAR-26 for NEFT Cr-ABCD0001234-ACME TECH PVT LTD-RAHUL. Avl bal INR 1,20,345.67", "credit", 50000.0, "HDFC 5997", "Acme Tech Pvt Ltd", "2026-03-10", null)
        ok("AX-HDFCBK-S", "Rs.2,340.50 spent on HDFC Bank Card x1291 at AMAZON on 12-03-2026:14:22:10. Avl Lmt: Rs.1,45,000.00", "debit", 2340.5, "HDFC CC 1291", "Amazon", "2026-03-12", null)
        ok("AX-HDFCBK-S", "Credit Alert! Rs.899.00 credited to HDFC Bank A/c XX5997 on 09-03-26 from VPA rahul@okhdfcbank (UPI 607011112222)", "credit", 899.0, "HDFC 5997", null, "2026-03-09", null)
        ok("VM-SBIINB-S", "Dear UPI user A/C X1234 debited by 320.0 on date 12Mar26 trf to RAMESH KUMAR Refno 607455556666. If not u? call 1800111109. -SBI", "debit", 320.0, "SBI 1234", "RAMESH KUMAR", "2026-03-12", "607455556666")
        ok("VM-SBIINB-S", "Dear UPI user A/C X1234 credited by Rs 5000 on date 11Mar26 trf from AMIT SHARMA Refno 607455557777 -SBI", "credit", 5000.0, "SBI 1234", "AMIT SHARMA", "2026-03-11", "607455557777")
        ok("AD-ICICIB-S", "ICICI Bank Acct XX3456 debited for Rs 1,999.00 on 11-Mar-26; NETFLIX credited. UPI:607333344445. Call 18002662 for dispute.", "debit", 1999.0, "ICICI 3456", "Netflix", "2026-03-11", null)
        ok("AD-ICICIB-S", "ICICI Bank Acct XX3456 credited with Rs 25,000.00 on 05-Mar-26 by NEFT-N0123456789-FREELANCE CLIENT. Avl Bal Rs 80,000.12", "credit", 25000.0, "ICICI 3456", null, "2026-03-05", null)
        ok("AD-ICICIB-S", "Rs 3,200.00 spent on ICICI Bank Card XX9012 on 10-Mar-26 at BIGBASKET. Avl Limit: Rs 96,800.00.", "debit", 3200.0, "ICICI CC 9012", "BigBasket", "2026-03-10", null)
        ok("VK-AXISBK-S", "Spent INR 780 Axis Bank Card no. XX4455 12-03-26 18:30:11 IST UBER India Avl Lmt INR 1,19,220 SMS BLOCK 4455 to 919951860002", "debit", 780.0, "AXIS CC 4455", "Uber", "2026-03-12", null)
        ok("VK-AXISBK-S", "INR 15000 debited A/c no. XX7788 14-03-26 11:02:44 UPI/P2A/607222223333/LANDLORD SMS BLOCKUPI Cust ID to 919951860002 -Axis Bank", "debit", 15000.0, "AXIS 7788", "Landlord", "2026-03-14", null)
        ok("VK-AXISBK-S", "INR 62000 credited to A/c no. XX7788 01-03-26 10:00:00 NEFT/AXISP00123456789/EMPLOYER LTD salary", "credit", 62000.0, "AXIS 7788", null, "2026-03-01", null)
        ok("AD-KOTAKB-S", "Sent Rs.600.00 from Kotak Bank AC X9988 to dmart@ybl on 13-03-26.UPI Ref 607555566667. Not you? Call 18602662666", "debit", 600.0, "KOTAK 9988", "DMart", "2026-03-13", "607555566667")
        ok("AD-KOTAKB-S", "Received Rs.1,500.00 in your Kotak Bank AC X9988 from friend@oksbi on 12-03-26.UPI Ref:607555566668", "credit", 1500.0, "KOTAK 9988", null, "2026-03-12", "607555566668")
        ok("VM-PNBSMS-S", "A/c XX0987 debited INR 2,500.00 on 12/03/2026 UPI/607777788889/PHARMEASY. Bal INR 10,200.50 -PNB", "debit", 2500.0, "PNB 0987", "Pharmeasy", "2026-03-12", null)
        ok("VM-BOBSMS-S", "Your a/c XX1122 is debited with Rs 1,000 on 10-03-2026 and credited to HP PETROL PUMP via UPI Ref 607888899990. -BOB", "debit", 1000.0, "BOB 1122", "HP PETROL PUMP", "2026-03-10", "607888899990")
        ok("VM-CANBNK-S", "An amount of INR 4,000.00 has been DEBITED to your account XX3344 on 09/03/2026 Total Avail.bal INR 22,100.00 - Canara Bank", "debit", 4000.0, "CANARA 3344", null, "2026-03-09", null)
        ok("VM-UNIONB-S", "A/c *5566 Debited for Rs:350.00 on 08-03-2026 14:11:00 by UPI Ref 607999900001 Avl Bal Rs:5,640.00-Union Bank of India", "debit", 350.0, "UNION 5566", null, "2026-03-08", "607999900001")
        ok("VM-IDFCFB-S", "Your A/C XX7799 is debited by INR 899.00 on 07-03-2026 towards UPI/607111100002/SPOTIFY. Avl bal INR 14,300.00. IDFC FIRST Bank", "debit", 899.0, "IDFC 7799", "Spotify", "2026-03-07", null)
        ok("VM-INDBNK-S", "Your a/c no. XXXXXX3210 is credited by Rs.2000.00 on 06-03-2026 by a/c linked to mobile no 98XXXXX210 (IMPS Ref no 607222200003)", "credit", 2000.0, "INDIAN 3210", null, "2026-03-06", "607222200003")
        ok("AX-HDFCBK-S", "Thank you for paying Rs.8,500.00 towards your HDFC Bank Credit Card ending 1291 on 05-03-2026 via NetBanking.", "credit", 8500.0, "HDFC CC 1291", null, "2026-03-05", null)
        ok("AX-HDFCBK-S", "Your EMI of Rs.12,450.00 for Loan A/c 448 has been debited from A/c XX5997 on 05-03-26", "debit", 12450.0, "HDFC 5997", "Loan 448", "2026-03-05", null)
        none("AX-HDFCBK-S", "Rs.200.00 will be debited from your a/c XX5997 on 20-03-26 towards NETFLIX mandate")
        none("AX-HDFCBK-S", "Your OTP for transaction of Rs.1,500.00 at AMAZON is 482913. Do not share with anyone.")
        none("AX-HDFCBK-S", "Dear Customer, Rs.12,345.00 is the total amount due on your HDFC Bank Credit Card 1291, minimum amount due Rs.617. Pay by 25-03-2026.")
        ok("AX-HDFCBK-S", "Refund of Rs.899.00 from AMAZON credited to your HDFC Bank Card x1291 on 14-03-26.", "credit", 899.0, null, null, "2026-03-14", null)
        none("AD-ICICIB-S", "Dear Customer, your ICICI Bank Credit Card XX9012 statement is generated. Total Amt Due Rs 5,430.00, Min Amt Due Rs 270.00. Due Date 28-Mar-26.")
        ok("VM-PAYTMB-S", "Rs 150 paid to Chai Point from Paytm Payments Bank a/c. Ref 607000011112. Bal Rs 2,040", "debit", 150.0, "PAYTM", null, null, "607000011112")
        ok("JM-PHONPE-S", "Paid Rs.275 to Local Kirana via PhonePe. UPI Ref 607000022223", "debit", 275.0, "", null, null, "607000022223")
        none("AX-HDFCBK-S", "Get 5x rewards on your next purchase! Offer valid till 31-03-26. T&C apply. Rs.500 cashback on spends above Rs.5000.")
        ok("AX-HDFCBK-S", "ATM withdrawal of Rs.5,000.00 from HDFC Bank Card x1291 at ATM ABC ROAD on 04-03-26. Avl Bal Rs 25,000.00", "debit", 5000.0, null, null, "2026-03-04", null)
        ok("VM-SBIINB-S", "Your a/c no. XXXXXXXX1234 is debited for Rs.499.00 on 02-03-26 for ACH D- AIRTEL Xstream Ref 12345", "debit", 499.0, "SBI 1234", "Airtel", "2026-03-02", null)
        ok("AX-HDFCBK-S", "Rs.10,000.00 transferred from A/c XX5997 to A/c XX1122 on 03-03-26. IMPS Ref 607333300004", "debit", 10000.0, "HDFC 5997", null, "2026-03-03", "607333300004")
        ok("AX-HDFCBK-S", "Interest of Rs.312.00 credited to your HDFC Bank A/c XX5997 on 31-03-26", "credit", 312.0, "HDFC 5997", null, "2026-03-31", null)
        ok("AD-ICICIB-S", "Dear Customer, a debit of Rs 49,999.00 was made on your card ending 9012 at CROMA on 02-Mar-26. If not you, call 18002662.", "debit", 49999.0, "ICICI CC 9012", "CROMA", "2026-03-02", null)
        none("AX-HDFCBK-S", "UPI Mandate: You have successfully created a mandate for Rs.649.00 for NETFLIX COM, starting 20-03-26. Ref 607000033334")
        ok("AX-HDFCBK-S", "Dear Customer, Rs. 1.00 debited from a/c XX5997 for UPI-verification 607000044445. Ignore if done by you.", "debit", 1.0, "HDFC 5997", null, null, null)
        ok("VM-YESBNK", "Rs.1,200.00 debited from YES BANK a/c XX4321 on 12-03-2026 towards UPI/607123450001/BOOKMYSHOW. Avl Bal Rs.8,000.00", "debit", 1200.0, "YES 4321", "Bookmyshow", "2026-03-12", null)
        ok("VM-INDUSB", "Rs 560.00 spent on IndusInd Bank Credit Card XX7766 at ZOMATO on 11-03-2026. Avl Limit Rs 74,440.00", "debit", 560.0, "INDUSIND CC 7766", "Zomato", "2026-03-11", null)
        ok("VM-FEDBNK", "Rs.3,000.00 credited to your Federal Bank A/c XX2233 on 10-03-2026 via UPI from SURESH P. Ref 607123450002", "credit", 3000.0, "FEDERAL 2233", "SURESH P", "2026-03-10", "607123450002")
        ok("VM-RBLBNK", "INR 1,750.00 spent on RBL Bank Credit Card ending 8877 at MYNTRA on 09-03-2026.", "debit", 1750.0, "RBL CC 8877", "Myntra", "2026-03-09", null)
        ok("VM-SBICRD", "Rs.2,999.00 spent on your SBI Credit Card ending 5544 at FLIPKART on 08-03-26. Avl Lmt Rs.47,001.00", "debit", 2999.0, "SBI CC 5544", "Flipkart", "2026-03-08", null)
        ok("VM-AMEXIN", "Alert: You've spent INR 6,400.00 on your AMEX card ** 91005 at TAJ HOTELS on 07 March 2026 at 08:15 PM IST.", "debit", 6400.0, null, null, "2026-03-07", null)
        ok("VM-BOIIND", "Rs.800.00 Debited from your BOI A/c XXXX0045 on 06-03-2026 UPI Ref No 607123450003 to CAFE COFFEE DAY", "debit", 800.0, "BOI 0045", "CAFE COFFEE DAY", "2026-03-06", "607123450003")
        ok("VM-SCBANK", "Rs 9,500.00 debited from your Standard Chartered A/c XX3300 on 05-Mar-2026 NEFT to JOHN DOE", "debit", 9500.0, "SCB 3300", null, "2026-03-05", null)
        ok("VM-AUBANK", "Rs.1,100.00 has been credited to your AU Small Finance Bank A/c XX6600 on 04-03-2026 by UPI-607123450004", "credit", 1100.0, "AU 6600", null, "2026-03-04", null)
        ok("VM-BDNBNK", "Your Bandhan Bank A/c XX5500 is debited with INR 250.00 on 03-03-2026 for UPI txn to ola@ybl. Ref 607123450005", "debit", 250.0, "BANDHAN 5500", "Ola", "2026-03-03", "607123450005")
        ok("VM-HDFCBK", "HDFC Bank: Rs 4,999.00 debited from a/c **5997 on 02-03-26 to BIGTREE ENTERTAINMENT PVT LTD via UPI. Ref 607123450006", "debit", 4999.0, "HDFC 5997", "BIGTREE ENTERTAINMENT PVT LTD", "2026-03-02", "607123450006")
        ok("VM-HDFCBK", "Salary credit: Rs.85,000.00 credited to HDFC Bank a/c XX5997 on 01-03-2026 by NEFT-ACME INDIA PVT LTD", "credit", 85000.0, "HDFC 5997", null, "2026-03-01", null)
        ok("VM-HDFCBK", "Rs 349.00 debited from a/c **5997 on 28-02-26 for AUTOPAY GOOGLE PLAY. UMN 607123450007", "debit", 349.0, "HDFC 5997", null, "2026-02-28", null)
        ok("VM-ICICIB", "Dear Customer, Your a/c no. XXXXXXXX3456 is credited with Rs. 1,00,000.00 on 27-Feb-26. Info: RTGS-ABCD1234-BUILDER LTD. Avl Bal: Rs. 1,80,000.00", "credit", 100000.0, "ICICI 3456", null, "2026-02-27", null)
        ok("VM-ICICIB", "Your ICICI Bank Credit Card XX9012 has been used for a transaction of INR 1,250.00 on 26-Feb-26 at 14:05:12. Info: SWIGGY. Avbl Credit Limit: INR 90,000", "debit", 1250.0, "ICICI CC 9012", "Swiggy", "2026-02-26", null)
        ok("VM-AXISBK", "Thank you for using your Axis Bank Credit Card no. XX4455 for INR 4,500.00 at DOMINOS on 25-02-26 14:20:11 IST", "debit", 4500.0, "AXIS CC 4455", "Domino's", "2026-02-25", null)
        ok("VM-AXISBK", "Payment of INR 18,000.00 received on your Axis Bank Credit Card no. XX4455 on 24-02-26. Thank you.", "credit", 18000.0, "AXIS CC 4455", null, "2026-02-24", null)
        ok("VM-SBIPSG", "Payment of Rs 1,430.00 for your Tata Power connection has been received. Thank you. -SBI Pay", "debit", 1430.0, "SBI", null, null, null)
        ok("AD-AIRTEL", "Rs 499 paid to Airtel via Airtel Thanks app. Txn ID 607123450008", "debit", 499.0, "", null, null, null)
        ok("VM-HDFCBK", "Your a/c XX5997 is credited by Rs 120 on 23-02-26 as UPI reversal for failed txn Ref 607123450009", "credit", 120.0, "HDFC 5997", null, "2026-02-23", "607123450009")
        none("VM-HDFCBK", "UPI txn of Rs 700 to MERCHANT failed. Amount will be reversed within 3 days.")
        none("VM-HDFCBK", "Dear Customer, your credit card statement for Feb is Rs 14,200.00 due on 15-03-2026. Pay now at hdfcbk.io/xyz")
        none("VM-HDFCBK", "Congratulations! You are pre-approved for a personal loan of Rs 5,00,000. Apply now")
        none("VM-HDFCBK", "Your a/c XX5997 balance is Rs 45,210.55 as of 22-02-26. Avl Bal Rs 45,210.55")
        ok("VM-HDFCBK", "Rs 25,000 debited from a/c XX5997 and invested in HDFC Mutual Fund SIP folio 123456 on 21-02-26", "debit", 25000.0, null, null, "2026-02-21", null)
        ok("VM-HDFCBK", "Rs.1,500.00 debited from a/c **5997 on 20-02-26 to UPI Lite wallet load", "debit", 1500.0, "HDFC 5997", null, "2026-02-20", null)
        ok("VM-HDFCBK", "INR 500 withdrawn from ATM XYZ using card ending 5997 on 19-02-26. Avl Bal INR 12,000", "debit", 500.0, "HDFC 5997", null, "2026-02-19", null)
        none("VM-HDFCBK", "Dear Customer, URGENT: your KYC expired. Click bit.ly/abc to update or account will be blocked.")
        ok("VM-HDFCBK", "Rs.75.50 debited from a/c **5997 on 18-02-26 to VPA paytmqr1xyz@paytm (UPI Ref No 607123450011)", "debit", 75.5, "HDFC 5997", null, "2026-02-18", "607123450011")
        ok("VM-HDFCBK", "Rs.2,000.00 credited to a/c **5997 on 17-02-26 by cheque deposit 123456. Clearing credit", "credit", 2000.0, "HDFC 5997", null, "2026-02-17", null)
        ok("VM-IDFCFB", "Your IDFC FIRST Bank A/C XX7799 is credited by INR 1,25,000.00 on 16-02-2026 from LIC OF INDIA maturity", "credit", 125000.0, "IDFC 7799", null, "2026-02-16", null)
        ok("VM-HDFCBK", "Rs 2,100 debited from a/c XX5997 on 15-02-26 to Mr RAHUL SHARMA via UPI Ref 607123450012", "debit", 2100.0, "HDFC 5997", "RAHUL SHARMA", "2026-02-15", "607123450012")
        ok("VM-ICICIB", "INR 12,000.00 debited from your ICICI Bank a/c XX3456 on 14-02-26 for ACH D- LIC OF INDIA-PREMIUM", "debit", 12000.0, "ICICI 3456", "Lic Of India", "2026-02-14", null)
        ok("VM-HDFCBK", "Rs 1,99,999.00 sent from HDFC Bank A/c *5997 to RENT RECEIVER via NEFT on 13-02-26 UTR HDFCN123456789", "debit", 199999.0, "HDFC 5997", "RENT RECEIVER", "2026-02-13", null)
    }

    @Test
    fun sameMessageGivesSameId() {
        val body = "Rs.450.00 debited from a/c **5997 on 14-03-26 to VPA swiggy@icici (UPI Ref No 607412345678)."
        val a = Parser.parseSms(body, received, "AX-HDFCBK-S")!!
        val b = Parser.parseSms(body, received + 5000, "AX-HDFCBK-S")!!
        assertEquals(a.id, b.id)
        // Without a reference number the id comes from the text, and is still stable.
        val noRef = "Rs.99.00 debited from a/c **5997 on 14-03-26 to MYSTERY SHOP"
        assertEquals(Parser.parseSms(noRef, received, "AX-HDFCBK-S")!!.id, Parser.parseSms(noRef, received, "AX-HDFCBK-S")!!.id)
    }

    @Test
    fun billsBecomeRemindersNotPayments() {
        val card = "Dear Customer, Rs.12,345.00 is the total amount due on your HDFC Bank Credit Card 1291, minimum amount due Rs.617. Pay by 25-03-2026."
        val r = Parser.parseReminder(card, received, "AX-HDFCBK-S")
        assertNotNull(r)
        assertEquals("2026-03-25", r!!.dueDate)
        assertEquals(12345.0, r.amount, 0.001)
        assertEquals(617.0, r.minAmount!!, 0.001)
        assertNull(Parser.parseSms(card, received, "AX-HDFCBK-S"))

        val stmt = "Dear Customer, your ICICI Bank Credit Card XX9012 statement is generated. Total Amt Due Rs 5,430.00, Min Amt Due Rs 270.00. Due Date 28-Mar-26."
        val s = Parser.parseReminder(stmt, received, "AD-ICICIB-S")
        assertNotNull(s)
        assertEquals("2026-03-28", s!!.dueDate)
        assertEquals(5430.0, s.amount, 0.001)
        assertEquals(270.0, s.minAmount!!, 0.001)

        val autopay = "Rs.200.00 will be debited from your a/c XX5997 on 20-03-26 towards NETFLIX mandate"
        val a = Parser.parseReminder(autopay, received, "AX-HDFCBK-S")
        assertNotNull(a)
        assertEquals("2026-03-20", a!!.dueDate)
        assertTrue(a.recurring)
    }

    @Test
    fun balanceIsReadFromPaymentMessage() {
        assertEquals(12000.5, Parser.balanceFromTxn("Rs.2,340.50 spent on HDFC Bank Card x1291 at AMAZON. Avl Bal Rs 12,000.50")!!, 0.001)
    }

    @Test
    fun regionalBanksAreRecognised() {
        // Maharashtra Gramin Bank signs "MGB"; a bank name inside the other person's UPI address is not your bank.
        ok("VM-MAHGRM-S", "Your a/c no. XXXX883076 is debited Rs.190.00 on 19-10-26 and a/c linked to virtual address paytm.s20uk16@pty is credited (UPI Ref no 061700089155)MGB", "debit", 190.0, "MGB 883076", "paytm.s20uk16", "2026-10-19", null)
        ok("AX-HDFCBK-S", "Rs.450.00 debited from a/c **5997 on 14-03-26 to VPA paytm.shop@paytm (UPI Ref No 607412345678)", "debit", 450.0, "HDFC 5997", null, "2026-03-14", null)
        ok("VM-BOMBNK-S", "Your Bank of Maharashtra a/c XX1234 is credited by Rs.5000.00 on 12-10-26. Avl Bal Rs.9000", "credit", 5000.0, "BOM 1234", null, "2026-10-12", null)
        ok("VM-ABCDEF-S", "A/c XX4455 debited Rs.200 on 11-10-26 UPI Ref 123456789012. Telangana Grameena Bank", "debit", 200.0, "TGB 4455", null, "2026-10-11", null)
        ok("VM-ABCDEF-S", "A/c XX7788 debited Rs.300 on 11-10-26 UPI Ref 123456789013. Bihar Gramin Bank", "debit", 300.0, "BIHAR GRAMIN 7788", null, "2026-10-11", null)
        ok("VM-ABCDEF-S", "A/c XX9911 debited Rs.120 on 11-10-26 UPI Ref 123456789014. Airtel Payments Bank", "debit", 120.0, "AIRTEL 9911", null, "2026-10-11", null)
    }

    @Test
    fun mgbIsDecidedByTheSender() {
        // "MGB" in the text of a message from some other sender is not Maharashtra Gramin Bank.
        ok("VM-XYZABC-S", "Rs.300.00 debited from A/c XX7788 on 11-10-26 to MGB TRADERS UPI Ref 123456789013", "debit", 300.0, "7788", null, "2026-10-11", null)
        ok("VM-XYZABC-S", "Your a/c no. XXXX883076 is debited Rs.50.00 on 20-08-26 (UPI Ref no 481050341654) -MGB04245", "debit", 50.0, "883076", null, "2026-08-20", null)
        // From the MGB sender it is.
        ok("AX-MGBBNK-S", "Your a/c no. XXXX883076 is debited Rs.50.00 on 20-08-26 (UPI Ref no 481050341654) -MGB04245", "debit", 50.0, "MGB 883076", null, "2026-08-20", null)
    }

    @Test
    fun bankFromTheTextNeedsTheFullName() {
        // A short name on its own is not enough from an unknown sender ...
        ok("VM-XYZABC-S", "Rs.300.00 debited from A/c XX7788 on 11-10-26 to SBI LIFE UPI Ref 123456789013", "debit", 300.0, "7788", null, "2026-10-11", null)
        // ... the full name is.
        ok("VM-XYZABC-S", "Rs.300.00 debited from A/c XX7788 on 11-10-26. HDFC Bank UPI Ref 123456789013", "debit", 300.0, "HDFC 7788", null, "2026-10-11", null)
        ok("VM-XYZABC-S", "A/c XX4455 debited Rs.200 on 11-10-26 UPI Ref 123456789012. Telangana Grameena Bank", "debit", 200.0, "TGB 4455", null, "2026-10-11", null)
    }

    @Test
    fun aSenderYouTaughtNamesTheBank() {
        assertEquals("MGBBNK", Parser.senderCore("AX-MGBBNK-S"))
        assertEquals("MGBBNK", Parser.senderCore("MGBBNK"))
        assertEquals("MGBBNK", Parser.senderCore("VM-MGBBNK"))
        Parser.taughtBanks = mapOf("ZZBANK" to "MGB")
        try {
            ok("VM-ZZBANK-S", "Your a/c no. XXXX883076 is debited Rs.50.00 on 20-08-26 (UPI Ref no 481050341654)", "debit", 50.0, "MGB 883076", null, "2026-08-20", null)
        } finally { Parser.taughtBanks = emptyMap() }
    }

    @Test
    fun paymentTimeComesFromTheMessageElseArrival() {
        assertEquals(18 * 3600 + 27 * 60 + 54, TxnTime.fromText("credited on 07/07/2026 at 18:27:54. Bal Rs 5")!!)
        assertEquals(18 * 3600 + 5 * 60, TxnTime.fromText("debited on 02-05-26 6:05 PM")!!)
        assertEquals(0, TxnTime.fromText("debited at 12:00 AM")!!)
        assertEquals(null, TxnTime.fromText("debited Rs.50 Ref 481050341654 on 20-08-26"))
        // 2026-10-19 12:30 UTC is 18:00 in India
        assertEquals(18 * 3600, TxnTime.fromArrival(1_792_413_000_000L).let { it })
    }

    @Test
    fun maharashtraGraminBankMessages() {
        val src = "AX-MGBBNK-S"
        ok(src, "Your A/c no XXXXX83076 is credited by RS. 931.97 through By Transfer on 07/07/2026 at 18:27:54.Current BAL: Rs. 965.08 CR. -MGB04245", "credit", 931.97, "MGB 83076", null, "2026-07-07", null)
        ok(src, "Your Account XXXXX83076 had a Micro ATM debit transaction of Rs. 6000.00 on 23/08/2026 at 15:52:09.available balance is Rs. 1609.08 CR MGB 04245", "debit", 6000.0, "MGB 83076", null, "2026-08-23", null)
        ok(src, "Your a/c no. XXXX883076 is credited Rs.100.00 on 18-09-26 by a/c linked to virtual address 9860737048@ptyes (UPI Ref no 314964004488)MGB", "credit", 100.0, "MGB 883076", "9860737048", "2026-09-18", null)
        ok(src, "Your a/c no. XXXX883076 is debited Rs.50.00 on 20-08-26 and a/c linked to virtual address 90828180822@sbi is credited (UPI Ref no 481050341654)MGB", "debit", 50.0, "MGB 883076", "90828180822", "2026-08-20", null)
        ok(src, "ALERT: Your account 80XXXXXXXX76 has been debited by RS 10 on 02/05/2026 13:05 MGB", "debit", 10.0, "MGB 76", null, "2026-05-02", null)
        assertEquals(11.08, Parser.balanceFromTxn("Your A/c no XXXXX83076 is credited by RS. 1.00 through BULK POSTING on 15/10/2026 at 13:34:13.Current BAL: Rs. 11.08 CR. -MGB04245")!!, 0.001)
        assertEquals(1609.08, Parser.balanceFromTxn("Your Account XXXXX83076 had a Micro ATM debit transaction of Rs. 6000.00 on 23/08/2026 at 15:52:09.available balance is Rs. 1609.08 CR MGB 04245")!!, 0.001)
    }

    @Test
    fun sameAccountWithDifferentDigitCountsIsJoined() {
        val m = Analytics.autoMerge(mapOf("MGB 883076" to 1400, "83076" to 680, "MGB 3076" to 30, "SBI 3076" to 29, "HDFC 5997" to 100, "HDFC CC 5997" to 5))
        assertEquals("MGB 883076", m["83076"])
        assertEquals("MGB 883076", m["MGB 3076"])
        assertNull(m["SBI 3076"])           // a different bank's account is never joined
        assertNull(m["HDFC CC 5997"])       // a card is never joined to a savings account
        assertNull(m["MGB 883076"])
        // A two-digit tail joins only when exactly one account of that bank ends the same way.
        val m2 = Analytics.autoMerge(mapOf("MGB 883076" to 1400, "MGB 83076" to 680, "MGB 76" to 3))
        assertEquals("MGB 883076", m2["MGB 76"])
        val m3 = Analytics.autoMerge(mapOf("SBI 247710" to 7, "SBI 868710" to 3, "SBI 10" to 2))
        assertNull(m3["SBI 10"])
    }

    @Test
    fun insurerReceiptsAreNotPayments() {
        none("VM-ICICIL-S", "Dear Customer, Thank you for choosing ICICI Lombard General Insurance as your preferred Insurance partner. We have received your payment of Rs.1398 against policy number 3005/O/309608071/00/B00.Issuance of Policy is subject to Proposal acceptance and Premium realization. Team ICICI Lombard")
        none("VM-LICIND-S", "Dear Policyholder, premium of Rs.12,000 received against policy 123456789. Thank you. LIC")
    }

    @Test
    fun onlyBankSendersAreTrusted() {
        assertTrue(Parser.isFinancialSender("AX-HDFCBK-S"))
        // Depositories and exchanges are not bank senders.
        assertFalse(Parser.isFinancialSender("VM-CDSL-S"))
        assertFalse(Parser.isFinancialSender("AD-CDSLDP"))
        assertFalse(Parser.isFinancialSender("JD-NSDL-T"))
        assertFalse(Parser.isFinancialSender("NSEIND"))
        assertFalse(Parser.isFinancialSender("VK-BSELTD-S"))
        assertTrue(Parser.isFinancialSender("VM-NSDLPB-S"))
        assertTrue(Parser.isFinancialSender("JD-HDFCBK-S"))
        assertFalse(Parser.isFinancialSender("MYFRIEND"))
    }

    @Test
    fun loanNoticesNameTheLoanNotASavingsAccount() {
        val due = "DUES Payment: Use this link https://hdfcbk.io/HDFCBK/s/aaaa1111 to pay Rs.4300 for HDFC Bank Loan A/c 70****123."
        assertEquals(null, Parser.parseSms(due, 1760000000000L, "HDFCBK"))
        val r = Parser.parseReminder(due, 1760000000000L, "HDFCBK")!!
        assertEquals("HDFC Loan 123", r.counterparty)
        assertEquals(4300.0, r.amount, 0.001)
        assertEquals("HDFC", r.account)
        assertTrue(Parser.isLoanAccountText("HDFC Bank Loan Ac no.xxx789 - PEMI Interest on disbursement of Rs 1000000 will be debited along with the next EMI."))
        // The disbursed amount is not a bill.
        assertEquals(null, Parser.parseReminder("HDFC Bank Loan Ac no.xxx789 - PEMI Interest on disbursement of Rs 1000000 will be debited along with the next EMI.", 1760000000000L, "HDFCBK"))
    }

    @Test
    fun debitCardMessageKeepsTheCardNumber() {
        val m = "Rs.100.00 spent on HDFC Bank Card x4321 at SHOP on 2025-04-21:17:24:21 Avl bal: 5000.00.Not You? Call 18002586161 / SMS BLOCK DC 4321 to 7308080808"
        assertEquals("HDFC DC 4321", Parser.parseSms(m, 1745236000000L, "HDFCBK")!!.account)
        val cc = "Rs.100.00 spent on HDFC Bank Credit Card ending 4321 at SHOP on 21-04-25. Avl limit: 50000"
        assertEquals("HDFC CC 4321", Parser.parseSms(cc, 1745236000000L, "HDFCBK")!!.account)
    }
}
