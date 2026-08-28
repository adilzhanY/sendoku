package com.sendoku.app.game

import com.sendoku.app.data.SavedGame
import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.technique.Techniques
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Where the time went, after a game that was won.
 *
 * Two things have to be true or this is worse than nothing. It must say nothing at all about
 * a solve that never slowed down, because an app that finds a lesson in every win turns
 * winning into being marked. And the technique it names has to have been genuinely available
 * on the board as it stood at that moment rather than on the finished board, or it is telling
 * the player about something that was not there yet.
 */
class PostMortemTest {

    private fun puzzle() = GradedGenerator(Dimensions.CLASSIC, Random(31)).let { maker ->
        var made = maker.next(Symmetry.ROTATIONAL)
        while (made == null || made.grade != Grade.STEADY) made = maker.next(Symmetry.ROTATIONAL)
        made
    }

    /** Solves the puzzle, waiting [pause] before the digit at [pauseBefore]. */
    private fun solved(pause: Int = 0, pauseBefore: Int = 3): GameState {
        var state = GameState.start(puzzle())
        val empties = state.cells.indices.filter { state.cells[it].isEmpty }
        for ((index, cell) in empties.withIndex()) {
            state = state.tick(if (index == pauseBefore) pause.seconds else 10.seconds)
            state = state.select(cell).enter(state.solution.atIndex(cell))
        }
        return state
    }

    @Test
    fun aSolveThatNeverSlowedDownIsToldNothing() {
        val quick = solved(pause = 5)
        assertTrue(quick.isSolved)
        assertEquals(emptyList<Moment>(), PostMortem.of(quick))
    }

    @Test
    fun aLongPauseIsNamedWithWhatWasThere() {
        val slow = solved(pause = 300)
        val moments = PostMortem.of(slow)
        assertEquals(1, moments.size)
        assertEquals(300.seconds, moments.first().spent)
    }

    @Test
    fun theTechniqueNamedWasReallyAvailableThen() {
        val slow = solved(pause = 300)
        val moment = PostMortem.of(slow).first()
        val board = Board(slow.dims)
        for (index in slow.cells.indices) {
            if (slow.cells[index].isGiven) board.setAtIndex(index, slow.cells[index].digit)
        }
        for (placement in slow.placements) {
            if (placement.at > moment.at.inWholeSeconds) break
            board.setAtIndex(placement.cell, placement.digit)
        }
        assertEquals(moment.available, Techniques.availableOn(board)?.technique)
    }

    @Test
    fun anUnfinishedGameIsToldNothing() {
        var state = GameState.start(puzzle()).tick(20.minutes)
        val cell = state.cells.indexOfFirst { it.isEmpty }
        state = state.select(cell).enter(state.solution.atIndex(cell))
        assertEquals(emptyList<Moment>(), PostMortem.of(state))
    }

    @Test
    fun aPlacementTakenBackIsNotPartOfTheRecord() {
        var state = GameState.start(puzzle())
        val cell = state.cells.indexOfFirst { it.isEmpty }
        state = state.tick(10.seconds).select(cell).enter(state.solution.atIndex(cell))
        assertEquals(1, state.placements.size)
        assertEquals(0, state.undo().placements.size)
        assertEquals(1, state.undo().redo().placements.size)
    }

    @Test
    fun theRecordSurvivesBeingWrittenDown() {
        // A solve can be put down and picked up tomorrow, and the post mortem is about the
        // whole of it.
        val played = solved(pause = 300)
        val back = SavedGame.of(played).toState(GameSettings())
        assertEquals(played.placements, back.placements)
    }
}
