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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.sendoku.app.R
import com.sendoku.app.data.Appearance
import com.sendoku.app.data.ThemeMode
import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.HintLevel
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
    onExport: () -> Unit,
    onImport: () -> Unit,
    onResetCourse: () -> Unit,
    /** What the last export or import did, shown until the screen is left. */
    dataMessage: String?,
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

        // Sound comes first, because it is the one setting somebody goes looking for in a
        // hurry. A person who does not want noise wants it off now, not after scrolling
        // past four themes, and if they cannot find it they turn the app off instead.
        SectionLabel(stringResource(R.string.settings_feedback))
        Toggle(
            label = stringResource(R.string.settings_sound),
            checked = settings.sound,
            note = stringResource(R.string.settings_sound_note),
        ) { onChange(settings.copy(sound = it)) }
        Toggle(label = stringResource(R.string.settings_haptics), checked = settings.haptics) {
            onChange(settings.copy(haptics = it))
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
            label = stringResource(R.string.settings_auto_notes),
            checked = settings.autoNotes,
            note = stringResource(R.string.settings_auto_notes_note),
        ) { onChange(settings.copy(autoNotes = it)) }
        Toggle(
            label = stringResource(R.string.settings_highlight_homes),
            checked = settings.highlightHomes,
        ) { onChange(settings.copy(highlightHomes = it)) }
        Toggle(
            label = stringResource(R.string.settings_auto_check),
            checked = settings.autoCheck || settings.mistakeLimit != null,
            note = stringResource(
                if (settings.mistakeLimit != null) {
                    R.string.settings_auto_check_forced
                } else {
                    R.string.settings_auto_check_note
                },
            ),
        ) { onChange(settings.copy(autoCheck = it)) }
        Toggle(
            label = stringResource(R.string.settings_flag_conflicts),
            checked = settings.flagConflicts,
        ) { onChange(settings.copy(flagConflicts = it)) }

        SectionLabel(stringResource(R.string.settings_clock))
        Toggle(label = stringResource(R.string.settings_show_timer), checked = settings.showTimer) {
            onChange(settings.copy(showTimer = it))
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

        SectionLabel(stringResource(R.string.settings_hints))
        Toggle(
            stringResource(R.string.settings_hint_limit),
            settings.hintLimit != null,
        ) { onChange(settings.copy(hintLimit = if (it) 3 else null)) }
        Text(
            text = stringResource(R.string.settings_hint_note),
            style = Sendoku.type.body,
            color = colors.muted,
            modifier = Modifier.padding(vertical = dimens.spaceS),
        )
        Text(
            text = stringResource(R.string.settings_hint_detail),
            style = Sendoku.type.overline,
            color = colors.muted,
            modifier = Modifier.padding(top = dimens.spaceS),
        )
        for (level in HintLevel.entries) {
            Choice(
                label = stringResource(hintDetailName(level)),
                detail = stringResource(hintDetailNote(level)),
                selected = settings.hintDetail == level,
            ) { onChange(settings.copy(hintDetail = level)) }
        }

        SectionLabel(stringResource(R.string.settings_your_data))
        Text(
            text = stringResource(R.string.settings_data_note),
            style = Sendoku.type.body,
            color = colors.muted,
            modifier = Modifier.padding(bottom = dimens.spaceS),
        )
        Action(stringResource(R.string.settings_export), "settings:export", onExport)
        Action(stringResource(R.string.settings_import), "settings:import", onImport)
        Action(stringResource(R.string.settings_reset_course), "settings:reset-course", onResetCourse)
        if (dataMessage != null) {
            Text(
                text = dataMessage,
                style = Sendoku.type.body,
                color = colors.accent,
                modifier = Modifier.padding(top = dimens.spaceS).testTag("settings:data-message"),
            )
        }

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
private fun Toggle(label: String, checked: Boolean, note: String? = null, onChange: (Boolean) -> Unit) {
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
        Column(Modifier.weight(1f)) {
            Text(label, style = Sendoku.type.label, color = colors.given)
            // The line that says what a setting costs. Two of these change the game rather
            // than the furniture, and finding that out by playing is finding out too late.
            if (note != null) Text(note, style = Sendoku.type.body, color = colors.muted)
        }
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

/** A row that does something, rather than one that holds a setting. */
@Composable
private fun Action(label: String, tag: String, onClick: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Text(
        text = label,
        style = Sendoku.type.label,
        color = colors.accent,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .clickable(onClick = onClick)
            .testTag(tag)
            .padding(horizontal = dimens.spaceM, vertical = dimens.spaceM),
    )
}
