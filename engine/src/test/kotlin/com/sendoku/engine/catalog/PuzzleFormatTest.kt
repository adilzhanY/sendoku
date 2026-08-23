package com.sendoku.engine.catalog

import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Symmetry
import com.sendoku.engine.technique.TechniqueId
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PuzzleFormatTest {

    private val classic = Dimensions.CLASSIC

    private fun sample(count: Int, seed: Int = 201): List<RatedPuzzle> {
        val maker = GradedGenerator(classic, Random(seed.toLong()))
        val out = ArrayList<RatedPuzzle>()
        var symmetry = 0
        while (out.size < count) {
            val rated = maker.next(Symmetry.entries[symmetry % Symmetry.entries.size]) ?: continue
            symmetry++
            out.add(rated)
        }
        return out
    }

    private fun roundTrip(puzzles: List<RatedPuzzle>): PuzzleCatalog {
        val bytes = ByteArrayOutputStream()
        PuzzleFormat.write(bytes, classic, puzzles)
        return PuzzleFormat.read(ByteArrayInputStream(bytes.toByteArray()))
    }

    @Test
    fun `a batch survives the round trip exactly`() {
        val original = sample(25)
        val catalog = roundTrip(original)

        assertEquals(classic, catalog.dims)
        assertEquals(original.size, catalog.puzzles.size)
        for ((before, after) in original.zip(catalog.puzzles)) {
            assertEquals(before.puzzle.givens, after.puzzle.givens)
            assertEquals(before.puzzle.solution, after.puzzle.solution)
            assertEquals(before.rating, after.rating, absoluteTolerance = 1e-9)
            assertEquals(before.grade, after.grade)
            assertEquals(before.hardest, after.hardest)
            assertEquals(before.symmetry, after.symmetry)
            assertEquals(before.usage, after.usage)
        }
    }

    @Test
    fun `an empty batch is still a valid file`() {
        val catalog = roundTrip(emptyList())
        assertEquals(emptyList(), catalog.puzzles)
        assertEquals(classic, catalog.dims)
    }

    @Test
    fun `the givens are always a subset of the solution`() {
        for (rated in roundTrip(sample(15)).puzzles) {
            for (cell in 0 until 81) {
                val given = rated.puzzle.givens.atIndex(cell)
                if (given == Board.EMPTY) continue
                assertEquals(rated.puzzle.solution.atIndex(cell), given, "cell $cell")
            }
        }
    }

    @Test
    fun `packing beats writing the digits out as text`() {
        val puzzles = sample(200)
        val packed = ByteArrayOutputStream().also { PuzzleFormat.write(it, classic, puzzles) }.size()
        val asText = puzzles.sumOf {
            it.puzzle.givens.toString().replace("\n", "").length +
                it.puzzle.solution.toString().replace("\n", "").length + 2
        }
        println("FORMAT ${puzzles.size} puzzles: $packed bytes packed, $asText bytes as text")
        assertTrue(packed < asText / 3, "packed $packed bytes is not much better than $asText")
    }

    @Test
    fun `records are a fixed width so one puzzle can be read on its own`() {
        val width = PuzzleFormat.recordBytes(classic)
        assertEquals(41 + 11 + 2 + 1 + 1 + TechniqueId.entries.size, width)
        // The uncompressed size is the header plus one record each, exactly.
        val puzzles = sample(10)
        val raw = ByteArrayOutputStream()
        java.util.zip.GZIPInputStream(
            ByteArrayInputStream(
                ByteArrayOutputStream().also { PuzzleFormat.write(it, classic, puzzles) }.toByteArray(),
            ),
        ).use { it.copyTo(raw) }
        assertEquals(PuzzleFormat.HEADER_BYTES + puzzles.size * width, raw.size())
    }

    @Test
    fun `a file that is not a batch is refused`() {
        val rubbish = ByteArrayOutputStream()
        java.util.zip.GZIPOutputStream(rubbish).use { it.write("hello there".toByteArray()) }
        assertFailsWith<IOException> { PuzzleFormat.read(ByteArrayInputStream(rubbish.toByteArray())) }
    }

    @Test
    fun `a truncated file is refused rather than half read`() {
        val bytes = ByteArrayOutputStream().also { PuzzleFormat.write(it, classic, sample(10)) }.toByteArray()
        val cut = bytes.copyOf(bytes.size - 40)
        assertFailsWith<IOException> { PuzzleFormat.read(ByteArrayInputStream(cut)) }
    }

    @Test
    fun `the catalog can be sliced by grade`() {
        val catalog = roundTrip(sample(60, seed = 211))
        val total = Grade.entries.sumOf { catalog.byGrade(it).size }
        assertEquals(catalog.puzzles.size, total)
        assertEquals(catalog.counts.values.sum(), catalog.puzzles.size)
        for (grade in Grade.entries) {
            for (rated in catalog.byGrade(grade)) assertEquals(grade, rated.grade)
        }
    }

    @Test
    fun `a grid that is not nine by nine round trips too`() {
        val maker = GradedGenerator(Dimensions.SIX, Random(213))
        val puzzles = generateSequence { maker.next(Symmetry.MIRROR) }.take(8).toList()
        val bytes = ByteArrayOutputStream()
        PuzzleFormat.write(bytes, Dimensions.SIX, puzzles)
        val catalog = PuzzleFormat.read(ByteArrayInputStream(bytes.toByteArray()))
        assertEquals(Dimensions.SIX, catalog.dims)
        assertEquals(puzzles.size, catalog.puzzles.size)
        assertEquals(puzzles.first().puzzle.solution, catalog.puzzles.first().puzzle.solution)
    }
}
