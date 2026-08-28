package com.sendoku.engine.catalog

import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions

/**
 * What a code turned out to point at.
 *
 * Two kinds, because there are two honest ways to write a puzzle down. One names a puzzle in
 * the batch that ships with the app, which is three characters and costs nothing to look up.
 * The other carries the grid itself, which is longer and works for a puzzle that was never in
 * the batch: one generated on the phone, or one typed in from a newspaper.
 */
public sealed interface PuzzleRef {

    /** Puzzle [index] of the shipped batch, as it stood at batch version [version]. */
    public data class Batch(val version: Int, val index: Int) : PuzzleRef

    /** The clues themselves. Everything else about the puzzle is derived from them. */
    public data class Grid(val givens: Board) : PuzzleRef
}

/** Why a code could not be read. The words a player sees belong to the app, not to the engine. */
public enum class CodeFault {
    /** Not a Sendoku code at all: wrong shape, wrong prefix, or nothing worth parsing. */
    UNREADABLE,

    /** A character that is not in the alphabet. Usually a typo, often an I, an O or a zero. */
    BAD_CHARACTER,

    /** The right shape and the wrong checksum, so something was lost on the way here. */
    CORRUPT,

    /** Written by a newer version of the app than this one. */
    TOO_NEW,

    /** Reads correctly and points at a puzzle that is not there. */
    OUT_OF_RANGE,
}

/** The answer to reading a code: the puzzle it names, or why it could not be read. */
public sealed interface CodeResult {
    public data class Ok(val ref: PuzzleRef) : CodeResult
    public data class Failed(val fault: CodeFault) : CodeResult
}

/**
 * Writing a puzzle down so it can be sent to somebody.
 *
 * The point of a code is that two people end up on the same grid. There were three ways to do
 * that and only two of them survive contact with this app.
 *
 * A generator seed, the way a Minecraft seed works, is the one that does not. Generation here
 * is deterministic, so a seed would reproduce a grid, but a seed has no idea what difficulty it
 * is going to produce: the app generates and then rates, and the rating is what makes a puzzle
 * a Severe rather than a Gentle. Replaying a seed means paying that search again on the other
 * phone, which is seconds at the hard grades and much worse at the top. Worse still, any later
 * change to the generator or to the technique ladder would silently make every code ever shared
 * open a different puzzle.
 *
 * So a code names a puzzle rather than a recipe for one. The batch that ships in the app holds
 * four thousand of them, which is twelve bits, which is three characters once the alphabet is
 * chosen. Anything not in the batch gets written out in full instead: a bitmap of which cells
 * are given, then the digits in them, which over the range of clue counts the batch actually
 * holds lands between thirty five and forty three characters, prefix and check character
 * included. Long to type and nothing at all to paste, which is how these actually travel.
 *
 * The alphabet has no I, no O, no zero and no one, because these get read down a phone and
 * typed back in by hand. Reading is case insensitive and forgives spaces and dashes, since
 * every messaging app in the world will helpfully break a long code across a line.
 */
public object PuzzleCode {

    /**
     * The alphabet. Thirty two characters, none of which can be mistaken for another.
     *
     * Crockford's set, which drops I, L, O and U: the first three because they are the ones
     * confused with 1 and 0, and U so that no accidental word can be spelled by a code.
     */
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    private val VALUE = IntArray(128) { -1 }.also { table ->
        for ((index, char) in ALPHABET.withIndex()) {
            table[char.code] = index
            table[char.lowercaseChar().code] = index
        }
        // The four that are not in the alphabet are the four people type by mistake, so they
        // are read as what was meant rather than refused.
        table['I'.code] = VALUE_OF_ONE
        table['i'.code] = VALUE_OF_ONE
        table['L'.code] = VALUE_OF_ONE
        table['l'.code] = VALUE_OF_ONE
        table['O'.code] = VALUE_OF_ZERO
        table['o'.code] = VALUE_OF_ZERO
    }

    /**
     * Which batch a short code is talking about.
     *
     * Bumped only if the shipped batch ever changes in a way that moves an existing puzzle,
     * which is a thing this project has undertaken not to do: puzzles may be appended to the
     * end of the batch and never reordered, because a short code is an index in a player's
     * hands. [CatalogFreeze] is the test that holds that promise.
     */
    public const val BATCH_VERSION: Int = 1

    /** How many characters carry the index in a short code. Three of them hold 32,768. */
    private const val INDEX_CHARS = 3

