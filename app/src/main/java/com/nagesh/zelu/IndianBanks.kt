package com.nagesh.zelu

/**
 * Banks beyond the big ones Parser already knows: other public-sector, private and small-finance banks, the rural
 * (Gramin) banks and the larger co-operative banks. One line per bank, so adding one later is a single entry.
 *
 * label   - what Zelu calls the account ("MGB 883076")
 * names   - how the bank writes its own name in an SMS (matched as whole words, capitals ignored)
 * senders - pieces of the sender id (best guesses; a bank still works from its name in the text if one is wrong)
 * color   - badge colour while Zelu has no logo for the bank
 */
object IndianBanks {
    class Bank(val label: String, val color: Long, val names: List<String>, val senders: List<String>, val mark: String = label.take(4))

    private fun b(label: String, color: Long, names: List<String>, senders: List<String> = emptyList(), mark: String = label.take(4)) =
        Bank(label, color, names, senders, mark)

    val all: List<Bank> = listOf(
        // Public-sector and private banks not in Parser's main list
        b("BOM", 0xFFD32F2F, listOf("BANK OF MAHARASHTRA", "MAHABANK"), listOf("MAHABK", "BOMBNK")),
        b("INDIAN", 0xFF1565C0, listOf("INDIAN BANK")),
        b("CENTRAL", 0xFFC62828, listOf("CENTRAL BANK OF INDIA", "CENTRAL BANK")),
        b("PSB", 0xFF1B5E20, listOf("PUNJAB & SIND BANK", "PUNJAB AND SIND BANK"), listOf("PSIBNK", "PSBANK")),
        b("JKB", 0xFF0D47A1, listOf("J&K BANK", "JAMMU & KASHMIR BANK", "JAMMU AND KASHMIR BANK"), listOf("JKBANK", "JKBNK")),
        b("CSB", 0xFF283593, listOf("CSB BANK", "CATHOLIC SYRIAN BANK"), listOf("CSBBNK", "CSBANK")),
        b("DHANLAXMI", 0xFF2E7D32, listOf("DHANLAXMI BANK", "DHANLAXMI"), listOf("DLXBNK", "DHANBK"), "DLB"),
        b("CUB", 0xFF6A1B9A, listOf("CITY UNION BANK"), listOf("CUBANK", "CITYUB")),
        b("TMB", 0xFF00695C, listOf("TAMILNAD MERCANTILE BANK", "TMB BANK"), listOf("TMBANK", "TMBBNK")),
        b("NAINITAL", 0xFF4527A0, listOf("NAINITAL BANK"), listOf("NTBANK", "NAINIB"), "NTB"),
        b("DCB", 0xFF00838F, listOf(), listOf("DCBBNK", "DCBANK")),
        // Small-finance and payments banks
        b("JANA", 0xFF2E7D32, listOf("JANA SMALL FINANCE BANK", "JANA BANK"), listOf("JANASF", "JANABK")),
        b("SURYODAY", 0xFFEF6C00, listOf("SURYODAY SMALL FINANCE BANK", "SURYODAY"), listOf("SURYOD"), "SSFB"),
        b("ESAF", 0xFF2E7D32, listOf("ESAF SMALL FINANCE BANK", "ESAF BANK"), listOf("ESAFBK", "ESAFSF")),
        b("UTKARSH", 0xFF6A1B9A, listOf("UTKARSH SMALL FINANCE BANK"), listOf("UTKARS"), "USFB"),
        b("CAPITAL", 0xFF1565C0, listOf("CAPITAL SMALL FINANCE BANK"), listOf("CAPSFB"), "CSFB"),
        b("FINCARE", 0xFFAD1457, listOf("FINCARE SMALL FINANCE BANK"), listOf("FINCAR")),
        b("NESFB", 0xFF00796B, listOf("NORTH EAST SMALL FINANCE BANK"), listOf("NESFBL")),
        b("FINO", 0xFF1565C0, listOf("FINO PAYMENTS BANK"), listOf("FINOPB")),
        // Rural (Gramin / Grameena) banks
        b("MGB", 0xFF1B7A3D, listOf("MAHARASHTRA GRAMIN BANK", "MGB"), listOf("MGBBNK", "MAHGRM", "MGBANK", "MAHAGB", "MGRBNK")),
        b("VKGB", 0xFF2E7D32, listOf("VIDARBHA KONKAN GRAMIN BANK", "VKGB"), listOf("VKGBNK")),
        b("BARODA UP", 0xFFEF7D00, listOf("BARODA UP BANK", "BARODA UTTAR PRADESH GRAMIN BANK"), listOf("BUPGB", "BARUPB"), "BUPB"),
        b("BARODA GUJARAT", 0xFFEF7D00, listOf("BARODA GUJARAT GRAMIN BANK"), listOf("BGGBNK"), "BGGB"),
        b("BARODA RAJASTHAN", 0xFFEF7D00, listOf("BARODA RAJASTHAN KSHETRIYA GRAMIN BANK"), listOf("BRKGBK"), "BRKG"),
        b("APGB", 0xFF2E7D32, listOf("ANDHRA PRADESH GRAMEENA VIKAS BANK", "APGVB"), listOf("APGVBK", "APGVB")),
        b("TGB", 0xFF1565C0, listOf("TELANGANA GRAMEENA BANK"), listOf("TGBANK", "TGRBNK")),
        b("KARNATAKA GRAMIN", 0xFFB71C1C, listOf("KARNATAKA GRAMIN BANK"), emptyList(), "KGB"),
        b("KERALA GRAMIN", 0xFF00695C, listOf("KERALA GRAMIN BANK"), emptyList(), "KGB"),
        b("TAMIL NADU GRAMA", 0xFF4527A0, listOf("TAMIL NADU GRAMA BANK", "TNGB"), listOf("TNGBNK"), "TNGB"),
        b("PUDUVAI GRAMA", 0xFF6A1B9A, listOf("PUDUVAI BHARATHIAR GRAMA BANK"), emptyList(), "PBGB"),
        b("ODISHA GRAMYA", 0xFF1565C0, listOf("ODISHA GRAMYA BANK", "UTKAL GRAMEEN BANK"), listOf("OGBANK"), "OGB"),
        b("ARYAVART", 0xFF283593, listOf("ARYAVART BANK"), listOf("ARYAVT")),
        b("PRATHAMA UP", 0xFF00838F, listOf("PRATHAMA UP GRAMIN BANK"), emptyList(), "PUPG"),
        b("SARVA UP", 0xFF2E7D32, listOf("SARVA UP GRAMIN BANK"), emptyList(), "SUPG"),
        b("UTTAR BIHAR", 0xFF0D47A1, listOf("UTTAR BIHAR GRAMIN BANK"), emptyList(), "UBGB"),
        b("DAKSHIN BIHAR", 0xFF0D47A1, listOf("DAKSHIN BIHAR GRAMIN BANK"), emptyList(), "DBGB"),
        b("MADHYA BIHAR", 0xFF0D47A1, listOf("MADHYA BIHAR GRAMIN BANK"), emptyList(), "MBGB"),
        b("MADHYA PRADESH GRAMIN", 0xFF2E7D32, listOf("MADHYA PRADESH GRAMIN BANK"), emptyList(), "MPGB"),
        b("MADHYANCHAL", 0xFF00695C, listOf("MADHYANCHAL GRAMIN BANK"), emptyList(), "MGB2"),
        b("CHHATTISGARH GRAMIN", 0xFFD32F2F, listOf("CHHATTISGARH RAJYA GRAMIN BANK"), emptyList(), "CRGB"),
        b("JHARKHAND GRAMIN", 0xFF1565C0, listOf("JHARKHAND RAJYA GRAMIN BANK"), emptyList(), "JRGB"),
        b("PUNJAB GRAMIN", 0xFFA6192E, listOf("PUNJAB GRAMIN BANK"), emptyList(), "PGB"),
        b("HIMACHAL GRAMIN", 0xFF2E7D32, listOf("HIMACHAL PRADESH GRAMIN BANK"), emptyList(), "HPGB"),
        b("ASSAM GRAMIN", 0xFF00796B, listOf("ASSAM GRAMIN VIKASH BANK"), emptyList(), "AGVB"),
        b("BANGIYA GRAMIN", 0xFF1565C0, listOf("BANGIYA GRAMIN VIKASH BANK"), emptyList(), "BGVB"),
        b("MARUDHARA GRAMIN", 0xFFEF6C00, listOf("RAJASTHAN MARUDHARA GRAMIN BANK"), emptyList(), "RMGB"),
        b("GUJARAT GRAMIN", 0xFF2E7D32, listOf("GUJARAT GRAMIN BANK"), emptyList(), "GGB"),
        // Larger co-operative banks
        b("SARASWAT", 0xFFC62828, listOf("SARASWAT BANK", "SARASWAT CO-OP BANK", "SARASWAT CO-OPERATIVE BANK"), listOf("SARSWT", "SRSWTB"), "SCB2"),
        b("COSMOS", 0xFF0D47A1, listOf("COSMOS BANK", "COSMOS CO-OP BANK", "COSMOS CO-OPERATIVE BANK"), listOf("COSMOS")),
        b("TJSB", 0xFF1B5E20, listOf("TJSB BANK", "TJSB SAHAKARI BANK"), listOf("TJSBNK")),
        b("JANATA", 0xFF6A1B9A, listOf("JANATA SAHAKARI BANK"), listOf("JSBANK"), "JSB"),
        b("NKGSB", 0xFF283593, listOf("NKGSB BANK", "NKGSB CO-OP BANK"), listOf("NKGSBB")),
        b("ABHYUDAYA", 0xFFAD1457, listOf("ABHYUDAYA BANK", "ABHYUDAYA CO-OP BANK"), listOf("ABHYUD"), "ABCB"),
        b("BHARAT CO-OP", 0xFF00695C, listOf("BHARAT CO-OPERATIVE BANK", "BHARAT CO-OP BANK"), listOf("BHARTB"), "BCB"),
        b("SVC", 0xFF1565C0, listOf("SVC BANK", "SHAMRAO VITHAL CO-OP BANK"), listOf("SVCBNK")),
        b("KALUPUR", 0xFFD32F2F, listOf("KALUPUR COMMERCIAL CO-OP BANK", "KALUPUR BANK"), listOf("KALUPR"), "KCCB"),
        b("NAGPUR NAGARIK", 0xFF2E7D32, listOf("NAGPUR NAGARIK SAHAKARI BANK"), listOf("NNSBNK"), "NNSB"),
        b("DOMBIVLI NAGARI", 0xFF4527A0, listOf("DOMBIVLI NAGARI SAHAKARI BANK"), listOf("DNSBNK"), "DNSB"),
        b("THANE BHARAT", 0xFF1565C0, listOf("THANE BHARAT SAHAKARI BANK"), listOf("TBSBNK"), "TBSB"),
        b("APNA SAHAKARI", 0xFFEF6C00, listOf("APNA SAHAKARI BANK"), listOf("APNASB"), "APNA"),
        b("MSC BANK", 0xFF00838F, listOf("MAHARASHTRA STATE CO-OP BANK", "MAHARASHTRA STATE CO-OPERATIVE BANK", "MSC BANK"), listOf("MSCBNK"), "MSCB"),
        b("NANDED DCC", 0xFF2E7D32, listOf("NANDED DISTRICT CENTRAL CO-OP BANK", "NANDED DISTRICT CENTRAL CO-OPERATIVE BANK"), listOf("NDCCBK"), "NDCC"),
        b("PUNE DCC", 0xFF1565C0, listOf("PUNE DISTRICT CENTRAL CO-OP BANK", "PUNE DISTRICT CENTRAL CO-OPERATIVE BANK"), listOf("PDCCBK"), "PDCC"),
        b("NASHIK DCC", 0xFF6A1B9A, listOf("NASHIK DISTRICT CENTRAL CO-OP BANK", "NASHIK DISTRICT CENTRAL CO-OPERATIVE BANK"), listOf("NDCCNK"), "NDCC"),
    )

