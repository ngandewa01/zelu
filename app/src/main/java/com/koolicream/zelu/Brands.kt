package com.koolicream.zelu

/**
 * Shops and apps by the group that owns them, so a payee is found whichever name it arrives under.
 * Used after the main word lists, so what was already matched keeps its category. Names of up to five letters
 * match only as a whole word (see Classifier.has); the owner's own name is listed too, because card statements show it.
 */
object Brands {
    val EXTRA: LinkedHashMap<String, List<String>> = linkedMapOf(
        "Home services" to listOf("snabbit", "pronto", "helpr"),
        "Transport" to listOf(
            "indrive", "in drive", "bharat taxi", "bharattaxi", "meru cabs", "meru taxi", "savaari", "blusmart", "zoomcar", "zoom car", "revv", "vogo",
            "borzo", "zingbus", "intrcity", "intr city", "flixbus", "flix bus", "paytm bus", "red bus", "uber india", "ani technologies", "roppen",
        ),
        "Groceries" to listOf("smart bazaar", "smart point", "metro cash", "spar hypermarket", "max hypermarket"),
        "Health" to listOf("netmeds", "tata 1mg", "1mg"),
        "Personal care" to listOf("tira beauty", "tira"),
        "Shopping" to listOf(
            // Aditya Birla group (ABFRL)
            "aditya birla fashion", "aditya birla lifestyle", "abfrl", "peter england", "louis philippe", "van heusen", "allen solly", "pantaloons", "style up",
            "reebok", "forever 21", "american eagle", "bewakoof", "tigc", "tasva", "jaypore", "sabyasachi", "tarun tahiliani", "shantnu", "masaba", "marigold lane",
            "ralph lauren", "ted baker", "hackett london", "fred perry", "simon carter", "aurelia", "wishful", "folksong", "elleven",
            // Reliance Retail
            "reliance retail", "reliance trends", "trends footwear", "azorte", "fashion factory", "amante", "zivame", "clovia", "shein", "hamleys", "reliance jewels",
            // Kewal Kiran and other menswear
            "kewal kiran", "killer jeans", "lawman", "integriti", "easies", "mufti", "highlander",
            // Tata group
            "trent ltd", "westside", "zudio", "utsa", "tanishq", "fastrack", "taneira", "caratlane", "titan eye", "tata cliq", "tatacliq", "tata neu", "croma",
            // Landmark group
            "landmark group", "max fashion", "max retail", "max lifestyle", "home centre", "centrepoint", "splash", "babyshop", "easybuy",
            // Arvind, Raymond, Vedant, Page, Metro, Shoppers Stop
            "arvind fashion", "us polo", "u s polo", "flying machine", "tommy hilfiger", "calvin klein",
            "park avenue", "colorplus", "parx", "raymond",
            "vedant fashions", "manyavar", "mohey", "mebaz", "twamev",
            "page industries", "jockey", "metro brands", "metro shoes", "mochi", "walkway",
            "shoppers stop", "intune", "home stop", "crossword",
            // Global and Indian clothing, shoes and home
            "nike", "puma", "adidas", "skechers", "levis", "levi's", "h&m", "zara", "uniqlo", "pepe jeans", "spykar", "jack & jones", "jack and jones", "vero moda",
            "wrangler", "fabindia", "biba", "woodland", "red tape", "redtape", "metro footwear", "crocs", "hush puppies", "snitch", "souled store",
            "hometown", "nilkamal", "wakefit", "sleepwell",
        ),
    )

    private val HEALTH_OR_INSURANCE = Regex("""\b(health|healthcare|hospital|insurance|life|clinic|pharmacy|medical)\b""", RegexOption.IGNORE_CASE)

    /** "Max" on its own is the shop, unless the message is about health or insurance (Max Healthcare, Max Life). */
    fun isBareMaxShop(payee: String, raw: String): Boolean =
        payee.trim().lowercase().let { it == "max" || it == "max retail" } && !HEALTH_OR_INSURANCE.containsMatchIn(raw)
}
