package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The steps between stored payments and what the screens count (see Reconcile).
class ReconcileTest {
    private fun row(id: String, date: String, amount: Double, dir: String, kind: String, cat: String, account: String = "HDFC 5997",
                    payee: String? = null, ref: String? = null, raw: String = "", source: String = "AX-HDFCBK-S") =
        Row(id, date, amount, dir, payee, ref, raw, source, account, kind, cat)

    @Test
    fun aReversalWithTheSameReferenceNetsAgainstItsSpend() {
        val spend = row("ref-1", "2026-10-12", 500.0, "debit", "expense", "Shopping", payee = "Amazon", ref = "628745678901")
        val back = row("ref-1-cr", "2026-10-12", 500.0, "credit", "refund", "Refund", ref = "628745678901")
        val p = Reconcile.pairRefunds(listOf(spend, back), emptyMap())
        assertEquals("Shopping", p.rows.first { it.id == "ref-1-cr" }.category)
        assertTrue("paired without asking", p.suggestions.isEmpty())
        // "No, not related" is respected even then.
        val no = Reconcile.pairRefunds(listOf(spend, back), mapOf("ref-1-cr" to false))
        assertEquals("Refund", no.rows.first { it.id == "ref-1-cr" }.category)
    }

    // One income tax refund, told by SBI (the refund banker) and by HDFC (where the money landed).
    @Test
    fun aCreditToldByTwoBanksCountsOnce() {
        val sbi = row("s", "2026-10-29", 20990.0, "credit", "income", "Tax refund", account = "SBI 5997", source = "JM-SBIBNK-S",
            raw = "Dear Customer, For PAN XXXXXX066M, An IT Refund amount of Rs 20990 for AY-2026-27 has been credited to your " +
                "account XXXXXXXXXX5997 on 2026-10-29. -SBI").copy(ms = 1792821540000L)
        val hdfc = row("h", "2026-10-29", 20990.0, "credit", "income", "Tax refund", account = "HDFC 5997", source = "BP-HDFCBK-S",
            raw = "Update! INR 20,990.00 deposited in HDFC Bank A/c XX5997 on 29-OCT-26 for NEFT Cr-SBIN0000TBU-ITDTAX REFUND " +
                "2026-27. Avl bal INR 44,050.68.").copy(ms = 1792822020000L)
        val other = row("o", "2026-10-20", 500.0, "debit", "expense", "Food", account = "HDFC 5997")
        // The account labels are joined first (HDFC 5997 is where 5997 mostly appears), then the SBI notice drops out.
        val out = Reconcile.dedupCrossBank(Reconcile.canonicalizeAccounts(listOf(sbi, hdfc, other)))
        assertEquals(listOf("h", "o"), out.map { it.id }.sorted())
        // Two equal credits from your own bank on one day are two payments.
        val twice = hdfc.copy(id = "h2", ms = hdfc.ms + 3600_000L)
        assertEquals(2, Reconcile.dedupCrossBank(listOf(hdfc, twice)).size)
        // Days apart, the other bank's message is not an echo.
        val late = sbi.copy(account = "HDFC 5997", date = "2026-10-25", ms = sbi.ms - 4 * 86_400_000L)
        assertEquals(2, Reconcile.dedupCrossBank(listOf(late, hdfc)).size)
    }

    @Test
    fun aTaxRefundIsIncome() {
        fun cls(raw: String) = Classifier.classify(Txn("t", "2026-10-29", 20990.0, "credit", null, null, raw, "BP-HDFCBK-S", "HDFC 5997"), emptyList())
        val r = cls("Update! INR 20,990.00 deposited in HDFC Bank A/c XX5997 on 29-OCT-26 for NEFT Cr-SBIN0000TBU-ITDTAX REFUND 2026-27")
        assertEquals("income" to "Tax refund", r.kind to r.category)
        val s = cls("An IT Refund amount of Rs 20990 for AY-2026-27 has been credited to your account XXXXXXXXXX5997")
        assertEquals("income" to "Tax refund", s.kind to s.category)
    }

    // A failed ATM withdrawal: the reversal arrives seconds after the withdrawal, for the same amount on the same card.
    @Test
    fun aReversalWithinMinutesNetsWithoutAsking() {
        val spend = row("w", "2026-04-03", 10000.0, "debit", "expense", "ATM / Cash", account = "HDFC 5997",
            raw = "Withdrawn Rs.10000 From HDFC Bank Card x1208 At +HAFEEZPET On 2026-04-03:08:40:38 Bal Rs.747635 Not You?")
            .copy(ms = 1775185838000L)
        val back = row("r", "2026-04-03", 10000.0, "credit", "refund", "Refund", account = "HDFC 5997",
            raw = "ATM Txn Reversed:Rs.10000 reversed to your HDFC Bank Card 1208 At +HAFEEZPET On 2026-04-03:08:40:40.Inconvenience regretted")
            .copy(ms = 1775185840000L)
        val p = Reconcile.pairRefunds(listOf(spend, back), emptyMap())
        assertEquals("ATM / Cash", p.rows.first { it.id == "r" }.category)
        assertTrue("paired without asking", p.suggestions.isEmpty())
        // "Not related" still wins.
        assertEquals("Refund", Reconcile.pairRefunds(listOf(spend, back), mapOf("r" to false)).rows.first { it.id == "r" }.category)
        // Hours later, or on another account, it is a real refund to ask about (or leave alone).
        val later = back.copy(raw = "Rs.10000 refunded to your HDFC Bank Card 1208 at 14:05:10", ms = 1775185840000L + 5 * 3600_000L)
        assertEquals(1, Reconcile.pairRefunds(listOf(spend, later), emptyMap()).suggestions.size)
        val other = back.copy(account = "SBI 4821")
        assertEquals("Refund", Reconcile.pairRefunds(listOf(spend, other), emptyMap()).rows.first { it.id == "r" }.category)
    }

