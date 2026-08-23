package com.sendoku.engine.killer

import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions

/**
 * A cage: some cells, and what they have to add up to.
 *
 * Cells are held sorted, which is what makes two cages built in different orders compare
 * equal and what lets a whole cage list be compared in a test without sorting first.
 *
 * A cage never repeats a digit. That is the ordinary Killer rule and it is not derivable
 * from the sum, so every technique and the solver both rely on it being stated here.
 */
public data class Cage(val sum: Int, val cells: List<Int>) {

    init {
        require(cells.isNotEmpty()) { "a cage has to hold a cell" }
        require(cells.size == cells.distinct().size) { "a cage cannot hold the same cell twice" }
        require(sum > 0) { "a cage sums to something" }
    }

    public val size: Int get() = cells.size

    public companion object {
        public fun of(sum: Int, vararg cells: Int): Cage = Cage(sum, cells.sorted())
    }
}

/**
 * A Killer puzzle: a grid of cages, and the solution they describe.
 *
 * There are no given digits in a classic Killer, which is why this holds cages rather than a
 * board of clues. The engine keeps the solution alongside, the same as a normal rated puzzle
 * does, because a hint that knows the answer can say a cell is wrong before the contradiction
 * shows up thirty moves later.
 */
public data class KillerPuzzle(val dims: Dimensions, val cages: List<Cage>, val solution: Board) {

    init {
        val covered = cages.flatMap { it.cells }
        require(covered.size == dims.cellCount) {
            "the cages cover ${covered.size} cells, the grid has ${dims.cellCount}"
        }
        require(covered.distinct().size == covered.size) { "a cell is in more than one cage" }
        for (cage in cages) {
            val total = cage.cells.sumOf { solution.atIndex(it) }
            require(total == cage.sum) { "a cage says ${cage.sum} but its cells add to $total" }
        }
    }

    /** The cage each cell belongs to, by cell index. Built once, read by the solver per node. */
    public val cageOfCell: IntArray = IntArray(dims.cellCount).also { map ->
        for ((index, cage) in cages.withIndex()) {
            for (cell in cage.cells) map[cell] = index
        }
    }
}
