package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid

/**
 * One connected group of cells linked by strong links on a single digit, split into the
 * two alternating colours.
 *
 * Exactly one of the two colours is the true one. Which is unknown, and never needs to
 * be known: every colouring rule works by showing that one colour cannot be true.
 */
internal class Cluster(val digit: Int, val first: List<Int>, val second: List<Int>) {
    val cells: Set<Int> = (first + second).toSet()

    /** The colour that is not [colour]. */
    fun other(colour: List<Int>): List<Int> = if (colour === first) second else first

    val colours: List<List<Int>> get() = listOf(first, second)
}

/**
 * Builds the strong link graph for one digit and two colours every component of it.
 *
 * A strong link is a house where the digit has exactly two homes: one of them is the
 * digit, so the two alternate. Chains of those links are the whole basis of colouring.
 */
internal fun clustersFor(grid: CandidateGrid, digit: Int): List<Cluster> {
    val links = HashMap<Int, MutableList<Int>>()
    for (house in grid.houses) {
        if (grid.isPlacedIn(house, digit)) continue
        val homes = grid.cellsOf(house).filter { digit in grid.candidatesAt(it) }
        if (homes.size != 2) continue
        val (a, b) = homes
        links.getOrPut(a) { ArrayList() }.add(b)
        links.getOrPut(b) { ArrayList() }.add(a)
    }
    if (links.isEmpty()) return emptyList()

    val colour = HashMap<Int, Int>()
    val clusters = ArrayList<Cluster>()

    for (seed in links.keys.sorted()) {
        if (seed in colour) continue
        val first = ArrayList<Int>()
        val second = ArrayList<Int>()
        colour[seed] = 0
        val queue = ArrayDeque<Int>()
        queue.add(seed)
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            val shade = colour.getValue(cell)
            if (shade == 0) first.add(cell) else second.add(cell)
            for (next in links[cell].orEmpty()) {
                if (next in colour) continue
                colour[next] = 1 - shade
                queue.add(next)
            }
        }
        clusters.add(Cluster(digit, first.sorted(), second.sorted()))
    }
    return clusters
}

/** True when some cell of [left] shares a house with some cell of [right]. */
internal fun CandidateGrid.anySees(left: List<Int>, right: List<Int>): Boolean =
    left.any { a -> right.any { b -> sees(a, b) } }

/**
 * Simple colouring, on one digit at a time.
 *
 * Colour a chain of strong links alternately. Exactly one colour is true, which gives two
 * separate conclusions.
 *
 * The trap: if two cells of the same colour share a house, that colour would put the digit
 * twice in one house, so it is false and every cell wearing it can lose the digit.
 *
 * The wrap: a cell outside the chain that sees both colours cannot hold the digit, because
 * whichever colour turns out to be true already claims it.
 */
public object SimpleColouring : Technique {

    override val id: TechniqueId get() = TechniqueId.SIMPLE_COLOURING

    override fun find(grid: CandidateGrid): Deduction? {
        for (digit in 1..grid.size) {
            for (cluster in clustersFor(grid, digit)) {
                trap(grid, cluster)?.let { return it }
                wrap(grid, cluster)?.let { return it }
            }
        }
        return null
    }

    private fun trap(grid: CandidateGrid, cluster: Cluster): Deduction? {
        for (colour in cluster.colours) {
            val clash = colour.any { a -> colour.any { b -> grid.sees(a, b) } }
            if (!clash) continue
            val eliminations = colour.filter { cluster.digit in grid.candidatesAt(it) }
            if (eliminations.isEmpty()) continue
            return Deduction(
                technique = TechniqueId.SIMPLE_COLOURING,
                focusCells = cluster.cells.sorted(),
                focusCandidates = cluster.cells.sorted().map { CellDigit(it, cluster.digit) },
                eliminations = eliminations.map { CellDigit(it, cluster.digit) },
            )
        }
        return null
    }

