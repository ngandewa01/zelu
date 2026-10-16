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

// ---- Zelu palette (60-30-10: paper / card+ink / orange accent) ----
object ZColor {
    val Paper = Color(0xFFECEEE6)
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF17231F)
    val InkSoft = Color(0xFF3B4A44)
    val Muted = Color(0xFF69766F)
    val Line = Color(0xFFD8DDD3)
    val Accent = Color(0xFFFF6A2B)
    val AccentInk = Color(0xFFFFFFFF)
    val Income = Color(0xFF16685E)
    val Warn = Color(0xFFB5442B)
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

val CAT_COLORS: Map<String, Color> = mapOf(
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
