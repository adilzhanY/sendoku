package com.sendoku.app.game

import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The four pieces of help a player can switch on, and what each of them may not do.
 *
 * They are conveniences rather than hints, and the line between the two is the whole point:
 * every one of these says something the board is already showing. None may reach for the
 * solution except auto check, which exists to, and which is off unless asked for.
 */
class BoardHelpTest {

    private val puzzle = GradedGenerator(Dimensions.CLASSIC, Random(11)).let { maker ->
        generateSequence { maker.next(Symmetry.ROTATIONAL) }.first()
    }

    private fun game(settings: GameSettings = GameSettings()) = GameState.start(puzzle, settings)

    @Test
    fun `filling every note writes what each cell could take and nothing else`() {
        val filled = game().fillAllMarks()

        for (cell in filled.cells.indices) {
            if (!filled.cells[cell].isEmpty) {
                assertTrue("a filled cell was given notes", filled.cells[cell].marks.isEmpty)
                continue
            }
            assertEquals("cell $cell", filled.candidatesAt(cell), filled.cells[cell].marks)
            assertTrue(
                "cell $cell was pencilled with a digit that cannot go there",
                filled.solution.atIndex(cell) in filled.cells[cell].marks,
            )
        }
    }

    @Test
    fun `filling every note is one move, so one undo puts it back`() {
        val before = game()
        val after = before.fillAllMarks()
        assertTrue(after.cells != before.cells)
        assertEquals(before.cells, after.undo().cells)
    }

    @Test
    fun `auto notes keeps the board pencilled as digits go in, and stays one undo deep`() {
        var state = game(GameSettings(autoNotes = true)).fillAllMarks()
        val empty = state.cells.indices.first { state.cells[it].isEmpty }
        val digit = state.solution.atIndex(empty)

        val before = state.cells
        state = state.select(empty).enter(digit)

        val geometry = com.sendoku.engine.Geometry.of(Dimensions.CLASSIC)
        for (peer in geometry.peersOf(empty)) {
            assertTrue(
                "a peer kept a note for the digit just placed",
                digit !in state.cells[peer].marks,
            )
        }
        for (cell in state.cells.indices) {
            if (!state.cells[cell].isEmpty) continue
            assertEquals("cell $cell drifted from the truth", state.candidatesAt(cell), state.cells[cell].marks)
        }
        assertEquals("refilling the notes cost a second undo", before, state.undo().cells)
    }

    @Test
    fun `auto check says nothing until it is asked to`() {
        val wrongDigit = (1..9).first { it != puzzle.puzzle.solution.atIndex(firstEmpty()) }

        // With the limit off as well. While a limit is running the marking is not optional,
        // because a mistake that is charged for has to be one the player can see.
        val paper = GameSettings(mistakeLimit = null, autoCheck = false)
        val quiet = game(paper).select(firstEmpty()).enter(wrongDigit)
        assertTrue("a wrong digit was flagged with the setting off", quiet.flaggedWrong.isEmpty())

        val loud = game(GameSettings(autoCheck = true)).select(firstEmpty()).enter(wrongDigit)
        assertEquals(setOf(firstEmpty()), loud.flaggedWrong)
    }

    @Test
    fun `auto check never flags a digit that is right`() {
        val cell = firstEmpty()
        val state = game(GameSettings(autoCheck = true)).select(cell).enter(puzzle.puzzle.solution.atIndex(cell))
        assertTrue(state.flaggedWrong.isEmpty())
    }

    @Test
    fun `the homes of a digit are the empty cells that could still take it`() {
        val state = game(GameSettings(highlightHomes = true))
        val placed = state.cells.indices.first { !state.cells[it].isEmpty }
        val digit = state.cells[placed].digit

        val homes = state.select(placed).highlightedHomes

        assertTrue(homes.isNotEmpty())
        for (cell in homes) {
            assertTrue("cell $cell is not empty", state.cells[cell].isEmpty)
            assertTrue("cell $cell cannot take the digit", digit in state.candidatesAt(cell))
        }
        for (cell in state.cells.indices) {
            if (cell in homes || !state.cells[cell].isEmpty) continue
            assertFalse("cell $cell could take the digit and was not lit", digit in state.candidatesAt(cell))
        }
    }

    @Test
    fun `the homes stay dark when the setting is off`() {
        val state = game()
        val placed = state.cells.indices.first { !state.cells[it].isEmpty }
        assertTrue(state.select(placed).highlightedHomes.isEmpty())
    }

    private fun firstEmpty(): Int = puzzle.puzzle.givens.let { givens ->
        (0 until 81).first { givens.atIndex(it) == Board.EMPTY }
    }
}
