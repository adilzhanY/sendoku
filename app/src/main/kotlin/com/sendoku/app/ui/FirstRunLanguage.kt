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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.theme.Sendoku

/**
 * Whether the language question has been answered, or does not need asking.
 *
 * The flag says whether this player answered it. The other three say whether they were ever
 * going to be asked: somebody with a saved game, a finished game or a lesson in progress has
 * been using the app since before this screen existed, and stopping them on an update to ask
 * a question they answered by playing is an update that feels broken.
 */
public fun languageAnswered(asked: Boolean, hasGame: Boolean, hasHistory: Boolean, hasLessons: Boolean): Boolean =
    asked || hasGame || hasHistory || hasLessons

/**
 * The first thing a new player sees, and the only time they see it.
 *
 * The app has always followed the phone, and settings has always been able to change it. What
 * was missing is the moment where somebody whose phone is in one language but who reads
 * another finds out that this app has theirs at all. Seven languages is worth one screen.
 *
 * The answer is preselected, so the whole thing is one tap for almost everybody. Following
 * the phone stays the default, because it is right for most people and it keeps being right
 * if they change their phone later.
 */
@Composable
public fun FirstRunLanguage(onChoose: (Language) -> Unit, modifier: Modifier = Modifier) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    // What the phone is set to, and whether that is one of ours. A phone in Portuguese gets
    // told so rather than being quietly given English as though it were the same thing.
    val phone = LocalConfiguration.current.locales[0]?.language.orEmpty()
    val spoken = Language.entries.firstOrNull { it.tag.isNotEmpty() && it.tag.substringBefore('-') == phone }

    var chosen by remember { mutableStateOf(if (spoken != null) Language.SYSTEM else Language.ENGLISH) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(dimens.spaceM)
            .testTag("first-run"),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
    ) {
        Text(stringResource(R.string.app_name), style = Sendoku.type.display, color = colors.given)
        Text(
            text = stringResource(R.string.first_run_prompt),
            style = Sendoku.type.body,
            color = colors.muted,
            modifier = Modifier.padding(bottom = dimens.spaceS),
        )
        if (spoken == null) {
            Text(
                text = stringResource(R.string.first_run_no_translation),
                style = Sendoku.type.body,
                color = colors.muted,
                modifier = Modifier.padding(bottom = dimens.spaceS).testTag("first-run:untranslated"),
            )
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
        ) {
            for (language in Language.entries) {
                Choice(
                    label = stringResource(language.label),
                    // The one row that needs saying twice: following the phone means nothing
                    // unless you are told what the phone is currently set to.
                    detail = when {
                        language != Language.SYSTEM -> null
                        spoken != null -> stringResource(R.string.first_run_system_is, stringResource(spoken.label))
                        else -> null
                    },
                    selected = language == chosen,
                    tag = "first-run:${language.name.lowercase()}",
                ) {
                    chosen = language
                }
            }
        }

        HintChoice(
            label = stringResource(R.string.first_run_continue),
            accent = true,
            tag = "first-run:continue",
            onClick = { onChoose(chosen) },
            modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceS),
        )
    }
}

@Composable
private fun Choice(label: String, detail: String?, selected: Boolean, tag: String, onClick: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(if (selected) colors.surfaceRaised else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = dimens.spaceM, vertical = dimens.spaceS)
            .testTag(tag)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.RadioButton
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        Box(selected)
        Column(Modifier.weight(1f)) {
            Text(label, style = Sendoku.type.label, color = colors.given)
            if (detail != null) {
                Text(detail, style = Sendoku.type.body, color = colors.muted)
            }
        }
    }
}

/** The mark on the chosen row. A filled circle, the same one the settings list uses. */
@Composable
private fun Box(selected: Boolean) {
    val colors = Sendoku.colors
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (selected) colors.accent else colors.hairline),
    )
}
