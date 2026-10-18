package com.nagesh.zelu

object Classifier {

    // Category -> keywords, matched against counterparty + SMS text (lowercased).
    // Order matters: earlier entries win.
    // Old built-in names folded into the standard set (applied to stored rules/overrides too).
    val ALIAS = mapOf("Milk & Dairy" to "Groceries", "FASTag" to "Transport", "Gym" to "Health", "Internet" to "Bills",
        "Mobile" to "Bills", "Laundry" to "Home services", "Maid" to "Home services", "Domestic help" to "Home services")

    val CATEGORIES: LinkedHashMap<String, List<String>> = linkedMapOf(
        "Food" to listOf("momentale", "swiggy", "bundl", "zomato", "restaurant", "cafe", "bakery", "dominos", "mcdonald", "kfc", "pizza", "eatfit", "brewpub", "dineout", "barbeque", "biryani", "subway", "starbucks", "dunkin", "burger king", "wendys", "haldiram", "wow momo", "faasos", "behrouz", "box8", "theobroma", "keventers", "baskin", "chaayos", "third wave", "blue tokai", "eatsure", "jubilant food", "pizza hut", "pizzahut", "devyani", "sapphire foods", "popeyes", "taco bell", "barista", "cafe coffee day", "ccd", "monginis", "bikaner", "saravana", "sagar ratna", "mainland china", "chinese wok", "chowman", "kwality wall", "mad over donuts", "jumboking", "papa john", "smokin joe", "oven story", "ovenstory", "punjab grill", "chai point", "nic ice cream", "mithai", "sweet shop", "sweets", "hungerbox", "hunger box", "kohinoor", "olive mithai"),
        "Groceries" to listOf("blinkit", "zepto", "bigbasket", "big basket", "dmart", "d mart", "avenue supermart", "q mart", "qmart", "pure o natural", "bbnow", "first club", "instamart", "grofers", "supermarket", "kirana", "reliance fresh", "reliance smart", "avm", "more retail", "jiomart", "innovative retail", "country delight", "countrydelight", "milkbasket", "milk basket", "suprdaily", "supr daily", "bbdaily", "bb daily", "mother dairy", "amul", "heritage milk", "milk", "dairy"),
        "Fuel" to listOf("petrol", "fuel", "hpcl", "iocl", "bpcl", "indian oil", "shell", "fuel station"),
        "Transport" to listOf("uber", "ola", "rapido", "metro", "bmtc", "redbus", "namma yatri", "toll", "toll plaza", "fastag", "fasttag", "netc"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "meesho", "ajio", "nykaa", "tatacliq", "croma", "reliance digital", "shoppers stop", "lifestyle", "decathlon", "ikea", "westside", "pantaloons", "firstcry", "pepperfry", "urban ladder", "lenskart", "titan", "tanishq", "caratlane", "bata", "vijay sales", "max fashion", "reliance trends", "trends", "oneplus", "one plus", "mi store", "samsung", "shopping mall", "yousta", "urbanofashion", "urbano fashion", "urbano", "zudio", "trendz", "centro",
            "van heusen", "vanheusen", "louis philippe", "allen solly", "peter england", "blackberrys", "raymond", "park avenue", "color plus", "colorplus",
            "jockey", "jack & jones", "jack and jones", "vero moda", "pepe jeans", "flying machine", "us polo", "uspa", "united colors of benetton", "benetton",
            "tommy hilfiger", "calvin klein", "h&m", "zara", "uniqlo", "forever 21", "fabindia", "fab india", "w for woman", "wforwoman", "biba", "aurelia",
            "global desi", "soch", "manyavar", "mohey", "levis", "levi's", "wrangler", "spykar", "mufti", "snitch", "bewakoof", "the souled store",
            "metro shoes", "mochi", "red tape", "redtape", "woodland", "skechers", "nike", "adidas", "puma", "reebok", "crocs", "hush puppies", "clarks", "sparx", "relaxo", "liberty shoes",
            "brand factory", "home centre", "home center", "nilkamal", "godrej interio", "hopscotch",
            "poorvika", "sangeetha mobiles", "bajaj electronics", "girias", "pai international", "lot mobiles",
            "kalyan jewellers", "malabar gold", "joyalukkas", "pc jeweller", "senco", "fastrack", "sephora", "tira beauty", "health & glow", "health and glow", "snapdeal", "reliance brands", "reliance retail", "shoppe", "shoe mart"),
        "Investment" to listOf("zerodha", "groww", "upstox", "kuvera", "smallcase", "indmoney", "paytm money", "angel one", "angelone", "icici direct", "hdfc securities", "demat", "mutual fund", "elss", "tier1", "tier 1", "tier2", " ppf ", "nippon india", "sovereign gold"),
        "Personal care" to listOf("salon", "spa ", "barber", "haircut", "parlour", "parlor", "jawed habib", "lakme salon", "looks salon", "enrich", "bodycraft", "naturals salon", "toni & guy", "toni and guy", "tony and guy", "tony n guy", "toniguy", "tony guy", "bblunt", "green trends"),
        "ATM / Cash" to listOf(" atm", "atm ", "cash wdl", "cash withdrawal", "cash withdrawn", "self wdl", "cash deposit", "cardless cash"),
        "Wallet" to emptyList(), // detected specially (wallet loads); listed so it can be filtered/picked
        "Home services" to listOf("urban company", "urbancompany", "urbanclap", "urban clap", "housejoy", "nobroker", "handyman", "laundry", "dryclean", "dry clean", "uclean", "pressto", "washmart", "laundromat", "maid", "housemaid", "domestic help", "househelp", "house help", "nanny", "babysitter"),
        "Entertainment" to listOf("netflix", "spotify", "bookmyshow", "hotstar", "prime video", "jiocinema", "google play", "youtube", "sony liv", "zee5", "pvr", "inox", "cinepolis", "audible", "gaana", "wynk", "jiohotstar", "hungama", "eros now", "voot", "mx player", "alt balaji", "altbalaji"),
        "Subscriptions" to listOf("adobe", "microsoft", "office 365", "microsoft 365", "google one", "icloud", "dropbox", "canva", "openai", "chatgpt", "anthropic", "github", "notion", "figma", "grammarly", "linkedin"),
        "Bills" to listOf("electricity", "gas agency", "dth", "tata power", "bescom", "mseb", "bharat connect", "water bill", "gas bill", "broadband", "fibernet", "jiofiber", "jio fiber", "airtel xstream", "xstream", "act fibernet", "actcorp", "hathway", "excitel", "spectra", "tikona", "wifi", "mobile recharge", "recharge", "prepaid", "postpaid", "airtel", "jio", "vodafone", " vi ", "bsnl", "idea"),
        "Travel" to listOf("irctc", "makemytrip", "goibibo", "cleartrip", "indigo", "vistara", "air india", "oyo", "airbnb", "ixigo"),
        "Donation" to listOf("donation", "donate", "charity", "giveindia", "give india", "ketto", "milaap", "iskcon", "tirumala", "ttd", "relief fund", "seva", "temple", "ngo"),
        "EMI" to listOf("emi", "loan", "installment", "instalment", "bajaj finance", "finserv", "nbfc", "ach d", "ach/", "ach-", "nach", "e-mandate", "emandate", "ecs"),
        "Health" to listOf("pharmeasy", "1mg", "apollo", "netmeds", "pharmacy", "clinic", "diagnostic", "medplus", "practo", "tata 1mg", "wellness forever", "wellness", "medical store", "dawakhana", "cult.fit", "cultfit", "cure.fit", "curefit", "gold's gym", "golds gym", "anytime fitness", "fitness first", "healthifyme", "gym ", "fitso"),
        "Education" to listOf("school fee", "tuition", "college fee", "university", "byju", "unacademy", "udemy", "coursera", "vedantu", "upgrad", "exam fee", "coaching"),
        "Rent" to listOf("rent"),
    )

