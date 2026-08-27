package com.sendoku.app.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.data.Appearance
import com.sendoku.app.data.ThemeMode
import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.HintLevel
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuIcons
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
            BackButton(onClick = onBack)
            Text(stringResource(R.string.settings_title), style = Sendoku.type.title, color = colors.given)
        }

        // Seven groups, each behind its own mark, and the order is the order somebody reaches
        // for them. Sound first, because it is the one setting a person goes looking for in a
        // hurry: somebody who does not want noise wants it off now, not after scrolling past
        // four themes, and if they cannot find it they close the app instead. Language second,
        // for the same reason turned around: somebody who cannot read the app cannot go
        // looking for the setting that fixes that.
        Section(SendokuIcons.Sound, stringResource(R.string.settings_feedback))
        Toggle(
            label = stringResource(R.string.settings_sound),
            checked = settings.sound,
            note = stringResource(R.string.settings_sound_note),
        ) { onChange(settings.copy(sound = it)) }
        Toggle(label = stringResource(R.string.settings_haptics), checked = settings.haptics) {
            onChange(settings.copy(haptics = it))
        }

        Section(SendokuIcons.Globe, stringResource(R.string.settings_language))
        val activity = LocalActivity.current
        val here = LocalContext.current
        var language by remember { mutableStateOf(Languages.current(here)) }
        // Two at a time, until the text is too big for two. Thirteen rows in one column is
        // half a screen of scrolling in the middle of the page, and every group under it pays
        // for that. But a language is a proper noun and may not be broken in half: at twice
        // the font scale two columns turned Italiano into Italian over o, so past a point the
        // list goes back to one column, where every name fits whole.
        val columns = if (LocalDensity.current.fontScale > 1.3f) 1 else 2
        FlowRow(maxItemsInEachRow = columns) {
            for (choice in Language.entries) {
                Choice(
                    label = stringResource(choice.label),
                    detail = null,
                    selected = language == choice,
                    modifier = Modifier.weight(1f),
                ) {
                    language = choice
                    activity?.let { Languages.choose(it, choice) }
                }
            }
        }

        // How the app looks and what it shows while you play, which is furniture rather than
        // rules: nothing in here changes what a puzzle asks of you.
        Section(SendokuIcons.Palette, stringResource(R.string.settings_interface))
        for (theme in SendokuThemeId.entries) {
            Choice(
                label = stringResource(themeName(theme)),
                detail = stringResource(themeSummary(theme)),
                selected = appearance.theme == theme,
            ) { onAppearanceChange(appearance.copy(theme = theme)) }
        }

        if (!SendokuThemes.isFixed(appearance.theme)) {
            Label(stringResource(R.string.settings_light_or_dark))
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
        Label(stringResource(R.string.settings_clock))
        Toggle(label = stringResource(R.string.settings_show_timer), checked = settings.showTimer) {
            onChange(settings.copy(showTimer = it))
        }

        // What the board does for you while you are playing on it. All of it is bookkeeping
        // somebody would otherwise do by hand, and none of it decides anything.
        Section(SendokuIcons.Board, stringResource(R.string.settings_board))
        Toggle(
            label = stringResource(R.string.settings_highlight_peers),
            checked = settings.highlightPeers,
        ) { onChange(settings.copy(highlightPeers = it)) }
        Toggle(
            label = stringResource(R.string.settings_highlight_same),
            checked = settings.highlightSameDigit,
        ) { onChange(settings.copy(highlightSameDigit = it)) }
        Toggle(
            label = stringResource(R.string.settings_highlight_homes),
            checked = settings.highlightHomes,
        ) { onChange(settings.copy(highlightHomes = it)) }
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
            label = stringResource(R.string.settings_flag_conflicts),
            checked = settings.flagConflicts,
        ) { onChange(settings.copy(flagConflicts = it)) }

        // And the ones that change the game rather than the furniture. They are together
        // because they belong together: whether a mistake ends the puzzle, whether you are
        // told about it, and what a hint is allowed to say are one decision about how much
        // the app carries for you.
        Section(SendokuIcons.Rules, stringResource(R.string.settings_rules))
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
            stringResource(R.string.settings_hint_limit),
            settings.hintLimit != null,
        ) { onChange(settings.copy(hintLimit = if (it) 3 else null)) }
        Text(
            text = stringResource(R.string.settings_hint_note),
            style = Sendoku.type.body,
            color = colors.muted,
            modifier = Modifier.padding(vertical = dimens.spaceS),
        )
        Label(stringResource(R.string.settings_hint_detail))
        for (level in HintLevel.entries) {
            Choice(
                label = stringResource(hintDetailName(level)),
                detail = stringResource(hintDetailNote(level)),
                selected = settings.hintDetail == level,
            ) { onChange(settings.copy(hintDetail = level)) }
        }

        Section(SendokuIcons.Data, stringResource(R.string.settings_your_data))
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

        // The row says what is behind it rather than repeating the heading above it. A
        // group called ABOUT SENDOKU whose only row says About is a stutter, not a signpost.
        Section(SendokuIcons.Info, stringResource(R.string.settings_about))
        Action(stringResource(R.string.account_about_detail), "settings:about", onAbout)
    }
}

/** A radio row. Used where the choices are exclusive rather than on and off. */
@Composable
private fun Choice(
    label: String,
    detail: String?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Row(
        modifier = modifier
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

/**
 * The head of one group, with the mark that makes it findable at a glance.
 *
 * The icon is decorative as far as a screen reader is concerned: it says exactly what the
 * word next to it says, and reading it twice would be worse than not reading it at all.
 */
@Composable
private fun Section(icon: ImageVector, text: String) {
    val dimens = Sendoku.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimens.spaceL, bottom = dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Sendoku.colors.accent,
            modifier = Modifier.size(SECTION_ICON),
        )
        Text(text, style = Sendoku.type.overline, color = Sendoku.colors.accent)
    }
}

/** A quieter heading, for a choice that sits inside a group rather than starting one. */
@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = Sendoku.type.overline,
        color = Sendoku.colors.muted,
        modifier = Modifier.padding(top = Sendoku.dimens.spaceM, bottom = Sendoku.dimens.spaceXs),
    )
}

/** Big enough to find, small enough that seven of them do not become the page. */
private val SECTION_ICON = 20.dp

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
