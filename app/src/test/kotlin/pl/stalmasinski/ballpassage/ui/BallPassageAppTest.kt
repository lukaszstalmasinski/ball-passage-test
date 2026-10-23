package pl.stalmasinski.ballpassage.ui

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import pl.stalmasinski.ballpassage.BallPassageApplication
import pl.stalmasinski.ballpassage.BuildConfig
import pl.stalmasinski.ballpassage.MainActivity
import pl.stalmasinski.ballpassage.core.TubeDimensions
import pl.stalmasinski.ballpassage.ui.calculator.DimensionField
import java.math.BigDecimal

/** Drives the real app, including DataStore, on the JVM through Robolectric. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class BallPassageAppTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val repository get() = (composeRule.activity.application as BallPassageApplication).userDataRepository

    @Before
    @After
    fun clearUserData() = runBlocking {
        repository.clearHistory()
        repository.balls.first().forEach { repository.removeBall(it) }
    }

    @Test
    fun showsPlaceholderBeforeInput() {
        composeRule.onNodeWithText("Enter the tube dimensions to calculate the ball diameter.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun calculatesMinimumBallDiameterAndHighlightsTableCell() {
        enter(outer = "48.3", wall = "5", radius = "70")

        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("31.8 mm")
        composeRule.onNodeWithText("Suitable ball: 31.8 mm to less than 38.3 mm").assertIsDisplayed()
        // d_i = 38.3 mm -> row 30–39 (index 1); R / d_o = 1.44 -> column 1.0–1.5 (index 1).
        composeRule.onNodeWithTag(TestTags.tableCell(1, 1)).performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag(TestTags.tableCell(0, 0)).assertIsNotSelected()
        composeRule.onNodeWithContentDescription("Inner diameter 30–39 mm, R / dₒ ≥ 1.0 < 1.5: 6.5 mm").assertExists()
        composeRule.onNodeWithTag(TestTags.EXTRAPOLATION_WARNING).assertDoesNotExist()
    }

    @Test
    fun warnsWhenResultIsExtrapolated() {
        enter(outer = "63", wall = "4", radius = "200")

        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("48 mm")
        composeRule.onNodeWithTag(TestTags.EXTRAPOLATION_WARNING).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun explainsHowToFixTooTightBend() {
        enter(outer = "48.3", wall = "5", radius = "20")

        composeRule.onNodeWithTag(TestTags.FAILURE).performScrollTo().assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "The bend is too tight: R / dₒ is 0.41, but the table starts at 0.5. " +
                    "The bend radius must be at least 24.15 mm.",
            )
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun reportsWallTooThickOnTheField() {
        enter(outer = "20", wall = "10", radius = "40")

        composeRule
            .onNodeWithText("Wall is too thick – it must be less than half of the outer diameter")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Correct the highlighted fields.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun updatesResultWhileTyping() {
        enter(outer = "48.3", wall = "5", radius = "70")

        composeRule.onNodeWithTag(TestTags.field(DimensionField.BEND_RADIUS)).performTextReplacement("120")

        // R / d_o = 2.48 -> deduction 5 mm.
        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("33.3 mm")
    }

    @Test
    fun clearAllEmptiesTheFields() {
        enter(outer = "48.3", wall = "5", radius = "70")

        composeRule.onNodeWithTag(TestTags.CLEAR_ALL).performClick()

        composeRule.onNodeWithTag(TestTags.field(DimensionField.OUTER_DIAMETER)).assertTextEquals("Outer diameter dₒ", "")
        composeRule.onNodeWithTag(TestTags.CLEAR_ALL).assertDoesNotExist()
    }

    @Test
    fun suggestsBallFromTheSet() {
        composeRule.onNodeWithTag(TestTags.destination(Destination.BALLS.name)).performClick()
        addBall("35")
        addBall("32")
        addBall("20")
        composeRule.onNodeWithTag(TestTags.destination(Destination.CALCULATOR.name)).performClick()

        enter(outer = "48.3", wall = "5", radius = "70")

        composeRule.waitUntil { composeRule.onAllNodes(hasText("From your set: use the 32 mm ball")).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Also fits: 35 mm").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun withoutBallSetOffersToCreateOne() {
        enter(outer = "48.3", wall = "5", radius = "70")

        composeRule.onNodeWithText("Add your balls to get a suggestion").performScrollTo().performClick()

        composeRule.onNodeWithText("Ball set").assertIsDisplayed()
    }

    @Test
    fun removedBallCanBeRestored() {
        composeRule.onNodeWithTag(TestTags.destination(Destination.BALLS.name)).performClick()
        addBall("32")

        composeRule.onNodeWithContentDescription("Remove the 32 mm ball").performClick()
        composeRule.waitUntil { composeRule.onAllNodes(hasText("Undo")).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("32 mm ball removed").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").performClick()

        waitForTag(TestTags.ballItem(BigDecimal("32")))
    }

    @Test
    fun rejectsDuplicateBall() {
        composeRule.onNodeWithTag(TestTags.destination(Destination.BALLS.name)).performClick()
        addBall("32")

        composeRule.onNodeWithTag(TestTags.BALL_INPUT).performTextInput("32,0")
        composeRule.onNodeWithTag(TestTags.BALL_ADD).performClick()

        composeRule.onNodeWithText("This ball is already in your set").assertIsDisplayed()
    }

    @Test
    fun recentTubeFillsTheFields() {
        runBlocking { repository.addToHistory(TubeDimensions(BigDecimal("33.7"), BigDecimal("4.5"), BigDecimal("60"))) }
        waitForTag(TestTags.historyItem(0))

        composeRule.onNodeWithTag(TestTags.historyItem(0)).assertTextContains("33.7 × 4.5 · R 60").performClick()

        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("19.7 mm")
    }

    @Test
    fun sharesResultAsText() {
        enter(outer = "48.3", wall = "5", radius = "70")

        composeRule.onNodeWithTag(TestTags.SHARE).performClick()

        val chooser = shadowOf(composeRule.activity).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val text = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!.getStringExtra(Intent.EXTRA_TEXT)!!
        assertTrue(text, text.contains("dₒ = 48.3 mm, s = 5 mm, R = 70 mm"))
        assertTrue(text, text.contains("Minimum ball diameter: 31.8 mm (ball smaller than 38.3 mm)"))
    }

    @Test
    fun infoShowsVersion() {
        composeRule.onNodeWithTag(TestTags.destination(Destination.INFO.name)).performClick()

        composeRule.onNodeWithText("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun backFromAnotherTabReturnsToCalculator() {
        composeRule.onNodeWithTag(TestTags.destination(Destination.INFO.name)).performClick()

        composeRule.onNodeWithText("Information").assertIsDisplayed()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertTrue("activity should stay open", !composeRule.activity.isFinishing)
        composeRule.onNodeWithTag(TestTags.field(DimensionField.OUTER_DIAMETER)).assertExists()
        composeRule.onNodeWithText("Information").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "+pl")
    fun usesPolishTextsAndDecimalComma() {
        enter(outer = "48,3", wall = "5", radius = "70")

        composeRule.onNodeWithText("Minimalna średnica kulki").assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("31,8 mm")
        composeRule.onNodeWithText("Kulki").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "+de")
    fun usesGermanTexts() {
        enter(outer = "48,3", wall = "5", radius = "70")

        composeRule.onNodeWithText("Mindest-Kugeldurchmesser").assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("31,8 mm")
    }

    private fun enter(outer: String, wall: String, radius: String) {
        composeRule.onNodeWithTag(TestTags.field(DimensionField.OUTER_DIAMETER)).performTextInput(outer)
        composeRule.onNodeWithTag(TestTags.field(DimensionField.WALL_THICKNESS)).performTextInput(wall)
        composeRule.onNodeWithTag(TestTags.field(DimensionField.BEND_RADIUS)).performTextInput(radius)
    }

    private fun addBall(diameter: String) {
        composeRule.onNodeWithTag(TestTags.BALL_INPUT).performTextInput(diameter)
        composeRule.onNodeWithTag(TestTags.BALL_ADD).performClick()
        waitForTag(TestTags.ballItem(BigDecimal(diameter)))
    }

    /** DataStore writes on a background thread, which the compose test clock does not wait for. */
    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
