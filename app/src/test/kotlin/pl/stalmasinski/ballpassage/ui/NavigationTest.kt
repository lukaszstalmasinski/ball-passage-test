package pl.stalmasinski.ballpassage.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pl.stalmasinski.ballpassage.BuildConfig
import pl.stalmasinski.ballpassage.MainActivity
import pl.stalmasinski.ballpassage.ui.calculator.DimensionField

/** Moving between the tabs of the running app, and the Info tab, under Robolectric. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class NavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun backFromAnotherTabReturnsToCalculator() {
        composeRule.openTab(Destination.INFO)
        composeRule.onNodeWithText("Information").assertIsDisplayed()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertFalse("activity should stay open", composeRule.activity.isFinishing)
        composeRule.onNodeWithTag(TestTags.field(DimensionField.OUTER_DIAMETER)).assertExists()
        composeRule.onNodeWithText("Information").assertDoesNotExist()
    }

    @Test
    fun calculatorWithoutBallsLeadsToTheBallsTab() {
        composeRule.enterTube(outer = "48.3", wall = "5", radius = "70")

        composeRule.onNodeWithText("Add your balls to get a suggestion").performScrollTo().performClick()

        composeRule.onNodeWithTag(TestTags.SET_SELECTOR).assertIsDisplayed()
    }

    @Test
    fun infoShowsVersion() {
        composeRule.openTab(Destination.INFO)

        composeRule.onNodeWithText("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
