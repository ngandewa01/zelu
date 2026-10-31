package com.koolicream.zelu

object Classifier {

    // Category -> keywords, matched against counterparty + SMS text (lowercased).
    // Order matters: earlier entries win.
    // Old built-in names folded into the standard set (applied to stored rules/overrides too).
    val ALIAS = mapOf("Milk & Dairy" to "Groceries", "FASTag" to "Transport", "Gym" to "Fitness", "Internet" to "Bills",
        "Mobile" to "Bills", "Laundry" to "Home services", "Maid" to "Domestic help")

    val CATEGORIES: LinkedHashMap<String, List<String>> = linkedMapOf(
        "Food" to listOf("pan shop", "paan shop", "namkeen", "glass house", "creamery", "almond house", "wow momo", "haldiram", "baskin", "robbin", "robbins", "busking robbin", "cream stone", "cream-stone", "creamstone", "pure veg", "pure vegetarian", "veg restaurant", "family restaurant", "food court",
            "tea house", "chaiwala", "chai wala", "mess", "canteen", "bhojanalay", "bhojan", "thali", "burger singh", "ice cream parlour", "ice cream parlor", "juice centre", "juice center", "fast food", "fastfood", "bar and restaurant", "bar & restaurant", "pub", "brewery", "sweets and snacks", "food court", "lassi", "falooda", "vada pav", "idli", "paratha", "kulfi", "hotel and restaurant",
            "momentale", "swiggy", "bundl", "zomato", "restaurant", "cafe", "bakery", "bakeries", "bekery", "bekeries", "bakry", "catering", "caterer", "confectionery", "dominos",
            "mcdonald", "kfc", "pizza", "eatfit", "brewpub", "dineout", "barbeque", "biryani", "subway", "starbucks",
            "dunkin", "burger king", "wendys", "haldiram", "wow momo", "faasos", "behrouz", "box8", "theobroma",
            "keventers", "baskin", "chaayos", "third wave", "blue tokai", "eatsure", "jubilant food", "pizza hut",
            "pizzahut", "devyani", "sapphire foods", "popeyes", "taco bell", "barista", "cafe coffee day", "ccd",
            "monginis", "bikaner", "saravana", "sagar ratna", "mainland china", "chinese wok", "chowman",
            "kwality wall", "mad over donuts", "jumboking", "papa john", "smokin joe", "oven story", "ovenstory",
            "punjab grill", "chai point", "nic ice cream", "mithai", "sweet shop", "sweets", "tea stall", "tea shop",
            "grab*", "grabfood",  "hungerbox", "hunger box", "kohinoor", "olive mithai"),
        "Groceries" to listOf("flower shop", "milk basked", "milk basket", "general store", "gen store", "vegitable", "vegitables", "vegetable", "veggies", "veggie", "food mart", "foodmart", "food bazaar", "grand bazar", "grand bazaar", "smart bazar", "smart bazaar",
            "flour mill","floor mill","flour","atta","chakki","rice mill","oil mill","masala","milk parlour", "milk parlor", "aavin", "amul parlour", "meat", "chicken", "mutton", "fish", "eggs", "egg", "fruit", "flowers", "flower", "daily mart", "super mart", "supermart", "mini mart", "bazaar", "bazar", "wholesale", "ration", "general stores", "provision store", "fresh mart", "big bazaar", "daily needs", "poultry",
            "nature's basket", "natures basket", "spencer", "star bazaar", "easyday", "ratnadeep",
            "vijetha", "licious", "freshtohome", "fresh to home", "milkmantra", "ninjacart", "nandini", "vishal mega",
            "fruits", "vegetables", "bakers basket", "blinkit", "zepto", "bigbasket", "big basket", "dmart", "d mart",
            "avenue supermart", "q mart", "qmart", "pure o natural", "bbnow", "first club", "instamart", "grofers",
            "supermarket", "kirana", "reliance fresh", "reliance smart", "avm", "more retail", "jiomart",
            "innovative retail", "country delight", "countrydelight", "milkbasket", "milk basket", "suprdaily",
            "supr daily", "bbdaily", "bb daily", "mother dairy", "amul", "heritage milk", "milk", "dairy"),
        "Fuel" to listOf("filling", "ram diesel", "diesel shop", "petrol and diesel", "petrol & diesel",
            "cng", "gas bank", "ev charging", "ev charge", "tata power ez", "ezcharge", "statiq", "ather grid", "service station", "petrol and diesel", "diesel", "essar",
            "bharat petroleum", "bharatpetroleum", "hindustan pet", "indianoil", "indian oil corp", "ioc", "hp pay", "hp petrol", "jio oil", "jiooil", "nayara energy", "reliance petro", "petrol pump", "petrol bunk", "filling station", "bpcl", "hpcl", "iocl", "hindustan petroleum", "nayara", "reliance bp", "jio-bp", "jio bp",
            "petrol", "fuel", "hpcl", "iocl", "bpcl", "indian oil", "shell", "fuel station"),
        "Transport" to listOf("royal enfield", "honda motor", "honda cars", "kia motor", "kia india", "hyundai motor", "maruti suzuki", "nexa", "arena", "tata motors", "mahindra", "toyota", "mg motor", "skoda", "volkswagen", "renault", "nissan", "bajaj auto", "tvs motor", "hero motocorp", "yamaha", "suzuki motor", "ather energy", "ola electric", "ktm", "jawa", "harley", "bmw", "mercedes", "motors", "motor", "showroom", "automobiles", "automotive",
            "taxi", "cabs", "rickshaw", "auto rickshaw", "autorickshaw", "auto", "bmrcl", "namma metro", "driver", "meru", "gozo", "zoomcar", "vogo", "indrive", "local train", "metro card",
            "ola cabs", "olacabs", "blu smart", "bluesmart", "yulu", "delhi metro", "dmrc", "hmrl",
            "tsrtc", "ksrtc", "msrtc", "apsrtc", "abhibus", "parking", "cab", "uber", "ola", "rapido", "metro", "bmtc",
            "redbus", "namma yatri", "toll", "toll plaza", "fastag", "fasttag", "netc"),
        "Shopping" to listOf("royal oak", "pictures", "framing", "photography", "photo studio", "photo frame", "cloth house", "cloth home", "cloth store", "clothing", "textiles", "cloth", "mobile shoppee", "shoppee", "mobile shop", "electronic mart", "electronic", "foxtale", "nyka", "minimalist", "minimilist",
            "stationary","stationaries","stationery", "books", "bookshop", "book shop", "gift", "gifts", "toys", "toy", "furniture", "mattress", "optical store", "mobile store", "accessories", "luggage", "handicraft", "handicrafts",
            "amazon", "flipkart", "myntra", "meesho", "ajio", "nykaa", "tatacliq", "croma",
            "reliance digital", "shoppers stop", "lifestyle", "decathlon", "ikea", "westside", "pantaloons", "firstcry",
            "pepperfry", "urban ladder", "lenskart", "titan", "tanishq", "caratlane", "bata", "vijay sales",
            "max fashion", "reliance trends", "trends", "oneplus", "one plus", "mi store", "samsung", "shopping mall",
            "yousta", "urbanofashion", "urbano fashion", "urbano", "zudio", "trendz", "centro",
            "van heusen", "vanheusen", "louis philippe", "allen solly", "peter england", "blackberrys", "raymond",
                "park avenue", "color plus", "colorplus",
            "jockey", "jack & jones", "jack and jones", "vero moda", "pepe jeans", "flying machine", "us polo", "uspa",
                "united colors of benetton", "benetton",
            "tommy hilfiger", "calvin klein", "h&m", "zara", "uniqlo", "forever 21", "fabindia", "fab india",
                "w for woman", "wforwoman", "biba", "aurelia",
            "global desi", "soch", "manyavar", "mohey", "levis", "levi's", "wrangler", "spykar", "mufti", "snitch",
                "bewakoof", "the souled store",
            "metro shoes", "mochi", "red tape", "redtape", "woodland", "skechers", "nike", "adidas", "puma", "reebok",
                "crocs", "hush puppies", "clarks", "sparx", "relaxo", "liberty shoes",
            "brand factory", "home centre", "home center", "nilkamal", "godrej interio", "hopscotch",
            "poorvika", "sangeetha mobiles", "bajaj electronics", "girias", "pai international", "lot mobiles",
            "kalyan jewellers", "malabar gold", "joyalukkas", "pc jeweller", "senco", "fastrack", "sephora",
                "tira beauty", "health & glow", "health and glow", "snapdeal", "reliance brands", "reliance retail",
                "shoppe", "shoe mart"),
        "Investment" to listOf("sip", "nps", "securities", "broking", "stock broker", "stockbroker", "stock broking", "gold bond", "mutual funds", "asset management", "ppf account",
            "icici prudential m", "bse limited", "nse clearing", "indian clearing", "zerodha",
            "groww", "upstox", "kuvera", "smallcase", "indmoney", "paytm money", "angel one", "angelone",
            "icici direct", "hdfc securities", "demat", "mutual fund", "elss", "tier1", "tier 1", "tier2", " ppf ",
            "nippon india", "sovereign gold"),
        "Personal care" to listOf("hair", "hair cut", "hair salon", "hairdresser", "tattoo", "nails", "nail art", "makeup", "make up", "massage", "wellness spa", "naturals",
            "salon", "spa ", "barber", "haircut", "parlour", "parlor", "jawed habib",
            "lakme salon", "looks salon", "enrich", "bodycraft", "naturals salon", "toni & guy", "toni and guy",
            "tony and guy", "tony n guy", "toniguy", "tony guy", "bblunt", "green trends"),
        "ATM / Cash" to listOf(" atm", "atm ", "cash wdl", "cash withdrawal", "cash withdrawn", "self wdl",
            "cash deposit", "cardless cash", "withdrawn at", "withdrawn from your", "aeps", "micro atm"),
        "Wallet" to emptyList(), // detected specially (wallet loads); listed so it can be filtered/picked
        "Bank charges" to emptyList(), // detected specially (fees and charges the bank or card takes); listed so it can be picked
        // People who work in your home: their own category, ahead of Home services so a cook or maid lands here.
        "Domestic help" to listOf("maid", "housemaid", "domestic help", "househelp", "house help", "nanny", "babysitter",
            "cook", "kaamwali", "kamwali", "ayah", "caretaker", "maid service", "domestic worker", "home nurse", "driver salary"),
        "Home services" to listOf("anacity", "anarock",
            "painter", "pest", "car wash", "carwash", "gas cylinder", "cylinder", "water can", "water cans", "ro service", "ro water", "cable tv", "cable operator", "cable", "cleaning", "housekeeping",
            "mygate", "101society", "apnacomplex", "nobrokerhood", "society maintenance", " chs",
            "apartmentadda", "apartment adda", "adda.io", "societyrun", "isocietymanager", "societykit", "society kit",
            "societynotebook", "society notebook", "paperbit", "societykingdom", "ggate", "housing society",
            "housing soc", "hsg soc", "chsl", "society maint", "owners association", "owners assn",
            "apartment association", "apartments association", "residents welfare", "residents association", "rwa",
            "pest control", "plumber", "electrician", "carpenter", "water purifier", "aquaguard", "kent ro",
            "ac service", "urban company", "urbancompany", "urbanclap", "urban clap", "housejoy", "nobroker",
            "handyman", "laundry", "dryclean", "dry clean", "uclean", "pressto", "washmart", "laundromat",
            ),
        "Entertainment" to listOf("kuku tv", "kuku fm", "kukufm", "kuku", "mxplayer", "atrangii", "atrangi network", "atrangii network",
            "gaming", "game zone", "gamezone", "games", "amusement", "water park", "waterpark", "theme park", "concert", "stadium", "night club", "nightclub", "bowling", "dream11", "mpl", "arcade", "escape room", "go karting", "imagica", "wonderla", "paytm insider",
            "disney", "apple music", "sonyliv", "aha video", "sun nxt", "sunnxt", "hoichoi",
            "insider.in", "steam", "playstation", "xbox", "nintendo", "epic games", "cinema", "theatre", "theater",
            "multiplex", "netflix", "spotify", "bookmyshow", "hotstar", "prime video", "jiocinema", "google play",
            "youtube", "sony liv", "zee5", "pvr", "inox", "cinepolis", "audible", "gaana", "wynk", "jiohotstar",
            "hungama", "eros now", "voot", "mx player", "alt balaji", "altbalaji"),
        "Subscriptions" to listOf("apple", "claude", "newspaper", "google workspace", "gsuite", "g suite", "copilot", "cloudflare", "hostinger", "bluehost", "hosting", "mailchimp", "substack", "patreon", "tradingview", "duolingo", "evernote", "1password", "lastpass", "nordvpn", "expressvpn", "proton",
            "apple.com/bill", "apple services", "apple media", "google cloud", "amazon web",
            "aws", "digitalocean", "namecheap", "godaddy", "zoho", "slack", "zoom", "midjourney", "perplexity",
            "cursor", "jetbrains", "replit", "vercel", "heroku", "adobe", "microsoft", "office 365", "microsoft 365",
            "google one", "icloud", "dropbox", "canva", "openai", "chatgpt", "anthropic", "github", "notion", "figma",
            "grammarly", "linkedin"),
        "Bills" to listOf("tgspdcl", "tgnpdcl", "tsnpdcl", "telangana electricity", "apepdcl", "apcpdcl", "mescom", "hescom", "gescom", "pgvcl", "ugvcl", "mgvcl", "dgvcl", "dhbvn", "uhbvn", "jvvnl", "avvnl", "jdvvnl", "mpmkvvcl", "pvvnl", "dvvnl", "mvvnl", "puvvnl", "kesco", "wbsedcl", "cesu", "tpcodl", "tpwodl", "tpnodl", "tpsodl", "apdcl", "jbvnl", "bsphcl", "nbpdcl", "sbpdcl", "cspdcl", "tangedco", "tnpdcl", "hpseb", "bhagya nagar gas", "bhagyanagar gas", "adani gas", "fiber", "fibre", "airtel fiber", "act fiber", "gtpl", "siti networks", "den networks", "you broadband", "alliance broadband", "spectranet", "nextra", "connect broadband", "railwire", "netplus", "one broadband", "discom",
            "water board", "jal board", "jal nigam", "kseb", "pspcl", "uppcl", "torrent power", "adani electricity", "cesc", "bses", "tpddl", "bijli", "videocon d2h", "water tax", "sewage", "landline", "mtnl",
            "billdesk", "infobil", "bharat bill", "bbps", "tatapower", "starhealth", "insurance",
            "sms alert", "alert chg", "annual fee", "membership fee", "annual maint", "maint chgs", "finance charge",
            "interest charge", "interest on cash", "late payment", "cross currency markup", "cheque return",
            "non-maintenance", "electricity board", "msedcl", "tneb", "tsspdcl", "apspdcl", "bwssb", "hmwssb", "indane",
            "hp gas", "bharatgas", "bharat gas", "mahanagar gas", "adani gas", "piped gas", "tata play", "tataplay",
            "dish tv", "sun direct", "d2h", "municipal", "property tax", "lic of india", "insurance premium",
            "life insurance", "hdfc life", "sbi life", "max life", "star health", "care health", "acko",
            "digit insurance", "policybazaar", "electricity", "gas agency", "dth", "tata power", "bescom", "mseb",
            "bharat connect", "water bill", "gas bill", "broadband", "fibernet", "jiofiber", "jio fiber",
            "airtel xstream", "xstream", "act fibernet", "actcorp", "hathway", "excitel", "spectra", "tikona", "wifi",
            "mobile recharge", "recharge", "prepaid", "postpaid", "airtel", "jio", "vodafone", " vi ", "bsnl", "idea"),
        "Travel" to listOf("taj hotels","itc hotels","hotels.com","hotel booking","hotel stay","hotel rooms","gmr airport", "airport", "gmr hyderabad", "gmr delhi", "delhi airport", "bial", "mumbai airport",
            "hostel", "homestay", "home stay", "visa", "forex", "passport", "vfs", "cox & kings", "cox and kings", "sotc", "kesari", "veena world", "club mahindra", "sterling holidays", "trivago", "expedia", "hotels.com", "marriott", "hyatt", "hilton", "radisson", "lemon tree", "ihg", "oberoi",
            "easemytrip", "yatra", "agoda", "booking.com", "spicejet", "akasa", "air asia", "airasia",
            "treebo", "fabhotels", "zostel", "thomas cook", "lodge", "guest house", "irctc", "makemytrip", "goibibo",
            "cleartrip", "indigo", "vistara", "air india", "oyo", "airbnb", "ixigo"),
        "Donation" to listOf("charity", "charitable trust", "charitable", "ngo", "orphanage", "orphan", "anathalaya", "anathashram", "old age home", "vridhashram",
            "mandir", "church", "mosque", "masjid", "dargah", "gurudwara", "gurdwara", "hundi", "ashram", "charitable",
            "donation", "donate", "charity", "giveindia", "give india", "ketto", "milaap", "iskcon",
            "tirumala", "ttd", "relief fund", "seva", "temple", "ngo"),
        "EMI" to listOf("loan emi", "home loan", "car loan", "personal loan", "vehicle loan", "two wheeler loan", "finance", "navi finserv", "fibe", "stashfin", "zest money", "zestmoney", "lazypay", "simpl", "paysense", "cashe", "fatakpay", "moneytap", "protium", "idfc first loan",
            "emi", "loan", "loans", "installment", "instalment", "bajaj finance", "finserv", "nbfc",
            "ach d", "ach/", "ach-", "nach", "e-mandate", "emandate", "ecs",
            // Lenders and loan apps whose EMI debits name only the company.
            "kisetsu", "credit saison", "home credit", "tata capital", "hdb financial", "fullerton", "kreditbee",
            "moneyview", "money view", "earlysalary", "early salary", "dmi finance", "muthoot", "manappuram",
            "aditya birla finance", "l&t finance", "piramal finance", "poonawalla", "kissht", "mpokket", "lendingkart",
            "shriram finance", "iifl finance", "cholamandalam", "mahindra finance", "sundaram finance", "hero fincorp",
            "tvs credit", "axis finance", "home finance", "housing finance", "lic housing", "avanse", "incred"),
        "Fitness" to listOf("cult.fit", "cultfit", "cure.fit", "curefit", "gold's gym", "golds gym", "anytime fitness", "fitness first", "healthifyme", "gym", "gyms", "fitso", "fitness", "crossfit", "cross fit", "talwalkar", "powerworld", "multifit", "fitpass", "f45", "playo", "hudle", "zumba", "pilates", "yoga studio", "yoga centre", "yoga center", "yoga classes", "martial arts", "karate", "taekwondo", "kickboxing", "badminton", "swimming", "sports academy", "sports club", "yoga"),
        "Health" to listOf("health solutions", "child health", "child health centre", "child health center", "neonatology", "neonatolog", "pediatrics", "paediatrics", "pediatrician", "children hospital", "childrens hospital",
            "maternity","fertility","ivf","gynec","gynaec","gynae","obstetric","pediatric","paediatric","neonatal","diagnostics","diagnostic","lab","labs",
            "laboratory", "laboratories", "labs", "lab", "nursing home", "xray", "x-ray", "scan centre", "scan center", "scan", "veterinary", "vet", "pet clinic", "pet care", "optician", "polyclinic", "homeo", "homoeo", "doctor", "eye hospital", "eye clinic", "skin clinic", "hearing",
            "medico", "drug", "drugs", "druggist", "druggists", "drugist", "drugists", "chemist", "chemists", "drug store", "drug house", "medicos", "dental", "dentist", "eye care", "optical", "ortho", "physio",
            "pathlab", "path lab", "thyrocare", "lal path", "metropolis", "redcliffe", "orange health", "tata1mg",
            "truemeds", "pharmacies", "ayush", "ayurved", "pharmeasy", "1mg", "apollo", "netmeds", "pharmacy", "clinic",
            "diagnostic", "medplus", "practo", "tata 1mg", "wellness forever", "wellness", "medical store", "dawakhana"),
        "Education" to listOf("educational society", "education society", "educational trust", "education trust",
            "nptel", "hostel fee", "school fees", "tuition fee", "exam fees", "college fees", "khan academy", "skillshare", "linkedin learning", "coaching centre", "coaching center", "kindergarten", "playschool", "play school", "preschool", "creche", "montessori", "tutorial", "tutorials",
            "uforward", "takeuforward", "scaler", "physics wallah", "physicswallah", "simplilearn",
            "great learning", "greatlearning", "leetcode", "masai", "coding ninjas", "codingninjas", "interviewbit",
            "geeksforgeeks", "toppr", "testbook", "adda247", "allen career", "aakash", "fiitjee", "resonance",
            "stationery", "book store", "bookstore", "library", "school fee", "tuition", "college fee", "university",
            "byju", "unacademy", "udemy", "coursera", "vedantu", "upgrad", "exam fee", "coaching"),
        "Rent" to listOf("pg", "paying guest", "landlord", "lease", "house rent", "room rent", "flat rent", "cred rent", "nobroker rent",
            "rent"),
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
        """\b(paytm wallet|amazon ?pay|amazonpay|phonepe wallet|phonepe balance|mobikwik|freecharge|jio ?money|jio ?wallet|g ?pay balance|google pay balance|pluxee|sodexo|zeta wallet|jupiter money|fi money|airtel ?money|payzapp|ola money|wallet balance|e-?wallet|\bwallet\b)\b""",
        RegexOption.IGNORE_CASE,
    )
    // Money going OUT of the wallet (a spend from balance) or cashback landing in it —
    // not counted, to avoid double-counting after the bank load was already booked.
    private val AMAZON_PAY = Regex("""\bamazon\s?pay\b""", RegexOption.IGNORE_CASE)
    private val WALLET_SPEND = Regex(
        """\b(from|via|using|thru|through)\b[a-z' ]{0,14}wallet\b|wallet[a-z' ]{0,14}\b(spent|paid|debited|purchase)|cashback""",
        RegexOption.IGNORE_CASE,
    )

