package com.nagesh.zelu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

// ---- Zelu themes: pick a palette (15 of them) x light/dark. 60-30-10: paper / card+ink / accent ----
object ZTheme {
    var dark by mutableStateOf(false)
    var palette by mutableStateOf(0)
}

// One mode of a palette. Soft tints and InkSoft are blended from these.
class PalSet(
    val paper: Long, val card: Long, val ink: Long, val muted: Long, val line: Long,
    val accent: Long, val accentInk: Long, val income: Long, val warn: Long,
)

class Pal(val name: String, val light: PalSet, val dark: PalSet)

val PALETTES: List<Pal> = listOf(
    Pal("Sand",
        PalSet(0xFFF9F8F6, 0xFFEFE9E3, 0xFF2B2622, 0xFF7A6F66, 0xFFD9CFC7, 0xFFA8845A, 0xFFFFFFFF, 0xFF6F8A6E, 0xFFA65D57),
        PalSet(0xFF1E1B18, 0xFF28241F, 0xFFEDE6DE, 0xFF9A8F85, 0xFF3A342E, 0xFFC9B59C, 0xFF2B2622, 0xFF8DAA8C, 0xFFD2807A)),
    Pal("Moss",
        PalSet(0xFFEAF4E9, 0xFFF6FAF5, 0xFF1F2A1D, 0xFF667563, 0xFFACC5A6, 0xFF5C7057, 0xFFFFFFFF, 0xFF4F8A8B, 0xFFA65D57),
        PalSet(0xFF1A211A, 0xFF232B22, 0xFFE3EDE1, 0xFF8FA08C, 0xFF323E31, 0xFF89A482, 0xFF16201A, 0xFF5FA3A4, 0xFFD2807A)),
    Pal("Honey",
        PalSet(0xFFFCF0DA, 0xFFFFF8EA, 0xFF4C4541, 0xFF8A7F70, 0xFFE3D6B0, 0xFFB8862B, 0xFFFFFFFF, 0xFF7E8350, 0xFFB0584E),
        PalSet(0xFF1F1C19, 0xFF2A2622, 0xFFF3E8D2, 0xFFA39983, 0xFF3A352E, 0xFFF2C46A, 0xFF2B2622, 0xFFA5AA70, 0xFFD27E72)),
    Pal("Forest & Peach",
        PalSet(0xFFF7EAE0, 0xFFFDF6F0, 0xFF4A2819, 0xFF8A6A5A, 0xFFE9CDB8, 0xFF1D4533, 0xFFFFFFFF, 0xFF3F7A5C, 0xFFB04A3E),
        PalSet(0xFF14201A, 0xFF1D2B23, 0xFFF7EAE0, 0xFF9AA89F, 0xFF2C3C33, 0xFFF9D2BA, 0xFF14201A, 0xFF6FB08C, 0xFFE08A7A)),
    Pal("Dusk & Amber",
        PalSet(0xFFF5EFD9, 0xFFFBF8EC, 0xFF2E3347, 0xFF7B7F8E, 0xFFE8DDB4, 0xFF767F9E, 0xFFFFFFFF, 0xFF5F8F7A, 0xFFB2605A),
        PalSet(0xFF1C1F2B, 0xFF262A39, 0xFFEDEAD8, 0xFF9A9DAC, 0xFF363B4D, 0xFF9AA3C4, 0xFF1C1F2B, 0xFF7FB09A, 0xFFD4807A)),
    Pal("Slate",
        PalSet(0xFFDCDFE4, 0xFFECEEF1, 0xFF272B33, 0xFF727A87, 0xFFCDD1D8, 0xFF6574AD, 0xFFFFFFFF, 0xFF5E8F78, 0xFFB5646B),
        PalSet(0xFF1B1E23, 0xFF23272D, 0xFFE3E5E8, 0xFF8A9099, 0xFF30353C, 0xFF8F9BD0, 0xFF1B1E23, 0xFF7FAE97, 0xFFCC8087)),
    Pal("Mocha",
        PalSet(0xFFE3DDDA, 0xFFF0ECEA, 0xFF2E2828, 0xFF7D7471, 0xFFD3CBC7, 0xFFA8624F, 0xFFFFFFFF, 0xFF6E8F72, 0xFFA9555C),
        PalSet(0xFF1F1B1A, 0xFF292423, 0xFFEAE4E1, 0xFF9A908C, 0xFF38312F, 0xFFD08A76, 0xFF1F1B1A, 0xFF86A98A, 0xFFCF7A80)),
    Pal("Graphite & Copper",
        PalSet(0xFFDFDFDD, 0xFFEEEEEC, 0xFF252525, 0xFF77776F, 0xFFCDCDC9, 0xFFA8643C, 0xFFFFFFFF, 0xFF648F78, 0xFFAD5A56),
        PalSet(0xFF1A1A1A, 0xFF242424, 0xFFE6E6E4, 0xFF9A9A96, 0xFF333333, 0xFFD08D64, 0xFF1A1A1A, 0xFF7FAF96, 0xFFD07873)),
    Pal("Harbour Blue",
        PalSet(0xFFD9E0E6, 0xFFE9EEF2, 0xFF1F2A36, 0xFF6C7B8A, 0xFFC5D0DA, 0xFF457396, 0xFFFFFFFF, 0xFF5A9183, 0xFFB8686C),
        PalSet(0xFF171C22, 0xFF202830, 0xFFE1E7ED, 0xFF8A97A4, 0xFF2D3742, 0xFF7BA6C6, 0xFF171C22, 0xFF7CB3A5, 0xFFD58A8D)),
    Pal("Ink & Lilac",
        PalSet(0xFFDEDFE6, 0xFFEDEEF3, 0xFF23253A, 0xFF74768C, 0xFFCDCFDC, 0xFF7A72B8, 0xFFFFFFFF, 0xFF5E9283, 0xFFB5646F),
        PalSet(0xFF17182A, 0xFF20223A, 0xFFE4E5F2, 0xFF9496B0, 0xFF2F3250, 0xFFA9A2DB, 0xFF17182A, 0xFF7FB5A6, 0xFFD68A93)),
    Pal("Navy & Sky",
        PalSet(0xFFDCE2E8, 0xFFECF0F1, 0xFF1F2A3B, 0xFF6B778A, 0xFFC6CFD8, 0xFF2F86C4, 0xFFFFFFFF, 0xFF3F9A83, 0xFFC4604A),
        PalSet(0xFF18202C, 0xFF222C3B, 0xFFECF0F1, 0xFF93A0B3, 0xFF33425B, 0xFF4AA8E8, 0xFF10161F, 0xFF5FBF9F, 0xFFFCA27A)),
    Pal("Olive & Tan",
        PalSet(0xFFEDE6C6, 0xFFFEFAE0, 0xFF283618, 0xFF6F7550, 0xFFDCD5B0, 0xFFBC6C25, 0xFFFFFFFF, 0xFF606C38, 0xFFB04A3A),
        PalSet(0xFF1C2612, 0xFF283618, 0xFFFEFAE0, 0xFFA9A98A, 0xFF3A4A26, 0xFFDDA15E, 0xFF1C2612, 0xFF9AAE62, 0xFFD8765F)),
    Pal("Charcoal & Flame",
        PalSet(0xFFE8E3D8, 0xFFFFFCF2, 0xFF252422, 0xFF6E6A62, 0xFFCCC5B9, 0xFFEB5E28, 0xFFFFFFFF, 0xFF5B8A72, 0xFFB03A2E),
        PalSet(0xFF1A1918, 0xFF252422, 0xFFFFFCF2, 0xFFA29C90, 0xFF403D39, 0xFFF0713F, 0xFF1A1918, 0xFF7FB59A, 0xFFD9594C)),
    Pal("Persian Teal",
        PalSet(0xFFEBE5D2, 0xFFF6F1E4, 0xFF264653, 0xFF62777F, 0xFFD6CFB8, 0xFFE76F51, 0xFFFFFFFF, 0xFF238A7D, 0xFFC0392B),
        PalSet(0xFF16282F, 0xFF1E343D, 0xFFF3EEDD, 0xFF8FA6A8, 0xFF2D4651, 0xFFF4A261, 0xFF16282F, 0xFF4DBFAF, 0xFFE76F51)),
    Pal("Sage & Slate",
        PalSet(0xFFD5DCD0, 0xFFE6EBE2, 0xFF2F3E46, 0xFF667A74, 0xFFC0CBBD, 0xFF52796F, 0xFFFFFFFF, 0xFF5A9A6A, 0xFFB5604F),
        PalSet(0xFF1C2529, 0xFF26333A, 0xFFE6EBE2, 0xFF93A8A2, 0xFF354F52, 0xFF84A98C, 0xFF1C2529, 0xFF8FC49C, 0xFFD4806E)),
    Pal("Earth & Moss",
        PalSet(0xFFE3E0CF, 0xFFF1EFE2, 0xFF333D29, 0xFF78775F, 0xFFD0CDB4, 0xFF7F4F24, 0xFFFFFFFF, 0xFF656D4A, 0xFFB04A3A),
        PalSet(0xFF1F2418, 0xFF2A3120, 0xFFECE9D6, 0xFFA3A688, 0xFF414833, 0xFFC29F6E, 0xFF1F2418, 0xFFA4AC86, 0xFFD0745F)),
    Pal("Blush & Plum",
        PalSet(0xFFEFDFE3, 0xFFF7EBEC, 0xFF1D1E2C, 0xFF6A6F7C, 0xFFE2CAD6, 0xFF7F6AA0, 0xFFFFFFFF, 0xFF5E9A8C, 0xFFB5546A),
        PalSet(0xFF15161F, 0xFF1D1E2C, 0xFFF7EBEC, 0xFFA39DB3, 0xFF33344A, 0xFFC0B2D2, 0xFF15161F, 0xFF7FB8AA, 0xFFE07A92)),
    Pal("Ocean & Coral",
        PalSet(0xFFF4E9CF, 0xFFFDFCDC, 0xFF0C3C4C, 0xFF5E7F88, 0xFFE3D8B8, 0xFF0081A7, 0xFFFFFFFF, 0xFF1E9B8F, 0xFFD2554B),
        PalSet(0xFF0B2A35, 0xFF103846, 0xFFFDFCDC, 0xFF8DB0B8, 0xFF1B4C5C, 0xFF2DC3CC, 0xFF0B2A35, 0xFF6CC9A8, 0xFFF07167)),
)

