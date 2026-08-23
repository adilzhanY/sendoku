package com.sendoku.engine.catalog

import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
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

    /** How many puzzles of each grade the batch holds. */
    public val counts: Map<Grade, Int> get() = byGrade.mapValues { it.value.size }

    public companion object {
        /** Opens a batch. The stream is read fully and closed. */
        public fun from(input: InputStream): CatalogReader = CatalogReader(PuzzleFormat.Body.inflate(input))
    }
}
