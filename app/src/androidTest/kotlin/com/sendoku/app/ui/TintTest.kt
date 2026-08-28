package com.sendoku.app.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.sendoku.app.data.SavedGame
import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameState
import com.sendoku.app.game.clearTints
import com.sendoku.app.game.tint
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

/**
 * Colouring cells in.
 *
 * A tint is a working note and the tests are about what it must never do: change the rules,
 * be undone by undo, or be lost when the app is closed halfway through following a chain,
 * which is the only time anybody is using it.
 */
class TintTest {

    @get:Rule
    val compose = createComposeRule()

    private fun game(): GameState {
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(31))
        var made = maker.next(Symmetry.ROTATIONAL)
        while (made == null || made.grade != Grade.TRICKY) made = maker.next(Symmetry.ROTATIONAL)
        return GameState.start(made)
    }

    @Test
    fun aTintGoesOnAndComesOffAgain() {
        val empty = game().cells.indexOfFirst { it.isEmpty }
        val coloured = game().tint(empty, 2)
        assertEquals(2, coloured.tints[empty])
        assertEquals(emptyMap<Int, Int>(), coloured.tint(empty, 2).tints)
        assertEquals(3, coloured.tint(empty, 3).tints[empty])
    }

    @Test
    fun colouringChangesNothingAboutTheGame() {
        val before = game()
        val cell = before.cells.indexOfFirst { it.isEmpty }
        val after = before.tint(cell, 0)
        assertEquals(before.cells, after.cells)
        assertEquals(before.mistakes, after.mistakes)
        assertEquals(before.canUndo, after.canUndo)
        assertEquals(before.isSolved, after.isSolved)
    }

    @Test
    fun undoDoesNotWalkBackThroughColours() {
        // Undo is for the board. Somebody who has just coloured four cells wants the digit
        // they typed by mistake taken back, not their working.
        val start = game()
        val cell = start.cells.indexOfFirst { it.isEmpty }
        val played = start.select(cell).enter(start.solution.atIndex(cell)).tint(cell, 1)
        val undone = played.undo()
        assertEquals("undo took the colour off", 1, undone.tints[cell])
        assertTrue("undo did not take the digit back", undone.cells[cell].isEmpty)
    }

    @Test
    fun coloursSurviveBeingWrittenDownAndReadBack() {
        val start = game()
        val cells = start.cells.indices.filter { start.cells[it].isEmpty }.take(4)
        var coloured = start
        for ((index, cell) in cells.withIndex()) coloured = coloured.tint(cell, index)

        val saved = SavedGame.of(coloured)
        val back = saved.toState(GameSettings())
        assertEquals(coloured.tints, back.tints)
        assertNotEquals(0, back.tints.size)
    }

    @Test
    fun aGameWithNoColoursWritesNothingDown() {
        assertEquals("", SavedGame.of(game()).tints)
    }

    @Test
    fun clearingTakesThemAllOff() {
        var coloured = game()
        for (cell in 0 until 9) coloured = coloured.tint(cell, cell % 4)
        assertEquals(emptyMap<Int, Int>(), coloured.clearTints().tints)
    }
}
