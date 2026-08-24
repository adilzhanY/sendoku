package com.sendoku.engine.technique

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates
import com.sendoku.engine.Dimensions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sue de Coq, on positions small enough to check by hand.
 *
 * The rule strikes two different groups of digits in two different houses off one pattern,
 * so the tests check both halves separately. The counting condition is the whole rule, and
 * a version that fired without it would be wrong rather than merely eager, so there is a
 * test for each way the count can fail.
 */
class SueDeCoqTest {

    private fun blank() = CandidateGrid.of(Board(Dimensions.CLASSIC))

    private fun CandidateGrid.only(cell: Int, vararg keep: Int) = apply {
        val kept = Candidates.of(*keep)
        Candidates.all(dims).forEach { if (it !in kept) eliminate(cell, it) }
    }

    private fun rc(row: Int, col: Int) = row * 9 + col

    /**
     * The pattern used by most of these tests.
     *
     * Row 0 crosses the top left box at r1c1 and r1c2, which hold {1,3} and {2,4}. The
     * crossing is two cells and four digits, so it is two digits short of locked. r1c5
     * takes {1,2} further along the row, r2c1 takes {3,4} lower in the box, and between
     * them they claim exactly the two the crossing leaves over.
     */
    private fun pattern() = blank()
        .only(rc(0, 0), 1, 3)
        .only(rc(0, 1), 2, 4)
        .only(rc(0, 4), 1, 2)
        .only(rc(1, 0), 3, 4)

    @Test
    fun `the line loses the digits of the set that lies along it`() {
        val step = assertNotNull(SueDeCoq.find(pattern()))

        // Along the row but past the box, so the box half of the rule cannot be mistaken
        // for this half. Those cells share the row with the pattern and nothing else.
        val struckInRow = step.eliminations.filter { it.cell / 9 == 0 && it.cell % 9 >= 3 }
        assertTrue(struckInRow.isNotEmpty(), "nothing was struck from the line")
        for ((cell, digit) in struckInRow) {
            assertTrue(digit == 1 || digit == 2, "the line lost $digit, which is not in its set")
            assertTrue(cell !in listOf(rc(0, 0), rc(0, 1), rc(0, 4)), "the pattern struck itself")
        }
        assertTrue(
            step.eliminations.any { it.cell == rc(0, 2) && it.digit == 1 },
            "the third crossing cell kept its marks, though it is in the line like the rest",
        )
    }

    @Test
    fun `the box loses the digits of the set that lies inside it`() {
        val step = assertNotNull(SueDeCoq.find(pattern()))

        val struckInBox = step.eliminations.filter { it.cell % 9 < 3 && it.cell / 9 < 3 && it.cell / 9 > 0 }
        assertTrue(struckInBox.isNotEmpty(), "nothing was struck from the box")
        for ((cell, digit) in struckInBox) {
            assertTrue(digit == 3 || digit == 4, "the box lost $digit, which is not in its set")
            assertTrue(cell != rc(1, 0), "the pattern struck itself")
        }
    }

    @Test
    fun `a crossing one digit short of the count proves nothing`() {
        // Three digits over two cells is a naked pair's worth of information, not this.
        val grid = blank()
            .only(rc(0, 0), 1, 3)
            .only(rc(0, 1), 1, 3)
            .only(rc(0, 4), 1, 2)
            .only(rc(1, 0), 3, 4)

        assertNull(SueDeCoq.find(grid))
    }

    @Test
    fun `two sets that share a digit prove nothing`() {
        val grid = blank()
            .only(rc(0, 0), 1, 3)
            .only(rc(0, 1), 2, 4)
            .only(rc(0, 4), 1, 2)
            .only(rc(1, 0), 2, 4)

        assertNull(SueDeCoq.find(grid))
    }

    @Test
    fun `a set holding a digit from outside the pool proves nothing`() {
        val grid = blank()
            .only(rc(0, 0), 1, 3)
            .only(rc(0, 1), 2, 4)
            .only(rc(0, 4), 1, 9)
            .only(rc(1, 0), 3, 4)

        assertNull(SueDeCoq.find(grid))
    }

    @Test
    fun `the hint carries the whole pattern and both houses`() {
        val step = assertNotNull(SueDeCoq.find(pattern()))

        assertEquals(listOf(rc(0, 0), rc(0, 1), rc(0, 4), rc(1, 0)), step.focusCells)
        assertEquals(2, step.houses.size, "a hint must be able to outline the box and the line")
        assertTrue(step.focusCandidates.all { it.cell in step.focusCells })
    }
}
