package pl.stalmasinski.ballpassage.ui

import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import pl.stalmasinski.ballpassage.MainActivity
import pl.stalmasinski.ballpassage.ui.calculator.DimensionField
import java.math.BigDecimal

// Steps through the running app, shared by the Robolectric tests (test) and the device tests
// (androidTest); see the testShared source directory in app/build.gradle.kts.

/** Compose rule that runs the whole app (MainActivity). */
typealias AppTestRule = AndroidComposeTestRule<*, MainActivity>

/** Upper bound for content that appears after a state update the compose test clock does not track. */
private const val WAIT_MILLIS = 15_000L

fun AppTestRule.enterTube(outer: String, wall: String, radius: String) {
    onNodeWithTag(TestTags.field(DimensionField.OUTER_DIAMETER)).performTextInput(outer)
    onNodeWithTag(TestTags.field(DimensionField.WALL_THICKNESS)).performTextInput(wall)
    onNodeWithTag(TestTags.field(DimensionField.BEND_RADIUS)).performTextInput(radius)
}

fun AppTestRule.openTab(destination: Destination) {
    waitForTag(TestTags.destination(destination.name))
    onNodeWithTag(TestTags.destination(destination.name)).performClick()
    if (destination == Destination.BALLS) waitForTag(TestTags.SET_SELECTOR)
}

fun AppTestRule.addBall(diameter: String) {
    onNodeWithTag(TestTags.BALL_INPUT).performTextInput(diameter)
    onNodeWithTag(TestTags.BALL_ADD).performClick()
    waitForBall(diameter)
}

/**
 * Waits until the ball is in the list of the Balls tab, scrolling the list to it: the list is lazy,
 * so a row off screen (e.g. under the keyboard) does not exist until it is scrolled into view.
 */
fun AppTestRule.waitForBall(diameter: String) {
    waitForTagInList(TestTags.ballItem(BigDecimal(diameter)))
}

/** Waits until a node with [tag] is in the lazy list of the screen and scrolls it into view. */
fun AppTestRule.waitForTagInList(tag: String) {
    waitUntil(timeoutMillis = WAIT_MILLIS) {
        runCatching { onAllNodes(hasScrollToKeyAction()).onFirst().performScrollToNode(hasTestTag(tag)) }.isSuccess
    }
}

fun AppTestRule.waitForTag(tag: String) {
    waitUntil(timeoutMillis = WAIT_MILLIS) { onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() }
}

fun AppTestRule.waitForText(text: String) {
    waitUntil(timeoutMillis = WAIT_MILLIS) { onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
}
