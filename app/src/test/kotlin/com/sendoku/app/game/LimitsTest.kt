package com.sendoku.app.game

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.sendoku.app.data.toSettings
import com.sendoku.app.data.write
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.RatedPuzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The two ways to lose.
 *
 * Three mistakes and three hints, both on by default and both switchable off. The hint limit
 * is the newer and stranger of the two, so most of these are about it.
 */
class LimitsTest {

    private val puzzle: RatedPuzzle by lazy {
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(5150))
        var made: RatedPuzzle? = null
        while (made == null) made = maker.next(Symmetry.ROTATIONAL)
        made
    }

    private fun game(settings: GameSettings = GameSettings()) = GameState.start(puzzle, settings)

    @Test
    fun `both limits are on by default, at three`() {
        val settings = GameSettings()
        assertEquals(3, settings.mistakeLimit)
        assertEquals(3, settings.hintLimit)
    }

    @Test
    fun `three hints ends the game`() {
        var state = game()
        repeat(2) { state = state.countHint() }
        assertFalse("two hints should not end it", state.isOver)

        state = state.countHint()
        assertTrue("three hints did not end it", state.isOver)
        assertTrue(state.outOfHints)
        assertFalse("losing on hints is not losing on mistakes", state.outOfMistakes)
    }

    @Test
    fun `three mistakes ends the game`() {
        var state = game()
        val empty = state.cells.indices.first { state.cells[it].isEmpty }
        val right = state.solution.atIndex(empty)
        val wrong = (1..9).first { it != right }

        repeat(3) {
            state = state.select(empty).enter(wrong).erase()
        }
        assertTrue("three mistakes did not end it", state.isOver)
        assertTrue(state.outOfMistakes)
    }

    @Test
    fun `a hint asked for after the game is over does not count`() {
        // Otherwise the count on the win screen creeps up while somebody reads it.
        var state = game()
        repeat(3) { state = state.countHint() }
        val ended = state

        state = state.countHint().countHint()
        assertEquals(ended.hintsUsed, state.hintsUsed)
    }

    @Test
    fun `turning the hint limit off gives back as many as you like`() {
        var state = game(GameSettings(hintLimit = null))
        repeat(20) { state = state.countHint() }
        assertFalse("hints ended a game with no hint limit", state.isOver)
        assertEquals(20, state.hintsUsed)
    }

    @Test
    fun `turning the mistake limit off leaves the hint limit alone`() {
        var state = game(GameSettings(mistakeLimit = null))
        repeat(3) { state = state.countHint() }
        assertTrue("the hint limit stopped working when mistakes were switched off", state.isOver)
    }

    @Test
    fun `a solved game is won even if the last hint used up the allowance`() {
        // Solving is solving. A player who spent their third hint on the final cell has not
        // lost, and the order these two are checked in is what decides that.
        var state = game(GameSettings(hintLimit = 3))
        repeat(2) { state = state.countHint() }
        for (cell in state.cells.indices) {
            if (state.cells[cell].isEmpty) state = state.select(cell).enter(state.solution.atIndex(cell))
        }
        assertTrue(state.isSolved)
        assertFalse("a solved board was called a loss", state.isFailed)
    }

    @Test
    fun `a fresh install gets the limits, and switching one off sticks`() {
        // The bug this catches: reading an absent preference as null rather than as the
        // default, so a new phone came up with both limits quietly switched off.
        val fresh = emptyPreferences().toSettings()
        assertEquals(3, fresh.mistakeLimit)
        assertEquals(3, fresh.hintLimit)

        val switchedOff = mutablePreferencesOf().apply { write(GameSettings(hintLimit = null)) }
        assertEquals(null, switchedOff.toSettings().hintLimit)
        assertEquals(3, switchedOff.toSettings().mistakeLimit)
    }

    @Test
    fun `a limit of zero is refused rather than starting an unplayable game`() {
        val thrown = runCatching { GameSettings(hintLimit = 0) }.exceptionOrNull()
        assertTrue("expected a refusal, got $thrown", thrown is IllegalArgumentException)
    }
}