    /** Digits are packed nine at a time, because nine of them fit in twenty nine bits. */
    private const val GROUP = 9
    private const val GROUP_BITS = 29

    /** Bits needed for a final group of one to eight digits, indexed by how many there are. */
    private val PART_BITS = intArrayOf(0, 4, 7, 10, 13, 16, 20, 23, 26)

    /** A short code for puzzle [index] of the shipped batch. */
    public fun forBatch(index: Int): String {
        require(index >= 0) { "a batch index cannot be negative" }
        require(index < 1 shl (INDEX_CHARS * BITS_PER_CHAR)) { "index $index does not fit in a short code" }
        val body = buildString {
            for (shift in (INDEX_CHARS - 1) downTo 0) {
                append(ALPHABET[(index shr (shift * BITS_PER_CHAR)) and CHAR_MASK])
            }
        }
        return "${versionChar(BATCH_VERSION)}-$body"
    }

    /**
     * A long code carrying [givens] itself.
     *
     * Only the clues are written. The solution is not: a puzzle with one answer is defined by
     * its clues, and the solver finds it again in under a millisecond, so sending it would be
     * doubling the length of the code to save nothing.
     */
    public fun forGrid(givens: Board): String {
        val dims = givens.dims
        val writer = BitWriter()
        for (index in 0 until dims.cellCount) {
            writer.write(if (givens.atIndex(index) == Board.EMPTY) 0 else 1, 1)
        }
        val digits = (0 until dims.cellCount)
            .map { givens.atIndex(it) }
            .filter { it != Board.EMPTY }
            .map { it - 1 }
        var at = 0
        while (at < digits.size) {
            val take = minOf(GROUP, digits.size - at)
            var packed = 0L
            for (offset in 0 until take) packed = packed * dims.size + digits[at + offset]
            writer.write(packed, if (take == GROUP) GROUP_BITS else PART_BITS[take])
            at += take
        }
        val body = writer.toBase32()
        return "$GRID_PREFIX${versionChar(BATCH_VERSION)}-$body${checksum(body)}"
    }

    /**
     * Reads either kind of code.
     *
     * [cellCount] is the size of grid the app is playing, which a long code does not carry:
     * a code is not the place to negotiate a grid size, and every puzzle this app has ever
     * shipped is nine by nine.
     */
    public fun read(text: String, dims: Dimensions = Dimensions.CLASSIC): CodeResult {
        val cleaned = text.trim()
            .removePrefix(LINK_PREFIX)
            .filter { !it.isWhitespace() && it != '-' && it != '_' }
        if (cleaned.length < INDEX_CHARS + 1) return CodeResult.Failed(CodeFault.UNREADABLE)

        return if (cleaned.first().uppercaseChar() == GRID_PREFIX) {
            readGrid(cleaned.drop(1), dims)
        } else {
            readBatch(cleaned)
        }
    }

    private fun readBatch(cleaned: String): CodeResult {
        if (cleaned.length != INDEX_CHARS + 1) return CodeResult.Failed(CodeFault.UNREADABLE)
        val version = versionOf(cleaned.first()) ?: return CodeResult.Failed(CodeFault.UNREADABLE)
        if (version > BATCH_VERSION) return CodeResult.Failed(CodeFault.TOO_NEW)
        var index = 0
        for (char in cleaned.drop(1)) {
            val value = valueOf(char) ?: return CodeResult.Failed(CodeFault.BAD_CHARACTER)
            index = (index shl BITS_PER_CHAR) or value
        }
        return CodeResult.Ok(PuzzleRef.Batch(version, index))
    }

    private fun readGrid(cleaned: String, dims: Dimensions): CodeResult {
        if (cleaned.length < 2) return CodeResult.Failed(CodeFault.UNREADABLE)
        val version = versionOf(cleaned.first()) ?: return CodeResult.Failed(CodeFault.UNREADABLE)
        if (version > BATCH_VERSION) return CodeResult.Failed(CodeFault.TOO_NEW)

        val body = cleaned.drop(1).dropLast(1)
        val given = cleaned.last()
        for (char in body + given) if (valueOf(char) == null) return CodeResult.Failed(CodeFault.BAD_CHARACTER)
        if (checksum(body) != given.uppercaseChar()) return CodeResult.Failed(CodeFault.CORRUPT)

        val reader = BitReader.of(body) ?: return CodeResult.Failed(CodeFault.BAD_CHARACTER)
        val board = unpack(reader, dims) ?: return CodeResult.Failed(CodeFault.CORRUPT)
        return CodeResult.Ok(PuzzleRef.Grid(board))
    }

