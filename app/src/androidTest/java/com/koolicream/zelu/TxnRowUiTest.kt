package com.koolicream.zelu

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// A payment row on a real screen shows the payee and the amount with its sign.
@RunWith(AndroidJUnit4::class)
class TxnRowUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showsPayeeAndAmount() {
        val row = Row("ref-1", "2026-10-18", 34900, Dir.DEBIT, "SWIGGY", null, "", "AX-HDFCBK-S", "HDFC 5997", Kind.EXPENSE, "Food")
        compose.setContent { TxnRow(row, false) {} }
        compose.onNodeWithText("Swiggy").assertIsDisplayed()
        compose.onNodeWithText("−₹349").assertIsDisplayed()
    }
}
