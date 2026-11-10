package pl.stalmasinski.ballpassage.ui.calculator

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pl.stalmasinski.ballpassage.MainActivity
import pl.stalmasinski.ballpassage.core.calculation.TubeDimensions
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.enterTube
import pl.stalmasinski.ballpassage.ui.givenBalls
import pl.stalmasinski.ballpassage.ui.repository
import pl.stalmasinski.ballpassage.ui.sharedText
import pl.stalmasinski.ballpassage.ui.waitForTag
import pl.stalmasinski.ballpassage.ui.waitForText
import java.math.BigDecimal

/** The calculator tab of the running app, under Robolectric. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class CalculatorTabTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun showsPlaceholderBeforeInput() {
        composeRule.onNodeWithText("Enter the tube dimensions to calculate the ball diameter.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun calculatesBallDiameterAndHighlightsTableCell() {
        composeRule.enterTube(outer = "48.3", wall = "5", radius = "70")

        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("31.8 mm")
        composeRule.onNodeWithText("Ball diameter per standard").assertIsDisplayed()
        composeRule.onNodeWithText("less than", substring = true).assertDoesNotExist()
        // d_i = 38.3 mm -> row 30–39 (index 1); R / d_o = 1.44 -> column 1.0–1.5 (index 1).
        composeRule.onNodeWithTag(TestTags.tableCell(1, 1)).performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag(TestTags.tableCell(0, 0)).assertIsNotSelected()
        composeRule.onNodeWithContentDescription("Inner diameter 30–39 mm, R / dₒ ≥ 1.0 < 1.5: 6.5 mm").assertExists()
        composeRule.onNodeWithTag(TestTags.EXTRAPOLATION_WARNING).assertDoesNotExist()
    }

    @Test
    fun warnsWhenResultIsExtrapolated() {
        composeRule.enterTube(outer = "63", wall = "4", radius = "200")

        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("48 mm")
        composeRule.onNodeWithTag(TestTags.EXTRAPOLATION_WARNING).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun explainsHowToFixTooTightBend() {
        composeRule.enterTube(outer = "48.3", wall = "5", radius = "20")

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
        composeRule.enterTube(outer = "20", wall = "10", radius = "40")

        composeRule
            .onNodeWithText("Wall is too thick – it must be less than half of the outer diameter")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Correct the highlighted fields.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun updatesResultWhileTyping() {
        composeRule.enterTube(outer = "48.3", wall = "5", radius = "70")

        composeRule.onNodeWithTag(TestTags.field(DimensionField.BEND_RADIUS)).performTextReplacement("120")

        // R / d_o = 2.48 -> deduction 5 mm.
        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("33.3 mm")
    }

    @Test
    fun clearAllEmptiesTheFields() {
        composeRule.enterTube(outer = "48.3", wall = "5", radius = "70")

        composeRule.onNodeWithTag(TestTags.CLEAR_ALL).performClick()

        composeRule.onNodeWithTag(TestTags.field(DimensionField.OUTER_DIAMETER)).assertTextEquals("Outer diameter dₒ", "")
        composeRule.onNodeWithTag(TestTags.CLEAR_ALL).assertDoesNotExist()
    }

    @Test
    fun keyboardNextMovesToTheNextField() {
        val outer = composeRule.onNodeWithTag(TestTags.field(DimensionField.OUTER_DIAMETER))
        outer.performTextInput("48.3")
        outer.performImeAction()
        composeRule.onNodeWithTag(TestTags.field(DimensionField.WALL_THICKNESS)).assertIsFocused().performTextInput("5")
        composeRule.onNodeWithTag(TestTags.field(DimensionField.WALL_THICKNESS)).performImeAction()
        composeRule.onNodeWithTag(TestTags.field(DimensionField.BEND_RADIUS)).assertIsFocused().performTextInput("70")

        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("31.8 mm")
    }

    @Test
    fun suggestsBallFromTheSet() {
        composeRule.givenBalls("35", "32", "20")

        composeRule.enterTube(outer = "48.3", wall = "5", radius = "70")

        composeRule.waitForText("From your set: use the 32 mm ball")
        composeRule.onNodeWithText("Also fits: 35 mm").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun recentTubeFillsTheFields() {
        runBlocking { composeRule.repository.addToHistory(TubeDimensions(BigDecimal("33.7"), BigDecimal("4.5"), BigDecimal("60"))) }
        composeRule.waitForTag(TestTags.historyItem(0))

        composeRule.onNodeWithTag(TestTags.historyItem(0)).assertTextContains("33.7 × 4.5 · R 60").performClick()

        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("19.7 mm")
    }

    @Test
    fun sharesResultAsText() {
        composeRule.enterTube(outer = "48.3", wall = "5", radius = "70")

        composeRule.onNodeWithTag(TestTags.SHARE).performClick()

        val text = composeRule.sharedText()
        assertTrue(text, text.contains("dₒ = 48.3 mm, s = 5 mm, R = 70 mm"))
        assertTrue(text, text.lines().contains("Ball diameter per standard: 31.8 mm"))
    }
}
