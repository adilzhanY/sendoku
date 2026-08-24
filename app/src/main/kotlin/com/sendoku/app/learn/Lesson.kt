package com.sendoku.app.learn

import androidx.annotation.StringRes
import com.sendoku.engine.Dimensions
import com.sendoku.engine.technique.TechniqueId

/**
 * A named group of lessons, in the order a player meets them.
 *
 * Named for what the player is learning to do, not for the cost band the techniques sit in.
 * "Digits locked to a line" is a thing you can picture; "cost 2.6 to 2.8" is not.
 *
 * The order of this enum is the order of the course.
 */
public enum class Stage(@StringRes public val title: Int) {
    FIRST_STEPS(com.sendoku.app.R.string.stage_first_steps),
    ONE_AT_A_TIME(com.sendoku.app.R.string.stage_one_at_a_time),
    NOTES(com.sendoku.app.R.string.stage_notes),
    LOCKED_TO_A_LINE(com.sendoku.app.R.string.stage_locked_to_a_line),
    GROUPS(com.sendoku.app.R.string.stage_groups),
    PATTERNS(com.sendoku.app.R.string.stage_patterns),
    WINGS(com.sendoku.app.R.string.stage_wings),
    ONE_ANSWER(com.sendoku.app.R.string.stage_one_answer),
    COLOURING(com.sendoku.app.R.string.stage_colouring),
    CHAINS(com.sendoku.app.R.string.stage_chains),
    THE_FAR_END(com.sendoku.app.R.string.stage_the_far_end),
    THE_DEEP_END(com.sendoku.app.R.string.stage_the_deep_end),
}

/**
 * What a lesson is made of, one beat at a time.
 *
 * A lesson is a script rather than a page of prose with a picture. The player reads a
 * sentence, watches the board do one thing, reads the next sentence. That is how somebody
 * explains a technique across a table, and it is the only format where the board and the
 * words cannot drift apart.
 */
public sealed interface Step {

    /** Say something. The board does not move. */
    public data class Say(@StringRes val text: Int) : Step

    /**
     * Light up some cells while saying something.
     *
     * [focus] is what the argument rests on and [strike] is what it rules out, the same two
     * sets the hint panel already uses, so the board draws a lesson exactly as it draws a hint.
     */
    public data class Show(
        @StringRes val text: Int,
        val focus: Set<Int> = emptySet(),
        val strike: Set<Int> = emptySet(),
    ) : Step

    /** Put a digit in, so the player sees the consequence of what was just argued. */
    public data class Place(@StringRes val text: Int, val cell: Int, val digit: Int) : Step

    /**
     * Stop and wait for the player.
     *
     * [wrong] is said when they place something else. Never a cross and a score: the reason
     * is the whole point of the lesson, so a wrong answer gets the reason again, pointed at
     * the cell they chose.
     */
    public data class YourTurn(@StringRes val text: Int, val cell: Int, val digit: Int, @StringRes val wrong: Int) :
        Step
}

/**
 * One lesson.
 *
 * [teaches] is the techniques this lesson is about, and is empty for the lessons about the
 * rules, about notes, and about how to look. It is a list because the quads lesson covers the
 * naked and the hidden one together: they are the same idea and separating them would be two
 * short lessons that each say half of it.
 *
 * The order the course teaches techniques in has to agree with the engine's ladder, and a test
 * enforces it. Not as one flat list, though. The ladder costs an X-Wing at 3.2 and a hidden
 * pair at 3.4, so a strict reading would teach a fish in the middle of the subsets, and that
 * is a worse course than it is a faithful one. The rule is: stages run in order of their
 * cheapest technique, and inside a stage the lessons follow the ladder exactly.
 *
 * [dims] is the grid the lesson is taught on. The first lessons are four by four on purpose.
 * A player who has never seen a sudoku can hold sixteen cells in their head and can see that
 * a row of four holds one of each digit. On eighty one cells that same sentence is a claim
 * they have to take on trust.
 */
public data class Lesson(
    val id: LessonId,
    val stage: Stage,
    @StringRes val title: Int,
    @StringRes val summary: Int,
    val teaches: List<TechniqueId>,
    val dims: Dimensions,
    /** The starting grid, as text the engine can parse. Empty means an empty grid. */
    val board: String,
    val steps: List<Step>,
) {
    /** The one sentence the player keeps. Shown at the end and again in the course map. */
    val takeaway: Int get() = summary
}

/**
 * A lesson's name, as stored.
 *
 * An enum rather than a string, because progress is written to the database by this name and
 * a typo in a string would silently lose somebody's place. Adding a lesson is adding a
 * constant; renaming one is a migration, which is the right amount of friction.
 */
public enum class LessonId {
    WHAT_A_SUDOKU_IS,
    THE_THREE_HOUSES,
    GIVENS,
    FIRST_SOLVE_FOUR,
    FIRST_SOLVE_SIX,
    NAKED_SINGLE,
    HIDDEN_SINGLE,
    SCANNING,
    FIRST_SOLVE_NINE,
    PENCIL_MARKS,
    WHEN_TO_MARK,
    KEEPING_MARKS_TRUE,
    POINTING_PAIR,
    CLAIMING_PAIR,
    NAKED_PAIR,
    HIDDEN_PAIR,
    NAKED_TRIPLE,
    HIDDEN_TRIPLE,
    QUADS,
    X_WING,
    SWORDFISH,
    JELLYFISH,
    XY_WING,
    XYZ_WING,
    W_WING,
    UNIQUE_RECTANGLE,
    BUG_PLUS_ONE,
    SIMPLE_COLOURING,
    REMOTE_PAIRS,
    MULTI_COLOURING,
    X_CHAIN,
    XY_CHAIN,
    WRITING_A_CHAIN_DOWN,
    SUE_DE_COQ,
    ALS_XZ,
    WHEN_NOTHING_APPLIES,
    WHAT_TO_LOOK_FOR_FIRST,
    A_BEYOND_TOGETHER,
    WHAT_THE_DEEP_END_ASKS,
    ALS_XY_WING,
    DEATH_BLOSSOM,
    FORCING_CHAIN,
}
