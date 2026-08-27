package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.data.GradeRecord
import com.sendoku.app.data.HintLog
import com.sendoku.app.data.Statistics
import com.sendoku.app.theme.Sendoku
import com.sendoku.engine.Grade
import kotlin.time.Duration

/**
 * What the player has done.
 *
 * The hardest puzzle solved comes first, because in an app built around difficulty that is
 * the number somebody actually wants to see. Everything else is supporting detail.
 */
@Composable
public fun StatsScreen(
    statistics: Statistics,
    hints: HintLog,
    onBack: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = dimens.spaceS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            BackButton(onClick = onBack)
            Text(stringResource(R.string.stats_title), style = Sendoku.type.title, color = colors.given)
        }

        if (statistics.isEmpty) {
            EmptyState()
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = dimens.spaceS),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Headline(statistics.totalSolved.toString(), stringResource(R.string.stats_solved))
            Headline(statistics.currentStreak.toString(), stringResource(R.string.stats_streak))
            Headline(statistics.hardestRating?.let { "%.1f".format(it) } ?: "-", stringResource(R.string.stats_hardest))
        }

        statistics.hardestGrade?.let { grade ->
            Note(stringResource(R.string.stats_hardest_note, stringResource(gradeName(grade)).lowercase()))
        }
        if (statistics.longestStreak > statistics.currentStreak) {
            Note(pluralStringResource(R.plurals.stats_longest_run, statistics.longestStreak, statistics.longestStreak))
        }
        Note(stringResource(R.string.stats_totals, statistics.totalTime.readable(), statistics.totalHints))

        Section(stringResource(R.string.stats_by_grade))
        // Easiest first, the same order the home screen uses. Two lists of the same six
        // things in opposite orders is a way to misread both.
        for (grade in Grade.entries) {
            val record = statistics.byGrade.getValue(grade)
            if (record.played == 0) continue
            GradeStat(record)
        }

        // What the player has actually asked for help with. The rule at the top of this list
        // is the lesson they need, and it is the one number in the app that would be worth
        // sending to a server if the app did that, which it does not.
        if (hints.total > 0) {
            Section(stringResource(R.string.stats_hints_title))
            for ((technique, count) in hints.byTechnique.entries.sortedByDescending { it.value }.take(5)) {
                Line(stringResource(TechniqueCopy.nameOf(technique)), count.toString())
            }
            for ((level, count) in hints.byLevel.entries.sortedBy { it.key.ordinal }) {
                Line(stringResource(hintDetailName(level)), count.toString())
            }
            hints.hardest?.let { worst ->
                Note(stringResource(R.string.stats_hints_note, stringResource(TechniqueCopy.nameOf(worst))))
            }
        }

        if (statistics.hardestTechnique.isNotEmpty()) {
            Section(stringResource(R.string.stats_needed))
            val most = statistics.hardestTechnique.values.max()
            for ((technique, count) in statistics.hardestTechnique.entries.sortedBy { it.key.cost }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = dimens.spaceXs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
                ) {
                    Text(
                        text = stringResource(TechniqueCopy.nameOf(technique)),
                        style = Sendoku.type.body,
                        color = colors.given,
                        modifier = Modifier.fillMaxWidth(0.45f),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.8f * count / most)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(colors.accent),
                    )
                    Text(count.toString(), style = Sendoku.type.timer, color = colors.muted)
                }
            }
        }

        Text(
            text = stringResource(R.string.stats_reset),
            style = Sendoku.type.overline,
            color = colors.conflict,
            modifier = Modifier
                .padding(top = dimens.spaceXl)
                .clip(RoundedCornerShape(dimens.radiusS))
                .clickable { confirmReset = true }
                .padding(dimens.spaceS),
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = colors.surfaceRaised,
            title = {
                Text(stringResource(R.string.stats_reset_title), style = Sendoku.type.title, color = colors.given)
            },
            text = {
                Text(
                    stringResource(R.string.stats_reset_body),
                    style = Sendoku.type.body,
                    color = colors.muted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onReset()
                }) {
                    Text(
                        stringResource(R.string.stats_reset_confirm),
                        color = colors.conflict,
                        style = Sendoku.type.label,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.stats_reset_cancel), color = colors.muted, style = Sendoku.type.label)
                }
            },
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Sendoku.dimens.spaceXl),
        verticalArrangement = Arrangement.spacedBy(Sendoku.dimens.spaceS),
    ) {
        Text(stringResource(R.string.stats_empty_title), style = Sendoku.type.title, color = Sendoku.colors.given)
        Text(
            text = stringResource(R.string.stats_empty_body),
            style = Sendoku.type.body,
            color = Sendoku.colors.muted,
        )
    }
}

@Composable
private fun Headline(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = Sendoku.type.display, color = Sendoku.colors.given)
        Text(label, style = Sendoku.type.overline, color = Sendoku.colors.muted)
    }
}

@Composable
private fun Section(title: String) {
    Text(
        text = title.shout(),
        style = Sendoku.type.overline,
        color = Sendoku.colors.muted,
        modifier = Modifier.padding(top = Sendoku.dimens.spaceL, bottom = Sendoku.dimens.spaceXs),
    )
}

/** A label and a number, for the rows that are only ever a tally. */
@Composable
private fun Line(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Sendoku.dimens.spaceXs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = Sendoku.type.body, color = Sendoku.colors.given)
        Text(value, style = Sendoku.type.body, color = Sendoku.colors.muted)
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = Sendoku.type.body, color = Sendoku.colors.muted)
}

@Composable
private fun GradeStat(record: GradeRecord) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(colors.surface)
            .padding(dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.fillMaxWidth(0.5f)) {
            Text(stringResource(gradeName(record.grade)), style = Sendoku.type.label, color = colors.given)
            Text(
                text = if (record.abandoned == 0) {
                    stringResource(R.string.stats_solved_count, record.solved)
                } else {
                    stringResource(R.string.stats_solved_and_lost, record.solved, record.abandoned)
                },
                style = Sendoku.type.body,
                color = colors.muted,
            )
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
            Text(record.best?.readable() ?: "-", style = Sendoku.type.timer, color = colors.accent)
            Text(
                text = record.average?.let { stringResource(R.string.stats_average, it.readable()) } ?: "",
                style = Sendoku.type.body,
                color = colors.muted,
            )
        }
    }
}

/** Hours only when there are hours, because "0:04:12" reads as a mistake. */
internal fun Duration.readable(): String {
    val total = inWholeSeconds
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        "$hours:" + minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0')
    } else {
        "$minutes:" + seconds.toString().padStart(2, '0')
    }
}
