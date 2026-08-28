package com.sendoku.engine.killer

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates
import com.sendoku.engine.Dimensions
import com.sendoku.engine.technique.SolveOutcome
import com.sendoku.engine.technique.TechniqueId
import com.sendoku.engine.technique.apply
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The cage rules, one at a time.
 *
 * A cage technique that is wrong is worse than one that is missing: it eliminates a digit
 * that was the answer, and the puzzle dies twenty moves later with nothing to show why. So
 * each one is checked against a hand built position where the answer is known, and the whole
 * ladder is checked against generated puzzles where the solution is known in advance.
 */
class CageTechniquesTest {

    private val classic = Dimensions.CLASSIC

    /** An empty grid with cages over it, which is what a Killer starts as. */
    private fun position(cages: List<Cage>, solution: Board): Pair<CandidateGrid, KillerPuzzle> {
        val puzzle = KillerPuzzle(classic, cages, solution)
        val grid = checkNotNull(CandidateGrid.ofOrNull(Board(classic)))
        return grid to puzzle
    }

    /** A solved grid, which is all any of these need to be built against. */
    private fun solved(seed: Int = 7): Board {
        val generator = KillerGenerator(classic, Random(seed))
        return checkNotNull(generator.next()).solution
    }

    @Test
    fun `a cage sum rules out every digit no combination needs`() {
        val solution = solved()
        // Three cells adding to six can only be 1, 2 and 3.
        val cells = listOf(0, 1, 2)
        val sum = cells.sumOf { solution.atIndex(it) }
        val cages = cagesAround(solution, Cage(sum, cells))
        val (grid, puzzle) = position(cages, solution)

        val step = CageSum.find(grid, puzzle)
        assertNotNull(step, "a cage of three said nothing at all")
        assertEquals(TechniqueId.CAGE_SUM, step.technique)
        // Whichever cage it fired on, and there are eighty one of them here, the rule is the
        // same: it may only strike inside that cage, and only digits no combination needs.
        val fired = step.focusCells
        val firedSum = fired.sumOf { solution.atIndex(it) }
        val possible = Combinations.possibleDigits(fired.size, firedSum)
        for ((cell, digit) in step.eliminations) {
            assertTrue(cell in fired, "it struck a digit outside the cage")
            assertTrue(possible and (1 shl (digit - 1)) == 0, "it struck a digit the cage could hold")
        }
        assertTrue(cells.isNotEmpty() && sum > 0)
    }

    @Test
    fun `a cage sum never strikes the digit that belongs there`() {
        // The test that matters. Everything else about a technique can be imperfect; this
        // cannot, because striking the answer breaks the puzzle silently.
        val solution = solved(11)
        val cages = KillerGenerator(classic, Random(11)).next()?.cages.orEmpty()
        val (grid, puzzle) = position(cages, solution)
        repeat(60) {
            val step = CageTechniques.ladder.firstNotNullOfOrNull { it.find(grid, puzzle) } ?: return
            for ((cell, digit) in step.eliminations) {
                assertTrue(
                    digit != solution.atIndex(cell),
                    "${step.technique} struck the answer out of cell $cell",
                )
            }
            for ((cell, digit) in step.placements) {
                assertEquals(solution.atIndex(cell), digit, "${step.technique} placed the wrong digit")
            }
            grid.apply(step)
        }
    }

    @Test
    fun `innies and outies name the cell a house does not settle`() {
        val solution = solved(3)
        val puzzle = checkNotNull(KillerGenerator(classic, Random(3)).next())
        val grid = checkNotNull(CandidateGrid.ofOrNull(Board(classic)))
        var found: com.sendoku.engine.technique.Deduction? = null
        repeat(200) {
            found = InniesAndOuties.find(grid, puzzle)
            if (found != null) return@repeat
            val step = CageTechniques.ladder.firstNotNullOfOrNull { it.find(grid, puzzle) }
                ?: com.sendoku.engine.technique.Techniques.availableOn(grid.toBoard())
                ?: return@repeat
            grid.apply(step)
        }
        val step = found
        if (step != null) {
            for ((cell, digit) in step.placements) {
                assertEquals(puzzle.solution.atIndex(cell), digit, "an innie named the wrong digit")
            }
        }
    }