    // Paying a BNPL / postpaid bill settles a balance — a transfer, not a fresh spend.
    private val BNPL = Regex("""\b(olamoney|ola money|lazypay|simpl|paylater|pay later|postpaid account)\b""", RegexOption.IGNORE_CASE)

    // A plain multi-word personal name (no business word) — a person, not a merchant.
    private val PERSON_NAME = Regex("""^[\p{L}\p{M}]{2,}\.?(?: [\p{L}\p{M}]+\.?){1,3}$""")
    // Money that was earned, not sent by a friend: salary, pension, interest...
    private val EARNED = Regex("""\b(?:salary|sal|freelanc\w*|pension|interest|dividend|stipend|bonus|incentive|commission|payroll|reimburse\w*|wages?|honorarium|govt|government)\b""", RegexOption.IGNORE_CASE)
    private val BUSINESS_WORD = Regex(
        """\b(ltd|limited|pvt|private|llp|inc|corp|corporation|company|systems?|software|technolog\w*|""" +
            """industries|enterprises?|retail|services?|solutions?|stores?|traders?|agenc\w*|mart|""" +
            """supermarket|foods?|electricals?|electronics|hardware|textiles?|medicals?|motors?|""" +
            """infotech|labs?|hospital|clinic|pharma|bank|finance|insurance|capital|ventures?)\b""",
        RegexOption.IGNORE_CASE,
    )

