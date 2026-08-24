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

    /**
     * One Diabolical, walked forward.
     *
     * Almost every technique lesson stands on this grid or on a later position of it. That is
     * deliberate: a player who meets the twelfth technique on the twelfth unfamiliar board has
     * to read the board again each time, and the reading is not what is being taught. Coming
     * back to a grid they have seen since the pointing pair means the only new thing on screen
     * is the new idea.
     *
     * Every one of these positions was found by asking the engine where each technique first
     * applies, rather than by drawing a grid and hoping. The cells each lesson lights up are
     * the cells the engine names.
     */
    private const val WALK = "3...46...1.83.7..47..12....9....4.13.1.....8.82.6....9....38..25..7.24.8...95...7"
    private const val WALK_TRIPLE = "3...46...1.8397..47..12....9....4.13.1.....8.82.6....9....38..25..7.24.8...95...7"
    private const val WALK_HIDDEN_PAIR =
        "3...46...1.8397..47..125...9....4.13.1.." +
            "...8.82.6....9....38..25..7.24.8...95..." +
            "7"
    private const val WALK_RECTANGLE =
        "3..846...1.8397..47..125...9....4.13.1.." +
            "...8.82.6....9....38..25..7.24.8...95..." +
            "7"
    private const val WALK_W_WING = "3..846...1.8397..47..125..69....4.13.1.....8.82.6....9....38..25..7.24.8...95...7"
    private const val WALK_XY_WING = "3..846..11.8397..47..125..69..584.1341327.68582.6....9....38..25..7.24.8...95...7"
    private const val WALK_XYZ_WING =
        "3..846..11.8397..47..125..69..584.134132" +
            "7968582.6137.9....38..25..7.24.8...95..." +
            "7"

    /** A different grid, nearly finished, where a chain of one pair runs across it. */
    private const val REMOTE = "51863742997412.5633265941878...62...6.1...2.82..48...6185946372469...8..732815694"

    /** A column crossing a box, with four digits crowded into two of the shared cells. */
    private const val CROSSING = "5.9.6.732271..5...6.32.7.5.8.....51.9.5...4...645....93984.2..5...659348456.8...."

    /** Three groups in a chain down the right hand side, with a 1 that cannot survive. */
    private const val THREE_GROUPS = "1....4..54....596.956.27.4.8....645.5..483....495.2..8.94.58127785241...2..7.9584"

    /** A stem with two petals in the top band, and an 8 that has to land in one of them. */
    private const val BLOSSOM = "92..41.7....9..1...5.3...6...423865.....1.....82..931..7..93.8.8.9..5....3.87..91"

    /** One cell from being ambiguous, which is the whole of the BUG argument. */
    private const val ALMOST_BUG = "9..1.73..7.2396.853..4.2..9135729...8675412932946385174.1963.5.5892746316.38159.4"

    public val lessons: List<Lesson> = listOf(
        Lesson(
            id = LessonId.WHAT_A_SUDOKU_IS,
            stage = Stage.FIRST_STEPS,
            title = R.string.lesson_what_is_title,
            summary = R.string.lesson_what_is_summary,
            teaches = emptyList(),
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
            teaches = emptyList(),
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
            teaches = emptyList(),
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
            teaches = emptyList(),
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
            teaches = emptyList(),
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
            teaches = listOf(TechniqueId.NAKED_SINGLE),
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
        Lesson(
            id = LessonId.HIDDEN_SINGLE,
            stage = Stage.ONE_AT_A_TIME,
            title = R.string.lesson_hidden_single_title,
            summary = R.string.lesson_hidden_single_summary,
            teaches = listOf(TechniqueId.HIDDEN_SINGLE),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_hidden_single_1),
                Step.Show(R.string.lesson_hidden_single_2, focus = setOf(24)),
                Step.Show(R.string.lesson_hidden_single_3, focus = setOf(24), strike = setOf(6, 15, 25, 26)),
                Step.Say(R.string.lesson_hidden_single_4),
                Step.YourTurn(
                    text = R.string.lesson_hidden_single_turn,
                    cell = 24,
                    digit = 8,
                    wrong = R.string.lesson_hidden_single_wrong,
                ),
                Step.Say(R.string.lesson_hidden_single_5),
            ),
        ),
        Lesson(
            id = LessonId.SCANNING,
            stage = Stage.ONE_AT_A_TIME,
            title = R.string.lesson_scanning_title,
            summary = R.string.lesson_scanning_summary,
            teaches = emptyList(),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_scanning_1),
                Step.Show(R.string.lesson_scanning_2, focus = setOf(0, 1, 2, 9, 10, 11, 18, 19, 20)),
                Step.Say(R.string.lesson_scanning_3),
                Step.Show(R.string.lesson_scanning_4, focus = setOf(24)),
                Step.Say(R.string.lesson_scanning_5),
            ),
        ),
        Lesson(
            id = LessonId.FIRST_SOLVE_NINE,
            stage = Stage.ONE_AT_A_TIME,
            title = R.string.lesson_first_nine_title,
            summary = R.string.lesson_first_nine_summary,
            teaches = emptyList(),
            dims = Dimensions.CLASSIC,
            board = NINE_BY_NINE,
            steps = listOf(
                Step.Say(R.string.lesson_first_nine_1),
                Step.Show(R.string.lesson_first_nine_2, focus = setOf(40)),
                Step.YourTurn(
                    text = R.string.lesson_first_nine_turn,
                    cell = 40,
                    digit = 5,
                    wrong = R.string.lesson_first_nine_wrong,
                ),
                Step.Say(R.string.lesson_first_nine_3),
                Step.Say(R.string.lesson_first_nine_4),
            ),
        ),
        Lesson(
            id = LessonId.PENCIL_MARKS,
            stage = Stage.NOTES,
            title = R.string.lesson_pencil_marks_title,
            summary = R.string.lesson_pencil_marks_summary,
            teaches = emptyList(),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_pencil_marks_1),
                Step.Show(R.string.lesson_pencil_marks_2, focus = setOf(6)),
                Step.Say(R.string.lesson_pencil_marks_3),
                Step.Say(R.string.lesson_pencil_marks_4),
                Step.Say(R.string.lesson_pencil_marks_5),
            ),
        ),
        Lesson(
            id = LessonId.WHEN_TO_MARK,
            stage = Stage.NOTES,
            title = R.string.lesson_when_to_mark_title,
            summary = R.string.lesson_when_to_mark_summary,
            teaches = emptyList(),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_when_to_mark_1),
                Step.Say(R.string.lesson_when_to_mark_2),
                Step.Show(R.string.lesson_when_to_mark_3, focus = setOf(19, 20, 23, 26)),
                Step.Say(R.string.lesson_when_to_mark_4),
            ),
        ),
        Lesson(
            id = LessonId.KEEPING_MARKS_TRUE,
            stage = Stage.NOTES,
            title = R.string.lesson_marks_true_title,
            summary = R.string.lesson_marks_true_summary,
            teaches = emptyList(),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_marks_true_1),
                Step.Show(R.string.lesson_marks_true_2, focus = setOf(24), strike = setOf(6, 15, 25, 26)),
                Step.Say(R.string.lesson_marks_true_3),
                Step.Say(R.string.lesson_marks_true_4),
            ),
        ),
        Lesson(
            id = LessonId.POINTING_PAIR,
            stage = Stage.LOCKED_TO_A_LINE,
            title = R.string.lesson_pointing_title,
            summary = R.string.lesson_pointing_summary,
            teaches = listOf(TechniqueId.LOCKED_CANDIDATES_POINTING),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_pointing_1),
                Step.Show(R.string.lesson_pointing_2, focus = setOf(38, 47)),
                Step.Say(R.string.lesson_pointing_3),
                Step.Show(R.string.lesson_pointing_4, focus = setOf(38, 47), strike = setOf(65, 74)),
                Step.Say(R.string.lesson_pointing_5),
            ),
        ),
        Lesson(
            id = LessonId.CLAIMING_PAIR,
            stage = Stage.LOCKED_TO_A_LINE,
            title = R.string.lesson_claiming_title,
            summary = R.string.lesson_claiming_summary,
            teaches = listOf(TechniqueId.LOCKED_CANDIDATES_CLAIMING),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_claiming_1),
                Step.Show(R.string.lesson_claiming_2, focus = setOf(15, 16)),
                Step.Say(R.string.lesson_claiming_3),
                Step.Show(R.string.lesson_claiming_4, focus = setOf(15, 16), strike = setOf(6, 7)),
                Step.Say(R.string.lesson_claiming_5),
            ),
        ),
        Lesson(
            id = LessonId.NAKED_PAIR,
            stage = Stage.GROUPS,
            title = R.string.lesson_naked_pair_title,
            summary = R.string.lesson_naked_pair_summary,
            teaches = listOf(TechniqueId.NAKED_PAIR),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_naked_pair_1),
                Step.Show(R.string.lesson_naked_pair_2, focus = setOf(36, 54)),
                Step.Say(R.string.lesson_naked_pair_3),
                Step.Show(R.string.lesson_naked_pair_4, focus = setOf(36, 54), strike = setOf(72)),
                Step.Say(R.string.lesson_naked_pair_5),
            ),
        ),
        Lesson(
            id = LessonId.HIDDEN_PAIR,
            stage = Stage.GROUPS,
            title = R.string.lesson_hidden_pair_title,
            summary = R.string.lesson_hidden_pair_summary,
            teaches = listOf(TechniqueId.HIDDEN_PAIR),
            dims = Dimensions.CLASSIC,
            board = WALK_HIDDEN_PAIR,
            steps = listOf(
                Step.Say(R.string.lesson_hidden_pair_1),
                Step.Show(R.string.lesson_hidden_pair_2, focus = setOf(30, 39)),
                Step.Say(R.string.lesson_hidden_pair_3),
                Step.Say(R.string.lesson_hidden_pair_4),
                Step.Say(R.string.lesson_hidden_pair_5),
            ),
        ),
        Lesson(
            id = LessonId.NAKED_TRIPLE,
            stage = Stage.GROUPS,
            title = R.string.lesson_naked_triple_title,
            summary = R.string.lesson_naked_triple_summary,
            teaches = listOf(TechniqueId.NAKED_TRIPLE),
            dims = Dimensions.CLASSIC,
            board = WALK_TRIPLE,
            steps = listOf(
                Step.Say(R.string.lesson_naked_triple_1),
                Step.Show(R.string.lesson_naked_triple_2, focus = setOf(15, 16, 26)),
                Step.Say(R.string.lesson_naked_triple_3),
                Step.Show(R.string.lesson_naked_triple_4, focus = setOf(15, 16, 26), strike = setOf(6, 7, 8, 24, 25)),
                Step.Say(R.string.lesson_naked_triple_5),
            ),
        ),
        Lesson(
            id = LessonId.HIDDEN_TRIPLE,
            stage = Stage.GROUPS,
            title = R.string.lesson_hidden_triple_title,
            summary = R.string.lesson_hidden_triple_summary,
            teaches = listOf(TechniqueId.HIDDEN_TRIPLE),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_hidden_triple_1),
                Step.Show(R.string.lesson_hidden_triple_2, focus = setOf(10, 15, 16)),
                Step.Say(R.string.lesson_hidden_triple_3),
                Step.Say(R.string.lesson_hidden_triple_4),
            ),
        ),
        Lesson(
            id = LessonId.QUADS,
            stage = Stage.GROUPS,
            title = R.string.lesson_quads_title,
            summary = R.string.lesson_quads_summary,
            teaches = listOf(TechniqueId.NAKED_QUAD, TechniqueId.HIDDEN_QUAD),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_quads_1),
                Step.Show(R.string.lesson_quads_2, focus = setOf(19, 20, 23, 26), strike = setOf(24, 25)),
                Step.Say(R.string.lesson_quads_3),
                Step.Show(R.string.lesson_quads_4, focus = setOf(55, 56, 60, 61)),
                Step.Say(R.string.lesson_quads_5),
            ),
        ),
        Lesson(
            id = LessonId.X_WING,
            stage = Stage.PATTERNS,
            title = R.string.lesson_x_wing_title,
            summary = R.string.lesson_x_wing_summary,
            teaches = listOf(TechniqueId.X_WING),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_x_wing_1),
                Step.Show(R.string.lesson_x_wing_2, focus = setOf(30, 33, 39, 42)),
                Step.Say(R.string.lesson_x_wing_3),
                Step.Show(R.string.lesson_x_wing_4, focus = setOf(30, 33, 39, 42), strike = setOf(6, 15)),
                Step.Say(R.string.lesson_x_wing_5),
            ),
        ),
        Lesson(
            id = LessonId.SWORDFISH,
            stage = Stage.PATTERNS,
            title = R.string.lesson_swordfish_title,
            summary = R.string.lesson_swordfish_summary,
            teaches = listOf(TechniqueId.SWORDFISH),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_swordfish_1),
                Step.Show(R.string.lesson_swordfish_2, focus = setOf(15, 16, 30, 33, 39, 42)),
                Step.Say(R.string.lesson_swordfish_3),
                Step.Show(R.string.lesson_swordfish_4, focus = setOf(15, 16, 30, 33, 39, 42), strike = setOf(6, 7)),
                Step.Say(R.string.lesson_swordfish_5),
            ),
        ),
        Lesson(
            id = LessonId.JELLYFISH,
            stage = Stage.PATTERNS,
            title = R.string.lesson_jellyfish_title,
            summary = R.string.lesson_jellyfish_summary,
            teaches = listOf(TechniqueId.JELLYFISH),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_jellyfish_1),
                Step.Show(R.string.lesson_jellyfish_2, focus = setOf(49, 50, 56, 60, 65, 67, 74, 77, 78)),
                Step.Say(R.string.lesson_jellyfish_3),
                Step.Say(R.string.lesson_jellyfish_4),
            ),
        ),
        Lesson(
            id = LessonId.XY_WING,
            stage = Stage.WINGS,
            title = R.string.lesson_xy_wing_title,
            summary = R.string.lesson_xy_wing_summary,
            teaches = listOf(TechniqueId.XY_WING),
            dims = Dimensions.CLASSIC,
            board = WALK_XY_WING,
            steps = listOf(
                Step.Say(R.string.lesson_xy_wing_1),
                Step.Show(R.string.lesson_xy_wing_2, focus = setOf(28)),
                Step.Show(R.string.lesson_xy_wing_3, focus = setOf(10, 28, 47)),
                Step.Say(R.string.lesson_xy_wing_4),
                Step.Show(R.string.lesson_xy_wing_5, focus = setOf(10, 28, 47), strike = setOf(2)),
                Step.Say(R.string.lesson_xy_wing_6),
            ),
        ),
        Lesson(
            id = LessonId.XYZ_WING,
            stage = Stage.WINGS,
            title = R.string.lesson_xyz_wing_title,
            summary = R.string.lesson_xyz_wing_summary,
            teaches = listOf(TechniqueId.XYZ_WING),
            dims = Dimensions.CLASSIC,
            board = WALK_XYZ_WING,
            steps = listOf(
                Step.Say(R.string.lesson_xyz_wing_1),
                Step.Show(R.string.lesson_xyz_wing_2, focus = setOf(1, 6, 15)),
                Step.Say(R.string.lesson_xyz_wing_3),
                Step.Show(R.string.lesson_xyz_wing_4, focus = setOf(1, 6, 15), strike = setOf(7)),
                Step.Say(R.string.lesson_xyz_wing_5),
            ),
        ),
        Lesson(
            id = LessonId.W_WING,
            stage = Stage.WINGS,
            title = R.string.lesson_w_wing_title,
            summary = R.string.lesson_w_wing_summary,
            teaches = listOf(TechniqueId.W_WING),
            dims = Dimensions.CLASSIC,
            board = WALK_W_WING,
            steps = listOf(
                Step.Say(R.string.lesson_w_wing_1),
                Step.Show(R.string.lesson_w_wing_2, focus = setOf(15, 30)),
                Step.Say(R.string.lesson_w_wing_3),
                Step.Show(R.string.lesson_w_wing_4, focus = setOf(15, 30), strike = setOf(33)),
                Step.Say(R.string.lesson_w_wing_5),
            ),
        ),
        Lesson(
            id = LessonId.UNIQUE_RECTANGLE,
            stage = Stage.ONE_ANSWER,
            title = R.string.lesson_rectangle_title,
            summary = R.string.lesson_rectangle_summary,
            teaches = listOf(TechniqueId.UNIQUE_RECTANGLE),
            dims = Dimensions.CLASSIC,
            board = WALK_RECTANGLE,
            steps = listOf(
                Step.Say(R.string.lesson_rectangle_1),
                Step.Show(R.string.lesson_rectangle_2, focus = setOf(30, 33, 39, 42)),
                Step.Say(R.string.lesson_rectangle_3),
                Step.Say(R.string.lesson_rectangle_4),
                Step.Show(R.string.lesson_rectangle_5, focus = setOf(30, 33, 39, 42), strike = setOf(33, 42)),
                Step.Say(R.string.lesson_rectangle_6),
            ),
        ),
        Lesson(
            id = LessonId.BUG_PLUS_ONE,
            stage = Stage.ONE_ANSWER,
            title = R.string.lesson_bug_title,
            summary = R.string.lesson_bug_summary,
            teaches = listOf(TechniqueId.BUG_PLUS_ONE),
            dims = Dimensions.CLASSIC,
            board = ALMOST_BUG,
            steps = listOf(
                Step.Say(R.string.lesson_bug_1),
                Step.Show(R.string.lesson_bug_2, focus = setOf(7)),
                Step.Say(R.string.lesson_bug_3),
                Step.Say(R.string.lesson_bug_4),
            ),
        ),
        Lesson(
            id = LessonId.SIMPLE_COLOURING,
            stage = Stage.COLOURING,
            title = R.string.lesson_colouring_title,
            summary = R.string.lesson_colouring_summary,
            teaches = listOf(TechniqueId.SIMPLE_COLOURING),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_colouring_1),
                Step.Show(R.string.lesson_colouring_2, focus = setOf(56, 60, 78)),
                Step.Say(R.string.lesson_colouring_3),
                Step.Show(R.string.lesson_colouring_4, focus = setOf(56, 60, 78), strike = setOf(6)),
                Step.Say(R.string.lesson_colouring_5),
            ),
        ),
        Lesson(
            id = LessonId.REMOTE_PAIRS,
            stage = Stage.COLOURING,
            title = R.string.lesson_remote_pairs_title,
            summary = R.string.lesson_remote_pairs_summary,
            teaches = listOf(TechniqueId.REMOTE_PAIRS),
            dims = Dimensions.CLASSIC,
            board = REMOTE,
            steps = listOf(
                Step.Say(R.string.lesson_remote_pairs_1),
                Step.Show(R.string.lesson_remote_pairs_2, focus = setOf(29, 30, 39, 47)),
                Step.Say(R.string.lesson_remote_pairs_3),
                Step.Show(R.string.lesson_remote_pairs_4, focus = setOf(29, 30, 39, 47), strike = setOf(50)),
                Step.Say(R.string.lesson_remote_pairs_5),
            ),
        ),
        Lesson(
            id = LessonId.MULTI_COLOURING,
            stage = Stage.COLOURING,
            title = R.string.lesson_multi_colouring_title,
            summary = R.string.lesson_multi_colouring_summary,
            teaches = listOf(TechniqueId.MULTI_COLOURING),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_multi_colouring_1),
                Step.Show(R.string.lesson_multi_colouring_2, focus = setOf(6, 8, 56, 60, 78)),
                Step.Say(R.string.lesson_multi_colouring_3),
                Step.Show(R.string.lesson_multi_colouring_4, focus = setOf(6, 8, 56, 60, 78), strike = setOf(6)),
                Step.Say(R.string.lesson_multi_colouring_5),
            ),
        ),
        Lesson(
            id = LessonId.X_CHAIN,
            stage = Stage.CHAINS,
            title = R.string.lesson_x_chain_title,
            summary = R.string.lesson_x_chain_summary,
            teaches = listOf(TechniqueId.X_CHAIN),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_x_chain_1),
                Step.Show(R.string.lesson_x_chain_2, focus = setOf(15, 16, 30, 33, 39, 42)),
                Step.Say(R.string.lesson_x_chain_3),
                Step.Show(R.string.lesson_x_chain_4, focus = setOf(15, 16, 30, 33, 39, 42), strike = setOf(6)),
                Step.Say(R.string.lesson_x_chain_5),
            ),
        ),
        Lesson(
            id = LessonId.XY_CHAIN,
            stage = Stage.CHAINS,
            title = R.string.lesson_xy_chain_title,
            summary = R.string.lesson_xy_chain_summary,
            teaches = listOf(TechniqueId.XY_CHAIN),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_xy_chain_1),
                Step.Show(R.string.lesson_xy_chain_2, focus = setOf(36, 44, 49, 51, 54, 67)),
                Step.Say(R.string.lesson_xy_chain_3),
                Step.Show(R.string.lesson_xy_chain_4, focus = setOf(36, 44, 49, 51, 54, 67), strike = setOf(64, 65)),
                Step.Say(R.string.lesson_xy_chain_5),
            ),
        ),
        Lesson(
            id = LessonId.WRITING_A_CHAIN_DOWN,
            stage = Stage.CHAINS,
            title = R.string.lesson_writing_chains_title,
            summary = R.string.lesson_writing_chains_summary,
            teaches = emptyList(),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_writing_chains_1),
                Step.Show(R.string.lesson_writing_chains_2, focus = setOf(36, 44, 49, 51, 54, 67)),
                Step.Say(R.string.lesson_writing_chains_3),
                Step.Say(R.string.lesson_writing_chains_4),
                Step.Say(R.string.lesson_writing_chains_5),
            ),
        ),
        Lesson(
            id = LessonId.SUE_DE_COQ,
            stage = Stage.THE_FAR_END,
            title = R.string.lesson_sue_de_coq_title,
            summary = R.string.lesson_sue_de_coq_summary,
            teaches = listOf(TechniqueId.SUE_DE_COQ),
            dims = Dimensions.CLASSIC,
            board = CROSSING,
            steps = listOf(
                Step.Say(R.string.lesson_sue_de_coq_1),
                Step.Show(R.string.lesson_sue_de_coq_2, focus = setOf(43, 52)),
                Step.Show(R.string.lesson_sue_de_coq_3, focus = setOf(43, 51, 52, 61)),
                Step.Say(R.string.lesson_sue_de_coq_4),
                Step.Show(R.string.lesson_sue_de_coq_5, focus = setOf(43, 51, 52, 61), strike = setOf(16, 79)),
                Step.Say(R.string.lesson_sue_de_coq_6),
            ),
        ),
        Lesson(
            id = LessonId.ALS_XZ,
            stage = Stage.THE_FAR_END,
            title = R.string.lesson_als_title,
            summary = R.string.lesson_als_summary,
            teaches = listOf(TechniqueId.ALS_XZ),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_als_1),
                Step.Show(R.string.lesson_als_2, focus = setOf(1, 10, 19, 28)),
                Step.Say(R.string.lesson_als_3),
                Step.Show(R.string.lesson_als_4, focus = setOf(1, 10, 19, 28, 55), strike = setOf(64)),
                Step.Say(R.string.lesson_als_5),
                Step.Say(R.string.lesson_als_6),
            ),
        ),
        Lesson(
            id = LessonId.WHEN_NOTHING_APPLIES,
            stage = Stage.THE_FAR_END,
            title = R.string.lesson_nothing_applies_title,
            summary = R.string.lesson_nothing_applies_summary,
            teaches = emptyList(),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_nothing_applies_1),
                Step.Say(R.string.lesson_nothing_applies_2),
                Step.Say(R.string.lesson_nothing_applies_3),
                Step.Say(R.string.lesson_nothing_applies_4),
            ),
        ),
        Lesson(
            id = LessonId.WHAT_TO_LOOK_FOR_FIRST,
            stage = Stage.THE_FAR_END,
            title = R.string.lesson_look_first_title,
            summary = R.string.lesson_look_first_summary,
            teaches = emptyList(),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_look_first_1),
                Step.Say(R.string.lesson_look_first_2),
                Step.Say(R.string.lesson_look_first_3),
                Step.Say(R.string.lesson_look_first_4),
                Step.Say(R.string.lesson_look_first_5),
            ),
        ),
        Lesson(
            id = LessonId.A_BEYOND_TOGETHER,
            stage = Stage.THE_FAR_END,
            title = R.string.lesson_together_title,
            summary = R.string.lesson_together_summary,
            teaches = emptyList(),
            dims = Dimensions.CLASSIC,
            board = WALK,
            steps = listOf(
                Step.Say(R.string.lesson_together_1),
                Step.Show(R.string.lesson_together_2, focus = setOf(13)),
                Step.Show(R.string.lesson_together_3, focus = setOf(24)),
                Step.Show(R.string.lesson_together_4, focus = setOf(38, 47), strike = setOf(65, 74)),
                Step.Show(R.string.lesson_together_5, focus = setOf(36, 54), strike = setOf(72)),
                Step.Show(R.string.lesson_together_6, focus = setOf(30, 33, 39, 42), strike = setOf(6, 15)),
                Step.Show(R.string.lesson_together_7, focus = setOf(1, 10, 19, 28, 55), strike = setOf(64)),
                Step.Say(R.string.lesson_together_8),
                Step.Say(R.string.lesson_together_9),
            ),
        ),
        Lesson(
            id = LessonId.WHAT_THE_DEEP_END_ASKS,
            stage = Stage.THE_DEEP_END,
            title = R.string.lesson_deep_end_title,
            summary = R.string.lesson_deep_end_summary,
            teaches = emptyList(),
            dims = Dimensions.CLASSIC,
            board = THREE_GROUPS,
            steps = listOf(
                Step.Say(R.string.lesson_deep_end_1),
                Step.Say(R.string.lesson_deep_end_2),
                Step.Say(R.string.lesson_deep_end_3),
                Step.Say(R.string.lesson_deep_end_4),
                Step.Say(R.string.lesson_deep_end_5),
            ),
        ),
        Lesson(
            id = LessonId.ALS_XY_WING,
            stage = Stage.THE_DEEP_END,
            title = R.string.lesson_als_xy_title,
            summary = R.string.lesson_als_xy_summary,
            teaches = listOf(TechniqueId.ALS_XY_WING),
            dims = Dimensions.CLASSIC,
            board = THREE_GROUPS,
            steps = listOf(
                Step.Say(R.string.lesson_als_xy_1),
                Step.Show(R.string.lesson_als_xy_2, focus = setOf(7, 26)),
                Step.Show(R.string.lesson_als_xy_3, focus = setOf(7, 26, 43, 52, 70)),
                Step.Show(R.string.lesson_als_xy_4, focus = setOf(7, 26, 43, 52, 70), strike = setOf(35, 44)),
                Step.Say(R.string.lesson_als_xy_5),
                Step.Say(R.string.lesson_als_xy_6),
            ),
        ),
        Lesson(
            id = LessonId.DEATH_BLOSSOM,
            stage = Stage.THE_DEEP_END,
            title = R.string.lesson_blossom_title,
            summary = R.string.lesson_blossom_summary,
            teaches = listOf(TechniqueId.DEATH_BLOSSOM),
            dims = Dimensions.CLASSIC,
            board = BLOSSOM,
            steps = listOf(
                Step.Say(R.string.lesson_blossom_1),
                Step.Show(R.string.lesson_blossom_2, focus = setOf(3)),
                Step.Show(R.string.lesson_blossom_3, focus = setOf(3, 6)),
                Step.Show(R.string.lesson_blossom_4, focus = setOf(3, 14, 22, 23)),
                Step.Say(R.string.lesson_blossom_5),
                Step.Show(R.string.lesson_blossom_6, focus = setOf(3, 6, 14, 22, 23), strike = setOf(24, 26)),
                Step.Say(R.string.lesson_blossom_7),
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
     * The lesson that teaches a technique, if the course has one.
     *
     * This is what turns "what is this?" in a hint from a page of definitions into the lesson
     * itself. Every technique the engine knows has one, and a test says so, but this stays
     * nullable because a technique added to the engine tomorrow will not.
     */
    public fun teaching(technique: TechniqueId): Lesson? = lessons.firstOrNull { technique in it.teaches }

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
