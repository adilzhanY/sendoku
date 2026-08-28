package com.sendoku.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Solver
import com.sendoku.engine.technique.TechniqueSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Typing a puzzle in, and being told the truth about it.
 *
 * The three answers are three different situations and the screen must not blur them: a grid
 * with no answer has a typo in it, a grid with several is not a puzzle, and a grid with one
 * the ladder cannot reach is a perfectly good puzzle that this app cannot teach. The last of
 * those is the one worth being careful about, because pretending otherwise would mean hints
 * that cannot finish what they started.
 */
class EnterPuzzleTest {

    @get:Rule
    val compose = createComposeRule()

    private val classic = Dimensions.CLASSIC

    /** A real puzzle, taken off the shipped batch so the test is not judging a made up grid. */
    private val real = "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79"

    private var checked: Board? = null
    private var played: Board? = null

    private fun show(verdict: Verdict) {
        compose.setContent {
            SendokuTheme {
                EnterPuzzleScreen(
                    verdict = verdict,
                    onCheck = { checked = it },
                    onPlay = { played = it },
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun anEmptyGridSaysHowToStart() {
        show(Verdict.Unknown)
        compose.onNodeWithTag("enter:check").assertIsDisplayed()
        compose.onNodeWithTag("enter:clear").assertIsDisplayed()
        // Nothing to play until it has been checked.
        compose.onNodeWithTag("enter:play").assertDoesNotExist()
    }

    @Test
    fun aDigitLandsInTheSelectedCell() {
        show(Verdict.Unknown)
        compose.onNodeWithTag("game:cell:0").performClick()
        compose.onNodeWithTag("pad:5").performClick()
        compose.onNodeWithTag("enter:check").performClick()
        compose.waitForIdle()
        assertEquals(5, checked?.atIndex(0))
    }

    @Test
    fun aDigitTappedTwiceComesBackOut() {
        show(Verdict.Unknown)
        compose.onNodeWithTag("game:cell:0").performClick()
        compose.onNodeWithTag("pad:5").performClick()
        compose.onNodeWithTag("pad:5").performClick()
        compose.onNodeWithTag("enter:check").performClick()
        compose.waitForIdle()
        assertEquals(Board.EMPTY, checked?.atIndex(0))
    }

    @Test
    fun aGridWithNoAnswerCannotBePlayed() {
        show(Verdict.Impossible)
        compose.onNodeWithTag("enter:play").assertDoesNotExist()
    }

    @Test
    fun aGridTheLadderCannotFinishCannotBePlayedEither() {
        // It is a perfectly good sudoku. It is not one this app can explain, and offering it
        // would mean hints that give up halfway.
        show(Verdict.BeyondTheLadder)
        compose.onNodeWithTag("enter:play").assertDoesNotExist()
    }

    @Test
    fun aGridWithSeveralAnswersIsOfferedAnyway() {
        // With the sentence above saying what it is. A puzzle typed in wrong is still worth
        // looking at, and refusing to open it helps nobody.
        show(Verdict.Ambiguous)
        compose.onNodeWithTag("enter:play").assertIsDisplayed()
    }

    @Test
    fun aRealPuzzleCanBePlayed() {
        show(Verdict.Ready(Grade.TRICKY, 3.4))
        compose.onNodeWithTag("enter:play").performClick()
        compose.waitForIdle()
        assertEquals(Board.EMPTY, played?.atIndex(0))
    }

    @Test
    fun theVerdictIsSaidInWords() {
        show(Verdict.Ready(Grade.TRICKY, 3.42))
        compose.onNodeWithText("One answer. This is a Hard puzzle, rated 3.4.").assertIsDisplayed()
    }

    @Test
    fun theEngineAgreesWithWhatTheScreenPromises() {
        // The screen only ever reports what these two say, so this is the test that the
        // three verdicts mean what they claim.
        val board = Board.parse(classic, real)
        assertEquals(1, Solver(classic).countSolutions(board, limit = 2))
        assertEquals(true, TechniqueSolver().solve(board.copy()).isSolved)

        val empty = Board(classic)
        assertEquals(2, Solver(classic).countSolutions(empty, limit = 2))

        val broken = Board.parse(classic, real).also { it.setAtIndex(2, 5) }
        assertEquals(0, Solver(classic).countSolutions(broken, limit = 2))
        assertNull("a broken grid was solved anyway", Solver(classic).solve(broken))
    }
}
