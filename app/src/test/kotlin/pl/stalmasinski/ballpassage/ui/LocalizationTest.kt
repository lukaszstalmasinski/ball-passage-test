package pl.stalmasinski.ballpassage.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pl.stalmasinski.ballpassage.MainActivity

/** The app in Polish and German: texts, app name and the decimal comma, under Robolectric. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class LocalizationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    @Config(qualifiers = "+pl")
    fun usesPolishTextsAndDecimalComma() {
        composeRule.enterTube(outer = "48,3", wall = "5", radius = "70")

        composeRule.onNodeWithText("Średnica kulki wg normy").assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("31,8 mm")
        composeRule.onNodeWithText("Kulki").assertIsDisplayed()
        composeRule.onNodeWithText("Przelot kulki").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "+de")
    fun usesGermanTexts() {
        composeRule.enterTube(outer = "48,3", wall = "5", radius = "70")

        composeRule.onNodeWithText("Kugeldurchmesser nach Norm").assertIsDisplayed()
        composeRule.onNodeWithText("Kugeldurchlauf").assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.BALL_DIAMETER).assertTextEquals("31,8 mm")
    }
}
