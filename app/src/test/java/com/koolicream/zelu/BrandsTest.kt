package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Test

class BrandsTest {
    private fun cat(payee: String, raw: String = "Rs 500 paid to $payee via UPI"): String =
        Classifier.classify(Txn("t", "2026-10-17", 50_000, Dir.DEBIT, payee, null, raw, "HDFCBK", "HDFC 1234", 0L), emptyList()).category

    @Test fun spacedNamesFindTheBrand() {
        assertEquals("Food", cat("MC DONALDS", "ICICI Bank Acct XX1234 debited for Rs 54.60 on 17-Oct-26; MC DONALDS credited. UPI:1"))
        assertEquals("Food", cat("Domino's Pizza"))
        assertEquals("Transport", cat("Red Bus"))
    }

    @Test fun bakeriesAndCaterersAreFood() {
        for (n in listOf("Catering Service", "SRI SAI CATERING SERVICES", "Lakshmi Bakers", "Hot Bake House", "New Star Bakeries", "Royal Confectionery", "Iyengar Bakery", "Sri Bekery", "New Bekeries"))
            assertEquals(n, "Food", cat(n))
    }

    @Test fun fuelStationsAreFuel() {
        for (n in listOf("HPCL", "IOCL", "BPCL", "Jio Oil", "Jio BP", "Nayara Petrol", "Nayara Energy", "HP Petrol Pump", "Indian Oil", "Sri Ganesh Petrol Bunk", "BPCL Retail Outlet", "HPCL Fuel Station"))
            assertEquals(n, "Fuel", cat(n))
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

    @Test fun gymsAndFitness() {
        assertEquals("Health", cat("HIGHLIFE CROSSFIT", "Sent Rs.16000.00 From HDFC Bank A/C *5997 To HIGHLIFE CROSSFIT On 01/10/26 Ref 1"))
        assertEquals("Health", cat("Cult.fit"))
        assertEquals("Health", cat("POWER HOUSE GYM"))
        assertEquals("Health", cat("Snap Fitness"))
        assertEquals("Health", cat("Talwalkars"))
        assertEquals("Health", cat("F45 Training"))
        assertEquals("Health", cat("Playo"))
        assertEquals("Health", cat("Ace Badminton Academy"))
        // Not gyms: EatFit is food, a gymkhana club isn't caught by "gym".
        assertEquals("Food", cat("EatFit"))
        assertEquals("Unknown", cat("Deccan Gymkhana"))
    }

    @Test fun maxIsTheShopUnlessItIsHealthOrInsurance() {
        assertEquals("Shopping", cat("Max"))
        assertEquals("Shopping", cat("MAX FASHION"))
        assertEquals("Health", cat("Max Healthcare", "Rs 5000 paid to Max Healthcare hospital via UPI"))
    }
}
