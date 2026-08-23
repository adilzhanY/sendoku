package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.House

/**
 * Every human technique the solver knows, in rough order of difficulty.
 *
 * The order is the ladder. A solver that tries techniques in declaration order is
 * trying them cheapest first, and the hardest id a puzzle needs is what gives that
 * puzzle its grade.
 *
 * Most of these have no implementation yet. They are declared up front because the
 * order matters more than the code: adding one later must not renumber the rest.
 */
public enum class TechniqueId(public val displayName: String) {
    NAKED_SINGLE("Naked single"),
    HIDDEN_SINGLE("Hidden single"),
    LOCKED_CANDIDATES_POINTING("Pointing pair or triple"),
    LOCKED_CANDIDATES_CLAIMING("Claiming pair or triple"),
    NAKED_PAIR("Naked pair"),
    HIDDEN_PAIR("Hidden pair"),
    NAKED_TRIPLE("Naked triple"),
    HIDDEN_TRIPLE("Hidden triple"),
    NAKED_QUAD("Naked quad"),
    HIDDEN_QUAD("Hidden quad"),
    X_WING("X-Wing"),
    SIMPLE_COLOURING("Simple colouring"),
    XY_WING("XY-Wing"),
    XYZ_WING("XYZ-Wing"),
    W_WING("W-Wing"),
    SWORDFISH("Swordfish"),
    REMOTE_PAIRS("Remote pairs"),
    UNIQUE_RECTANGLE("Unique rectangle"),
    BUG_PLUS_ONE("BUG plus one"),
    JELLYFISH("Jellyfish"),
    MULTI_COLOURING("Multi colouring"),
    X_CHAIN("X-Chain"),
    XY_CHAIN("XY-Chain"),
    ALS_XZ("ALS XZ"),
}

/** One digit in one cell. The cell is a row-major index into the grid. */
public data class CellDigit(val cell: Int, val digit: Int)

/**
 * One step of human logic, and everything the app needs to draw it.
 *
 * A deduction is inert. Finding it changes nothing, which is what lets the hint system
 * show a step before the player commits to it. [apply] is the only thing that writes.
 *
 * @param technique which rule fired
 * @param focusCells the cells the logic rests on, highlighted by a hint
 * @param focusCandidates the specific pencil marks the logic rests on
 * @param houses the regions the logic rests on, so a hint can outline them
 * @param placements digits proved to belong in a cell
 * @param eliminations pencil marks proved impossible
 */
public data class Deduction(
    val technique: TechniqueId,
    val focusCells: List<Int> = emptyList(),
    val focusCandidates: List<CellDigit> = emptyList(),
    val houses: List<House> = emptyList(),
    val placements: List<CellDigit> = emptyList(),
    val eliminations: List<CellDigit> = emptyList(),
) {

    init {
        require(placements.isNotEmpty() || eliminations.isNotEmpty()) {
            "$technique produced a deduction that changes nothing"
        }
    }

    /** Cells this step writes to, whether by placing a digit or striking a mark. */
    public val changedCells: List<Int>
        get() = (placements.map { it.cell } + eliminations.map { it.cell }).distinct()
}

/**
 * A rule that reads a grid and proposes one step.
 *
 * Implementations are stateless and must not modify the grid they are handed. Returning
 * null means "this rule has nothing to say about this grid right now", which is the
 * normal case for all but one or two techniques on any given board.
 */
public interface Technique {

    public val id: TechniqueId

    /** The first step this rule can prove, or null when it can prove none. */
    public fun find(grid: CandidateGrid): Deduction?
}

/**
 * Writes a deduction into a grid. Returns true when anything actually changed.
 *
 * Eliminations go first so that a step which both strikes marks and places a digit
 * leaves the grid consistent either way.
 */
public fun CandidateGrid.apply(deduction: Deduction): Boolean {
    var changed = false
    for ((cell, digit) in deduction.eliminations) {
        if (eliminate(cell, digit)) changed = true
    }
    for ((cell, digit) in deduction.placements) {
        place(cell, digit)
        changed = true
    }
    return changed
}
