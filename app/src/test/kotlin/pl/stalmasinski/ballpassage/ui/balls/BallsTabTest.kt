package pl.stalmasinski.ballpassage.ui.balls

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pl.stalmasinski.ballpassage.MainActivity
import pl.stalmasinski.ballpassage.ui.Destination
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.addBall
import pl.stalmasinski.ballpassage.ui.enterTube
import pl.stalmasinski.ballpassage.ui.givenBalls
import pl.stalmasinski.ballpassage.ui.openTab
import pl.stalmasinski.ballpassage.ui.repository
import pl.stalmasinski.ballpassage.ui.sharedText
import pl.stalmasinski.ballpassage.ui.waitForTag
import pl.stalmasinski.ballpassage.ui.waitForText
import java.math.BigDecimal

/**
 * The Balls tab of the running app, under Robolectric. Flows through dialogs run on a device
 * (androidTest), because Robolectric never gets idle after typing into a dialog.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class BallsTabTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun addedBallAppearsInTheList() {
        composeRule.openTab(Destination.BALLS)

        composeRule.addBall("32")

        composeRule.onNodeWithText("Ø 32 mm").assertIsDisplayed()
    }

    @Test
    fun failedSaveShowsAMessage() {
        composeRule.openTab(Destination.BALLS)
        composeRule.repository.failWrites = true

        composeRule.onNodeWithTag(TestTags.BALL_INPUT).performTextInput("32")
        composeRule.onNodeWithTag(TestTags.BALL_ADD).performClick()

        composeRule.waitForText("Could not save the change. Check the free storage on the phone and try again.")
    }

    @Test
    fun longListComposesOnlyTheVisibleBalls() {
        composeRule.givenBalls(*(1..200).map { "$it" }.toTypedArray())
        composeRule.openTab(Destination.BALLS)

        composeRule.onNodeWithTag(TestTags.ballItem(BigDecimal("1"))).assertExists()
        composeRule.onNodeWithTag(TestTags.ballItem(BigDecimal("200"))).assertDoesNotExist()

        composeRule.onNode(hasScrollToKeyAction()).performScrollToNode(hasTestTag(TestTags.ballItem(BigDecimal("200"))))

        composeRule.onNodeWithTag(TestTags.ballItem(BigDecimal("200"))).assertIsDisplayed()
    }

    @Test
    fun removedBallCanBeRestored() {
        composeRule.givenBalls("32")
        composeRule.openTab(Destination.BALLS)

        composeRule.onNodeWithContentDescription("Remove the 32 mm ball").performClick()
        composeRule.waitForText("Undo")
        composeRule.onNodeWithText("32 mm ball removed").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").performClick()

        composeRule.waitForTag(TestTags.ballItem(BigDecimal("32")))
    }

    @Test
    fun rejectsDuplicateBall() {
        composeRule.givenBalls("32")
        composeRule.openTab(Destination.BALLS)

        composeRule.onNodeWithTag(TestTags.BALL_INPUT).performTextInput("32,0")
        composeRule.onNodeWithTag(TestTags.BALL_ADD).performClick()

        composeRule.onNodeWithText("This ball is already in your set").assertIsDisplayed()
    }

    @Test
    fun marksBallsForTheCalculatedTube() {
        composeRule.givenBalls("30", "32", "35")
        composeRule.openTab(Destination.BALLS)
        composeRule.onNodeWithText("Calculate a tube in the calculator to see which balls fit it.").assertIsDisplayed()

        composeRule.openTab(Destination.CALCULATOR)
        composeRule.enterTube(outer = "48.3", wall = "5", radius = "70")
        composeRule.openTab(Destination.BALLS)

        composeRule.onNodeWithText("For tube 48.3 × 5 · R 70").assertIsDisplayed()
        composeRule.onNodeWithText("Ball per standard: 31.8 mm").assertIsDisplayed()
        composeRule.onNodeWithText("Use this").assertIsDisplayed()
        composeRule.onNodeWithText("Fits").assertIsDisplayed()
    }

    @Test
    fun exportShowsQrCodeAndSharesText() {
        composeRule.givenBalls("32")
        composeRule.openTab(Destination.BALLS)

        composeRule.onNodeWithTag(TestTags.EXPORT).performClick()
        composeRule.onNodeWithTag(TestTags.QR_CODE).assertExists()
        composeRule.onNodeWithTag(TestTags.EXPORT_SHARE_TEXT).performScrollTo().performClick()

        val text = composeRule.sharedText()
        assertTrue(text, text.contains("BPT1\nMy ball set\n32"))
    }
}
