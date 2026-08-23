package com.sendoku.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GeneratorTest {

    @Test
    fun `every generated puzzle has exactly one solution`() {
        val solver = Solver(Dimensions.CLASSIC)
        repeat(5) { seed ->
            val puzzle = Generator(Dimensions.CLASSIC, Random(seed.toLong())).generate()
            assertTrue(solver.hasUniqueSolution(puzzle.givens), "seed $seed produced an ambiguous puzzle")
            assertEquals(puzzle.solution, assertNotNull(solver.solve(puzzle.givens)))
        }
    }

    @Test
    fun `the givens are a subset of the solution`() {
        val puzzle = Generator(Dimensions.CLASSIC, Random(11)).generate()
        for (index in 0 until Dimensions.CLASSIC.cellCount) {
            val given = puzzle.givens.atIndex(index)
            if (given != Board.EMPTY) {
                assertEquals(puzzle.solution.atIndex(index), given, "cell $index disagrees with the solution")
            }
        }
    }

    @Test
    fun `the same seed makes the same puzzle`() {
        val first = Generator(Dimensions.CLASSIC, Random(2026)).generate()
        val second = Generator(Dimensions.CLASSIC, Random(2026)).generate()
        assertEquals(first.givens, second.givens)
        assertEquals(first.solution, second.solution)
    }

    @Test
    fun `rotational symmetry removes cells in pairs`() {
        val size = Dimensions.CLASSIC.size
        val puzzle = Generator(Dimensions.CLASSIC, Random(5)).generate(Symmetry.ROTATIONAL)
        for (index in 0 until Dimensions.CLASSIC.cellCount) {
            val row = index / size
            val col = index % size
            val partner = (size - 1 - row) * size + (size - 1 - col)
            val here = puzzle.givens.atIndex(index) == Board.EMPTY
            val there = puzzle.givens.atIndex(partner) == Board.EMPTY
            assertEquals(here, there, "cell $index and its partner $partner disagree")
        }
    }

    @Test
    fun `a clue floor is respected`() {
        val puzzle = Generator(Dimensions.CLASSIC, Random(3)).generate(Symmetry.NONE, minClues = 40)
        assertTrue(puzzle.clueCount >= 40, "expected at least 40 clues, got ${puzzle.clueCount}")
    }

    @Test
    fun `asymmetric digging goes below thirty clues`() {
        val puzzle = Generator(Dimensions.CLASSIC, Random(9)).generate(Symmetry.NONE)
        assertTrue(puzzle.clueCount in 17..35, "unexpected clue count ${puzzle.clueCount}")
    }

    @Test
    fun `generates the other grid sizes too`() {
        for (dims in listOf(Dimensions.JUNIOR, Dimensions.SIX)) {
            val puzzle = Generator(dims, Random(1)).generate()
            assertTrue(Solver(dims).hasUniqueSolution(puzzle.givens), "grid ${dims.size} was ambiguous")
        }
    }

    @Test
    fun `every symmetry produces a proper puzzle`() {
        for (symmetry in Symmetry.entries) {
            val puzzle = Generator(Dimensions.CLASSIC, Random(77)).generate(symmetry)
            assertTrue(
                Solver(Dimensions.CLASSIC).hasUniqueSolution(puzzle.givens),
                "$symmetry produced an ambiguous puzzle",
            )
            assertTrue(puzzle.clueCount in 17..81, "$symmetry produced ${puzzle.clueCount} clues")
        }
    }

    @Test
    fun `a symmetric puzzle really is symmetric`() {
        val size = 9
        fun partnerOf(symmetry: Symmetry, index: Int): Int {
            val row = index / size
            val col = index % size
            return when (symmetry) {
                Symmetry.NONE -> index
                Symmetry.ROTATIONAL -> (size - 1 - row) * size + (size - 1 - col)
                Symmetry.MIRROR -> row * size + (size - 1 - col)
                Symmetry.VERTICAL -> (size - 1 - row) * size + col
                Symmetry.DIAGONAL -> col * size + row
            }
        }
        for (symmetry in Symmetry.entries - Symmetry.NONE) {
            val puzzle = Generator(Dimensions.CLASSIC, Random(78)).generate(symmetry)
            for (index in 0 until 81) {
                val empty = puzzle.givens.atIndex(index) == Board.EMPTY
                val partnerEmpty = puzzle.givens.atIndex(partnerOf(symmetry, index)) == Board.EMPTY
                assertEquals(empty, partnerEmpty, "$symmetry broke at cell $index")
            }
        }
    }

    @Test
    fun `removing symmetry gives up fewer clues than keeping it`() {
        // Not a hard rule for a single seed, so compare averages over a handful.
        fun average(symmetry: Symmetry) = (0 until 12)
            .map { Generator(Dimensions.CLASSIC, Random(90L + it)).generate(symmetry).clueCount }
            .average()
        assertTrue(average(Symmetry.NONE) < average(Symmetry.ROTATIONAL))
    }
}
