package com.koolicream.zelu

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SouthWest

// ---- Zelu themes: pick a palette (6 of them) x light/dark. 60-30-10: paper / card+ink / accent ----
object ZTheme {
    var dark by mutableStateOf(false)
    var palette by mutableStateOf(0)
}

// One mode of a palette. Soft tints and InkSoft are blended from these.
class PalSet(
    val paper: Long, val card: Long, val ink: Long, val muted: Long, val line: Long,
    val accent: Long, val accentInk: Long, val income: Long, val warn: Long,
    val hero: Hero? = null,
)

// The big "Safe to spend" block of a bolder theme: a two-colour gradient with its own text colour.
class Hero(val from: Long, val to: Long, val ink: Long)

class Pal(val name: String, val light: PalSet, val dark: PalSet)

val PALETTES: List<Pal> = listOf(
    Pal("Sand",
        PalSet(0xFFF9F8F6, 0xFFEFE9E3, 0xFF2B2622, 0xFF7A6F66, 0xFFD9CFC7, 0xFFA8845A, 0xFFFFFFFF, 0xFF6F8A6E, 0xFFA65D57),
        PalSet(0xFF1E1B18, 0xFF28241F, 0xFFEDE6DE, 0xFF9A8F85, 0xFF3A342E, 0xFFC9B59C, 0xFF2B2622, 0xFF8DAA8C, 0xFFD2807A)),
    // The bolder themes: a two-colour gradient on the Safe-to-spend block, one strong accent, tinted page and cards.
    Pal("Aurora",
        PalSet(0xFFF4F2FF, 0xFFFFFFFF, 0xFF1B1740, 0xFF6C679A, 0xFFE0DCF7, 0xFF5B3DF5, 0xFFFFFFFF, 0xFF0E9F6E, 0xFFD9344F, Hero(0xFF5B3DF5, 0xFF1CC8EE, 0xFFFFFFFF)),
        PalSet(0xFF0B0A1F, 0xFF16143A, 0xFFEEEBFF, 0xFF9C97CC, 0xFF2A2760, 0xFF8B7BFF, 0xFF0B0A1F, 0xFF3DDC97, 0xFFFF6B81, Hero(0xFF6D4DFF, 0xFF19C3E6, 0xFFFFFFFF))),
    Pal("Ember",
        PalSet(0xFFFFF4EE, 0xFFFFFFFF, 0xFF2A0F1E, 0xFF8A5C6B, 0xFFF6D7CC, 0xFFF2452D, 0xFFFFFFFF, 0xFF178F5B, 0xFFB3202A, Hero(0xFFFF8A1F, 0xFFF2288A, 0xFFFFFFFF)),
        PalSet(0xFF180812, 0xFF2A1020, 0xFFFFEDE6, 0xFFC28C9D, 0xFF4A1D36, 0xFFFF6A3D, 0xFF180812, 0xFF4BDE97, 0xFFFF7A7A, Hero(0xFFFF9A2E, 0xFFF0348F, 0xFFFFFFFF))),
    Pal("Volt",
        PalSet(0xFFF7F8EE, 0xFFFFFFFF, 0xFF101208, 0xFF666A4A, 0xFFE4E7CC, 0xFF101208, 0xFFC6FF2E, 0xFF12915A, 0xFFC7342B, Hero(0xFFB6F000, 0xFF2EE6A6, 0xFF101208)),
        PalSet(0xFF0A0A0A, 0xFF161616, 0xFFF5F5F0, 0xFF9A9A90, 0xFF2A2A26, 0xFFC6FF2E, 0xFF0A0A0A, 0xFF4ADE80, 0xFFFF6B5E, Hero(0xFFC6FF2E, 0xFF2EF2A0, 0xFF0A0A0A))),
    Pal("Ice",
        PalSet(0xFFF0FAFF, 0xFFFFFFFF, 0xFF0B2530, 0xFF52798A, 0xFFCBE8F3, 0xFF0891B2, 0xFFFFFFFF, 0xFF0E9F6E, 0xFFD9344F, Hero(0xFF0891B2, 0xFF3B82F6, 0xFFFFFFFF)),
        PalSet(0xFF06151C, 0xFF0D2733, 0xFFE6F7FD, 0xFF85B4C6, 0xFF17414F, 0xFF38D2F0, 0xFF06151C, 0xFF3DDC97, 0xFFFF6B81, Hero(0xFF0E8FB0, 0xFF3B6FF0, 0xFFFFFFFF))),
    Pal("Mint",
        PalSet(0xFFF2FFF8, 0xFFFFFFFF, 0xFF06281A, 0xFF4F8068, 0xFFCFF3E0, 0xFF059669, 0xFFFFFFFF, 0xFF0E8A3E, 0xFFD9344F, Hero(0xFF34D399, 0xFFFDE047, 0xFF06281A)),
        PalSet(0xFF04140D, 0xFF0B281B, 0xFFE4FBEF, 0xFF84BB9F, 0xFF15432F, 0xFF34D399, 0xFF04140D, 0xFF4ADE80, 0xFFFF6B81, Hero(0xFF34D399, 0xFFFDE047, 0xFF06281A))),
)

