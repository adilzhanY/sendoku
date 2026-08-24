package com.sendoku.app.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.engine.Grade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * The lock, from the outside.
 *
 * [LevelsOpenTest] settles which levels are open. What this settles is that a shut one
 * really cannot be started, because a row that looks locked and still deals a puzzle when
 * tapped is worse than no lock at all.
 */
class LevelLockTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(state: HomeState, onPlay: (Grade) -> Unit) {
        compose.setContent {
            SendokuTheme {
                HomeScreen(state = state, onPlay = onPlay, onResume = {}, onDaily = {})
            }
        }
    }

    @Test
    fun aLockedLevelCannotBeStarted() {
        var started: Grade? = null
        show(HomeState(solvedByGrade = emptyMap(), inProgress = null)) { started = it }

        compose.onNodeWithTag("home:grade:STEADY").performClick()
        compose.waitForIdle()

        assertNull("a locked level dealt a puzzle", started)
    }

    @Test
    fun theOpenLevelStarts() {
        var started: Grade? = null
        show(HomeState(solvedByGrade = emptyMap(), inProgress = null)) { started = it }

        compose.onNodeWithTag("home:grade:GENTLE").performClick()
        compose.waitForIdle()

        assertEquals(Grade.GENTLE, started)
    }

    @Test
    fun winningTheEasiestOpensTheNextOne() {
        var started: Grade? = null
        show(HomeState(solvedByGrade = mapOf(Grade.GENTLE to 1), inProgress = null)) { started = it }

        compose.onNodeWithTag("home:grade:STEADY").performClick()
        compose.waitForIdle()

        assertEquals(Grade.STEADY, started)
    }
}
