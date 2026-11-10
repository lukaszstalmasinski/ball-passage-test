package pl.stalmasinski.ballpassage.ui

import android.content.Intent
import kotlinx.coroutines.runBlocking
import org.robolectric.Shadows.shadowOf
import pl.stalmasinski.ballpassage.TestBallPassageApplication
import pl.stalmasinski.ballpassage.data.FakeUserDataRepository
import java.math.BigDecimal

// Robolectric-only steps; the steps shared with device tests are in testShared/AppSteps.kt.

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

/** The text passed to the system share sheet by the last share action. */
fun AppTestRule.sharedText(): String {
    val chooser = shadowOf(activity).nextStartedActivity
    check(chooser.action == Intent.ACTION_CHOOSER) { "Expected the share sheet, got ${chooser.action}" }
    @Suppress("DEPRECATION")
    return chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!.getStringExtra(Intent.EXTRA_TEXT)!!
}
