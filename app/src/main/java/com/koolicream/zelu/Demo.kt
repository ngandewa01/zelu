package com.koolicream.zelu

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.Calendar
import java.util.Locale

/**
 * Sample data, for store screenshots, the review video and showing Zelu to someone. While it is on, the app runs on
 * a separate storage area (DemoContext) filled from made-up bank SMS read by the normal parser, so the screens look
 * like real use. Your own payments, settings and backups are not read or changed; switching off brings them back.
 */
object Demo {
    private const val PREFS = "zelu_demo"

    /** Whether the app is showing sample data. Read by MainActivity to pick the storage area. */
    var on by mutableStateOf(false)
        private set

    fun load(context: Context) { on = real(context).getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("on", false) }

    fun set(context: Context, value: Boolean) {
        val base = real(context)
        base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("on", value).apply()
        if (value) DemoContext(base).prepare()
        on = value
    }

    private fun real(context: Context): Context = (context as? DemoContext)?.baseContext ?: context

    private class Merchant(val name: String, val min: Int, val max: Int, val style: Char)

    // A = HDFC savings UPI, C = ICICI savings UPI, B = ICICI credit card. Names are well-known merchants.
    private val daily = listOf(
        Merchant("SWIGGY", 180, 650, 'A'), Merchant("ZOMATO", 220, 780, 'C'), Merchant("UBER INDIA", 120, 420, 'C'),
        Merchant("ZEPTO", 150, 690, 'C'), Merchant("BIGBASKET", 600, 2400, 'A'), Merchant("AMAZON PAY IN", 400, 2900, 'B'),
        Merchant("APOLLO PHARMACY", 180, 950, 'A'), Merchant("MYNTRA", 700, 2600, 'B'), Merchant("DMART", 900, 3100, 'B'),
        Merchant("BOOKMYSHOW", 300, 900, 'B'), Merchant("OLA", 110, 380, 'A'), Merchant("STARBUCKS", 260, 640, 'B'),
    )

    /** About two months of made-up bank SMS, newest first, the same on every run. */
    fun messages(now: Long = System.currentTimeMillis()): List<RawSms> {
        val rnd = java.util.Random(20261020L)
        val out = ArrayList<RawSms>()
        fun ref() = (629_000_000_000L + (rnd.nextDouble() * 999_999_999L).toLong()).toString()
        fun amt(v: Double) = String.format(Locale.US, "%,.2f", v)
        val cal = Calendar.getInstance(Dates.IST)
        for (back in 0 until 60) {
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_MONTH, -back)
            val dom = cal.get(Calendar.DAY_OF_MONTH)
            fun at(hour: Int, min: Int): Long = (cal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, min); set(Calendar.SECOND, 0)
            }.timeInMillis
            fun fmt(p: String, t: Long) = java.text.SimpleDateFormat(p, Locale.US).apply { timeZone = Dates.IST }.format(t)
            fun add(sender: String, body: String, t: Long) { if (t <= now) out.add(RawSms(sender, body, t)) }

