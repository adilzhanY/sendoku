package com.sendoku.app.learn

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A lesson from a keyboard.
 *
 * The course was the one part of the app a keyboard could not reach, which made it the one
 * part somebody using a phone with a keyboard, or a switch device, could not do.
 */
class LessonKeyTest {

    private class Recorder {
        var digit: Int? = null
        var next = 0
        var back = 0
        var replay = 0
    }

    private fun press(key: Key, waiting: Boolean = false, expected: Int? = null): Pair<Boolean, Recorder> {
        val record = Recorder()
        val handled = lessonKey(
            key = key,
            waiting = waiting,
            expected = expected,
            onDigit = { record.digit = it },
            onNext = { record.next++ },
            onBack = { record.back++ },
            onReplay = { record.replay++ },
        )
        return handled to record
    }

    @Test
    fun `right steps forward and left steps back`() {
        assertEquals(1, press(Key.DirectionRight).second.next)
        assertEquals(1, press(Key.DirectionLeft).second.back)
    }

    @Test
    fun `enter and space step forward too, since both mean go on`() {
        assertEquals(1, press(Key.Enter).second.next)
        assertEquals(1, press(Key.Spacebar).second.next)
    }

    @Test
    fun `a digit does nothing while the lesson is talking`() {
        // Otherwise pressing 5 halfway through the reading silently answers a question three
        // steps away, and the lesson jumps for no reason the player can see.
        val (handled, record) = press(Key.Five, waiting = false)
        assertFalse(handled)
        assertEquals(null, record.digit)
    }

    @Test
    fun `a digit answers when the lesson is waiting for one`() {
        val (handled, record) = press(Key.Five, waiting = true, expected = 2)
        assertTrue(handled)
        assertEquals(5, record.digit)
    }

    @Test
    fun `stepping on is refused while the lesson is waiting for an answer`() {
        val (handled, record) = press(Key.DirectionRight, waiting = true, expected = 2)
        assertFalse(handled)
        assertEquals(0, record.next)
    }

    @Test
    fun `r replays from the start`() {
        assertEquals(1, press(Key.R).second.replay)
    }

    @Test
    fun `a key with no meaning is left alone`() {
        assertFalse(press(Key.Q).first)
    }
}
