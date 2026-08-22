package com.sendoku.engine

import kotlin.random.Random

/** How the removed cells are arranged. Symmetry costs clues but looks far better. */
public enum class Symmetry {
    /** Remove cells one at a time. Gives the fewest clues. */
    NONE,

    /** Remove a cell together with its 180 degree partner. What newspapers print. */
    ROTATIONAL,

    /** Remove a cell together with its left to right mirror. */
    MIRROR,
}

/** A puzzle and the single grid it resolves to. */
public data class Puzzle(
    val givens: Board,
    val solution: Board,
) {
    val clueCount: Int get() = givens.clueCount
}

/**
 * Makes puzzles by solving forwards and then digging holes backwards.
 *
 * Pass a seeded [Random] to get the same puzzle on every device, which is how the
 * daily puzzle works without a server.
 */
public class Generator(
    private val dims: Dimensions = Dimensions.CLASSIC,
    private val random: Random = Random.Default,
) {

    private val solver = Solver(dims)

    /** A complete, legal grid with no empty cells. */
    public fun completeGrid(): Board =
        requireNotNull(solver.solve(Board(dims), random)) { "an empty grid always has a solution" }

    /**
     * Digs holes out of a complete grid for as long as the solution stays unique.
     *
     * [minClues] stops the dig early, which is useful for easy levels. It is a floor,
     * not a target, so the result usually has a few more clues than asked for.
     */
    public fun generate(
        symmetry: Symmetry = Symmetry.ROTATIONAL,
        minClues: Int = 0,
    ): Puzzle {
        val solution = completeGrid()
        val working = solution.copy()

        val order = IntArray(dims.cellCount) { it }
        for (i in order.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = order[i]
            order[i] = order[j]
            order[j] = tmp
        }

        for (index in order) {
            if (working.clueCount <= minClues) break
            val group = partners(index, symmetry)
            if (group.all { working.atIndex(it) == Board.EMPTY }) continue
            if (working.clueCount - group.count { working.atIndex(it) != Board.EMPTY } < minClues) continue

            val removed = group.map { it to working.atIndex(it) }
            group.forEach { working.setAtIndex(it, Board.EMPTY) }

            if (!solver.hasUniqueSolution(working)) {
                removed.forEach { (cell, digit) -> working.setAtIndex(cell, digit) }
            }
        }

        return Puzzle(givens = working, solution = solution)
    }

    /** The cells that must come out together to keep the chosen symmetry. */
    private fun partners(index: Int, symmetry: Symmetry): List<Int> {
        val size = dims.size
        val row = index / size
        val col = index % size
        val mirror = when (symmetry) {
            Symmetry.NONE -> index
            Symmetry.ROTATIONAL -> (size - 1 - row) * size + (size - 1 - col)
            Symmetry.MIRROR -> row * size + (size - 1 - col)
        }
        return if (mirror == index) listOf(index) else listOf(index, mirror)
    }
}
