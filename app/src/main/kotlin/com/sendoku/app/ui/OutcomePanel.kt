package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.sendoku.app.R
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.Sendoku

/**
 * What the player sees when the puzzle is finished, won or lost.
 *
 * The numbers are the point. A player who has just spent forty minutes on a Diabolical grid
 * wants to know what it cost them, and the hardest technique it needed is the part that
 * tells them something about themselves rather than about the clock.
 *
 * There is no advertisement here and never will be, which is most of why this app exists.
 */
@Composable
public fun OutcomePanel(
    state: GameState,
    onNextPuzzle: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val won = state.isSolved

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(dimens.boardRadius))
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(dimens.spaceL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
        ) {
            Text(
                text = stringResource(if (won) R.string.outcome_solved else R.string.outcome_lost),
                style = Sendoku.type.overline,
                color = if (won) colors.accent else colors.conflict,
            )
            Text(
                text = state.elapsed.clock(),
                style = Sendoku.type.display,
                color = colors.given,
            )
            Text(
                text = stringResource(
                    R.string.outcome_rated,
                    stringResource(gradeName(state.grade)),
                    "%.2f".format(state.rating),
                ),
                style = Sendoku.type.body,
                color = colors.muted,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = dimens.spaceM),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat(stringResource(R.string.outcome_hints), state.hintsUsed.toString())
                Stat(stringResource(R.string.outcome_mistakes), state.mistakes.toString())
                Stat(stringResource(R.string.outcome_moves), state.past.size.toString())
            }

            state.hardest?.let { technique ->
                Text(
                    // Taken from the rating rather than from what the player did, because a
                    // player may well have found a longer way round. This describes the
                    // puzzle, not the solve.
                    text = stringResource(
                        R.string.outcome_needed,
                        stringResource(TechniqueCopy.nameOf(technique)).lowercase(),
                    ),
                    style = Sendoku.type.body,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceM),
                horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
            ) {
                OutcomeButton(
                    stringResource(R.string.outcome_home),
                    accent = false,
                    onClick = onHome,
                    modifier = Modifier.weight(1f),
                )
                OutcomeButton(
                    label = stringResource(R.string.outcome_another, stringResource(gradeName(state.grade))),
                    accent = true,
                    onClick = onNextPuzzle,
                    modifier = Modifier.weight(1.6f),
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = Sendoku.type.title, color = Sendoku.colors.given)
        Text(label, style = Sendoku.type.overline, color = Sendoku.colors.muted)
    }
}

@Composable
private fun OutcomeButton(label: String, accent: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Box(
        modifier = modifier
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(if (accent) colors.accent else colors.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = Sendoku.type.label,
            color = if (accent) colors.onAccent else colors.muted,
        )
    }
}
