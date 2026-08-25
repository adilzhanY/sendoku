package com.sendoku.app.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.game.Cell
import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.ui.NumberPad
import com.sendoku.app.ui.SudokuBoard
import com.sendoku.engine.Board

/**
 * A lesson, played.
 *
 * One board, one passage of text, one action. Never all three competing for the eye, which is
 * why the text sits under the board rather than beside it and why there is a single accent
 * coloured button rather than a row of choices.
 *
 * The board is the same SudokuBoard the game draws. A lesson highlights cells through the
 * hint sets it already has, so a lesson looks exactly like a hint looks, and there is no
 * second board implementation to keep in step with the first.
 */
@Composable
public fun LessonPlayer(
    lesson: Lesson,
    onFinished: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
    startAt: Int = 0,
    onStep: (Int) -> Unit = {},
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    // rememberSaveable, so turning the phone over mid lesson does not start it again.
    var index by rememberSaveable(lesson.id) { mutableIntStateOf(startAt.coerceIn(0, lesson.steps.lastIndex)) }
    var answered by rememberSaveable(lesson.id) { mutableStateOf(false) }
    var wrongTold by rememberSaveable(lesson.id) { mutableStateOf(false) }
    var revealed by rememberSaveable(lesson.id) { mutableStateOf(false) }

    val step = lesson.steps[index]
    val state = remember(lesson.id, index, answered) { stateAt(lesson, index, answered) }
    val turn = step as? Step.YourTurn
    val waiting = turn != null && !answered

    val focus = remember { FocusRequester() }
    LaunchedEffect(lesson.id) { runCatching { focus.requestFocus() } }

    fun go(to: Int) {
        index = to.coerceIn(0, lesson.steps.lastIndex)
        answered = false
        wrongTold = false
        revealed = false
        onStep(index)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .focusRequester(focus)
            .focusable()
            // The game screen has been playable from a keyboard since the start and a lesson
            // was not, which made the course the one part of the app a keyboard could not
            // reach. Same keys where they mean the same thing.
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    lessonKey(
                        key = event.key,
                        waiting = waiting,
                        expected = turn?.digit,
                        onDigit = { digit ->
                            if (digit == turn?.digit) {
                                answered = true
                                wrongTold = false
                            } else {
                                wrongTold = true
                            }
                        },
                        onNext = { if (index == lesson.steps.lastIndex) onFinished() else go(index + 1) },
                        onBack = { go(index - 1) },
                        onReplay = { go(0) },
                    )
                }
            },
    ) {
        // Beside the board when the screen is wider than it is tall, under it otherwise. A
        // lesson stacked vertically in landscape leaves the board the size of a stamp, which
        // is the one thing a lesson about a grid cannot afford.
        val sideBySide = maxWidth > maxHeight

        val board: @Composable (Modifier) -> Unit = { boardModifier ->
            Box(modifier = boardModifier, contentAlignment = Alignment.Center) {
                SudokuBoard(
                    state = state,
                    onSelect = {},
                    hintLogic = (step as? Step.Show)?.focus.orEmpty() + turnFocus(turn, answered),
                    hintStrike = (step as? Step.Show)?.strike.orEmpty(),
                    // The same treatment a hint gets, for the same reason: a lesson that
                    // highlights differently from a hint teaches the wrong thing twice.
                    spotlight = true,
                    modifier = Modifier.testTag("lesson:board"),
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(dimens.spaceM),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            Header(lesson, index, onLeave)

            if (sideBySide) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceL),
                ) {
                    board(Modifier.fillMaxHeight().weight(1f))
                    Box(Modifier.fillMaxHeight().weight(1f), contentAlignment = Alignment.Center) {
                        Talk(lesson, step, wrongTold, turn)
                    }
                }
            } else {
                board(Modifier.fillMaxWidth().weight(1f))
            }

            if (!sideBySide) Talk(lesson, step, wrongTold, turn)

            if (waiting) {
                NumberPad(
                    state = state,
                    onDigit = { digit ->
                        if (digit == turn.digit) {
                            answered = true
                            wrongTold = false
                        } else {
                            // Never a failure. The reason again, and the lesson waits.
                            wrongTold = true
                        }
                    },
                )
            }

            Controls(
                lesson = lesson,
                index = index,
                waiting = waiting,
                revealed = revealed,
                onBack = { go(index - 1) },
                onNext = { if (index == lesson.steps.lastIndex) onFinished() else go(index + 1) },
                onReveal = {
                    revealed = true
                    answered = true
                },
                onReplay = { go(0) },
            )
        }
    }
}

