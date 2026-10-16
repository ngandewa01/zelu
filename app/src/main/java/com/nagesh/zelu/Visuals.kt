package com.nagesh.zelu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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

fun catColor(cat: String): Color =
    CAT_COLORS[cat] ?: CustomCats.map[cat]?.let { Color(it.second) } ?: Color(0xFF7B8C85)

fun bankColor(account: String): Color {
    val a = account.uppercase()
    return when {
        a.startsWith("HDFC") -> Color(0xFF004C8F)
        a.startsWith("ICICI") -> Color(0xFFB02A30)
        a.startsWith("SBI") -> Color(0xFF22409A)
        a.startsWith("AXIS") -> Color(0xFF97144D)
        a.startsWith("KOTAK") -> Color(0xFFD71921)
        else -> ZColor.Ink
    }
}

@Composable
fun MerchantTile(name: String, category: String, size: Dp = 38.dp, kind: String = "") {
    val b = brandFor(name)
    // Transfers and person-to-person lending get the banking-style ⇄ symbol.
    val symbol = when {
        b != null -> null
        kind == "transfer" || kind == "lent" || kind == "borrowed" -> "↔️"
        else -> CAT_ICON[category] ?: CustomCats.map[category]?.first
    }
    val bg = b?.bg ?: catColor(category).copy(alpha = 0.18f)
    val fg = b?.fg ?: catColor(category)
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        when {
            b != null -> Text(b.mark, color = b.fg, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.38f).sp)
            symbol != null -> Text(symbol, fontSize = (size.value * 0.44f).sp)
            else -> Text((name.ifBlank { category }).trim().take(1).uppercase(),
                color = fg, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.38f).sp)
        }
    }
}
