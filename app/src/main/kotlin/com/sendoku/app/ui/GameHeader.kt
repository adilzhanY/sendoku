package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarIcon(
                icon = SendokuIcons.Back,
                label = stringResource(R.string.back),
                onClick = onLeave,
                tag = "game:back",
            )
            Text(
                text = title(state),
                style = Sendoku.type.label,
                color = colors.given,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            BarIcon(
                icon = SendokuIcons.Settings,
                label = stringResource(R.string.settings_title),
                onClick = onSettings,
                tag = "game:settings",
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Stat(
                label = stringResource(R.string.stat_difficulty),
                value = stringResource(gradeName(state.grade)),
                modifier = Modifier.weight(1f),
            )
            // Both, always. A count that appears only when a setting is on is a count nobody
            // learns to read, and these two are now the two ways to lose.
            Stat(
                label = stringResource(R.string.stat_mistakes),
                value = state.settings.mistakeLimit
                    ?.let { stringResource(R.string.mistakes_of, state.mistakes, it) }
                    ?: state.mistakes.toString(),
                warn = state.mistakes > 0,
                modifier = Modifier.weight(1f),
            )
            Stat(
                label = stringResource(R.string.stat_hints),
                value = state.settings.hintLimit
                    ?.let { stringResource(R.string.mistakes_of, state.hintsUsed, it) }
                    ?: state.hintsUsed.toString(),
                warn = state.settings.hintLimit != null && state.hintsUsed > 0,
                modifier = Modifier.weight(1f),
            )
            if (state.settings.showTimer) {
                Stat(
                    label = stringResource(R.string.stat_time),
                    value = state.elapsed.clock(),
                    modifier = Modifier.weight(1f),
                )
                if (canPause) PauseButton(onPause)
            }
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

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier, warn: Boolean = false) {
    val colors = Sendoku.colors
    Column(
        modifier = modifier
            // A hair of air on both sides, so two neighbouring stats cannot end up reading
            // as one word when both of them have filled their quarter of the screen.
            .padding(horizontal = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label, $value" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Four of these share the width of the screen, so at a large font scale a German
        // label is wider than its quarter of it. Wrapping put "Leicht" over two lines and
        // slid "0 von 3" sideways into the column next to it, so they shrink to fit instead.
        // Down to five if that is what it takes. Four of these share the width of the screen,
        // so a quarter of it is about seventy density pixels once the padding is off, and at
        // twice the font scale a ten letter word in a monospace face needs every bit of that.
        // "Dificultad" in Terminal was the case that found it, arriving as "Dificult".
        OneLine(label, Sendoku.type.statLabel, colors.muted, min = 5.sp)
        OneLine(value, Sendoku.type.statValue, if (warn) colors.conflict else colors.given)
    }
}

@Composable
private fun PauseButton(onPause: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val label = stringResource(R.string.stat_pause)
    Box(
        modifier = Modifier
            .size(dimens.minTouchTarget)
            .clip(CircleShape)
            .background(colors.surfaceRaised)
            .clickable(onClick = onPause)
            .testTag("game:pause")
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(SendokuIcons.Pause, contentDescription = null, tint = colors.muted, modifier = Modifier.size(BAR_ICON))
    }
}

/**
 * The line in the middle of the bar.
 *
 * A daily says which day it is, because a player who opened the calendar and tapped a square
 * in March should be able to see that is where they are. Anything else says nothing, since
 * the grade is already named underneath.
 */
@Composable
private fun title(state: GameState): String {
    val day = state.dailyEpochDay ?: return stringResource(R.string.app_name)
    // Formatted by the platform, not by a format string of our own. A date written the
    // English way in a Russian listing is the kind of small wrongness that reads as sloppy.
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    return LocalDate.ofEpochDay(day).format(format)
}

private val BAR_ICON = 22.dp
