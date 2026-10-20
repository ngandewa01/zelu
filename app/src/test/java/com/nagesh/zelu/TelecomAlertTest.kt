package com.nagesh.zelu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TelecomAlertTest {
    @Test fun phoneBalanceAlertIsNotABankAccount() {
        val body = "Voice Call\nUsed: 12 min 34 sec\n\nCharged: Rs. 7.5\nBalance: Rs. 25.77"
        assertNull(Parser.parseBalance(body, 1L, "JZ-JioBln"))
    }

    @Test fun sendersContainingIobLettersAreNotIob() {
        val body = "Available balance in a/c XX1234 is Rs. 9000.00 as on 18-10-26."
        assertEquals(null, Parser.parseBalance(body, 1L, "JZ-JioBln")?.first?.takeIf { it.startsWith("IOB") })
        val real = Parser.parseBalance(body, 1L, "VM-IOBCHN-S")
        assertNotNull(real)
        assertEquals("IOB 1234", real!!.first)
    }
}
