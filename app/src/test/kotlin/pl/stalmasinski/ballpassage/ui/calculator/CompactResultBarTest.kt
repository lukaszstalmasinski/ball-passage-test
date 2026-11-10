package pl.stalmasinski.ballpassage.ui.calculator

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.stalmasinski.ballpassage.core.Ball
import pl.stalmasinski.ballpassage.core.BallSet
import pl.stalmasinski.ballpassage.ui.theme.BallPassageTheme
import java.math.BigDecimal

/** The bar shown above the keyboard; Robolectric shows no keyboard, so it is tested on its own. */
@RunWith(AndroidJUnit4::class)
class CompactResultBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val set = BallSet("set", "", listOf(Ball("1", BigDecimal("32"), label = "K-12"), Ball("2", BigDecimal("35"))))

    private fun show(outer: String = "", wall: String = "", radius: String = "", ballSet: BallSet = set) {
        val inputs = mapOf(
            DimensionField.OUTER_DIAMETER to outer,
            DimensionField.WALL_THICKNESS to wall,
            DimensionField.BEND_RADIUS to radius,
        )
        val state = CalculatorViewModel.buildState(inputs, ballSet, emptyList())
        composeRule.setContent { BallPassageTheme(dynamicColor = false) { CompactResultBar(state) } }
    }

    @Test
    fun showsTheBallDiameterAndTheRecommendedBallWithItsMarking() {
        show(outer = "48.3", wall = "5", radius = "70")

        composeRule.onNodeWithText("Ø 31.8 mm").assertIsDisplayed()
        composeRule.onNodeWithText("Ball 32 mm · K-12").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Extrapolated, outside the standard").assertDoesNotExist()
    }

    @Test
    fun showsARecommendedBallWithoutMarking() {
        show(outer = "48.3", wall = "5", radius = "70", ballSet = BallSet("set", "", listOf(Ball("1", BigDecimal("35")))))

        composeRule.onNodeWithText("Ball 35 mm").assertIsDisplayed()
    }

    @Test
    fun warnsAboutExtrapolatedValues() {
        show(outer = "63", wall = "4", radius = "200")

        composeRule.onNodeWithText("Ø 48 mm").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Extrapolated, outside the standard").assertIsDisplayed()
    }

    @Test
    fun showsFailures() {
        show(outer = "48.3", wall = "5", radius = "10")

        composeRule.onNodeWithText("Ball diameter cannot be determined").assertIsDisplayed()
    }

    @Test
    fun asksForInputWhileFieldsAreEmpty() {
        show(outer = "48.3")

        composeRule.onNodeWithText("Enter the tube dimensions").assertIsDisplayed()
    }

    @Test
    fun asksToCorrectInvalidFields() {
        show(outer = "4.8.3")

        composeRule.onNodeWithText("Correct the highlighted fields.").assertIsDisplayed()
    }
}
