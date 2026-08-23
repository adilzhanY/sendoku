package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid

/**
 * X-Chain. One digit, links that alternate between strong and weak.
 *
 * A strong link is a house where the digit has exactly two homes, so at least one of them
 * holds it. A weak link is any two cells that see each other, so at most one of them holds
 * it. Alternate them, starting and ending with a strong link, and the two ends of the
 * chain cannot both be empty of the digit. Whatever sees both ends therefore loses it.
 *
 * The shortest useful chain is three links, which is the shape sold under the names
 * skyscraper, two string kite and turbot fish. This finds those and a little beyond, but
 * not arbitrarily long chains: the search branches on every cell that sees the current
 * one, so depth is capped to keep a full puzzle rating under a millisecond.
 */
public object XChain : Technique {

    /** Longest chain searched, counted in links. Must stay odd. */
    private const val MAX_LINKS = 5

    override val id: TechniqueId get() = TechniqueId.X_CHAIN

    override fun find(grid: CandidateGrid): Deduction? {
        for (digit in 1..grid.size) {
            val strong = strongLinks(grid, digit)
            if (strong.isEmpty()) continue
            for (start in strong.keys.sorted()) {
                val path = ArrayList<Int>()
                path.add(start)
                val found = walk(grid, digit, strong, start, path)
                if (found != null) return found
            }
        }
        return null
    }

    /** The path ends where a strong link is due. Takes one, then optionally a weak one. */
    private fun walk(
        grid: CandidateGrid,
        digit: Int,
        strong: Map<Int, List<Int>>,
        start: Int,
        path: MutableList<Int>,
    ): Deduction? {
        val current = path.last()
        for (next in strong[current].orEmpty()) {
            if (next in path) continue
            path.add(next)
            val links = path.size - 1

            if (links >= 3) {
                val targets = grid.seenByBoth(start, next)
                    .filter { it !in path && digit in grid.candidatesAt(it) }
                if (targets.isNotEmpty()) {
                    return Deduction(
                        technique = TechniqueId.X_CHAIN,
                        focusCells = path.toList(),
                        focusCandidates = path.map { CellDigit(it, digit) },
                        eliminations = targets.sorted().map { CellDigit(it, digit) },
                    )
                }
            }

            if (links + 2 <= MAX_LINKS) {
                for (weak in grid.peersOf(next)) {
                    if (digit !in grid.candidatesAt(weak) || weak in path) continue
                    path.add(weak)
                    val found = walk(grid, digit, strong, start, path)
                    path.removeAt(path.lastIndex)
                    if (found != null) return found
                }
            }
            path.removeAt(path.lastIndex)
        }
        return null
    }

    /** Cell to the cells it is conjugate with, for one digit. */
    private fun strongLinks(grid: CandidateGrid, digit: Int): Map<Int, List<Int>> {
        val links = HashMap<Int, MutableList<Int>>()
        for (house in grid.houses) {
            if (grid.isPlacedIn(house, digit)) continue
            val homes = grid.cellsOf(house).filter { digit in grid.candidatesAt(it) }
            if (homes.size != 2) continue
            val (a, b) = homes
            links.getOrPut(a) { ArrayList() }.let { if (b !in it) it.add(b) }
            links.getOrPut(b) { ArrayList() }.let { if (a !in it) it.add(a) }
        }
        for (list in links.values) list.sort()
        return links
    }
}

/**
 * XY-Chain. A chain of two candidate cells, each handing the next one its value.
 *
 * Start at a cell holding `{x, a}` and suppose it is not x, so it is a. The next cell sees
 * it and also holds a, so that one is not a, so it is whatever else it holds. Follow the
 * implication along and if the last cell in the chain is forced to x, then either the
 * first cell was x all along or the last one is. Nothing that sees both can be x.
 *
 * Every cell in the chain holds exactly two candidates, which keeps the branching low and
 * makes this the one chain technique cheap enough to run to real depth.
 */
public object XYChain : Technique {

    /** Longest chain searched, counted in cells. */
    private const val MAX_CELLS = 8

    override val id: TechniqueId get() = TechniqueId.XY_CHAIN

    override fun find(grid: CandidateGrid): Deduction? {
        val bivalue = (0 until grid.cellCount).filter { grid.candidatesAt(it).size == 2 }
        if (bivalue.size < 3) return null

        for (start in bivalue) {
            for (target in grid.candidatesAt(start).toList()) {
                val carried = grid.candidatesAt(start).toList().first { it != target }
                val path = ArrayList<Int>()
                path.add(start)
                val found = walk(grid, bivalue, path, target, carried)
                if (found != null) return found
            }
        }
        return null
    }

    /**
     * [carried] is the value the last cell of [path] is forced to, on the assumption that
     * the first cell is not [target]. The chain closes when a cell is forced to [target].
     */
    private fun walk(
        grid: CandidateGrid,
        bivalue: List<Int>,
        path: MutableList<Int>,
        target: Int,
        carried: Int,
    ): Deduction? {
        val current = path.last()
        for (next in grid.peersOf(current)) {
            if (next in path) continue
            val pair = grid.candidatesAt(next)
            if (pair.size != 2 || carried !in pair) continue
            val forced = pair.toList().first { it != carried }

            path.add(next)
            if (forced == target && path.size >= 3) {
                val targets = grid.seenByBoth(path.first(), next)
                    .filter { it !in path && target in grid.candidatesAt(it) }
                if (targets.isNotEmpty()) {
                    return Deduction(
                        technique = TechniqueId.XY_CHAIN,
                        focusCells = path.toList(),
                        focusCandidates = path.flatMap { cell ->
                            grid.candidatesAt(cell).toList().map { CellDigit(cell, it) }
                        },
                        eliminations = targets.sorted().map { CellDigit(it, target) },
                    )
                }
            }
            if (path.size < MAX_CELLS) {
                val found = walk(grid, bivalue, path, target, forced)
                if (found != null) return found
            }
            path.removeAt(path.lastIndex)
        }
        return null
    }
}
