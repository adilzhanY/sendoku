package com.sendoku.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.sendoku.app.data.FinishedGame
import com.sendoku.app.data.Statistics
import com.sendoku.app.learn.CourseProgress
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.engine.Grade
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/**
 * The You page as a record rather than as a row of numbers.
 *
 * The figures used to be five on one line, which is a fifth of a phone each, and the one
 * reading "12/42" did not fit in a fifth, so it shrank and dragged its own baseline out of
 * line with the other four. What is checked here is what replaced it: the hardest puzzle
 * beaten leads the page, it is a puzzle that was actually beaten, every level has a row
 * whether it has been played or not, and the four doors are still one tap away.
 */
class AccountRecordTest {

    @get:Rule
    val compose = createComposeRule()

    private fun game(grade: Grade, rating: Double, solved: Boolean, at: Long) = FinishedGame(
        givens = "",
        grade = grade,
        rating = rating,
        hardest = null,
        elapsed = 12.minutes,
        hintsUsed = 0,
        mistakes = 0,
        solved = solved,
        finishedAt = at,
    )

    private val day = 86_400_000L
    private val now = System.currentTimeMillis()

    private val history = listOf(
        game(Grade.GENTLE, 1.2, solved = true, at = now - day * 3),
        game(Grade.TRICKY, 3.4, solved = true, at = now - day * 2),
        // Harder than anything won, and lost. It must not be the record.
        game(Grade.BEYOND, 8.8, solved = false, at = now - day),
    )

    private fun show(history: List<FinishedGame>, onStats: () -> Unit = {}, onSettings: () -> Unit = {}) {
        compose.setContent {
            SendokuTheme {
                AccountScreen(
                    statistics = Statistics.of(history),
                    course = CourseProgress(),
                    history = history,
                    onStats = onStats,
                    onHistory = {},
                    onSettings = onSettings,
                    onAbout = {},
                )
            }
        }
    }

    @Test
    fun theHardestPuzzleBeatenLeadsThePage() {
        show(history)
        compose.onNodeWithTag("account:hardest").assertIsDisplayed()
        compose.onNodeWithText("3.4").assertIsDisplayed()
    }

    @Test
    fun aPuzzleThatBeatYouIsNotYourRecord() {
        show(history)
        compose.onNodeWithText("8.8").assertDoesNotExist()
    }

    @Test
    fun everyLevelHasARowEvenTheOnesNeverPlayed() {
        // The empty rows are the interesting half in this app: they are how far there is
        // left to go. A chart that hides them cannot say that.
        show(history)
        for (grade in Grade.entries) {
            compose.onNodeWithTag("account:beaten:${grade.name}").performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun withNothingPlayedThereIsAnInvitationAndNoCharts() {
        show(emptyList())
        compose.onNodeWithTag("account:empty").assertIsDisplayed()
        compose.onNodeWithTag("account:hardest").assertDoesNotExist()
    }

    @Test
    fun theDoorsAreStillOneTap() {
        var stats = 0
        var settings = 0
        show(history, onStats = { stats++ }, onSettings = { settings++ })
        compose.onNodeWithTag("account:stats").performScrollTo().performClick()
        compose.onNodeWithTag("account:settings").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(1, stats)
        assertEquals(1, settings)
    }

    @Test
    fun theDoorsAreThereWithNothingPlayedToo() {
        show(emptyList())
        compose.onNodeWithTag("account:settings").assertIsDisplayed()
    }
}
