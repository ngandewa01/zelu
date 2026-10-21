package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Test

class RedactTest {
    @Test
    fun hidesLongNumbersAndUpiNames() {
        assertEquals("Rs.450.00 debited from a/c **5997 to s***@icici Ref XXXXXXXXX678",
            Redact.message("Rs.450.00 debited from a/c **5997 to swiggy@icici Ref 607412345678"))
    }

    @Test
    fun keepsAmountsDatesAndShortNumbers() {
        val s = "Rs 1,23,456.78 spent on 12-03-26 at card 9012"
        assertEquals(s, Redact.message(s))
    }
}
