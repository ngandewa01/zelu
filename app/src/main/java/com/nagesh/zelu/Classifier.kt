package com.nagesh.zelu

object Classifier {

    // Category -> keywords, matched against counterparty + SMS text (lowercased).
    // Order matters: earlier entries win.
    val CATEGORIES: LinkedHashMap<String, List<String>> = linkedMapOf(
        "Food" to listOf("swiggy", "zomato", "restaurant", "cafe", "bakery", "dominos", "mcdonald", "kfc", "pizza", "eatfit", "brewpub", "dineout", "barbeque", "biryani"),
        "Groceries" to listOf("blinkit", "zepto", "bigbasket", "dmart", "d mart", "avenue supermart", "q mart", "qmart", "pure o natural", "instamart", "grofers", "supermarket", "kirana", "reliance fresh", "reliance smart", "avm", "more retail", "jiomart", "innovative retail"),
        "Milk & Dairy" to listOf("country delight", "countrydelight", "milkbasket", "milk basket", "suprdaily", "supr daily", "bbdaily", "bb daily", "mother dairy", "amul", "heritage milk", "milk", "dairy"),
        "Fuel" to listOf("petrol", "fuel", "hpcl", "iocl", "bpcl", "indian oil", "shell", "fuel station"),
        "Transport" to listOf("uber", "ola", "rapido", "metro", "bmtc", "redbus", "namma yatri", "fastag"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "meesho", "ajio", "nykaa", "tatacliq", "croma", "reliance digital", "shoppers stop", "lifestyle"),
        "Investment" to listOf("zerodha", "groww", "upstox", "kuvera", "smallcase", "indmoney", "paytm money", "angel one", "angelone", "icici direct", "hdfc securities", "demat", "mutual fund", "elss", " nps ", " ppf ", "nippon india", "sovereign gold"),
        "Gym" to listOf("cult.fit", "cultfit", "cure.fit", "curefit", "gold's gym", "golds gym", "anytime fitness", "fitness first", "healthifyme", "gym ", "fitso"),
        "Personal care" to listOf("salon", "spa ", "barber", "haircut", "parlour", "parlor", "jawed habib", "lakme salon", "looks salon", "enrich", "bodycraft", "naturals salon"),
        "Cash" to listOf(" atm", "atm ", "cash wdl", "cash withdrawal", "cash withdrawn", "self wdl"),
        "Home services" to listOf("urban company", "urbancompany", "urbanclap", "urban clap", "housejoy", "nobroker", "handyman"),
        "Entertainment" to listOf("netflix", "spotify", "bookmyshow", "hotstar", "prime video", "jiocinema", "google play", "youtube", "sony liv", "zee5"),
        "Internet" to listOf("broadband", "fibernet", "jiofiber", "jio fiber", "airtel xstream", "xstream", "act fibernet", "actcorp", "hathway", "excitel", "spectra", "tikona", "wifi"),
        "Mobile" to listOf("mobile recharge", "recharge", "prepaid", "postpaid", "airtel", "jio", "vodafone", " vi ", "bsnl", "idea"),
        "Bills" to listOf("electricity", "gas agency", "dth", "tata power", "bescom", "mseb", "bharat connect", "water bill", "gas bill"),
        "Travel" to listOf("irctc", "makemytrip", "goibibo", "cleartrip", "indigo", "vistara", "air india", "oyo", "airbnb", "ixigo"),
        "Laundry" to listOf("laundry", "dryclean", "dry clean", "uclean", "pressto", "washmart", "laundromat"),
        "Maid" to listOf("maid", "housemaid", "domestic help", "househelp", "house help", "nanny", "babysitter"),
        "Donation" to listOf("donation", "donate", "charity", "giveindia", "give india", "ketto", "milaap", "iskcon", "tirumala", "ttd", "relief fund", "seva", "temple", "ngo"),
        "EMI" to listOf("emi", "loan", "installment", "instalment", "bajaj finance", "finserv", "nbfc", "ach d", "ach/", "ach-", "nach", "e-mandate", "emandate", "ecs"),
        "Health" to listOf("pharmeasy", "1mg", "apollo", "netmeds", "pharmacy", "hospital", "clinic", "diagnostic", "medplus", "practo", "tata 1mg"),
        "Rent" to listOf("rent"),
    )

    // A credit-card bill payment: not a new spend, it settles spends already counted.
    private val CARD_BILL = Regex(
        """(credit card.{0,20}(payment|bill|paid|autopay|auto[- ]?debit)|payment (received|towards).{0,20}credit card|\bcc bill\b|card bill paid)""",
        RegexOption.IGNORE_CASE,
    )

    // CRED is used almost entirely to pay credit-card bills.
    private val CRED = Regex("""\bcred\b""", RegexOption.IGNORE_CASE)

    // Money landing ON a credit card = a bill settlement, not income.
    private val CARD_RECV = Regex(
        """card ending \d|payment[^.]{0,25}\bcard\b|towards[^.]{0,20}card|to your[^.]{0,15}card|credit card (bill|payment)""",
        RegexOption.IGNORE_CASE,
    )

    data class Result(val kind: String, val category: String)

    fun classify(t: Txn, ownAccounts: List<String>): Result {
        val cp = (t.counterparty ?: "").lowercase()

        // Paying a credit-card bill (directly or via CRED) is a transfer, not a new spend.
        if (t.direction == "debit" && (CARD_BILL.containsMatchIn(t.raw) || CRED.containsMatchIn(t.raw))) {
            return Result("transfer", "Card bill")
        }

        // Money moving between your own accounts is not income or spending.
        if (cp.isNotEmpty() && ownAccounts.any { it.isNotEmpty() && cp.contains(it.lowercase()) }) {
            return Result("transfer", "Transfer")
        }

        if (t.direction == "credit") {
            if (Parser.REFUND.containsMatchIn(t.raw)) return Result("refund", "Refund")
            // A payment landing on your credit card is settling a bill, not income.
            if (CARD_RECV.containsMatchIn(t.raw)) return Result("transfer", "Card bill")
            return Result("income", "Income")
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