private fun mix(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)

object ZColor {
    private val s: PalSet get() = PALETTES[ZTheme.palette.coerceIn(0, PALETTES.size - 1)].let { if (ZTheme.dark) it.dark else it.light }
    val Paper get() = Color(s.paper)
    val Card get() = Color(s.card)
    val Ink get() = Color(s.ink)
    val InkSoft get() = mix(Color(s.ink), Color(s.paper), 0.22f)
    val Muted get() = Color(s.muted)
    val Line get() = Color(s.line)
    val Accent get() = Color(s.accent)
    val AccentInk get() = Color(s.accentInk)
    val Income get() = Color(s.income)
    val Warn get() = Color(s.warn)
    // Soft tints for pills and highlight tiles, blended from the card colour.
    val WarnSoft get() = mix(Color(s.card), Color(s.warn), 0.14f)
    val IncomeSoft get() = mix(Color(s.card), Color(s.income), 0.14f)
    val OrangeSoft get() = mix(Color(s.card), Color(s.accent), 0.16f)
    val AmberSoft get() = mix(Color(s.card), AmberInk, 0.16f)
    val AmberInk get() = if (ZTheme.dark) Color(0xFFE3B35A) else Color(0xFFB7791F)
}

data class Brand(val key: String, val name: String, val bg: Color, val fg: Color, val mark: String)

