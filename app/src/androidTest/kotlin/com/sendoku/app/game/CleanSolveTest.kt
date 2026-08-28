package com.sendoku.app.game

import com.sendoku.app.data.FinishedGame
import com.sendoku.app.data.SavedGame
import com.sendoku.app.data.Statistics
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * What makes a solve clean, and what quietly does not.
 *
 * Three facts and no fourth. The interesting one is notes: rubbing every note out again does
 * not make a solve noteless, because the notes were used, and a mark that can be erased into
 * existence is not worth having.
 */
class CleanSolveTest {

    private fun puzzle() = GradedGenerator(Dimensions.CLASSIC, Random(31)).let { maker ->
        var made = maker.next(Symmetry.ROTATIONAL)
        while (made == null || made.grade != Grade.STEADY) made = maker.next(Symmetry.ROTATIONAL)
        made
    }

    private fun solved(notes: Boolean = false, mistakes: Int = 0): GameState {
        var state = GameState.start(puzzle())
        val empties = state.cells.indices.filter { state.cells[it].isEmpty }
        if (notes) {
            state = state.setPencilMode(true).select(empties.first()).enter(1).setPencilMode(false)
        }
        repeat(mistakes) { index ->
            val cell = empties[index]
            val right = state.solution.atIndex(cell)
            state = state.select(cell).enter(if (right == 9) 1 else right + 1)
        }
        for (cell in empties) {
            state = state.select(cell).enter(state.solution.atIndex(cell))
        }
        return state
    }

    @Test
    fun aSolveWithNothingAskedForIsClean() {
        val finished = FinishedGame.of(solved(), finishedAt = 1L)
        assertTrue(finished.isClean)
    }

    @Test
    fun aNoteSpoilsIt() {
        val finished = FinishedGame.of(solved(notes = true), finishedAt = 1L)
        assertFalse(finished.isClean)
    }

    @Test
    fun rubbingTheNotesOutAgainDoesNotBringItBack() {
        var state = solved(notes = true)
        state = state.clearMarks()
        assertTrue("the note was forgotten", state.notesUsed)
        assertFalse(FinishedGame.of(state, finishedAt = 1L).isClean)
    }

    @Test
    fun aMistakeSpoilsIt() {
        val finished = FinishedGame.of(solved(mistakes = 1), finishedAt = 1L)
        assertFalse(finished.isClean)
    }

    @Test
    fun aGameThatWasLostIsNeverClean() {
        val state = GameState.start(puzzle())
        val finished = FinishedGame.of(state, finishedAt = 1L)
        assertFalse("an unsolved game counted as clean", finished.isClean)
    }

    @Test
    fun theFactSurvivesBeingWrittenDown() {
        val state = solved(notes = true)
        assertTrue(SavedGame.of(state).toState(GameSettings()).notesUsed)
    }

    @Test
    fun oldGamesAreNotClaimedAsClean() {
        // A row recorded before the column existed reads as notes used, because nobody knows
        // whether they were, and inventing the answer puts a mark on a history nobody earned.
        val old = FinishedGame(
            givens = "",
            grade = Grade.GENTLE,
            rating = 1.0,
            hardest = null,
            elapsed = kotlin.time.Duration.ZERO,
            hintsUsed = 0,
            mistakes = 0,
            solved = true,
            finishedAt = 1L,
            notesUsed = true,
        )
        assertFalse(old.isClean)
    }

    @Test
    fun theStatisticsCountThem() {
        val clean = FinishedGame.of(solved(), finishedAt = 1L)
        val noted = FinishedGame.of(solved(notes = true), finishedAt = 2L)
        val stats = Statistics.of(listOf(clean, noted))
        assertEquals(2, stats.totalSolved)
        assertEquals(1, stats.cleanSolves)
    }
}