    private fun wrap(grid: CandidateGrid, cluster: Cluster): Deduction? {
        val targets = (0 until grid.cellCount).filter { cell ->
            cell !in cluster.cells &&
                cluster.digit in grid.candidatesAt(cell) &&
                cluster.first.any { grid.sees(cell, it) } &&
                cluster.second.any { grid.sees(cell, it) }
        }
        if (targets.isEmpty()) return null
        return Deduction(
            technique = TechniqueId.SIMPLE_COLOURING,
            focusCells = cluster.cells.sorted(),
            focusCandidates = cluster.cells.sorted().map { CellDigit(it, cluster.digit) },
            eliminations = targets.map { CellDigit(it, cluster.digit) },
        )
    }
}

/**
 * Multi colouring. Two separate chains on the same digit, played against each other.
 *
 * Rule one: if a colour of chain A sees both colours of chain B, it is false. One of B's
 * colours is true, and that colour would clash with it either way.
 *
 * Rule two: if a colour of A merely sees a colour of B, then between the two opposite
 * colours at least one must be true. Anything seeing both of those loses the digit.
 */
public object MultiColouring : Technique {

    override val id: TechniqueId get() = TechniqueId.MULTI_COLOURING

    override fun find(grid: CandidateGrid): Deduction? {
        for (digit in 1..grid.size) {
            val clusters = clustersFor(grid, digit)
            if (clusters.size < 2) continue

            for (leftIndex in clusters.indices) {
                for (rightIndex in leftIndex + 1 until clusters.size) {
                    val left = clusters[leftIndex]
                    val right = clusters[rightIndex]
                    if (left.cells.intersect(right.cells).isNotEmpty()) continue

                    ruleOne(grid, left, right)?.let { return it }
                    ruleOne(grid, right, left)?.let { return it }
                    ruleTwo(grid, left, right)?.let { return it }
                }
            }
        }
        return null
    }

    /** A colour of [target] that sees both colours of [other] cannot be true. */
    private fun ruleOne(grid: CandidateGrid, target: Cluster, other: Cluster): Deduction? {
        for (colour in target.colours) {
            if (!grid.anySees(colour, other.first)) continue
            if (!grid.anySees(colour, other.second)) continue
            val eliminations = colour.filter { target.digit in grid.candidatesAt(it) }
            if (eliminations.isEmpty()) continue
            return Deduction(
                technique = TechniqueId.MULTI_COLOURING,
                focusCells = (target.cells + other.cells).sorted(),
                focusCandidates = (target.cells + other.cells).sorted()
                    .map { CellDigit(it, target.digit) },
                eliminations = eliminations.map { CellDigit(it, target.digit) },
            )
        }
        return null
    }

    /** If one colour of each chain clash, the two opposite colours cover the digit. */
    private fun ruleTwo(grid: CandidateGrid, left: Cluster, right: Cluster): Deduction? {
        val digit = left.digit
        for (leftColour in left.colours) {
            for (rightColour in right.colours) {
                if (!grid.anySees(leftColour, rightColour)) continue
                val leftOther = left.other(leftColour)
                val rightOther = right.other(rightColour)
                if (leftOther.isEmpty() || rightOther.isEmpty()) continue

                val targets = (0 until grid.cellCount).filter { cell ->
                    cell !in left.cells && cell !in right.cells &&
                        digit in grid.candidatesAt(cell) &&
                        leftOther.any { grid.sees(cell, it) } &&
                        rightOther.any { grid.sees(cell, it) }
                }
                if (targets.isEmpty()) continue

                return Deduction(
                    technique = TechniqueId.MULTI_COLOURING,
                    focusCells = (left.cells + right.cells).sorted(),
                    focusCandidates = (left.cells + right.cells).sorted().map { CellDigit(it, digit) },
                    eliminations = targets.map { CellDigit(it, digit) },
                )
            }
        }
        return null
    }
}