    @Test
    fun `a locked cage never strikes a digit outside its house`() {
        val puzzle = checkNotNull(KillerGenerator(classic, Random(5)).next())
        val grid = checkNotNull(CandidateGrid.ofOrNull(Board(classic)))
        repeat(80) {
            val step = CageLocked.find(grid, puzzle)
            if (step != null) {
                for ((cell, digit) in step.eliminations) {
                    assertTrue(
                        digit != puzzle.solution.atIndex(cell),
                        "a locked cage struck the answer out of cell $cell",
                    )
                }
            }
            val next = CageTechniques.ladder.firstNotNullOfOrNull { it.find(grid, puzzle) } ?: return
            grid.apply(next)
        }
    }

    @Test
    fun `the combinations are the ones a solver would write down`() {
        // Three cells adding to six: one set, 1 2 3. Two adding to seventeen: one set, 8 9.
        assertEquals(listOf(setOf(1, 2, 3)), Combinations.of(3, 6).map { Candidates(it).toList().toSet() })
        assertEquals(listOf(setOf(8, 9)), Combinations.of(2, 17).map { Candidates(it).toList().toSet() })
        // Nothing at all adds to two in three cells.
        assertEquals(emptyList(), Combinations.of(3, 2))
        // Every digit appears somewhere in a five cell cage adding to twenty five.
        assertEquals(0b111111111, Combinations.possibleDigits(5, 25))
    }

    @Test
    fun `a cage cannot be dealt digits its cells will not take`() {
        // 1, 2 and 3 add to six, but not if no cell in the cage can hold a 1.
        val allowed = listOf(0b110, 0b110, 0b110)
        assertEquals(emptyList(), Combinations.fitting(6, allowed))
        // One cell can only be a 1 and another only a 9, so twelve has exactly one way to
        // be made: 1, 2 and 9.
        assertEquals(1, Combinations.fitting(12, listOf(0b000000001, 0b100000000, 0b111111111)).size)
    }

    @Test
    fun `the rater finishes a generated killer and says what it needed`() {
        val puzzle = checkNotNull(KillerGenerator(classic, Random(21)).next())
        val report = KillerRater(puzzle).solve()
        if (report.isSolved) {
            assertEquals(puzzle.solution.toString(), report.board.toString())
            assertNotNull(report.hardest)
            assertTrue(report.rating > 0.0)
            assertTrue(report.usage.values.sum() == report.steps.size)
        } else {
            // Being stuck is a legitimate answer: it means this puzzle needs a rule the
            // ladder does not have yet, and shipping it would mean hints that give up.
            assertEquals(SolveOutcome.STUCK, report.outcome)
        }
    }

    @Test
    fun `nothing is claimed about a board the rules cannot touch`() {
        val solution = solved(31)
        // One cage covering the whole grid, which no rule can say anything useful about.
        val cages = listOf(Cage((0 until classic.cellCount).sumOf { solution.atIndex(it) }, (0 until 81).toList()))
        val puzzle = KillerPuzzle(classic, cages, solution)
        val grid = checkNotNull(CandidateGrid.ofOrNull(Board(classic)))
        assertNull(CageSingle.find(grid, puzzle))
    }

    /** The rest of the grid as one cell cages, so the puzzle is legal and the test is about one cage. */
    private fun cagesAround(solution: Board, cage: Cage): List<Cage> {
        val rest = (0 until classic.cellCount).filter { it !in cage.cells }
            .map { Cage(solution.atIndex(it), listOf(it)) }
        return listOf(cage) + rest
    }
}