val BRANDS = listOf(
    Brand("blinkit", "Blinkit", Color(0xFFF8CB46), Color(0xFF1C1C1C), "B"),
    Brand("swiggy", "Swiggy", Color(0xFFFC8019), Color(0xFFFFFFFF), "S"),
    Brand("zomato", "Zomato", Color(0xFFE23744), Color(0xFFFFFFFF), "Z"),
    Brand("zepto", "Zepto", Color(0xFF5F259F), Color(0xFFFFFFFF), "Z"),
    Brand("amazon", "Amazon", Color(0xFF232F3E), Color(0xFFFF9900), "a"),
    Brand("flipkart", "Flipkart", Color(0xFF2874F0), Color(0xFFFFFFFF), "F"),
    Brand("myntra", "Myntra", Color(0xFFFF3F6C), Color(0xFFFFFFFF), "M"),
    Brand("uber", "Uber", Color(0xFF111111), Color(0xFFFFFFFF), "U"),
    Brand("rapido", "Rapido", Color(0xFFFCC419), Color(0xFF1C1C1C), "R"),
    Brand("netflix", "Netflix", Color(0xFFE50914), Color(0xFFFFFFFF), "N"),
    Brand("pharmeasy", "PharmEasy", Color(0xFF10847E), Color(0xFFFFFFFF), "P"),
    Brand("bookmyshow", "BookMyShow", Color(0xFFC4242B), Color(0xFFFFFFFF), "b"),
    Brand("airtel", "Airtel", Color(0xFFE40000), Color(0xFFFFFFFF), "A"),
    Brand("jio", "Jio", Color(0xFF0A3D91), Color(0xFFFFFFFF), "J"),
    Brand("irctc", "IRCTC", Color(0xFF1E4D8C), Color(0xFFFFFFFF), "I"),
    Brand("hpcl", "HPCL", Color(0xFF0B5EA8), Color(0xFFFFFFFF), "H"),
    Brand("qualcomm", "Qualcomm", Color(0xFF3253DC), Color(0xFFFFFFFF), "Q"),
    Brand("urban company", "Urban Company", Color(0xFFEC5F3B), Color(0xFFFFFFFF), "U"),
    Brand("country delight", "Country Delight", Color(0xFF1E9E4A), Color(0xFFFFFFFF), "C"),
    Brand("mother dairy", "Mother Dairy", Color(0xFF00843D), Color(0xFFFFFFFF), "M"),
)

