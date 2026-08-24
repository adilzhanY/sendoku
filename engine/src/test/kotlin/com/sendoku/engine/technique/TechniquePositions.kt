package com.sendoku.engine.technique

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Generator
import com.sendoku.engine.House
import com.sendoku.engine.HouseKind
import kotlin.random.Random

/**
 * One hand built position per technique, in one place.
 *
 * Each grid is cut down from an empty board by striking candidates, which keeps the
 * positions small enough to read and to reason about. Every one of them is the minimum
 * shape the rule needs, so if a rule stops firing on its own position, the rule broke.
 */
internal object TechniquePositions {

    private val classic = Dimensions.CLASSIC

    private fun blank() = CandidateGrid.of(Board(classic))

    private fun CandidateGrid.only(cell: Int, vararg keep: Int) = apply {
        val kept = Candidates.of(*keep)
        Candidates.all(dims).forEach { if (it !in kept) eliminate(cell, it) }
    }

    private fun CandidateGrid.confine(house: House, digit: Int, vararg keep: Int) = apply {
        for (cell in cellsOf(house)) if (cell !in keep) eliminate(cell, digit)
    }

    private fun CandidateGrid.confineRow(row: Int, digit: Int, vararg cols: Int) =
        confine(House(HouseKind.ROW, row), digit, *cols.map { indexOf(row, it) }.toIntArray())

    private fun rc(row: Int, col: Int) = row * 9 + col

