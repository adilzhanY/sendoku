package com.sendoku.engine.technique

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates
import com.sendoku.engine.Dimensions
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Death Blossom, one condition at a time.
 *
 * The rule rests on the stem seeing every home of its digit inside the matching petal. Miss
 * that and the petal is never forced, so the elimination is simply false. The same goes for
 * a petal missing for one of the stem's digits: two petals out of three prove nothing at
 * all, because the third case is left standing.
 */
class DeathBlossomTest {

    private fun blank() = CandidateGrid.of(Board(Dimensions.CLASSIC))

    private fun CandidateGrid.only(cell: Int, vararg keep: Int) = apply {
        val kept = Candidates.of(*keep)
        Candidates.all(dims).forEach { if (it !in kept) eliminate(cell, it) }
    }

    private fun rc(row: Int, col: Int) = row * 9 + col

    /** Stem {1,2} at the centre, a petal along its row for the 1 and down its column for the 2. */
    private fun flower() = blank()
        .only(rc(4, 4), 1, 2)
        .only(rc(4, 0), 1, 5, 7).only(rc(4, 1), 1, 7)
        .only(rc(0, 4), 2, 5, 9).only(rc(1, 4), 2, 9)

    @Test
    fun `the digit both petals hold falls where it can be seen in both`() {
        val step = assertNotNull(DeathBlossom.find(flower()))

        assertTrue(step.eliminations.isNotEmpty())
        for ((cell, digit) in step.eliminations) {
            assertTrue(digit == 5, "the flower struck a $digit, and 5 is the only digit it argues about")
            assertTrue(cell !in step.focusCells, "the flower struck itself")
        }
        assertTrue(step.eliminations.any { it.cell == rc(0, 0) }, "the corner cell keeps its 5")
    }

    @Test
    fun `the stem is part of the hint, since nothing works without it`() {
        val step = assertNotNull(DeathBlossom.find(flower()))
        assertTrue(rc(4, 4) in step.focusCells, "the stem is missing from the hint")
    }

    @Test
    fun `a stem digit with no petal proves nothing`() {
        // The stem can be a 3, and nothing at all is claimed about what happens then, so the
        // whole argument has a case missing.
        val grid = blank()
            .only(rc(4, 4), 1, 2, 3)
            .only(rc(4, 0), 1, 5, 7).only(rc(4, 1), 1, 7)
            .only(rc(0, 4), 2, 5, 9).only(rc(1, 4), 2, 9)

        assertNull(DeathBlossom.find(grid))
    }

    @Test
    fun `a petal the stem cannot see everywhere proves nothing`() {
        // The 2 in the second petal now has a home outside the stem's column, so the stem
        // taking 2 no longer empties it.
        val grid = blank()
            .only(rc(4, 4), 1, 2)
            .only(rc(4, 0), 1, 5, 7).only(rc(4, 1), 1, 7)
            .only(rc(0, 4), 2, 5, 9).only(rc(0, 5), 2, 9)

        assertNull(DeathBlossom.find(grid))
    }

    @Test
    fun `the digit the flower strikes is never one the stem could be`() {
        val step = assertNotNull(DeathBlossom.find(flower()))
        for ((_, digit) in step.eliminations) {
            assertTrue(digit != 1 && digit != 2, "the flower struck a digit its own stem is arguing about")
        }
    }
}
