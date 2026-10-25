package pl.stalmasinski.ballpassage.ui.balls

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.stalmasinski.ballpassage.core.Ball
import pl.stalmasinski.ballpassage.core.BallSetCodec
import pl.stalmasinski.ballpassage.core.SharedBall
import pl.stalmasinski.ballpassage.core.SharedBallSet
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.theme.BallPassageTheme
import java.math.BigDecimal

/**
 * Dialogs on their own. Instrumented rather than Robolectric tests: Robolectric never gets idle
 * after interacting with text fields inside Compose dialogs, while devices render them normally.
 */
@RunWith(AndroidJUnit4::class)
class BallsDialogsTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) =
        rule.setContent { BallPassageTheme(dynamicColor = false) { content() } }

    @Test
    fun rangeDialogPassesTheValuesAndCloses() {
        var received: List<String>? = null
        var dismissed = false
        show {
            RangeDialog(
                onAdd = { from, to, step ->
                    received = listOf(from, to, step)
                    RangeResult.Added(3)
                },
                onDismiss = { dismissed = true },
            )
        }

        rule.onNodeWithTag(TestTags.rangeField(0)).performTextInput("20")
        rule.onNodeWithTag(TestTags.rangeField(1)).performTextInput("22,5")
        rule.onNodeWithTag(TestTags.rangeField(2)).performTextReplacement("0.5")
        rule.onNodeWithTag(TestTags.RANGE_CONFIRM).performClick()

        assertEquals(listOf("20", "22,5", "0.5"), received)
        assertTrue(dismissed)
    }

    @Test
    fun rangeDialogShowsTheError() {
        var dismissed = false
        show {
            RangeDialog(onAdd = { _, _, _ -> RangeResult.Invalid(RangeError.TO_BELOW_FROM) }, onDismiss = { dismissed = true })
        }

        rule.onNodeWithTag(TestTags.RANGE_CONFIRM).performClick()

        rule.onNodeWithText("The last diameter must not be smaller than the first").assertIsDisplayed()
        assertEquals(false, dismissed)
    }

    @Test
    fun ballDetailsDialogSavesLabelAndLocation() {
        var saved: Pair<String, String>? = null
        show {
            BallDetailsDialog(
                ball = Ball("1", BigDecimal("32"), label = "K-1"),
                onSave = { label, location -> saved = label to location },
                onDismiss = {},
            )
        }

        rule.onNodeWithText("Ø 32 mm").assertIsDisplayed()
        rule.onNodeWithTag(TestTags.BALL_LABEL).performTextReplacement("K-12")
        rule.onNodeWithTag(TestTags.BALL_LOCATION).performTextInput("case 2")
        rule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()

        assertEquals("K-12" to "case 2", saved)
    }

    @Test
    fun setNameDialogNeedsAName() {
        var confirmed: String? = null
        show { SetNameDialog(title = pl.stalmasinski.ballpassage.R.string.set_new_title, initialName = "", onConfirm = { confirmed = it }, onDismiss = {}) }

        rule.onNodeWithTag(TestTags.DIALOG_CONFIRM).assertIsNotEnabled()
        rule.onNodeWithTag(TestTags.SET_NAME).performTextInput("Hall A")
        rule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()

        assertEquals("Hall A", confirmed)
    }

    @Test
    fun deleteSetDialogNamesTheSetAndCount() {
        var confirmed = false
        show { DeleteSetDialog(setName = "Hall A", ballCount = 3, onConfirm = { confirmed = true }, onDismiss = {}) }

        rule.onNodeWithText("Delete “Hall A”?").assertIsDisplayed()
        rule.onNodeWithText("The set with 3 balls will be deleted.").assertIsDisplayed()
        rule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()

        assertTrue(confirmed)
    }

    @Test
    fun importPasteDialogFindsTheSetInAMessage() {
        var found: SharedBallSet? = null
        show { ImportPasteDialog(onParse = BallSetCodec::decode, onFound = { found = it }, onDismiss = {}) }

        rule.onNodeWithTag(TestTags.IMPORT_TEXT).performTextInput("Hi!\n\nBPT1\nHall B\n30\tK-1\n35")
        rule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()

        assertEquals("Hall B", found?.name)
        assertEquals(2, found?.balls?.size)
    }

    @Test
    fun importPasteDialogReportsMissingSet() {
        var found: SharedBallSet? = null
        show { ImportPasteDialog(onParse = BallSetCodec::decode, onFound = { found = it }, onDismiss = {}) }

        rule.onNodeWithTag(TestTags.IMPORT_TEXT).performTextInput("hello")
        rule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()

        rule.onNodeWithText("No ball set found").assertIsDisplayed()
        assertNull(found)
    }

    @Test
    fun importConfirmDialogListsTheBalls() {
        var confirmed = false
        val shared = SharedBallSet("Hall B", listOf(SharedBall(BigDecimal("30")), SharedBall(BigDecimal("35.5"))))
        show { ImportConfirmDialog(shared = shared, onConfirm = { confirmed = true }, onDismiss = {}) }

        rule.onNodeWithText("Import “Hall B”?").assertIsDisplayed()
        rule.onNodeWithText("2 balls").assertIsDisplayed()
        rule.onNodeWithText("30, 35.5 mm").assertIsDisplayed()
        rule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()

        assertTrue(confirmed)
    }
}
