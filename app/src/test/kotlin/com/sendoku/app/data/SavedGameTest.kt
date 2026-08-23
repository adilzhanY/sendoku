package com.sendoku.app.data

import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameState
import com.sendoku.engine.Candidates
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.RatedPuzzle
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A saved game has to come back as the same game.
 *
 * This is the one place where getting it slightly wrong is invisible until a player closes
 * the app on a nearly finished Diabolical puzzle and reopens it to find their pencil marks
 * gone. So the round trip is checked whole, not field by field.
 */
class SavedGameTest {

    private val puzzle: RatedPuzzle by lazy {
        var made: RatedPuzzle? = null
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(9201))
        while (made == null) made = maker.next(Symmetry.ROTATIONAL)
        made
    }

    /** A game with a bit of everything in it: digits, marks, a clock, mistakes, a selection. */
    private fun playedGame(): GameState {
        var state = GameState.start(puzzle)
        val empties = state.cells.indices.filter { state.cells[it].isEmpty }

        state = state.select(empties[0]).enter(state.solution.atIndex(empties[0]))
        state = state.select(empties[1]).enter((1..9).first { it != state.solution.atIndex(empties[1]) })
        state = state.setPencilMode(true)
        state = state.select(empties[2]).enter(3).enter(7).enter(9)
        state = state.select(empties[3]).enter(1)
        state = state.setPencilMode(false)
        return state.select(empties[4]).tick(12.minutes + 34.seconds).countHint()
    }

    @Test
    fun `a game survives being written down and read back`() {
        val before = playedGame()
        val after = SavedGame.of(before).toState(before.settings)

        assertEquals(before.cells, after.cells)
        assertEquals(before.selected, after.selected)
        assertEquals(before.pencilMode, after.pencilMode)
        assertEquals(before.elapsed, after.elapsed)
        assertEquals(before.mistakes, after.mistakes)
        assertEquals(before.hintsUsed, after.hintsUsed)
        assertEquals(before.grade, after.grade)
        assertEquals(before.rating, after.rating, 1e-9)
        assertEquals(before.hardest, after.hardest)
        assertEquals(before.solution, after.solution)
    }

    @Test
    fun `the givens come back as givens and the entries as entries`() {
        val before = playedGame()
        val after = SavedGame.of(before).toState(before.settings)
        for (index in 0 until 81) {
            assertEquals("cell $index", before.cells[index].isGiven, after.cells[index].isGiven)
            assertEquals("cell $index", before.cells[index].digit, after.cells[index].digit)
        }
    }

    @Test
    fun `pencil marks come back exactly`() {
        val before = playedGame()
        val after = SavedGame.of(before).toState(before.settings)
        val marked = before.cells.indices.filter { before.cells[it].marks.isNotEmpty }
        assertTrue("the test game has no pencil marks in it", marked.isNotEmpty())
        for (index in marked) {
            assertEquals(before.cells[index].marks, after.cells[index].marks)
        }
    }

    @Test
    fun `a fresh game survives too`() {
        val before = GameState.start(puzzle)
        val after = SavedGame.of(before).toState(before.settings)
        assertEquals(before.cells, after.cells)
        assertEquals(null, after.selected)
    }

    @Test
    fun `the undo history is deliberately not kept`() {
        val before = playedGame()
        assertTrue(before.canUndo)
        val after = SavedGame.of(before).toState(before.settings)
        // Documented behaviour, not an oversight: the board comes back, the history does not.
        assertTrue(!after.canUndo)
    }

    @Test
    fun `settings are supplied on the way back in, not stored with the game`() {
        val before = playedGame()
        val strict = GameSettings(flagConflicts = false, mistakeLimit = 3)
        val after = SavedGame.of(before).toState(strict)
        assertEquals(strict, after.settings)
    }

    @Test
    fun `every mark set survives the encoding, including all nine and none`() {
        val all = (0..0x1FF).map { Candidates(it) }
        val encoded = SavedGame.encodeMarks(all)
        assertEquals(all, SavedGame.decodeMarks(encoded, all.size))
    }

    @Test
    fun `a mark string of the wrong length is refused rather than misread`() {
        try {
            SavedGame.decodeMarks("000", 81)
            error("a short mark string should have been refused")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("expected"))
        }
    }

    @Test
    fun `the puzzle itself is stored, not a pointer into the batch`() {
        // A batch index would be four bytes and would be wrong the first time the batch is
        // regenerated, silently handing the player a different puzzle.
        val saved = SavedGame.of(playedGame())
        assertEquals(81, saved.givens.length)
        assertEquals(81, saved.solution.length)
        assertNotEquals(saved.givens, saved.solution)
    }

    @Test
    fun `a row round trips through the database shapes`() {
        val saved = SavedGame.of(playedGame())
        assertEquals(saved, saved.toRow(savedAt = 12345L).toSaved())
    }

    @Test
    fun `a finished game records what it cost`() {
        val state = playedGame()
        val finished = FinishedGame.of(state, finishedAt = 999L)
        assertEquals(state.grade, finished.grade)
        assertEquals(state.elapsed, finished.elapsed)
        assertEquals(state.mistakes, finished.mistakes)
        assertEquals(state.hintsUsed, finished.hintsUsed)
        assertEquals(state.isSolved, finished.solved)
        assertEquals(999L, finished.finishedAt)
        assertEquals(finished, finished.toRow().toFinished())
    }
}
