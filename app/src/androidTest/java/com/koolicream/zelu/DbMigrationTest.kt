package com.koolicream.zelu

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// The database a phone already has (version 1, payments only) becomes version 2 with every payment kept.
@RunWith(AndroidJUnit4::class)
class DbMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ZeluDb::class.java)

    @Test fun version1PaymentsSurviveTheMoveToVersion2() {
        helper.createDatabase("migration-test", 1).apply {
            execSQL("INSERT INTO txn (id, date, amount_paise, direction, counterparty, ref, raw, source, account, kind, category, ms) " +
                "VALUES ('ref-1', '2026-10-18', 34900, 'debit', 'SWIGGY', '629145872310', 'Sent Rs.349.00', 'AX-HDFCBK-S', 'HDFC 5997', 'expense', 'Food', 0)")
            close()
        }
        val db = helper.runMigrationsAndValidate("migration-test", 2, true)
        db.query("SELECT amount_paise, kind FROM txn WHERE id = 'ref-1'").use { c ->
            c.moveToFirst()
            assertEquals(34900L, c.getLong(0))
            assertEquals("expense", c.getString(1))
        }
        // The new tables are there and empty.
        db.query("SELECT COUNT(*) FROM kv").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        db.query("SELECT COUNT(*) FROM doc").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
    }
}