    // A credit-card bill payment: not a new spend, it settles spends already counted.
    private val CARD_BILL = Regex(
        """(credit card.{0,20}(payment|bill|paid|autopay|auto[- ]?debit)|payment (received|towards).{0,20}credit card|\bcc bill\b|card bill paid)""",
        RegexOption.IGNORE_CASE,
    )

    // CRED is used almost entirely to pay credit-card bills.
    private val CRED = Regex("""\bcred\b""", RegexOption.IGNORE_CASE)

    // Prepaid / app wallets (Paytm, PhonePe, Amazon Pay, Mobikwik…). Zelu reads bank
    // SMS, so the money leaves your account when you LOAD the wallet — that load is the
    // real spend. Spending from the wallet balance afterwards would double-count.
    private val WALLET = Regex(
        """\b(paytm wallet|amazon ?pay|amazonpay|phonepe wallet|phonepe balance|mobikwik|freecharge|jio ?money|jio ?wallet|airtel ?money|payzapp|ola money|wallet balance|e-?wallet|\bwallet\b)\b""",
        RegexOption.IGNORE_CASE,
    )
    // Money going OUT of the wallet (a spend from balance) or cashback landing in it —
    // not counted, to avoid double-counting after the bank load was already booked.
    private val WALLET_SPEND = Regex(
        """\b(from|via|using|thru|through)\b[a-z' ]{0,14}wallet\b|wallet[a-z' ]{0,14}\b(spent|paid|debited|purchase)|cashback""",
        RegexOption.IGNORE_CASE,
    )

