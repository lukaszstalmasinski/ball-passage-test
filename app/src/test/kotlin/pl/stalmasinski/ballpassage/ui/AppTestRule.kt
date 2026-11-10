package pl.stalmasinski.ballpassage.ui

import android.content.Intent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import kotlinx.coroutines.runBlocking
import org.robolectric.Shadows.shadowOf
import pl.stalmasinski.ballpassage.MainActivity
import pl.stalmasinski.ballpassage.TestBallPassageApplication
import pl.stalmasinski.ballpassage.data.FakeUserDataRepository
import pl.stalmasinski.ballpassage.ui.calculator.DimensionField
import java.math.BigDecimal

/** Compose rule that runs the whole app (MainActivity) under Robolectric. */
typealias AppTestRule = AndroidComposeTestRule<*, MainActivity>

/** The in-memory repository of [TestBallPassageApplication]. */
val AppTestRule.repository: FakeUserDataRepository
    get() = (activity.application as TestBallPassageApplication).userDataRepository

/** Puts balls into the active set without going through the UI. */
fun AppTestRule.givenBalls(vararg diameters: String) {
    var id = 0
    runBlocking {
        repository.updateBallSets { sets ->
            sets.addBalls(sets.activeSetId, diameters.map(::BigDecimal)) { "ball-${id++}" }
        }
    }
    waitForIdle()
}

fun AppTestRule.enterTube(outer: String, wall: String, radius: String) {
    onNodeWithTag(TestTags.field(DimensionField.OUTER_DIAMETER)).performTextInput(outer)
    onNodeWithTag(TestTags.field(DimensionField.WALL_THICKNESS)).performTextInput(wall)
    onNodeWithTag(TestTags.field(DimensionField.BEND_RADIUS)).performTextInput(radius)
}

fun AppTestRule.openTab(destination: Destination) {
    onNodeWithTag(TestTags.destination(destination.name)).performClick()
    if (destination == Destination.BALLS) waitForTag(TestTags.SET_SELECTOR)
}

fun AppTestRule.addBall(diameter: String) {
    onNodeWithTag(TestTags.BALL_INPUT).performTextInput(diameter)
    onNodeWithTag(TestTags.BALL_ADD).performClick()
    waitForTag(TestTags.ballItem(BigDecimal(diameter)))
}

/** Waits for content that appears after a state update the compose test clock does not track. */
fun AppTestRule.waitForTag(tag: String) {
    waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() }
}

fun AppTestRule.waitForText(text: String) {
    waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
}

/** The text passed to the system share sheet by the last share action. */
fun AppTestRule.sharedText(): String {
    val chooser = shadowOf(activity).nextStartedActivity
    check(chooser.action == Intent.ACTION_CHOOSER) { "Expected the share sheet, got ${chooser.action}" }
    @Suppress("DEPRECATION")
    return chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!.getStringExtra(Intent.EXTRA_TEXT)!!
}