// The chosen palette is saved by name ("colours"). Older versions saved a position in a longer list ("palette");
// those palettes are gone, so each maps to the closest one here.
private val OLD_PALETTES = listOf("Sand", "Ocean", "Ocean", "Ocean", "Ocean", "Peach", "Ocean", "Mono")
private val REMOVED_PALETTES = mapOf("Sage" to "Ice", "Lagoon" to "Ice", "Ocean" to "Ice", "Lavender" to "Aurora", "Peach" to "Ember", "Mono" to "Sand", "Rose" to "Ember")

fun paletteIndex(sp: android.content.SharedPreferences): Int {
    // Themes that are gone move to the closest one left; anything unknown opens on Sand.
    val saved = sp.getString("colours", null) ?: OLD_PALETTES.getOrNull(sp.getInt("palette", 0)) ?: "Sand"
    val name = REMOVED_PALETTES[saved] ?: saved
    return PALETTES.indexOfFirst { it.name == name }.coerceAtLeast(0)
}

fun savePalette(sp: android.content.SharedPreferences, index: Int) {
    sp.edit().putString("colours", PALETTES[index].name).apply()
}

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
    /** The bolder themes' gradient block, or null for the plain themes. */
    val Hero: Hero? get() = s.hero
    // Soft tints for pills and highlight tiles, blended from the card colour.
    val WarnSoft get() = mix(Color(s.card), Color(s.warn), 0.14f)
    val IncomeSoft get() = mix(Color(s.card), Color(s.income), 0.14f)
    val OrangeSoft get() = mix(Color(s.card), Color(s.accent), 0.16f)
    val AmberSoft get() = mix(Color(s.card), AmberInk, 0.16f)
    val AmberInk get() = if (ZTheme.dark) Color(0xFFE3B35A) else Color(0xFFB7791F)
    // Neutral round tile behind category icons: white on tinted cards, a soft grey on white ones, lifted in dark mode.
    val Tile get() = when {
        ZTheme.dark -> mix(Color(s.card), Color(s.ink), 0.09f)
        s.card == 0xFFFFFFFF -> mix(Color(s.card), Color(s.ink), 0.06f)
        else -> Color.White
    }
}

// Safe-to-spend heat map: 20 steps from green (plenty of room) through yellow and orange to red (nearly out).
// [room] is today's daily allowance against a normal day (budget / days in month); 2x or more is full green.
// Light mode uses deeper tones so pale yellow stays readable on a light card.
fun heatColor(room: Double): Color {
    val stops = if (ZTheme.dark) listOf(0xFF34C759, 0xFF86D678, 0xFFD6E28C, 0xFFF5D246, 0xFFFAB46E, 0xFFF58232, 0xFFEB483C)
        else listOf(0xFF1E9E48, 0xFF5AA83C, 0xFF9A9E1E, 0xFFC99A06, 0xFFD9822A, 0xFFD9621C, 0xFFD23A2E)
    val at = listOf(0f, 0.2f, 0.35f, 0.5f, 0.65f, 0.8f, 1f)
    val step = Math.round((1 - room.coerceIn(0.0, 2.0) / 2) * 19).toInt()
    val t = step / 19f
    val i = (1 until at.size).first { t <= at[it] }
    return mix(Color(stops[i - 1]), Color(stops[i]), (t - at[i - 1]) / (at[i] - at[i - 1]))
}
// The bar's piece beyond the budget, a darker red than the last heat step.
val HeatOver get() = if (ZTheme.dark) Color(0xFFA8231B) else Color(0xFF8E1B14)

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
    Brand("urban company", "Urban Company", Color(0xFFEC5F3B), Color(0xFFFFFFFF), "U"),
    Brand("country delight", "Country Delight", Color(0xFF1E9E4A), Color(0xFFFFFFFF), "C"),
    Brand("mother dairy", "Mother Dairy", Color(0xFF00843D), Color(0xFFFFFFFF), "M"),
)

