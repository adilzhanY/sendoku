package com.sendoku.app.ui

import androidx.activity.compose.LocalActivity
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.sendoku.app.R
import com.sendoku.app.theme.Sendoku

/**
 * The language picker, on a page of its own.
 *
 * It used to be a block in the middle of settings, two columns wide to keep the scrolling
 * down. Eighteen languages is more than a block can hold: it was most of a screen sitting
 * between the sound switches and the themes, and every group under it paid for that, for a
 * setting a person changes once and then never again.
 *
 * One column, and one column at every font scale. The two column list already had to collapse
 * to one on a large scale because a language name is a proper noun and may not be broken in
 * half, so the wide form was only ever right for some people. On a page of its own there is
 * nothing to save room for, and the same list is right for everybody.
 */
@Composable
public fun LanguageScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val activity = LocalActivity.current
    val here = LocalContext.current
    val current = Languages.current(here)
    val spoken = phoneLanguage()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(dimens.spaceM)
            .testTag("language"),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            BackButton(onClick = onBack)
            Text(stringResource(R.string.settings_language), style = Sendoku.type.title, color = colors.given)
        }

        for (choice in Language.entries) {
            LanguageRow(
                label = stringResource(choice.label),
                // The one row that needs saying twice: following the phone means nothing
                // unless you are told what the phone is currently set to.
                detail = if (choice == Language.SYSTEM && spoken != null) {
                    stringResource(R.string.first_run_system_is, stringResource(spoken.label))
                } else {
                    null
                },
                selected = choice == current,
                tag = "language:${choice.name.lowercase()}",
            ) {
                // Popped before the language is applied, not after. Choosing a language
                // restarts the activity, on 13 and later because the system does it and below
                // that because Languages.choose does, and the back stack is saved across that
                // restart like every other piece of state. Applying first would put this page
                // back on top of the stack, so the player would tap a language and land on the
                // language page again, in the new language, wondering whether it worked.
                onBack()
                activity?.let { Languages.choose(it, choice) }
            }
        }
    }
}

/**
 * The language the phone is set to, when Sendoku is written in it.
 *
 * Null when it is not, and the caller says nothing rather than guessing. Shared with the
 * first run screen and with the settings row so all three agree about what following the
 * phone currently means.
 */
@Composable
internal fun phoneLanguage(): Language? {
    val phone = LocalConfiguration.current.locales[0]?.language.orEmpty()
    return Language.entries.firstOrNull { it.tag.isNotEmpty() && it.tag.substringBefore('-') == phone }
}

/** One language. A radio row, because the choice is exclusive and only one can be on. */
@Composable
private fun LanguageRow(label: String, detail: String?, selected: Boolean, tag: String, onSelect: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .clickable(onClick = onSelect)
            .padding(horizontal = dimens.spaceS, vertical = dimens.spaceXs)
            .testTag(tag)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.RadioButton
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = colors.accent, unselectedColor = colors.muted),
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = Sendoku.type.label, color = colors.given)
            if (detail != null) {
                Text(detail, style = Sendoku.type.body, color = colors.muted)
            }
        }
    }
}
