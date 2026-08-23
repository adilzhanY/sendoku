package com.sendoku.app.ui

import com.sendoku.engine.House
import com.sendoku.engine.technique.Deduction
import com.sendoku.engine.technique.TechniqueId

/**
 * Every technique, explained in words a person would use.
 *
 * This is the difference between an app that helps and an app that finishes the puzzle for
 * you. "Hidden pair" means nothing to somebody who does not already know what it means, so
 * each one says what to look for, and then why it works.
 *
 * The text lives here rather than in the engine because it is language, and the engine has
 * none. It will move into string resources when the app is translated, and the technique
 * names will need a translator who plays sudoku rather than one who does not.
 */
public object TechniqueCopy {

    /** One line: what kind of thing to go and look for. Shown at the first tap. */
    public fun lookFor(id: TechniqueId): String = when (id) {
        TechniqueId.NAKED_SINGLE -> "There is a cell with only one digit left that can go in it."
        TechniqueId.HIDDEN_SINGLE -> "There is a digit with only one place left to go."
        TechniqueId.LOCKED_CANDIDATES_POINTING ->
            "In one box, a digit is stuck on a single row or column."
        TechniqueId.LOCKED_CANDIDATES_CLAIMING ->
            "On one line, a digit can only sit inside a single box."
        TechniqueId.NAKED_PAIR -> "Two cells in one region hold the same two digits and nothing else."
        TechniqueId.HIDDEN_PAIR -> "Two digits have nowhere to go in one region except the same two cells."
        TechniqueId.NAKED_TRIPLE -> "Three cells in one region share three digits between them."
        TechniqueId.HIDDEN_TRIPLE -> "Three digits are confined to the same three cells."
        TechniqueId.NAKED_QUAD -> "Four cells in one region share four digits between them."
        TechniqueId.HIDDEN_QUAD -> "Four digits are confined to the same four cells."
        TechniqueId.X_WING -> "Two rows hold a digit in the same two columns, or the other way round."
        TechniqueId.SWORDFISH -> "Three rows hold a digit inside the same three columns."
        TechniqueId.JELLYFISH -> "Four rows hold a digit inside the same four columns."
        TechniqueId.XY_WING -> "Three cells of two digits each, arranged so one digit is squeezed out."
        TechniqueId.XYZ_WING -> "Like an XY-Wing, but the middle cell has a third digit too."
        TechniqueId.W_WING -> "Two cells holding the same pair, joined by a digit with only two homes."
        TechniqueId.SIMPLE_COLOURING -> "One digit forms a chain that alternates all the way along."
        TechniqueId.MULTI_COLOURING -> "Two separate chains on one digit, played off against each other."
        TechniqueId.REMOTE_PAIRS -> "A chain of cells all holding the very same two digits."
        TechniqueId.UNIQUE_RECTANGLE ->
            "Four cells form a rectangle that would give the puzzle two answers."
        TechniqueId.BUG_PLUS_ONE -> "Every cell but one is down to two digits."
        TechniqueId.X_CHAIN -> "A chain on one digit, alternating between must and cannot."
        TechniqueId.XY_CHAIN -> "A chain of two digit cells, each one forcing the next."
        TechniqueId.ALS_XZ -> "Two groups of cells, each one digit away from being locked."
    }