    val byTechnique: Map<TechniqueId, () -> CandidateGrid> = mapOf(
        TechniqueId.NAKED_SINGLE to { blank().only(rc(4, 4), 7) },

        TechniqueId.HIDDEN_SINGLE to { blank().confineRow(0, 5, 0) },

        // The 5 in the top left box can only sit on the first row.
        TechniqueId.LOCKED_CANDIDATES_POINTING to {
            blank().apply {
                for (row in 1..2) for (col in 0..2) eliminate(rc(row, col), 5)
            }
        },

        // The 5 in the first row can only sit in the top left box.
        TechniqueId.LOCKED_CANDIDATES_CLAIMING to { blank().confineRow(0, 5, 0, 1, 2) },

        TechniqueId.NAKED_PAIR to { blank().only(rc(0, 0), 1, 2).only(rc(0, 1), 1, 2) },

        // The two homes sit in different boxes, or the pair would also be a claiming pair.
        TechniqueId.HIDDEN_PAIR to { blank().confineRow(0, 1, 0, 4).confineRow(0, 2, 0, 4) },

        TechniqueId.NAKED_TRIPLE to {
            blank().only(rc(0, 0), 1, 2).only(rc(0, 1), 2, 3).only(rc(0, 2), 1, 3)
        },

        TechniqueId.HIDDEN_TRIPLE to {
            blank().confineRow(0, 1, 0, 4).confineRow(0, 2, 4, 8).confineRow(0, 3, 0, 8)
        },

        TechniqueId.NAKED_QUAD to {
            blank()
                .only(rc(0, 0), 1, 2).only(rc(0, 1), 2, 3)
                .only(rc(0, 2), 3, 4).only(rc(0, 3), 1, 4)
        },

        TechniqueId.HIDDEN_QUAD to {
            blank()
                .confineRow(0, 1, 0, 1, 2, 3).confineRow(0, 2, 0, 1, 2, 3)
                .confineRow(0, 3, 0, 1, 2, 3).confineRow(0, 4, 0, 1, 2, 3)
        },

        // Rows from different bands, so no box ends up with its 5 pinned to one line.
        TechniqueId.X_WING to { blank().confineRow(0, 5, 0, 4).confineRow(4, 5, 0, 4) },

        TechniqueId.SWORDFISH to {
            blank().confineRow(0, 5, 0, 3).confineRow(3, 5, 3, 6).confineRow(6, 5, 0, 6)
        },

        TechniqueId.JELLYFISH to {
            blank()
                .confineRow(0, 5, 0, 3).confineRow(3, 5, 3, 6)
                .confineRow(4, 5, 6, 8).confineRow(6, 5, 8, 0)
        },

        TechniqueId.XY_WING to {
            blank().only(rc(0, 0), 1, 2).only(rc(0, 4), 1, 3).only(rc(4, 0), 2, 3)
        },

        TechniqueId.XYZ_WING to {
            blank().only(rc(0, 0), 1, 2, 3).only(rc(1, 1), 1, 3).only(rc(0, 5), 2, 3)
        },

        TechniqueId.W_WING to {
            blank().only(rc(0, 0), 1, 2).only(rc(4, 4), 1, 2).confineRow(8, 1, 0, 4)
        },

        TechniqueId.SIMPLE_COLOURING to {
            blank()
                .confine(House(HouseKind.BOX, 0), 5, rc(0, 0), rc(2, 2))
                .confine(House(HouseKind.ROW, 2), 5, rc(2, 2), rc(2, 7))
                .confine(House(HouseKind.COLUMN, 7), 5, rc(2, 7), rc(6, 7))
        },

        TechniqueId.MULTI_COLOURING to {
            blank()
                .confine(House(HouseKind.BOX, 0), 5, rc(0, 0), rc(2, 2))
                .confine(House(HouseKind.BOX, 6), 5, rc(6, 0), rc(8, 2))
        },

        TechniqueId.UNIQUE_RECTANGLE to {
            blank()
                .only(rc(0, 0), 1, 2).only(rc(0, 1), 1, 2)
                .only(rc(3, 0), 1, 2).only(rc(3, 1), 1, 2, 5)
        },

        TechniqueId.BUG_PLUS_ONE to { grave() },

        TechniqueId.REMOTE_PAIRS to {
            blank()
                .only(rc(0, 0), 1, 2).only(rc(0, 4), 1, 2)
                .only(rc(4, 4), 1, 2).only(rc(4, 8), 1, 2)
        },

        TechniqueId.X_CHAIN to {
            blank()
                .confine(House(HouseKind.COLUMN, 0), 5, rc(0, 0), rc(4, 0))
                .confine(House(HouseKind.COLUMN, 4), 5, rc(1, 4), rc(4, 4))
        },

        TechniqueId.XY_CHAIN to {
            blank().only(rc(0, 0), 1, 2).only(rc(0, 4), 2, 3).only(rc(4, 4), 3, 1)
        },

        TechniqueId.ALS_XZ to {
            blank().only(rc(0, 0), 1, 2).only(rc(4, 0), 1, 3).only(rc(4, 1), 2, 3)
        },

        // Three groups in a row. {1,5} and {1,7} in row 1 make the first, the cell {1,2}
        // below is the hinge, and {2,5,9} with {2,9} further along row 2 make the third.
        // The 1 joins the first two, the 2 joins the last two, and the 5 is what falls. The
        // outer groups are two cells apiece on purpose: with one cell each the whole thing
        // is an XY-Wing, which is a rule seven rungs cheaper.
        TechniqueId.ALS_XY_WING to {
            blank()
                .only(rc(0, 0), 1, 5).only(rc(0, 1), 1, 7)
                .only(rc(1, 0), 1, 2)
                .only(rc(1, 4), 2, 5, 9).only(rc(1, 5), 2, 9)
        },

        // Row 0 crosses the top left box in two cells holding four digits between them.
        // A cell further along the row takes {1,2}, a cell lower in the box takes {3,4},
        // and the two of them account for the whole pool.
        TechniqueId.SUE_DE_COQ to {
            blank()
                .only(rc(0, 0), 1, 3).only(rc(0, 1), 2, 4)
                .only(rc(0, 4), 1, 2)
                .only(rc(1, 0), 3, 4)
        },
    )

    /**
     * A bivalue universal grave with one cell left holding a third candidate.
     *
     * Two complete solutions that agree nowhere give every cell two candidates and every
     * digit twice in every house. The centre cell keeps a third, which is the only thing
     * standing between this grid and a pattern that cannot have one solution.
     */
    private fun grave(): CandidateGrid {
        val solution = Generator(classic, Random(31)).completeGrid()
        val grid = blank()
        for (cell in 0 until grid.cellCount) {
            val digit = solution.atIndex(cell)
            val next = digit % 9 + 1
            val keep = if (cell == 40) Candidates.of(digit, next, next % 9 + 1) else Candidates.of(digit, next)
            Candidates.all(classic).forEach { if (it !in keep) grid.eliminate(cell, it) }
        }
        return grid
    }
}
