package pl.stalmasinski.ballpassage.ui.balls

import androidx.compose.ui.test.assertIsDisplayed
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
import pl.stalmasinski.ballpassage.core.balls.BallSets
import pl.stalmasinski.ballpassage.ui.Destination
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.addBall
import pl.stalmasinski.ballpassage.ui.enterTube
import pl.stalmasinski.ballpassage.ui.openTab
import pl.stalmasinski.ballpassage.ui.waitForTag
import pl.stalmasinski.ballpassage.ui.waitForText
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
        goTo(Destination.BALLS)

        composeRule.onNodeWithTag(TestTags.RANGE_OPEN).performClick()
        composeRule.onNodeWithTag(TestTags.rangeField(0)).performTextInput("20")
        composeRule.onNodeWithTag(TestTags.rangeField(1)).performTextInput("22")
        composeRule.onNodeWithTag(TestTags.RANGE_CONFIRM).performClick()

        listOf("20", "21", "22").forEach { composeRule.waitForTag(TestTags.ballItem(BigDecimal(it))) }
    }

    @Test
    fun rangeDialogShowsErrors() {
        goTo(Destination.BALLS)

        composeRule.onNodeWithTag(TestTags.RANGE_OPEN).performClick()
        composeRule.onNodeWithTag(TestTags.rangeField(0)).performTextInput("30")
        composeRule.onNodeWithTag(TestTags.rangeField(1)).performTextInput("20")
        composeRule.onNodeWithTag(TestTags.RANGE_CONFIRM).performClick()

        composeRule.onNodeWithText("The last diameter must not be smaller than the first").assertIsDisplayed()
    }

    @Test
    fun ballDetailsAppearInTheRecommendation() {
        goTo(Destination.BALLS)
        composeRule.addBall("32")

        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag(TestTags.ballItem(BigDecimal("32"))).performScrollTo().performClick()
        composeRule.waitForTag(TestTags.BALL_LABEL)
        composeRule.onNodeWithTag(TestTags.BALL_LABEL).performTextInput("K-12")
        composeRule.onNodeWithTag(TestTags.BALL_LOCATION).performTextInput("case 2")
        composeRule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()
        composeRule.waitForText("K-12 · case 2")

        goTo(Destination.CALCULATOR)
        composeRule.enterTube(outer = "48.3", wall = "5", radius = "70")

        composeRule.onNodeWithText("From your set: use the 32 mm ball").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("K-12 · case 2").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun setsCanBeCreatedAndSwitched() {
        goTo(Destination.BALLS)
        composeRule.addBall("32")

        composeRule.onNodeWithTag(TestTags.SET_SELECTOR).performClick()
        composeRule.onNodeWithTag(TestTags.SET_NEW).performClick()
        composeRule.onNodeWithTag(TestTags.SET_NAME).performTextInput("Hall A")
        composeRule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()

        composeRule.waitForText("Hall A")
        composeRule.onNodeWithText("No balls added yet.").assertIsDisplayed()
        composeRule.addBall("40")

        composeRule.onNodeWithTag(TestTags.SET_SELECTOR).performClick()
        composeRule.onNodeWithText("My ball set").performClick()
        composeRule.waitForTag(TestTags.ballItem(BigDecimal("32")))
        composeRule.onNodeWithTag(TestTags.ballItem(BigDecimal("40"))).assertDoesNotExist()
    }

    @Test
    fun pastedSetIsImported() {
        goTo(Destination.BALLS)

        composeRule.onNodeWithTag(TestTags.BALLS_MENU).performClick()
        composeRule.onNodeWithText("Import: paste text").performClick()
        composeRule.onNodeWithTag(TestTags.IMPORT_TEXT).performTextInput("Hi!\n\nBPT1\nHall B\n30\tK-1\n35")
        composeRule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()
        composeRule.onNodeWithText("Import “Hall B”?").assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()

        composeRule.waitForTag(TestTags.ballItem(BigDecimal("35")))
        composeRule.onNodeWithText("Hall B").assertIsDisplayed()
        composeRule.onNodeWithText("K-1").assertIsDisplayed()
    }

    @Test
    fun deletingASetReturnsToTheOtherOne() {
        goTo(Destination.BALLS)
        composeRule.onNodeWithTag(TestTags.SET_SELECTOR).performClick()
        composeRule.onNodeWithTag(TestTags.SET_NEW).performClick()
        composeRule.onNodeWithTag(TestTags.SET_NAME).performTextInput("Hall A")
        composeRule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()
        composeRule.waitForText("Hall A")

        composeRule.onNodeWithTag(TestTags.BALLS_MENU).performClick()
        composeRule.onNodeWithText("Delete set").performClick()
        composeRule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()

        composeRule.waitForText("My ball set")
    }



    /** The navigation bar is hidden while the keyboard is open, so close it first. */
    private fun goTo(destination: Destination) {
        Espresso.closeSoftKeyboard()
        composeRule.openTab(destination)
    }



}
