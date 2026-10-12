package com.nagesh.zelu

object Classifier {

    // Category -> keywords matched against counterparty and SMS text (case-insensitive).
    val CATEGORIES: LinkedHashMap<String, List<String>> = linkedMapOf(
        "Food" to listOf("swiggy", "zomato", "restaurant", "cafe", "bakery"),
        "Groceries" to listOf("blinkit", "zepto", "bigbasket", "dmart", "instamart"),
        "Transport" to listOf("uber", "ola", "rapido", "irctc", "metro", "petrol", "fuel"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "meesho"),
        "Bills" to listOf("airtel", "jio", "electricity", "recharge", "broadband", "gas"),
        "Entertainment" to listOf("netflix", "spotify", "bookmyshow", "hotstar"),
    )

    data class Result(val kind: String, val category: String)

    fun classify(t: Txn, ownAccounts: List<String>): Result {
        val cp = (t.counterparty ?: "").lowercase()

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
        return Result("expense", "Uncategorized")
    }
}
