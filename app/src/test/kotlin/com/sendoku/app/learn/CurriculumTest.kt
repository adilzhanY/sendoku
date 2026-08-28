package com.sendoku.app.learn

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Solver
import com.sendoku.engine.killer.CageTechniques
import com.sendoku.engine.killer.KillerPuzzle
import com.sendoku.engine.killer.KillerSolver
import com.sendoku.engine.technique.Techniques
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The course, checked against the engine rather than against a proofread.
 *
 * A lesson is prose next to a board, and prose is exactly the kind of thing that stays wrong
 * for a year because everybody who reads it already knows the answer. So the parts a machine
 * can check, it checks: the boards are legal, the digits the lessons place are the digits the
 * solution wants, and the order the techniques are taught in is the order the engine costs
 * them.
 *
 * If the course and the solver ever disagree, the solver is right and the course is a bug.
 */
class CurriculumTest {

    @Test
    fun `every lesson has its own name`() {
        val ids = Curriculum.lessons.map { it.id }
        assertEquals("two lessons share an id, so progress would overwrite itself", ids.distinct(), ids)
    }

    @Test
    fun `every lesson board is a legal position with one solution`() {
        for (lesson in Curriculum.lessons) {
            val board = Board.parse(lesson.dims, lesson.board)
            // A Killer lesson starts from an empty grid, because a Killer has no clues at
            // all: what makes its answer the only one is the cages, so that is what is asked.
            if (lesson.cages.isNotEmpty()) {
                assertEquals(
                    "${lesson.id} draws cages with more than one answer",
                    1,
                    KillerSolver.countSolutions(lesson.dims, lesson.cages, limit = 2),
                )
                continue
            }
            val solver = Solver(lesson.dims)
            assertTrue("${lesson.id} starts from a position that breaks the rules", solver.isLegal(board))
            assertTrue("${lesson.id} starts from a position with more than one answer", solver.hasUniqueSolution(board))
        }
    }

    @Test
    fun `every digit a lesson places is the digit the answer wants`() {
        for (lesson in Curriculum.lessons) {
            val board = Board.parse(lesson.dims, lesson.board)
            val solution = if (lesson.cages.isNotEmpty()) {
                KillerSolver.solve(lesson.dims, lesson.cages)
            } else {
                Solver(lesson.dims).solve(board)
            }
            assertNotNull("${lesson.id} cannot be solved", solution)
            requireNotNull(solution)
            for (step in lesson.steps) {
                val (cell, digit, what) = when (step) {
                    is Step.Place -> Triple(step.cell, step.digit, "places")
                    is Step.YourTurn -> Triple(step.cell, step.digit, "asks for")
                    else -> continue
                }
                assertEquals(
                    "${lesson.id} $what $digit in cell $cell, the answer there is ${solution.atIndex(cell)}",
                    solution.atIndex(cell),
                    digit,
                )
            }
        }
    }

    @Test
    fun `a lesson never places a digit into a given`() {
        for (lesson in Curriculum.lessons) {
            val board = Board.parse(lesson.dims, lesson.board)
            for (step in lesson.steps) {
                val cell = when (step) {
                    is Step.Place -> step.cell
                    is Step.YourTurn -> step.cell
                    else -> continue
                }
                assertEquals(
                    "${lesson.id} writes into cell $cell, which already holds a given",
                    Board.EMPTY,
                    board.atIndex(cell),
                )
            }
        }
    }

    @Test
    fun `every cell a lesson points at is on the board`() {
        for (lesson in Curriculum.lessons) {
            val cells = lesson.dims.cellCount
            val touched = lesson.steps.flatMap { step ->
                when (step) {
                    is Step.Show -> step.focus + step.strike
                    is Step.Place -> setOf(step.cell)
                    is Step.YourTurn -> setOf(step.cell)
                    is Step.Say -> emptySet()
                }
            }
            for (cell in touched) {
                assertTrue("${lesson.id} points at cell $cell, the grid has $cells", cell in 0 until cells)
            }
        }
    }

    @Test
    fun `inside a stage, techniques are taught in the order the engine costs them`() {
        // Not one flat list. The ladder puts an X-Wing at 3.2 and a hidden pair at 3.4, so a
        // strict reading would teach a fish in the middle of the subsets. Families are taught
        // together and the stages are what the ladder orders.
        for (stage in Stage.entries) {
            val taught = Curriculum.of(stage).flatMap { it.teaches }
            val expected = Curriculum.ladderOrder.filter { it in taught }
            assertEquals("$stage teaches its techniques out of the engine's order", expected, taught)
        }
    }

    @Test
    fun `stages run in order of their cheapest technique`() {
        val rungs = Stage.entries
            .map { stage -> stage to Curriculum.of(stage).flatMap { it.teaches } }
            .filter { it.second.isNotEmpty() }
            .map { (stage, taught) -> stage to taught.minOf { Curriculum.ladderOrder.indexOf(it) } }
        for ((earlier, later) in rungs.zipWithNext()) {
            assertTrue(
                "${later.first} starts cheaper than ${earlier.first}, so the course goes backwards",
                later.second > earlier.second,
            )
        }
    }