    @Test
    fun aRefundOfTheSameAmountIsAskedAboutFirst() {
        val spend = row("a", "2026-10-01", 1299.0, "debit", "expense", "Shopping", payee = "Myntra")
        val refund = row("b", "2026-10-09", 1299.0, "credit", "refund", "Refund", payee = "Myntra")
        val asked = Reconcile.pairRefunds(listOf(spend, refund), emptyMap())
        assertEquals(listOf("b" to "a"), asked.suggestions.map { it.first.id to it.second.id })
        assertEquals("Refund", asked.rows.first { it.id == "b" }.category)
        val yes = Reconcile.pairRefunds(listOf(spend, refund), mapOf("b" to true))
        assertEquals("Shopping", yes.rows.first { it.id == "b" }.category)
        assertTrue(yes.suggestions.isEmpty())
        // A refund older than the spend, or far bigger than what is left of it, is not suggested.
        val late = Reconcile.pairRefunds(listOf(spend.copy(date = "2026-10-10"), refund), emptyMap())
        assertTrue(late.suggestions.isEmpty())
    }

    @Test
    fun oneReferenceOnTwoOfYourAccountsIsATransfer() {
        val out = row("ref-9", "2026-10-12", 10000.0, "debit", "expense", "Unknown", payee = "RAHUL SHARMA", ref = "628745670000")
        val inn = row("ref-9-cr", "2026-10-12", 10000.0, "credit", "transfer", "Transfer", account = "SBI 1234", payee = "RAHUL SHARMA", ref = "628745670000")
        val r = Reconcile.detectSelfTransfers(listOf(out, inn))
        assertEquals(listOf("transfer", "transfer"), r.map { it.kind })
        // A payment you categorised yourself stays as you set it.
        val mine = Reconcile.detectSelfTransfers(listOf(out, inn), setOf("ref-9"))
        assertEquals("expense", mine.first { it.id == "ref-9" }.kind)
        // The same reference on the same account (a payment and its reversal) is not a transfer.
        val same = Reconcile.detectSelfTransfers(listOf(out, inn.copy(account = "HDFC 5997", kind = "refund", category = "Refund")))
        assertEquals("expense", same.first { it.id == "ref-9" }.kind)
    }

    @Test
    fun theSameAmountLeavingAndArrivingNextDayIsATransfer() {
        val out = row("o", "2026-10-12", 7000.0, "debit", "expense", "Unknown")
        val inn = row("i", "2026-10-13", 7000.0, "credit", "income", "Income", account = "SBI 1234")
        assertEquals(listOf("transfer", "transfer"), Reconcile.detectSelfTransfers(listOf(out, inn)).map { it.kind })
        val far = inn.copy(date = "2026-10-16")
        assertEquals(listOf("expense", "income"), Reconcile.detectSelfTransfers(listOf(out, far)).map { it.kind })
    }

    @Test
    fun repeatedNoticesCountOnce() {
        // A biller's own "payment received" note (no account) beside your bank's debit of the same amount.
        val bank = row("d", "2026-10-15", 1850.0, "debit", "expense", "Bills", payee = "BESCOM")
        val echo = row("e", "2026-10-16", 1850.0, "debit", "expense", "Bills", account = "", payee = "BESCOM", source = "VM-BESCOM-S")
        assertEquals(listOf("d"), Reconcile.dedupBillers(listOf(bank, echo)).map { it.id })
        // Two EMI notices for one installment: the one with the account stays.
        val emi1 = row("n1", "2026-10-05", 15432.0, "debit", "expense", "EMI", account = "")
        val emi2 = row("n2", "2026-10-05", 15432.0, "debit", "expense", "EMI")
        assertEquals(listOf("n2"), Reconcile.dedupBillers(listOf(emi1, emi2)).map { it.id })
        // A known brand's two confirmations of one payment.
        val j1 = row("j1", "2026-10-03", 299.0, "debit", "expense", "Bills", payee = "Jio")
        val j2 = row("j2", "2026-10-03", 299.0, "debit", "expense", "Bills", payee = "Jio", account = "HDFC 1111")
        assertEquals(1, Reconcile.dedupBillers(listOf(j1, j2)).size)
    }

