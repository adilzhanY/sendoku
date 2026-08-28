package com.sendoku.app.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sendoku.app.R
import com.sendoku.app.game.GameState

/**
 * One finished game, in the terms the shared card is drawn from.
 *
 * Read in a composition and shared from a click, which is the whole reason this exists as a
 * value rather than as a function. Every word on the card comes out of the string table, so
 * it has to be read where a composable can read it, and a click handler is not one. Getting
 * that wrong would not crash anything: it would quietly write English cards for a player
 * reading Japanese, which is the kind of bug nobody reports.
 *
 * The result panel and the history screen both build one of these, so a game shared a week
 * later is the same picture it would have been on the day.
 */
internal data class GameCard(
    val appName: String,
    val chooser: String,
    val title: String,
    val grade: String,
    val lines: List<ShareCard.Line>,
    val grid: ShareCard.Grid?,
    val look: ShareCard.Look,
) {
    fun share(context: Context) {
        val card = ShareCard.draw(
            appName = appName,
            title = title,
            grade = grade,
            lines = lines,
            grid = grid,
            look = look,
        )
        ShareResult.share(context, card, chooser)
    }
}

@Composable
internal fun rememberGameCard(state: GameState, solved: Boolean = state.isSolved): GameCard = GameCard(
    appName = stringResource(R.string.app_name),
    chooser = stringResource(R.string.outcome_share),
    title = stringResource(if (solved) R.string.card_solved else R.string.card_lost),
    // A Killer says so, because the same grade means a different afternoon.
    grade = if (state.cages.isEmpty()) {
        stringResource(gradeName(state.grade))
    } else {
        stringResource(
            R.string.killer_card,
            stringResource(R.string.killer_title),
            stringResource(gradeName(state.grade)),
        )
    },
    lines = listOfNotNull(
        ShareCard.Line(stringResource(R.string.stat_time), state.elapsed.clock()),
        ShareCard.Line(
            label = stringResource(R.string.stat_mistakes),
            value = state.settings.mistakeLimit
                ?.let { stringResource(R.string.mistakes_of, state.mistakes, it) }
                ?: state.mistakes.toString(),
        ),
        ShareCard.Line(
            label = stringResource(R.string.stat_hints),
            value = state.settings.hintLimit
                ?.let { stringResource(R.string.mistakes_of, state.hintsUsed, it) }
                ?: state.hintsUsed.toString(),
        ),
        // Only when it is true, and only on a win. Three zeroes on the card already say the
        // same thing to somebody who reads them; this says it to somebody who glances.
        if (solved && state.hintsUsed == 0 && state.mistakes == 0 && !state.notesUsed) {
            ShareCard.Line(stringResource(R.string.clean_stat), stringResource(R.string.clean_mark))
        } else {
            null
        },
    ),
    // The board as it was left, so the picture shows the puzzle rather than describing it.
    grid = ShareCard.Grid(
        size = state.size,
        boxWidth = state.dims.boxWidth,
        boxHeight = state.dims.boxHeight,
        digits = state.cells.map { it.digit },
        given = state.cells.indices.filter { state.cells[it].isGiven }.toSet(),
    ),
    look = rememberCardLook(),
)