// Bright, clearly different colours, one per category, each with a lighter tone for dark mode. They sit on a
// neutral tile (ZColor.Tile), so the icon itself carries the colour and stands out.
private fun buildCatColors(): Map<String, Color> {
    val d = ZTheme.dark
    fun c(light: Long, dark: Long) = Color(if (d) dark else light)
    return mapOf(
        "Unknown" to c(0xFFE5484D, 0xFFFF6369),
        "Food" to c(0xFFFF6B1A, 0xFFFF8A3D),
        "Groceries" to c(0xFF22B455, 0xFF3DD171),
        "Shopping" to c(0xFF06B6D4, 0xFF22D3EE),
        "Health" to c(0xFFEC4899, 0xFFF472B6),
        "Travel" to c(0xFF8B5CF6, 0xFFA78BFA),
        "Entertainment" to c(0xFF6366F1, 0xFF818CF8),
        "Bills" to c(0xFF0D9488, 0xFF2DD4BF),
        "Fuel" to c(0xFFEAB308, 0xFFFACC15),
        "Transport" to c(0xFF3B82F6, 0xFF60A5FA),
        "Home services" to c(0xFFF59E0B, 0xFFFBBF24),
        "Rent" to c(0xFF9A3412, 0xFFD2875A),
        "EMI" to c(0xFF64748B, 0xFF94A3B8),
        "Subscriptions" to c(0xFFC026D3, 0xFFE879F9),
        "Gym" to c(0xFF65A30D, 0xFFA3E635),
        "Milk & Dairy" to c(0xFF0EA5E9, 0xFF7DD3FC),
        "Internet" to c(0xFF2563EB, 0xFF93C5FD),
        "Mobile" to c(0xFF7C3AED, 0xFFC4B5FD),
        "Laundry" to c(0xFF0891B2, 0xFF67E8F9),
        "Maid" to c(0xFFA16207, 0xFFE0B04A),
        "Domestic help" to c(0xFFA16207, 0xFFE0B04A),
        "Donation" to c(0xFFF43F5E, 0xFFFB7185),
        "Investment" to c(0xFF16A34A, 0xFF4ADE80),
        "FASTag" to c(0xFF475569, 0xFFA8B3C2),
        "Education" to c(0xFF4F46E5, 0xFFA5B4FC),
        "Personal care" to c(0xFFDB2777, 0xFFF9A8D4),
        "ATM / Cash" to c(0xFF15803D, 0xFF86EFAC),
        "Bank charges" to c(0xFFB45309, 0xFFF59E0B),
        "Wallet" to c(0xFF7C3AED, 0xFFC4B5FD),
        "Income" to ZColor.Income,
        "Refund" to ZColor.Income,
        "Transfer" to ZColor.Muted,
        "Card bill" to ZColor.Muted,
    )
}

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
    "Bank charges" to "💸",    // money flying away
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

// Logos of UPI and payment apps and the shops that run them, matched on the payee name as a whole word or phrase.
private val PAY_LOGOS: List<Pair<Regex, Int>> = listOf(
    "amazon ?pay" to R.drawable.pay_amazonpay, "bhim" to R.drawable.pay_bhim, "cred" to R.drawable.pay_cred, "dhani" to R.drawable.pay_dhani,
    "digi ?khata" to R.drawable.pay_digikhata, "fam ?pay" to R.drawable.pay_fampay, "flipkart" to R.drawable.pay_flipkart,
    "google ?pay|gpay|g pay" to R.drawable.pay_gpay, "groww" to R.drawable.pay_groww, "jupiter" to R.drawable.pay_jupiter,
    "kiwi" to R.drawable.pay_kiwi, "make ?my ?trip" to R.drawable.pay_makemytrip, "mobi ?kwik" to R.drawable.pay_mobikwik,
    "navi" to R.drawable.pay_navi, "paytm" to R.drawable.pay_paytm, "phone ?pe" to R.drawable.pay_phonepe, "pop ?upi" to R.drawable.pay_popupi,
    "samsung ?pay" to R.drawable.pay_samsungpay, "shriram ?one" to R.drawable.pay_shriramone, "slice" to R.drawable.pay_slice,
    "super\\.? ?money" to R.drawable.pay_supermoney, "tata ?neu" to R.drawable.pay_tataneu, "time ?pay" to R.drawable.pay_timepay,
    "whatsapp" to R.drawable.pay_whatsapp, "bajaj ?fin" to R.drawable.pay_bajaj, "aditya birla capital" to R.drawable.pay_abcapital,
    "niyo" to R.drawable.pay_niyo, "omni ?card" to R.drawable.pay_omnicard,
).map { (k, v) -> Regex("""\b(?:$k)\b""", RegexOption.IGNORE_CASE) to v }