    // Money landing ON a credit card = a bill settlement, not income.
    private val CARD_RECV = Regex(
        """card ending \d|payment[^.]{0,25}\bcard\b|towards[^.]{0,20}card|to your[^.]{0,15}card|credit card (bill|payment)|received on your[^.]{0,30}credit card|credit card (xx|\*+)?\d""",
        RegexOption.IGNORE_CASE,
    )

    // Contributions to retirement / market investments: tracked separately, so they count
    // as neither spending nor income (PF is deducted pre-salary; NPS you transfer in).
    private val INVEST = Regex(
        """\b(epf|epfo|provident fund|passbook|uan|pf contribution|nps|tier\s?1|tier\s?2|pran|""" +
            """mutual fund|zerodha|groww|upstox|kuvera|smallcase|indmoney|demat|elss|folio|sip debit|""" +
            """icici direct|hdfc securities|paytm money|angel ?one|ppf|nippon india|sovereign gold|""" +
            """etmoney|5paisa|motilal|fixed deposit|recurring deposit|term deposit|tdr|stdr|bse limited|nse clearing|""" +
            // Banks write FDs and RDs short: "AMT TRF TO FD A.C NO20044...", "FD No XX4321", "RD A/c".
            """(?:to|into)\s+(?:an?\s+|the\s+|your\s+)?fd|fd\s*(?:a/?c|acct|account|no\.?|number|booking)|fd\s*(?:no\.?\s*)?(?:x+|\*+)?\d{3,}|rd\s*(?:a/?c|acct|account)|""" +
            """indian clearing corp|icici prudential m(?:f|utual)?|prudential mutual|mf utilities)\b""",
        RegexOption.IGNORE_CASE,
    )

