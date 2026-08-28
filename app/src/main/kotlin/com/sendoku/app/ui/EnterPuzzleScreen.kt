package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.game.Cell
import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.Sendoku
import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade

/**
 * Typing in a puzzle from somewhere else.
 *
 * The engine has always been able to do this. The solver says whether a grid has one answer,
 * the technique ladder says how hard it is and what it turns on, and the hint engine explains
 * the next step of any position at all. None of that was reachable unless the app had dealt
 * you the puzzle itself, which meant the one grid you could never get help with was the one
 * in front of you on paper.
 *
 * So this screen is a way in, and nothing more: the same board, the same keys, the same
 * conflict marking, and then the same game. Every digit typed here is a clue rather than a
 * move, which is the only thing that makes it different from playing.
 *
 * Conflicts are marked while typing whether or not the player has that switch on, because
 * the commonest thing that goes wrong here is a mistyped clue, and finding that out at the
 * end, after a check that says the grid is impossible, is a miserable way to learn it.
 */
@Composable
public fun EnterPuzzleScreen(
    verdict: Verdict,
    onCheck: (Board) -> Unit,
    onPlay: (Board) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    dims: Dimensions = Dimensions.CLASSIC,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    var digits by rememberSaveable { mutableStateOf(List(dims.cellCount) { Board.EMPTY }) }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }

    // A game state built out of what has been typed, so the board and the keys that draw a
    // real game draw this one too. There is no answer to be wrong against yet, so the
    // solution is whatever is on the board: nothing can be a mistake while it is being set.
    val board = remember(digits) {
        Board(dims).also { for ((index, digit) in digits.withIndex()) it.setAtIndex(index, digit) }
    }
    val state = remember(digits, selected) {
        GameState(
            dims = dims,
            solution = board,
            grade = Grade.GENTLE,
            rating = 0.0,
            hardest = null,
            // Every digit here is a clue, so every digit here is drawn as one. A grid
            // typed in from a newspaper is all givens by definition.
            cells = digits.map { Cell(digit = it, isGiven = it != Board.EMPTY) },
            selected = selected,
            settings = GameSettings(flagConflicts = true, showTimer = false),
        )
    }

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            BackButton(onClick = onBack, tag = "enter:back")
            Text(
                text = stringResource(R.string.enter_title),
                style = Sendoku.type.title,
                color = colors.given,
                modifier = Modifier.weight(1f),
            )
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.spaceXs),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(minOf(maxWidth, BOARD_CAP))) {
                SudokuBoard(
                    state = state,
                    onSelect = { selected = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // The keys against the bottom of the screen, the same as when playing, because it
        // is the same thumb doing the same thing.
        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth().padding(dimens.spaceS),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
        ) {
            Text(
                text = said(verdict, digits.count { it != Board.EMPTY }),
                style = Sendoku.type.body,
                color = when (verdict) {
                    is Verdict.Ready -> colors.accent
                    Verdict.Impossible, Verdict.BeyondTheLadder -> colors.conflict
                    else -> colors.muted
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.spaceS),
            )

            NumberPad(
                state = state,
                onDigit = { digit ->
                    val at = selected ?: return@NumberPad
                    digits = digits.toMutableList().also {
                        // Tapping the digit already there takes it out again, which is how
                        // every other digit key in this app behaves.
                        it[at] = if (it[at] == digit) Board.EMPTY else digit
                    }
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(dimens.padGap)) {
                HintChoice(
                    label = stringResource(R.string.enter_clear),
                    accent = false,
                    tag = "enter:clear",
                    onClick = { digits = List(dims.cellCount) { Board.EMPTY } },
                    modifier = Modifier.weight(1f),
                )
                HintChoice(
                    label = stringResource(R.string.enter_check),
                    accent = verdict !is Verdict.Ready,
                    tag = "enter:check",
                    onClick = { onCheck(board) },
                    modifier = Modifier.weight(1f),
                )
                // Playable the moment it has one answer. A grid with more than one is still
                // offered, with the sentence above saying so: a puzzle typed in wrong is
                // still worth looking at, and refusing to open it helps nobody.
                if (verdict is Verdict.Ready || verdict is Verdict.Ambiguous) {
                    HintChoice(
                        label = stringResource(R.string.enter_play),
                        accent = true,
                        tag = "enter:play",
                        onClick = { onPlay(board) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** The one sentence under the board, which is the whole of what this screen has to say. */
@Composable
private fun said(verdict: Verdict, clues: Int): String = when (verdict) {
    Verdict.Unknown -> pluralStringResource(R.plurals.enter_help, clues, clues)

    Verdict.Impossible -> stringResource(R.string.enter_impossible)

    Verdict.Ambiguous -> stringResource(R.string.enter_ambiguous)

    Verdict.BeyondTheLadder -> stringResource(R.string.enter_beyond)

    is Verdict.Ready -> stringResource(
        R.string.enter_ready,
        stringResource(gradeName(verdict.grade)),
        "%.1f".format(verdict.rating),
    )
}

/** The same cap the playing screen uses, so a board is the same size on both. */
private val BOARD_CAP = 560.dp

/** What the app can say about a grid somebody typed in. */
public sealed interface Verdict {
    /** Not checked yet, or changed since it was. */
    public data object Unknown : Verdict

    /** No arrangement of digits finishes it. Something in it is wrong. */
    public data object Impossible : Verdict

    /** More than one answer, so it is not a puzzle, though it can still be played. */
    public data object Ambiguous : Verdict

    /** One answer, and the ladder can reach it. This is a puzzle, and this is its grade. */
    public data class Ready(val grade: Grade, val rating: Double) : Verdict

    /** One answer, and the ladder cannot reach it by reasoning alone. */
    public data object BeyondTheLadder : Verdict
}