    private fun rx(name: String) = name.trim().split(Regex("\\s+")).joinToString("""\s+""") { Regex.escape(it) }

    /** Alternation of every bank name, longest first so "BANK OF MAHARASHTRA" wins over a shorter name inside it. */
    val namePattern: String = all.flatMap { it.names }.distinct().sortedByDescending { it.length }.joinToString("|") { rx(it) }

    /** (sender id piece, bank label) for every bank. */
    val senderCodes: List<Pair<String, String>> = all.flatMap { bk -> bk.senders.map { it to bk.label } }

    private val byName: Map<String, String> = all.flatMap { bk -> bk.names.map { it.uppercase().replace(Regex("\\s+"), " ") to bk.label } }.toMap()
    private val byLabel: Map<String, Bank> = all.associateBy { it.label }

    /** The label for a name found in an SMS, or null when it is not one of these banks. */
    fun labelFor(matched: String): String? = byName[matched.uppercase().replace(Regex("\\s+"), " ")]

    /** The bank a stored account label ("MGB 883076", "BARODA UP 1234") starts with, or null. */
    fun forAccount(account: String): Bank? {
        val a = account.uppercase()
        return all.filter { a == it.label || a.startsWith(it.label + " ") }.maxByOrNull { it.label.length }
    }
}