    // Name-pattern heuristics: generalize to merchants we'll never have in any list.
    // "Sri Balaji Medicals", "Annapurna Supermarket", "Balaji Travels" all classify from
    // a generic word in the name. Checked after the exact dictionary, before giving up.
    private val NAME_HINTS: List<Pair<Regex, String>> = listOf(
        Regex("""\b(hotels|grand\s+hotel|boutique\s+hotel|luxury\s+hotel|residency|regency|plaza|inns?|lodging|lodges?|hostels?|homestays?|visa|forex|travels?|tours|holidays|airlines|airways|aviation|railways?|irctc|bus\s*stand|roadways|rtc|resorts?|retreats?|suites?|rest\s*house|circuit\s*house|dharamshala|sarai|sarovar|getaways?|hideaways?|tourism|innkeeping|guest\s*services|travel\s*&\s*stay|stays?|escapes?)\b""", RegexOption.IGNORE_CASE) to "Travel",
        Regex("""\b(gyms?|fitness|yoga|crossfit|zumba|pilates|karate|taekwondo|badminton|swimming|sports\s+(?:academy|club))\b""", RegexOption.IGNORE_CASE) to "Fitness",
        Regex("""\b(neonatolog\w*|child\s+health|health\s+solutions|maternity|fertility|ivf|gyn(?:ae?|e)colog\w*|obstetric\w*|pa?ediatric\w*|neonatal|doctors?|laborator(?:y|ies)|labs?|scans?|x-?ray|mri|vet|veterinary|nursing\s+home|optician|polyclinic|physiotherapy|homeo(?:pathy)?|medical|medicals|pharmacy|pharma|chemists?|drugs?|druggists?|drugists?|hospital|hospitals|clinic|clinics|diagnostic|diagnostics|pathology|healthcare|dental|dentist)\b""", RegexOption.IGNORE_CASE) to "Health",
        Regex("""\b(meat|chicken|mutton|fish|eggs|poultry|fruits?|flowers?|mart|bazaar|bazar|wholesale|ration|milk|supermarket|super\s+market|kirana|groceries|grocery|provision|provisions|general\s+store|vegetable|vegetables|dairy\s+farm)\b""", RegexOption.IGNORE_CASE) to "Groceries",
        Regex("""\b(mess|canteen|bhojanalay|thali|lassi|bhel|fast\s?food|food\s+court|pub|brewery|bar\s+(?:&|and)\s+restaurant|tea\s+(?:stall|house|shop|centre|center)|chai\s?wal(?:a|e)|restaurants?|restuarants?|cafe|hotel|foods?|kitchen|dhaba|sweets?|bakery|bekery|bekeries|bakeries|bakers?|bakes|bake ?house|catering|confectionery|tiffin|biryani|darshini|caterers?|juice|snacks|burgers?|pizza|momos?|coffee|chai|ice ?cream|donuts?|doughnuts?|grill|brasserie|bistro|eatery|diner|dosa|chaat|patisserie|confection|sweet shop|mithai|restro|family restaurant)\b""", RegexOption.IGNORE_CASE) to "Food",
        Regex("""\b(filling|diesel|petrol|petroleum|jio ?oil|nayara|hpcl|bpcl|iocl|indian ?oil|fuels?|filling\s+station|petro|gas\s+station)\b""", RegexOption.IGNORE_CASE) to "Fuel",
        Regex("""\b(hair|hairdresser|tattoo|nails?|makeup|massage|salon|spa|beauty|parlour|parlor|barber|unisex|grooming)\b""", RegexOption.IGNORE_CASE) to "Personal care",
        Regex("""\b(kindergarten|playschool|preschool|montessori|tutorials?|school|college|university|academy|institute|tuition|coaching|vidyalaya|education|classes)\b""", RegexOption.IGNORE_CASE) to "Education",
        Regex("""\b(cloth\s+(?:house|home|store)|clothing|textiles?|photography|framing|pictures|photo\s+studio|showrooms?|electronic|stationary|stationery|bookshop|books|gifts?|toys?|furniture|mattress|handicrafts?|luggage|accessories|electronics|electricals?|hardware|mobiles|shopping mall|mall|departmental store|garments|footwear|apparel|fashion|boutique|retail|retailer|readymade|collections?|wardrobe|shoes?|sarees?|silks?|kanchi|kanjeevaram|jewell?ers?|jewell?ery)\b""", RegexOption.IGNORE_CASE) to "Shopping",
        Regex("""\b(vegitables?|gen(?:eral)?\s+stores?|food\s+mart|foodmart|flower\s+shop|pan\s+shop|paan\s+shop|flour\s+mills?|floor\s+mills?)\b""", RegexOption.IGNORE_CASE) to "Groceries",
        Regex("""\b(namkeen|creamery|cream\s?-?stone|pure\s+veg(?:etarian)?|veg\s+restaurant)\b""", RegexOption.IGNORE_CASE) to "Food",
        Regex("""\b(airports?)\b""", RegexOption.IGNORE_CASE) to "Travel",
        Regex("""\b(educational\s+society|education\s+society)\b""", RegexOption.IGNORE_CASE) to "Education",
        Regex("""\b(motors?|automobiles?|automotive|showrooms?|taxi|cabs?|rickshaw|autorickshaw|parking|toll)\b""", RegexOption.IGNORE_CASE) to "Transport",
        Regex("""\b(cinemas?|theatres?|theaters?|multiplex|amusement|water\s?park|gaming|game\s?zone|bowling|stadium|night\s?club|entertainment)\b""", RegexOption.IGNORE_CASE) to "Entertainment",
        Regex("""\b(maid|house\s?maid|cook|nanny|babysitter|ayah|kaamwali|kamwali)\b""", RegexOption.IGNORE_CASE) to "Domestic help",
        Regex("""\b(plumber|electrician|carpenter|painter|pest\s+control|laundry|dry\s?clean|car\s?wash|housekeeping|cleaning)\b""", RegexOption.IGNORE_CASE) to "Home services",
        Regex("""\b(securities|broking|stock\s?broker|mutual\s+funds?|asset\s+management)\b""", RegexOption.IGNORE_CASE) to "Investment",
        Regex("""\b(charity|ngo|orphanage|orphan|old\s+age\s+home|mandir|temple|church|mosque|masjid|gurudwara|gurdwara|trust|foundation|charitable|ashram|mutt)\b""", RegexOption.IGNORE_CASE) to "Donation",
    )

