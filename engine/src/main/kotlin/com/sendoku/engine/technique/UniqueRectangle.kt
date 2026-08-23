package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates
import com.sendoku.engine.House

/**
 * Unique rectangle, types one to four.
 *
 * Four unsolved cells sitting on two rows, two columns and exactly two boxes, all four
 * holding the same pair `{x, y}`, are a deadly pattern: whatever else the grid says, the
 * two digits could be swapped around the rectangle to give a second solution. A proper
 * puzzle has one solution, so that arrangement can never happen, and something has to
 * break it.
 *
 * This is the first rule that reasons about the puzzle rather than the grid. It is only
 * sound because Sendoku never ships a puzzle with more than one solution, and it only
 * looks at unsolved cells, which by definition were not given.
 *
 * The two boxes matter. Spread the four cells across four boxes and the swap stops being
 * legal, because each box would end up holding the same digit twice.
 */
public object UniqueRectangle : Technique {

    override val id: TechniqueId get() = TechniqueId.UNIQUE_RECTANGLE

    override fun find(grid: CandidateGrid): Deduction? {
        val size = grid.size
        for (topRow in 0 until size - 1) {
            for (bottomRow in topRow + 1 until size) {
                for (leftCol in 0 until size - 1) {
                    for (rightCol in leftCol + 1 until size) {
                        val corners = listOf(
                            topRow * size + leftCol,
                            topRow * size + rightCol,
                            bottomRow * size + leftCol,
                            bottomRow * size + rightCol,
                        )
                        val deduction = inspect(grid, corners)
                        if (deduction != null) return deduction
                    }
                }
            }
        }
        return null
    }

    private fun inspect(grid: CandidateGrid, corners: List<Int>): Deduction? {
        if (corners.any { !grid.isEmpty(it) }) return null
        if (corners.map { grid.boxOf(it) }.distinct().size != 2) return null

        var common = Candidates.all(grid.dims)
        for (cell in corners) common = common and grid.candidatesAt(cell)
        if (common.size < 2) return null

        val digits = common.toList()
        for (i in digits.indices) {
            for (j in i + 1 until digits.size) {
                val pair = Candidates.of(digits[i], digits[j])
                val floor = corners.filter { grid.candidatesAt(it) == pair }
                val roof = corners.filter { grid.candidatesAt(it) != pair }

                typeOne(grid, corners, pair, floor, roof)?.let { return it }
                if (floor.size != 2) continue
                typeTwo(grid, corners, pair, roof)?.let { return it }
                typeFour(grid, corners, pair, roof)?.let { return it }
                typeThree(grid, corners, pair, roof)?.let { return it }
            }
        }
        return null
    }

    /**
     * Type one. Three corners hold nothing but the pair, so the fourth cannot hold the
     * pair either: it would complete the deadly pattern. Whatever extra it carries is
     * therefore its value.
     */
    private fun typeOne(
        grid: CandidateGrid,
        corners: List<Int>,
        pair: Candidates,
        floor: List<Int>,
        roof: List<Int>,
    ): Deduction? {
        if (floor.size != 3 || roof.size != 1) return null
        val odd = roof.single()
        val eliminations = (grid.candidatesAt(odd) and pair).toList().map { CellDigit(odd, it) }
        if (eliminations.isEmpty()) return null
        return deduction(grid, corners, pair, eliminations)
    }

    /**
     * Type two. Both remaining corners carry the same single extra digit. One of them has
     * to take it, or the rectangle closes, so nothing seeing both of them can hold it.
     */
    private fun typeTwo(
        grid: CandidateGrid,
        corners: List<Int>,
        pair: Candidates,
        roof: List<Int>,
    ): Deduction? {
        val extras = roof.map { grid.candidatesAt(it) without pair }
        if (extras.any { !it.isSingle } || extras[0] != extras[1]) return null
        val extra = extras[0].single

        val targets = grid.seenByBoth(roof[0], roof[1])
            .filter { it !in corners && extra in grid.candidatesAt(it) }
        if (targets.isEmpty()) return null
        return deduction(grid, corners, pair, targets.map { CellDigit(it, extra) })
    }

    /**
     * Type four. The two remaining corners share a house, and inside it one of the pair
     * digits has nowhere else to go.
     *
     * Say y is locked to those two corners. Putting x in either of them forces y into the
     * other, and then all four corners hold nothing but x and y. So x goes from both.
     */
    private fun typeFour(
        grid: CandidateGrid,
        corners: List<Int>,
        pair: Candidates,
        roof: List<Int>,
    ): Deduction? {
        val shared = sharedHouses(grid, roof[0], roof[1])
        if (shared.isEmpty()) return null
        val digits = pair.toList()

        for (locked in digits) {
            val drop = digits.first { it != locked }
            for (house in shared) {
                if (grid.isPlacedIn(house, locked)) continue
                val homes = grid.cellsOf(house).filter { locked in grid.candidatesAt(it) }
                if (homes.size != 2 || homes.toSet() != roof.toSet()) continue

                val eliminations = roof
                    .filter { drop in grid.candidatesAt(it) }
                    .map { CellDigit(it, drop) }
                if (eliminations.isEmpty()) continue
                return deduction(grid, corners, pair, eliminations, listOf(house))
            }
        }
        return null
    }

    /**
     * Type three. The two remaining corners between them carry a set of extra digits, and
     * at least one of those extras must be used. That makes the pair of corners behave
     * like a single cell holding exactly those extras, which can then join a naked subset
     * with real cells in a house the corners share.
     */
    private fun typeThree(
        grid: CandidateGrid,
        corners: List<Int>,
        pair: Candidates,
        roof: List<Int>,
    ): Deduction? {
        val extras = (grid.candidatesAt(roof[0]) or grid.candidatesAt(roof[1])) without pair
        if (extras.size < 2 || extras.size > 4) return null
        val subsetSize = extras.size

        for (house in sharedHouses(grid, roof[0], roof[1])) {
            val others = grid.cellsOf(house).filter {
                it !in corners && grid.isEmpty(it) && grid.candidatesAt(it).size in 2..subsetSize
            }
            if (others.size < subsetSize - 1) continue

            var found: Deduction? = null
            forEachCombination(others.size, subsetSize - 1) { picks ->
                val partners = picks.map { others[it] }
                var union = extras
                for (cell in partners) union = union or grid.candidatesAt(cell)
                if (union.size != subsetSize) return@forEachCombination false

                val eliminations = buildList {
                    for (cell in grid.cellsOf(house)) {
                        if (cell in corners || cell in partners) continue
                        (grid.candidatesAt(cell) and union).forEach { add(CellDigit(cell, it)) }
                    }
                }
                if (eliminations.isEmpty()) return@forEachCombination false

                found = deduction(grid, corners, pair, eliminations, listOf(house))
                true
            }
            if (found != null) return found
        }
        return null
    }

    private fun sharedHouses(grid: CandidateGrid, a: Int, b: Int): List<House> =
        grid.housesOf(a).filter { it in grid.housesOf(b) }

    private fun deduction(
        grid: CandidateGrid,
        corners: List<Int>,
        pair: Candidates,
        eliminations: List<CellDigit>,
        houses: List<House> = emptyList(),
    ): Deduction = Deduction(
        technique = TechniqueId.UNIQUE_RECTANGLE,
        focusCells = corners,
        focusCandidates = corners.flatMap { cell ->
            (grid.candidatesAt(cell) and pair).toList().map { CellDigit(cell, it) }
        },
        houses = houses,
        eliminations = eliminations.sortedWith(compareBy({ it.cell }, { it.digit })),
    )
}
