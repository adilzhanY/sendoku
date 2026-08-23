package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.House
import com.sendoku.engine.HouseKind

/**
 * Pointing, also called locked candidates type 1.
 *
 * If every place a digit could go inside one box lies on a single row or column, then
 * the digit is somewhere on that line whatever else happens. So it cannot be anywhere
 * else on that line, outside the box.
 *
 * This places nothing. It only strikes pencil marks, which is what makes it the first
 * technique on the ladder that a player has to think about rather than see.
 */
public object PointingCandidates : Technique {

    override val id: TechniqueId get() = TechniqueId.LOCKED_CANDIDATES_POINTING

    override fun find(grid: CandidateGrid): Deduction? {
        for (box in 0 until grid.size) {
            val house = House(HouseKind.BOX, box)
            for (digit in 1..grid.size) {
                if (grid.isPlacedIn(house, digit)) continue
                val homes = grid.cellsOf(house).filter { digit in grid.candidatesAt(it) }
                if (homes.size < 2) continue

                val line = sharedLine(grid, homes) ?: continue
                val eliminations = grid.cellsOf(line)
                    .filter { it !in homes && digit in grid.candidatesAt(it) }
                    .map { CellDigit(it, digit) }
                if (eliminations.isEmpty()) continue

                return Deduction(
                    technique = id,
                    focusCells = homes,
                    focusCandidates = homes.map { CellDigit(it, digit) },
                    houses = listOf(house, line),
                    eliminations = eliminations,
                )
            }
        }
        return null
    }

    /** The row or column every one of [cells] sits on, or null when they are spread out. */
    private fun sharedLine(grid: CandidateGrid, cells: List<Int>): House? {
        val row = grid.rowOf(cells.first())
        if (cells.all { grid.rowOf(it) == row }) return House(HouseKind.ROW, row)
        val col = grid.colOf(cells.first())
        if (cells.all { grid.colOf(it) == col }) return House(HouseKind.COLUMN, col)
        return null
    }
}

/**
 * Claiming, also called locked candidates type 2. The mirror of [PointingCandidates].
 *
 * If every place a digit could go on one line lies inside a single box, the digit is in
 * that box, so it cannot be anywhere else in the box, off the line.
 */
public object ClaimingCandidates : Technique {

    override val id: TechniqueId get() = TechniqueId.LOCKED_CANDIDATES_CLAIMING

    override fun find(grid: CandidateGrid): Deduction? {
        for (kind in listOf(HouseKind.ROW, HouseKind.COLUMN)) {
            for (index in 0 until grid.size) {
                val line = House(kind, index)
                val deduction = findIn(grid, line)
                if (deduction != null) return deduction
            }
        }
        return null
    }

    private fun findIn(grid: CandidateGrid, line: House): Deduction? {
        for (digit in 1..grid.size) {
            if (grid.isPlacedIn(line, digit)) continue
            val homes = grid.cellsOf(line).filter { digit in grid.candidatesAt(it) }
            if (homes.size < 2) continue

            val box = grid.boxOf(homes.first())
            if (homes.any { grid.boxOf(it) != box }) continue

            val boxHouse = House(HouseKind.BOX, box)
            val eliminations = grid.cellsOf(boxHouse)
                .filter { it !in homes && digit in grid.candidatesAt(it) }
                .map { CellDigit(it, digit) }
            if (eliminations.isEmpty()) continue

            return Deduction(
                technique = id,
                focusCells = homes,
                focusCandidates = homes.map { CellDigit(it, digit) },
                houses = listOf(line, boxHouse),
                eliminations = eliminations,
            )
        }
        return null
    }
}
