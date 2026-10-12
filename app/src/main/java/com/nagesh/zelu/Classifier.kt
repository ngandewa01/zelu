package com.nagesh.zelu

object Classifier {

    // Category -> keywords, matched against counterparty + SMS text (lowercased).
    // Order matters: earlier entries win.
    val CATEGORIES: LinkedHashMap<String, List<String>> = linkedMapOf(
        "Food" to listOf("swiggy", "zomato", "restaurant", "cafe", "bakery", "dominos", "mcdonald", "kfc", "pizza", "eatfit", "brewpub", "dineout", "barbeque", "biryani"),
        "Groceries" to listOf("blinkit", "zepto", "bigbasket", "dmart", "instamart", "grofers", "supermarket", "kirana", "reliance fresh", "avm", "more retail", "jiomart"),
        "Fuel" to listOf("petrol", "fuel", "hpcl", "iocl", "bpcl", "indian oil", "shell", "fuel station"),
        "Transport" to listOf("uber", "ola", "rapido", "metro", "bmtc", "redbus", "namma yatri", "fastag"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "meesho", "ajio", "nykaa", "tatacliq", "croma", "reliance digital", "shoppers stop", "lifestyle"),
        "Bills" to listOf("airtel", "jio", "vodafone", " vi ", "bsnl", "electricity", "recharge", "broadband", "gas agency", "dth", "tata power", "bescom", "mseb", "postpaid", "bharat connect", "water bill"),
        "Entertainment" to listOf("netflix", "spotify", "bookmyshow", "hotstar", "prime video", "jiocinema", "google play", "youtube", "sony liv", "zee5"),
        "Travel" to listOf("irctc", "makemytrip", "goibibo", "cleartrip", "indigo", "vistara", "air india", "oyo", "airbnb", "ixigo"),
        "EMI" to listOf("emi", "loan", "bajaj finance", "finserv", "nbfc"),
        "Health" to listOf("pharmeasy", "1mg", "apollo", "netmeds", "pharmacy", "hospital", "clinic", "diagnostic", "medplus", "practo", "tata 1mg"),
        "Rent" to listOf("rent"),
    )

    // A credit-card bill payment: not a new spend, it settles spends already counted.
    private val CARD_BILL = Regex(
        """(credit card.{0,20}(payment|bill|paid|autopay|auto[- ]?debit)|payment (received|towards).{0,20}credit card|\bcc bill\b|card bill paid)""",
        RegexOption.IGNORE_CASE,
    )

    data class Result(val kind: String, val category: String)

    fun classify(t: Txn, ownAccounts: List<String>): Result {
        val cp = (t.counterparty ?: "").lowercase()

        // Paying a credit-card bill is a transfer, so spends aren't counted twice.
        if (t.direction == "debit" && CARD_BILL.containsMatchIn(t.raw)) {
            return Result("transfer", "Card bill")
        }

        // Money moving between your own accounts is not income or spending.
        if (cp.isNotEmpty() && ownAccounts.any { it.isNotEmpty() && cp.contains(it.lowercase()) }) {
            return Result("transfer", "Transfer")
        }

        if (t.direction == "credit") {
            return if (Parser.REFUND.containsMatchIn(t.raw)) Result("refund", "Refund")
            else Result("income", "Income")
        }

        val hay = "$cp ${t.raw}".lowercase()
        for ((category, words) in CATEGORIES) {
            if (words.any { hay.contains(it) }) {
                return Result("expense", category)
            }
        }
        return Result("expense", "Unknown")
    }
}
