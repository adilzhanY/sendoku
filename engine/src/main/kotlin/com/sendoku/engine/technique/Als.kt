package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates
import com.sendoku.engine.House

/**
 * An almost locked set: `n` cells inside one house holding `n + 1` candidates between them.
 *
 * Take any one candidate away and the set becomes locked, meaning its cells use up every
 * digit it holds. A single bivalue cell is the smallest one.
 */
internal class Als(val house: House, val cells: List<Int>, val candidates: Candidates) {

    fun overlaps(other: Als): Boolean = cells.any { it in other.cells }
}

/**
 * Finding almost locked sets, once, for whichever technique wants them.
 *
 * Four techniques now reason about groups of cells acting as one, and enumerating the sets
 * is the expensive half of all four. Keeping the search here means they agree about what a
 * set is, and that raising the size cap raises it everywhere at once.
 */
internal object AlsFinder {

    /** Sets larger than this are not searched. Beyond four cells the count explodes. */
    const val MAX_CELLS: Int = 4

    /** Every almost locked set up to [maxCells] cells, one house at a time. */
    fun collect(grid: CandidateGrid, maxCells: Int = MAX_CELLS): List<Als> {
        val sets = ArrayList<Als>()
        for (house in grid.houses) {
            val open = grid.cellsOf(house).filter { grid.isEmpty(it) }
            if (open.size < 2) continue
            val limit = minOf(maxCells, open.size - 1)
            for (size in 1..limit) {
                forEachCombination(open.size, size) { picks ->
                    val cells = picks.map { open[it] }
                    var union = Candidates.EMPTY
                    for (cell in cells) union = union or grid.candidatesAt(cell)
                    if (union.size == size + 1) sets.add(Als(house, cells, union))
                    false
                }
            }
        }
        return sets
    }

    /** The cells of [als] that could still hold [digit]. */
    fun homes(grid: CandidateGrid, als: Als, digit: Int): List<Int> =
        als.cells.filter { digit in grid.candidatesAt(it) }

    /**
     * True when every home of [digit] across the two sets sees every other one, so the
     * digit cannot appear in both sets at once.
     */
    fun restricted(grid: CandidateGrid, left: Als, right: Als, digit: Int): Boolean {
        val here = homes(grid, left, digit)
        val there = homes(grid, right, digit)
        if (here.isEmpty() || there.isEmpty()) return false
        return here.all { a -> there.all { b -> grid.sees(a, b) } }
    }

    /** Every cell of the sets, with every mark it still holds, for a hint to light up. */
    fun marksOf(grid: CandidateGrid, cells: List<Int>): List<CellDigit> =
        cells.sorted().flatMap { cell -> grid.candidatesAt(cell).toList().map { CellDigit(cell, it) } }
}
