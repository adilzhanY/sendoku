package com.sendoku.app.data

import android.content.Context
import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Puzzle
import com.sendoku.engine.Solver
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.CatalogReader
import com.sendoku.engine.catalog.DailyPuzzle
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.PuzzleRef
import com.sendoku.engine.catalog.PuzzleSupply
import com.sendoku.engine.catalog.RatedPuzzle
import com.sendoku.engine.catalog.Supplied
import com.sendoku.engine.technique.TechniqueSolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * A puzzle as handed over, and where in the batch it came from.
 *
 * The index is what lets a puzzle be named in five characters instead of forty. It is null
 * for a puzzle made on the phone or one that arrived as a grid, and those get written out in
 * full instead, which is the whole reason there are two kinds of code.
 */
public data class Dealt(val puzzle: RatedPuzzle, val catalogIndex: Int? = null)

/** Where the next puzzle comes from. */
public interface PuzzleSource {
    public suspend fun next(grade: Grade): Dealt

    /** The puzzle for a given day, which is the same one on every device. */
    public suspend fun daily(epochDay: Long): Dealt

    /**
     * The puzzle a share code names, or null when it names one that is not there.
     *
     * A code that points into the batch is a lookup. A code that carries a grid has to be
     * rated here, because a puzzle nobody has rated is a puzzle the app can say nothing
     * about: not its grade, not what technique it needs, and not whether it is solvable by
     * reasoning at all.
     */
    public suspend fun byCode(ref: PuzzleRef): Dealt?
}

/**
 * The batch that ships inside the app, with live generation behind it.
 *
 * The reader is built once and kept. Inflating a hundred and fifty kilobytes takes a few
 * milliseconds, which is fine on the way into a game and is not fine every time somebody
 * taps for another one.
 */
public class CatalogPuzzleSource(private val open: () -> java.io.InputStream) : PuzzleSource {

    private val supply: PuzzleSupply by lazy {
        PuzzleSupply(reader, GradedGenerator(Dimensions.CLASSIC, Random.Default))
    }

    /**
     * Puzzles this device has already been handed.
     *
     * In memory only, for now. Remembering them across launches means writing them down,
     * which belongs with the rest of the history work rather than here.
     */
    private val played = HashSet<Int>()

    private val reader: CatalogReader by lazy { open().use { CatalogReader.from(it) } }

    override suspend fun next(grade: Grade): Dealt = withContext(Dispatchers.Default) {
        val supplied = supply.take(grade, played, Random.Default)
        if (supplied is Supplied.FromCatalog) played.add(supplied.index)
        Dealt(
            puzzle = supplied.puzzle,
            catalogIndex = when (supplied) {
                is Supplied.FromCatalog -> supplied.index
                is Supplied.Recycled -> supplied.index
                is Supplied.GeneratedLive -> null
            },
        )
    }

    override suspend fun byCode(ref: PuzzleRef): Dealt? = withContext(Dispatchers.Default) {
        when (ref) {
            is PuzzleRef.Batch ->
                if (ref.index in 0 until reader.size) Dealt(reader.puzzleAt(ref.index), ref.index) else null

            is PuzzleRef.Grid -> rate(ref.givens)?.let { Dealt(it) }
        }
    }

    /**
     * A grid that arrived from outside, judged on its merits.
     *
     * Three things have to be true before it can be played, and each of them is a different
     * kind of wrong. It has to have exactly one answer, or it is not a puzzle. It has to be
     * finishable by the technique ladder, or this app cannot hint at it and will not pretend
     * otherwise. And the two have to agree, which they always do, but a code from a stranger
     * is not the place to assume it.
     */
    private fun rate(givens: Board): RatedPuzzle? {
        val dims = givens.dims
        if (!Solver(dims).hasUniqueSolution(givens)) return null
        val report = TechniqueSolver().solve(givens.copy())
        if (!report.isSolved) return null
        return RatedPuzzle(
            puzzle = Puzzle(givens = givens, solution = report.board),
            rating = report.rating,
            grade = report.grade,
            hardest = report.hardest,
            symmetry = Symmetry.NONE,
            usage = report.usage,
        )
    }

    override suspend fun daily(epochDay: Long): Dealt = withContext(Dispatchers.Default) {
        // Never from the live generator and never marked as played: the daily is chosen by
        // the date, and it has to be the same grid whether or not this device has met it.
        val grade = DailyPuzzle.gradeFor(epochDay)
        val choices = reader.indicesOf(grade)
        val index = choices[DailyPuzzle.indexFor(epochDay, choices.size)]
        Dealt(reader.puzzleAt(index), index)
    }

    public companion object {
        /** The batch lives on the classpath, inside the app's own jar. */
        public fun fromResources(): CatalogPuzzleSource = CatalogPuzzleSource {
            checkNotNull(CatalogPuzzleSource::class.java.getResourceAsStream(CATALOG)) {
                "the puzzle batch is missing from the app"
            }
        }

        /** Kept for symmetry with anything that later ships a batch as an Android asset. */
        public fun fromAssets(context: Context, name: String): CatalogPuzzleSource =
            CatalogPuzzleSource { context.assets.open(name) }

        private const val CATALOG = "/catalog/classic.sdkb"
    }
}
