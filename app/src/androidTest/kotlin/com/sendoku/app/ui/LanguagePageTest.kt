package com.sendoku.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.sendoku.app.data.Appearance
import com.sendoku.app.data.ThemeMode
import com.sendoku.app.game.GameSettings
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.app.theme.SendokuThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The language picker after it moved out of the middle of settings.
 *
 * Two things are being held down here, and the second is the one that would go unnoticed.
 * The first is that the page exists, lists every language once, and marks the one in force.
 * The second is that settings no longer draws that list itself: a refactor that adds the page
 * and forgets to take the old block out ships eighteen radio rows twice, and every test above
 * still passes because everything it looked for is still on the page.
 */
class LanguagePageTest {

    @get:Rule
    val compose = createComposeRule()

    private fun settings(onLanguage: () -> Unit = {}) {
        compose.setContent {
            SendokuTheme {
                SettingsScreen(
                    settings = GameSettings(),
                    appearance = Appearance(theme = SendokuThemeId.DEEP_FIELD, mode = ThemeMode.DARK),
                    onChange = {},
                    onAppearanceChange = {},
                    onBack = {},
                    onAbout = {},
                    onLanguage = onLanguage,
                    onExport = {},
                    onImport = {},
                    onResetCourse = {},
                    dataMessage = null,
                )
            }
        }
    }

    private fun page(onBack: () -> Unit = {}) {
        compose.setContent { SendokuTheme { LanguageScreen(onBack = onBack) } }
    }

    @Test
    fun settingsHasOneLanguageRowRatherThanAList() {
        settings()
        compose.onNodeWithTag("settings:language").performScrollTo().assertIsDisplayed()
        // The old block drew a row per language. If any of them is still here, the list was
        // added to the new page without being taken off the old one.
        for (language in Language.entries) {
            assertEquals(
                "settings still draws the ${language.name} row itself",
                0,
                compose.onAllNodesWithTag("language:${language.name.lowercase()}").fetchSemanticsNodes().size,
            )
        }
    }

    @Test
    fun theRowSaysTheLanguageRatherThanTheWordLanguage() {
        // A group headed LANGUAGE whose only row says Language is a stutter, not a signpost,
        // and it is also the one setting on the page whose value you could not read without
        // opening it. The row carries the language in force, written in that language.
        settings()
        assertEquals(
            "the row repeats its own heading instead of naming the language",
            1,
            compose.onAllNodesWithText("Language").fetchSemanticsNodes().size,
        )
        compose.onNodeWithText("English").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theRowOpensThePage() {
        var opened = 0
        settings(onLanguage = { opened++ })
        compose.onNodeWithTag("settings:language").performScrollTo().performClick()
        assertEquals("tapping the language row did nothing", 1, opened)
    }

    @Test
    fun thePageListsEveryLanguageExactlyOnce() {
        page()
        for (language in Language.entries) {
            val rows = compose.onAllNodesWithTag("language:${language.name.lowercase()}").fetchSemanticsNodes()
            assertEquals("${language.name} appears ${rows.size} times", 1, rows.size)
        }
        // A guard on the guard. If the enum ever collapses, every check above passes on air.
        assertTrue("there are hardly any languages to list", Language.entries.size > 12)
    }

    @Test
    fun followingThePhoneIsTheFirstRowAndTheMarkedOne() {
        // The test phone is never set to a per app language, so the app is following it, and
        // that is the row the mark belongs on. Anything else means the page opened showing a
        // choice the player has not made.
        page()
        compose.onNodeWithTag("language:system").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("language:japanese").performScrollTo().assertIsNotSelected()
    }

    /** How far down the page a row is drawn, which is the only thing that says what order is. */
    private fun topOf(language: Language): Float = compose
        .onNodeWithTag("language:${language.name.lowercase()}")
        .fetchSemanticsNode()
        .positionInRoot
        .y

    @Test
    fun thePageIsInTheOrderTheRuleGives() {
        // Languages.inDisplayOrder is tested on its own, so what is checked here is that the
        // page actually calls it. It listed them in the order the enum happened to declare
        // until this test existed, and that order looked deliberate enough to survive review.
        page()
        assertTrue("following the phone is not first", topOf(Language.SYSTEM) < topOf(Language.ENGLISH))
        // The Latin block, alphabetical, which is the part of the list people read.
        assertTrue("Bahasa Indonesia is not before Deutsch", topOf(Language.INDONESIAN) < topOf(Language.GERMAN))
        assertTrue("Deutsch is not before English", topOf(Language.GERMAN) < topOf(Language.ENGLISH))
        assertTrue("Tiếng Việt is not before Türkçe", topOf(Language.VIETNAMESE) < topOf(Language.TURKISH))
        // And the scripts in blocks behind it.
        assertTrue("Cyrillic is not after Latin", topOf(Language.TURKISH) < topOf(Language.RUSSIAN))
        assertTrue("Arabic is not after Cyrillic", topOf(Language.UKRAINIAN) < topOf(Language.ARABIC))
        assertTrue("Hindi is not after Arabic", topOf(Language.ARABIC) < topOf(Language.HINDI))
    }

    @Test
    fun choosingALanguageLeavesThePage() {
        // Popped before the language is applied, because applying it restarts the activity and
        // the back stack is saved across that restart. Without this the player taps a language
        // and lands back on the language page, in the new language, unsure whether it worked.
        var back = 0
        page(onBack = { back++ })
        compose.onNodeWithTag("language:system").performScrollTo().performClick()
        assertEquals("the page stayed open after a language was chosen", 1, back)
    }
}
