package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.sendoku.app.R
import com.sendoku.app.data.FinishedGame
import com.sendoku.app.game.PuzzleOrigin
import com.sendoku.app.theme.Sendoku
import java.text.DateFormat
import java.util.Date

/**
 * Every game that is over, newest first.
 *
 * All of this was already being written down, one row per finished game, and nothing had ever
 * shown it to the player. The result panel appears for ten seconds at the end of a game and
 * then the only copy of that grid is gone, which is a strange thing for an app whose whole
 * promise is that your record is yours and stays on your phone.
 *
 * Grouped by the day it was finished. Forty rows in a flat list is a wall, and the thing a
 * player is looking for when they open this is nearly always "the one from last night".
 */
@Composable
public fun HistoryScreen(
    games: List<FinishedGame>,
    onBack: () -> Unit,
    onOpen: (FinishedGame) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            BackButton(onClick = onBack, tag = "history:back")
            Text(stringResource(R.string.history_title), style = Sendoku.type.title, color = colors.given)
        }

        if (games.isEmpty()) {
            Empty()
            return@Column
        }

        val days = games.groupBy { startOfDay(it.finishedAt) }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = dimens.spaceM).testTag("history:list"),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
            contentPadding = PaddingValues(bottom = dimens.spaceXl),
        ) {
            for ((day, played) in days) {
                item(key = "day-$day") {
                    Text(
                        text = dayLabel(day),
                        style = Sendoku.type.overline,
                        color = colors.muted,
                        modifier = Modifier.padding(top = dimens.spaceM, bottom = dimens.spaceXs),
                    )
                }
                items(played, key = { it.finishedAt }) { game -> Entry(game) { onOpen(game) } }
            }
        }
    }
}

/** One finished game: how hard, how it went, how long, and when. */
@Composable
private fun Entry(game: FinishedGame, onClick: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val grade = stringResource(gradeName(game.grade))
    val outcome = stringResource(if (game.solved) R.string.card_solved else R.string.history_lost)
    val time = game.elapsed.clock()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = dimens.spaceM, vertical = dimens.spaceS)
            .testTag("history:game:${game.finishedAt}")
            .semantics(mergeDescendants = true) {
                contentDescription = "$grade, $outcome, $time"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
    ) {
        Column(Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(grade, style = Sendoku.type.label, color = colors.given)
                // A daily and a ladder puzzle are the same thing to a player looking for one
                // game, so they sit in one list, and the row says which it was.
                if (game.dailyEpochDay != null) {
                    Text(
                        text = stringResource(R.string.home_daily),
                        style = Sendoku.type.overline,
                        color = colors.accent,
                    )
                }
                // And so does a puzzle somebody sent, or one typed in from somewhere else.
                // Both are a different kind of game to have played: neither opened a level,
                // and both can be played again by whoever brought them.
                val marker = when (game.origin) {
                    PuzzleOrigin.SHARED -> R.string.code_shared
                    PuzzleOrigin.ENTERED -> R.string.enter_marker
                    else -> null
                }
                if (marker != null) {
                    Text(
                        text = stringResource(marker),
                        style = Sendoku.type.overline,
                        color = colors.accent,
                    )
                }
            }
            Text(
                text = outcome,
                style = Sendoku.type.body,
                color = if (game.solved) colors.muted else colors.conflict,
            )
        }
        Text(time, style = Sendoku.type.statValue, color = colors.given)
    }
}

@Composable
private fun Empty() {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Box(Modifier.fillMaxSize().padding(dimens.spaceXl), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
            Text(stringResource(R.string.stats_empty_title), style = Sendoku.type.title, color = colors.given)
            Text(stringResource(R.string.history_empty_body), style = Sendoku.type.body, color = colors.muted)
        }
    }
}

/**
 * The midnight before a timestamp, in the phone's own timezone.
 *
 * Grouping by day has to mean the player's day. A game finished at eleven at night belongs to
 * the evening it happened in, whatever the clock says anywhere else.
 */
private fun startOfDay(at: Long): Long {
    val calendar = java.util.Calendar.getInstance()
    calendar.timeInMillis = at
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

/** The date, written the way the reader's language writes dates. */
private fun dayLabel(at: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(at))
