package com.sendoku.engine.technique

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Generator
import org.junit.jupiter.api.Tag
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The one property that matters more than any other: a technique must never be wrong.
 *
 * A rule that misses a step costs a hint. A rule that strikes the digit which actually
 * belongs in a cell destroys the puzzle, and it does so silently, several moves later.
 * So every technique is run against puzzles whose solution is already known.
 */
@Tag("slow")
class TechniqueSoundnessTest {

    /**
     * The whole ladder, taken from the ladder itself.
     *
     * Listing the techniques here by hand meant a new one could be added to the solver and
     * quietly skip the only test that checks it is not wrong. Reading the ladder means the
     * opposite: a technique cannot reach a player without passing through here first.
     */
    private val techniques = Techniques.ladder

    @Test
    fun `no technique ever contradicts the real solution`() {
        var steps = 0
        var eliminations = 0
        var placements = 0

        repeat(40) { seed ->
            val puzzle = Generator(Dimensions.CLASSIC, Random(seed.toLong())).generate()
            val grid = CandidateGrid.of(puzzle.givens)

            while (true) {
                val step = techniques.firstNotNullOfOrNull { it.find(grid) } ?: break
                for ((cell, digit) in step.placements) {
                    assertEquals(
                        puzzle.solution.atIndex(cell),
                        digit,
                        "${step.technique} placed the wrong digit, seed $seed cell $cell",
                    )
                    placements++
                }
                for ((cell, digit) in step.eliminations) {
                    assertTrue(
                        puzzle.solution.atIndex(cell) != digit,
                        "${step.technique} struck the true digit, seed $seed cell $cell digit $digit",
                    )
                    eliminations++
                }
                grid.apply(step)
                assertFalse(grid.hasContradiction, "${step.technique} broke the grid on seed $seed")
                steps++
            }

            // Whatever is left must still hold the solution as a live option everywhere.
            for (cell in 0 until grid.cellCount) {
                if (!grid.isEmpty(cell)) continue
                assertTrue(
                    puzzle.solution.atIndex(cell) in grid.candidatesAt(cell),
                    "seed $seed lost the true digit for cell $cell",
                )
            }
        }

        assertTrue(steps > 100, "only $steps steps fired, the corpus is not exercising the rules")
        assertTrue(placements > 0)
        assertTrue(eliminations > 0)
    }

    /**
     * Just the two singles, and deliberately no more.
     *
     * Running a wing or a fish straight off the givens is useless, because almost no cell
     * has two candidates yet and the rule never fires. Reducing with the full cheap set is
     * useless the other way: the cheap rules finish the job and the rule under test never
     * gets a position either. Singles only is the setting where every rule on the ladder
     * finds real work, which is what makes this test worth running.
     */
    private val basics = listOf(NakedSingle, HiddenSingle)

    /**
     * Rules that never come up in a random corpus, so the firing check below skips them.
     *
     * A grave needs the whole grid to collapse to bivalue cells, which did not happen once
     * in three hundred generated puzzles. It is covered by its own tests, on a grave built
     * by hand, and it stays in the soundness sweep in case it ever does fire.
     */
    private val neverFiresHere = setOf(TechniqueId.BUG_PLUS_ONE)

    /** Applies the basics, minus [except] so the rule under test still has work to do. */
    private fun reduce(grid: CandidateGrid, except: Technique) {
        val rules = basics.filter { it !== except }
        while (true) {
            val step = rules.firstNotNullOfOrNull { it.find(grid) } ?: return
            grid.apply(step)
        }
    }

    @Test
    fun `each technique on its own never contradicts the real solution`() {
        for (technique in techniques) {
            var fired = 0
            repeat(150) { seed ->
                val puzzle = Generator(Dimensions.CLASSIC, Random(2000L + seed)).generate()
                val grid = CandidateGrid.of(puzzle.givens)
                while (true) {
                    reduce(grid, technique)
                    val step = technique.find(grid) ?: break
                    for ((cell, digit) in step.placements) {
                        assertEquals(
                            puzzle.solution.atIndex(cell),
                            digit,
                            "${technique.id} placed the wrong digit, seed $seed cell $cell",
                        )
                    }
                    for ((cell, digit) in step.eliminations) {
                        assertTrue(
                            puzzle.solution.atIndex(cell) != digit,
                            "${technique.id} struck the true digit, seed $seed cell $cell digit $digit",
                        )
                    }
                    grid.apply(step)
                    fired++
                }
                assertFalse(grid.hasContradiction, "${technique.id} broke the grid on seed $seed")
                for (cell in 0 until grid.cellCount) {
                    if (!grid.isEmpty(cell)) continue
                    assertTrue(
                        puzzle.solution.atIndex(cell) in grid.candidatesAt(cell),
                        "${technique.id} lost the true digit for cell $cell on seed $seed",
                    )
                }
            }
            if (technique.id !in neverFiresHere) {
                assertTrue(fired > 0, "${technique.id} never fired anywhere in the corpus")
            }
        }
    }

