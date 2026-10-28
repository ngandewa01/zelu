package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Test

// Loan apps and lenders whose EMI debits name only the company are EMI, not an unknown payee.
class LenderEmiTest {
    private val received = 1759622400000L // 5 Oct 2025

    private fun category(body: String): String {
        val t = Parser.parseSms(body, received, "AD-HDFCBK-S")!!
        return Classifier.classify(t, emptyList()).category
    }

    @Test
    fun kisetsuSaisonIsEmi() {
        assertEquals("EMI", category("Sent Rs.14849.00 From HDFC Bank A/C x5997 To KISETSU SAISON FINANCE INDIA PVT LTD On 05/10/25 Ref 527812345678 Not You? Call 18002586161"))
    }

    @Test
    fun otherLoanAppsAreEmi() {
        assertEquals("EMI", category("Sent Rs.3200.00 From HDFC Bank A/C x5997 To KREDITBEE On 05/10/25 Ref 527812345679 Not You? Call 18002586161"))
        assertEquals("EMI", category("Sent Rs.5400.00 From HDFC Bank A/C x5997 To TATA CAPITAL LIMITED On 05/10/25 Ref 527812345680 Not You? Call 18002586161"))
    }
}
