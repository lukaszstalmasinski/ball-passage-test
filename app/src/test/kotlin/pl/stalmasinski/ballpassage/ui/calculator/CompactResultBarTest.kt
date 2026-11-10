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
import pl.stalmasinski.ballpassage.core.Standard
import pl.stalmasinski.ballpassage.ui.theme.BallPassageTheme
import java.math.BigDecimal

/** The bar shown above the keyboard; Robolectric shows no keyboard, so it is tested on its own. */
@RunWith(AndroidJUnit4::class)
class CompactResultBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val set = BallSet("set", "", listOf(Ball("1", BigDecimal("32"), label = "K-12"), Ball("2", BigDecimal("35"))))

    private fun show(standard: Standard, vararg inputs: Pair<DimensionField, String>, ballSet: BallSet = set) {
        val state = CalculatorViewModel.buildState(mapOf(*inputs), standard, ballSet, emptyList())
        composeRule.setContent { BallPassageTheme(dynamicColor = false) { CompactResultBar(state) } }
    }

    @Test
    fun showsTheBallDiameterAndTheRecommendedBall() {
        show(
            Standard.VGB_S_013,
            DimensionField.OUTER_DIAMETER to "48.3",
            DimensionField.WALL_THICKNESS to "5",
            DimensionField.BEND_RADIUS to "70",
        )

        composeRule.onNodeWithText("Ø 31.8 mm").assertIsDisplayed()
        composeRule.onNodeWithText("Ball 32 mm · K-12").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Extrapolated, outside the standard").assertDoesNotExist()
    }

    @Test
    fun warnsAboutExtrapolatedValues() {
        show(
            Standard.VGB_S_013,
            DimensionField.OUTER_DIAMETER to "63",
            DimensionField.WALL_THICKNESS to "4",
            DimensionField.BEND_RADIUS to "200",
        )

        composeRule.onNodeWithText("Ø 48 mm").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Extrapolated, outside the standard").assertIsDisplayed()
    }

    @Test
    fun warnsAboutTubesOutsideTheKtaScope() {
        show(
            Standard.KTA_3211_1,
            DimensionField.OUTER_DIAMETER to "48.3",
            DimensionField.WALL_THICKNESS to "3",
            DimensionField.UNDERSIZE to "0",
            ballSet = BallSet("set", "", listOf(Ball("1", BigDecimal("36")))),
        )

        // D_K = 0.9 · 42.3 − 2 = 36.07 mm
        composeRule.onNodeWithText("Ø 36.07 mm").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Outside the scope of KTA 3211.1").assertIsDisplayed()
        composeRule.onNodeWithText("Ball 36 mm").assertDoesNotExist()
    }

    @Test
    fun showsFailuresAndMissingInput() {
        show(
            Standard.KTA_3211_1,
            DimensionField.OUTER_DIAMETER to "6",
            DimensionField.WALL_THICKNESS to "2",
            DimensionField.UNDERSIZE to "0",
        )
        composeRule.onNodeWithText("Ball diameter cannot be determined").assertIsDisplayed()
    }

    @Test
    fun asksForInputWhileFieldsAreEmpty() {
        show(Standard.KTA_3211_1, DimensionField.OUTER_DIAMETER to "25")
        composeRule.onNodeWithText("Enter the tube dimensions").assertIsDisplayed()
    }

    @Test
    fun asksToCorrectInvalidFields() {
        show(Standard.VGB_S_013, DimensionField.OUTER_DIAMETER to "4.8.3")
        composeRule.onNodeWithText("Correct the highlighted fields.").assertIsDisplayed()
    }
}
