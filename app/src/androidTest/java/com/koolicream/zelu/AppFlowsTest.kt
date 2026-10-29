package com.koolicream.zelu

import android.content.Context
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        waitFor { onHome() }   // past the start screen
    }

    @After fun back() {
        scenario?.close()
        Demo.set(app, false)
    }

    private fun waitFor(timeoutMs: Long = 30_000, cond: () -> Boolean) = compose.waitUntil(timeoutMs) { cond() }

    // Home is the screen with the Add button (its words are drawn in capitals, so a tag is the steadier handle).
    // The emulator screen is small, so Home's Add button and the form's lower fields sit below what is visible: scroll to them first.
    private fun at(tag: String): SemanticsNodeInteraction = compose.onNodeWithTag(tag).performScrollTo()

    private fun onHome() = compose.onAllNodesWithTag("add-transaction").fetchSemanticsNodes().isNotEmpty()

    @Test fun opensOnHomeWithSamplePayments() {
        at("add-transaction").assertIsDisplayed()
        compose.onNodeWithTag("nav-ledger").assertIsDisplayed()
    }

    @Test fun tabsOpenAndHomeComesBack() {
        for (tab in listOf("spend", "payees", "dues")) {
            compose.onNodeWithTag("nav-$tab").performClick()
            waitFor { !onHome() }
        }
        compose.onNodeWithTag("nav-ledger").performClick()
        waitFor { onHome() }
        at("add-transaction").assertIsDisplayed()
    }

    @Test fun aPaymentAddedByHandAppearsOnHome() {
        at("add-transaction").performClick()
        compose.onNodeWithText("Add a transaction").assertIsDisplayed()
        at("add-amount").performTextInput("123")
        at("add-payee").performTextInput("Zelu Chai Test")
        at("add-save").performClick()
        waitFor { compose.onAllNodesWithText("Add a transaction").fetchSemanticsNodes().isEmpty() }
        waitFor { compose.onAllNodesWithText("Zelu Chai Test").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Zelu Chai Test").assertExists()
    }

    @Test fun anEmptyAmountIsNotSaved() {
        at("add-transaction").performClick()
        at("add-save").performClick()
        compose.onNodeWithText("Add a transaction").assertIsDisplayed()     // still open: nothing to save yet
    }

    @Test fun sourceAccountListHasCashAndYourAccounts() {
        at("add-transaction").performClick()
        at("add-account").performClick()
        compose.onNodeWithText("Select source account").assertIsDisplayed()
        val accounts = SemanticsMatcher("an account in the list") {
            it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("account-") == true
        }
        val n = compose.onAllNodes(accounts).fetchSemanticsNodes().size
        assertTrue("Cash plus the sample accounts, got $n", n > 1)
        at("account-Cash").assertIsDisplayed()
        at("account-Cash").performClick()
        compose.onNodeWithText("Add a transaction").assertIsDisplayed()     // back on the form
    }

    @Test fun backClosesTheForm() {
        at("add-transaction").performClick()
        compose.onNodeWithText("Add a transaction").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        waitFor { compose.onAllNodesWithText("Add a transaction").fetchSemanticsNodes().isEmpty() }
        at("add-transaction").assertIsDisplayed()
    }
}
