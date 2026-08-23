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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.sendoku.app.game.GameSettings
import com.sendoku.app.theme.Sendoku

/**
 * The settings that exist so far.
 *
 * Only the ones the game already honours. A switch that does nothing is worse than a
 * missing one, so the theme picker, the sound toggle and the rest arrive with the features
 * they control rather than ahead of them.
 */
@Composable
public fun SettingsScreen(
    settings: GameSettings,
    onChange: (GameSettings) -> Unit,
    onBack: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = dimens.spaceM),
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
            Text("Settings", style = Sendoku.type.title, color = colors.given)
        }

        SectionLabel("The board")
        Toggle(
            label = "Highlight the row, column and box",
            checked = settings.highlightPeers,
        ) { onChange(settings.copy(highlightPeers = it)) }
        Toggle(
            label = "Highlight the same digit elsewhere",
            checked = settings.highlightSameDigit,
        ) { onChange(settings.copy(highlightSameDigit = it)) }
        Toggle(
            label = "Rub out pencil marks a placement rules out",
            checked = settings.autoClearMarks,
        ) { onChange(settings.copy(autoClearMarks = it)) }
        Toggle(
            label = "Mark a digit that repeats in a row, column or box",
            checked = settings.flagConflicts,
        ) { onChange(settings.copy(flagConflicts = it)) }

        SectionLabel("The clock")
        Toggle(label = "Show the timer", checked = settings.showTimer) {
            onChange(settings.copy(showTimer = it))
        }

        SectionLabel("Mistakes")
        Toggle(
            label = "End the game after three mistakes",
            checked = settings.mistakeLimit != null,
        ) { onChange(settings.copy(mistakeLimit = if (it) 3 else null)) }
        Text(
            text = "Off by default. The hard grades take long enough that losing an hour to " +
                "one slip would be miserable.",
            style = Sendoku.type.body,
            color = colors.muted,
            modifier = Modifier.padding(vertical = dimens.spaceS),
        )

        Text(
            text = "No advertisements, no tracking, no purchases, and nothing leaves this phone.",
            style = Sendoku.type.body,
            color = colors.muted,
            modifier = Modifier.padding(top = dimens.spaceL),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = Sendoku.type.overline,
        color = Sendoku.colors.muted,
        modifier = Modifier.padding(top = Sendoku.dimens.spaceM, bottom = Sendoku.dimens.spaceXs),
    )
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .clickable { onChange(!checked) }
            .padding(horizontal = dimens.spaceS, vertical = dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        Text(label, style = Sendoku.type.label, color = colors.given, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.muted,
                uncheckedTrackColor = colors.surface,
                uncheckedBorderColor = colors.hairline,
            ),
        )
    }
}
