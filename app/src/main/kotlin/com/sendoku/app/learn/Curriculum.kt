package com.sendoku.app.learn

import com.sendoku.app.R
import com.sendoku.engine.Dimensions
import com.sendoku.engine.technique.TechniqueId
import com.sendoku.engine.technique.Techniques

/**
 * The course, as data.
 *
 * Written and reviewed as a list before any of it had a screen, which is the only way to see
 * whether a course makes sense. A curriculum built screen by screen becomes whatever the
 * screens made easy.
 *
 * Two rules hold this together and both are tested rather than trusted:
 *
 *  - Lessons that teach a technique appear in the order the engine costs them. The ladder
 *    decides what is harder than what, in one place, and the course reads it.
 *  - Every board here is a legal position, and every digit a lesson places is the digit the
 *    solution has. A lesson cannot teach something the solver disagrees with.
 *
 * Cell numbers are row major from zero. On a four by four that is 0 to 15, and cell 6 is the
 * third cell of the second row.
 */
public object Curriculum {

    /**
     * Four by four is where the rules are obvious rather than asserted.
     *
     * Two cells empty, and the pair of them is exactly the argument the fourth lesson makes.
     * Generated and checked rather than drawn by hand: the first grid here was hand drawn and
     * had two answers, which the curriculum test caught before anybody read the lesson.
     *
     * ```
     * 1 . | . 4
     * 2 4 | 1 3
     * ----+----
     * 4 1 | 3 2
     * 3 2 | 4 1
     * ```
     */
    private const val FOUR_BY_FOUR = "1..4241341323241"

    /**
     * The same idea one size up, where a box stops being square and the eye has to work.
     *
     * One cell empty, in the top left box, so the lesson can ask for it without the player
     * having to hold six houses in mind at once.
     *
     * ```
     * 2 4 . | 3 1 6
     * 1 6 3 | 2 4 5
     * ------+------
     * 3 2 1 | 5 6 4
     * 4 5 6 | 1 3 2
     * ------+------
     * 6 3 2 | 4 5 1
     * 5 1 4 | 6 2 3
     * ```
     */
    private const val SIX_BY_SIX = "24.316163245321564456132632451514623"

    /**
     * A nine by nine that gives way to nothing but singles.
     *
     * Taken from the Gentle end of the shipped catalog rather than invented, so it is a real
     * puzzle with one solution rather than a grid that happens to look like one.
     */
    private const val NINE_BY_NINE =
        "53..7...." +
            "6..195..." +
            ".98....6." +
            "8...6...3" +
            "4..8.3..1" +
            "7...2...6" +
            ".6....28." +
            "...419..5" +
            "....8..79"