    @Test
    fun `no technique is taught twice`() {
        val taught = Curriculum.lessons.flatMap { it.teaches }
        assertEquals("a technique has two lessons", taught.distinct(), taught)
    }

    @Test
    fun `every technique the engine knows has a lesson`() {
        val taught = Curriculum.lessons.flatMap { it.teaches }.toSet()
        val missing = Curriculum.ladderOrder.filterNot { it in taught }
        assertTrue("the course never teaches: $missing", missing.isEmpty())
    }

    @Test
    fun `stages run in order and none is interleaved`() {
        val stages = Curriculum.lessons.map { it.stage }
        assertEquals(
            "a stage is split, so the course map would show its lessons in two places",
            stages.distinct(),
            stages.fold(emptyList<Stage>()) { seen, stage ->
                if (seen.lastOrNull() == stage) seen else seen + stage
            },
        )
        assertEquals(
            "the stages appear out of the order the enum declares",
            stages.distinct().sortedBy { it.ordinal },
            stages.distinct(),
        )
    }

    @Test
    fun `every lesson says something before it asks for anything`() {
        for (lesson in Curriculum.lessons) {
            assertTrue("${lesson.id} has no steps at all", lesson.steps.isNotEmpty())
            val firstAsk = lesson.steps.indexOfFirst { it is Step.YourTurn }
            if (firstAsk >= 0) {
                assertTrue("${lesson.id} asks the player for a digit before explaining anything", firstAsk > 0)
            }
        }
    }

    @Test
    fun `the course starts small and only then gets bigger`() {
        // Sixteen cells before eighty one, so the first sentence about a row is one the player
        // can check by looking rather than one they have to believe.
        // The Killer stage starts small again, on purpose and for the same reason: a row of
        // six adding to twenty one is a sentence somebody can check by eye, and forty five
        // across nine cells is a claim they have to take on trust. So it is judged on its
        // own rather than as a step backwards in the ordinary course.
        val classic = Curriculum.lessons.filter { it.stage != Stage.CAGES }.map { it.dims.cellCount }
        assertEquals("the course does not start on the smallest grid", 16, classic.first())
        assertEquals("grid sizes go up and down through the course", classic.sorted(), classic)
        val killer = Curriculum.of(Stage.CAGES).map { it.dims.cellCount }
        assertTrue("the Killer lessons do not start small", killer.all { it <= 36 })
    }

    @Test
    fun `a lesson that names a technique shows a position where the engine agrees it applies`() {
        // The point of the whole course. A lesson may not claim a naked single sits in a cell
        // unless the solver, asked independently, finds the same one. Anything else is the app
        // teaching a rule its own hints would contradict.
        for (lesson in Curriculum.lessons) {
            if (lesson.teaches.isEmpty()) continue
            // A cage rule is checked against the cage ladder, on the board the lesson draws,
            // for the same reason and to the same standard.
            if (lesson.cages.isNotEmpty()) {
                assertCageLessonAgrees(lesson)
                continue
            }
            val grid = CandidateGrid.ofOrNull(Board.parse(lesson.dims, lesson.board))
            assertNotNull("${lesson.id} starts from a position with a contradiction in it", grid)
            requireNotNull(grid)

            val shown = lesson.steps.filterIsInstance<Step.Show>().flatMap { it.focus }.toSet()
            for (technique in lesson.teaches) {
                val finder = Techniques.ladder.first { it.id == technique }
                val found = finder.find(grid)
                assertNotNull("${lesson.id} teaches $technique on a board where it does not apply", found)
                requireNotNull(found)

                val agreed = found.placements.map { it.cell }.toSet() + found.focusCells
                assertTrue(
                    "${lesson.id} points at $shown, the engine's $technique is at $agreed",
                    shown.any { it in agreed },
                )
            }
        }
    }

    /** The Killer half of the test above: the cage ladder has to agree with the lesson. */
    private fun assertCageLessonAgrees(lesson: Lesson) {
        val solution = checkNotNull(KillerSolver.solve(lesson.dims, lesson.cages)) {
            "${lesson.id} draws cages that describe nothing"
        }
        val puzzle = KillerPuzzle(lesson.dims, lesson.cages, solution)
        val grid = checkNotNull(CandidateGrid.ofOrNull(Board.parse(lesson.dims, lesson.board)))
        val shown = lesson.steps.filterIsInstance<Step.Show>().flatMap { it.focus }.toSet()
        for (technique in lesson.teaches) {
            val finder = CageTechniques.ladder.first { it.id == technique }
            val found = finder.find(grid, puzzle)
            assertNotNull("${lesson.id} teaches $technique on a board where it does not apply", found)
            requireNotNull(found)
            val agreed = found.placements.map { it.cell }.toSet() + found.focusCells
            assertTrue(
                "${lesson.id} points at $shown, the engine's $technique is at $agreed",
                shown.any { it in agreed },
            )
        }
    }

    @Test
    fun `the first lessons teach the rules and no technique`() {
        val firstSteps = Curriculum.of(Stage.FIRST_STEPS)
        assertTrue("the course has no opening stage", firstSteps.isNotEmpty())
        for (lesson in firstSteps) {
            assertTrue("${lesson.id} names a technique before the rules are taught", lesson.teaches.isEmpty())
        }
    }
}
