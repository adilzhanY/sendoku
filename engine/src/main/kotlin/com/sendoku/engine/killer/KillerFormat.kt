package com.sendoku.engine.killer

import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.technique.TechniqueId
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** A Killer puzzle that has been through the ladder, so its difficulty is known. */
public data class RatedKiller(
    val puzzle: KillerPuzzle,
    val rating: Double,
    val grade: Grade,
    val hardest: TechniqueId?,
    val usage: Map<TechniqueId, Int>,
)

/**
 * The on disk format for a batch of Killer puzzles.
 *
 * A Killer is a solved grid and a way of cutting it into cages, and that is all this stores.
 * The sums are not written down at all: a cage's sum is the total of its cells in the
 * solution, so writing it would be storing the same fact twice and giving a corrupt file a
 * way to disagree with itself.
 *
 * That makes a record a solution and one byte per cell saying which cage it belongs to, which
 * is a hundred and twenty five bytes for a nine by nine, fixed width, so puzzle `n` sits at a
 * known offset exactly as it does in the classic batch.
 *
 * ```
 * header   magic "SDKK", version, boxWidth, boxHeight, count
 * record   solution nibbles | cage of each cell | rating | hardest
 * ```
 */
public object KillerFormat {

    private val MAGIC = byteArrayOf('S'.code.toByte(), 'D'.code.toByte(), 'K'.code.toByte(), 'K'.code.toByte())

    public const val VERSION: Int = 1

    public const val HEADER_BYTES: Int = 11

    private const val RATING_SCALE = 100.0

    /** Bytes one puzzle occupies once the file is decompressed. */
    public fun recordBytes(dims: Dimensions): Int = solutionBytes(dims) + dims.cellCount + 2 + 1

    private fun solutionBytes(dims: Dimensions) = (dims.cellCount + 1) / 2

    /** Writes [puzzles] to [output], gzipped. The stream is closed. */
    public fun write(output: OutputStream, dims: Dimensions, puzzles: List<RatedKiller>) {
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
    public fun read(input: InputStream): List<RatedKiller> {
        val body = Body.inflate(input)
        return (0 until body.count).map { body.decode(it) }
    }

    private fun writeRecord(out: DataOutputStream, dims: Dimensions, rated: RatedKiller) {
        val solution = rated.puzzle.solution
        var index = 0
        while (index < dims.cellCount) {
            val high = solution.atIndex(index)
            val low = if (index + 1 < dims.cellCount) solution.atIndex(index + 1) else 0
            out.writeByte((high shl 4) or low)
            index += 2
        }
        for (cell in 0 until dims.cellCount) {
            out.writeByte(rated.puzzle.cageOfCell[cell])
        }
        out.writeShort(Math.round(rated.rating * RATING_SCALE).toInt())
        out.writeByte(rated.hardest?.let { it.ordinal + 1 } ?: 0)
    }

    /** A decompressed batch, still packed, so one puzzle can be read out of hundreds. */
    internal class Body(val dims: Dimensions, val count: Int, private val bytes: ByteArray) {
        private val width = recordBytes(dims)

        private fun offsetOf(index: Int): Int {
            require(index in 0 until count) { "puzzle $index is not in a batch of $count" }
            return index * width
        }

        /** Two bytes at a known offset, without decoding the grid. */
        fun ratingAt(index: Int): Double {
            val at = offsetOf(index) + solutionBytes(dims) + dims.cellCount
            val raw = ((bytes[at].toInt() and 0xFF) shl 8) or (bytes[at + 1].toInt() and 0xFF)
            return raw / RATING_SCALE
        }

        fun decode(index: Int): RatedKiller {
            var at = offsetOf(index)

            val solution = Board(dims)
            for (cell in 0 until dims.cellCount) {
                val packed = bytes[at + cell / 2].toInt()
                val digit = if (cell % 2 == 0) (packed shr 4) and 0xF else packed and 0xF
                solution.setAtIndex(cell, digit)
            }
            at += solutionBytes(dims)

            val members = HashMap<Int, MutableList<Int>>()
            for (cell in 0 until dims.cellCount) {
                members.getOrPut(bytes[at + cell].toInt() and 0xFF) { ArrayList() }.add(cell)
            }
            at += dims.cellCount

            // The sum is the cells themselves, added up in the answer. Storing it would be
            // writing the same fact twice and letting a corrupt file disagree with itself.
            val cages = members.entries.sortedBy { it.key }.map { (_, cells) ->
                Cage(cells.sumOf { solution.atIndex(it) }, cells.sorted())
            }

            val raw = ((bytes[at].toInt() and 0xFF) shl 8) or (bytes[at + 1].toInt() and 0xFF)
            at += 2
            val rating = raw / RATING_SCALE
            val hardestOrdinal = bytes[at].toInt() and 0xFF
            val hardest = if (hardestOrdinal == 0) null else TechniqueId.entries[hardestOrdinal - 1]

            return RatedKiller(
                puzzle = KillerPuzzle(dims, cages, solution),
                rating = rating,
                grade = Grade.of(rating),
                hardest = hardest,
                usage = emptyMap(),
            )
        }

        companion object {
            fun inflate(input: InputStream): Body {
                val raw = GZIPInputStream(input).use { it.readBytes() }
                if (raw.size < HEADER_BYTES) throw IOException("batch is too short to hold a header")
                if (!raw.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
                    throw IOException("not a Sendoku Killer batch")
                }
                val version = raw[4].toInt() and 0xFF
                if (version != VERSION) {
                    throw IOException("batch is version $version, this build reads $VERSION")
                }
                val dims = Dimensions(raw[5].toInt() and 0xFF, raw[6].toInt() and 0xFF)
                val count = ((raw[7].toInt() and 0xFF) shl 24) or
                    ((raw[8].toInt() and 0xFF) shl 16) or
                    ((raw[9].toInt() and 0xFF) shl 8) or
                    (raw[10].toInt() and 0xFF)
                if (count < 0) throw IOException("batch claims $count puzzles")

                val expected = HEADER_BYTES.toLong() + count.toLong() * recordBytes(dims)
                if (raw.size.toLong() != expected) {
                    throw IOException("batch is ${raw.size} bytes, expected $expected for $count puzzles")
                }
                return Body(dims, count, raw.copyOfRange(HEADER_BYTES, raw.size))
            }
        }
    }
}

/**
 * The shipped Killer batch, read one puzzle at a time.
 *
 * The same shape as the classic [com.sendoku.engine.catalog.CatalogReader] and for the same
 * reason: inflating the file costs milliseconds and decoding every puzzle in it costs a lot
 * more, and the app only ever wants one.
 */
public class KillerCatalogReader private constructor(private val body: KillerFormat.Body) {

    public val dims: Dimensions get() = body.dims

    public val size: Int get() = body.count

    /** Which puzzles belong to which grade, built on first use from the ratings alone. */
    private val byGrade: Map<Grade, List<Int>> by lazy {
        val buckets = HashMap<Grade, MutableList<Int>>()
        for (index in 0 until size) {
            buckets.getOrPut(Grade.of(body.ratingAt(index))) { ArrayList() }.add(index)
        }
        buckets
    }

    public fun gradeAt(index: Int): Grade = Grade.of(body.ratingAt(index))

    public fun puzzleAt(index: Int): RatedKiller = body.decode(index)

    public fun indicesOf(grade: Grade): List<Int> = byGrade[grade].orEmpty()

    public val counts: Map<Grade, Int> get() = byGrade.mapValues { it.value.size }

    public companion object {
        public fun from(input: InputStream): KillerCatalogReader = KillerCatalogReader(KillerFormat.Body.inflate(input))
    }
}
