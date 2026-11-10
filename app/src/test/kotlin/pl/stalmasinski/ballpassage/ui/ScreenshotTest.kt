package pl.stalmasinski.ballpassage.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pl.stalmasinski.ballpassage.core.balls.Ball
import pl.stalmasinski.ballpassage.core.balls.BallSelector
import pl.stalmasinski.ballpassage.core.balls.BallSet
import pl.stalmasinski.ballpassage.core.balls.SharedBallSet
import pl.stalmasinski.ballpassage.core.calculate
import pl.stalmasinski.ballpassage.core.calculation.CalculationResult
import pl.stalmasinski.ballpassage.core.tube
import pl.stalmasinski.ballpassage.ui.balls.BallsActions
import pl.stalmasinski.ballpassage.ui.balls.BallsScreen
import pl.stalmasinski.ballpassage.ui.balls.BallsUiState
import pl.stalmasinski.ballpassage.ui.balls.RangeResult
import pl.stalmasinski.ballpassage.ui.balls.TubeContext
import pl.stalmasinski.ballpassage.ui.calculator.CalculatorScreen
import pl.stalmasinski.ballpassage.ui.calculator.CalculatorViewModel
import pl.stalmasinski.ballpassage.ui.calculator.DimensionField
import pl.stalmasinski.ballpassage.ui.theme.BallPassageTheme
import java.math.BigDecimal

/**
 * Screenshots of the main screens, compared with the images in src/test/screenshots.
 *
 * - ./gradlew verifyRoborazziDebug compares (CI runs it in Checks),
 * - ./gradlew recordRoborazziDebug records new images after an intended change of the UI.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp")
class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val ballSet = BallSet(
        id = "set",
        name = "",
        balls = listOf("30", "31", "32", "33", "35").map { Ball(it, BigDecimal(it), label = if (it == "32") "K-12" else "") },
    )

    @Test
    fun calculatorResult() = capture("calculator_result") { CalculatorScreenWith("48.3", "5", "70") }

    @Test
    fun calculatorResultDark() = capture("calculator_result_dark", dark = true) { CalculatorScreenWith("48.3", "5", "70") }

    @Test
    @Config(qualifiers = "+pl")
    fun calculatorResultPolish() = capture("calculator_result_pl") { CalculatorScreenWith("48,3", "5", "70") }

    @Test
    fun calculatorExtrapolated() = capture("calculator_extrapolated") { CalculatorScreenWith("63", "4", "200") }

    @Test
    fun ballsWithTube() = capture("balls_with_tube") {
        val result = calculate("48.3", "5", "70") as CalculationResult.Success
        BallsScreen(
            state = BallsUiState(sets = listOf(ballSet), activeSet = ballSet),
            tubeContext = TubeContext(result, BallSelector.fittingBalls(result, ballSet.balls)),
            actions = NoActions,
        )
    }

    @Composable
    private fun CalculatorScreenWith(outer: String, wall: String, radius: String) {
        val state = CalculatorViewModel.buildState(
            inputs = mapOf(
                DimensionField.OUTER_DIAMETER to outer,
                DimensionField.WALL_THICKNESS to wall,
                DimensionField.BEND_RADIUS to radius,
            ),
            ballSet = ballSet,
            history = listOf(tube("48.3", "5", "70"), tube("33.7", "4.5", "60")),
        )
        CalculatorScreen(
            state = state,
            onValueChange = { _, _ -> },
            onClearAll = {},
            onApplyHistory = { _, _ -> },
            onClearHistory = {},
            onOpenBalls = {},
        )
    }

    private fun capture(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent { BallPassageTheme(darkTheme = dark, dynamicColor = false, content = content) }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    private object NoActions : BallsActions {
        override fun onInputChange(text: String) = Unit
        override fun add() = false
        override fun addRange(from: String, to: String, step: String) = RangeResult.Added(0)
        override fun remove(ball: Ball) = Unit
        override fun restore(ball: Ball) = Unit
        override fun updateDetails(ball: Ball, label: String, location: String) = Unit
        override fun createSet(name: String) = Unit
        override fun renameSet(name: String) = Unit
        override fun deleteSet() = Unit
        override fun selectSet(set: BallSet) = Unit
        override fun exportText(name: String) = ""
        override fun parseImport(text: String): SharedBallSet? = null
        override fun importSet(shared: SharedBallSet) = Unit
        override fun onSaveErrorShown() = Unit
    }

    private companion object {
        /** Rendering can differ by a few anti-aliased pixels between machines. */
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.005F))
    }
}
