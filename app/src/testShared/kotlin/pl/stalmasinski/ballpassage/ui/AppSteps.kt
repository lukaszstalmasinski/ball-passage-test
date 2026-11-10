package pl.stalmasinski.ballpassage.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
    waitForTag(TestTags.ballItem(BigDecimal(diameter)))
}

fun AppTestRule.waitForTag(tag: String) {
    waitUntil(timeoutMillis = WAIT_MILLIS) { onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() }
}

fun AppTestRule.waitForText(text: String) {
    waitUntil(timeoutMillis = WAIT_MILLIS) { onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
}
