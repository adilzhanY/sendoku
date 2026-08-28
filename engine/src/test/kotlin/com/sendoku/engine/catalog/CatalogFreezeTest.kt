package com.sendoku.engine.catalog

import com.sendoku.engine.Dimensions
import org.junit.jupiter.api.Tag
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The batch is frozen, and this is the promise.
 *
 * A short share code is a puzzle's index in the shipped batch, written down and handed to
 * somebody else. That makes the order of the file part of the app's contract with its players
 * rather than an implementation detail: regenerate the batch, and every code anybody ever sent
 * opens a different puzzle. [SavedGame] refuses to store an index for exactly this reason, and
 * its comment says so.
 *
 * So from the first release the rule is: puzzles may be appended to the end of the batch, and
 * never reordered, replaced or removed. This test holds the rule by fingerprinting the first
 * four thousand records. Adding to the end leaves the fingerprint alone. Anything else fails
 * here, loudly, with a message saying what it just broke.
 *
 * If a puzzle in the batch ever genuinely has to change, the fix is not to edit this number.
 * It is to bump [PuzzleCode.BATCH_VERSION], which makes every old code read as belonging to a
 * batch this app no longer has, and to say so in the release notes.
 */
@Tag("slow")
class CatalogFreezeTest {

    /**
     * The first four thousand records of the batch as shipped in version 1.0.
     *
     * Taken over the decoded givens and solution of each puzzle in file order, so it changes
     * if a puzzle changes or if two of them swap places, and does not change if the format
     * around them is rewritten.
     */
    private val frozen = "f31121b1460df0fee67905da957098f2"

    private val catalog: PuzzleCatalog by lazy {
        val stream = checkNotNull(javaClass.getResourceAsStream("/catalog/classic.sdkb")) {
            "the shipped catalog is missing, run ./gradlew :engine:generateCatalog"
        }
        stream.use { PuzzleFormat.read(it) }
    }

    private fun fingerprint(count: Int): String {
        val digest = MessageDigest.getInstance("SHA-256")
        for (index in 0 until count) {
            val puzzle = catalog.puzzles[index].puzzle
            digest.update(puzzle.givens.toString().toByteArray())
            digest.update(puzzle.solution.toString().toByteArray())
        }
        return digest.digest().joinToString("") { "%02x".format(it) }.take(32)
    }

    @Test
    fun `the first four thousand puzzles are the ones that shipped`() {
        assertTrue(
            catalog.puzzles.size >= FROZEN_COUNT,
            "the batch has shrunk to ${catalog.puzzles.size}: puzzles are only ever added",
        )
        assertEquals(
            frozen,
            fingerprint(FROZEN_COUNT),
            "the shipped batch has been reordered or regenerated, which invalidates every " +
                "share code anybody has ever sent. Puzzles may only ever be appended.",
        )
    }

    @Test
    fun `every shipped puzzle can be named by a short code and found again`() {
        for (index in catalog.puzzles.indices) {
            val code = PuzzleCode.forBatch(index)
            val read = PuzzleCode.read(code)
            assertTrue(read is CodeResult.Ok, "puzzle $index came back as $read")
            assertEquals(PuzzleRef.Batch(PuzzleCode.BATCH_VERSION, index), read.ref)
        }
    }

    @Test
    fun `every shipped puzzle also round trips as a grid`() {
        // The long code is the fallback for anything not in the batch, but it has to be right
        // for the batch too: a player who shares an entered puzzle and a player who shares a
        // dealt one are using the same reader.
        for (rated in catalog.puzzles) {
            val code = PuzzleCode.forGrid(rated.puzzle.givens)
            val read = PuzzleCode.read(code, Dimensions.CLASSIC)
            assertTrue(read is CodeResult.Ok, "a shipped puzzle came back as $read")
            val back = (read.ref as PuzzleRef.Grid).givens
            assertEquals(rated.puzzle.givens.toString(), back.toString())
        }
    }

    private companion object {
        /** What shipped in 1.0. Later releases add to the end, so this number stays. */
        const val FROZEN_COUNT = 4000
    }
}
