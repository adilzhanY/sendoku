package com.sendoku.app.ui

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.sendoku.app.theme.SendokuTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * The question, and the answer it comes with already filled in.
 *
 * The screen is worth almost nothing if it is not one tap for the people whose phone is
 * already in a language the app speaks, and worth a great deal to the person whose phone is
 * in one language while they read another. Both of those are what these check.
 */
class FirstRunLanguageTest {

    @get:Rule
    val compose = createComposeRule()

    /** The screen, as a phone set to one particular language would draw it. */
    private fun shownOn(tag: String, onChoose: (Language) -> Unit = {}) {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(base.resources.configuration).apply {
            setLocales(LocaleList(Locale.forLanguageTag(tag)))
        }
        val context = base.createConfigurationContext(configuration)
        compose.setContent {
            // Both, and they have to agree: the configuration is what the screen reads the
            // phone's language from, and the context is what every string on it comes out of.
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalContext provides context,
            ) {
                SendokuTheme { FirstRunLanguage(onChoose = onChoose) }
            }
        }
    }

    @Test
    fun everyLanguageIsOfferedInItsOwnWords() {
        shownOn("en")
        for (language in Language.entries) {
            compose.onNodeWithTag("first-run:${language.name.lowercase()}").assertIsDisplayed()
        }
    }

    @Test
    fun aPhoneInALanguageWeSpeakIsAlreadyAnswered() {
        // German phone, one tap, and the app carries on following the phone rather than
        // pinning German: if they change their phone later, the app changes with it.
        var chosen: Language? = null
        shownOn("de") { chosen = it }
        compose.onNodeWithTag("first-run:continue").performClick()
        compose.waitForIdle()
        assertEquals(Language.SYSTEM, chosen)
        assertTrue(
            "a phone we speak should not be told there is no translation",
            compose.onAllNodesWithTag("first-run:untranslated").fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun aPhoneInALanguageWeDoNotSpeakIsToldSo() {
        // Swedish, which this app does not speak and is not planning to. The honest answer
        // is that there is no translation yet and English is the nearest thing, which is what
        // the screen says rather than quietly using English as though the phone had asked for
        // it. Pick the language here from the ones Sendoku will not have: this test failed the
        // day Portuguese arrived, which is the test doing its job.
        var chosen: Language? = null
        shownOn("sv") { chosen = it }
        compose.onNodeWithTag("first-run:untranslated").assertIsDisplayed()
        compose.onNodeWithTag("first-run:continue").performClick()
        compose.waitForIdle()
        assertEquals(Language.ENGLISH, chosen)
    }

    @Test
    fun choosingAnotherLanguageIsWhatComesBack() {
        var chosen: Language? = null
        shownOn("en") { chosen = it }
        compose.onNodeWithTag("first-run:japanese").performClick()
        compose.onNodeWithTag("first-run:continue").performClick()
        compose.waitForIdle()
        assertEquals(Language.JAPANESE, chosen)
    }
}
