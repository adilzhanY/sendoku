package com.sendoku.app.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.sendoku.app.ui.languageAnswered
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one question the app asks, and the rule that it is asked once.
 *
 * The flag is a boolean in the preferences and nothing else, which is the whole design: not a
 * version number, not a date, not a count of launches. The only thing worth knowing is
 * whether this person has answered.
 */
class FirstRunTest {

    private val key = booleanPreferencesKey("language_asked")

    @Test
    fun `a fresh install has not been asked`() {
        val preferences = mutablePreferencesOf()
        assertFalse("a new player would have been skipped", preferences[key] == true)
    }

    @Test
    fun `once answered it stays answered`() {
        val preferences = mutablePreferencesOf()
        preferences[key] = true
        assertTrue("the answer did not stick", preferences[key] == true)
    }

    /**
     * The other half of the rule, written down where it can be read.
     *
     * A player who has a saved game, a finished game or a lesson in progress was using this
     * app before the screen existed. Asking them on an update is worse than never asking:
     * they answered by playing.
     */
    @Test
    fun `having played counts as an answer`() {
        assertTrue(answered(asked = false, hasGame = true, hasHistory = false, hasLessons = false))
        assertTrue(answered(asked = false, hasGame = false, hasHistory = true, hasLessons = false))
        assertTrue(answered(asked = false, hasGame = false, hasHistory = false, hasLessons = true))
        assertFalse(answered(asked = false, hasGame = false, hasHistory = false, hasLessons = false))
        assertTrue(answered(asked = true, hasGame = false, hasHistory = false, hasLessons = false))
    }

    private fun answered(asked: Boolean, hasGame: Boolean, hasHistory: Boolean, hasLessons: Boolean): Boolean =
        languageAnswered(asked = asked, hasGame = hasGame, hasHistory = hasHistory, hasLessons = hasLessons)
}
