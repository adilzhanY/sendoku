package com.sendoku.app.learn

import com.sendoku.engine.Dimensions
import com.sendoku.engine.catalog.CatalogReader
import com.sendoku.engine.technique.TechniqueId
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * How long the player waits for an exercise.
 *
 * The screen says it is looking rather than freezing, so a slow search is not a bug. It is
 * still worth knowing, because a technique that takes ten seconds to find is one nobody will
 * practise twice, and the answer would be to ship those positions rather than search for them.
 */
class PracticeSpeedTest {

    private val dims = Dimensions.CLASSIC

    private fun puzzles(count: Int) = sequence {
        val reader = CatalogReader.from(javaClass.getResourceAsStream("/catalog/classic.sdkb")!!)
        for (index in 0 until minOf(count, reader.size)) yield(reader.puzzleAt(index))
    }

    @Test
    fun `every technique can be found, and how long it takes is printed`() {
        val slow = mutableListOf<String>()
        // The cage rules are practised on a Killer board, which is a later release. This
        // is about the rules an ordinary puzzle can need.
        for (technique in TechniqueId.entries.filterNot { it.isCage }) {
            var found: Exercise? = null
            val took = measureTimeMillis { found = PracticePositions.find(technique, puzzles(SEARCH), dims) }
            val state = if (found == null) "NOT FOUND" else "ok"
            println("PRACTICE ${technique.name} ${took}ms $state")
            if (found == null || took > SLOW_MILLIS) slow += "${technique.name} ${took}ms $state"
        }
        assertTrue(
            "these were slow or missing, so they may need shipping rather than searching: $slow",
            slow.isEmpty(),
        )
    }

    private companion object {
        /**
         * How many puzzles the search may walk.
         *
         * The whole batch, because BUG plus one needs it. Twenty three of the twenty four
         * techniques turn up in the first dozen puzzles in under thirty milliseconds. BUG plus
         * one is genuinely rare, appears nowhere in the first four hundred, and costs about
         * three hundred milliseconds over the full three thousand. Still comfortably under a
         * glance, so it is searched for like the rest rather than shipped as a fixed position.
         */
        const val SEARCH = 3000

        /** Longer than this and a player is waiting rather than glancing. */
        const val SLOW_MILLIS = 3_000L
    }
}