    public val lessons: List<Lesson> = listOf(
        Lesson(
            id = LessonId.WHAT_A_SUDOKU_IS,
            stage = Stage.FIRST_STEPS,
            title = R.string.lesson_what_is_title,
            summary = R.string.lesson_what_is_summary,
            teaches = null,
            dims = Dimensions.JUNIOR,
            board = FOUR_BY_FOUR,
            steps = listOf(
                Step.Say(R.string.lesson_what_is_1),
                Step.Show(R.string.lesson_what_is_2, focus = row(0, 4)),
                Step.Show(R.string.lesson_what_is_3, focus = column(0, 4)),
                Step.Say(R.string.lesson_what_is_4),
                Step.Show(R.string.lesson_what_is_5, focus = setOf(1, 2)),
            ),
        ),
        Lesson(
            id = LessonId.THE_THREE_HOUSES,
            stage = Stage.FIRST_STEPS,
            title = R.string.lesson_houses_title,
            summary = R.string.lesson_houses_summary,
            teaches = null,
            dims = Dimensions.JUNIOR,
            board = FOUR_BY_FOUR,
            steps = listOf(
                Step.Say(R.string.lesson_houses_1),
                Step.Show(R.string.lesson_houses_2, focus = row(1, 4)),
                Step.Show(R.string.lesson_houses_3, focus = column(1, 4)),
                Step.Show(R.string.lesson_houses_4, focus = setOf(0, 1, 4, 5)),
                Step.Show(R.string.lesson_houses_5, focus = setOf(5), strike = seenBy(5)),
            ),
        ),
        Lesson(
            id = LessonId.GIVENS,
            stage = Stage.FIRST_STEPS,
            title = R.string.lesson_givens_title,
            summary = R.string.lesson_givens_summary,
            teaches = null,
            dims = Dimensions.JUNIOR,
            board = FOUR_BY_FOUR,
            steps = listOf(
                Step.Show(R.string.lesson_givens_1, focus = (0..15).toSet() - setOf(1, 2)),
                Step.Say(R.string.lesson_givens_2),
                Step.Show(R.string.lesson_givens_3, focus = setOf(1, 2)),
                Step.Say(R.string.lesson_givens_4),
            ),
        ),
        Lesson(
            id = LessonId.FIRST_SOLVE_FOUR,
            stage = Stage.FIRST_STEPS,
            title = R.string.lesson_first_four_title,
            summary = R.string.lesson_first_four_summary,
            teaches = null,
            dims = Dimensions.JUNIOR,
            board = FOUR_BY_FOUR,
            steps = listOf(
                Step.Say(R.string.lesson_first_four_1),
                Step.Show(R.string.lesson_first_four_2, focus = row(0, 4)),
                Step.Place(R.string.lesson_first_four_3, cell = 1, digit = 3),
                Step.Show(R.string.lesson_first_four_4, focus = setOf(2), strike = setOf(0, 1, 3)),
                Step.YourTurn(
                    text = R.string.lesson_first_four_5,
                    cell = 2,
                    digit = 2,
                    wrong = R.string.lesson_first_four_wrong,
                ),
                Step.Say(R.string.lesson_first_four_6),
            ),
        ),
        Lesson(
            id = LessonId.FIRST_SOLVE_SIX,
            stage = Stage.FIRST_STEPS,
            title = R.string.lesson_first_six_title,
            summary = R.string.lesson_first_six_summary,
            teaches = null,
            dims = Dimensions.SIX,
            board = SIX_BY_SIX,
            steps = listOf(
                Step.Say(R.string.lesson_first_six_1),
                Step.Show(R.string.lesson_first_six_2, focus = setOf(0, 1, 2, 6, 7, 8)),
                Step.Say(R.string.lesson_first_six_3),
                Step.YourTurn(
                    text = R.string.lesson_first_six_4,
                    cell = 2,
                    digit = 5,
                    wrong = R.string.lesson_first_six_wrong,
                ),
                Step.Say(R.string.lesson_first_six_5),
            ),
        ),
        Lesson(
            id = LessonId.NAKED_SINGLE,
            stage = Stage.ONE_AT_A_TIME,
            title = R.string.lesson_naked_single_title,
            summary = R.string.lesson_naked_single_summary,
            teaches = TechniqueId.NAKED_SINGLE,
            dims = Dimensions.CLASSIC,
            board = NINE_BY_NINE,
            steps = listOf(
                Step.Say(R.string.lesson_naked_single_1),
                Step.Show(R.string.lesson_naked_single_2, focus = setOf(NAKED_SINGLE_CELL)),
                Step.Show(
                    R.string.lesson_naked_single_3,
                    focus = setOf(NAKED_SINGLE_CELL),
                    strike = seenBy9(NAKED_SINGLE_CELL),
                ),
                Step.Say(R.string.lesson_naked_single_4),
                Step.Say(R.string.lesson_naked_single_5),
            ),
        ),
    )

    /**
     * The cell the naked single lesson is about, which is the first one the engine finds in
     * that grid. Named rather than written three times, because the three uses have to agree.
     */
    private const val NAKED_SINGLE_CELL = 40

    /** The lessons of one stage, in order. */
    public fun of(stage: Stage): List<Lesson> = lessons.filter { it.stage == stage }

    public fun byId(id: LessonId): Lesson = lessons.first { it.id == id }

    /**
     * What the engine says the cost of each technique is, for the ordering test.
     *
     * Read from the ladder rather than copied, so adding a technique to the engine and
     * forgetting to place its lesson is a test failure rather than a course that teaches
     * XY-Wings before pairs.
     */
    public val ladderOrder: List<TechniqueId> = Techniques.ladder.map { it.id }

    private fun row(index: Int, size: Int): Set<Int> = (0 until size).map { index * size + it }.toSet()

    private fun column(index: Int, size: Int): Set<Int> = (0 until size).map { it * size + index }.toSet()

    /** Every cell a four by four cell shares a house with, itself excluded. */
    private fun seenBy(cell: Int): Set<Int> = seen(cell, Dimensions.JUNIOR)

    private fun seenBy9(cell: Int): Set<Int> = seen(cell, Dimensions.CLASSIC)

    private fun seen(cell: Int, dims: Dimensions): Set<Int> {
        val size = dims.size
        val row = cell / size
        val col = cell % size
        val box = dims.boxOf(row, col)
        return (0 until dims.cellCount)
            .filter { it != cell }
            .filter {
                it / size == row || it % size == col || dims.boxOf(it / size, it % size) == box
            }
            .toSet()
    }
}