private fun buildCatColors(): Map<String, Color> = mapOf(
    "Food" to ZColor.Accent,
    "Groceries" to ZColor.Income,
    "Shopping" to ZColor.Ink,
    "Bills" to Color(0xFF7B8C85),
    "Entertainment" to Color(0xFFC9A13B),
    "Fuel" to Color(0xFF8A6BB0),
    "Rent" to Color(0xFFB5573D),
    "EMI" to Color(0xFF4F6D8A),
    "Travel" to Color(0xFF3E9C86),
    "Health" to Color(0xFFC97B84),
    "Transport" to Color(0xFF6E7F99),
    "Milk & Dairy" to Color(0xFF7E97B8),
    "Internet" to Color(0xFF4E6E8E),
    "Mobile" to Color(0xFF9A6FB0),
    "Laundry" to Color(0xFF5AA0A8),
    "Maid" to Color(0xFF9A8F5E),
    "Domestic help" to Color(0xFF9A8F5E),
    "Home services" to Color(0xFFC08457),
    "Donation" to Color(0xFFCB6D5A),
    "Investment" to Color(0xFF3A7D52),
    "FASTag" to Color(0xFF5C6B7A),
    "Education" to Color(0xFF4A6FA5),
    "Subscriptions" to Color(0xFF7B5EA7),
    "Gym" to Color(0xFFB5744A),
    "Personal care" to Color(0xFFCF8FB0),
    "ATM / Cash" to Color(0xFF788074),
    "Unknown" to Color(0xFFC1C8C2),
    "Income" to ZColor.Income,
    "Refund" to ZColor.Income,
    "Transfer" to ZColor.Muted,
    "Card bill" to ZColor.Muted,
    "Wallet" to Color(0xFF8A7CB8),
    "Education" to Color(0xFF5B8DB8),
)

