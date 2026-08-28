package com.sendoku.engine.catalog

import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The codes players send each other.
 *
 * A code has one job: two people end up on the same grid. So the round trip is the whole
 * test, and it is run over every index a short code can hold and over grids across the range
 * of clue counts the batch actually contains.
 *
 * The other half is refusing. A code that arrives cut in half by a chat app, or with an O
 * typed where a zero belongs, must be turned away with a reason rather than opened as some
 * other puzzle, because a share code that silently plays the wrong grid is worse than one
 * that does not work at all.
 */
class PuzzleCodeTest {

    private val classic = Dimensions.CLASSIC

    @Test
    fun `a short code round trips for every index it can hold`() {
        for (index in 0 until 4096) {
            val code = PuzzleCode.forBatch(index)
            val read = PuzzleCode.read(code)
            assertTrue(read is CodeResult.Ok, "index $index came back as $read")
            assertEquals(PuzzleRef.Batch(PuzzleCode.BATCH_VERSION, index), read.ref)
        }
    }

    @Test
    fun `a short code is five characters and reads the same in any case`() {
        val code = PuzzleCode.forBatch(1234)
        assertEquals(5, code.length, "the code is $code")
        val expected = (PuzzleCode.read(code) as CodeResult.Ok).ref
        for (variant in listOf(code.lowercase(), code.replace("-", ""), " $code ", "sendoku://p/$code")) {
            assertEquals(expected, (PuzzleCode.read(variant) as CodeResult.Ok).ref, "reading $variant")
        }
    }

    @Test
    fun `a grid round trips whatever it holds`() {
        val random = Random(7)
        for (clues in 17..40) {
            val board = Board(classic)
            val cells = (0 until classic.cellCount).shuffled(random).take(clues)
            for (cell in cells) board.setAtIndex(cell, random.nextInt(1, classic.size + 1))

            val code = PuzzleCode.forGrid(board)
            val read = PuzzleCode.read(code)
            assertTrue(read is CodeResult.Ok, "a $clues clue grid came back as $read")
            val back = (read.ref as PuzzleRef.Grid).givens
            for (index in 0 until classic.cellCount) {
                assertEquals(board.atIndex(index), back.atIndex(index), "cell $index of a $clues clue grid")
            }
        }
    }

    @Test
    fun `a grid code stays short enough to paste`() {
        // Thirty five to forty three over the clue counts the batch actually holds. A code
        // that quietly grew past that is one nobody will send, so the size is a promise
        // rather than an accident.
        val random = Random(11)
        for (clues in 22..34) {
            val board = Board(classic)
            for (cell in (0 until classic.cellCount).shuffled(random).take(clues)) {
                board.setAtIndex(cell, random.nextInt(1, classic.size + 1))
            }
            val code = PuzzleCode.forGrid(board)
            assertTrue(code.length in 35..43, "a $clues clue code is ${code.length} characters: $code")
        }
    }

    @Test
    fun `a code cut short is refused`() {
        val board = Board(classic).also { it.setAtIndex(0, 5) }
        val code = PuzzleCode.forGrid(board)
        for (cut in 2 until code.length - 1) {
            val result = PuzzleCode.read(code.take(cut))
            assertTrue(result is CodeResult.Failed, "${code.take(cut)} was accepted")
        }
    }

    @Test
    fun `a character the alphabet does not know is refused`() {
        val code = PuzzleCode.forBatch(500)
        val broken = code.dropLast(1) + "£"
        assertEquals(CodeFault.BAD_CHARACTER, (PuzzleCode.read(broken) as CodeResult.Failed).fault)
    }

    @Test
    fun `the letters people type by mistake are read as what they meant`() {
        // I and L are read as one, O as zero. Nobody who reads a code down a phone gets this
        // right, and the alternative is a code that does not work for a reason nobody can see.
        val code = PuzzleCode.forBatch(0)
        val typed = code.replace('0', 'O')
        assertEquals(
            (PuzzleCode.read(code) as CodeResult.Ok).ref,
            (PuzzleCode.read(typed) as CodeResult.Ok).ref,
        )
    }

    @Test
    fun `a code from a later version is refused as too new`() {
        val code = PuzzleCode.forBatch(9)
        val newer = "B" + code.drop(1)
        assertEquals(CodeFault.TOO_NEW, (PuzzleCode.read(newer) as CodeResult.Failed).fault)
    }

    @Test
    fun `nonsense is refused rather than parsed`() {
        for (text in listOf("", "  ", "hello", "A-", "?", "sendoku://p/")) {
            assertTrue(PuzzleCode.read(text) is CodeResult.Failed, "\"$text\" was accepted")
        }
    }

    @Test
    fun `two different grids never write the same code`() {
        val random = Random(3)
        val seen = HashSet<String>()
        repeat(500) {
            val board = Board(classic)
            for (cell in (0 until classic.cellCount).shuffled(random).take(25)) {
                board.setAtIndex(cell, random.nextInt(1, classic.size + 1))
            }
            assertTrue(seen.add(PuzzleCode.forGrid(board)), "two grids wrote the same code")
        }
        assertNotEquals(0, seen.size)
    }
}
