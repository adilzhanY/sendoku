package com.sendoku.engine.catalog

import com.sendoku.engine.Dimensions
import com.sendoku.engine.Generator
import com.sendoku.engine.Grade
import com.sendoku.engine.Symmetry
import com.sendoku.engine.technique.TechniqueSolver
import kotlin.random.Random

/**
 * Makes puzzles of a difficulty you asked for, by making puzzles and throwing most away.
 *
 * There is no way to build a puzzle of a chosen grade directly. Difficulty is a property
 * of the finished grid, discovered by solving it, so the only honest approach is to
 * generate, rate, and keep the ones that landed where you wanted.
 *
 * That is cheap for the common grades and expensive for the rare ones. Roughly half of all
 * generated puzzles come out Gentle and only about one in fifty comes out Tricky, so a
 * Tricky batch costs fifty times what a Gentle batch costs. See [BatchRun] for the way
 * round that: rate every puzzle once and file it under whatever grade it turned out to be,
 * rather than asking for one grade at a time.
 *
 * Puzzles that the ladder cannot finish are dropped, never shipped. Sendoku does not
 * publish a puzzle it cannot explain.
 */
public class GradedGenerator(
    private val dims: Dimensions = Dimensions.CLASSIC,
    private val random: Random = Random.Default,
    private val solver: TechniqueSolver = TechniqueSolver(),
) {

    private val generator = Generator(dims, random)

    /**
     * Makes one puzzle and rates it, whatever grade it turns out to be.
     *
     * Returns null when the ladder cannot finish it, which happens to a few percent of
     * grids and simply means that one is beyond what Sendoku can explain.
     */
    public fun next(symmetry: Symmetry = Symmetry.ROTATIONAL, digFloor: Int = 0): RatedPuzzle? {
        val puzzle = generator.generate(symmetry, minClues = digFloor)
        val report = solver.solve(puzzle.givens)
        if (!report.isSolved) return null
        return RatedPuzzle(
            puzzle = puzzle,
            rating = report.rating,
            grade = report.grade,
            hardest = report.hardest,
            symmetry = symmetry,
            usage = report.usage,
        )
    }

    /**
     * Keeps generating until one lands on [grade], or until [attempts] runs out.
     *
     * Returns null rather than something close. A puzzle filed under the wrong grade is
     * worse than a missing puzzle, because the player learns not to trust the ladder.
     */
    public fun generate(
        grade: Grade,
        symmetry: Symmetry = Symmetry.ROTATIONAL,
        spec: GradeSpec = GradeSpec.of(grade),
        attempts: Int = 20_000,
    ): RatedPuzzle? {
        require(attempts > 0) { "attempts must be positive" }
        repeat(attempts) {
            val candidate = next(symmetry, spec.digFloor) ?: return@repeat
            if (candidate.grade == grade && spec.accepts(candidate.clueCount)) return candidate
        }
        return null
    }
}