// Built once per palette+mode, because a few entries follow the theme (Accent, Ink, Income, Muted).
private var catKey = -1
private var catCache: Map<String, Color> = emptyMap()
val CAT_COLORS: Map<String, Color>
    get() {
        val k = ZTheme.palette * 2 + (if (ZTheme.dark) 1 else 0)
        if (k != catKey) { catCache = buildCatColors(); catKey = k }
        return catCache
    }

// Emoji shown instead of a letter for service/abstract categories that rarely have a
// recognizable brand logo. Merchant-heavy categories (Food, Shopping…) keep the initial.
val CAT_ICON: Map<String, String> = mapOf(
    "Investment" to "🏺",      // amphora / jar
    "FASTag" to "🛣️",    // motorway
    "Education" to "🎓",       // graduation cap
    "Subscriptions" to "🔁",   // repeat
    "Gym" to "🏋️",       // weightlifter
    "Personal care" to "💇",   // haircut
    "ATM / Cash" to "🏧",      // ATM
    "Transfer" to "↔️",        // left-right arrow
    "Card bill" to "💳",       // credit card
    "Education" to "🎓",
    "Wallet" to "👛",          // coin purse
    "Donation" to "🙏",        // folded hands
    "Maid" to "🧹",            // broom
    "Domestic help" to "🧹",
    "Laundry" to "🧺",         // basket
    "Home services" to "🔧",   // wrench
    "Milk & Dairy" to "🥛",    // glass of milk
    "Internet" to "🌐",        // globe
    "Mobile" to "📱",          // phone
    "Bills" to "🧾",           // receipt
    "EMI" to "🏦",             // bank
    "Rent" to "🏠",            // house
)

fun brandFor(name: String): Brand? {
    val n = name.lowercase()
    return BRANDS.firstOrNull { n.contains(it.key) }
}

// User-made categories: name -> (emoji, ARGB colour). Snapshot state so tiles recompose.
object CustomCats {
    var map by mutableStateOf<Map<String, Pair<String, Long>>>(emptyMap())
    val names: List<String> get() = map.keys.toList()
}

// Chip label for a custom category (old ones carry an emoji; new ones use an icon, so just the name).
fun customLabel(c: String): String {
    val e = CustomCats.map[c]?.first
    return if (e == null || e.startsWith("i:")) c else "$e $c"
}

fun catColor(cat: String): Color =
    CAT_COLORS[cat] ?: CustomCats.map[cat]?.let { Color(it.second) } ?: Color(0xFF7B8C85)

