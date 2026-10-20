package com.nagesh.zelu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SmsModelTest {
    private val model get() = SmsModel.parse(File("src/main/assets/sms_model.tsv").readText())

    // Words and pairs must come out exactly as they do in the Python that trained the model.
    @Test
    fun tokensMatchThePythonTrainer() {
        val t = SmsModel.tokens("Debit\nINR 600.00\nA/c no. XX1234\n26-03-24 15:23:24 IST\nNon-Maint Avg bal chgs Jan\nChk Bal axisbank.com/W\nFor details, call 18001035577 - Axis Bank")
        assertEquals(listOf("debit", "inr", "num", "a", "c", "no", "num", "num", "num", "num", "num", "num"), t.take(12))
        assertTrue(t.contains("url") && t.contains("phone"))
    }

    @Test
    fun kindsAreTold() {
        val m = model
        val cases = listOf(
            "Debit\nINR 600.00\nA/c no. XX1234\n26-03-24 15:23:24 IST\nNon-Maint Avg bal chgs Jan\nChk Bal axisbank.com/W\nFor details, call 18001035577 - Axis Bank" to "fee",
            "Rs.250.00 spent on Card x4321 at SWIGGY on 12-05-26. Avl bal Rs.5000.00" to "spend",
            "INR 5000.00 credited to A/c XX1111 on 03-04-26 by UPI from RAHUL SHARMA" to "credit",
            "123456 is your OTP. Do not share with anyone" to "ignore",
            "Your credit card bill of Rs 4500.00 is due on 20-11-26. Pay now" to "reminder",
            // A balance-only message is not money received, even with an amount and "Available" in it.
            "Available Bal in HDFC Bank A/c XX7310 as on yesterday:02-NOV-26 is INR 4,512.30. Cheques are subject to clearing.For updated A/C Bal dial 18002703333." to "ignore",
            "Rs.500.00 credited to HDFC Bank A/c XX5997 on 13-10-26 from VPA anil@okicici. Avl bal INR 81,265.94" to "credit",
        )
        for ((text, want) in cases) assertEquals(text, want, m.best(text).first)
    }

    @Test
    fun answersTeachIt() {
        val m = model
        val odd = "Zorbo wallet topup of Rs 100 done at 10:22"
        val before = m.predict(odd)["fee"]!!
        repeat(3) { m.learn(odd, "fee") }
        assertTrue(m.predict(odd)["fee"]!! > before)
    }
}
