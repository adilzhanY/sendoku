package com.sendoku.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.engine.Grade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

/**
 * The home page answers "what was I doing" first.
 *
 * The old page put eight level rows above the puzzle in progress, so the one thing a
 * returning player opened the app for was the last thing on the screen, and Resume was
 * written twice. What is checked here is the order and the count: the game in progress sits
 * above the levels, and there is exactly one way to resume it.
 */
class HomeLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    private val saved = InProgressSummary(
        grade = Grade.TRICKY,
        placed = 41,
        total = 81,
        elapsed = 18.minutes,
        givens = "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79",
        entries = ".".repeat(81),
    )

    private fun show(state: HomeState, onResume: () -> Unit = {}, onPlay: (Grade) -> Unit = {}) {
        compose.setContent {
            SendokuTheme {
                HomeScreen(state = state, onPlay = onPlay, onResume = onResume, onDaily = {})
            }
        }
    }

    @Test
    fun theGameInProgressIsAboveTheLevels() {
        show(HomeState(solvedByGrade = mapOf(Grade.GENTLE to 3), inProgress = saved))
        val card = compose.onNodeWithTag("home:continue").fetchSemanticsNode().positionInRoot.y
        val levels = compose.onNodeWithTag("home:levels:toggle").fetchSemanticsNode().positionInRoot.y
        assertTrue("the levels are above the puzzle in progress", card < levels)
    }

    @Test
    fun thereIsOneWayToResume() {
        var resumed = 0
        show(HomeState(solvedByGrade = mapOf(Grade.GENTLE to 3), inProgress = saved), onResume = { resumed++ })
        compose.onNodeWithTag("home:continue").performClick()
        compose.waitForIdle()
        assertEquals(1, resumed)
    }

    @Test
    fun withNothingToResumeTheLevelTileIsWhatIsLeft() {
        var started: Grade? = null
        show(HomeState(solvedByGrade = emptyMap(), inProgress = null), onPlay = { started = it })
        compose.onNodeWithTag("home:continue").assertDoesNotExist()
        compose.onNodeWithTag("home:new").performClick()
        compose.waitForIdle()
        assertEquals(Grade.GENTLE, started)
    }

    @Test
    fun theDailyTileSaysTheStreak() {
        show(
            HomeState(
                solvedByGrade = emptyMap(),
                inProgress = null,
                streak = 4,
                today = LocalDate.of(2026, 8, 27),
            ),
        )
        compose.onNodeWithText("4 days in a row").assertIsDisplayed()
    }

    /**
     * The chevron folds the levels between the two ways of showing them.
     *
     * Which one is on screen to begin with depends on how tall the screen is, so what is
     * checked is that the chevron changes it and that the level is still there either way. A
     * fold that loses a level is a fold that loses the game behind it.
     */
    @Test
    fun theChevronFoldsTheLevels() {
        show(HomeState(solvedByGrade = mapOf(Grade.GENTLE to 3), inProgress = null))
        val gate = "one clear digit at a time"
        val spelledOut = compose.onAllNodesWithText(gate).fetchSemanticsNodes().isNotEmpty()

        compose.onNodeWithTag("home:levels:toggle").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("home:grade:GENTLE").assertIsDisplayed()
        assertEquals(
            "the chevron did not change anything",
            !spelledOut,
            compose.onAllNodesWithText(gate).fetchSemanticsNodes().isNotEmpty(),
        )
    }
}
