package com.sendoku.app.learn

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Dimensions
import com.sendoku.engine.catalog.CatalogReader
import com.sendoku.engine.technique.TechniqueId
import com.sendoku.engine.technique.Techniques
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The exercises, checked against the engine that produced them.
 *
 * The point of generating a position rather than writing one is that it cannot be wrong. This
 * proves that: every exercise handed out really does contain the technique it names, at the
 * cells it names.
 */
class PracticePositionsTest {

    private val dims = Dimensions.CLASSIC

    private fun puzzles(count: Int) = sequence {
        val reader = CatalogReader.from(javaClass.getResourceAsStream("/catalog/classic.sdkb")!!)
        for (index in 0 until minOf(count, reader.size)) yield(reader.puzzleAt(index))
    }

    @Test
    fun `an exercise really contains the technique it names`() {
        for (technique in listOf(
            TechniqueId.HIDDEN_SINGLE,
            TechniqueId.LOCKED_CANDIDATES_POINTING,
            TechniqueId.NAKED_PAIR,
            TechniqueId.X_WING,
        )) {
            val exercise = PracticePositions.find(technique, puzzles(4), dims)
            assertNotNull("no position found for $technique", exercise)
            requireNotNull(exercise)
            assertEquals(technique, exercise.technique)

            val grid = CandidateGrid.ofOrNull(Board.parse(dims, exercise.board))
            assertNotNull("$technique was set on a board with a contradiction", grid)
            requireNotNull(grid)

            val found = Techniques.ladder.first { it.id == technique }.find(grid)
            assertNotNull("$technique does not apply to the board it was found on", found)
        }
    }

    @Test
    fun `the cells an exercise asks for are the cells the engine names`() {
        val exercise = PracticePositions.find(TechniqueId.X_WING, puzzles(4), dims)
        assertNotNull(exercise)
        requireNotNull(exercise)

        val grid = requireNotNull(CandidateGrid.ofOrNull(Board.parse(dims, exercise.board)))
        val found = requireNotNull(Techniques.ladder.first { it.id == TechniqueId.X_WING }.find(grid))
        val agreed = (found.focusCells + found.placements.map { it.cell }).toSet()

        assertEquals("the answer does not match the engine's own", agreed, exercise.cells)
        assertTrue("an X-Wing that eliminates nothing is not worth practising", exercise.eliminations.isNotEmpty())
    }

    @Test
    fun `every technique the app teaches can be practised`() {
        // The claim behind the practice screen: whatever the course names, the app can hand
        // you a real board with one on it. Not "and nothing cheaper applies", which is neither
        // achievable on a digits only board nor desirable, since finding the X-Wing on a grid
        // that also has a single in it is the actual skill.
        // Cage rules excluded: they cannot appear on a board without cages, and the
        // batch this searches is an ordinary one.
        val missing = TechniqueId.entries
            .filterNot { it.isCage }
            .filter { PracticePositions.find(it, puzzles(3000), dims) == null }
        assertTrue("no practice position exists for: $missing", missing.isEmpty())
    }

    @Test
    fun `a technique that never appears returns nothing rather than looping`() {
        // One puzzle is not enough to contain every technique, and the search has to say so
        // instead of walking the same grid forever.
        val exercise = PracticePositions.find(TechniqueId.ALS_XZ, puzzles(1), dims)
        assertTrue(
            "expected either a real ALS position or nothing at all",
            exercise == null || exercise.technique == TechniqueId.ALS_XZ,
        )
    }
}