    // Card alerts end with an offer ("To convert this txn to EMI give a missed call... Know more about
    // EMI conversion at..."). That says nothing about what was bought, so it is cut before matching.
    private val EMI_OFFER = Regex(
        """[^.]*\b(?:convert\w*|conversion)\b[^.]*\bemi\b[^.]*|[^.]*\bemi\b[^.]*\b(?:convert\w*|conversion)\b[^.]*""",
        RegexOption.IGNORE_CASE,
    )
    // "Not you? Call toll free 1800...": the bank's helpline, not a toll paid.
    private val HELPLINE = Regex("""\btoll[\s-]*free\b""", RegexOption.IGNORE_CASE)

    // Short keywords ("emi", "ola", "ccd", "vi") must stand alone as words, or "chemist" becomes EMI and
    // "Mohola" becomes Ola. Longer ones may sit inside a word ("kimshospital" still finds "hospital").
    private fun has(hay: String, kw: String): Boolean {
        if (kw.length > 5 || !kw.first().isLetterOrDigit() || !kw.last().isLetterOrDigit()) return hay.contains(kw)
        var i = hay.indexOf(kw)
        while (i >= 0) {
            val before = if (i == 0) ' ' else hay[i - 1]
            val after = if (i + kw.length >= hay.length) ' ' else hay[i + kw.length]
            if (!before.isLetterOrDigit() && !after.isLetterOrDigit()) return true
            i = hay.indexOf(kw, i + 1)
        }
        return false
    }

