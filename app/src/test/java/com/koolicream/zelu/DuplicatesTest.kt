package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Test

// "Possible duplicates" asks only about payments that could really be one payment reported twice.
class DuplicatesTest {
    private val received = 1747900000000L // 22 May 2025

    private fun row(body: String, src: String = "AD-HDFCBK-S"): Row {
        val t = Parser.parseSms(body, received, src)!!
        val c = Classifier.classify(t, emptyList())
        return Row(t.id, t.date, t.amount, t.direction, t.counterparty, t.ref, t.raw, t.source, t.account, c.kind, c.category, t.ms)
    }

    @Test
    fun aUpiPaymentAndAnNeftTransferAreTwoPayments() {
        val upi = row("Sent Rs.50000.00 From HDFC Bank A/C x5997 To RAVI KUMAR On 22/05/25 Ref 245243200001 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808")
        val neft = row("Amt Deducted! Rs.50000 from your HDFC Bank A/c XX5997 for NEFT transaction via HDFC Bank Online Banking Not you?Call 18002586161/SMS BLOCK OB to 7308080808")
        assertEquals(upi.date, neft.date)
        assertEquals(upi.account, neft.account)
        assertEquals(emptyList<Pair<Row, Row>>(), Analytics.possibleDuplicates(listOf(upi, neft), emptySet()))
    }

    @Test
    fun twoNoticesOfOneNeftTransferAreAskedAbout() {
        val debit = row("Amt Deducted! Rs.50000 from your HDFC Bank A/c XX5997 for NEFT transaction via HDFC Bank Online Banking on 22-05-25")
        val done = row("NEFT transaction of Rs.50000 from your HDFC Bank A/c XX5997 on 22-05-25 has been credited to beneficiary RAVI KUMAR")
        assertEquals(1, Analytics.possibleDuplicates(listOf(debit, done), emptySet()).size)
    }
}
