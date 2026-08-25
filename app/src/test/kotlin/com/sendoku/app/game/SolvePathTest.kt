package com.sendoku.app.game

import com.sendoku.engine.Dimensions
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.technique.TechniqueId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The solve path, which is the answer to "what was I supposed to have seen".
 *
 * It is only ever shown after the game is over, so the thing worth pinning down is not what
 * it withholds but that it is the truth: the same walk the rating came from, in the same
 * order, finishing the same grid.
 */
class SolvePathTest {

    private val puzzle = GradedGenerator(Dimensions.CLASSIC, Random(31)).let { maker ->
        generateSequence { maker.next(Symmetry.ROTATIONAL) }.first { it.hardest != null }
    }

    @Test
    fun `the path is the whole solve, numbered from one`() {
        val path = SolvePath.of(puzzle.puzzle.givens)

        assertTrue("the path is empty", path.steps.isNotEmpty())
        assertEquals(path.steps.indices.map { it + 1 }, path.steps.map { it.number })
        assertEquals("the path is not as long as the rating said", puzzle.stepCount, path.steps.size)
    }

    @Test
    fun `every step either places a digit or strikes a mark`() {
        for (step in SolvePath.of(puzzle.puzzle.givens).steps) {
            assertTrue("step ${step.number} does nothing", step.placement != null || step.struck > 0)
        }
    }

    @Test
    fun `every digit the path places is the digit the answer wants`() {
        val solution = puzzle.puzzle.solution
        for (step in SolvePath.of(puzzle.puzzle.givens).steps) {
            val place = step.placement ?: continue
            assertEquals("step ${step.number}", solution.atIndex(place.cell), place.digit)
        }
    }

    @Test
    fun `the hardest step in the path is the technique the grade is named after`() {
        val path = SolvePath.of(puzzle.puzzle.givens)
        val hardest = path.steps.maxByOrNull { it.technique.cost }?.technique
        assertEquals(puzzle.hardest, hardest)
    }

    @Test
    fun `the deep end rules are the ones marked out`() {
        val singles = SolvePath.Step(1, TechniqueId.NAKED_SINGLE, null, 1, 9)
        val fork = SolvePath.Step(2, TechniqueId.FORCING_CHAIN, null, 1, 9)
        assertTrue(!singles.advanced)
        assertTrue(fork.advanced)
    }
}
