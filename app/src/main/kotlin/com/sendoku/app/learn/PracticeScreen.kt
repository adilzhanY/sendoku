package com.sendoku.app.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.sendoku.app.R
import com.sendoku.app.game.Cell
import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.ui.BackButton
import com.sendoku.app.ui.SudokuBoard
import com.sendoku.app.ui.TechniqueCopy
import com.sendoku.engine.Board
import com.sendoku.engine.Grade

/**
 * Practice: a board, and find the pattern.
 *
 * Reading a lesson about an X-Wing and finding one on a grid are different skills, and only
 * the second is the one that makes a puzzle solvable. So practice asks for the cells rather
 * than for a digit: tap the cells the argument rests on, in any order, because the corners of
 * a rectangle have no first.
 *
 * A wrong tap says which part of the rule it breaks rather than counting a mistake. Nothing
 * here is scored, there is no timer, and the only number kept is the run of correct answers,
 * which is what mastery reads.
 */
@Composable
public fun PracticeScreen(
    exercise: Exercise?,
    onAnswer: (Boolean) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    var picked by remember(exercise) { mutableStateOf(emptySet<Int>()) }
    var wrong by remember(exercise) { mutableStateOf<Int?>(null) }
    var solved by remember(exercise) { mutableStateOf(false) }
    var revealed by remember(exercise) { mutableStateOf(false) }

    if (exercise == null) {
        Box(modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.practice_looking),
                style = Sendoku.type.body,
                color = colors.muted,
                modifier = Modifier.padding(dimens.spaceXl),
            )
        }
        return
    }

    val state = remember(exercise) { boardOf(exercise) }

    fun tap(cell: Int) {
        if (solved || revealed) return
        if (cell in exercise.cells) {
            wrong = null
            picked = picked + cell
            if (picked == exercise.cells) {
                solved = true
                onAnswer(true)
            }
        } else {
            wrong = cell
            onAnswer(false)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.background)) {
        val sideBySide = maxWidth > maxHeight

        val board: @Composable (Modifier) -> Unit = { boardModifier ->
            Box(modifier = boardModifier, contentAlignment = Alignment.Center) {
                SudokuBoard(
                    state = state,
                    onSelect = ::tap,
                    hintLogic = if (revealed) exercise.cells else picked,
                    hintStrike = if (solved || revealed) exercise.eliminations else emptySet(),
                    wrong = setOfNotNull(wrong),
                    // Only once the answer is known. Dimming the board while somebody is
                    // hunting for the pattern would answer the question they were asked.
                    spotlight = solved || revealed,
                    modifier = Modifier.testTag("practice:board"),
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(dimens.spaceM),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
            ) {
                BackButton(onClick = onBack)
                Text(
                    text = stringResource(TechniqueCopy.nameOf(exercise.technique)),
                    style = Sendoku.type.label,
                    color = colors.given,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${picked.size}/${exercise.cells.size}",
                    style = Sendoku.type.statLabel,
                    color = colors.muted,
                )
            }

            if (sideBySide) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceL),
                ) {
                    board(Modifier.fillMaxHeight().weight(1f))
                    Box(Modifier.fillMaxHeight().weight(1f), contentAlignment = Alignment.Center) {
                        Prompt(exercise, picked, wrong, solved, revealed)
                    }
                }
            } else {
                board(Modifier.fillMaxWidth().weight(1f))
                Prompt(exercise, picked, wrong, solved, revealed)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
            ) {
                PracticeButton(
                    label = stringResource(R.string.practice_show_me),
                    enabled = !solved && !revealed,
                    accent = false,
                    tag = "practice:reveal",
                    onClick = {
                        revealed = true
                        onAnswer(false)
                    },
                    modifier = Modifier.weight(1f),
                )
                PracticeButton(
                    label = stringResource(
                        if (solved ||
                            revealed
                        ) {
                            R.string.practice_another
                        } else {
                            R.string.practice_skip
                        },
                    ),
                    enabled = true,
                    accent = solved || revealed,
                    tag = "practice:next",
                    onClick = onNext,
                    modifier = Modifier.weight(1.4f),
                )
            }
        }
    }
}

@Composable
private fun Prompt(exercise: Exercise, picked: Set<Int>, wrong: Int?, solved: Boolean, revealed: Boolean) {
    val colors = Sendoku.colors
    val name = stringResource(TechniqueCopy.nameOf(exercise.technique))
    val text = when {
        solved -> stringResource(R.string.practice_right, name)

        revealed -> stringResource(R.string.practice_revealed, name)

        wrong != null -> stringResource(R.string.practice_wrong, name)

        picked.isNotEmpty() -> stringResource(R.string.practice_keep_going)

        else -> stringResource(R.string.practice_find, name) + " " + stringResource(
            TechniqueCopy.lookFor(exercise.technique),
        )
    }
    Text(
        text = text,
        style = Sendoku.type.body,
        color = if (wrong != null) colors.conflict else colors.given,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sendoku.dimens.spaceXl)
            .verticalScroll(rememberScrollState())
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("practice:text"),
    )
}

@Composable
private fun PracticeButton(
    label: String,
    enabled: Boolean,
    accent: Boolean,
    tag: String,
    onClick: () -> Unit,
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
        Text(label, style = Sendoku.type.label, color = if (accent) colors.onAccent else colors.muted)
    }
}

/** The exercise as a board. Everything already placed is a given, since nothing is entered here. */
internal fun boardOf(exercise: Exercise): GameState {
    val board = Board.parse(exercise.dims, exercise.board)
    return GameState(
        dims = exercise.dims,
        solution = board,
        grade = Grade.GENTLE,
        rating = 0.0,
        hardest = null,
        cells = (0 until exercise.dims.cellCount).map { cell ->
            val digit = board.atIndex(cell)
            Cell(digit = digit, isGiven = digit != Board.EMPTY)
        },
        settings = GameSettings(showTimer = false, mistakeLimit = null),
        isRunning = false,
    )
}