    // Paying a BNPL / postpaid bill settles a balance — a transfer, not a fresh spend.
    private val BNPL = Regex("""\b(olamoney|ola money|lazypay|simpl|paylater|pay later|postpaid account)\b""", RegexOption.IGNORE_CASE)

    // A plain multi-word personal name (no business word) — a person, not a merchant.
    private val PERSON_NAME = Regex("""^[A-Za-z]{2,}(?: [A-Za-z]{2,}){1,3}$""")
    private val BUSINESS_WORD = Regex("""\b(ltd|limited|pvt|private|llp|inc|corp|corporation|company|systems?|software|technolog\w*|industries|enterprises?|retail|services?|solutions?|stores?|traders?|agenc\w*|mart|supermarket|foods?|motors?|infotech|labs?|hospital|clinic|pharma|bank|finance|insurance|capital|ventures?)\b""", RegexOption.IGNORE_CASE)

    // Money landing ON a credit card = a bill settlement, not income.
    private val CARD_RECV = Regex(
        """card ending \d|payment[^.]{0,25}\bcard\b|towards[^.]{0,20}card|to your[^.]{0,15}card|credit card (bill|payment)|received on your[^.]{0,30}credit card|credit card (xx|\*+)?\d""",
        RegexOption.IGNORE_CASE,
    )

    // Contributions to retirement / market investments: tracked separately, so they count
    // as neither spending nor income (PF is deducted pre-salary; NPS you transfer in).
    private val INVEST = Regex(
        """\b(epf|epfo|provident fund|passbook|uan|pf contribution|nps|tier\s?1|tier\s?2|pran|mutual fund|zerodha|groww|upstox|kuvera|smallcase|indmoney|demat|elss|folio|sip debit|icici direct|hdfc securities|paytm money|angel ?one|ppf|nippon india|sovereign gold|etmoney|5paisa|motilal)\b""",
        RegexOption.IGNORE_CASE,
    )

    // Name-pattern heuristics: generalize to merchants we'll never have in any list.
    // "Sri Balaji Medicals", "Annapurna Supermarket", "Balaji Travels" all classify from
    // a generic word in the name. Checked after the exact dictionary, before giving up.
    private val NAME_HINTS: List<Pair<Regex, String>> = listOf(
        Regex("""\b(travels?|tours|holidays|airlines|airways|aviation|railways?|irctc|bus\s*stand|roadways|rtc|resorts?|retreats?|suites?|rest\s*house|circuit\s*house|dharamshala|sarai|sarovar|getaways?|hideaways?|tourism|innkeeping|guest\s*services|travel\s*&\s*stay|stays?|escapes?)\b""", RegexOption.IGNORE_CASE) to "Travel",
        Regex("""\b(medical|medicals|pharmacy|pharma|chemist|hospital|hospitals|clinic|clinics|diagnostic|diagnostics|pathology|healthcare|dental|dentist)\b""", RegexOption.IGNORE_CASE) to "Health",
        Regex("""\b(supermarket|super\s+market|kirana|groceries|grocery|provision|provisions|general\s+store|vegetable|vegetables|dairy\s+farm)\b""", RegexOption.IGNORE_CASE) to "Groceries",
        Regex("""\b(restaurants?|restuarants?|cafe|hotel|foods?|kitchen|dhaba|sweets?|bakery|bakers|tiffin|biryani|darshini|caterers?|juice|snacks|burgers?|pizza|momos?|coffee|chai|ice ?cream|donuts?|doughnuts?|grill|brasserie|bistro|eatery|diner|dosa|chaat|patisserie|confection|sweet shop|mithai|restro|family restaurant)\b""", RegexOption.IGNORE_CASE) to "Food",
        Regex("""\b(petrol|petroleum|fuels?|filling\s+station|petro|gas\s+station)\b""", RegexOption.IGNORE_CASE) to "Fuel",
        Regex("""\b(salon|spa|beauty|parlour|parlor|barber|unisex|grooming)\b""", RegexOption.IGNORE_CASE) to "Personal care",
        Regex("""\b(school|college|university|academy|institute|tuition|coaching|vidyalaya|education|classes)\b""", RegexOption.IGNORE_CASE) to "Education",
        Regex("""\b(electronics|mobiles|shopping mall|mall|departmental store|garments|footwear|apparel|fashion|boutique|retail|retailer|readymade|collections?|wardrobe|shoes?|sarees?|silks?|kanchi|kanjeevaram|jewell?ers?|jewell?ery)\b""", RegexOption.IGNORE_CASE) to "Shopping",
    )

    // Card alerts end with an offer ("To convert this txn to EMI give a missed call... Know more about
    // EMI conversion at..."). That says nothing about what was bought, so it is cut before matching.
    private val EMI_OFFER = Regex(
        """[^.]*\b(?:convert\w*|conversion)\b[^.]*\bemi\b[^.]*|[^.]*\bemi\b[^.]*\b(?:convert\w*|conversion)\b[^.]*""",
        RegexOption.IGNORE_CASE,
    )

