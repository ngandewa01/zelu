package com.nagesh.zelu

object Classifier {

    // Category -> keywords, matched against counterparty + SMS text (lowercased).
    // Order matters: earlier entries win.
    val CATEGORIES: LinkedHashMap<String, List<String>> = linkedMapOf(
        "Food" to listOf("swiggy", "zomato", "restaurant", "cafe", "bakery", "dominos", "mcdonald", "kfc", "pizza", "eatfit", "brewpub", "dineout", "barbeque", "biryani", "subway", "starbucks", "dunkin", "burger king", "wendys", "haldiram", "wow momo", "faasos", "behrouz", "box8", "theobroma", "keventers", "baskin", "chaayos", "third wave", "blue tokai"),
        "Groceries" to listOf("blinkit", "zepto", "bigbasket", "dmart", "d mart", "avenue supermart", "q mart", "qmart", "pure o natural", "instamart", "grofers", "supermarket", "kirana", "reliance fresh", "reliance smart", "avm", "more retail", "jiomart", "innovative retail"),
        "Milk & Dairy" to listOf("country delight", "countrydelight", "milkbasket", "milk basket", "suprdaily", "supr daily", "bbdaily", "bb daily", "mother dairy", "amul", "heritage milk", "milk", "dairy"),
        "Fuel" to listOf("petrol", "fuel", "hpcl", "iocl", "bpcl", "indian oil", "shell", "fuel station"),
        "FASTag" to listOf("toll", "toll plaza", "fastag", "fasttag", "netc"),
        "Transport" to listOf("uber", "ola", "rapido", "metro", "bmtc", "redbus", "namma yatri"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "meesho", "ajio", "nykaa", "tatacliq", "croma", "reliance digital", "shoppers stop", "lifestyle", "decathlon", "ikea", "westside", "pantaloons", "firstcry", "pepperfry", "urban ladder", "lenskart", "titan", "tanishq", "caratlane", "bata", "vijay sales", "max fashion", "reliance trends"),
        "Investment" to listOf("zerodha", "groww", "upstox", "kuvera", "smallcase", "indmoney", "paytm money", "angel one", "angelone", "icici direct", "hdfc securities", "demat", "mutual fund", "elss", "tier1", "tier 1", "tier2", "nps", "pran", " ppf ", "nippon india", "sovereign gold"),
        "Gym" to listOf("cult.fit", "cultfit", "cure.fit", "curefit", "gold's gym", "golds gym", "anytime fitness", "fitness first", "healthifyme", "gym ", "fitso"),
        "Personal care" to listOf("salon", "spa ", "barber", "haircut", "parlour", "parlor", "jawed habib", "lakme salon", "looks salon", "enrich", "bodycraft", "naturals salon"),
        "Cash" to listOf(" atm", "atm ", "cash wdl", "cash withdrawal", "cash withdrawn", "self wdl"),
        "Home services" to listOf("urban company", "urbancompany", "urbanclap", "urban clap", "housejoy", "nobroker", "handyman"),
        "Entertainment" to listOf("netflix", "spotify", "bookmyshow", "hotstar", "prime video", "jiocinema", "google play", "youtube", "sony liv", "zee5", "pvr", "inox", "cinepolis", "audible", "gaana", "wynk", "jiohotstar"),
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

    // Name-pattern heuristics: generalize to merchants we'll never have in any list.
    // "Sri Balaji Medicals", "Annapurna Supermarket", "Balaji Travels" all classify from
    // a generic word in the name. Checked after the exact dictionary, before giving up.
    private val NAME_HINTS: List<Pair<Regex, String>> = listOf(
        Regex("""\b(travels?|tours|holidays|airlines|airways|aviation)\b""", RegexOption.IGNORE_CASE) to "Travel",
        Regex("""\b(medical|medicals|pharmacy|pharma|chemist|hospital|hospitals|clinic|clinics|diagnostic|diagnostics|pathology|healthcare|dental|dentist)\b""", RegexOption.IGNORE_CASE) to "Health",
        Regex("""\b(supermarket|super\s+market|kirana|groceries|grocery|provision|provisions|general\s+store|vegetable|vegetables|dairy\s+farm)\b""", RegexOption.IGNORE_CASE) to "Groceries",
        Regex("""\b(restaurant|cafe|hotel|foods|kitchen|dhaba|sweets|bakery|bakers|tiffin|biryani|darshini|caterers?|juice|snacks)\b""", RegexOption.IGNORE_CASE) to "Food",
        Regex("""\b(petrol|petroleum|fuels?|filling\s+station|petro|gas\s+station)\b""", RegexOption.IGNORE_CASE) to "Fuel",
        Regex("""\b(salon|spa|beauty|parlour|parlor|barber|unisex|grooming)\b""", RegexOption.IGNORE_CASE) to "Personal care",
        Regex("""\b(school|college|university|academy|institute|tuition|coaching|vidyalaya|education|classes)\b""", RegexOption.IGNORE_CASE) to "Education",
        Regex("""\b(electronics|mobiles)\b""", RegexOption.IGNORE_CASE) to "Shopping",
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
        // 1) Exact brand dictionary (most specific).
        for ((category, words) in CATEGORIES) {
            if (words.any { hay.contains(it) }) return Result("expense", category)
        }
        // 2) Name-pattern heuristics (generalize to unknown local merchants).
        for ((re, category) in NAME_HINTS) {
            if (re.containsMatchIn(hay)) return Result("expense", category)
        }
        // 3) Genuinely unguessable -> ask once (then remembered per payee).
        return Result("expense", "Unknown")
    }
}
