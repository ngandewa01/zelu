package com.koolicream.zelu

import android.content.Context
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// The main flows of the whole app, on the sample data (its own storage, so the phone's real payments are never touched):
// the app opens on Home with payments, the tabs open and come back, a payment is added by hand and appears on Home, and
// the source account can be picked from the list.
@RunWith(AndroidJUnit4::class)
class AppFlowsTest {
    @get:Rule val compose = createEmptyComposeRule()

    private lateinit var app: Context
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun sampleData() {
        app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        Demo.set(app, true)                       // a fresh sample: accounts confirmed, so the app opens on Home
        val demo = DemoContext(app)
        Ingest.rebuild(demo, Demo.messages())
        Storage.setDataVersion(demo, Storage.PARSER_VERSION)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitFor { compose.onAllNodesWithText("Recent").fetchSemanticsNodes().isNotEmpty() }   // past the start screen
    }

    @After fun back() {
        scenario?.close()
        Demo.set(app, false)
    }

    private fun waitFor(timeoutMs: Long = 30_000, cond: () -> Boolean) = compose.waitUntil(timeoutMs) { cond() }

    private fun onHome() = compose.onAllNodesWithText("Recent").fetchSemanticsNodes().isNotEmpty()

    @Test fun opensOnHomeWithSamplePayments() {
        compose.onNodeWithText("Recent").assertIsDisplayed()
        compose.onNodeWithTag("add-transaction").assertIsDisplayed()
        compose.onNodeWithTag("nav-ledger").assertIsDisplayed()
    }

    @Test fun tabsOpenAndHomeComesBack() {
        for (tab in listOf("spend", "payees", "dues")) {
            compose.onNodeWithTag("nav-$tab").performClick()
            waitFor { !onHome() }
        }
        compose.onNodeWithTag("nav-ledger").performClick()
        waitFor { onHome() }
        compose.onNodeWithText("Recent").assertIsDisplayed()
    }

    @Test fun aPaymentAddedByHandAppearsOnHome() {
        compose.onNodeWithTag("add-transaction").performClick()
        compose.onNodeWithText("Add a transaction").assertIsDisplayed()
        compose.onNodeWithTag("add-amount").performTextInput("123")
        compose.onNodeWithTag("add-payee").performTextInput("Zelu Chai Test")
        compose.onNodeWithTag("add-save").performClick()
        waitFor { compose.onAllNodesWithText("Add a transaction").fetchSemanticsNodes().isEmpty() }
        waitFor { compose.onAllNodesWithText("Zelu Chai Test").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Zelu Chai Test").assertIsDisplayed()
    }

    @Test fun anEmptyAmountIsNotSaved() {
        compose.onNodeWithTag("add-transaction").performClick()
        compose.onNodeWithTag("add-save").performClick()
        compose.onNodeWithText("Add a transaction").assertIsDisplayed()     // still open: nothing to save yet
    }

    @Test fun sourceAccountListHasCashAndYourAccounts() {
        compose.onNodeWithTag("add-transaction").performClick()
        compose.onNodeWithTag("add-account").performClick()
        compose.onNodeWithText("Select source account").assertIsDisplayed()
        val accounts = SemanticsMatcher("an account in the list") {
            it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("account-") == true
        }
        val n = compose.onAllNodes(accounts).fetchSemanticsNodes().size
        assertTrue("Cash plus the sample accounts, got $n", n > 1)
        compose.onNodeWithTag("account-Cash").assertIsDisplayed()
        compose.onNodeWithTag("account-Cash").performClick()
        compose.onNodeWithText("Add a transaction").assertIsDisplayed()     // back on the form
    }

    @Test fun backClosesTheForm() {
        compose.onNodeWithTag("add-transaction").performClick()
        compose.onNodeWithText("Add a transaction").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        waitFor { compose.onAllNodesWithText("Add a transaction").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Recent").assertIsDisplayed()
    }
}
