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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.sendoku.app.R
import com.sendoku.app.data.Appearance
import com.sendoku.app.data.ThemeMode
import com.sendoku.app.game.GameSettings
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuThemeId
import com.sendoku.app.theme.SendokuThemes

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
    appearance: Appearance,
    onChange: (GameSettings) -> Unit,
    onAppearanceChange: (Appearance) -> Unit,
    onBack: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            Text(
                text = stringResource(R.string.back),
                style = Sendoku.type.overline,
                color = colors.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.radiusS))
                    .clickable(onClick = onBack)
                    .padding(dimens.spaceS),
            )
            Text(stringResource(R.string.settings_title), style = Sendoku.type.title, color = colors.given)
        }

        SectionLabel(stringResource(R.string.settings_look))
        for (theme in SendokuThemeId.entries) {
            Choice(
                label = stringResource(themeName(theme)),
                detail = stringResource(themeSummary(theme)),
                selected = appearance.theme == theme,
            ) { onAppearanceChange(appearance.copy(theme = theme)) }
        }

        if (!SendokuThemes.isFixed(appearance.theme)) {
            SectionLabel(stringResource(R.string.settings_light_or_dark))
            for (mode in ThemeMode.entries) {
                Choice(
                    label = stringResource(modeName(mode)),
                    detail = null,
                    selected = appearance.mode == mode,
                ) { onAppearanceChange(appearance.copy(mode = mode)) }
            }
        } else {
            Text(
                text = stringResource(R.string.settings_terminal_is_dark),
                style = Sendoku.type.body,
                color = colors.muted,
                modifier = Modifier.padding(vertical = dimens.spaceS),
            )
        }

        SectionLabel(stringResource(R.string.settings_board))
        Toggle(
            label = stringResource(R.string.settings_highlight_peers),
            checked = settings.highlightPeers,
        ) { onChange(settings.copy(highlightPeers = it)) }
        Toggle(
            label = stringResource(R.string.settings_highlight_same),
            checked = settings.highlightSameDigit,
        ) { onChange(settings.copy(highlightSameDigit = it)) }
        Toggle(
            label = stringResource(R.string.settings_auto_clear),
            checked = settings.autoClearMarks,
        ) { onChange(settings.copy(autoClearMarks = it)) }
        Toggle(
            label = stringResource(R.string.settings_flag_conflicts),
            checked = settings.flagConflicts,
        ) { onChange(settings.copy(flagConflicts = it)) }

        SectionLabel(stringResource(R.string.settings_clock))
        Toggle(label = stringResource(R.string.settings_show_timer), checked = settings.showTimer) {
            onChange(settings.copy(showTimer = it))
        }

        SectionLabel(stringResource(R.string.settings_feedback))
        Toggle(label = stringResource(R.string.settings_haptics), checked = settings.haptics) {
            onChange(settings.copy(haptics = it))
        }
        Toggle(label = stringResource(R.string.settings_sound), checked = settings.sound) {
            onChange(settings.copy(sound = it))
        }

        SectionLabel(stringResource(R.string.settings_mistakes))
        Toggle(
            label = stringResource(R.string.settings_mistake_limit),
            checked = settings.mistakeLimit != null,
        ) { onChange(settings.copy(mistakeLimit = if (it) 3 else null)) }
        Text(
            text = stringResource(R.string.settings_mistake_note),
            style = Sendoku.type.body,
            color = colors.muted,
            modifier = Modifier.padding(vertical = dimens.spaceS),
        )

        Text(
            text = stringResource(R.string.settings_about),
            style = Sendoku.type.overline,
            color = colors.accent,
            modifier = Modifier
                .padding(top = dimens.spaceL)
                .clip(RoundedCornerShape(dimens.radiusS))
                .clickable(onClick = onAbout)
                .padding(dimens.spaceS),
        )
    }
}

/** A radio row. Used where the choices are exclusive rather than on and off. */
@Composable
private fun Choice(label: String, detail: String?, selected: Boolean, onSelect: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .clickable(onClick = onSelect)
            .padding(horizontal = dimens.spaceS, vertical = dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = colors.accent,
                unselectedColor = colors.muted,
            ),
        )
        Column(Modifier.fillMaxWidth()) {
            Text(label, style = Sendoku.type.label, color = colors.given)
            if (detail != null) {
                Text(detail, style = Sendoku.type.body, color = colors.muted)
            }
        }
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
