package com.sendoku.engine.catalog

import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Puzzle
import com.sendoku.engine.Symmetry
import com.sendoku.engine.technique.TechniqueId
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** A batch of rated puzzles, as read back from disk. */
public data class PuzzleCatalog(val dims: Dimensions, val puzzles: List<RatedPuzzle>) {
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

    /**
     * Bumped whenever a record changes shape, so an old file is refused rather than misread.
     *
     * Version 2 writes the number of usage slots into the header. Version 1 did not, and is
     * still read, because the batch that ships in the app is a version 1 file and reordering
     * or rewriting it would invalidate every share code anybody has ever sent.
     */
    public const val VERSION: Int = 2

    public const val HEADER_BYTES: Int = 11

    /** Ratings are stored as hundredths, which is finer than any grade boundary needs. */
    private const val RATING_SCALE = 100.0

    /** Two bytes, which is what the rating occupies between the mask and the technique. */
    private const val RATING_BYTES = 2

    /**
     * How many techniques a batch records usage for.
     *
     * A batch written today has one byte per technique the engine knows. That number used to
     * be read from [TechniqueId] at the moment of reading, which was a trap with a long fuse:
     * adding a rule to the engine changed the record width, and every batch already written,
     * including the one shipped in the app and frozen against the share codes, was silently
     * misread from the second record onwards. Adding the Killer cage rules is what sprang it.
     *
     * So the count belongs to the file. Version 1 files were all written when the engine had
     * twenty eight techniques and do not carry it, which is what this is for. The number is
     * measurable rather than guessed: the shipped batch is 336,011 bytes for 4,000 puzzles,
     * which is 84 bytes a record, which is 56 bytes of puzzle and 28 of usage.
     */
    private const val LEGACY_USAGE_SLOTS = 28

    /** The first version that writes the usage slot count into its header. */
    private const val SLOTTED_VERSION = 2

    /** Bytes one puzzle occupies once the file is decompressed, for a batch with [slots]. */
    public fun recordBytes(dims: Dimensions, slots: Int = TechniqueId.entries.size): Int =
        solutionBytes(dims) + maskBytes(dims) + 2 + 1 + 1 + slots

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
            out.writeByte(TechniqueId.entries.size)
            for (rated in puzzles) writeRecord(out, dims, rated)
        }
    }

    /** Reads a batch back. The stream is closed. */
    public fun read(input: InputStream): PuzzleCatalog {
        val body = Body.inflate(input)
        val puzzles = ArrayList<RatedPuzzle>(body.count)
        repeat(body.count) { index -> puzzles.add(body.decode(index)) }
        return PuzzleCatalog(body.dims, puzzles)
    }

    /**
     * A decompressed batch, still in its packed form.
     *
     * Holding the bytes rather than the objects is what lets a caller read one puzzle out
     * of three thousand. Records are a fixed width, so puzzle `n` starts at a known offset
     * and nothing before it has to be looked at.
     */
    internal class Body(
        val dims: Dimensions,
        val count: Int,
        private val bytes: ByteArray,
        /** How many techniques this file records usage for, which is a fact about the file. */
        private val slots: Int,
    ) {
        private val width = recordBytes(dims, slots)
        private val solutionBytes = solutionBytes(dims)
        private val maskBytes = maskBytes(dims)

        private fun offsetOf(index: Int): Int {
            require(index in 0 until count) { "puzzle $index is not in a batch of $count" }
            return index * width
        }

        /** Two bytes at a known offset. Nothing else in the record is touched. */
        fun ratingAt(index: Int): Double {
            val at = offsetOf(index) + solutionBytes + maskBytes
            val raw = ((bytes[at].toInt() and 0xFF) shl 8) or (bytes[at + 1].toInt() and 0xFF)
            return raw / RATING_SCALE
        }

        /**
         * The hardest technique one puzzle needs, read without decoding the grid.
         *
         * One byte at a known offset, the same trick the rating uses. It is what makes it
         * cheap to ask the batch for every puzzle that needs an X-Wing: four thousand byte
         * reads rather than four thousand grids.
         */
        fun hardestAt(index: Int): TechniqueId? {
            val at = offsetOf(index) + solutionBytes + maskBytes + RATING_BYTES
            val ordinal = bytes[at].toInt() and 0xFF
            return if (ordinal == 0) null else TechniqueId.entries[ordinal - 1]
        }

        fun decode(index: Int): RatedPuzzle {
            var at = offsetOf(index)

            val solution = Board(dims)
            for (cell in 0 until dims.cellCount) {
                val packed = bytes[at + cell / 2].toInt()
                val digit = if (cell % 2 == 0) (packed shr 4) and 0xF else packed and 0xF
                solution.setAtIndex(cell, digit)
            }
            at += solutionBytes

            val givens = Board(dims)
            for (cell in 0 until dims.cellCount) {
                val present = bytes[at + cell / 8].toInt() and (1 shl (cell % 8)) != 0
                if (present) givens.setAtIndex(cell, solution.atIndex(cell))
            }
            at += maskBytes

            val rating = (((bytes[at].toInt() and 0xFF) shl 8) or (bytes[at + 1].toInt() and 0xFF)) / RATING_SCALE
            at += 2

            val hardestOrdinal = bytes[at++].toInt() and 0xFF
            val hardest = if (hardestOrdinal == 0) null else TechniqueId.entries[hardestOrdinal - 1]
            val symmetry = Symmetry.entries[bytes[at++].toInt() and 0xFF]

            // One byte per slot the file has, not per technique this build knows. A file
            // written before a rule existed has nothing to say about it, and a file written
            // after this build was compiled has slots it cannot name, which are skipped.
            val usage = LinkedHashMap<TechniqueId, Int>()
            for (slot in 0 until slots) {
                val used = bytes[at++].toInt() and 0xFF
                val id = TechniqueId.entries.getOrNull(slot) ?: continue
                if (used > 0) usage[id] = used
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

        companion object {
            /** Decompresses and validates the header. The stream is closed. */
            fun inflate(input: InputStream): Body {
                val raw = GZIPInputStream(input).use { it.readBytes() }
                if (raw.size < HEADER_BYTES) throw IOException("batch is too short to hold a header")
                if (!raw.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
                    throw IOException("not a Sendoku batch")
                }
                val version = raw[4].toInt() and 0xFF
                if (version > VERSION) {
                    throw IOException("batch is version $version, this build reads $VERSION")
                }
                val dims = Dimensions(raw[5].toInt() and 0xFF, raw[6].toInt() and 0xFF)
                val count = ((raw[7].toInt() and 0xFF) shl 24) or
                    ((raw[8].toInt() and 0xFF) shl 16) or
                    ((raw[9].toInt() and 0xFF) shl 8) or
                    (raw[10].toInt() and 0xFF)
                if (count < 0) throw IOException("batch claims $count puzzles")

                // Version 1 carried no slot count and was always written by an engine with
                // twenty nine techniques. Version 2 says so in the header, which is what
                // stops the next new rule from misreading every batch already written.
                val header = if (version >= SLOTTED_VERSION) HEADER_BYTES + 1 else HEADER_BYTES
                val slots = if (version >= SLOTTED_VERSION) raw[HEADER_BYTES].toInt() and 0xFF else LEGACY_USAGE_SLOTS

                val expected = header.toLong() + count.toLong() * recordBytes(dims, slots)
                if (raw.size.toLong() != expected) {
                    throw IOException("batch is ${raw.size} bytes, expected $expected for $count puzzles")
                }
                return Body(dims, count, raw.copyOfRange(header, raw.size), slots)
            }
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
}
