package com.koolicream.zelu

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Which unread bank messages are worth listing under Missed messages: the parser's own rules decide what is not a
// payment at all (Parser.isNotAPayment), so this list never disagrees with what the ledger reads.
class MissedMessagesTest {
    private fun listed(body: String) = Ingest.looksLikeMoneyMoved(body, "BP-HDFCBK-S")

    @Test fun declinedAndFailedAttemptsAreNotListed() {
        assertFalse(listed("TXN DECLINED: Rs.2374.25 on 20-10-26 at 17:27 on HDFC Bank Debit Card xx1208. " +
            "Reason: Intl online usage disabled. Enable:https://1.hdfc.bank.in/HDFCBK/s/a/4KwPpj8x"))
        assertFalse(listed("Your UPI txn of Rs 450 to SWIGGY has failed. Amount not debited."))
    }

    @Test fun moneyThatCameBackIsStillAPayment() {
        assertFalse(Parser.isNotAPayment("Rs.500.00 credited to A/c XX5997 on 18-10-26 against amount debited for failed UPI txn Ref 629634567891", "AX-HDFCBK-S"))
    }

    @Test fun ordinaryPaymentsAreStillListed() {
        assertTrue(listed("Rs.349.00 debited from A/c XX5997 to SWIGGY via UPI"))
        assertFalse(listed("Your OTP for txn of Rs 500 is 123456"))
    }
}