// (match words in the account label, logo slug, brand colour)
private val BANKS_UI = listOf(
    Triple(listOf("HDFC"), "hdfc", 0xFF004C8F), Triple(listOf("ICICI"), "icic", 0xFFB02A30),
    Triple(listOf("SBI"), "sbin", 0xFF22409A), Triple(listOf("AXIS"), "utib", 0xFF97144D),
    Triple(listOf("KOTAK"), "kkbk", 0xFFD71921), Triple(listOf("UNION"), "ubin", 0xFFD2232A),
    Triple(listOf("YES"), "yesb", 0xFF0B5FA5), Triple(listOf("IDFC"), "idfb", 0xFF9C1D26),
    Triple(listOf("INDUS"), "indb", 0xFF8B1C3A), Triple(listOf("BARODA", "BOB"), "barb", 0xFFEF7D00),
    Triple(listOf("PNB", "PUNJAB"), "punb", 0xFFA6192E), Triple(listOf("CANARA"), "cnrb", 0xFF1B75BB),
    Triple(listOf("BANK OF INDIA", "BOI"), "bkid", 0xFF0A4D8C), Triple(listOf("FEDERAL"), "fdrl", 0xFF1C4E9A),
    Triple(listOf("RBL"), "ratn", 0xFF21409A), Triple(listOf("IDBI"), "ibkl", 0xFF00796B),
    Triple(listOf("KARNATAKA"), "karb", 0xFFB71C1C), Triple(listOf("SOUTH INDIAN", "SIB"), "sibl", 0xFF1E5AA8),
    Triple(listOf("KVB", "VYSYA"), "kvbl", 0xFF7B2D8E), Triple(listOf("STANDARD CHARTERED", "SCB"), "scbl", 0xFF0473EA),
    Triple(listOf("AU "), "aubl", 0xFFE8731A), Triple(listOf("PAYTM"), "pytm", 0xFF0A8FD6),
)

private fun bankEntry(account: String) = account.uppercase().let { a ->
    BANKS_UI.firstOrNull { e -> e.first.any { a.startsWith(it) || a.contains(" $it") } }
}

fun bankColor(account: String): Color = bankEntry(account)?.let { Color(it.third) } ?: ZColor.Ink

// Drawable resource id of the bank's logo, or null when we have none.
fun bankLogoRes(account: String): Int? = when (bankEntry(account)?.second) {
    "hdfc" -> R.drawable.bank_hdfc; "icic" -> R.drawable.bank_icic; "sbin" -> R.drawable.bank_sbin
    "utib" -> R.drawable.bank_utib; "kkbk" -> R.drawable.bank_kkbk; "ubin" -> R.drawable.bank_ubin
    "yesb" -> R.drawable.bank_yesb; "idfb" -> R.drawable.bank_idfb; "indb" -> R.drawable.bank_indb
    "barb" -> R.drawable.bank_barb; "punb" -> R.drawable.bank_punb; "cnrb" -> R.drawable.bank_cnrb
    "bkid" -> R.drawable.bank_bkid; "fdrl" -> R.drawable.bank_fdrl; "ratn" -> R.drawable.bank_ratn
    "ibkl" -> R.drawable.bank_ibkl; "karb" -> R.drawable.bank_karb; "sibl" -> R.drawable.bank_sibl
    "kvbl" -> R.drawable.bank_kvbl; "scbl" -> R.drawable.bank_scbl; "aubl" -> R.drawable.bank_aubl
    "pytm" -> R.drawable.bank_pytm
    else -> null
}

@Composable
fun MerchantTile(name: String, category: String, size: Dp = 38.dp, kind: String = "") {
    val b = brandFor(name)
    // Transfers and person-to-person lending get the banking-style swap icon.
    val icon = when {
        b != null -> null
        kind == "transfer" || kind == "lent" || kind == "borrowed" -> CatIcons.forCategory("Transfer")
        else -> CatIcons.forCategory(category)
    }
    // Old custom categories may still hold an emoji.
    val emoji = if (b == null && icon == null) CustomCats.map[category]?.first?.takeIf { !it.startsWith("i:") } else null
    val bg = b?.bg ?: catColor(category).copy(alpha = 0.18f)
    val fg = b?.fg ?: catColor(category)
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        when {
            b != null -> Text(b.mark, color = b.fg, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.38f).sp)
            icon != null -> Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(size * 0.54f))
            emoji != null -> Text(emoji, fontSize = (size.value * 0.44f).sp)
            else -> Text((name.ifBlank { category }).trim().take(1).uppercase(),
                color = fg, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.38f).sp)
        }
    }
}
