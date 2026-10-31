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

    /** Companies by their registered name, which is what banks print ("BIGTREE ENTERTAINMENT" is BookMyShow). */
    val LEGAL: LinkedHashMap<String, List<String>> = linkedMapOf(
        "Home services" to listOf("no broker", "housing.com", "magicbricks", "99acres", "square yards"),
        "Food" to listOf("absolute barbecue", "absolute barbecues", "paradise biryani", "paradise food", "la pino", "lapinoz", "saravana bhavan", "adyar ananda bhavan", "a2b", "mtr", "vidyarthi bhavan", "rameshwaram cafe", "naturals ice cream", "hard rock cafe", "truffles", "meghana foods", "shah ghouse", "bawarchi", "pista house", "chutneys", "minerva", "udupi", "kamat hotel", "hotel kamat", "karachi bakery", "cafe niloufer", "niloufer", "mehfil", "kritunga", "rayalaseema ruchulu", "ohri's", "ohris", "biryani blues", "biryani by kilo", "lunchbox", "freshmenu", "sweet truth", "krispy kreme", "third wave coffee", "tim hortons", "wendy's", "nando's", "nandos", "pizza express", "california burrito", "goli vada pav", "rolls mania", "kathi junction", "bercos", "berco's", "yo china", "beijing bites", "chicking", "ammi's biryani", "ammis biryani", "dhaba", "mandi house", "tiffin", "darshini", "smokehouse deli", "the bowl company", "wok express", "chili's", "haldirams", "sukh sagar", "shree sagar", "anand sweets", "g pulla reddy", "bundl technologies", "eternal limited", "eternal ltd", "jubilant foodworks", "devyani international", "westlife foodworld", "westlife development", "connaught plaza restaurants", "hardcastle restaurants", "coffee day", "tata starbucks", "barbeque nation", "restaurant brands asia", "rebel foods", "curefoods", "thickshake factory", "mojo pizza", "wow momo foods", "sunshine teahouse", "mountain trail foods", "haldiram snacks", "bikanervala", "burman hospitality", "yum restaurants", "subway systems", "domino"),
        "Groceries" to listOf("supermarket grocery supplies", "innovative retail concepts", "grofers india", "blink commerce", "kiranakart", "avenue supermarts", "spencers retail", "spencer's retail", "dunzo digital", "delightful gourmet", "vijetha supermarket"),
        "Shopping" to listOf("durian", "royaloak", "royal oak furniture", "damro", "evok", "furlenco", "rentomojo", "duroflex", "kurlon", "kurl-on", "sleepycat", "the sleep company", "livspace", "homelane", "furnitures", "furnishing", "furnishings", "furniture mart", "furniture world", "home decor", "flipkart internet", "instakart services", "myntra designs", "fsn e-commerce", "fsn ecommerce", "brainbees solutions", "lifestyle international", "v-mart retail", "vmart retail", "fashnear technologies", "lenskart solutions", "tata unistore", "infiniti retail", "decathlon sports", "ikea india", "bluestone jewellery", "titan company", "imagine marketing", "amazon seller services", "clicktech retail", "appario retail", "cloudtail india", "nykaa e-retail"),
        "Transport" to listOf("uber india systems", "roppen transportation", "blu-smart", "yulu bikes", "l&t metro rail", "l and t metro", "hyderabad metro", "delhi metro rail", "bangalore metro rail", "mumbai metro", "chennai metro rail"),
        "Travel" to listOf("guesthouse", "hosteller", "gostops", "resort", "stayvista", "ginger hotel", "ihcl", "taj hotel", "itc hotel", "novotel", "ibis", "holiday inn", "trident hotel", "the leela", "fortune hotel", "sarovar", "makemytrip india", "ibibo group", "yatra online", "le travenues", "oravel stays", "oyo rooms", "interglobe aviation", "snv aviation", "akasa air", "tata sia airlines", "easy trip planners", "casa2 stays"),
        "Entertainment" to listOf("aha ott", "arha media", "shemaroo", "shemaroome", "sony pictures", "erosnow", "jio cinema", "discovery plus", "lionsgate play", "apple tv", "youtube premium", "ullu", "chaupal", "stage ott", "planet marathi", "manorama max", "docubay", "tata play binge", "ottplay", "dangal play", "fancode", "addatimes", "klikk", "neestream", "etv win", "tvf play", "mubi", "crunchyroll", "jiosaavn", "pocket fm", "bigtree entertainment", "pvr inox", "pvr limited", "pvr ltd", "inox leisure", "miraj cinemas", "asian cinemas", "carnival cinemas", "netflix entertainment", "star india", "novi digital", "jiostar", "jio star", "viacom18", "viacom 18", "spotify india", "zee entertainment", "sony pictures networks", "sporta technologies", "galactus funware", "winzo", "district by zomato"),
        "Bills" to listOf("bharti airtel", "reliance jio infocomm", "jio infocomm", "vodafone idea", "tata sky", "bharat sanchar", "atria convergence", "bangalore electricity", "indraprastha gas"),
        "Health" to listOf("frank ross", "emami frank ross", "guardian pharmacy", "davaindia", "dava india", "generic aadhaar", "jan aushadhi", "janaushadhi", "pradhan mantri bhartiya", "healthkart", "mfine", "tata health", "medlife", "hinduja", "fortis", "manipal hospital", "narayana health", "aster dm", "aster hospital", "kims", "yashoda", "care hospital", "rainbow hospital", "max healthcare", "medanta", "aiims", "dr lal", "healthians", "vijaya diagnostic", "tata 1 mg", "zeno health", "dawaa dost", "medkart", "plus pharmacy", "noble plus", "sanjivani", "lifecare", "optum", "cloudnine", "motherhood hospital", "ankura", "medibuddy", "pristyn care", "clove dental", "sabka dentist", "apollo diagnostics", "neuberg", "suburban diagnostics", "agilus", "srl diagnostics", "apollo pharmacies", "apollo pharmacy", "apollo health and lifestyle", "apollo hospitals", "api holdings", "1mg technologies", "medplus health", "practo technologies", "dr lal pathlabs", "lal pathlabs", "metropolis healthcare"),
        "Fitness" to listOf("curefit healthcare"),
        "Investment" to listOf("zerodha broking", "nextbillion technology", "rksv securities", "angel broking", "icici securities", "indian clearing corporation", "bse star mf", "computer age management", "kfin technologies"),
        "Education" to listOf("think and learn", "sorting hat technologies", "vedantu innovations", "upgrad education"),
    )

    private val HEALTH_OR_INSURANCE = Regex("""\b(health|healthcare|hospital|insurance|life|clinic|pharmacy|medical)\b""", RegexOption.IGNORE_CASE)

    /** "Max" on its own is the shop, unless the message is about health or insurance (Max Healthcare, Max Life). */
    fun isBareMaxShop(payee: String, raw: String): Boolean =
        payee.trim().lowercase().let { it == "max" || it == "max retail" } && !HEALTH_OR_INSURANCE.containsMatchIn(raw)
}
