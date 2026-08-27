package com.sendoku.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.sendoku.app.data.Appearance
import com.sendoku.app.data.ThemeMode
import com.sendoku.app.game.GameSettings
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.app.theme.SendokuThemeId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The settings page is seven groups, and each one has a heading you can find at a glance.
 *
 * It used to be nine headings in no particular order, with the two settings that change the
 * game itself sitting three screens apart from each other. What is tested here is the shape
 * rather than the wording: that every group is still there, and that nothing has quietly
 * fallen out of one while it was being moved into another.
 */
class SettingsGroupsTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(settings: GameSettings = GameSettings()) {
        compose.setContent {
            SendokuTheme {
                SettingsScreen(
                    settings = settings,
                    appearance = Appearance(theme = SendokuThemeId.DEEP_FIELD, mode = ThemeMode.DARK),
                    onChange = {},
                    onAppearanceChange = {},
                    onBack = {},
                    onAbout = {},
                    onExport = {},
                    onImport = {},
                    onResetCourse = {},
                    dataMessage = null,
                )
            }
        }
    }

    @Test
    fun everyGroupIsThere() {
        show()
        for (heading in listOf("Feedback", "Language", "Interface", "The board", "Game rules", "Your data")) {
            compose.onNodeWithText(heading).performScrollTo().assertIsDisplayed()
        }
        // The last one says what is behind it rather than repeating its own heading.
        compose.onNodeWithTag("settings:about").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theSoundSwitchIsTheFirstThingOnThePage() {
        // Somebody who does not want noise wants it off now. If this ever moves below the
        // themes, they will turn the app off instead of scrolling.
        show()
        compose.onNodeWithText("Sounds").assertIsDisplayed()
    }

    @Test
    fun theTwoSettingsThatChangeTheGameAreInOneGroup() {
        // The mistake limit and the hint limit decide what a puzzle costs you. They used to
        // be separated by seven board toggles, which is how somebody turns one off and never
        // finds the other.
        show()
        compose.onNodeWithText("Game rules").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("End the game after three mistakes").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("End the game after three hints").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theBoardGroupHoldsTheHelpAndNothingThatChangesTheRules() {
        show()
        compose.onNodeWithText("The board").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Highlight the row, column and box").performScrollTo().assertIsDisplayed()
        assertEquals(
            "the old headings are still on the page",
            0,
            compose.onAllNodesWithText("Mistakes").fetchSemanticsNodes().size,
        )
    }
}
