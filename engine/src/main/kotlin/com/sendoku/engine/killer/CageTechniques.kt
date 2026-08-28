package com.sendoku.engine.killer

import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates
import com.sendoku.engine.Geometry
import com.sendoku.engine.House
import com.sendoku.engine.HouseKind
import com.sendoku.engine.technique.CellDigit
import com.sendoku.engine.technique.Deduction
import com.sendoku.engine.technique.TechniqueId

/**
 * A rule that reads a Killer board and proposes one step.
 *
 * The same shape as an ordinary [com.sendoku.engine.technique.Technique] with one more thing
 * to hand: the cages. Everything a cage technique knows comes from a sum and the fact that a
 * cage never repeats a digit, and both of those live in [KillerPuzzle] rather than in the
 * grid, which is why this cannot simply be the existing interface.
 */
public interface CageTechnique {

    public val id: TechniqueId

    /** The first step this rule can prove on [grid], or null when it can prove none. */
    public fun find(grid: CandidateGrid, puzzle: KillerPuzzle): Deduction?
}

/**
 * A cage whose sum leaves only one set of digits possible.
 *
 * The first thing anybody learns about Killer. Three cells adding to six can only be 1, 2 and
 * 3, so nothing else can go in any of them, whatever the rest of the board says. It is the
 * cage equivalent of a naked single and it is where most of a Killer solve starts.
 */
public object CageSum : CageTechnique {

    override val id: TechniqueId = TechniqueId.CAGE_SUM

    override fun find(grid: CandidateGrid, puzzle: KillerPuzzle): Deduction? {
        for (cage in puzzle.cages) {
            val open = cage.cells.filter { grid.digitAt(it) == 0 }
            if (open.isEmpty()) continue
            val left = cage.sum - cage.cells.sumOf { grid.digitAt(it) }
            val allowed = open.map { grid.candidatesAt(it).mask }
            val fitting = Combinations.fitting(left, allowed, grid.size)
            if (fitting.isEmpty()) continue

            val possible = fitting.fold(0) { mask, combination -> mask or combination }
            val strikes = ArrayList<CellDigit>()
            for (cell in open) {
                for (digit in Candidates(grid.candidatesAt(cell).mask and possible.inv()).toList()) {
                    strikes.add(CellDigit(cell, digit))
                }
            }
            if (strikes.isEmpty()) continue
            return Deduction(
                technique = id,
                focusCells = cage.cells,
                eliminations = strikes,
            )
        }
        return null
    }
}

/**
 * A digit that has only one home left inside its cage.
 *
 * The cage version of a hidden single, and it is not the same claim: a digit can be forced
 * into a cell of a cage because every combination that still fits needs it and only one cell
 * can take it, even when that cell has several candidates of its own.
 */
public object CageSingle : CageTechnique {

    override val id: TechniqueId = TechniqueId.CAGE_SINGLE

    override fun find(grid: CandidateGrid, puzzle: KillerPuzzle): Deduction? {
        for (cage in puzzle.cages) {
            val open = cage.cells.filter { grid.digitAt(it) == 0 }
            if (open.size < 2) continue
            val left = cage.sum - cage.cells.sumOf { grid.digitAt(it) }
            val allowed = open.map { grid.candidatesAt(it).mask }
            val fitting = Combinations.fitting(left, allowed, grid.size)
            if (fitting.isEmpty()) continue

            // A digit every surviving combination needs has to be somewhere in the cage.
            val needed = fitting.reduce { all, combination -> all and combination }
            for (digit in Candidates(needed).toList()) {
                val homes = open.filter { digit in grid.candidatesAt(it) }
                if (homes.size != 1) continue
                val cell = homes.first()
                if (grid.candidatesAt(cell).size == 1) continue
                return Deduction(
                    technique = id,
                    focusCells = cage.cells,
                    placements = listOf(CellDigit(cell, digit)),
                )
            }
        }
        return null
    }
}

/**
 * What is left of a house once the cages inside it are accounted for.
 *
 * Every row, column and box holds one of each digit, so it adds to a known total: forty five
 * on a nine by nine. A house made up of whole cages plus one stray cell tells you that cell
 * exactly, and the same arithmetic from the other side names a cell that pokes out of the
 * house. Solvers call the two of them innies and outies, and they are the technique that
 * makes a hard Killer start moving at all.
 *
 * This one deliberately stops at a single leftover cell. Two leftover cells give a sum rather
 * than a digit, which is a real technique and a much harder one to explain in a hint, so it
 * belongs to a later release rather than to the first one that can rate a Killer at all.
 */
public object InniesAndOuties : CageTechnique {