@Composable
private fun Header(lesson: Lesson, index: Int, onLeave: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
    ) {
        Text(
            text = stringResource(R.string.back),
            style = Sendoku.type.overline,
            color = colors.muted,
            modifier = Modifier
                .clip(RoundedCornerShape(dimens.radiusS))
                .clickable(onClick = onLeave)
                .padding(dimens.spaceS)
                .testTag("lesson:back"),
        )
        Text(
            text = stringResource(lesson.title),
            style = Sendoku.type.label,
            color = colors.given,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.lesson_step_of, index + 1, lesson.steps.size),
            style = Sendoku.type.statLabel,
            color = colors.muted,
        )
    }
    Dots(lesson.steps.size, index)
}

/**
 * What the step says.
 *
 * Announced to a screen reader the moment it changes, since a lesson is a sequence of
 * statements and the board moving under a blind player says nothing on its own.
 */
@Composable
private fun Talk(lesson: Lesson, step: Step, wrongTold: Boolean, turn: Step.YourTurn?) {
    Text(
        text = spoken(lesson, step, wrongTold, turn),
        style = Sendoku.type.body,
        color = Sendoku.colors.given,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TEXT_MIN)
            .verticalScroll(rememberScrollState())
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("lesson:text"),
    )
}

/** One dot per step. A progress bar for six steps is a bar that never looks like it moves. */
@Composable
private fun Dots(count: Int, index: Int) {
    val colors = Sendoku.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Sendoku.dimens.spaceXs),
        horizontalArrangement = Arrangement.spacedBy(Sendoku.dimens.spaceXs),
    ) {
        repeat(count) { at ->
            Box(
                Modifier
                    .size(DOT)
                    .clip(CircleShape)
                    .background(if (at <= index) colors.accent else colors.surfaceRaised),
            )
        }
    }
}

@Composable
private fun Controls(
    lesson: Lesson,
    index: Int,
    waiting: Boolean,
    revealed: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onReveal: () -> Unit,
    onReplay: () -> Unit,
) {
    val dimens = Sendoku.dimens
    val last = index == lesson.steps.lastIndex
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
    ) {
        LessonButton(
            label = stringResource(R.string.lesson_back),
            enabled = index > 0,
            accent = false,
            onClick = onBack,
            tag = "lesson:previous",
            modifier = Modifier.weight(1f),
        )
        if (last) {
            LessonButton(
                label = stringResource(R.string.lesson_replay),
                enabled = true,
                accent = false,
                onClick = onReplay,
                tag = "lesson:replay",
                modifier = Modifier.weight(1f),
            )
        }
        if (waiting) {
            LessonButton(
                label = stringResource(R.string.lesson_show_me),
                enabled = !revealed,
                accent = true,
                onClick = onReveal,
                tag = "lesson:reveal",
                modifier = Modifier.weight(1.6f),
            )
        } else {
            LessonButton(
                label = stringResource(if (last) R.string.lesson_done else R.string.lesson_next),
                enabled = true,
                accent = true,
                onClick = onNext,
                tag = "lesson:next",
                modifier = Modifier.weight(1.6f),
            )
        }
    }
}