    /** Why it works. Shown at the last tap, once the player has seen the cells. */
    public fun because(id: TechniqueId): String = when (id) {
        TechniqueId.NAKED_SINGLE ->
            "Every other digit already appears in its row, its column or its box, so only " +
                "one is left. It has to go here."
        TechniqueId.HIDDEN_SINGLE ->
            "The region needs this digit somewhere, and every other cell in it is ruled out, " +
                "so this is the only place left."
        TechniqueId.LOCKED_CANDIDATES_POINTING ->
            "The box has to hold this digit somewhere, and every place it could go lies on " +
                "one line. So the digit is on that line, and it cannot be anywhere else along it."
        TechniqueId.LOCKED_CANDIDATES_CLAIMING ->
            "The line has to hold this digit somewhere, and every place it could go is inside " +
                "one box. So the digit is in that box, and it cannot be elsewhere in the box."
        TechniqueId.NAKED_PAIR ->
            "Those two cells will take those two digits between them, one each. No other cell " +
                "in the region can have either."
        TechniqueId.HIDDEN_PAIR ->
            "The region needs both digits, and only these two cells can take them, so they " +
                "take one each. Everything else in those two cells is out."
        TechniqueId.NAKED_TRIPLE ->
            "Three cells, three digits, one each. Nothing else in the region can use them, " +
                "even though no single cell holds all three."
        TechniqueId.HIDDEN_TRIPLE ->
            "Three digits with nowhere else to go fill three cells between them, so anything " +
                "else in those cells is impossible."
        TechniqueId.NAKED_QUAD ->
            "Four cells take four digits between them, which locks all four out of the rest " +
                "of the region."
        TechniqueId.HIDDEN_QUAD ->
            "Four digits confined to four cells fill them between them, so nothing else can " +
                "live there."
        TechniqueId.X_WING ->
            "Each of the two rows needs the digit once, and both can only place it in the same " +
                "two columns. Between them they use up both columns, so no other row can have " +
                "the digit there."
        TechniqueId.SWORDFISH ->
            "Three rows needing the digit, and only three columns to put it in. The three rows " +
                "use up all three columns, so nothing else in those columns can have it."
        TechniqueId.JELLYFISH ->
            "Four rows and four columns, the same argument again. The four rows fill all four " +
                "columns between them."
        TechniqueId.XY_WING ->
            "The middle cell is one of its two digits. Either way, one of the two outer cells " +
                "is forced to the shared digit, so anything seeing both of them cannot be it."
        TechniqueId.XYZ_WING ->
            "The middle cell can also be the shared digit, so the conclusion is weaker: only a " +
                "cell that sees all three loses it."
        TechniqueId.W_WING ->
            "If neither of the pair cells took the second digit, both would take the first, and " +
                "the linked region would have nowhere left to put it. So one of them is the " +
                "second digit."
        TechniqueId.SIMPLE_COLOURING ->
            "Colour the chain alternately and exactly one colour is true. Either a colour lands " +
                "twice in one region and is therefore false, or a cell outside sees both " +
                "colours and loses the digit whichever way it falls."
        TechniqueId.MULTI_COLOURING ->
            "One colour of the first chain clashes with a colour of the second, which forces " +
                "the digit into one of the two opposite colours. Anything seeing both of those " +
                "cannot hold it."
        TechniqueId.REMOTE_PAIRS ->
            "Neighbours in the chain see each other, so they alternate. The two ends are an odd " +
                "number of steps apart and therefore hold one digit each, and anything seeing " +
                "both can have neither."
        TechniqueId.UNIQUE_RECTANGLE ->
            "If those four cells held nothing but the same two digits, they could be swapped " +
                "around the rectangle and the puzzle would have two answers. It has one, so " +
                "that arrangement is impossible."
        TechniqueId.BUG_PLUS_ONE ->
            "If the extra candidate were removed, every digit would appear exactly twice in " +
                "every region, and a grid like that always has an even number of answers. This " +
                "puzzle has one, so the extra candidate is the answer."
        TechniqueId.X_CHAIN ->
            "The chain alternates between places the digit must be and places it cannot be. " +
                "The two ends cannot both be empty of it, so anything seeing both loses it."
        TechniqueId.XY_CHAIN ->
            "Suppose the first cell is not the digit. Follow the chain along and the last cell " +
                "is forced to be it. So one end or the other holds it."
        TechniqueId.ALS_XZ ->
            "One shared digit can only appear in one of the two groups, which locks the other " +
                "group and forces it to use every digit it has. So the second shared digit " +
                "lands in one group or the other."
    }

    /** The region a hint is about, if it is about one. */
    public fun where(deduction: Deduction): String? = when {
        deduction.houses.isEmpty() -> null
        deduction.houses.size == 1 -> "in ${deduction.houses.first()}"
        else -> "in " + deduction.houses.joinToString(" and ") { it.toString() }
    }

    /** What the step actually does, in one line. */
    public fun outcome(deduction: Deduction): String {
        val placement = deduction.placements.firstOrNull()
        if (placement != null) {
            return "So ${cellName(deduction, placement.cell)} is a ${placement.digit}."
        }
        val digits = deduction.eliminations.map { it.digit }.distinct().sorted()
        val cells = deduction.eliminations.map { it.cell }.distinct()
        val digitText = when (digits.size) {
            1 -> "the ${digits.first()}"
            2 -> "the ${digits[0]} and the ${digits[1]}"
            else -> "the " + digits.dropLast(1).joinToString(", ") + " and the " + digits.last()
        }
        val cellText = if (cells.size == 1) "one cell" else "${cells.size} cells"
        return "So $digitText can go from $cellText."
    }

    private fun cellName(deduction: Deduction, cell: Int): String {
        // Nine by nine is the only shape shipping, and the copy reads better with real
        // coordinates than with an index.
        val row = cell / 9 + 1
        val column = cell % 9 + 1
        return "row $row, column $column"
    }
}

/** Reads as `row 4`, counting from one the way a player would. */
internal fun House.readable(): String = toString()
