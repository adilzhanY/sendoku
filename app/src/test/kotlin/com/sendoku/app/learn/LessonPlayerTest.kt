package com.sendoku.app.learn

import com.sendoku.engine.Board
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The board a lesson shows at each step.
 *
 * Rebuilt from the lesson every time rather than mutated forwards, so stepping back is the
 * same operation as stepping on. These check that it really is, because a walkthrough that
 * cannot be replayed is a walkthrough somebody has to restart to reread.
 */
class LessonPlayerTest {

    private val lesson = Curriculum.byId(LessonId.FIRST_SOLVE_FOUR)

    @Test
    fun `the first step shows the board the lesson started from`() {
        val state = stateAt(lesson, 0, answered = false)
        val start = Board.parse(lesson.dims, lesson.board)
        for (cell in 0 until lesson.dims.cellCount) {
            assertEquals("cell $cell", start.atIndex(cell), state.cells[cell].digit)
        }
    }

    @Test
    fun `a placed digit appears at the step that places it and not before`() {
        val place = lesson.steps.filterIsInstance<Step.Place>().first()
        val at = lesson.steps.indexOf(place)

        assertEquals(Board.EMPTY, stateAt(lesson, at - 1, answered = false).cells[place.cell].digit)
        assertEquals(place.digit, stateAt(lesson, at, answered = false).cells[place.cell].digit)
    }

    @Test
    fun `stepping back really does undo, because the board is rebuilt`() {
        val place = lesson.steps.filterIsInstance<Step.Place>().first()
        val at = lesson.steps.indexOf(place)

        val forward = stateAt(lesson, at, answered = false)
        val back = stateAt(lesson, at - 1, answered = false)
        val forwardAgain = stateAt(lesson, at, answered = false)

        assertEquals(place.digit, forward.cells[place.cell].digit)
        assertEquals(Board.EMPTY, back.cells[place.cell].digit)
        assertEquals(forward.cells, forwardAgain.cells)
    }

    @Test
    fun `the cell a lesson asks for stays empty until it is answered`() {
        val turn = lesson.steps.filterIsInstance<Step.YourTurn>().first()
        val at = lesson.steps.indexOf(turn)

        assertEquals(Board.EMPTY, stateAt(lesson, at, answered = false).cells[turn.cell].digit)
        assertEquals(turn.digit, stateAt(lesson, at, answered = true).cells[turn.cell].digit)
    }

    @Test
    fun `what the lesson places is not marked as a given`() {
        // Otherwise it draws in the heavy weight the course just taught means "came with the
        // puzzle", which would contradict the lesson on givens three screens earlier.
        val turn = lesson.steps.filterIsInstance<Step.YourTurn>().first()
        val state = stateAt(lesson, lesson.steps.indexOf(turn), answered = true)
        assertFalse(state.cells[turn.cell].isGiven)
        assertTrue(state.cells[0].isGiven)
    }

    @Test
    fun `a lesson has no clock and no mistake limit`() {
        val state = stateAt(lesson, 0, answered = false)
        assertFalse("a lesson is not timed", state.settings.showTimer)
        assertEquals("a lesson cannot be failed", null, state.settings.mistakeLimit)
    }

    @Test
    fun `every lesson can be rebuilt at every one of its steps`() {
        for (lesson in Curriculum.lessons) {
            for (at in lesson.steps.indices) {
                val state = stateAt(lesson, at, answered = true)
                assertEquals("${lesson.id} step $at", lesson.dims.cellCount, state.cells.size)
            }
        }
    }
}
