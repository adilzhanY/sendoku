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
import androidx.compose.ui.unit.dp
import com.sendoku.app.data.GradeRecord
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
            Text(
                text = "BACK",
                style = Sendoku.type.overline,
                color = colors.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.radiusS))
                    .clickable(onClick = onBack)
                    .padding(dimens.spaceS),
            )
            Text("Statistics", style = Sendoku.type.title, color = colors.given)
        }

        if (statistics.isEmpty) {
            EmptyState()
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = dimens.spaceS),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Headline(statistics.totalSolved.toString(), "SOLVED")
            Headline(statistics.currentStreak.toString(), "STREAK")
            Headline(statistics.hardestRating?.let { "%.1f".format(it) } ?: "-", "HARDEST")
        }

        statistics.hardestGrade?.let { grade ->
            Note("The hardest you have finished is a ${grade.displayName.lowercase()}.")
        }
        if (statistics.longestStreak > statistics.currentStreak) {
            Note("Your longest run was ${statistics.longestStreak} days.")
        }
        Note("${statistics.totalTime.readable()} spent, ${statistics.totalHints} hints taken.")

        Section("By grade")
        for (grade in Grade.entries.reversed()) {
            val record = statistics.byGrade.getValue(grade)
            if (record.played == 0) continue
            GradeStat(record)
        }

        if (statistics.hardestTechnique.isNotEmpty()) {
            Section("What your puzzles needed")
            val most = statistics.hardestTechnique.values.max()
            for ((technique, count) in statistics.hardestTechnique.entries.sortedBy { it.key.cost }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = dimens.spaceXs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
                ) {
                    Text(
                        text = technique.displayName,
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
            text = "RESET STATISTICS",
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
            title = { Text("Throw away your history?", style = Sendoku.type.title, color = colors.given) },
            text = {
                Text(
                    "Every finished puzzle, every streak and every best time. There is no " +
                        "copy anywhere else, so this cannot be undone.",
                    style = Sendoku.type.body,
                    color = colors.muted,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; onReset() }) {
                    Text("Reset", color = colors.conflict, style = Sendoku.type.label)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text("Keep it", color = colors.muted, style = Sendoku.type.label)
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
        Text("Nothing yet", style = Sendoku.type.title, color = Sendoku.colors.given)
        Text(
            text = "Finish a puzzle and this fills up: how long each grade takes you, how " +
                "far you have climbed, and which techniques your puzzles have needed.",
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
        text = title.uppercase(),
        style = Sendoku.type.overline,
        color = Sendoku.colors.muted,
        modifier = Modifier.padding(top = Sendoku.dimens.spaceL, bottom = Sendoku.dimens.spaceXs),
    )
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
            Text(record.grade.displayName, style = Sendoku.type.label, color = colors.given)
            Text(
                text = if (record.abandoned == 0) {
                    "${record.solved} solved"
                } else {
                    "${record.solved} solved, ${record.abandoned} lost"
                },
                style = Sendoku.type.body,
                color = colors.muted,
            )
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
            Text(record.best?.readable() ?: "-", style = Sendoku.type.timer, color = colors.accent)
            Text(
                text = record.average?.let { "avg ${it.readable()}" } ?: "",
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