    @Test
    fun anEmiSeriesIsRecognisedFromOneKnownMonth() {
        val known = row("k", "2026-09-05", 12450.0, "debit", "expense", "EMI")
        val next = row("n", "2026-10-06", 12450.0, "debit", "expense", "Unknown")
        val other = row("x", "2026-10-06", 12450.0, "debit", "expense", "Unknown", account = "SBI 1234")
        val r = Reconcile.inferEmiSeries(listOf(known, next, other), emptySet())
        assertEquals("EMI", r.first { it.id == "n" }.category)
        assertEquals("Unknown", r.first { it.id == "x" }.category)
        assertEquals("Unknown", Reconcile.inferEmiSeries(listOf(known, next), setOf("n")).first { it.id == "n" }.category)
    }

    @Test
    fun oneAccountWrittenSeveralWaysBecomesOne() {
        val rows = List(5) { row("a$it", "2026-10-0${it + 1}", 10.0, "debit", "expense", "Food", account = "HDFC 4898") } +
            row("b", "2026-10-07", 10.0, "debit", "expense", "Food", account = "HDFC 898") +
            // A loan EMI that named only the bank folds into that bank's main savings account.
            row("c", "2026-10-08", 10.0, "debit", "expense", "EMI", account = "HDFC")
        val r = Reconcile.canonicalizeAccounts(rows)
        assertEquals(setOf("HDFC 4898"), r.map { it.account }.toSet())
        // ... unless you separated them.
        val apart = Reconcile.canonicalizeAccounts(rows, setOf("HDFC 898"))
        assertTrue("HDFC 898" in apart.map { it.account })
    }

    @Test
    fun anAccountReportingALimitIsACard() {
        val rows = listOf(
            row("a", "2026-10-01", 10.0, "debit", "expense", "Food", account = "ICICI CC 9012", raw = "Rs 10 spent on ICICI Bank Card XX9012. Avl Limit: Rs 96,800.00"),
            row("b", "2026-10-02", 10.0, "debit", "expense", "Food", account = "ICICI 9012", raw = "Rs 10 spent on ICICI Bank Card XX9012."),
        )
        assertEquals(setOf("ICICI CC 9012"), Reconcile.canonicalizeAccounts(rows).map { it.account }.toSet())
    }

    @Test
    fun joinedAccountsCountUnderTheOneYouChose() {
        val rows = listOf(
            row("a", "2026-10-01", 10.0, "debit", "expense", "Food", account = "HDFC 5997"),
            row("b", "2026-10-02", 10.0, "debit", "expense", "Food", account = "HDFC 1208"),
            // The savings-looking label a loan's EMI messages create, which you joined to that loan.
            row("c", "2026-10-05", 900.0, "debit", "expense", "EMI", account = "HDFC 0448"),
        )
        val r = Reconcile.joinAccounts(rows, mapOf("HDFC 1208" to "HDFC 5997", "HDFC 0448" to "HDFC Loan 448"))
        assertEquals("HDFC 5997", r.first { it.id == "b" }.account)
        // A label joined to a loan counts where the money left from: the bank's main account.
        assertEquals("HDFC 5997", r.first { it.id == "c" }.account)
    }

    private fun bill(due: String, amount: Double, account: String = "HDFC", min: Double? = null) =
        Reminder("r", due, amount, min, "HDFC", account, true, "", "AX-HDFCBK-S")

    @Test
    fun aBillPaidFromTheSameBankForTheSameAmountIsPaid() {
        val paid = row("p", "2026-10-01", 1999.0, "debit", "expense", "Bills")
        assertEquals("p", Reconcile.paidBy(bill("2026-10-02", 1999.0), listOf(paid))?.id)
        // A card bill paid at least the minimum due counts too.
        val part = row("q", "2026-10-05", 5000.0, "debit", "transfer", "Card bill")
        assertEquals("q", Reconcile.paidBy(bill("2026-10-06", 12000.0, "HDFC CC 1291", min = 600.0), listOf(part))?.id)
    }

    @Test
    fun aBillIsNotPaidByAnUnrelatedPayment() {
        val due = bill("2026-10-02", 1999.0)
        // Another amount, another bank, too long before, or money coming in: not a match.
        assertEquals(null, Reconcile.paidBy(due, listOf(row("a", "2026-10-01", 2500.0, "debit", "expense", "Bills"))))
        assertEquals(null, Reconcile.paidBy(due, listOf(row("b", "2026-10-01", 1999.0, "debit", "expense", "Bills", account = "SBI 1234"))))
        assertEquals(null, Reconcile.paidBy(due, listOf(row("c", "2026-09-10", 1999.0, "debit", "expense", "Bills"))))
        assertEquals(null, Reconcile.paidBy(due, listOf(row("d", "2026-10-01", 1999.0, "credit", "income", "Income"))))
        // No bank on the bill: never guessed.
        assertEquals(null, Reconcile.paidBy(bill("2026-10-02", 1999.0, ""), listOf(row("e", "2026-10-01", 1999.0, "debit", "expense", "Bills"))))
    }
}