    @Test
    fun `the cheap rules all get used in an ordinary cheapest first solve`() {
        val used = mutableSetOf<TechniqueId>()
        repeat(60) { seed ->
            val puzzle = Generator(Dimensions.CLASSIC, Random(1000L + seed)).generate()
            val grid = CandidateGrid.of(puzzle.givens)
            while (true) {
                val step = techniques.firstNotNullOfOrNull { it.find(grid) } ?: break
                used.add(step.technique)
                grid.apply(step)
            }
        }
        // The expensive rules are allowed to sit idle here: a cheaper one usually gets there
        // first. What must never happen is a cheap rule going quiet.
        for (id in listOf(
            TechniqueId.NAKED_SINGLE,
            TechniqueId.HIDDEN_SINGLE,
            TechniqueId.LOCKED_CANDIDATES_POINTING,
            TechniqueId.LOCKED_CANDIDATES_CLAIMING,
            TechniqueId.NAKED_PAIR,
        )) {
            assertTrue(id in used, "$id never fired across the whole batch")
        }
    }

    @Test
    fun `these eleven rules finish most ordinary puzzles`() {
        var solved = 0
        val total = 40
        repeat(total) { seed ->
            val puzzle = Generator(Dimensions.CLASSIC, Random(seed.toLong())).generate()
            val grid = CandidateGrid.of(puzzle.givens)
            while (true) {
                val step = techniques.firstNotNullOfOrNull { it.find(grid) } ?: break
                grid.apply(step)
            }
            if (grid.isSolved) {
                assertEquals(puzzle.solution, grid.toBoard(), "seed $seed solved to the wrong grid")
                solved++
            }
        }
        // Not a correctness bar, a sanity bar. If this collapses, a rule has stopped firing.
        assertTrue(solved >= total / 2, "only $solved of $total fell to the first eleven rules")
    }

    @Test
    fun `no technique claims anything about a solved grid`() {
        val solved = CandidateGrid.of(Generator(Dimensions.CLASSIC, Random(9)).completeGrid())
        for (technique in techniques) {
            assertEquals(null, technique.find(solved), "${technique.id} invented a step")
        }
    }

    @Test
    fun `no technique modifies the grid while looking`() {
        val puzzle = Generator(Dimensions.CLASSIC, Random(21)).generate()
        val grid = CandidateGrid.of(puzzle.givens)
        val before = grid.copy()
        for (technique in techniques) technique.find(grid)
        assertEquals(before.toBoard(), grid.toBoard())
        for (cell in 0 until grid.cellCount) {
            assertEquals(before.candidatesAt(cell), grid.candidatesAt(cell), "cell $cell")
        }
    }

    @Test
    fun `every technique id is distinct`() {
        assertEquals(techniques.size, techniques.map { it.id }.toSet().size)
    }

    @Test
    fun `an easy puzzle needs nothing harder than the singles`() {
        val grid = CandidateGrid.of(Board.parse(Dimensions.CLASSIC, EASY))
        val used = mutableSetOf<TechniqueId>()
        while (true) {
            val step = techniques.firstNotNullOfOrNull { it.find(grid) } ?: break
            used.add(step.technique)
            grid.apply(step)
        }
        assertTrue(grid.isSolved)
        // This one happens to fall to naked singles alone, so the claim is that nothing
        // above the singles was needed, not that both fired.
        assertTrue(
            used.all { it == TechniqueId.NAKED_SINGLE || it == TechniqueId.HIDDEN_SINGLE },
            "an easy grid reached for $used",
        )
        assertTrue(TechniqueId.NAKED_SINGLE in used)
    }

    private companion object {
        const val EASY =
            "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79"
    }
}
