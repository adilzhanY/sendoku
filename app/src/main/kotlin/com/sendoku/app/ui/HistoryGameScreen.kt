package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.data.FinishedGame
import com.sendoku.app.learn.Curriculum
import com.sendoku.app.theme.Sendoku
import com.sendoku.engine.technique.TechniqueId

/**
 * One game out of the history, as it ended.
 *
 * The board is rebuilt rather than stored twice: the digits that were on it, the clues that
 * came with it, and nothing else. There are no pencil marks, because a finished game is a
 * result rather than a position to carry on from, and there is nothing to tap.
 *
 * The share button draws the same card the result panel draws, from the same code, so a game
 * shared a week later is the picture it would have been on the day.
 */
@Composable
public fun HistoryGameScreen(
    game: FinishedGame,
    onBack: () -> Unit,
    onLearn: (TechniqueId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val context = LocalContext.current
    val state = remember(game.finishedAt) { game.replay() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            BackButton(onClick = onBack, tag = "history:game:back")
            Text(
                text = stringResource(gradeName(game.grade)),
                style = Sendoku.type.title,
                color = colors.given,
            )
            Text(
                text = stringResource(if (game.solved) R.string.card_solved else R.string.history_lost),
                style = Sendoku.type.body,
                color = if (game.solved) colors.accent else colors.conflict,
            )
        }

        if (state != null) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.spaceS),
                contentAlignment = Alignment.Center,
            ) {
                val side = minOf(maxWidth, 560.dp)
                Box(Modifier.size(side)) {
                    SudokuBoard(
                        state = state,
                        onSelect = {},
                        live = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.history_no_board),
                style = Sendoku.type.body,
                color = colors.muted,
                modifier = Modifier.padding(horizontal = dimens.spaceM).testTag("history:no-board"),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(dimens.spaceM),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Stat(stringResource(R.string.stat_time), game.elapsed.clock())
            Stat(stringResource(R.string.outcome_mistakes), game.mistakes.toString())
            Stat(stringResource(R.string.outcome_hints), game.hintsUsed.toString())
        }

        game.hardest?.let { technique ->
            val lesson = Curriculum.teaching(technique)
            val name = stringResource(TechniqueCopy.nameOf(technique))
            Text(
                text = stringResource(R.string.outcome_needed, name),
                style = Sendoku.type.body,
                color = colors.muted,
                modifier = Modifier.padding(horizontal = dimens.spaceM),
            )
            if (lesson != null) {
                Text(
                    text = stringResource(R.string.outcome_learn, name),
                    style = Sendoku.type.overline,
                    color = colors.accent,
                    modifier = Modifier
                        .padding(horizontal = dimens.spaceM, vertical = dimens.spaceS)
                        .clip(RoundedCornerShape(dimens.radiusS))
                        .clickable { onLearn(technique) }
                        .padding(dimens.spaceXs)
                        .testTag("history:learn"),
                )
            }
        }

        if (state != null) {
            val card = rememberGameCard(state, game.solved)
            HintChoice(
                label = stringResource(R.string.outcome_share),
                accent = true,
                tag = "history:share",
                onClick = { card.share(context) },
                modifier = Modifier.fillMaxWidth().padding(dimens.spaceM),
            )
        }
    }
}

/** One number with its name under it, the same shape the result panel uses. */
@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OneLine(value, Sendoku.type.title, Sendoku.colors.given)
        OneLine(label, Sendoku.type.overline, Sendoku.colors.muted)
    }
}
