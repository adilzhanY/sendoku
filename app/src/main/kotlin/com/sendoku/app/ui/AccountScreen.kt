package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sendoku.app.R
import com.sendoku.app.data.FinishedGame
import com.sendoku.app.data.Statistics
import com.sendoku.app.learn.CourseProgress
import com.sendoku.app.learn.Curriculum
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuIcons
import com.sendoku.engine.Grade
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration

/**
 * Everything about the player rather than about a puzzle.
 *
 * There is no account to sign into, which makes the name of this tab slightly funny. What
 * lives here is the same thing an account screen holds elsewhere: your record, your settings,
 * and the way to take your data with you. The difference is that all of it is on this phone
 * and none of it is anybody else's.
 *
 * The record used to be five numbers on one line, which is where it went wrong. Five figures
 * sharing the width of a phone means each gets a fifth of it, and a fifth is not enough for
 * "12/42", so that one shrank, and once it shrank its baseline no longer matched the other
 * four and its label sat a line higher than theirs. A row of numbers that cannot hold its own
 * line reads as broken, because it is.
 *
 * So the record is drawn now rather than listed. The hardest puzzle you have beaten leads,
 * because in an app whose whole pitch is difficulty that is the number worth having and no
 * other sudoku app can show it. Under it the plain counts sit in a grid, where every figure
 * has the same width and every baseline agrees. Then the two pictures the database could
 * always have drawn and never did: what you have beaten at each level, and every day of the
 * last five weeks you played on.
 */
@Composable
public fun AccountScreen(
    statistics: Statistics,
    course: CourseProgress,
    /** Every finished game, for the best solve and for the days played. */
    history: List<FinishedGame>,
    onStats: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val best = remember(history) { history.filter { it.solved }.maxByOrNull { it.rating } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
    ) {
        Text(stringResource(R.string.account_title), style = Sendoku.type.title, color = colors.given)
        Text(
            text = stringResource(R.string.account_no_account),
            style = Sendoku.type.body,
            color = colors.muted,
            modifier = Modifier.padding(bottom = dimens.spaceS),
        )

        if (statistics.isEmpty) {
            Text(
                text = stringResource(R.string.account_empty),
                style = Sendoku.type.body,
                color = colors.muted,
                modifier = Modifier.padding(bottom = dimens.spaceM).testTag("account:empty"),
            )
        } else {
            best?.let { Hardest(it) }
            Figures(statistics, course)
            // Above the charts, not below them. The charts are worth scrolling for; the way
            // to the settings is not something a player should have to scroll to find.
            Doors(onStats, onHistory, onSettings, onAbout)
            Beaten(statistics)
            Recent(history)
        }

        if (statistics.isEmpty) {
            Doors(onStats, onHistory, onSettings, onAbout)
        }
    }
}

/**
 * The hardest puzzle beaten, said properly.
 *
 * The rating rather than the level alone, because two Expert puzzles are not the same puzzle
 * and the number is the only thing that says which one was harder. The day and the clock are
 * there because a record with no date is a boast rather than a memory.
 */
@Composable
private fun Hardest(game: FinishedGame) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val locale = LocalConfiguration.current.locales[0]
    val day = remember(game.finishedAt, locale) {
        Instant.ofEpochMilli(game.finishedAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.radiusL))
            .background(colors.surfaceRaised)
            .padding(dimens.spaceM)
            .testTag("account:hardest"),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Text(stringResource(R.string.account_hardest), style = Sendoku.type.overline, color = colors.accent)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
            Text("%.1f".format(game.rating), style = Sendoku.type.display, color = colors.given)
            Text(
                text = stringResource(gradeName(game.grade)),
                style = Sendoku.type.label,
                color = colors.muted,
                modifier = Modifier.padding(bottom = dimens.spaceXs),
            )
        }
        Text(
            text = stringResource(R.string.account_hardest_note, day, game.elapsed.clock()),
            style = Sendoku.type.body,
            color = colors.muted,
        )
    }
}

/**
 * The plain counts, in a grid rather than on a line.
 *
 * Three to a row, so each has a third of the screen instead of a fifth, and two rows rather
 * than one so nothing has to shrink to fit. Every one of them says what it counts.
 */
@Composable
private fun Figures(statistics: Statistics, course: CourseProgress) {
    val dimens = Sendoku.dimens
    val cells = listOf(
        stringResource(R.string.account_solved) to statistics.totalSolved.toString(),
        stringResource(R.string.account_played) to statistics.gamesPlayed.toString(),
        stringResource(R.string.account_streak) to statistics.currentStreak.toString(),
        stringResource(R.string.account_time) to statistics.totalTime.short(),
        stringResource(R.string.account_lessons) to "${course.finishedCount}/${Curriculum.lessons.size}",
        stringResource(R.string.account_hints) to statistics.totalHints.toString(),
    )
    Column(verticalArrangement = Arrangement.spacedBy(dimens.padGap)) {
        for (row in cells.chunked(3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.padGap)) {
                for ((label, value) in row) {
                    Figure(label, value, Modifier.weight(1f))
                }
            }
        }
    }
}

/** One number, with what it counts under it, on its own ground. */
@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Sendoku.dimens.radiusS))
            .background(Sendoku.colors.surface)
            .padding(vertical = Sendoku.dimens.spaceS, horizontal = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$value $label" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OneLine(value, Sendoku.type.title, Sendoku.colors.given, min = 8.sp)
        OneLine(label, Sendoku.type.overline, Sendoku.colors.muted, min = 6.sp)
    }
}