            // Two or three everyday spends.
            repeat(2 + rnd.nextInt(2)) {
                val m = daily[rnd.nextInt(daily.size)]
                val v = (m.min + rnd.nextInt(m.max - m.min)).toDouble()
                val t = at(9 + rnd.nextInt(13), rnd.nextInt(60))
                when (m.style) {
                    'A' -> add("AX-HDFCBK-S", "Sent Rs.${amt(v)}\nFrom HDFC Bank A/C *5997\nTo ${m.name}\nOn ${fmt("dd/MM/yy", t)}\nRef ${ref()}\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808", t)
                    'C' -> add("JD-ICICIT-S", "ICICI Bank Acct XX345 debited for Rs ${amt(v)} on ${fmt("dd-MMM-yy", t)}; ${m.name} credited. UPI:${ref()}. Call 18002662 for dispute. SMS BLOCK 345 to 9215676766.", t)
                    else -> add("JD-ICICIT-S", "INR ${amt(v)} spent using ICICI Bank Card XX1291 on ${fmt("dd-MMM-yy", t)} on ${m.name}. Avl Limit: INR ${amt(150000.0 - v * 7)}. If not you, call 1800 2662/SMS BLOCK 1291 to 9215676766.", t)
                }
            }
            // Fuel every ten days or so.
            if (back % 9 == 4) {
                val t = at(8, 40)
                add("AD-SBICRD-S", "Rs.${amt(1500.0 + rnd.nextInt(900))} spent on your SBI Credit Card ending 4417 at HPCL SAI BALAJI FUEL STN on ${fmt("dd/MM/yy", t)}. Trxn. not done by you? Report at https://sbicard.com/Dispute", t)
            }
            // Monthly: salary, rent, bills, subscriptions, cash, card bill and its statement.
            when (dom) {
                1 -> { val t = at(10, 5); add("AX-HDFCBK-S", "Update! INR 92,500.00 deposited in HDFC Bank A/c XX5997 on ${fmt("dd-MMM-yy", t).uppercase()} for NEFT Cr-CITI0000002-ACME TECHNOLOGIES PVT LTD-SALARY-CITIN26274512398.Avl bal INR 1,48,210.40", t) }
                3 -> { val t = at(9, 30); add("VM-SBIUPI-T", "Dear UPI user A/C X4821 debited by 22000.0 on date ${fmt("ddMMMyy", t)} trf to SURESH PATIL Refno ${ref()}. Remarks: House rent. If not u? call 1800111109. -SBI", t) }
                5 -> { val t = at(11, 12); add("AX-HDFCBK-S", "Sent Rs.${amt(1380.0 + rnd.nextInt(300))}\nFrom HDFC Bank A/C *5997\nTo BESCOM\nOn ${fmt("dd/MM/yy", t)}\nRef ${ref()}\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808", t) }
                7 -> { val t = at(19, 2); add("JD-ICICIT-S", "ICICI Bank Acct XX345 debited for Rs 299.00 on ${fmt("dd-MMM-yy", t)}; Airtel Prepaid credited. UPI:${ref()}. Call 18002662 for dispute. SMS BLOCK 345 to 9215676766.", t) }
                10 -> { val t = at(23, 1); add("BP-AXISBK-S", "Transaction of INR 649.00 on Axis Bank Credit Card no. XX8823 at NETFLIX.COM on ${fmt("dd-MM-yy HH:mm:ss", t)} IST via standing instruction (e-mandate) is successful.\nAvl Lmt INR 92,351.00", t) }
                12 -> { val t = at(17, 45); add("VK-KOTAKB-T", "Rs 5000.00 withdrawn from Kotak Bank a/c XX3381 using Debit Card XX7710 at ATM S1BW000123 THANE on ${fmt("dd-MM-yyyy", t)}. Avl bal Rs 23,418.77. Not you? Call 18602662666 - Kotak Bank", t) }
                14 -> { val t = at(10, 0); add("JD-ICICIT-S", "Dear Customer, your ICICI Bank Credit Card XX1291 statement is generated. Total Amt Due Rs 18,430.00, Min Amt Due Rs 920.00. Due Date ${fmt("dd-MMM-yy", t + 18L * 86_400_000L)}.", t) }
                16 -> { val t = at(20, 30); add("BP-AXISBK-S", "INR 349.00 refund credited to your Axis Bank Credit Card XX8823 on ${fmt("dd-MM-yy", t)} from SWIGGY.", t) }
                22 -> { val t = at(12, 15); add("BP-AXISBK-S", "Payment of INR 6,000.00 received on your Axis Bank Credit Card no. XX8823 on ${fmt("dd-MM-yy", t)}. Thank you.", t) }
                26 -> { val t = at(18, 40); add("JD-ICICIT-S", "Payment of INR 18,430.00 received on your ICICI Bank Credit Card no. XX1291 on ${fmt("dd-MM-yy", t)}. Thank you.", t) }
            }
        }
        return out.sortedByDescending { it.date }
    }
}

/**
 * The app's storage, moved aside while sample data is on: files go to files/demo and every settings store gets a
 * "demo_" name. Reading the SMS inbox through it returns the sample messages instead.
 */
class DemoContext(base: Context) : ContextWrapper(base) {
    private val dir by lazy { File(base.filesDir, "demo").apply { mkdirs() } }
    override fun getFilesDir(): File = dir
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
        baseContext.getSharedPreferences("demo_" + name, mode)
    // Code that keeps "the application context" for later gets one that still reads sample data but wraps the
    // application, not the screen this was made from, so a closed screen is never held on to.
    private val appDemo: DemoContext by lazy { if (base.applicationContext === base) this else DemoContext(base.applicationContext) }
    override fun getApplicationContext(): Context = appDemo

    /** A fresh sample: no old demo data, accounts already confirmed so the app opens straight on Home. */
    fun prepare() {
        dir.listFiles()?.forEach { it.deleteRecursively() }
        Storage.clearTransactions(this)
        for (n in listOf("zelu_prefs", "zelu_backup", "zelu_recap", "zelu_ui")) getSharedPreferences(n, MODE_PRIVATE).edit().clear().apply()
        Storage.setAccountsConfirmed(this)
    }
}