fun payLogoFor(name: String): Int? = if (name.isBlank()) null else PAY_LOGOS.firstOrNull { it.first.containsMatchIn(name) }?.second

// The network of each credit card (label -> Visa, Mastercard, RuPay, Amex, Diners, Maestro): read from its messages, or picked by you.
object CardNet {
    val names = listOf("Visa", "Mastercard", "RuPay", "Amex", "Diners", "Maestro")
    fun logo(net: String?): Int? = when (net) {
        "Visa" -> R.drawable.net_visa; "Mastercard" -> R.drawable.net_mastercard; "RuPay" -> R.drawable.net_rupay
        "Amex" -> R.drawable.net_amex; "Diners" -> R.drawable.net_diners; "Maestro" -> R.drawable.net_maestro
        else -> null
    }
}

@Composable
fun NetBadge(account: String, size: Dp = 16.dp, modifier: Modifier = Modifier) {
    val logo = CardNet.logo(LocalAccountInfo.current.networks[account]) ?: return
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(Color.White), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(logo), contentDescription = null, modifier = Modifier.size(size * 0.8f))
    }
}

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
    CAT_COLORS[cat] ?: CustomCats.map[cat]?.let { Color(it.second) } ?: IncomeCats.color(cat) ?: Color(0xFF7B8C85)

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
    Triple(listOf("UCO"), "ucba", 0xFF1F5FA8), Triple(listOf("IOB"), "ioba", 0xFF1B3F8B), Triple(listOf("BANDHAN"), "bdbl", 0xFFC62828),
    Triple(listOf("HSBC"), "hsbc", 0xFFDB0011), Triple(listOf("CITI"), "citi", 0xFF056DAE), Triple(listOf("DBS"), "dbs", 0xFFE31837),
    Triple(listOf("IPPB"), "ippb", 0xFFE2231A), Triple(listOf("NSDL"), "nsdlpb", 0xFF1A5BA6), Triple(listOf("AMEX"), "amex", 0xFF006FCF),
)

private fun bankEntry(account: String) = account.uppercase().let { a ->
    BANKS_UI.firstOrNull { e -> e.first.any { a.startsWith(it) || a.contains(" $it") } }
}

fun bankColor(account: String): Color = bankEntry(account)?.let { Color(it.third) }
    ?: IndianBanks.forAccount(account)?.let { Color(it.color) } ?: ZColor.Ink

// What to show where a logo goes when Zelu has no logo file for the bank: its short mark in its colour,
// or the first letter when the bank is not one Zelu knows.
@Composable
fun BankMark(account: String, fallback: Color = ZColor.Accent, size: androidx.compose.ui.unit.TextUnit = 14.sp) {
    val bk = IndianBanks.forAccount(account)
    if (bk == null) {
        Text(account.take(1).uppercase(), color = fallback, fontWeight = FontWeight.Bold, fontSize = size)
    } else {
        val s = when { bk.mark.length <= 2 -> size.value; bk.mark.length == 3 -> size.value * 0.8f; else -> size.value * 0.62f }
        Text(bk.mark, color = Color(bk.color), fontWeight = FontWeight.Bold, fontSize = s.sp, maxLines = 1, softWrap = false)
    }
}

// Drawable resource id of the bank's logo, or null when we have none.
fun bankLogoRes(account: String): Int? = logoFor(bankEntry(account)?.second
    ?: IndianBanks.forAccount(account)?.let { IndianBanks.logoSlug[it.label] })