    override val id: TechniqueId = TechniqueId.CAGE_INNIE

    override fun find(grid: CandidateGrid, puzzle: KillerPuzzle): Deduction? {
        val geometry = Geometry.of(puzzle.dims)
        val houseTotal = (1..puzzle.dims.size).sum()

        for (kind in HouseKind.entries) {
            for (index in 0 until puzzle.dims.size) {
                val house = House(kind, index)
                val cells = geometry.cellsOf(house).toSet()
                val found = single(grid, puzzle, house, cells, houseTotal)
                if (found != null) return found
            }
        }
        return null
    }

    /** The one cell of [house] that the cages covering it do not settle, if there is one. */
    private fun single(
        grid: CandidateGrid,
        puzzle: KillerPuzzle,
        house: House,
        cells: Set<Int>,
        houseTotal: Int,
    ): Deduction? {
        var sum = 0
        val leftover = ArrayList<Int>()
        val touching = puzzle.cages.filter { cage -> cage.cells.any { it in cells } }

        for (cage in touching) {
            val inside = cage.cells.filter { it in cells }
            if (inside.size == cage.size) {
                sum += cage.sum
            } else {
                // A cage poking out of the house: what it contributes is its sum less the
                // cells outside, and those are only known once they hold digits.
                val outside = cage.cells.filter { it !in cells }
                val placed = outside.map { grid.digitAt(it) }
                if (placed.any { it == 0 }) {
                    leftover += inside
                    continue
                }
                sum += cage.sum - placed.sum()
            }
        }

        val open = leftover.filter { grid.digitAt(it) == 0 }
        if (open.size != 1) return null
        val settled = leftover.filter { grid.digitAt(it) != 0 }.sumOf { grid.digitAt(it) }
        val digit = houseTotal - sum - settled
        val cell = open.first()
        if (digit !in 1..puzzle.dims.size) return null
        if (digit !in grid.candidatesAt(cell)) return null
        if (grid.candidatesAt(cell).size == 1) return null
        return Deduction(
            technique = id,
            focusCells = listOf(cell),
            houses = listOf(house),
            placements = listOf(CellDigit(cell, digit)),
        )
    }
}

/**
 * A cage that lies inside one house, and the digits it therefore owns.
 *
 * Every cell of the cage is in the same row, column or box, so the digits of the cage all
 * live in that house. Any digit that every surviving combination needs is therefore not
 * available anywhere else in the house, which is the cage equivalent of a pointing pair.
 */
public object CageLocked : CageTechnique {

    override val id: TechniqueId = TechniqueId.CAGE_LOCKED

    override fun find(grid: CandidateGrid, puzzle: KillerPuzzle): Deduction? {
        val geometry = Geometry.of(puzzle.dims)
        for (cage in puzzle.cages) {
            val open = cage.cells.filter { grid.digitAt(it) == 0 }
            if (open.size < 2) continue
            val left = cage.sum - cage.cells.sumOf { grid.digitAt(it) }
            val fitting = Combinations.fitting(left, open.map { grid.candidatesAt(it).mask }, grid.size)
            if (fitting.isEmpty()) continue
            val needed = fitting.reduce { all, combination -> all and combination }
            if (needed == 0) continue

            for (house in housesHolding(geometry, puzzle, open)) {
                val others = geometry.cellsOf(house).filter { it !in cage.cells && grid.digitAt(it) == 0 }
                val strikes = ArrayList<CellDigit>()
                for (digit in Candidates(needed).toList()) {
                    for (cell in others) {
                        if (digit in grid.candidatesAt(cell)) strikes.add(CellDigit(cell, digit))
                    }
                }
                if (strikes.isEmpty()) continue
                return Deduction(
                    technique = id,
                    focusCells = cage.cells,
                    houses = listOf(house),
                    eliminations = strikes,
                )
            }
        }
        return null
    }

    /** The houses that hold every one of [cells], which is what makes the cage locked. */
    private fun housesHolding(geometry: Geometry, puzzle: KillerPuzzle, cells: List<Int>): List<House> {
        val houses = ArrayList<House>()
        for (kind in HouseKind.entries) {
            for (index in 0 until puzzle.dims.size) {
                val house = House(kind, index)
                val inside = geometry.cellsOf(house).toSet()
                if (cells.all { it in inside }) houses.add(house)
            }
        }
        return houses
    }
}

/** Every cage rule, cheapest first, the same way the ordinary ladder is ordered. */
public object CageTechniques {

    public val ladder: List<CageTechnique> = listOf(
        CageSum,
        CageSingle,
        CageLocked,
        InniesAndOuties,
    )
}
