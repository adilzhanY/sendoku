package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sendoku.app.R
import com.sendoku.app.data.Statistics
import com.sendoku.app.learn.CourseProgress
import com.sendoku.app.learn.Curriculum
import com.sendoku.app.theme.Sendoku
import kotlin.time.Duration

/**
 * Everything about the player rather than about a puzzle.
 *
 * There is no account to sign into, which makes the name of this tab slightly funny, so the
 * screen opens by saying so. What lives here is the same thing an account screen holds
 * elsewhere: your record, your settings, and the way to take your data with you. The
 * difference is that all of it is on this phone and none of it is anybody else's.
 */
@Composable
public fun AccountScreen(
    statistics: Statistics,
    course: CourseProgress,
    onStats: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Text(stringResource(R.string.account_title), style = Sendoku.type.title, color = colors.given)
        Text(
            text = stringResource(R.string.account_no_account),
            style = Sendoku.type.body,
            color = colors.muted,
            modifier = Modifier.padding(bottom = dimens.spaceM),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = dimens.spaceM),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Solved and played, side by side and never merged. A player who finished a
            // puzzle and lost it wants to see that both things happened, and a page that
            // only counts wins tells them nothing except nought.
            Figure(
                stringResource(R.string.account_solved),
                statistics.totalSolved.toString(),
                Modifier.weight(1f),
            )
            Figure(
                stringResource(R.string.account_played),
                statistics.gamesPlayed.toString(),
                Modifier.weight(1f),
            )
            Figure(
                stringResource(R.string.account_streak),
                statistics.currentStreak.toString(),
                Modifier.weight(1f),
            )
            Figure(stringResource(R.string.account_time), statistics.totalTime.short(), Modifier.weight(1f))
            Figure(
                stringResource(R.string.account_lessons),
                "${course.finishedCount}/${Curriculum.lessons.size}",
                Modifier.weight(1f),
            )
        }

        Entry(
            label = stringResource(R.string.account_stats),
            detail = stringResource(R.string.account_stats_detail),
            tag = "account:stats",
            onClick = onStats,
        )
        Entry(
            label = stringResource(R.string.account_history),
            detail = stringResource(R.string.account_history_detail),
            tag = "account:history",
            onClick = onHistory,
        )
        Entry(
            label = stringResource(R.string.settings_title),
            detail = stringResource(R.string.account_settings_detail),
            tag = "account:settings",
            onClick = onSettings,
        )
        Entry(
            label = stringResource(R.string.about_title),
            detail = stringResource(R.string.account_about_detail),
            tag = "account:about",
            onClick = onAbout,
        )
    }
}

/**
 * One number, big, with what it counts under it.
 *
 * Five of these share the width of the screen, so each keeps to its fifth of it and shrinks
 * rather than spilling. At two hundred percent in German they used to run together into
 * GELÖSTGESPIELTSERIEZEIT, with the lessons count reading one letter per line down the side.
 */
@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OneLine(value, Sendoku.type.display, Sendoku.colors.given, min = 5.sp)
        OneLine(label.shout(), Sendoku.type.overline, Sendoku.colors.muted, min = 5.sp)
    }
}

/** A row that goes somewhere, or one that only reports when there is nowhere to go. */
@Composable
private fun Entry(label: String, detail: String, tag: String, onClick: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = dimens.spaceM, vertical = dimens.spaceM)
            .testTag(tag)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label, $detail"
                role = Role.Button
            },
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Text(label, style = Sendoku.type.label, color = colors.given)
        Text(detail, style = Sendoku.type.body, color = colors.muted)
    }
}

/** Hours and minutes, since a total playing time in seconds is a number nobody reads. */
private fun Duration.short(): String {
    val hours = inWholeHours
    val minutes = inWholeMinutes % 60
    return if (hours > 0) "${hours}h" else "${minutes}m"
}
