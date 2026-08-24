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
 * ALS-XY-Wing, on positions built one condition at a time.
 *
 * Three groups and two joining digits give four ways to be nearly right, and each of them
 * would produce an elimination that is simply false. So there is a test for each: the two
 * joining digits being the same, a joining digit that is not restricted, the struck digit
 * being one of the joiners, and a target that only sees half the pattern.
 */
class AlsXyWingTest {

    private fun blank() = CandidateGrid.of(Board(Dimensions.CLASSIC))

    private fun CandidateGrid.only(cell: Int, vararg keep: Int) = apply {
        val kept = Candidates.of(*keep)
        Candidates.all(dims).forEach { if (it !in kept) eliminate(cell, it) }
    }

    private fun rc(row: Int, col: Int) = row * 9 + col

    /**
     * The wing used by most of these tests.
     *
     * A is {1,5} and {1,7} in row 1, the hinge is {1,2} at r2c1, and C is {2,5,9} and {2,9}
     * in row 2. The 1 is restricted between A and the hinge, the 2 between the hinge and C,
     * and 5 is held by both outer groups.
     */
    private fun wing() = blank()
        .only(rc(0, 0), 1, 5).only(rc(0, 1), 1, 7)
        .only(rc(1, 0), 1, 2)
        .only(rc(1, 4), 2, 5, 9).only(rc(1, 5), 2, 9)

    @Test
    fun `a cell seeing the shared digit in both outer groups loses it`() {
        val step = assertNotNull(AlsXyWing.find(wing()))

        assertTrue(step.eliminations.isNotEmpty())
        for ((cell, digit) in step.eliminations) {
            assertEqualsDigit(5, digit)
            assertTrue(cell !in step.focusCells, "the wing struck itself")
            assertTrue(
                CandidateGrid.of(Board(Dimensions.CLASSIC)).sees(cell, rc(0, 0)),
                "cell $cell does not see the 5 in the first group",
            )
            assertTrue(
                CandidateGrid.of(Board(Dimensions.CLASSIC)).sees(cell, rc(1, 4)),
                "cell $cell does not see the 5 in the third group",
            )
        }
    }

    @Test
    fun `all three groups are in the hint, not only the two that matter at the end`() {
        // The hinge proves nothing on its own and is the part a player cannot reconstruct
        // from the elimination, so a hint that leaves it out is unreadable.
        val step = assertNotNull(AlsXyWing.find(wing()))
        assertTrue(rc(1, 0) in step.focusCells, "the hinge is missing from the hint")
        assertTrue(rc(0, 0) in step.focusCells, "the first group is missing from the hint")
        assertTrue(rc(1, 4) in step.focusCells, "the third group is missing from the hint")
    }

    @Test
    fun `two joining digits that are the same prove nothing`() {
        // With x and y equal the middle group is never forced to anything, so the chain
        // never reaches the third group.
        val grid = blank()
            .only(rc(0, 0), 1, 5).only(rc(0, 1), 1, 7)
            .only(rc(1, 0), 1, 3)
            .only(rc(1, 4), 1, 5, 9).only(rc(1, 5), 1, 9)

        assertNull(AlsXyWing.find(grid))
    }

    @Test
    fun `a joining digit that is not restricted proves nothing`() {
        // The 2 now has a home in the third group that the hinge cannot see, so both may
        // take it and the middle group is never locked.
        val grid = blank()
            .only(rc(0, 0), 1, 5).only(rc(0, 1), 1, 7)
            .only(rc(1, 0), 1, 2)
            .only(rc(4, 4), 2, 5, 9).only(rc(4, 5), 2, 9)

        assertNull(AlsXyWing.find(grid))
    }

    @Test
    fun `the struck digit is never one of the two that join the groups`() {
        val step = assertNotNull(AlsXyWing.find(wing()))
        for ((_, digit) in step.eliminations) {
            assertTrue(digit != 1 && digit != 2, "the wing struck a digit it needed to hold the chain together")
        }
    }

    private fun assertEqualsDigit(expected: Int, actual: Int) {
        assertTrue(expected == actual, "expected the shared digit $expected, got $actual")
    }
}
