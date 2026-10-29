package com.koolicream.zelu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Previews of the shared pieces, drawn by Android Studio with sample rows (no phone or data needed).
private fun sample(id: String, payee: String?, rupees: Double, dir: Dir, kind: Kind, cat: String) =
    Row(id, "2026-10-28", paiseOf(rupees), dir, payee, null, "", "AX-HDFCBK-S", "HDFC 5997", kind, cat, 0)

private val SAMPLES = listOf(
    sample("1", "SWIGGY", 349.0, Dir.DEBIT, Kind.EXPENSE, "Food"),
    sample("2", "Salary", 85_000.0, Dir.CREDIT, Kind.INCOME, "Salary"),
    sample("3", "Ravi Kumar", 2_500.0, Dir.DEBIT, Kind.LENT, "Lent"),
    sample("4", null, 1_299.0, Dir.CREDIT, Kind.REFUND, "Refund"),
)

@Preview(name = "Payment rows", widthDp = 360)
@Composable private fun TxnRowsPreview() {
    Column(Modifier.background(ZColor.Paper).padding(16.dp)) { SAMPLES.forEach { TxnRow(it, false) {} } }
}

@Preview(name = "Tab row", widthDp = 320)
@Composable private fun TabRowPreview() {
    Column(Modifier.background(ZColor.Paper).padding(16.dp)) {
        TabRowWrap {
            KindTab("income", "Accounts", true, {})
            KindTab("transfer", "Cards", false, {})
            KindTab("lent", "Loans", false, {})
        }
    }
}

@Preview(name = "Long list in a panel", widthDp = 360)
@Composable private fun PanelSlicesPreview() {
    Column(Modifier.background(ZColor.Paper).padding(16.dp)) {
        PanelEnd(top = true)
        SAMPLES.forEach { PanelSlice { TxnRow(it, false) {} } }
        PanelEnd(top = false)
    }
}

@Preview(name = "Answer buttons", widthDp = 360)
@Composable private fun ActionCirclesPreview() {
    androidx.compose.foundation.layout.Row(Modifier.background(ZColor.Paper).padding(16.dp)) {
        ActionCircle(Icons.Rounded.Check, "Same payment", 0xFF22B455, 0xFF3DD171, false, Modifier.weight(1f)) {}
        ActionCircle(Icons.Rounded.Close, "Not related", 0xFF64748B, 0xFF94A3B8, false, Modifier.weight(1f)) {}
    }
}
