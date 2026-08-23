package com.sendoku.app.data

import android.content.Context
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.catalog.CatalogReader
import com.sendoku.engine.catalog.DailyPuzzle
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.PuzzleSupply
import com.sendoku.engine.catalog.RatedPuzzle
import com.sendoku.engine.catalog.Supplied
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Where the next puzzle comes from. */
public interface PuzzleSource {
    public suspend fun next(grade: Grade): RatedPuzzle

    /** The puzzle for a given day, which is the same one on every device. */
    public suspend fun daily(epochDay: Long): RatedPuzzle
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

    override suspend fun next(grade: Grade): RatedPuzzle = withContext(Dispatchers.Default) {
        val supplied = supply.take(grade, played, Random.Default)
        if (supplied is Supplied.FromCatalog) played.add(supplied.index)
        supplied.puzzle
    }

    override suspend fun daily(epochDay: Long): RatedPuzzle = withContext(Dispatchers.Default) {
        // Never from the live generator and never marked as played: the daily is chosen by
        // the date, and it has to be the same grid whether or not this device has met it.
        val grade = DailyPuzzle.gradeFor(epochDay)
        val choices = reader.indicesOf(grade)
        reader.puzzleAt(choices[DailyPuzzle.indexFor(epochDay, choices.size)])
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