@Composable
private fun LessonButton(
    label: String,
    enabled: Boolean,
    accent: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Box(
        modifier = modifier
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(if (accent) colors.accent else colors.surface)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.35f)
            .testTag(tag)
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = Sendoku.type.label,
            color = if (accent) colors.onAccent else colors.muted,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The board as it stands at a step.
 *
 * Rebuilt from the lesson rather than mutated, so stepping backwards is the same operation as
 * stepping forwards and there is no undo history to get wrong.
 */
internal fun stateAt(lesson: Lesson, index: Int, answered: Boolean): GameState {
    val board = Board.parse(lesson.dims, lesson.board)
    for (at in 0..index) {
        val step = lesson.steps[at]
        when {
            step is Step.Place -> board.setAtIndex(step.cell, step.digit)
            step is Step.YourTurn && (at < index || answered) -> board.setAtIndex(step.cell, step.digit)
            else -> Unit
        }
    }
    val givens = Board.parse(lesson.dims, lesson.board)
    return GameState(
        dims = lesson.dims,
        solution = board,
        grade = com.sendoku.engine.Grade.GENTLE,
        rating = 0.0,
        hardest = null,
        cells = (0 until lesson.dims.cellCount).map { cell ->
            val digit = board.atIndex(cell)
            Cell(digit = digit, isGiven = digit != Board.EMPTY && givens.atIndex(cell) != Board.EMPTY)
        },
        // A lesson has no clock and no mistake limit. Neither belongs in a place somebody is
        // being taught something.
        settings = GameSettings(showTimer = false, mistakeLimit = null),
        isRunning = false,
    )
}

private fun turnFocus(turn: Step.YourTurn?, answered: Boolean): Set<Int> =
    if (turn != null && !answered) setOf(turn.cell) else emptySet()

/**
 * What the step says, with the wrong answer note when there is one.
 *
 * A screen reader gets the cell named as well, because "this cell" is a pointing word and a
 * highlight is not a sentence. Working it out from the focus set means no lesson has to write
 * its own locator, and none of them can forget to.
 */
@Composable
private fun spoken(lesson: Lesson, step: Step, wrongTold: Boolean, turn: Step.YourTurn?): String {
    val text = when (step) {
        is Step.Say -> stringResource(step.text)
        is Step.Show -> stringResource(step.text)
        is Step.Place -> stringResource(step.text)
        is Step.YourTurn -> stringResource(step.text)
    }
    val cells = when (step) {
        is Step.Show -> step.focus
        is Step.Place -> setOf(step.cell)
        is Step.YourTurn -> setOf(step.cell)
        is Step.Say -> emptySet()
    }
    val size = lesson.dims.size
    val locator = stringResource(R.string.lesson_cell_at, 0, 0)
    val where = if (cells.isEmpty() || cells.size > SPOKEN_CELL_LIMIT) {
        ""
    } else {
        // Formatted here rather than through stringResource per cell, because a composable
        // cannot be called from inside a loop body that Compose cannot see into.
        " " + cells.sorted().joinToString(" ") { cell ->
            locator.replaceFirst("0", (cell / size + 1).toString())
                .replaceFirst("0", (cell % size + 1).toString())
        }
    }
    val wrong = if (wrongTold && turn != null) " " + stringResource(turn.wrong) else ""
    return text + where + wrong
}

/** Enough cells to read out. Past this it is a shape, and reading nine pairs of numbers helps nobody. */
private const val SPOKEN_CELL_LIMIT = 4

private val DOT = 6.dp
private val TEXT_MIN = 96.dp

/**
 * A key press in a lesson.
 *
 * Right and left step through, which is what an arrow key means everywhere else. A digit only
 * does something when the lesson is waiting for one, so pressing 5 while reading does not
 * silently answer a question three steps away.
 */
internal fun lessonKey(
    key: Key,
    waiting: Boolean,
    expected: Int?,
    onDigit: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onReplay: () -> Unit,
): Boolean {
    if (waiting && expected != null) {
        val digit = digitOf(key)
        if (digit != null) {
            onDigit(digit)
            return true
        }
    }
    return when (key) {
        Key.DirectionRight, Key.Enter, Key.Spacebar -> {
            if (!waiting) onNext()
            !waiting
        }

        Key.DirectionLeft -> {
            onBack()
            true
        }

        Key.R -> {
            onReplay()
            true
        }

        else -> false
    }
}

private fun digitOf(key: Key): Int? = when (key) {
    Key.One, Key.NumPad1 -> 1
    Key.Two, Key.NumPad2 -> 2
    Key.Three, Key.NumPad3 -> 3
    Key.Four, Key.NumPad4 -> 4
    Key.Five, Key.NumPad5 -> 5
    Key.Six, Key.NumPad6 -> 6
    Key.Seven, Key.NumPad7 -> 7
    Key.Eight, Key.NumPad8 -> 8
    Key.Nine, Key.NumPad9 -> 9
    else -> null
}
