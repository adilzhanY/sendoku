package com.sendoku.engine.catalog

import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Puzzle
import com.sendoku.engine.Symmetry
import com.sendoku.engine.technique.TechniqueId
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** A batch of rated puzzles, as read back from disk. */
public data class PuzzleCatalog(
    val dims: Dimensions,
    val puzzles: List<RatedPuzzle>,
) {
    public fun byGrade(grade: Grade): List<RatedPuzzle> = puzzles.filter { it.grade == grade }

    public val counts: Map<Grade, Int>
        get() = puzzles.groupingBy { it.grade }.eachCount()
}

/**
 * The on disk format for a batch of rated puzzles.
 *
 * Text would be the obvious choice and the wrong one. A puzzle written out as digits costs
 * 162 characters for the givens and the solution alone, and the batch ships inside the APK,
 * where every kilobyte is a download the player waits for on a bad connection.
 *
 * So it is packed. The solution goes in as one nibble per cell, and the givens ride along
 * as a bitmask over it, because every given is by definition a cell of the solution. That
 * is 52 bytes where the text was 162, before the whole file is gzipped on top.
 *
 * Records are a fixed width on purpose. Once the file is decompressed, puzzle `n` sits at
 * a known offset, so the app can read one puzzle without walking the batch.
 *
 * ```
 * header   magic "SDKB", version, boxWidth, boxHeight, count
 * record   solution nibbles | given mask | rating | hardest | symmetry | usage
 * ```
 */
public object PuzzleFormat {

    private val MAGIC = byteArrayOf('S'.code.toByte(), 'D'.code.toByte(), 'K'.code.toByte(), 'B'.code.toByte())

    /** Bumped whenever a record changes shape, so an old file is refused rather than misread. */
    public const val VERSION: Int = 1

    public const val HEADER_BYTES: Int = 11

    /** Ratings are stored as hundredths, which is finer than any grade boundary needs. */
    private const val RATING_SCALE = 100.0

    /** Bytes one puzzle occupies once the file is decompressed. */
    public fun recordBytes(dims: Dimensions): Int =
        solutionBytes(dims) + maskBytes(dims) + 2 + 1 + 1 + TechniqueId.entries.size

    private fun solutionBytes(dims: Dimensions) = (dims.cellCount + 1) / 2

    private fun maskBytes(dims: Dimensions) = (dims.cellCount + 7) / 8

    /** Writes [puzzles] to [output], gzipped. The stream is closed. */
    public fun write(output: OutputStream, dims: Dimensions, puzzles: List<RatedPuzzle>) {
        require(dims.size <= 15) { "a nibble holds digits up to 15, not ${dims.size}" }
        DataOutputStream(GZIPOutputStream(output)).use { out ->
            out.write(MAGIC)
            out.writeByte(VERSION)
            out.writeByte(dims.boxWidth)
            out.writeByte(dims.boxHeight)
            out.writeInt(puzzles.size)
            for (rated in puzzles) writeRecord(out, dims, rated)
        }
    }

    /** Reads a batch back. The stream is closed. */
    public fun read(input: InputStream): PuzzleCatalog {
        DataInputStream(GZIPInputStream(input)).use { source ->
            val magic = ByteArray(MAGIC.size)
            source.readFully(magic)
            if (!magic.contentEquals(MAGIC)) throw IOException("not a Sendoku batch")

            val version = source.readUnsignedByte()
            if (version != VERSION) throw IOException("batch is version $version, this build reads $VERSION")

            val dims = Dimensions(source.readUnsignedByte(), source.readUnsignedByte())
            val count = source.readInt()
            if (count < 0) throw IOException("batch claims $count puzzles")

            val puzzles = ArrayList<RatedPuzzle>(count)
            repeat(count) { index ->
                try {
                    puzzles.add(readRecord(source, dims))
                } catch (e: EOFException) {
                    throw IOException("batch ended after $index of $count puzzles", e)
                }
            }
            return PuzzleCatalog(dims, puzzles)
        }
    }

    private fun writeRecord(out: DataOutputStream, dims: Dimensions, rated: RatedPuzzle) {
        val solution = rated.puzzle.solution
        val givens = rated.puzzle.givens

        val nibbles = ByteArray(solutionBytes(dims))
        for (cell in 0 until dims.cellCount) {
            val digit = solution.atIndex(cell)
            val slot = cell / 2
            nibbles[slot] = if (cell % 2 == 0) {
                (digit shl 4).toByte()
            } else {
                (nibbles[slot].toInt() or digit).toByte()
            }
        }
        out.write(nibbles)

        val mask = ByteArray(maskBytes(dims))
        for (cell in 0 until dims.cellCount) {
            if (givens.atIndex(cell) == Board.EMPTY) continue
            mask[cell / 8] = (mask[cell / 8].toInt() or (1 shl (cell % 8))).toByte()
        }
        out.write(mask)

        out.writeShort(Math.round(rated.rating * RATING_SCALE).toInt())
        out.writeByte(rated.hardest?.let { it.ordinal + 1 } ?: 0)
        out.writeByte(rated.symmetry.ordinal)
        for (id in TechniqueId.entries) {
            out.writeByte((rated.usage[id] ?: 0).coerceAtMost(255))
        }
    }

    private fun readRecord(source: DataInputStream, dims: Dimensions): RatedPuzzle {
        val nibbles = ByteArray(solutionBytes(dims))
        source.readFully(nibbles)
        val solution = Board(dims)
        for (cell in 0 until dims.cellCount) {
            val packed = nibbles[cell / 2].toInt()
            val digit = if (cell % 2 == 0) (packed shr 4) and 0xF else packed and 0xF
            solution.setAtIndex(cell, digit)
        }

        val mask = ByteArray(maskBytes(dims))
        source.readFully(mask)
        val givens = Board(dims)
        for (cell in 0 until dims.cellCount) {
            val present = mask[cell / 8].toInt() and (1 shl (cell % 8)) != 0
            if (present) givens.setAtIndex(cell, solution.atIndex(cell))
        }

        val rating = source.readUnsignedShort() / RATING_SCALE
        val hardestOrdinal = source.readUnsignedByte()
        val hardest = if (hardestOrdinal == 0) null else TechniqueId.entries[hardestOrdinal - 1]
        val symmetry = Symmetry.entries[source.readUnsignedByte()]

        val usage = LinkedHashMap<TechniqueId, Int>()
        for (id in TechniqueId.entries) {
            val count = source.readUnsignedByte()
            if (count > 0) usage[id] = count
        }

        return RatedPuzzle(
            puzzle = Puzzle(givens = givens, solution = solution),
            rating = rating,
            grade = Grade.of(rating),
            hardest = hardest,
            symmetry = symmetry,
            usage = usage,
        )
    }
}