/**
 * How many puzzles beaten at each level.
 *
 * All eight, including the ones at nought. A chart that hides what you have not done yet is
 * a chart that cannot show you how far there is left to go, and in this app that is the
 * interesting half. The advanced levels keep the red they have everywhere else.
 */
@Composable
private fun Beaten(statistics: Statistics) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val counts = Grade.entries.associateWith { statistics.byGrade.getValue(it).solved }
    val most = counts.values.maxOrNull() ?: 0

    Column(
        modifier = Modifier.padding(top = dimens.spaceS),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Text(stringResource(R.string.account_beaten), style = Sendoku.type.overline, color = colors.muted)
        for (grade in Grade.entries) {
            val solved = counts.getValue(grade)
            val name = stringResource(gradeName(grade))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .testTag("account:beaten:${grade.name}")
                    .semantics(mergeDescendants = true) { contentDescription = "$name, $solved" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
            ) {
                Text(
                    text = name,
                    style = Sendoku.type.body,
                    color = colors.muted,
                    maxLines = 1,
                    modifier = Modifier.width(NAME_COLUMN),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(BAR)
                        .clip(CircleShape)
                        .background(colors.surface),
                ) {
                    if (solved > 0 && most > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth(solved.toFloat() / most)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(if (grade.isAdvanced) colors.conflict else colors.accent),
                        )
                    }
                }
                Text(
                    text = solved.toString(),
                    style = Sendoku.type.body,
                    color = if (solved == 0) colors.muted else colors.given,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(COUNT_COLUMN),
                )
            }
        }
    }
}

/**
 * Every day of the last five weeks, and whether a puzzle was finished on it.
 *
 * A streak you can look at rather than a number you have to take on trust. Three shades and
 * an empty one, because a scale finer than that is invisible at this size and a scale coarser
 * than that cannot tell one puzzle from an afternoon of them.
 */
@Composable
private fun Recent(history: List<FinishedGame>) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val zone = ZoneId.systemDefault()
    val today = remember { LocalDate.now(zone) }
    val byDay = remember(history) {
        history.groupingBy { Instant.ofEpochMilli(it.finishedAt).atZone(zone).toLocalDate() }.eachCount()
    }

    Column(
        modifier = Modifier.padding(top = dimens.spaceS),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Text(stringResource(R.string.account_recent), style = Sendoku.type.overline, color = colors.muted)
        Column(verticalArrangement = Arrangement.spacedBy(DAY_GAP)) {
            for (week in 0 until WEEKS) {
                Row(horizontalArrangement = Arrangement.spacedBy(DAY_GAP)) {
                    for (weekday in 0 until 7) {
                        val back = (WEEKS * 7 - 1) - (week * 7 + weekday)
                        val played = byDay[today.minusDays(back.toLong())] ?: 0
                        // One scale, made of the accent at three strengths, so a busy day
                        // reads as more than a quiet one rather than merely different.
                        Box(
                            Modifier
                                .size(DAY)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        played == 0 -> colors.surface
                                        played == 1 -> colors.accent.copy(alpha = 0.28f)
                                        played == 2 -> colors.accent.copy(alpha = 0.6f)
                                        else -> colors.accent
                                    },
                                ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The four places this page leads to.
 *
 * Tiles rather than the four full rows they used to be. The rows carried a sentence each
 * explaining what was behind them, which is worth reading once and never again, and they took
 * the whole lower half of a page that now has something to put there.
 */
@Composable
private fun Doors(onStats: () -> Unit, onHistory: () -> Unit, onSettings: () -> Unit, onAbout: () -> Unit) {
    val dimens = Sendoku.dimens
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceM),
        horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
    ) {
        Door(SendokuIcons.Chart, stringResource(R.string.account_stats), "account:stats", onStats, Modifier.weight(1f))
        Door(
            SendokuIcons.History,
            stringResource(R.string.account_history),
            "account:history",
            onHistory,
            Modifier.weight(1f),
        )
        Door(
            SendokuIcons.Settings,
            stringResource(R.string.settings_title),
            "account:settings",
            onSettings,
            Modifier.weight(1f),
        )
        Door(SendokuIcons.Info, stringResource(R.string.about_title), "account:about", onAbout, Modifier.weight(1f))
    }
}

@Composable
private fun Door(icon: ImageVector, label: String, tag: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Column(
        modifier = modifier
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(vertical = dimens.spaceS, horizontal = 4.dp)
            .testTag(tag)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                role = Role.Button
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(DOOR_ICON),
        )
        OneLine(label, Sendoku.type.overline, colors.given, min = 6.sp)
    }
}

private val DOOR_ICON = 20.dp
private val NAME_COLUMN = 74.dp
private val COUNT_COLUMN = 24.dp
private val BAR = 7.dp
private val DAY = 18.dp
private val DAY_GAP = 4.dp
private const val WEEKS = 5

/** Hours and minutes, since a total playing time in seconds is a number nobody reads. */
private fun Duration.short(): String {
    val hours = inWholeHours
    val minutes = inWholeMinutes % 60
    return if (hours > 0) "${hours}h" else "${minutes}m"
}
