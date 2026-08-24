package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates
import com.sendoku.engine.House
import com.sendoku.engine.HouseKind

/**
 * Sue de Coq. A box and a line share a few cells, and those cells are made to choose.
 *
 * Look at the cells where one box crosses one line. Call them the crossing. Take `n` of
 * them, empty, and count the digits they hold between them. When there are exactly `n + 2`
 * of those digits, the crossing is two digits short of being locked: it will use `n` of
 * them and leave two over.
 *
 * Now find two almost locked sets, one further along the line and one elsewhere in the
 * box, whose digits all come from that same pool, which share no digit with each other,
 * and which together account for the whole pool. Between them they claim the two digits
 * the crossing leaves over, and the arithmetic only works one way.
 *
 * The consequence: every digit belonging to the set in the line is used up inside the line
 * part of the pattern, and every digit belonging to the set in the box is used up inside
 * the box part. Anything else in that line loses the first group, anything else in that
 * box loses the second.
 *
 * Suppose a digit x of the line set escaped the pattern. Then neither the crossing nor the
 * line set holds it, so the crossing must take its digits from what is left, and the count
 * forces the crossing onto exactly the digits the box set needs. The box set and the
 * crossing share a box, so they cannot both have them, and the assumption dies. That is
 * the whole proof, and it is why the sizes have to add up before anything may be struck.
 */
public object SueDeCoq : Technique {

    /** Cells taken from the crossing. Two or three, which is all a classic box allows. */
    private const val MAX_CROSSING = 3

    /** Cells in one of the two outer sets. Bigger than this and the shape stops reading. */
    private const val MAX_OUTER = 3

    override val id: TechniqueId get() = TechniqueId.SUE_DE_COQ

    override fun find(grid: CandidateGrid): Deduction? {
        for (box in grid.houses.filter { it.kind == HouseKind.BOX }) {
            val boxCells = grid.cellsOf(box)
            for (line in linesThrough(grid, boxCells)) {
                val lineCells = grid.cellsOf(line)
                val onTheLine = lineCells.toHashSet()
                val crossing = boxCells.filter { it in onTheLine && grid.isEmpty(it) }
                if (crossing.size < 2) continue

                val found = crossings(grid, box, line, crossing)
                if (found != null) return found
            }
        }
        return null
    }

    /** Every row and column that meets this box, as houses. */
    private fun linesThrough(grid: CandidateGrid, boxCells: IntArray): List<House> {
        val rows = boxCells.map { grid.rowOf(it) }.distinct().map { House(HouseKind.ROW, it) }
        val columns = boxCells.map { grid.colOf(it) }.distinct().map { House(HouseKind.COLUMN, it) }
        return rows + columns
    }

    private fun crossings(grid: CandidateGrid, box: House, line: House, crossing: List<Int>): Deduction? {
        val limit = minOf(MAX_CROSSING, crossing.size)
        for (size in 2..limit) {
            var answer: Deduction? = null
            forEachCombination(crossing.size, size) { picks ->
                val cells = picks.map { crossing[it] }
                var pool = Candidates.EMPTY
                for (cell in cells) pool = pool or grid.candidatesAt(cell)
                if (pool.size == size + 2) {
                    answer = outerSets(grid, box, line, crossing, cells, pool)
                }
                answer != null
            }
            if (answer != null) return answer
        }
        return null
    }

    /**
     * The two outer sets: one along the line, one elsewhere in the box.
     *
     * Both are kept clear of the crossing entirely, not merely of the cells picked from it.
     * A set with a foot in the crossing belongs to the line and the box at once, and the
     * pattern stops being two separate claims on the pool.
     */
    private fun outerSets(
        grid: CandidateGrid,
        box: House,
        line: House,
        crossing: List<Int>,
        picked: List<Int>,
        pool: Candidates,
    ): Deduction? {
        val inLine = almostLocked(grid, line, grid.cellsOf(line).filter { it !in crossing }, pool)
        if (inLine.isEmpty()) return null
        val inBox = almostLocked(grid, box, grid.cellsOf(box).filter { it !in crossing }, pool)
        if (inBox.isEmpty()) return null

        for (lineSet in inLine) {
            for (boxSet in inBox) {
                if (lineSet.candidates overlaps boxSet.candidates) continue
                if (lineSet.candidates.size + boxSet.candidates.size != pool.size) continue
                if (lineSet.cells.any { it in boxSet.cells }) continue

                val struck = ArrayList<CellDigit>()
                strike(grid, grid.cellsOf(line), picked + lineSet.cells, lineSet.candidates, struck)
                strike(grid, grid.cellsOf(box), picked + boxSet.cells, boxSet.candidates, struck)
                if (struck.isEmpty()) continue

                val inside = picked + lineSet.cells + boxSet.cells
                return Deduction(
                    technique = id,
                    focusCells = inside.sorted(),
                    focusCandidates = AlsFinder.marksOf(grid, inside),
                    houses = listOf(box, line),
                    eliminations = struck.distinct().sortedWith(compareBy({ it.cell }, { it.digit })),
                )
            }
        }
        return null
    }

    /** Marks of [digits] on any cell of [house] that is not part of the pattern. */
    private fun strike(
        grid: CandidateGrid,
        house: IntArray,
        spared: List<Int>,
        digits: Candidates,
        into: MutableList<CellDigit>,
    ) {
        for (cell in house) {
            if (cell in spared || !grid.isEmpty(cell)) continue
            val hit = grid.candidatesAt(cell) and digits
            hit.forEach { into.add(CellDigit(cell, it)) }
        }
    }

    /** Almost locked sets inside [cells] whose digits all come from [pool]. */
    private fun almostLocked(grid: CandidateGrid, house: House, cells: List<Int>, pool: Candidates): List<Als> {
        val open = cells.filter { grid.isEmpty(it) && pool.containsAll(grid.candidatesAt(it)) }
        if (open.isEmpty()) return emptyList()

        val sets = ArrayList<Als>()
        val limit = minOf(MAX_OUTER, open.size)
        for (size in 1..limit) {
            forEachCombination(open.size, size) { picks ->
                val chosen = picks.map { open[it] }
                var union = Candidates.EMPTY
                for (cell in chosen) union = union or grid.candidatesAt(cell)
                if (union.size == size + 1) sets.add(Als(house, chosen, union))
                false
            }
        }
        return sets
    }
}
