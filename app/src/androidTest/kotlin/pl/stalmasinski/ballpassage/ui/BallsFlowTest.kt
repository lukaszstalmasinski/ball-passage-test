package pl.stalmasinski.ballpassage.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.stalmasinski.ballpassage.BallPassageApplication
import pl.stalmasinski.ballpassage.MainActivity
import pl.stalmasinski.ballpassage.core.BallSets
import pl.stalmasinski.ballpassage.ui.calculator.DimensionField
import java.math.BigDecimal

/** Ball set flows that go through dialogs, on a device or emulator. */
@RunWith(AndroidJUnit4::class)
class BallsFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val repository get() = (composeRule.activity.application as BallPassageApplication).userDataRepository

    @Before
    @After
    fun clearUserData() = runBlocking {
        repository.clearHistory()
        repository.updateBallSets { BallSets.initial("default") }
    }

    @Test
    fun rangeAddsBalls() {
        openBalls()

        composeRule.onNodeWithTag(TestTags.RANGE_OPEN).performClick()
        composeRule.onNodeWithTag(TestTags.rangeField(0)).performTextInput("20")
        composeRule.onNodeWithTag(TestTags.rangeField(1)).performTextInput("22")
        composeRule.onNodeWithTag(TestTags.RANGE_CONFIRM).performClick()

        listOf("20", "21", "22").forEach { waitForTag(TestTags.ballItem(BigDecimal(it))) }
    }

    @Test
    fun rangeDialogShowsErrors() {
        openBalls()

        composeRule.onNodeWithTag(TestTags.RANGE_OPEN).performClick()
        composeRule.onNodeWithTag(TestTags.rangeField(0)).performTextInput("30")
        composeRule.onNodeWithTag(TestTags.rangeField(1)).performTextInput("20")
        composeRule.onNodeWithTag(TestTags.RANGE_CONFIRM).performClick()

        composeRule.onNodeWithText("The last diameter must not be smaller than the first").assertIsDisplayed()
    }

    @Test
    fun ballDetailsAppearInTheRecommendation() {
        openBalls()
        addBall("32")

        composeRule.onNodeWithTag(TestTags.ballItem(BigDecimal("32"))).performClick()
        composeRule.onNodeWithTag(TestTags.BALL_LABEL).performTextInput("K-12")
        composeRule.onNodeWithTag(TestTags.BALL_LOCATION).performTextInput("case 2")
        composeRule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()
        waitForText("K-12 · case 2")

        goTo(Destination.CALCULATOR)
        enter(outer = "48.3", wall = "5", radius = "70")

        composeRule.onNodeWithText("From your set: use the 32 mm ball").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("K-12 · case 2").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun setsCanBeCreatedAndSwitched() {
        openBalls()
        addBall("32")

        composeRule.onNodeWithTag(TestTags.SET_SELECTOR).performClick()
        composeRule.onNodeWithTag(TestTags.SET_NEW).performClick()
        composeRule.onNodeWithTag(TestTags.SET_NAME).performTextInput("Hall A")
        composeRule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()

        waitForText("Hall A")
        composeRule.onNodeWithText("No balls added yet.").assertIsDisplayed()
        addBall("40")

        composeRule.onNodeWithTag(TestTags.SET_SELECTOR).performClick()
        composeRule.onNodeWithText("My ball set").performClick()
        waitForTag(TestTags.ballItem(BigDecimal("32")))
        composeRule.onNodeWithTag(TestTags.ballItem(BigDecimal("40"))).assertDoesNotExist()
    }

    @Test
    fun pastedSetIsImported() {
        openBalls()

        composeRule.onNodeWithTag(TestTags.BALLS_MENU).performClick()
        composeRule.onNodeWithText("Import: paste text").performClick()
        composeRule.onNodeWithTag(TestTags.IMPORT_TEXT).performTextInput("Hi!\n\nBPT1\nHall B\n30\tK-1\n35")
        composeRule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()
        composeRule.onNodeWithText("Import “Hall B”?").assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()

        waitForTag(TestTags.ballItem(BigDecimal("35")))
        composeRule.onNodeWithText("Hall B").assertIsDisplayed()
        composeRule.onNodeWithText("K-1").assertIsDisplayed()
    }

    @Test
    fun deletingASetReturnsToTheOtherOne() {
        openBalls()
        composeRule.onNodeWithTag(TestTags.SET_SELECTOR).performClick()
        composeRule.onNodeWithTag(TestTags.SET_NEW).performClick()
        composeRule.onNodeWithTag(TestTags.SET_NAME).performTextInput("Hall A")
        composeRule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()
        waitForText("Hall A")

        composeRule.onNodeWithTag(TestTags.BALLS_MENU).performClick()
        composeRule.onNodeWithText("Delete set").performClick()
        composeRule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()

        waitForText("My ball set")
    }

    private fun enter(outer: String, wall: String, radius: String) {
        composeRule.onNodeWithTag(TestTags.field(DimensionField.OUTER_DIAMETER)).performTextInput(outer)
        composeRule.onNodeWithTag(TestTags.field(DimensionField.WALL_THICKNESS)).performTextInput(wall)
        composeRule.onNodeWithTag(TestTags.field(DimensionField.BEND_RADIUS)).performTextInput(radius)
    }

    private fun openBalls() {
        goTo(Destination.BALLS)
        waitForTag(TestTags.SET_SELECTOR)
    }

    /** The navigation bar is hidden while the keyboard is open, so close it first. */
    private fun goTo(destination: Destination) {
        Espresso.closeSoftKeyboard()
        waitForTag(TestTags.destination(destination.name))
        composeRule.onNodeWithTag(TestTags.destination(destination.name)).performClick()
    }

    private fun addBall(diameter: String) {
        composeRule.onNodeWithTag(TestTags.BALL_INPUT).performTextInput(diameter)
        composeRule.onNodeWithTag(TestTags.BALL_ADD).performClick()
        waitForTag(TestTags.ballItem(BigDecimal(diameter)))
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