    /**
     * The bits back into a grid: which cells are given, then what is in them.
     *
     * Null the moment the bits run out or there are more of them than a grid can account for,
     * both of which mean the code was damaged on the way here rather than that it is a
     * different sort of code.
     */
    private fun unpack(reader: BitReader, dims: Dimensions): Board? {
        val filled = ArrayList<Int>()
        for (index in 0 until dims.cellCount) {
            val bit = reader.read(1) ?: return null
            if (bit == 1L) filled += index
        }
        val board = Board(dims)
        var at = 0
        while (at < filled.size) {
            val take = minOf(GROUP, filled.size - at)
            var packed = reader.read(if (take == GROUP) GROUP_BITS else PART_BITS[take]) ?: return null
            for (offset in take - 1 downTo 0) {
                board.setAtIndex(filled[at + offset], (packed % dims.size).toInt() + 1)
                packed /= dims.size
            }
            at += take
        }
        return if (packedTooLong(reader)) null else board
    }

    /** Anything left over beyond the padding of the last character means a code that is not ours. */
    private fun packedTooLong(reader: BitReader): Boolean = reader.remaining() >= BITS_PER_CHAR

    private fun versionChar(version: Int): Char = ALPHABET[VERSION_BASE + version]

    private fun versionOf(char: Char): Int? {
        val value = valueOf(char) ?: return null
        val version = value - VERSION_BASE
        return if (version < 1) null else version
    }

    private fun valueOf(char: Char): Int? {
        if (char.code >= VALUE.size) return null
        val value = VALUE[char.code]
        return if (value < 0) null else value
    }

    /**
     * One character over the body, so a code cut short by a chat app is refused rather than
     * played as a different puzzle. Five bits is not cryptography and does not need to be:
     * it is here to catch a truncation and a typo, and it catches thirty one in thirty two.
     */
    private fun checksum(body: String): Char {
        var sum = 0
        for ((index, char) in body.withIndex()) {
            sum = (sum * CHECK_MULTIPLIER + (valueOf(char) ?: 0) + index) % CHECK_MODULUS
        }
        return ALPHABET[sum]
    }

    private const val BITS_PER_CHAR = 5
    private const val CHAR_MASK = 31
    private const val VALUE_OF_ONE = 1
    private const val VALUE_OF_ZERO = 0

    /** Version letters start at A, so version one reads as A and nobody sees a digit. */
    private const val VERSION_BASE = 9

    private const val CHECK_MULTIPLIER = 31
    private const val CHECK_MODULUS = 32

    /** The first character of a long code, which is how the reader tells the two apart. */
    private const val GRID_PREFIX = 'S'

    /** Codes travel as links too, and a link pasted whole should still be read. */
    private const val LINK_PREFIX = "sendoku://p/"

    /** Writes values of a given bit width, most significant bit first, into base32 text. */
    private class BitWriter {
        private val bits = StringBuilder()

        fun write(value: Long, width: Int) {
            for (shift in width - 1 downTo 0) bits.append(if ((value shr shift) and 1L == 1L) '1' else '0')
        }

        fun write(value: Int, width: Int): Unit = write(value.toLong(), width)

        fun toBase32(): String = buildString {
            var at = 0
            while (at < bits.length) {
                var value = 0
                for (offset in 0 until BITS_PER_CHAR) {
                    val position = at + offset
                    value = (value shl 1) or if (position < bits.length && bits[position] == '1') 1 else 0
                }
                append(ALPHABET[value])
                at += BITS_PER_CHAR
            }
        }
    }

    /** The other half of [BitWriter]. Returns null rather than throwing when it runs out. */
    private class BitReader private constructor(text: String) {
        private val bits: String = buildString {
            for (char in text) {
                val value = valueOf(char) ?: return@buildString
                for (shift in BITS_PER_CHAR - 1 downTo 0) append(if ((value shr shift) and 1 == 1) '1' else '0')
            }
        }
        private var at = 0

        fun read(width: Int): Long? {
            if (at + width > bits.length) return null
            var value = 0L
            repeat(width) {
                value = (value shl 1) or if (bits[at] == '1') 1L else 0L
                at++
            }
            return value
        }

        fun remaining(): Int = bits.length - at

        companion object {
            /** Null when the text holds a character the alphabet does not know. */
            fun of(text: String): BitReader? {
                for (char in text) if (valueOf(char) == null) return null
                return BitReader(text)
            }
        }
    }
}
