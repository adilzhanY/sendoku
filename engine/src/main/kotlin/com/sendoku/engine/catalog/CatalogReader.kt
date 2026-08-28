package com.sendoku.engine.catalog

import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.technique.TechniqueId
import java.io.InputStream

/**
 * Reads one puzzle out of a batch without building the other two thousand nine hundred.
 *
 * The app opens a batch to play a single puzzle. Turning the whole file into objects to get
 * at one of them would cost a few megabytes of allocation and a visible pause on a cheap
 * phone, every time somebody starts a game.
 *
 * The file has to be decompressed in one go, because that is what gzip is. What this avoids
 * is the expensive half: records are a fixed width, so puzzle `n` sits at a known offset and
 * only that record is ever decoded. Asking a puzzle's grade is cheaper still, since the
 * rating is two bytes at a known place and the rest of the record is never read.
 */
public class CatalogReader private constructor(private val body: PuzzleFormat.Body) {

    public val dims: Dimensions get() = body.dims

    /** How many puzzles the batch holds. */
    public val size: Int get() = body.count

    /** Which puzzles belong to which grade. Built on first use from the ratings alone. */
    private val byGrade: Map<Grade, List<Int>> by lazy {
        val buckets = HashMap<Grade, MutableList<Int>>()
        for (index in 0 until size) {
            buckets.getOrPut(gradeAt(index)) { ArrayList() }.add(index)
        }
        buckets
    }

    /** The rating of one puzzle, read without decoding it. */
    public fun ratingAt(index: Int): Double = body.ratingAt(index)

    /** The grade of one puzzle, read without decoding it. */
    public fun gradeAt(index: Int): Grade = Grade.of(body.ratingAt(index))

    /** Decodes one puzzle. */
    public fun puzzleAt(index: Int): RatedPuzzle = body.decode(index)

    /** The indices of every puzzle of [grade], in file order. */
    public fun indicesOf(grade: Grade): List<Int> = byGrade[grade].orEmpty()

    /**
     * Which puzzles need which technique at their hardest, built on first use.
     *
     * The same shape as the grade index and for the same reason: reading one byte per puzzle
     * is cheap, and decoding four thousand grids to answer "which of these needs an X-Wing"
     * is not. A puzzle appears under exactly one technique, the hardest one it forces.
     */
    private val byHardest: Map<TechniqueId, List<Int>> by lazy {
        val buckets = HashMap<TechniqueId, MutableList<Int>>()
        for (index in 0 until size) {
            val hardest = body.hardestAt(index) ?: continue
            buckets.getOrPut(hardest) { ArrayList() }.add(index)
        }
        buckets
    }

    /** The hardest technique one puzzle needs, read without decoding it. */
    public fun hardestAt(index: Int): TechniqueId? = body.hardestAt(index)

    /** The indices of every puzzle whose hardest technique is [technique], in file order. */
    public fun indicesNeeding(technique: TechniqueId): List<Int> = byHardest[technique].orEmpty()

    /** How many puzzles in the batch top out at each technique. */
    public val needing: Map<TechniqueId, Int> get() = byHardest.mapValues { it.value.size }

    /** How many puzzles of each grade the batch holds. */
    public val counts: Map<Grade, Int> get() = byGrade.mapValues { it.value.size }

    public companion object {
        /** Opens a batch. The stream is read fully and closed. */
        public fun from(input: InputStream): CatalogReader = CatalogReader(PuzzleFormat.Body.inflate(input))
    }
}
