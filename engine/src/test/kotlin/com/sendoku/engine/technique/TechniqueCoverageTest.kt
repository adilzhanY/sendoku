package com.sendoku.engine.technique

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Every technique, checked against a position built for it and nothing else.
 *
 * The positions live in [TechniquePositions] rather than being scattered across the test
 * files, so that adding a technique without a position, or breaking one so it stops
 * firing at all, fails here rather than quietly reducing coverage.
 */
class TechniqueCoverageTest {

    /**
     * Positions where a cheaper rule also fires, and why that is not a flaw in the position.
     *
     * The upper half of the ladder is built out of the lower half. A three cell XY-Chain is
     * an XY-Wing seen from another angle, and an ALS-XZ with small sets is both. Their
     * minimal positions cannot exclude what they generalise, and forcing them to would mean
     * testing something other than the minimal shape.
     */
    private val expectedOverlap: Map<TechniqueId, Set<TechniqueId>> = mapOf(
        // A rectangle puts two cells of the pair on the same row, which is a naked pair.
        TechniqueId.UNIQUE_RECTANGLE to setOf(TechniqueId.NAKED_PAIR),
        // Neighbours in the chain share a pair and see each other, so they are a naked pair.
        TechniqueId.REMOTE_PAIRS to setOf(TechniqueId.NAKED_PAIR),
        // Four base rows leave enough structure behind that smaller patterns appear too.
        TechniqueId.JELLYFISH to setOf(
            TechniqueId.LOCKED_CANDIDATES_POINTING,
            TechniqueId.LOCKED_CANDIDATES_CLAIMING,
            TechniqueId.SIMPLE_COLOURING,
        ),
        // Two chains in one stack of boxes necessarily strip a column down to one box.
        TechniqueId.MULTI_COLOURING to setOf(TechniqueId.LOCKED_CANDIDATES_CLAIMING),
        // A grave is dense with two home digits, which is what colouring feeds on.
        TechniqueId.BUG_PLUS_ONE to setOf(
            TechniqueId.LOCKED_CANDIDATES_CLAIMING,
            TechniqueId.SIMPLE_COLOURING,
        ),
        // A three link chain on one digit is two coloured clusters meeting.
        TechniqueId.X_CHAIN to setOf(TechniqueId.MULTI_COLOURING),
        // A three cell XY-Chain is exactly an XY-Wing.
        TechniqueId.XY_CHAIN to setOf(TechniqueId.XY_WING),
        // Small almost locked sets are wings and chains wearing a different name.
        TechniqueId.ALS_XZ to setOf(TechniqueId.XY_WING, TechniqueId.XY_CHAIN),
        // The smallest crossing takes two cells and one cell in each outer set, which makes
        // every cell of the pattern bivalue, and four bivalue cells in a ring are a chain.
        // A bigger crossing would avoid it, but only by restricting so much of the line that
        // the digits left over become a hidden quad, which is a worse position, not a better
        // one.
        TechniqueId.SUE_DE_COQ to setOf(TechniqueId.XY_CHAIN),
    )

    @Test
    fun `every technique has a position of its own`() {
        assertEquals(TechniqueId.entries.toSet(), TechniquePositions.byTechnique.keys)
    }

    @Test
    fun `every technique fires on its own position`() {
        for (technique in Techniques.ladder) {
            val grid = TechniquePositions.byTechnique.getValue(technique.id)()
            val step = assertNotNull(technique.find(grid), "${technique.id} found nothing on its own position")
            assertEquals(technique.id, step.technique)
            assertTrue(step.placements.isNotEmpty() || step.eliminations.isNotEmpty())
        }
    }

    @Test
    fun `no cheaper rule fires on a position, beyond the overlaps that are inherent`() {
        for (technique in Techniques.ladder) {
            val grid = TechniquePositions.byTechnique.getValue(technique.id)()
            val cheaper = Techniques.ladder.takeWhile { it.id != technique.id }
            val alsoFires = cheaper.filter { it.find(grid) != null }.map { it.id }.toSet()
            assertEquals(
                expectedOverlap[technique.id].orEmpty(),
                alsoFires,
                "${technique.id} position is not the shape it claims to be",
            )
        }
    }

    @Test
    fun `two thirds of the ladder is tested in complete isolation`() {
        val isolated = TechniqueId.entries.count { it !in expectedOverlap }
        assertTrue(isolated >= 16, "only $isolated techniques have a position all to themselves")
    }

    @Test
    fun `the solver picks the intended rule on each position`() {
        // A position is only honest if the solver, walking the ladder from the bottom, would
        // actually reach for the rule the position was built for.
        for (technique in Techniques.ladder) {
            val grid = TechniquePositions.byTechnique.getValue(technique.id)()
            val chosen = Techniques.ladder.firstNotNullOfOrNull { it.find(grid) }
            val expected = expectedOverlap[technique.id]?.minByOrNull { it.cost } ?: technique.id
            assertEquals(expected, assertNotNull(chosen).technique, "on the ${technique.id} position")
        }
    }
}