    private fun onCard(account: String) = account.contains(" CC ") || account.startsWith("CC ") || account.endsWith(" CC")

    data class Result(val kind: Kind, val category: String)

    // A plain-language reason for why a transaction got its label — shown in the detail dialog.
    fun explain(counterparty: String?, raw: String, kind: Kind, category: String): String {
        val hay = ("${counterparty ?: ""} ${EMI_OFFER.replace(Parser.withoutPaymentsBank(raw), " ")}".replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")).lowercase()
        return when (kind) {
            Kind.INCOME -> "Counted as money in — a credit to your account."
            Kind.REFUND -> "A refund credited back to you."
            Kind.INVEST -> "Tracked as an investment (PF, NPS, funds, fixed deposits) — neither spending nor income."
            Kind.TRANSFER -> when (category) {
                "Card bill" -> "A credit-card bill payment — it settles spends already counted, so it isn't counted again."
                "Wallet" -> "Money moved to/from a wallet — counted once when it leaves your bank."
                else -> "A transfer — between your own accounts, or money a person sent you."
            }
            Kind.LENT -> "You tagged this as money lent out."
            Kind.BORROWED -> "You tagged this as money you borrowed."
            Kind.IGNORE -> "You've marked this to be ignored, so it's left out of totals."
            else -> {
                val kw = CATEGORIES[category]?.firstOrNull { has(hay, it) }
                when {
                    kw != null -> "Categorised as $category because \"$kw\" appeared in the message."
                    category == "Unknown" -> "No known merchant matched — tag it and Zelu will remember this payee."
                    else -> "Categorised as $category from the payee name."
                }
            }
        }
    }

    private val BANK_CHARGE = Regex(
        """\b(?:annual|joining|renewal|membership|late(?: payment)?|sms(?: alert)?|alert|atm(?: transaction)?|""" +
            """maintenance|non[- ]maintenance|minimum(?: average)? balance|min(?:imum)? bal|amb|""" +
            """mab|overlimit|over[- ]limit|cash advance|penal|penalty|cheque return|chq return|debit card|credit card|cheque book|chequebook|processing|locker|demand draft|stop payment|duplicate statement|""" +
            """ecs return|nach return|card replacement|service)\b[a-z ]{0,25}?\b(?:fees?|charges?)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val CHARGE_MAYBE = Regex(
        """\b(?:fees?|charges?)\s+(?:may|will|shall|would)\b|\b(?:fees?|charges?)\s+(?:if\s+)?applicable|""" +
            """\bno\s+(?:extra\s+|additional\s+)?(?:fees?|charges?)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val BANK_FEE = Regex("""\btowards\s+[^.]{0,40}\b(?:charges?|chgs|fees?|[ics]?gst)\b|^[^.]{0,30}\b(?:charges?|fees?|interest|markup)\b[^.]{0,60}\b(?:levied|charged|posted)\s+(?:on|to)\b""", RegexOption.IGNORE_CASE)
    private val EMBEDDED = listOf("school" to "Education", "college" to "Education", "academy" to "Education", "hospital" to "Health",
        "pharmacy" to "Health", "medical" to "Health", "clinic" to "Health", "restaurant" to "Food", "supermarket" to "Groceries")
    // Merchants whose money coming back to you is a refund, not income.
    private val REFUND_CATEGORIES = setOf("Food", "Groceries", "Shopping", "Travel", "Transport", "Entertainment")
    private val SELF_TRANSFER = Regex("""\bto (?:your )?own (?:a/?c|acct|account)\b|\((?:self)\)|\bself (?:transfer|a/?c)\b""", RegexOption.IGNORE_CASE)
    private val ACCOUNT_CHECK = Regex("""\b(?:a/?c|account) verification\b|\bpenny drop\b|\bverify your (?:bank )?(?:a/?c|account)\b""", RegexOption.IGNORE_CASE)
    // An income tax refund ("NEFT Cr-SBIN0000TBU-ITDTAX REFUND", SBI's "An IT Refund amount of Rs 20990"): money in,
    // not a purchase coming back.
    private val TAX_REFUND = Regex("""\bitdtax\b|\bit refund\b|\bincome[- ]tax refund\b|\btax refund\b""", RegexOption.IGNORE_CASE)
    private val REFUND_WORD = Regex("""\brefund(?:ed)?\b""", RegexOption.IGNORE_CASE)
    private val WALLET_LOAD = Regex("""\badd(?:ed)? money\b|\bwallet (?:top-?up|load|recharge)\b|\b(?:top-?up|load(?:ed)?) (?:your |to )?wallet\b""", RegexOption.IGNORE_CASE)
    // "chargeback credit", "UPI/REV/": money given back for a disputed or reversed payment.
    private val CARD_REFUND = Regex("""\bchargeback\b|\bupi/rev/|\bdispute (?:ref|resolved|credit)\b""", RegexOption.IGNORE_CASE)
    private val CASHBACK = Regex("""\bcash ?back\b|\breward (?:points? )?(?:credit|redemption)\b""", RegexOption.IGNORE_CASE)
    // "BBPS Bill Payment - SBI Card", "CC BILL PAY/HDFC BANK CREDIT CARD": a card bill paid through Bharat BillPay.
    private val CARD_BILL_BBPS = Regex("""\b(?:bbps|bill ?pay(?:ment)?)\b[^.]{0,40}\b(?:card|cc)\b|\bcc bill pay\b""", RegexOption.IGNORE_CASE)

    private val BILL_PAY = Regex("""\bbill ?pay\b""")

    fun classify(t: Txn, ownAccounts: List<String>): Result {
        val cp = (t.counterparty ?: "").lowercase()

        // Paying a credit-card bill (directly or via CRED) is a transfer, not a new spend.
        if (t.direction == Dir.DEBIT && (CARD_BILL.containsMatchIn(t.raw) || CARD_BILL_BBPS.containsMatchIn(t.raw) || CRED.containsMatchIn(t.raw) || BNPL.containsMatchIn(t.raw))) {
            return Result(Kind.TRANSFER, "Card bill")
        }

        // A fee the bank or card took (annual fee, late payment, minimum balance, SMS, ATM charges) is its own spend;
        // "charges may apply" on an ordinary payment is not.
        if (t.direction == Dir.DEBIT && BANK_CHARGE.containsMatchIn(t.raw) && !CHARGE_MAYBE.containsMatchIn(t.raw))
            return Result(Kind.EXPENSE, "Bank charges")

        // Money moving between your own accounts is not income or spending.
        if (cp.isNotEmpty() && ownAccounts.any { it.isNotEmpty() && cp.contains(it.lowercase()) }) {
            return Result(Kind.TRANSFER, "Transfer")
        }

        // Sent to your own account ("to your own a/c X8890 (SELF)") is a transfer between your accounts.
        if (SELF_TRANSFER.containsMatchIn(t.raw) || cp == "self") return Result(Kind.TRANSFER, "Transfer")
        if (t.direction == Dir.CREDIT && TAX_REFUND.containsMatchIn(t.raw)) return Result(Kind.INCOME, "Tax refund")
        // A tiny broker or bank deposit to check your account is not an investment.
        val verify = t.direction == Dir.CREDIT && ACCOUNT_CHECK.containsMatchIn(t.raw)
        // Retirement / market contributions are tracked on their own, not spend or income.
        if (!verify && (INVEST.containsMatchIn(t.raw) || INVEST.containsMatchIn(cp))) {
            return Result(Kind.INVEST, "Investment")
        }

        // Wallets: loading a wallet from your bank IS the spend (the money has left your
        // account), so a wallet load counts as an expense. Spending FROM the wallet, or
        // cashback landing in it, is NOT counted again — that would double-count.
        // A refund from a wallet company ("refund from AMAZON PAY INDIA") is a refund to you, not a wallet movement.
        if (t.direction == Dir.CREDIT && REFUND_WORD.containsMatchIn(t.raw)) return Result(Kind.REFUND, "Refund")
        // Loading a wallet ("PAYTM ADD MONEY", "Wallet top-up") is money moved to the wallet.
        if (t.direction == Dir.DEBIT && WALLET_LOAD.containsMatchIn(t.raw)) return Result(Kind.EXPENSE, "Wallet")
        // A card swipe at Amazon Pay is shopping on Amazon, not money loaded into a wallet.
        if (t.direction == Dir.DEBIT && onCard(t.account) && AMAZON_PAY.containsMatchIn(t.raw)) return Result(Kind.EXPENSE, "Shopping")
        if (WALLET.containsMatchIn(t.raw)) {
            if (t.direction == Dir.CREDIT || WALLET_SPEND.containsMatchIn(t.raw))
                return Result(Kind.IGNORE, "Wallet")
            return Result(Kind.EXPENSE, "Wallet")
        }

        if (t.direction == Dir.CREDIT) {
            if (Parser.REFUND.containsMatchIn(t.raw) || CARD_REFUND.containsMatchIn(t.raw)) return Result(Kind.REFUND, "Refund")
            // Cashback and reward credits are money in, even when they land on a card.
            if (CASHBACK.containsMatchIn(t.raw)) return Result(Kind.INCOME, "Income")
            // A loan paid out to you is borrowed money, not income.
            if (Parser.LOAN_DISBURSAL.containsMatchIn(t.raw)) return Result(Kind.TRANSFER, "Loan")
            // A payment landing on your credit card is settling a bill, not income.
            // Anything landing on an account we know is a credit card (e.g. "Payment of Rs X has
            // been received on your ... Credit Card XX8003 through BBPS") is the same thing.
            if (CARD_RECV.containsMatchIn(t.raw) || onCard(t.account))
                return Result(Kind.TRANSFER, "Card bill")
            // A shop or app sending money back ("UPI/CR/.../SWIGGY") is a refund.
            if (cp.isNotEmpty() && CATEGORIES.any { (cat, words) -> cat in REFUND_CATEGORIES && words.any { has(cp, it) } })
                return Result(Kind.REFUND, "Refund")
            // Money from a person ("from RAMESH IYER via IMPS") is a transfer between people, not income.
            val from = t.counterparty?.trim()
            if (from != null && PERSON_NAME.matches(from) && !BUSINESS_WORD.containsMatchIn(from) && !EARNED.containsMatchIn(t.raw) && !EARNED.containsMatchIn(from))
                return Result(Kind.TRANSFER, "Transfer")
            return Result(Kind.INCOME, "Income")
        }

        // Names often arrive run together ("KimsHospital", "SriLakshmiMedicals"): also match on the
        // words split at each lower->UPPER change, so "hospital", "medical"... are found.
        // The bank the money left from ("Airtel Payments Bank") says nothing about what it paid for.
        val raw = HELPLINE.replace(EMI_OFFER.replace(Parser.withoutPaymentsBank(t.raw), " "), " ")
        val spaced = ((t.counterparty ?: "") + " " + raw).replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
        val hay = ("$cp $raw $spaced").lowercase()
        val joined = hay.replace("*", " ")
        // A bank fee or its tax ("towards Debit Card Annual Maint Chgs", "towards IGST on DC Annual Fee") is a bill.
        if (BANK_FEE.containsMatchIn(raw)) return Result(Kind.EXPENSE, "Bank charges")
        // "Amazon Bill Pay", "BillPay"... is a bill payment through the platform, whatever the platform sells.
        if (BILL_PAY.containsMatchIn(cp)) return Result(Kind.EXPENSE, "Bills")
        // 1) Exact brand dictionary (most specific). Swiggy Instamart is groceries, though "swiggy" alone is food.
        if (has(hay, "instamart")) return Result(Kind.EXPENSE, "Groceries")
        for ((category, words) in CATEGORIES) {
            if (words.any { has(hay, it) }) return Result(Kind.EXPENSE, category)
        }
        // Shops and apps listed by the group that owns them.
        for ((category, words) in Brands.EXTRA.entries + Brands.LEGAL.entries) {
            if (words.any { has(hay, it) }) return Result(Kind.EXPENSE, category)
        }
        if (Brands.isBareMaxShop(t.counterparty ?: "", t.raw)) return Result(Kind.EXPENSE, "Shopping")
        // A payee written with a space or dash ("MC DONALDS", "Red Bus") still finds the word list's "mcdonald", "redbus".
        val payeeRun = cp.filter { it.isLetterOrDigit() }
        if (payeeRun.length >= 6) {
            val all = CATEGORIES.entries.asSequence() + Brands.EXTRA.entries.asSequence() + Brands.LEGAL.entries.asSequence()
            for (e in all) for (w in e.value) {
                val run = w.filter { it.isLetterOrDigit() }
                if (run.length >= 6 && payeeRun.contains(run)) return Result(Kind.EXPENSE, e.key)
            }
        }
        // 2) Name-pattern heuristics (generalize to unknown local merchants).
        for ((re, category) in NAME_HINTS) {
            if (re.containsMatchIn(hay)) return Result(Kind.EXPENSE, category)
        }
        // "RAZ*DELHIPUBLICSCHOOL": a telling word inside one long run-together name.
        for ((word, category) in EMBEDDED) {
            if (joined.split(' ').any { it.length > word.length + 3 && it.contains(word) }) return Result(Kind.EXPENSE, category)
        }
        // 3) Unguessable, including money sent to a person (rent, a maid, a shop's owner) -> counted as spend and asked
        //    once (then remembered per payee). Only your own accounts and card bills are transfers.
        return Result(Kind.EXPENSE, "Unknown")
    }
}
