package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sendoku.app.R
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuIcons
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * What is above the board.
 *
 * Two rows. A bar with the way out on one side and the settings on the other, and under it
 * the four numbers worth knowing while playing, each with the word for what it is.
 *
 * The words matter more than they look like they should. A bare "0/3" in a corner is a
 * riddle, and a clock with no label is read as the time of day at a glance. Naming them
 * costs one line of small text and removes the guessing.
 *
 * Hints are counted and shown. They are free and unlimited here, so the number is a record
 * rather than a rebuke, and hiding it would suggest there was something to hide.
 */
@Composable
public fun GameHeader(
    state: GameState,
    onLeave: () -> Unit,
    onSettings: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * False while a hint is on screen.
     *
     * Pausing hides the board, and hiding the board under an explanation of that board is
     * not something anybody meant to do. Leaving and the settings stay: those are deliberate,
     * and a player must never be trapped in a panel.
     */
    canPause: Boolean = true,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarIcon(
            icon = SendokuIcons.Back,
            label = stringResource(R.string.back),
            onClick = onLeave,
            tag = "game:back",
        )

        Row(
            modifier = Modifier.weight(1f).padding(horizontal = dimens.spaceXs),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Shrinks rather than wraps. Four words of German at twice the font scale used
            // to be what broke this line, and a title that wraps pushes the board down.
            OneLine(
                text = title(state),
                style = Sendoku.type.label,
                color = colors.given,
                min = 9.sp,
                modifier = Modifier.weight(1f, fill = false),
            )
            Marks(
                spent = state.mistakes,
                limit = state.settings.mistakeLimit,
                colour = colors.conflict,
                label = stringResource(R.string.stat_mistakes),
            )
            // Rings rather than discs, because two rows of identical dots side by side read
            // as one row of six. A life you have spent is a life gone; a hint is something
            // you asked for, and the two are not the same kind of mark.
            Marks(
                spent = state.hintsUsed,
                limit = state.settings.hintLimit,
                colour = colors.accent,
                label = stringResource(R.string.stat_hints),
                hollow = true,
            )
            if (state.settings.showTimer) {
                val clock = state.elapsed.clock()
                val time = stringResource(R.string.stat_time)
                Text(
                    text = clock,
                    style = Sendoku.type.timer,
                    color = colors.muted,
                    modifier = Modifier.semantics { contentDescription = "$time, $clock" },
                )
            }
        }

        if (state.settings.showTimer && canPause) {
            BarIcon(
                icon = SendokuIcons.Pause,
                label = stringResource(R.string.stat_pause),
                onClick = onPause,
                tag = "game:pause",
            )
        }
        BarIcon(
            icon = SendokuIcons.Settings,
            label = stringResource(R.string.settings_title),
            onClick = onSettings,
            tag = "game:settings",
        )
    }
}

/**
 * How many of something you have spent, as marks rather than as a sum.
 *
 * A limit is drawn as one dot per life, filled as they go. Reading three dots with one lit is
 * instant, where "1 of 3" is a small piece of arithmetic done every time you glance up, and
 * the dots take a third of the width, which is what let the whole header become one line.
 *
 * With no limit set there is nothing to count towards, so it is a plain number, and it stays
 * out of the way entirely until there is something to say.
 */
@Composable
private fun Marks(spent: Int, limit: Int?, colour: Color, label: String, hollow: Boolean = false) {
    val colors = Sendoku.colors
    if (limit == null) {
        if (spent == 0) return
        Text(
            text = spent.toString(),
            style = Sendoku.type.timer,
            color = colour,
            modifier = Modifier.semantics { contentDescription = "$label, $spent" },
        )
        return
    }
    val spoken = stringResource(R.string.mistakes_of, spent, limit)
    Row(
        horizontalArrangement = Arrangement.spacedBy(DOT_GAP),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$label, $spoken" },
    ) {
        repeat(limit) { index ->
            val on = index < spent
            val ink = if (on) colour else colors.hairline
            Box(
                Modifier
                    .size(DOT)
                    .clip(CircleShape)
                    .then(
                        if (hollow) {
                            Modifier.border(RING, ink, CircleShape)
                        } else {
                            Modifier.background(ink)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun BarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tag: String,
) {
    val dimens = Sendoku.dimens
    Box(
        modifier = Modifier
            .size(dimens.minTouchTarget)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(tag)
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Sendoku.colors.muted, modifier = Modifier.size(BAR_ICON))
    }
}

/**
 * The line in the middle of the bar.
 *
 * The level, because it is the one word that says what kind of afternoon this is going to be.
 * A daily says which day it is as well, because a player who opened the calendar and tapped a
 * square in March should be able to see that is where they are. The app's own name used to be
 * here, which is the one thing nobody opening the app needs to be told.
 */
@Composable
private fun title(state: GameState): String {
    val grade = stringResource(gradeName(state.grade))
    val day = state.dailyEpochDay ?: return grade
    // Formatted by the platform, not by a format string of our own. A date written the
    // English way in a Russian listing is the kind of small wrongness that reads as sloppy.
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    return stringResource(R.string.game_daily_title, LocalDate.ofEpochDay(day).format(format), grade)
}

private val BAR_ICON = 22.dp
private val DOT = 7.dp
private val DOT_GAP = 3.dp
private val RING = 1.5.dp
