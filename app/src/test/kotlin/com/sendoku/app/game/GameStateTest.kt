package com.sendoku.app.game

import com.sendoku.engine.Board
import com.sendoku.engine.Candidates
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Geometry
import com.sendoku.engine.Grade
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.RatedPuzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class GameStateTest {

    private val classic = Dimensions.CLASSIC
    private val geometry = Geometry.of(classic)

    private val puzzle: RatedPuzzle by lazy {
        var made: RatedPuzzle? = null
        val maker = GradedGenerator(classic, Random(9001))
        while (made == null) made = maker.next(Symmetry.ROTATIONAL)
        made
    }

    private fun game() = GameState.start(puzzle)

    /** The first empty cell, which is where most of these tests do their work. */
    private fun GameState.firstEmpty(): Int = cells.indices.first { cells[it].isEmpty }

    private fun GameState.firstGiven(): Int = cells.indices.first { cells[it].isGiven }

    private fun GameState.correctFor(cell: Int) = solution.atIndex(cell)

    private fun GameState.wrongFor(cell: Int) = (1..9).first { it != solution.atIndex(cell) }

    @Test
    fun `a new game keeps the givens and nothing else`() {
        val state = game()
        assertEquals(81, state.cells.size)
        assertEquals(puzzle.clueCount, state.cells.count { it.isGiven })
        assertEquals(puzzle.grade, state.grade)
        for (index in 0 until 81) {
            val cell = state.cells[index]
            assertEquals(puzzle.puzzle.givens.atIndex(index), cell.digit)
            assertEquals(cell.digit != Board.EMPTY, cell.isGiven)
            assertTrue(cell.marks.isEmpty)
        }
        assertNull(state.selected)
        assertFalse(state.isSolved)
        assertEquals(0, state.mistakes)
    }

    @Test
    fun `selecting a cell and entering a digit places it`() {
        val start = game()
        val at = start.firstEmpty()
        val after = start.select(at).enter(start.correctFor(at))
        assertEquals(start.correctFor(at), after.cells[at].digit)
        assertFalse(after.cells[at].isGiven)
    }

    @Test
    fun `entering with nothing selected does nothing at all`() {
        val start = game()
        assertSame(start, start.enter(5))
        assertSame(start, start.erase())
    }

    @Test
    fun `a given can be selected but never written over`() {
        val start = game()
        val at = start.firstGiven()
        val selected = start.select(at)
        assertEquals(at, selected.selected)
        assertSame(selected, selected.enter(1))
        assertSame(selected, selected.erase())
    }

    @Test
    fun `entering the same digit twice rubs it out`() {
        val start = game()
        val at = start.firstEmpty()
        val digit = start.correctFor(at)
        val placed = start.select(at).enter(digit)
        val cleared = placed.enter(digit)
        assertTrue(cleared.cells[at].isEmpty)
    }

    @Test
    fun `a wrong digit counts as a mistake and a right one does not`() {
        val start = game()
        val at = start.firstEmpty()
        assertEquals(0, start.select(at).enter(start.correctFor(at)).mistakes)
        assertEquals(1, start.select(at).enter(start.wrongFor(at)).mistakes)
    }

    @Test
    fun `mistakes add up and the limit ends the game`() {
        var state = game().withSettings(GameSettings(mistakeLimit = 3))
        val empties = state.cells.indices.filter { state.cells[it].isEmpty }.take(3)
        for (at in empties) {
            state = state.select(at).enter(state.wrongFor(at))
        }
        assertEquals(3, state.mistakes)
        assertTrue(state.isFailed)
        assertTrue(state.isOver)

        // A finished game accepts nothing more.
        val locked = state.select(state.firstEmpty())
        assertSame(locked, locked.enter(1))
    }

    @Test
    fun `no limit means the game never ends on mistakes`() {
        var state = game()
        for (at in state.cells.indices.filter { state.cells[it].isEmpty }.take(12)) {
            state = state.select(at).enter(state.wrongFor(at))
        }
        assertEquals(12, state.mistakes)
        assertFalse(state.isFailed)
    }

    @Test
    fun `a mistake limit of zero is refused rather than starting a lost game`() {
        try {
            GameSettings(mistakeLimit = 0)
            error("a limit of zero should have been refused")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("before it starts"))
        }
    }

    @Test
    fun `pencil marks toggle on and off`() {
        val start = game()
        val at = start.firstEmpty()
        val marked = start.select(at).setPencilMode(true).enter(4).enter(7)
        assertEquals(Candidates.of(4, 7), marked.cells[at].marks)
        assertTrue(marked.cells[at].isEmpty)

        val unmarked = marked.enter(4)
        assertEquals(Candidates.of(7), unmarked.cells[at].marks)
    }

    @Test
    fun `pencil mode refuses to mark a cell that already holds a digit`() {
        val start = game()
        val at = start.firstEmpty()
        val filled = start.select(at).enter(start.correctFor(at)).setPencilMode(true)
        assertSame(filled, filled.enter(3))
    }

    @Test
    fun `the pencil toggle flips`() {
        val start = game()
        assertFalse(start.pencilMode)
        assertTrue(start.togglePencilMode().pencilMode)
        assertFalse(start.togglePencilMode().togglePencilMode().pencilMode)
    }

    @Test
    fun `placing a digit rubs it out of the pencil marks that can see it`() {
        var state = game()
        val at = state.firstEmpty()
        val digit = state.correctFor(at)
        val peers = geometry.peersOf(at).filter { state.cells[it].isEmpty }

        state = state.setPencilMode(true)
        for (peer in peers) state = state.select(peer).enter(digit)
        for (peer in peers) assertTrue(digit in state.cells[peer].marks)

        state = state.setPencilMode(false).select(at).enter(digit)
        for (peer in peers) {
            assertFalse("cell $peer kept a mark it cannot have", digit in state.cells[peer].marks)
        }
    }

    @Test
    fun `turning off auto clearing leaves the marks alone`() {
        var state = game().withSettings(GameSettings(autoClearMarks = false))
        val at = state.firstEmpty()
        val digit = state.correctFor(at)
        val peer = geometry.peersOf(at).first { state.cells[it].isEmpty }

        state = state.setPencilMode(true).select(peer).enter(digit)
        state = state.setPencilMode(false).select(at).enter(digit)
        assertTrue(digit in state.cells[peer].marks)
    }

    @Test
    fun `erase clears the digit and every mark`() {
        val start = game()
        val at = start.firstEmpty()
        val marked = start.select(at).setPencilMode(true).enter(2).enter(5)
        val erased = marked.erase()
        assertTrue(erased.cells[at].isEmpty)
        assertTrue(erased.cells[at].marks.isEmpty)
    }

    @Test
    fun `erasing an empty cell is not a move`() {
        val start = game()
        val selected = start.select(start.firstEmpty())
        assertSame(selected, selected.erase())
        assertFalse(selected.canUndo)
    }

    @Test
    fun `undo puts everything back, including the marks a placement rubbed out`() {
        var state = game()
        val at = state.firstEmpty()
        val digit = state.correctFor(at)
        val peers = geometry.peersOf(at).filter { state.cells[it].isEmpty }

        state = state.setPencilMode(true)
        for (peer in peers) state = state.select(peer).enter(digit)
        val beforePlacing = state.setPencilMode(false)

        val afterPlacing = beforePlacing.select(at).enter(digit)
        val undone = afterPlacing.undo()

        assertEquals(beforePlacing.cells, undone.cells)
        assertTrue(undone.cells[at].isEmpty)
        for (peer in peers) assertTrue(digit in undone.cells[peer].marks)
    }

    @Test
    fun `redo puts it back again`() {
        val start = game()
        val at = start.firstEmpty()
        val digit = start.correctFor(at)
        val placed = start.select(at).enter(digit)
        val redone = placed.undo().redo()
        assertEquals(placed.cells, redone.cells)
        assertFalse(redone.canRedo)
    }

    @Test
    fun `undo and redo walk a whole run of moves`() {
        var state = game()
        val empties = state.cells.indices.filter { state.cells[it].isEmpty }.take(6)
        val snapshots = ArrayList<List<Cell>>()
        snapshots.add(state.cells)
        for (at in empties) {
            state = state.select(at).enter(state.correctFor(at))
            snapshots.add(state.cells)
        }
        for (step in empties.indices.reversed()) {
            state = state.undo()
            assertEquals("after undoing to step $step", snapshots[step], state.cells)
        }
        assertFalse(state.canUndo)
        for (step in empties.indices) {
            state = state.redo()
            assertEquals("after redoing to step $step", snapshots[step + 1], state.cells)
        }
        assertFalse(state.canRedo)
    }

    @Test
    fun `doing something new throws away the redo branch`() {
        val start = game()
        val empties = start.cells.indices.filter { start.cells[it].isEmpty }.take(2)
        val state = start
            .select(empties[0]).enter(start.correctFor(empties[0]))
            .undo()
        assertTrue(state.canRedo)

        val diverged = state.select(empties[1]).enter(state.correctFor(empties[1]))
        assertFalse(diverged.canRedo)
    }

    @Test
    fun `undo with nothing to undo is not an error`() {
        val start = game()
        assertSame(start, start.undo())
        assertSame(start, start.redo())
    }

    @Test
    fun `a repeated digit is a conflict and shows on both cells`() {
        val start = game()
        val at = start.firstEmpty()
        val peer = geometry.peersOf(at).first { start.cells[it].isEmpty }
        val digit = start.correctFor(at)

        val clashing = start.select(at).enter(digit).select(peer).enter(digit)
        assertTrue(at in clashing.conflicts)
        assertTrue(peer in clashing.conflicts)
    }

    @Test
    fun `a wrong digit that repeats nothing is a mistake but not a conflict`() {
        val start = game()
        val at = start.firstEmpty()
        // Find a digit that is wrong here and appears nowhere this cell can see.
        val seen = geometry.peersOf(at).map { start.cells[it].digit }.toSet()
        val quietlyWrong = (1..9).firstOrNull { it != start.correctFor(at) && it !in seen }
        if (quietlyWrong == null) return

        val state = start.select(at).enter(quietlyWrong)
        assertEquals(1, state.mistakes)
        assertFalse(at in state.conflicts)
    }

    @Test
    fun `turning conflicts off hides them without changing the board`() {
        val start = game()
        val at = start.firstEmpty()
        val peer = geometry.peersOf(at).first { start.cells[it].isEmpty }
        val digit = start.correctFor(at)
        val clashing = start.select(at).enter(digit).select(peer).enter(digit)

        val quiet = clashing.withSettings(GameSettings(flagConflicts = false))
        assertTrue(quiet.conflicts.isEmpty())
        assertEquals(clashing.cells, quiet.cells)
    }

    @Test
    fun `remaining counts down and reaches zero`() {
        var state = game()
        val digit = 1
        val before = state.remaining(digit)
        assertEquals(9 - state.cells.count { it.digit == digit }, before)

        val at = state.cells.indices.first { state.cells[it].isEmpty && state.solution.atIndex(it) == digit }
        state = state.select(at).enter(digit)
        assertEquals(before - 1, state.remaining(digit))
        assertFalse(state.isExhausted(digit))
    }

    @Test
    fun `a digit that is fully placed is exhausted`() {
        var state = game()
        for (at in 0 until 81) {
            if (state.cells[at].isEmpty && state.solution.atIndex(at) == 7) {
                state = state.select(at).enter(7)
            }
        }
        assertEquals(0, state.remaining(7))
        assertTrue(state.isExhausted(7))
        assertFalse(state.isExhausted(8))
    }

    @Test
    fun `peers and matches light up only when the settings allow`() {
        val start = game()
        val given = start.firstGiven()
        val lit = start.select(given)

        assertEquals(20, lit.highlightedPeers.size)
        assertTrue(lit.highlightedMatches.all { lit.cells[it].digit == lit.cells[given].digit })
        assertFalse(given in lit.highlightedMatches)

        val bare = lit.withSettings(GameSettings(highlightPeers = false, highlightSameDigit = false))
        assertTrue(bare.highlightedPeers.isEmpty())
        assertTrue(bare.highlightedMatches.isEmpty())
    }

    @Test
    fun `nothing is highlighted when nothing is selected`() {
        val start = game()
        assertTrue(start.highlightedPeers.isEmpty())
        assertTrue(start.highlightedMatches.isEmpty())
    }

    @Test
    fun `the clock runs, pauses and resumes`() {
        val start = game()
        val ticked = start.tick(5.seconds).tick(3.seconds)
        assertEquals(8.seconds, ticked.elapsed)

        val paused = ticked.pause()
        assertFalse(paused.isRunning)
        assertEquals(8.seconds, paused.tick(10.seconds).elapsed)

        val resumed = paused.resume()
        assertTrue(resumed.isRunning)
        assertEquals(9.seconds, resumed.tick(1.seconds).elapsed)
    }

    @Test
    fun `the clock stops for good once the game is over`() {
        var state = game().withSettings(GameSettings(mistakeLimit = 1))
        val at = state.firstEmpty()
        state = state.select(at).enter(state.wrongFor(at))
        assertTrue(state.isOver)
        assertEquals(state.elapsed, state.tick(30.seconds).elapsed)
        assertFalse(state.resume().isRunning)
    }

    @Test
    fun `filling in the solution finishes the game`() {
        var state = game()
        for (at in 0 until 81) {
            if (state.cells[at].isEmpty) state = state.select(at).enter(state.solution.atIndex(at))
        }
        assertTrue(state.isSolved)
        assertTrue(state.isOver)
        assertEquals(0, state.mistakes)
        assertTrue(state.conflicts.isEmpty())
        assertEquals(state.solution, state.toBoard())
    }

    @Test
    fun `hints are counted`() {
        assertEquals(2, game().countHint().countHint().hintsUsed)
    }

    @Test
    fun `selecting a cell off the grid is refused`() {
        val start = game()
        for (bad in listOf(-1, 81, 200)) {
            try {
                start.select(bad)
                error("cell $bad should have been refused")
            } catch (expected: IllegalArgumentException) {
                assertNotNull(expected.message)
            }
        }
        assertNull(start.select(null).selected)
    }

    @Test
    fun `entering something that is not a digit is refused`() {
        val start = game().select(0)
        for (bad in listOf(0, 10, -3)) {
            try {
                start.enter(bad)
                error("$bad should have been refused")
            } catch (expected: IllegalArgumentException) {
                assertNotNull(expected.message)
            }
        }
    }

    @Test
    fun `a game never mutates the state it came from`() {
        val start = game()
        val at = start.firstEmpty()
        val cellsBefore = start.cells.toList()
        start.select(at).enter(start.correctFor(at)).undo().redo().erase()
        assertEquals(cellsBefore, start.cells)
        assertNull(start.selected)
        assertEquals(0, start.mistakes)
    }

    @Test
    fun `the hint can take a wrong digit back off the board`() {
        var state = GameState.start(puzzle)
        val empty = state.cells.indices.first { state.cells[it].isEmpty }
        val right = state.solution.atIndex(empty)
        val wrong = (1..9).first { it != right }

        state = state.select(empty).enter(wrong)
        assertTrue(state.cells[empty].digit == wrong)

        state = state.eraseAll(setOf(empty))
        assertTrue(state.cells[empty].isEmpty)
        // One move, so one undo puts it back.
        assertTrue(state.undo().cells[empty].digit == wrong)
    }

    @Test
    fun `taking digits off never touches a given`() {
        val state = GameState.start(puzzle)
        val given = state.cells.indices.first { state.cells[it].isGiven }
        val digit = state.cells[given].digit

        assertEquals(state, state.eraseAll(setOf(given)))
        assertTrue(state.eraseAll(setOf(given)).cells[given].digit == digit)
    }

    @Test
    fun `erase is only offered when it would do something`() {
        var state = GameState.start(puzzle)
        assertTrue(!state.canErase)

        val given = state.cells.indices.first { state.cells[it].isGiven }
        assertTrue(!state.select(given).canErase)

        val empty = state.cells.indices.first { state.cells[it].isEmpty }
        state = state.select(empty)
        assertTrue(!state.canErase)

        state = state.enter(state.solution.atIndex(empty))
        assertTrue(state.canErase)
    }
}
