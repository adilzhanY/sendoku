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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.sendoku.app.R
import com.sendoku.app.game.GameState
import com.sendoku.app.learn.Curriculum
import com.sendoku.app.theme.Sendoku
import com.sendoku.engine.technique.TechniqueId

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
    /** Opens the lesson for the hardest technique the puzzle needed, when there is one. */
    onLearn: (TechniqueId) -> Unit,
    /** Opens the whole solution, step by step. Only ever reachable once the game is over. */
    onPath: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val won = state.isSolved
    val context = LocalContext.current

    // Read here rather than inside the click, because a click is not a composable and the card
    // has to be written in whatever language the player is reading.
    val appName = stringResource(R.string.app_name)
    val chooser = stringResource(R.string.outcome_share)
    val resultText = stringResource(if (won) R.string.card_solved else R.string.card_lost)
    val gradeText = stringResource(gradeName(state.grade))
    val labels = listOf(
        stringResource(R.string.stat_time) to state.elapsed.clock(),
        stringResource(R.string.stat_mistakes) to (
            state.settings.mistakeLimit
                ?.let { stringResource(R.string.mistakes_of, state.mistakes, it) }
                ?: state.mistakes.toString()
            ),
        stringResource(R.string.stat_hints) to (
            state.settings.hintLimit
                ?.let { stringResource(R.string.mistakes_of, state.hintsUsed, it) }
                ?: state.hintsUsed.toString()
            ),
    )
    // The board as it was left, so the picture shows the puzzle rather than describing it.
    val grid = ShareCard.Grid(
        size = state.size,
        boxWidth = state.dims.boxWidth,
        boxHeight = state.dims.boxHeight,
        digits = state.cells.map { it.digit },
        given = state.cells.indices.filter { state.cells[it].isGiven }.toSet(),
    )

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
                text = stringResource(
                    when {
                        won -> R.string.outcome_solved

                        // Which way it went wrong. "Lost" alone leaves somebody staring at a
                        // board wondering what they did.
                        state.outOfHints -> R.string.outcome_lost_hints

                        else -> R.string.outcome_lost
                    },
                ),
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
                Stat(
                    stringResource(R.string.outcome_hints),
                    state.settings.hintLimit
                        ?.let { stringResource(R.string.mistakes_of, state.hintsUsed, it) }
                        ?: state.hintsUsed.toString(),
                )
                Stat(
                    stringResource(R.string.outcome_mistakes),
                    state.settings.mistakeLimit
                        ?.let { stringResource(R.string.mistakes_of, state.mistakes, it) }
                        ?: state.mistakes.toString(),
                )
                Stat(stringResource(R.string.outcome_moves), state.past.size.toString())
            }

            state.hardest?.let { technique ->
                val lesson = Curriculum.teaching(technique)
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
                // The moment a technique means something. The player has just met it, in a
                // puzzle they finished, and the lesson is one tap away rather than a thing to
                // remember to look for later.
                if (lesson != null) {
                    Text(
                        text = stringResource(
                            R.string.outcome_learn,
                            stringResource(TechniqueCopy.nameOf(technique)),
                        ),
                        style = Sendoku.type.overline,
                        color = colors.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(dimens.radiusS))
                            .clickable { onLearn(technique) }
                            .padding(dimens.spaceS)
                            .testTag("outcome:learn"),
                    )
                }
            }

            // Every step, in order, for the player who wants to know what they were meant
            // to have seen. It is the question a beaten player actually asks, and no other
            // app on the store answers it.
            Text(
                text = stringResource(R.string.outcome_path),
                style = Sendoku.type.overline,
                color = colors.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.radiusS))
                    .clickable(onClick = onPath)
                    .padding(dimens.spaceS)
                    .testTag("outcome:path"),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceM),
                horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
            ) {
                OutcomeButton(
                    stringResource(R.string.outcome_share),
                    accent = false,
                    onClick = { shareCard(context, appName, chooser, resultText, gradeText, labels, grid) },
                    modifier = Modifier.weight(1f),
                    tag = "outcome:share",
                )
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
private fun OutcomeButton(
    label: String,
    accent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Box(
        modifier = modifier
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(if (accent) colors.accent else colors.surface)
            .clickable(onClick = onClick)
            .then(if (tag == null) Modifier else Modifier.testTag(tag)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = Sendoku.type.label,
            color = if (accent) colors.onAccent else colors.muted,
        )
    }
}

/**
 * Builds the card and offers it.
 *
 * Everything it needs arrives already translated, so this knows nothing about languages and
 * the drawing code knows nothing about the app.
 */
private fun shareCard(
    context: android.content.Context,
    appName: String,
    chooser: String,
    title: String,
    grade: String,
    lines: List<Pair<String, String>>,
    grid: ShareCard.Grid,
) {
    val card = ShareCard.draw(
        appName = appName,
        title = title,
        grade = grade,
        lines = lines.map { (label, value) -> ShareCard.Line(label, value) },
        grid = grid,
    )
    ShareResult.share(context, card, chooser)
}
