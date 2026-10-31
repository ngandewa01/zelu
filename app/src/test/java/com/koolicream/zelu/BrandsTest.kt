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

    @Test fun chemistsAreHealth() {
        for (n in listOf("Sri Sai Drugist", "Krishna Chemist", "Shree Chemist and Druggist", "City Drug House", "Raj Drugs", "Lakshmi Druggists", "New Drug Store"))
            assertEquals(n, "Health", cat(n))
    }

    @Test fun categorySweep() {
        val cases = mapOf(
            "Sri Sai Mess" to "Food",
            "Infosys Canteen" to "Food",
            "Chai Wala" to "Food",
            "Hot Bhojanalay" to "Food",
            "Pizza Hut" to "Food",
            "Chai Point" to "Food",
            "Royal Fast Food" to "Food",
            "Sri Lassi Corner" to "Food",
            "Fresh Chicken Centre" to "Groceries",
            "Fruits and Vegetables" to "Groceries",
            "Aavin Milk Parlour" to "Groceries",
            "Sri Ram Mutton Stall" to "Groceries",
            "Hari Fish Market" to "Groceries",
            "Ganesh Flowers" to "Groceries",
            "Daily Mart" to "Groceries",
            "Ration Shop" to "Groceries",
            "Statiq" to "Fuel",
            "Tata Power EZ Charge" to "Fuel",
            "City CNG Station" to "Fuel",
            "Essar" to "Fuel",
            "Sri Service Station" to "Fuel",
            "City Taxi" to "Transport",
            "Rapido" to "Transport",
            "Namma Yatri" to "Transport",
            "Auto Rickshaw Ravi" to "Transport",
            "BMRCL" to "Transport",
            "Pothys Stationery" to "Shopping",
            "Sapna Book Store" to "Education",
            "Gift Gallery" to "Shopping",
            "Kids Toys World" to "Shopping",
            "Nilkamal Furniture" to "Shopping",
            "Sleepwell Mattress" to "Shopping",
            "Luggage Hub" to "Shopping",
            "Zerodha" to "Investment",
            "Kotak Securities" to "Investment",
            "SIP Mutual Fund" to "Investment",
            "NPS Trust Tier I" to "Investment",
            "Hair Studio" to "Personal care",
            "Iron Gym" to "Fitness",
            "Grand Bazar" to "Groceries",
            "Reliance Smart Bazar" to "Groceries",
            "Sri Vegetable Shop" to "Groceries",
            "Lakshmi Flour Mill" to "Groceries",
            "Ganesh Floor Mill" to "Groceries",
            "Balaji Stationary" to "Shopping",
            "Yoga Centre" to "Fitness",
            "Tattoo Studio" to "Personal care",
            "Glow Makeup" to "Personal care",
            "Nails Bar" to "Personal care",
            "Raj Plumber" to "Home services",
            "Ganesh Painter" to "Home services",
            "Car Wash Point" to "Home services",
            "Sparkle Cleaning" to "Home services",
            "Shree Pest Control" to "Home services",
            "Dream11" to "Entertainment",
            "Fun Amusement Park" to "Entertainment",
            "Game Zone" to "Entertainment",
            "Bowling Alley" to "Entertainment",
            "Claude" to "Subscriptions",
            "Apple" to "Subscriptions",
            "Hostinger" to "Subscriptions",
            "Cloudflare" to "Subscriptions",
            "BESCOM" to "Bills",
            "Water Board" to "Bills",
            "Torrent Power" to "Bills",
            "Tata Play" to "Bills",
            "Zostel Hostel" to "Travel",
            "Sunset Homestay" to "Travel",
            "VFS Global Visa" to "Travel",
            "Marriott" to "Travel",
            "MakeMyTrip" to "Travel",
            "Sri Ganesh Mandir" to "Donation",
            "St Marys Church" to "Donation",
            "Jama Masjid" to "Donation",
            "Sai Baba Trust" to "Donation",
            "Helping Foundation" to "Donation",
            "Bajaj Finance" to "EMI",
            "Sri Ram Finance" to "EMI",
            "Stashfin" to "EMI",
            "Home Loan EMI" to "EMI",
            "City Lab" to "Health",
            "Sri Scan Centre" to "Health",
            "Doctor Sharma" to "Health",
            "Pet Clinic" to "Health",
            "Sunrise Maternity Home" to "Health",
            "Rainbow Fertility Centre" to "Health",
            "Agilus Diagnostics" to "Health",
            "Vijaya Diagnostic" to "Health",
            "Dr Lal Lab" to "Health",
            "City IVF" to "Health",
            "Nursing Home" to "Health",
            "NPTEL" to "Education",
            "Bright Kindergarten" to "Education",
            "Little Montessori" to "Education",
            "Khan Tutorials" to "Education",
            "PG Owner" to "Rent",
            "Landlord Ramesh" to "Rent",
            "House Rent" to "Rent",
        )
        val bad = cases.filter { (n, c) -> cat(n) != c }.map { (n, c) -> "$n -> ${cat(n)} (wanted $c)" }
        assertEquals(emptyList<String>(), bad)
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
        assertEquals("Fitness", cat("HIGHLIFE CROSSFIT", "Sent Rs.16000.00 From HDFC Bank A/C *5997 To HIGHLIFE CROSSFIT On 01/10/26 Ref 1"))
        assertEquals("Fitness", cat("Cult.fit"))
        assertEquals("Fitness", cat("POWER HOUSE GYM"))
        assertEquals("Fitness", cat("Snap Fitness"))
        assertEquals("Fitness", cat("Talwalkars"))
        assertEquals("Fitness", cat("F45 Training"))
        assertEquals("Fitness", cat("Playo"))
        assertEquals("Fitness", cat("Ace Badminton Academy"))
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
