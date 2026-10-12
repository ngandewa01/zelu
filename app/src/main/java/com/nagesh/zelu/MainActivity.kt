package com.nagesh.zelu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ---- Zelu palette (60-30-10): 60% paper bg, 30% card/ink content, 10% orange accent ----
private val PAPER = Color(0xFFECEEE6)
private val CARD = Color(0xFFFFFFFF)
private val INK = Color(0xFF17231F)
private val MUTED = Color(0xFF69766F)
private val LINE = Color(0xFFD8DDD3)
private val ACCENT = Color(0xFFFF6A2B)
private val ACCENT_INK = Color(0xFFFFFFFF)
private val INCOME = Color(0xFF16685E)
private val EXPENSE = Color(0xFFB5442B)

private val IST: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { ZeluApp() } }
    }
}

private fun money(d: Double): String = "₹" + String.format(Locale.US, "%,.0f", d)

private fun fmt(pattern: String, ms: Long): String =
    SimpleDateFormat(pattern, Locale.US).apply { timeZone = IST }.format(Date(ms))

@Composable
fun ZeluApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var rows by remember { mutableStateOf<List<Row>>(emptyList()) }
    var period by remember { mutableStateOf("Month") }
    var loading by remember { mutableStateOf(false) }

    fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    fun rescan() {
        scope.launch {
            loading = true
            withContext(Dispatchers.IO) {
                if (Storage.dataVersion(context) < Storage.PARSER_VERSION) Storage.clear(context)
                Ingest.ingest(context, SmsReader.readInbox(context, 5000))
                Storage.setDataVersion(context, Storage.PARSER_VERSION)
            }
            rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
            loading = false
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> if (result.values.all { it }) rescan() }

    LaunchedEffect(Unit) {
        rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
        if (hasSmsPermission()) rescan()
        else permLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
    }

    // ---- period filtering ----
    val now = System.currentTimeMillis()
    val today = fmt("yyyy-MM-dd", now)
    val weekAgo = fmt("yyyy-MM-dd", now - 7L * 24 * 3600 * 1000)
    val monthKey = today.substring(0, 7)
    val yearKey = today.substring(0, 4)
    val monthName = fmt("MMMM", now)

    val periodRows = rows.filter { r ->
        when (period) {
            "Week" -> r.date >= weekAgo
            "Month" -> r.date.startsWith(monthKey)
            "Year" -> r.date.startsWith(yearKey)
            else -> true
        }
    }
    val periodLabel = when (period) {
        "Week" -> "this week"
        "Month" -> monthName
        "Year" -> yearKey
        else -> "all time"
    }

    val spent = periodRows.filter { it.kind == "expense" }.sumOf { it.amount }
    val income = periodRows.filter { it.kind == "income" || it.kind == "refund" }.sumOf { it.amount }
    val categories = Summary.expenseByCategory(periodRows)
    val maxCat = categories.firstOrNull()?.second ?: 1.0

    Box(Modifier.fillMaxSize().background(PAPER)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 32.dp),
        ) {
            Text("Zelu", color = INK, fontSize = 22.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(16.dp))

            // period filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Week", "Month", "Year", "All").forEach { p ->
                    val selected = p == period
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) ACCENT else CARD)
                            .border(1.dp, if (selected) ACCENT else LINE, RoundedCornerShape(20.dp))
                            .clickable { period = p }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(p, color = if (selected) ACCENT_INK else MUTED, fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Text("Spent in $periodLabel", color = MUTED, fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(), textAlignCenter = true)
            Spacer(Modifier.height(6.dp))
            CenteredBig(money(spent))
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Income", color = MUTED, fontSize = 13.sp)
                    Text(money(income), color = INCOME, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (loading) {
                Spacer(Modifier.height(12.dp))
                Text("Reading messages…", color = MUTED, fontSize = 12.sp)
            }

            Spacer(Modifier.height(28.dp))
            Text("Categories", color = INK, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            if (categories.isEmpty()) {
                Text("No spending in $periodLabel.", color = MUTED, fontSize = 14.sp)
            } else {
                categories.forEach { (cat, total) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(cat, color = INK, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(money(total), color = INK, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.fillMaxWidth().height(6.dp)
                            .clip(RoundedCornerShape(3.dp)).background(LINE),
                    ) {
                        Box(
                            Modifier.fillMaxWidth((total / maxCat).toFloat().coerceIn(0.02f, 1f))
                                .height(6.dp).clip(RoundedCornerShape(3.dp)).background(ACCENT),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Recent", color = INK, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            periodRows.take(40).forEach { r ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            r.counterparty?.takeIf { it.isNotBlank() } ?: r.category,
                            color = INK, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text("${r.date} · ${r.category}", color = MUTED, fontSize = 11.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    val isOut = r.kind == "expense"
                    Text(
                        (if (isOut) "-" else "+") + money(r.amount),
                        color = if (isOut) EXPENSE else INCOME,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun CenteredBig(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(text, color = INK, fontSize = 36.sp, fontWeight = FontWeight.Bold)
    }
}

// Small helper so we can pass a centered flag to Text without importing TextAlign everywhere.
@Composable
private fun Text(
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlignCenter: Boolean = false,
) {
    androidx.compose.material3.Text(
        text = text,
        color = color,
        fontSize = fontSize,
        modifier = modifier,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = if (textAlignCenter) androidx.compose.ui.text.style.TextAlign.Center else null,
    )
}
