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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sendoku.app.R
import com.sendoku.app.game.GameState
import com.sendoku.app.game.PostMortem
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
    val card = rememberGameCard(state, won)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(dimens.boardRadius))
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            // It scrolls, because at a large font scale in a long language this is taller
            // than the board it covers, and the three buttons at the bottom of it are the
            // only way off a finished game.
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(dimens.spaceL),
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
                    label = stringResource(R.string.outcome_hints),
                    value = state.settings.hintLimit
                        ?.let { stringResource(R.string.mistakes_of, state.hintsUsed, it) }
                        ?: state.hintsUsed.toString(),
                    modifier = Modifier.weight(1f),
                )
                Stat(
                    label = stringResource(R.string.outcome_mistakes),
                    value = state.settings.mistakeLimit
                        ?.let { stringResource(R.string.mistakes_of, state.mistakes, it) }
                        ?: state.mistakes.toString(),
                    modifier = Modifier.weight(1f),
                )
                Stat(
                    label = stringResource(R.string.outcome_moves),
                    value = state.past.size.toString(),
                    modifier = Modifier.weight(1f),
                )
            }

            state.hardest?.let { technique ->
                val lesson = Curriculum.teaching(technique)
                Text(
                    // Taken from the rating rather than from what the player did, because a
                    // player may well have found a longer way round. This describes the
                    // puzzle, not the solve.
                    // Lower cased so it reads as a sentence, unless the name carries
                    // capitals of its own: "needed a naked single" is right, and so is
                    // "Needed a XYZ-Wing", but "needed a xyz-wing" is a name spelled wrong.
                    text = stringResource(R.string.outcome_needed, sentenceCased(technique)),
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

            // Where the time actually went. Only after a win, only when the setting is on,
            // and only when there was a pause worth naming: a fast clean solve is told
            // nothing at all, because an app that finds a lesson in every win turns winning
            // into being marked.
            if (won && state.settings.postMortem) {
                val moments = remember(state.placements) { PostMortem.of(state) }
                if (moments.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.post_mortem_title),
                        style = Sendoku.type.overline,
                        color = colors.muted,
                        modifier = Modifier.padding(top = dimens.spaceS),
                    )
                    for (moment in moments) {
                        Text(
                            text = stringResource(
                                R.string.post_mortem_line,
                                moment.spent.clock(),
                                stringResource(TechniqueCopy.nameOf(moment.available)),
                            ),
                            style = Sendoku.type.body,
                            color = colors.muted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("outcome:moment"),
                        )
                    }
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

            // The puzzle itself, as five characters somebody else can paste. The card above
            // is the boast; this is the grid, and a friend who wants to try the same one
            // needs the second thing rather than the first.
            val code = remember(state.cells, state.catalogIndex) { ShareCode.of(state) }
            val invitation = stringResource(R.string.code_invitation, stringResource(gradeName(state.grade)))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimens.radiusM))
                    .clickable { ShareCode.send(context, code, invitation, card.chooser) }
                    .testTag("outcome:code")
                    .padding(horizontal = dimens.spaceS, vertical = dimens.spaceXs),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(code, style = Sendoku.type.timer, color = colors.accent)
                Text(
                    text = stringResource(R.string.code_share),
                    style = Sendoku.type.body,
                    color = colors.muted,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceM),
                horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
            ) {
                OutcomeButton(
                    stringResource(R.string.outcome_share),
                    accent = false,
                    onClick = { card.share(context) },
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
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    // Three of these across the width of the board, each one keeping to its third of it.
    Column(modifier = modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        OneLine(value, Sendoku.type.title, Sendoku.colors.given)
        OneLine(label, Sendoku.type.overline, Sendoku.colors.muted)
    }
}

/** A technique name as it belongs inside a sentence, with acronyms left alone. */
@Composable
private fun sentenceCased(technique: com.sendoku.engine.technique.TechniqueId): String {
    val name = stringResource(TechniqueCopy.nameOf(technique))
    return if (name.drop(1).any { it.isUpperCase() }) name else name.lowercase()
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
        OneLine(label, Sendoku.type.label, if (accent) colors.onAccent else colors.muted, min = 6.sp)
    }
}
