package com.sendoku.app.ui

import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameState
import androidx.compose.ui.input.key.Key
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Geometry
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.RatedPuzzle
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The screen sends events and the state answers them.
 *
 * Keeping the mapping in one function means the screen holds no rules of its own, and this
 * is the test that says so: every event the UI can raise goes somewhere, and each one does
 * the thing its name promises.
 */
class GameEventTest {

    private val puzzle: RatedPuzzle by lazy {
        var made: RatedPuzzle? = null
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(9101))
        while (made == null) made = maker.next(Symmetry.ROTATIONAL)
        made
    }

    private fun game() = GameState.start(puzzle)

    private fun GameState.firstEmpty() = cells.indices.first { cells[it].isEmpty }

    @Test
    fun `selecting and entering go through the reducer`() {
        val start = game()
        val at = start.firstEmpty()
        val digit = start.solution.atIndex(at)

        val after = start
            .reduce(GameEvent.Select(at))
            .reduce(GameEvent.Digit(digit))

        assertEquals(at, after.selected)
        assertEquals(digit, after.cells[at].digit)
    }

    @Test
    fun `erase, undo and redo go through the reducer`() {
        val start = game()
        val at = start.firstEmpty()
        val placed = start.reduce(GameEvent.Select(at)).reduce(GameEvent.Digit(start.solution.atIndex(at)))

        val erased = placed.reduce(GameEvent.Erase)
        assertTrue(erased.cells[at].isEmpty)

        val undone = erased.reduce(GameEvent.Undo)
        assertEquals(placed.cells, undone.cells)

        val redone = undone.reduce(GameEvent.Redo)
        assertEquals(erased.cells, redone.cells)
    }

    @Test
    fun `the pencil toggle and the hint counter go through the reducer`() {
        val start = game()
        assertTrue(start.reduce(GameEvent.TogglePencil).pencilMode)
        assertEquals(1, start.reduce(GameEvent.Hint).hintsUsed)
    }

    @Test
    fun `pause, resume and tick go through the reducer`() {
        val start = game()
        val ticked = start.reduce(GameEvent.Tick(7.seconds))
        assertEquals(7.seconds, ticked.elapsed)

        val paused = ticked.reduce(GameEvent.Pause)
        assertFalse(paused.isRunning)
        assertEquals(7.seconds, paused.reduce(GameEvent.Tick(5.seconds)).elapsed)

        val resumed = paused.reduce(GameEvent.Resume)
        assertTrue(resumed.isRunning)
    }

    @Test
    fun `every event is handled, so none can be quietly forgotten`() {
        // A `when` over a sealed interface will not compile if an event is missed, and this
        // is the belt to that brace: each one must actually be reachable and do something.
        val start = game().reduce(GameEvent.Select(game().firstEmpty()))
        val events = listOf(
            GameEvent.Select(0),
            GameEvent.Digit(1),
            GameEvent.Tick(1.seconds),
            GameEvent.Erase,
            GameEvent.Undo,
            GameEvent.Redo,
            GameEvent.TogglePencil,
            GameEvent.Hint,
            GameEvent.Pause,
            GameEvent.Resume,
        )
        for (event in events) {
            // Nothing throws, whatever order they arrive in.
            start.reduce(event)
        }
    }

    @Test
    fun `the clock reads the way a person would say it`() {
        assertEquals("0:00", 0.seconds.clock())
        assertEquals("0:07", 7.seconds.clock())
        assertEquals("1:00", 1.minutes.clock())
        assertEquals("24:08", (24.minutes + 8.seconds).clock())
        assertEquals("100:00", 100.minutes.clock())
    }

    @Test
    fun `a mistake limit is respected through the reducer too`() {
        var state = game().withSettings(GameSettings(mistakeLimit = 1))
        val at = state.firstEmpty()
        val wrong = (1..9).first { it != state.solution.atIndex(at) }

        state = state.reduce(GameEvent.Select(at)).reduce(GameEvent.Digit(wrong))
        assertTrue(state.isOver)
        assertFalse(state.isRunning)
    }

    @Test
    fun `arrow keys move the selection and wrap at the edges`() {
        val start = game().reduce(GameEvent.Select(0))
        assertEquals(1, start.reduce(GameEvent.Nudge(0, 1)).selected)
        assertEquals(9, start.reduce(GameEvent.Nudge(1, 0)).selected)
        // Left from the first column lands on the last, rather than doing nothing.
        assertEquals(8, start.reduce(GameEvent.Nudge(0, -1)).selected)
        assertEquals(72, start.reduce(GameEvent.Nudge(-1, 0)).selected)
    }

    @Test
    fun `moving with nothing selected starts at the top left`() {
        assertEquals(0, game().reduce(GameEvent.Nudge(0, 1)).selected)
    }

    @Test
    fun `filling the candidates pencils in exactly what the cell can take`() {
        val start = game()
        val at = start.firstEmpty()
        val filled = start.reduce(GameEvent.Select(at)).reduce(GameEvent.FillMarks)

        val possible = start.candidatesAt(at)
        assertEquals(possible, filled.cells[at].marks)
        // The right answer is always among them, since it is a legal puzzle.
        assertTrue(start.solution.atIndex(at) in filled.cells[at].marks)
        // And nothing a peer already holds is offered.
        for (peer in Geometry.of(Dimensions.CLASSIC).peersOf(at)) {
            val digit = start.cells[peer].digit
            if (digit != 0) assertFalse(digit in filled.cells[at].marks)
        }
    }

    @Test
    fun `filling candidates is a single undoable move`() {
        val start = game()
        val at = start.firstEmpty()
        val filled = start.reduce(GameEvent.Select(at)).reduce(GameEvent.FillMarks)
        assertTrue(filled.canUndo)
        assertTrue(filled.reduce(GameEvent.Undo).cells[at].marks.isEmpty)
    }

    @Test
    fun `clearing the marks leaves any digit alone`() {
        val start = game()
        val at = start.firstEmpty()
        val marked = start.reduce(GameEvent.Select(at)).reduce(GameEvent.FillMarks)
        val cleared = marked.reduce(GameEvent.ClearMarks)
        assertTrue(cleared.cells[at].marks.isEmpty)
        assertTrue(cleared.cells[at].isEmpty)
    }

    @Test
    fun `filling candidates does nothing to a given or a filled cell`() {
        val start = game()
        val given = start.cells.indices.first { start.cells[it].isGiven }
        val onGiven = start.reduce(GameEvent.Select(given))
        assertSame(onGiven, onGiven.reduce(GameEvent.FillMarks))

        val at = start.firstEmpty()
        val filled = start.reduce(GameEvent.Select(at)).reduce(GameEvent.Digit(start.solution.atIndex(at)))
        assertSame(filled, filled.reduce(GameEvent.FillMarks))
    }

    @Test
    fun `the keyboard maps to the moves a player expects`() {
        val seen = ArrayList<GameEvent>()
        val sink: (GameEvent) -> Unit = { seen.add(it) }

        assertTrue(handleKey(Key.Five, true, sink))
        assertTrue(handleKey(Key.NumPad7, true, sink))
        assertTrue(handleKey(Key.DirectionRight, true, sink))
        assertTrue(handleKey(Key.Backspace, true, sink))
        assertTrue(handleKey(Key.Spacebar, true, sink))
        assertTrue(handleKey(Key.Z, true, sink))
        assertTrue(handleKey(Key.H, true, sink))

        assertEquals(
            listOf(
                GameEvent.Digit(5),
                GameEvent.Digit(7),
                GameEvent.Nudge(0, 1),
                GameEvent.Erase,
                GameEvent.TogglePencil,
                GameEvent.Undo,
                GameEvent.Hint,
            ),
            seen,
        )
    }

    @Test
    fun `a key the game does not use is left for everything else`() {
        val seen = ArrayList<GameEvent>()
        assertFalse(handleKey(Key.Q, true, { seen.add(it) }))
        assertFalse(handleKey(Key.Enter, true, { seen.add(it) }))
        // Key up is ignored, or every press would count twice.
        assertFalse(handleKey(Key.Five, false, { seen.add(it) }))
        assertTrue(seen.isEmpty())
    }

    @Test
    fun `progress is what decides whether leaving needs a question`() {
        val start = game()
        assertFalse(start.hasProgress)
        assertFalse(start.reduce(GameEvent.Select(start.firstEmpty())).hasProgress)

        val played = start
            .reduce(GameEvent.Select(start.firstEmpty()))
            .reduce(GameEvent.Digit(start.solution.atIndex(start.firstEmpty())))
        assertTrue(played.hasProgress)
    }
}
