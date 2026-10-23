package pl.stalmasinski.ballpassage.ui.calculator

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pl.stalmasinski.ballpassage.MainActivity

/** Drives the real activity on the JVM through Robolectric. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class CalculatorScreenTest {

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
        enter(outer = "48.3", wall = "5", radius = "70")

        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("31.8 mm")
        // d_i = 38.3 mm -> row 30–39 (index 1); R / d_o = 1.44 -> column 1.0–1.5 (index 1).
        composeRule.onNodeWithTag(TestTags.tableCell(1, 1)).performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag(TestTags.tableCell(0, 0)).assertIsNotSelected()
        composeRule.onNodeWithTag(TestTags.EXTRAPOLATION_WARNING).assertDoesNotExist()
    }

    @Test
    fun warnsWhenResultIsExtrapolated() {
        enter(outer = "63", wall = "4", radius = "200")

        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("48 mm")
        composeRule.onNodeWithTag(TestTags.EXTRAPOLATION_WARNING).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun explainsWhyNoResultIsAvailable() {
        enter(outer = "48.3", wall = "5", radius = "20")

        composeRule.onNodeWithTag(TestTags.FAILURE).performScrollTo().assertIsDisplayed()
        composeRule
            .onNodeWithText("The bend ratio R / dₒ of 0.41 is below the table minimum of 0.5.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun reportsWallTooThickOnTheField() {
        enter(outer = "20", wall = "10", radius = "40")

        composeRule
            .onNodeWithText("Wall is too thick – it must be less than half of the outer diameter")
            .performScrollTo().assertIsDisplayed()
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
    fun opensAboutSheet() {
        composeRule.onNodeWithTag(TestTags.ABOUT_BUTTON).performClick()

        composeRule.onNodeWithTag(TestTags.ABOUT_SHEET).assertIsDisplayed()
        composeRule.onNodeWithText("About Ball Passage Test").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "+pl")
    fun usesPolishTextsAndDecimalComma() {
        enter(outer = "48,3", wall = "5", radius = "70")

        composeRule.onNodeWithText("Średnica kulki").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("31,8 mm")
    }

    @Test
    @Config(qualifiers = "+de")
    fun usesGermanTexts() {
        enter(outer = "48,3", wall = "5", radius = "70")

        composeRule.onNodeWithText("Kugeldurchmesser").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("31,8 mm")
    }

    private fun enter(outer: String, wall: String, radius: String) {
        composeRule.onNodeWithTag(TestTags.field(DimensionField.OUTER_DIAMETER)).performTextInput(outer)
        composeRule.onNodeWithTag(TestTags.field(DimensionField.WALL_THICKNESS)).performTextInput(wall)
        composeRule.onNodeWithTag(TestTags.field(DimensionField.BEND_RADIUS)).performTextInput(radius)
    }
}
