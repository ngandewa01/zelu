package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountBookTest {
    private fun st(n: Int) = AccountBook.Stat(n, "2026-10-01", 100.0, "msg")

    // MGB written three ways (Zelu joined 83076 itself), a bare "76" with no bank, HDFC with a debit card and a second account,
    // an HDFC credit card, and a Union loan with the savings-looking label its EMI messages create.
    private val labels = mapOf(
        "MGB 883076" to st(23), "SBI 3076" to st(3), "76" to st(3), "48453" to st(2),
        "HDFC 5997" to st(40), "HDFC 1208" to st(12), "HDFC 0025" to st(4), "HDFC CC 9065" to st(31),
        "UNION 2166" to st(50), "UNION 0126" to st(6),
    )
    private val auto = mapOf("MGB 83076" to ("MGB 883076" to st(9)))
    private val loans = mapOf("UNION Loan 0126" to st(12))
    private fun book(reg: Map<String, Pair<String, String>> = emptyMap()) =
        AccountBook.build(labels, auto, loans, reg, emptyMap(), emptyMap())

    @Test
    fun namesAndNumbers() {
        assertEquals("Account ending 76", AccountBook.displayName("76"))
        assertEquals("MGB 883076", AccountBook.displayName("MGB 883076"))
        assertEquals("HDFC", AccountBook.bankOf("HDFC CC 9065"))
        assertEquals("UNION", AccountBook.bankOf("UNION Loan 0126"))
        assertEquals("BARODA UP", AccountBook.bankOf("BARODA UP 1234"))
        assertEquals(null, AccountBook.bankOf("76"))
        assertEquals("9065", AccountBook.numberOf("HDFC CC 9065"))
        assertTrue(AccountBook.numberMatch("76", "MGB 883076"))
        assertFalse(AccountBook.numberMatch("HDFC 0025", "HDFC 5997"))
    }

    @Test
    fun partsShowUnderTheirAccount() {
        val b = book(mapOf("SBI 3076" to ("duplicate" to "MGB 883076")))
        val mgb = b.first { it.key == "MGB 883076" }
        assertEquals(listOf("MGB 83076", "SBI 3076"), mgb.parts.map { it.key })
        assertTrue(mgb.parts.first { it.key == "MGB 83076" }.auto)
        // The auto part's payments are already in the account's own count; the joined one adds its own.
        assertEquals(26, mgb.count)
        assertFalse(b.any { it.key == "SBI 3076" })
    }

    @Test
    fun similarPutsMatchingNumbersFirst() {
        val b = book()
        val s = AccountBook.similar(b, b.first { it.key == "76" }).map { it.key }
        assertEquals(listOf("MGB 883076", "SBI 3076"), s.take(2))
        assertFalse("cards only with cards", s.contains("HDFC CC 9065"))
        assertFalse("loans only from their own sheet", s.contains("UNION Loan 0126"))
        val h = AccountBook.similar(b, b.first { it.key == "HDFC 5997" }).map { it.key }
        assertEquals(listOf("HDFC 1208", "HDFC 0025", "76", "48453"), h)
        assertEquals(listOf("UNION 0126"), AccountBook.similar(b, b.first { it.key == "UNION Loan 0126" }).map { it.key })
    }

    @Test
    fun joiningKeepsTheNameWithABank() {
        val b = book()
        val p = AccountBook.plan(b, emptyMap(), "76", setOf("MGB 883076"), "savings")
        assertEquals("MGB 883076", p.main)
        assertEquals("duplicate" to "MGB 883076", p.registry["76"])
        // From the debit card's sheet, the account with more payments keeps its name.
        val d = AccountBook.plan(b, emptyMap(), "HDFC 1208", setOf("HDFC 5997"), "savings")
        assertEquals("HDFC 5997", d.main)
        assertEquals("duplicate" to "HDFC 5997", d.registry["HDFC 1208"])
    }

    @Test
    fun untickingSeparatesAndKeepsApart() {
        val b = book(mapOf("SBI 3076" to ("duplicate" to "MGB 883076")))
        val p = AccountBook.plan(b, mapOf("SBI 3076" to ("duplicate" to "MGB 883076")), "MGB 883076", setOf("SBI 3076"), "savings")
        assertEquals("accepted" to "savings", p.registry["MGB 83076"])
        assertTrue(p.apartAdd.contains("MGB 83076"))
        assertFalse(p.registry.containsKey("SBI 3076"))
        val q = AccountBook.plan(b, mapOf("SBI 3076" to ("duplicate" to "MGB 883076")), "MGB 883076", emptySet(), "savings")
        assertEquals("accepted" to "savings", q.registry["SBI 3076"])
    }

    @Test
    fun joiningAnAccountBringsItsPartsAlong() {
        val reg = mapOf("HDFC 1208" to ("duplicate" to "HDFC 0025"))
        val b = book(reg)
        val p = AccountBook.plan(b, reg, "HDFC 5997", setOf("HDFC 0025"), "savings")
        assertEquals("HDFC 5997", p.main)
        assertEquals("duplicate" to "HDFC 5997", p.registry["HDFC 0025"])
        assertEquals("duplicate" to "HDFC 5997", p.registry["HDFC 1208"])
    }

    @Test
    fun loanKeepsItsName() {
        val b = book()
        val p = AccountBook.plan(b, emptyMap(), "UNION Loan 0126", setOf("UNION 0126"), "loan")
        assertEquals("UNION Loan 0126", p.main)
        assertEquals("duplicate" to "UNION Loan 0126", p.registry["UNION 0126"])
    }

    @Test
    fun typeChangeRenamesAndMovesParts() {
        val reg = mapOf("HDFC 1208" to ("duplicate" to "HDFC 0025"))
        val b = book(reg)
        val p = AccountBook.plan(b, reg, "HDFC 0025", setOf("HDFC 1208"), "card")
        assertEquals("HDFC CC 0025", p.main)
        assertEquals("accepted" to "card", p.registry["HDFC 0025"])
        assertEquals("duplicate" to "HDFC CC 0025", p.registry["HDFC 1208"])
    }

    @Test
    fun continueAcceptsWhatIsShown() {
        val reg = mapOf("48453" to ("declined" to "savings"), "HDFC 5997" to ("accepted" to "savings"))
        val c = AccountBook.confirmPlan(book(reg), reg)
        assertFalse(c.containsKey("48453"))
        assertFalse(c.containsKey("HDFC 5997"))
        assertEquals("accepted" to "card", c["HDFC CC 9065"])
        assertEquals("accepted" to "loan", c["UNION Loan 0126"])
    }

    @Test
    fun screenOrder() {
        val (known, unknown) = AccountBook.ordered(book())
        assertEquals(listOf("76", "48453"), unknown.map { it.key })
        assertEquals("HDFC 5997", known.first().key)
        assertEquals(listOf("HDFC 5997", "HDFC 1208", "HDFC 0025", "HDFC CC 9065"), known.take(4).map { it.key })
    }

    @Test
    fun loanFixedAndRecurringDepositTypes() {
        assertEquals("HDFC Loan 0025", AccountBook.relabel("HDFC 0025", "loan"))
        assertEquals("HDFC FD 0025", AccountBook.relabel("HDFC 0025", "fd"))
        assertEquals("HDFC RD 0025", AccountBook.relabel("HDFC FD 0025", "rd"))
        assertEquals("HDFC 0025", AccountBook.relabel("HDFC RD 0025", "savings"))
        assertEquals("fd", AccountBook.guessType("HDFC FD 0025"))
        assertEquals("HDFC", AccountBook.bankOf("HDFC RD 0025"))
        val b = book()
        val p = AccountBook.plan(b, emptyMap(), "HDFC 0025", emptySet(), "fd")
        assertEquals("HDFC FD 0025", p.main)
        assertEquals("accepted" to "fd", p.registry["HDFC FD 0025"])
    }

    @Test
    fun debitCardLinksToTheOnlySavingsAccountAtItsBank() {
        val ls = mapOf("HDFC DC 7777" to st(5), "HDFC 5997" to st(40), "SBI DC 1111" to st(2), "SBI 2222" to st(3), "SBI 3333" to st(4))
        val b = AccountBook.build(ls, emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())
        val hd = b.first { it.key == "HDFC DC 7777" }
        assertEquals("debit", hd.type)
        assertEquals("HDFC", hd.bank)
        assertEquals("HDFC 5997", AccountBook.cardLink(b, hd)?.key)
        // Two savings accounts at the bank: nothing is guessed.
        assertEquals(null, AccountBook.cardLink(b, b.first { it.key == "SBI DC 1111" }))
        // Confirming links it; the savings account keeps its name and the card counts under it.
        val p = AccountBook.plan(b, emptyMap(), "HDFC DC 7777", setOf("HDFC 5997"), "debit")
        assertEquals("HDFC 5997", p.main)
        assertEquals("duplicate" to "HDFC 5997", p.registry["HDFC DC 7777"])
        // A savings account is offered its bank's debit cards, a debit card only savings accounts.
        assertTrue(AccountBook.similar(b, b.first { it.key == "HDFC 5997" }).any { it.key == "HDFC DC 7777" })
        assertFalse(AccountBook.similar(b, hd).any { it.type == "debit" })
    }

    @Test
    fun cardNetworkIsReadFromTheMessage() {
        assertEquals("Visa", AccountBook.networkIn("Spent on HDFC Bank VISA Credit Card ending 1111"))
        assertEquals("Mastercard", AccountBook.networkIn("Your Master Card ending 2222 was used"))
        assertEquals("RuPay", AccountBook.networkIn("RuPay credit card xx3333"))
        assertEquals("Amex", AccountBook.networkIn("American Express Card"))
        assertEquals(null, AccountBook.networkIn("Rs 100 spent at Visakhapatnam store"))
    }
}
