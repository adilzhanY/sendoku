package com.sendoku.app.game

import com.sendoku.engine.Grade
import com.sendoku.engine.catalog.CatalogReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules about mistakes, written down after the app broke every one of them at once.
 *
 * Somebody lost an Easy puzzle with one cell to go, because that cell could take no digit at
 * all. What had happened was a single wrong digit twenty moves earlier which broke no rule,
 * so nothing on the board showed it, though the counter in the header had gone up and the
 * app therefore knew. Every attempt at the doomed cell then cost another mistake, and the
 * third one ended the game.
 *
 * Two rules come out of that, and both are tested here. A mistake that is charged for is a
 * mistake that is shown. And a mistake is charged for once, at the cell where it was made,
 * never again at the cells it ruined.
 */
class MistakeFairnessTest {

    private val puzzle = CatalogReader
        .from(checkNotNull(javaClass.getResourceAsStream("/catalog/classic.sdkb")))
        .let { catalog -> catalog.puzzleAt(catalog.indicesOf(Grade.GENTLE).first()) }

    private fun game(settings: GameSettings = GameSettings()) = GameState.start(puzzle, settings)

    /** A cell where a digit can be wrong without breaking any rule, and that digit. */
    private fun quietLie(state: GameState): Pair<Int, Int> {
        val cell = state.cells.indices.first { at ->
            state.cells[at].isEmpty &&
                (1..9).any { it != state.solution.atIndex(at) && it in state.candidatesAt(at) }
        }
        val digit = (1..9).first { it != state.solution.atIndex(cell) && it in state.candidatesAt(cell) }
        return cell to digit
    }

    @Test
    fun `a wrong digit that breaks no rule is shown at once`() {
        val start = game()
        val (cell, digit) = quietLie(start)
        val state = start.select(cell).enter(digit)

        assertEquals("the mistake was not counted", 1, state.mistakes)
        assertTrue("the mistake was counted and hidden", cell in state.flaggedWrong)
        assertTrue(
            "it breaks no rule, so the conflict flag alone would never have caught it",
            state.conflicts.isEmpty(),
        )
    }

    @Test
    fun `with no limit and no auto check the board says nothing, which is the paper mode`() {
        val start = game(GameSettings(mistakeLimit = null, autoCheck = false))
        val (cell, digit) = quietLie(start)
        val state = start.select(cell).enter(digit)

        assertTrue("a board that judges nothing marked something", state.flaggedWrong.isEmpty())
        assertEquals("a game with no limit still counts for the record", 1, state.mistakes)
    }

    @Test
    fun `a cell whose answer has been taken away never costs a mistake`() {
        var state = game()
        val (cell, digit) = quietLie(state)
        state = state.select(cell).enter(digit)
        val after = state.mistakes

        // Whatever this error ruined, find it and try every digit in it.
        val ruined = state.cells.indices.firstOrNull { at ->
            state.cells[at].isEmpty && state.solution.atIndex(at) !in state.candidatesAt(at)
        }
        assertTrue("the position under test never arose", ruined != null)
        requireNotNull(ruined)

        for (attempt in 1..9) state = state.select(ruined).enter(attempt)

        assertEquals("a cell an earlier mistake had already ruined was charged for", after, state.mistakes)
        assertTrue("the game ended over a cell that could never have been right", !state.isFailed)
    }

    @Test
    fun `one hidden slip can no longer end a game on its own`() {
        // The whole of what happened, played out: one wrong digit, then somebody carrying on
        // and filling in everything they can see.
        var state = game()
        val (cell, digit) = quietLie(state)
        state = state.select(cell).enter(digit)

        for (at in state.cells.indices) {
            if (!state.cells[at].isEmpty) continue
            val truth = state.solution.atIndex(at)
            state = if (truth in state.candidatesAt(at)) {
                state.select(at).enter(truth)
            } else {
                state.select(at).enter(state.candidatesAt(at).toList().firstOrNull() ?: 1)
            }
        }

        assertEquals("more than one mistake was charged for one slip", 1, state.mistakes)
        assertTrue("the game was lost to a single hidden slip", !state.isFailed)
    }

    @Test
    fun `three real mistakes in three good cells still end the game`() {
        // The limit has to keep working, or the fix has quietly removed the feature.
        var state = game()
        repeat(3) {
            val (cell, digit) = quietLie(state)
            state = state.select(cell).enter(digit)
            // Take it back off so the next cell is a fresh, healthy one.
            if (!state.isFailed) state = state.select(cell).erase()
        }
        assertEquals(3, state.mistakes)
        assertTrue("three real mistakes did not end the game", state.isFailed)
    }
}