private fun logoFor(slug: String?): Int? = when (slug) {
    "hdfc" -> R.drawable.bank_hdfc; "icic" -> R.drawable.bank_icic; "sbin" -> R.drawable.bank_sbin
    "utib" -> R.drawable.bank_utib; "kkbk" -> R.drawable.bank_kkbk; "ubin" -> R.drawable.bank_ubin
    "yesb" -> R.drawable.bank_yesb; "idfb" -> R.drawable.bank_idfb; "indb" -> R.drawable.bank_indb
    "barb" -> R.drawable.bank_barb; "punb" -> R.drawable.bank_punb; "cnrb" -> R.drawable.bank_cnrb
    "bkid" -> R.drawable.bank_bkid; "fdrl" -> R.drawable.bank_fdrl; "ratn" -> R.drawable.bank_ratn
    "ibkl" -> R.drawable.bank_ibkl; "karb" -> R.drawable.bank_karb; "sibl" -> R.drawable.bank_sibl
    "kvbl" -> R.drawable.bank_kvbl; "scbl" -> R.drawable.bank_scbl; "aubl" -> R.drawable.bank_aubl
    "pytm" -> R.drawable.bank_pytm
    "ucba" -> R.drawable.bank_ucba; "ioba" -> R.drawable.bank_ioba; "bdbl" -> R.drawable.bank_bdbl
    "mahb" -> R.drawable.bank_mahb; "idib" -> R.drawable.bank_idib; "cbin" -> R.drawable.bank_cbin
    "psib" -> R.drawable.bank_psib; "jaka" -> R.drawable.bank_jaka; "csbk" -> R.drawable.bank_csbk
    "dlxb" -> R.drawable.bank_dlxb; "ciub" -> R.drawable.bank_ciub; "tmbl" -> R.drawable.bank_tmbl
    "ntbl" -> R.drawable.bank_ntbl; "dcbl" -> R.drawable.bank_dcbl; "esmf" -> R.drawable.bank_esmf
    "ujvn" -> R.drawable.bank_ujvn; "fino" -> R.drawable.bank_fino; "airp" -> R.drawable.bank_airp; "mgb" -> R.drawable.bank_mgb
    "hsbc" -> R.drawable.bank_hsbc; "citi" -> R.drawable.bank_citi; "dbs" -> R.drawable.bank_dbs
    "ippb" -> R.drawable.bank_ippb; "nsdlpb" -> R.drawable.bank_nsdlpb; "amex" -> R.drawable.net_amex
    "jiop" -> R.drawable.bank_jiop
    else -> null
}

@Composable
fun MerchantTile(name: String, category: String, size: Dp = 38.dp, kind: Kind? = null) {
    payLogoFor(name)?.let { logo ->
        Box(Modifier.size(size).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(logo), contentDescription = null, modifier = Modifier.size(size * 0.66f))
        }
        return
    }
    BrandLogos.forName(name)?.let { BrandTile(it, size); return }
    val b = brandFor(name)
    val moneyIn = kind == Kind.INCOME || kind == Kind.REFUND
    // Transfers and person-to-person lending get the banking-style swap icon. Money in gets its category's icon (salary,
    // interest...): the arrows on a card already say which way the money went, so the tile does not repeat them.
    val icon = when {
        b != null -> null
        kind == Kind.TRANSFER || kind == Kind.LENT || kind == Kind.BORROWED -> CatIcons.forCategory("Transfer")
        kind == Kind.REFUND -> CatIcons.forCategory("Refund")
        else -> CatIcons.forCategory(category)
    }
    // Old custom categories may still hold an emoji.
    val emoji = if (b == null && icon == null) CustomCats.map[category]?.first?.takeIf { !it.startsWith("i:") } else null
    // The icon carries the colour; the round tile behind it stays neutral so the colour stands out.
    val bg = b?.bg ?: ZColor.Tile
    val fg = b?.fg ?: if (moneyIn) ZColor.Income else catColor(category)
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        when {
            b != null -> Text(b.mark, color = b.fg, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.38f).sp)
            icon != null -> Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(size * 0.6f))
            emoji != null -> Text(emoji, fontSize = (size.value * 0.44f).sp)
            else -> Text((name.ifBlank { category }).trim().take(1).uppercase(),
                color = fg, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.4f).sp)
        }
    }
}

// Which accounts have others linked under them (main account -> linked ones). Read by the account tiles and the Link accounts page.
// What account and card tiles anywhere on screen read: each card's network, the labels joined into each account,
// the limits you typed, and how to ask for one. Provided once at the top of the screen (ZeluApp) from the AppState
// that owns it, so no tile reaches into a global.
class AccountInfo(
    val networks: Map<String, String> = emptyMap(),
    val links: Map<String, List<String>> = emptyMap(),
    val manualLimits: Set<String> = emptySet(),
    val askLimit: (String) -> Unit = {},
    val networksChanged: () -> Unit = {},
)
val LocalAccountInfo = androidx.compose.runtime.compositionLocalOf { AccountInfo() }

// Hide balances: one switch for account balances, total balance and card limits.
// Money on the account, card and loan cards: hidden behind dots while the eye button is on.
fun privateMoney(v: Double): String = if (Privacy.hideBalance) Privacy.MASK else money(v)

object Privacy {
    var hideBalance by mutableStateOf(false)
    const val MASK = "₹ ••••"
    private fun sp(c: android.content.Context) = c.getSharedPreferences("zelu_security", android.content.Context.MODE_PRIVATE)
    fun load(c: android.content.Context) { hideBalance = sp(c).getBoolean("hide_balance", false) }
    fun setBalance(c: android.content.Context, on: Boolean) { hideBalance = on; sp(c).edit().putBoolean("hide_balance", on).apply() }
}
