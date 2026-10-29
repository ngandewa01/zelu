package com.koolicream.zelu

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Which unread bank messages are worth listing under Missed messages.
class MissedMessagesTest {
    @Test fun declinedAndFailedAttemptsAreNotListed() {
        assertFalse(Ingest.looksLikeMoneyMoved("TXN DECLINED: Rs.2374.25 on 20-10-26 at 17:27 on HDFC Bank Debit Card xx1208. " +
            "Reason: Intl online usage disabled. Enable:https://1.hdfc.bank.in/HDFCBK/s/a/4KwPpj8x"))
        assertFalse(Ingest.looksLikeMoneyMoved("Your UPI txn of Rs 450 to SWIGGY has failed. Amount not debited."))
        assertFalse(Ingest.looksLikeMoneyMoved("Txn of INR 1,200 on card XX4417 was unsuccessful due to insufficient balance"))
    }

    @Test fun moneyThatCameBackIsStillListed() {
        assertTrue(Ingest.looksLikeMoneyMoved("Rs 500 credited to your a/c XX5997 for failed UPI txn 629145872310"))
        assertTrue(Ingest.looksLikeMoneyMoved("ATM txn failed. Rs.2000 reversed to your account XX1208"))
    }

    @Test fun ordinaryPaymentsAreStillListed() {
        assertTrue(Ingest.looksLikeMoneyMoved("Rs.349.00 debited from A/c XX5997 to SWIGGY via UPI"))
        assertFalse(Ingest.looksLikeMoneyMoved("Your OTP for txn of Rs 500 is 123456"))
    }
}
