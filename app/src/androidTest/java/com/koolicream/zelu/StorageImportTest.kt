package com.koolicream.zelu

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

// Files an earlier version kept (or a restored backup wrote) are taken into the database, applied, and given back
// unchanged for backups and undo. Runs on sample data's own storage, so the phone's real data is never touched.
@RunWith(AndroidJUnit4::class)
class StorageImportTest {
    private lateinit var ctx: DemoContext

    @Before fun fresh() {
        ctx = DemoContext(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext)
        ctx.prepare()
        Storage.eraseAll(ctx)
    }

    private fun write(name: String, text: String) = File(ctx.filesDir, name).writeText(text)

    @Test fun oldFilesAreTakenInAppliedAndGivenBack() {
        val pay = JSONObject().put("id", "ref-1").put("date", "2026-10-18").put("amount", 349.0).put("direction", "debit")
            .put("counterparty", "SWIGGY").put("ref", "629145872310").put("raw", "Sent Rs.349.00").put("source", "AX-HDFCBK-S")
            .put("account", "HDFC 5997").put("kind", "expense").put("category", "Food")
        write("transactions.json", JSONObject().put("ref-1", pay).toString())
        write("overrides.json", JSONObject().put("ref-1", "expense|Groceries").toString())
        write("category_budgets.json", JSONObject().put("Food", 5000.0).toString())
        Storage.filesWritten(ctx)

        val rows = Storage.loadRows(ctx)
        assertEquals(1, rows.size)
        assertEquals(34900L, rows[0].paise)
        assertEquals(Kind.EXPENSE, rows[0].kind)
        assertEquals("Groceries", rows[0].category)          // the override was taken in and applied
        assertEquals(5000.0, Storage.loadBudgets(ctx)["Food"]!!, 0.0)

        // The files are kept aside, not read again.
        assertFalse(File(ctx.filesDir, "overrides.json").exists())
        assertTrue(File(ctx.filesDir, "overrides.json.imported").exists())
        assertTrue(File(ctx.filesDir, "transactions.json.imported").exists())

        // A backup carries the same file names and contents.
        val files = Storage.backupFiles(ctx)
        assertEquals("expense|Groceries", JSONObject(files.getValue("overrides.json")).getString("ref-1"))
        assertTrue(Storage.transactionsJson(ctx)!!.contains("ref-1"))
    }

    @Test fun oneDecisionWritesOneRowAndUndoRestoresIt() {
        write("transactions.json", JSONObject().put("ref-2", JSONObject().put("id", "ref-2").put("date", "2026-10-19")
            .put("amount", 120.0).put("direction", "debit").put("counterparty", "CAFE").put("raw", "").put("source", "")
            .put("account", "HDFC 5997").put("kind", "expense").put("category", "Unknown")).toString())
        Storage.filesWritten(ctx)
        val before = Storage.snapshot(ctx)
        Storage.setOverride(ctx, "ref-2", Kind.EXPENSE, "Food")
        assertEquals("Food", Storage.loadRows(ctx).single().category)
        Storage.restoreSnapshot(ctx, before)
        assertEquals("Unknown", Storage.loadRows(ctx).single().category)
    }
}