    private fun onCard(account: String) = account.contains(" CC ") || account.startsWith("CC ") || account.endsWith(" CC")

    data class Result(val kind: String, val category: String)

    // A plain-language reason for why a transaction got its label — shown in the detail dialog.
    fun explain(counterparty: String?, raw: String, kind: String, category: String): String {
        val hay = "${counterparty ?: ""} ${EMI_OFFER.replace(raw, " ")}".lowercase()
        return when (kind) {
            "income" -> "Counted as money in — a credit to your account."
            "refund" -> "A refund credited back to you."
            "invest" -> "Tracked as an investment (PF/NPS/funds) — neither spending nor income."
            "transfer" -> when (category) {
                "Card bill" -> "A credit-card bill payment — it settles spends already counted, so it isn't counted again."
                "Wallet" -> "Money moved to/from a wallet — counted once when it leaves your bank."
                else -> "Looks like a transfer — between your own accounts, or money sent to a person."
            }
            "lent" -> "You tagged this as money lent out."
            "borrowed" -> "You tagged this as money you borrowed."
            "ignore" -> "You've marked this to be ignored, so it's left out of totals."
            else -> {
                val kw = CATEGORIES[category]?.firstOrNull { hay.contains(it) }
                when {
                    kw != null -> "Categorised as $category because \"$kw\" appeared in the message."
                    category == "Unknown" -> "No known merchant matched — tag it and Zelu will remember this payee."
                    else -> "Categorised as $category from the payee name."
                }
            }
        }
    }

    fun classify(t: Txn, ownAccounts: List<String>): Result {
        val cp = (t.counterparty ?: "").lowercase()

        // Paying a credit-card bill (directly or via CRED) is a transfer, not a new spend.
        if (t.direction == "debit" && (CARD_BILL.containsMatchIn(t.raw) || CRED.containsMatchIn(t.raw) || BNPL.containsMatchIn(t.raw))) {
            return Result("transfer", "Card bill")
        }

        // Money moving between your own accounts is not income or spending.
        if (cp.isNotEmpty() && ownAccounts.any { it.isNotEmpty() && cp.contains(it.lowercase()) }) {
            return Result("transfer", "Transfer")
        }

        // Retirement / market contributions are tracked on their own, not spend or income.
        if (INVEST.containsMatchIn(t.raw) || INVEST.containsMatchIn(cp)) {
            return Result("invest", "Investment")
        }

        // Wallets: loading a wallet from your bank IS the spend (the money has left your
        // account), so a wallet load counts as an expense. Spending FROM the wallet, or
        // cashback landing in it, is NOT counted again — that would double-count.
        if (WALLET.containsMatchIn(t.raw)) {
            if (t.direction == "credit" || WALLET_SPEND.containsMatchIn(t.raw))
                return Result("ignore", "Wallet")
            return Result("expense", "Wallet")
        }

        if (t.direction == "credit") {
            if (Parser.REFUND.containsMatchIn(t.raw)) return Result("refund", "Refund")
            // A payment landing on your credit card is settling a bill, not income.
            // Anything landing on an account we know is a credit card (e.g. "Payment of Rs X has
            // been received on your ... Credit Card XX8003 through BBPS") is the same thing.
            if (CARD_RECV.containsMatchIn(t.raw) || onCard(t.account))
                return Result("transfer", "Card bill")
            return Result("income", "Income")
        }

        // Names often arrive run together ("KimsHospital", "SriLakshmiMedicals"): also match on the
        // words split at each lower->UPPER change, so "hospital", "medical"... are found.
        val raw = EMI_OFFER.replace(t.raw, " ")
        val spaced = ((t.counterparty ?: "") + " " + raw).replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
        val hay = ("$cp $raw $spaced").lowercase()
        // 1) Exact brand dictionary (most specific).
        for ((category, words) in CATEGORIES) {
            if (words.any { hay.contains(it) }) return Result("expense", category)
        }
        // 2) Name-pattern heuristics (generalize to unknown local merchants).
        for ((re, category) in NAME_HINTS) {
            if (re.containsMatchIn(hay)) return Result("expense", category)
        }
        // 3) Money sent to a named person (not a business) is a transfer, not a spend. A card swipe
        //    is always a purchase at a merchant, so it never takes this path.
        val who = t.counterparty?.trim()
        if (who != null && !onCard(t.account) && PERSON_NAME.matches(who) && !BUSINESS_WORD.containsMatchIn(who)) {
            return Result("transfer", "Transfer")
        }
        // 4) Genuinely unguessable -> ask once (then remembered per payee).
        return Result("expense", "Unknown")
    }
}
