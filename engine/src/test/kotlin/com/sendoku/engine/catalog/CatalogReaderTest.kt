package com.sendoku.engine.catalog

import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The loader, checked against the batch job that wrote the file.
 *
 * This is the seam where an offline job on a desktop meets a phone reading the result, and
 * a mistake here is invisible until a player opens a puzzle that is not the one the rater
 * approved. So it is checked both ways: against the batch as generated, and against the
 * batch as actually shipped.
 */
class CatalogReaderTest {

    private val classic = Dimensions.CLASSIC

    private fun shipped(): CatalogReader =
        checkNotNull(javaClass.getResourceAsStream("/catalog/classic.sdkb")).use { CatalogReader.from(it) }

    @Test
    fun `the loader agrees with the generator that produced the batch`() {
        val result = BatchRun.run(
            BatchRequest(
                targets = mapOf(Grade.GENTLE to 6, Grade.STEADY to 4, Grade.SEVERE to 4),
                seed = 601,
                workers = 4,
            ),
        )
        val bytes = ByteArrayOutputStream().also { PuzzleFormat.write(it, classic, result.puzzles) }
        val reader = CatalogReader.from(ByteArrayInputStream(bytes.toByteArray()))

        assertEquals(result.puzzles.size, reader.size)
        assertEquals(classic, reader.dims)
        for ((index, generated) in result.puzzles.withIndex()) {
            val loaded = reader.puzzleAt(index)
            assertEquals(generated.puzzle.givens, loaded.puzzle.givens, "puzzle $index givens")
            assertEquals(generated.puzzle.solution, loaded.puzzle.solution, "puzzle $index solution")
            assertEquals(generated.rating, loaded.rating, absoluteTolerance = 1e-9)
            assertEquals(generated.grade, loaded.grade)
            assertEquals(generated.hardest, loaded.hardest)
            assertEquals(generated.symmetry, loaded.symmetry)
            assertEquals(generated.usage, loaded.usage)
        }
    }

    @Test
    fun `reading one puzzle gives the same answer as reading them all`() {
        val reader = shipped()
        val whole = checkNotNull(javaClass.getResourceAsStream("/catalog/classic.sdkb"))
            .use { PuzzleFormat.read(it) }

        assertEquals(whole.puzzles.size, reader.size)
        for (index in whole.puzzles.indices) {
            assertEquals(whole.puzzles[index], reader.puzzleAt(index), "puzzle $index")
        }
    }

    @Test
    fun `a grade can be read without decoding a single puzzle`() {
        val reader = shipped()
        for (index in 0 until reader.size) {
            assertEquals(Grade.of(reader.ratingAt(index)), reader.gradeAt(index))
        }
        // And the cheap read agrees with the expensive one.
        for (index in listOf(0, 1, reader.size / 2, reader.size - 1)) {
            assertEquals(reader.puzzleAt(index).grade, reader.gradeAt(index))
            assertEquals(reader.puzzleAt(index).rating, reader.ratingAt(index), absoluteTolerance = 1e-9)
        }
    }

    @Test
    fun `the index by grade covers every puzzle exactly once`() {
        val reader = shipped()
        val seen = Grade.entries.flatMap { reader.indicesOf(it) }
        assertEquals(reader.size, seen.size)
        assertEquals(reader.size, seen.toSet().size)
        for (grade in Grade.entries) {
            assertEquals(500, reader.indicesOf(grade).size, "${grade.displayName} count")
            for (index in reader.indicesOf(grade)) assertEquals(grade, reader.gradeAt(index))
        }
        assertEquals(Grade.entries.associateWith { 500 }, reader.counts)
    }

    @Test
    fun `reading past the end is refused rather than returning nonsense`() {
        val reader = shipped()
        assertFailsWith<IllegalArgumentException> { reader.puzzleAt(-1) }
        assertFailsWith<IllegalArgumentException> { reader.puzzleAt(reader.size) }
        assertFailsWith<IllegalArgumentException> { reader.ratingAt(reader.size) }
    }

    @Test
    fun `a batch whose length does not match its own header is refused`() {
        val bytes = ByteArrayOutputStream().also {
            PuzzleFormat.write(it, classic, BatchRun.run(
                BatchRequest(targets = mapOf(Grade.GENTLE to 3), seed = 603, workers = 2),
            ).puzzles)
        }.toByteArray()
        assertFailsWith<java.io.IOException> {
            CatalogReader.from(ByteArrayInputStream(bytes.copyOf(bytes.size - 30)))
        }
    }

    @Test
    fun `reading one puzzle is far cheaper than reading the batch`() {
        val bytes = checkNotNull(javaClass.getResourceAsStream("/catalog/classic.sdkb")).use { it.readBytes() }
        val reader = CatalogReader.from(ByteArrayInputStream(bytes))
        repeat(3) { reader.puzzleAt(it) }

        val single = kotlin.system.measureNanoTime { repeat(200) { reader.puzzleAt(it * 7) } } / 200
        val whole = kotlin.system.measureNanoTime {
            PuzzleFormat.read(ByteArrayInputStream(bytes))
        }
        println("LOADER one puzzle ${single / 1000}us, whole batch ${whole / 1_000_000}ms")
        assertTrue(single * 100 < whole, "reading one puzzle is not meaningfully cheaper")
    }
}
