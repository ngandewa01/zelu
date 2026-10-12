package com.nagesh.zelu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ZeluScreen()
                }
            }
        }
    }
}

private fun money(d: Double): String = "₹" + String.format(Locale.US, "%,.2f", d)

@Composable
fun ZeluScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var senders by remember { mutableStateOf("") }
    var own by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<Row>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }

    fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    // Reads the inbox and refreshes, in the background. Safe to call repeatedly
    // because stored transactions are de-duplicated.
    fun rescan() {
        scope.launch {
            status = "Reading messages…"
            val res = withContext(Dispatchers.IO) {
                Ingest.ingest(context, SmsReader.readInbox(context, 2000))
            }
            rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
            status = if (res.added > 0) "Added ${res.added} new." else ""
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.all { it }) {
            rescan()
        } else {
            status = "Allow SMS permission in Settings to read your messages."
        }
    }

    // On open: load saved data, then read messages automatically.
    LaunchedEffect(Unit) {
        val s = withContext(Dispatchers.IO) { Storage.loadSettings(context) }
        senders = s.bankSenders
        own = s.ownAccounts
        rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
        if (hasSmsPermission()) {
            rescan()
        } else {
            permLauncher.launch(
                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS),
            )
        }
    }

    val months = Summary.monthlyTotals(rows)
    val categories = Summary.expenseByCategory(rows)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Zelu", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { showSettings = !showSettings }) {
                Text(if (showSettings) "Close" else "Settings")
            }
        }

        if (status.isNotEmpty()) {
            Text(status, fontSize = 13.sp)
        }

        if (showSettings) {
            Spacer(Modifier.height(8.dp))
            Text("Bank sender IDs (comma separated)", fontSize = 13.sp)
            OutlinedTextField(
                value = senders,
                onValueChange = { senders = it },
                placeholder = { Text("e.g. SBIINB, HDFCBK, ICICIT") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Text("Your own UPI IDs or names (comma separated)", fontSize = 13.sp)
            OutlinedTextField(
                value = own,
                onValueChange = { own = it },
                placeholder = { Text("e.g. yourname@okaxis") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                Storage.saveSettings(context, senders, own)
                showSettings = false
                rescan()
            }) {
                Text("Save")
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Monthly", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        if (months.isEmpty()) {
            Text("No transactions yet.", fontSize = 14.sp)
        } else {
            months.forEach { m ->
                Text(
                    "${m.month}: in ${money(m.income)} | out ${money(m.expense)} | net ${money(m.net)}",
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Expenses by category", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        categories.forEach { (cat, total) ->
            Text("$cat: ${money(total)}", fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(20.dp))
        Text("Recent", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        rows.take(30).forEach { r ->
            Text(
                "${r.date}  ${r.kind}  ${r.category}  ${money(r.amount)}  ${r.counterparty ?: ""}",
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}
