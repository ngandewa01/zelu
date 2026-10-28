package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// A rebuild must not lose payments whose message has left the inbox (see Rebuild).
class RebuildTest {
    private val gone = "Sent Rs.250.00 From HDFC Bank A/C *5997 To BLINKIT On 18/10/25 Ref 529112345678 Not You? Call 18002586161"
    private val still = "Sent Rs.120.00\nFrom HDFC Bank A/C *5997\nTo ZEPTO\nOn 18/10/26\nRef 629112345670"

    private fun stored(body: String, ms: Long, src: String = "AX-HDFCBK-S"): Row {
        val t = Parser.parseSms(body, ms, src)!!
        val c = Classifier.classify(t, emptyList())
        return Row(t.id, t.date, t.amount, t.direction, t.counterparty, t.ref, t.raw, t.source, t.account, c.kind, c.category, t.ms)
    }

    @Test
    fun paymentsWhoseMessageIsGoneAreReadFromTheirStoredCopy() {
        val old = stored(gone, 1760780000000L)
        val cur = stored(still, 1792300000000L)
        val plan = Rebuild.plan(listOf(RawSms("AX-HDFCBK-S", still, 1792300000000L)), listOf(old, cur), emptyList(), emptyList())
        assertEquals(1, plan.messages.size)
        val again = Parser.parseSms(plan.messages[0].body, plan.messages[0].date, plan.messages[0].address)!!
        // Read again, the stored copy gives the same payment under the same ID, so your decisions about it still apply.
        assertEquals(old.id, again.id)
        assertEquals(old.date, again.date)
        assertEquals(old.amount, again.amount, 0.001)
        assertTrue(plan.keep.isEmpty())
    }

    @Test
    fun aMessageWithNoDateOfItsOwnKeepsItsDay() {
        val body = "Rs.85.00 debited from A/c XX5997 to VPA chai@ybl UPI Ref 529000000001"
        val old = stored(body, 1760780000000L).copy(ms = 0)        // stored by a version that did not keep the time
        val plan = Rebuild.plan(emptyList(), listOf(old), emptyList(), emptyList())
        val again = Parser.parseSms(plan.messages[0].body, plan.messages[0].date, plan.messages[0].address)!!
        assertEquals(old.date, again.date)
    }

    @Test
    fun taughtPaymentsAutoPaysAndBillsAreKept() {
        val taught = Row("man-1a2b", "2025-06-01", 40.0, "debit", "Tea", null, "COOP BANK 40 TO TEA", "VM-COOPBK", "COOP", "expense", "Food", 1748736000000L)
        val mandate = Mandate("netflix", "Netflix", 649.0, "HDFC 5997", "active", null, 1748736000000L, 1748736000000L,
            "UPI AutoPay mandate created successfully towards Netflix for Rs 649", "AX-HDFCBK-S")
        val bill = Reminder("rem-1", "2025-07-05", 1200.0, null, "Airtel", "", false, "Your Airtel bill of Rs 1200 is due on 05-07-2025", "AD-AIRTEL-S")
        val fromMandate = bill.copy(id = "mand-netflix-2025-07-05")
        val mine = bill.copy(id = "man-1", source = "manual")
        val plan = Rebuild.plan(emptyList(), listOf(taught), listOf(mandate), listOf(bill, fromMandate, mine))
        assertEquals(listOf("man-1a2b"), plan.keep.map { it.id })
        assertEquals(setOf(mandate.raw, bill.raw), plan.messages.map { it.body }.toSet())
    }

    @Test
    fun decisionsMoveToTheNewCreditIds() {
        val rows = listOf(
            Row("ref-111", "2026-10-01", 5.0, "credit", null, "111", "", "", "", "refund", "Refund"),
            Row("ref-222", "2026-10-01", 5.0, "debit", null, "222", "", "", "", "expense", "Food"),
            Row("ref-333-cr", "2026-10-01", 5.0, "credit", null, "333", "", "", "", "income", "Income"),
        )
        val renames = Rebuild.creditRenames(rows)
        assertEquals(mapOf("ref-111" to "ref-111-cr"), renames)
        assertEquals("ref-111-cr", Rebuild.renameKey("ref-111", renames))
        assertEquals("ref-111-cr#2", Rebuild.renameKey("ref-111#2", renames))
        assertEquals("ref-222|ref-111-cr", Rebuild.renameKey("ref-222|ref-111", renames))
        assertEquals("ref-222", Rebuild.renameKey("ref-222", renames))
    }
}
