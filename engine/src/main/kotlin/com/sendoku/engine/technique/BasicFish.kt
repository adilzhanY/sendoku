package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.House
import com.sendoku.engine.HouseKind

/**
 * X-Wing, the smallest fish and the first technique that reasons across the whole grid
 * rather than inside one region.
 *
 * Take two rows where a digit has only two homes each, and suppose those homes sit in
 * the same two columns. Each row needs that digit once, and the two rows cannot both
 * take the same column, so between them they use up both columns. No other row can hold
 * the digit in either column.
 *
 * The argument works with rows and columns swapped, so the rule is checked both ways.
 */
public object XWing : Technique by BasicFish(TechniqueId.X_WING, size = 2)

/**
 * Swordfish, the same argument as [XWing] over three lines instead of two.
 *
 * Three rows whose homes for a digit all fall inside the same three columns claim those
 * columns. A base row is allowed only two of the three, which is what makes a swordfish
 * hard to see: it does not look like a tidy rectangle.
 */
public object Swordfish : Technique by BasicFish(TechniqueId.SWORDFISH, size = 3)

/**
 * Jellyfish, four lines. The largest basic fish worth searching for, because a fish of
 * five in a nine by nine grid is always mirrored by a smaller one on the other axis.
 */
public object Jellyfish : Technique by BasicFish(TechniqueId.JELLYFISH, size = 4)

/**
 * The shared rule behind every basic fish. X-Wing is two, Swordfish is three, Jellyfish
 * is four, and the argument never changes.
 *
 * `n` base lines whose homes for a digit all fall inside `n` cover lines have claimed
 * those cover lines: `n` lines need `n` distinct places for the digit, so the cover
 * lines are used up and no line outside the base can hold the digit there.
 *
 * A base line with fewer than `n` homes still counts, which is why the search allows
 * `2..n` rather than insisting on exactly `n`. Fewer than two homes would be a hidden
 * single, which a far cheaper rule already handles.
 */
internal class BasicFish(override val id: TechniqueId, private val size: Int) : Technique {

    init {
        require(size >= 2) { "a fish needs at least two lines" }
    }

    override fun find(grid: CandidateGrid): Deduction? {
        for (digit in 1..grid.size) {
            findFor(grid, digit, HouseKind.ROW, HouseKind.COLUMN)?.let { return it }
            findFor(grid, digit, HouseKind.COLUMN, HouseKind.ROW)?.let { return it }
        }
        return null
    }

    private fun findFor(grid: CandidateGrid, digit: Int, baseKind: HouseKind, coverKind: HouseKind): Deduction? {
        val bases = ArrayList<House>(grid.size)
        val homes = ArrayList<List<Int>>(grid.size)

        for (index in 0 until grid.size) {
            val line = House(baseKind, index)
            if (grid.isPlacedIn(line, digit)) continue
            val where = grid.cellsOf(line).filter { digit in grid.candidatesAt(it) }
            if (where.size !in 2..size) continue
            bases.add(line)
            homes.add(where)
        }
        if (bases.size < size) return null

        var found: Deduction? = null
        forEachCombination(bases.size, size) { picks ->
            val baseLines = picks.map { bases[it] }
            val corners = sortedSetOf<Int>()
            for (pick in picks) corners.addAll(homes[pick])

            val coverIndices = corners.map { lineIndexOf(grid, coverKind, it) }.toSortedSet()
            if (coverIndices.size != size) return@forEachCombination false

            val covers = coverIndices.map { House(coverKind, it) }
            val eliminations = buildList {
                for (cover in covers) {
                    for (cell in grid.cellsOf(cover)) {
                        if (cell in corners) continue
                        if (digit !in grid.candidatesAt(cell)) continue
                        add(CellDigit(cell, digit))
                    }
                }
            }.sortedBy { it.cell }
            if (eliminations.isEmpty()) return@forEachCombination false

            found = Deduction(
                technique = id,
                focusCells = corners.toList(),
                focusCandidates = corners.map { CellDigit(it, digit) },
                houses = baseLines + covers,
                eliminations = eliminations,
            )
            true
        }
        return found
    }

    private fun lineIndexOf(grid: CandidateGrid, kind: HouseKind, cell: Int): Int = when (kind) {
        HouseKind.ROW -> grid.rowOf(cell)
        HouseKind.COLUMN -> grid.colOf(cell)
        HouseKind.BOX -> grid.boxOf(cell)
    }
}
