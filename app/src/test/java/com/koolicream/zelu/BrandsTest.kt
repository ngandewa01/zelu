package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Test

class BrandsTest {
    private fun cat(payee: String, raw: String = "Rs 500 paid to $payee via UPI"): String =
        Classifier.classify(Txn("t", "2026-10-17", 500.0, "debit", payee, null, raw, "HDFCBK", "HDFC 1234", 0L), emptyList()).category

    @Test fun spacedNamesFindTheBrand() {
        assertEquals("Food", cat("MC DONALDS", "ICICI Bank Acct XX1234 debited for Rs 54.60 on 17-Oct-26; MC DONALDS credited. UPI:1"))
        assertEquals("Food", cat("Domino's Pizza"))
        assertEquals("Transport", cat("Red Bus"))
    }

    @Test fun appsAndOwners() {
        assertEquals("Home services", cat("Snabbit"))
        assertEquals("Home services", cat("PRONTO"))
        assertEquals("Transport", cat("inDrive"))
        assertEquals("Transport", cat("Bharat Taxi"))
        assertEquals("Shopping", cat("Peter England"))
        assertEquals("Shopping", cat("ABFRL"))
        assertEquals("Shopping", cat("Aditya Birla Fashion and Retail Ltd"))
        assertEquals("Shopping", cat("Mufti"))
        assertEquals("Shopping", cat("HIGHLANDER"))
        assertEquals("Shopping", cat("Nike India"))
        assertEquals("Shopping", cat("Puma Sports"))
        assertEquals("Shopping", cat("Reliance Retail Ltd"))
        assertEquals("Health", cat("Netmeds"))
    }

    @Test fun maxIsTheShopUnlessItIsHealthOrInsurance() {
        assertEquals("Shopping", cat("Max"))
        assertEquals("Shopping", cat("MAX FASHION"))
        assertEquals("Health", cat("Max Healthcare", "Rs 5000 paid to Max Healthcare hospital via UPI"))
    }
}
