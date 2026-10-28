package com.koolicream.zelu

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

// Line icons for categories (replaces the old emoji). Custom categories store "i:<key>".
object CatIcons {
    // key -> icon, in the order shown in the "new category" picker.
    val picker: LinkedHashMap<String, ImageVector> = linkedMapOf(
        "restaurant" to Icons.Rounded.Restaurant, "basket" to Icons.Rounded.ShoppingBasket,
        "cart" to Icons.Rounded.ShoppingCart, "fuel" to Icons.Rounded.LocalGasStation,
        "car" to Icons.Rounded.DirectionsCar, "bus" to Icons.Rounded.DirectionsBus,
        "flight" to Icons.Rounded.Flight, "beach" to Icons.Rounded.BeachAccess,
        "home" to Icons.Rounded.Home, "receipt" to Icons.Rounded.Receipt,
        "bolt" to Icons.Rounded.Bolt, "water" to Icons.Rounded.WaterDrop,
        "wifi" to Icons.Rounded.Wifi, "phone" to Icons.Rounded.PhoneAndroid,
        "movie" to Icons.Rounded.Movie, "music" to Icons.Rounded.MusicNote,
        "game" to Icons.Rounded.SportsEsports, "coffee" to Icons.Rounded.LocalCafe,
        "heart" to Icons.Rounded.Favorite, "fitness" to Icons.Rounded.FitnessCenter,
        "school" to Icons.Rounded.School, "book" to Icons.Rounded.MenuBook,
        "child" to Icons.Rounded.ChildCare, "pets" to Icons.Rounded.Pets,
        "cut" to Icons.Rounded.ContentCut, "laundry" to Icons.Rounded.LocalLaundryService,
        "clean" to Icons.Rounded.CleaningServices, "tools" to Icons.Rounded.Handyman,
        "gift" to Icons.Rounded.CardGiftcard, "work" to Icons.Rounded.Work,
        "cash" to Icons.Rounded.Payments, "wallet" to Icons.Rounded.AccountBalanceWallet,
        "bank" to Icons.Rounded.AccountBalance, "trend" to Icons.Rounded.TrendingUp,
        "repeat" to Icons.Rounded.Autorenew, "star" to Icons.Rounded.Star,
    )

    private val byCategory: Map<String, ImageVector> = mapOf(
        "Unknown" to Icons.Rounded.PriorityHigh,
        "Food" to Icons.Rounded.Restaurant, "Groceries" to Icons.Rounded.ShoppingBasket,
        "Shopping" to Icons.Rounded.ShoppingCart, "Fuel" to Icons.Rounded.LocalGasStation,
        "Transport" to Icons.Rounded.DirectionsCar, "Travel" to Icons.Rounded.Flight,
        "Bills" to Icons.Rounded.Receipt, "Entertainment" to Icons.Rounded.Movie,
        "Health" to Icons.Rounded.Favorite, "Education" to Icons.Rounded.School,
        "Rent" to Icons.Rounded.Home, "EMI" to Icons.Rounded.AccountBalance,
        "Investment" to Icons.Rounded.TrendingUp, "Subscriptions" to Icons.Rounded.Autorenew,
        "Personal care" to Icons.Rounded.ContentCut, "ATM / Cash" to Icons.Rounded.Payments,
        "Wallet" to Icons.Rounded.AccountBalanceWallet, "Home services" to Icons.Rounded.Handyman,
        "Donation" to Icons.Rounded.VolunteerActivism, "Transfer" to Icons.Rounded.SwapHoriz,
        "Card bill" to Icons.Rounded.CreditCard, "Bank charges" to Icons.Rounded.AccountBalance, "Income" to Icons.Rounded.ArrowDownward,
        "Refund" to Icons.Rounded.Undo, "Milk & Dairy" to Icons.Rounded.LocalDrink,
        "Internet" to Icons.Rounded.Wifi, "Mobile" to Icons.Rounded.PhoneAndroid,
        "Laundry" to Icons.Rounded.LocalLaundryService, "Maid" to Icons.Rounded.CleaningServices,
        "Domestic help" to Icons.Rounded.CleaningServices, "Gym" to Icons.Rounded.FitnessCenter,
        "FASTag" to Icons.Rounded.Toll,
        "Salary" to Icons.Rounded.Payments, "Interest" to Icons.Rounded.TrendingUp, "Dividend" to Icons.Rounded.TrendingUp,
        "Reimbursement" to Icons.Rounded.Receipt, "Rewards" to Icons.Rounded.Star, "Business" to Icons.Rounded.Handyman,
        "Gift" to Icons.Rounded.VolunteerActivism, "Rent received" to Icons.Rounded.Home,
    )

    // Icon for a category (standard or custom), or null to fall back to a letter.
    fun forCategory(name: String): ImageVector? {
        byCategory[name]?.let { return it }
        if (name in IncomeCats.custom) return Icons.Rounded.ArrowDownward
        val custom = CustomCats.map[name]?.first ?: return null
        return if (custom.startsWith("i:")) picker[custom.removePrefix("i:")] ?: Icons.Rounded.Category else null
    }

    fun forKey(key: String): ImageVector = picker[key] ?: Icons.Rounded.Category
}
