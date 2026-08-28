package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.theme.Sendoku
import com.sendoku.engine.catalog.CodeFault

/**
 * Where a puzzle somebody sent you comes in.
 *
 * A code is the only way a puzzle arrives from outside this phone, and the only thing anybody
 * ever does with one is paste it, so the box is a paste target first and a place to type
 * second. It reads what is in it as loosely as it can: any case, spaces and dashes ignored,
 * and a whole link pasted in works as well as the code out of the middle of it.
 *
 * When it cannot read something it says which of the four things went wrong rather than
 * refusing in general, because "that is not a code" and "that code arrived cut in half" are
 * different problems with different fixes, and only one of them is worth asking a friend to
 * send it again.
 */
@Composable
internal fun CodeBox(fault: CodeFault?, miss: CodeMiss?, onCode: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    var text by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    val send = {
        if (text.isNotBlank()) {
            keyboard?.hide()
            onCode(text)
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
        Text(
            text = stringResource(R.string.code_title),
            style = Sendoku.type.overline,
            color = colors.muted,
            modifier = Modifier.padding(top = Sendoku.dimens.spaceS),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = dimens.minTouchTarget)
                    .clip(RoundedCornerShape(dimens.radiusM))
                    .background(colors.surface)
                    .padding(horizontal = dimens.spaceM, vertical = dimens.spaceS),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it.take(MAX_CODE) },
                    singleLine = true,
                    textStyle = Sendoku.type.timer.copy(color = colors.given),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(
                        // Codes are letters and digits and nothing else, and they are read
                        // out loud in capitals even though the reader does not care.
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { send() }),
                    modifier = Modifier.fillMaxWidth().testTag("home:code:field"),
                )
                if (text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.code_hint),
                        style = Sendoku.type.body,
                        color = colors.muted,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .heightIn(min = dimens.minTouchTarget)
                    .clip(RoundedCornerShape(dimens.radiusM))
                    .background(if (text.isBlank()) colors.surface else colors.accent)
                    .clickable(enabled = text.isNotBlank(), onClick = send)
                    .testTag("home:code:play")
                    .padding(horizontal = dimens.spaceM),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.code_play),
                    style = Sendoku.type.label,
                    color = if (text.isBlank()) colors.muted else colors.onAccent,
                )
            }
        }

        val message = codeMessage(fault, miss)
        if (message != null) {
            Text(
                text = message,
                style = Sendoku.type.body,
                color = colors.conflict,
                modifier = Modifier.padding(top = 2.dp).testTag("home:code:fault"),
            )
        }
    }
}

/**
 * Why a puzzle could not be opened, when the code itself was fine.
 *
 * Two different problems and two different answers. A code naming a puzzle this build does
 * not have is somebody else's app being newer. A grid the ladder cannot finish is a puzzle
 * this app will not pretend to be able to teach, which is a promise rather than a limitation.
 */
public enum class CodeMiss { NOT_IN_THIS_VERSION, CANNOT_BE_REASONED }

/** What went wrong, in a sentence the player can act on. */
@Composable
internal fun codeMessage(fault: CodeFault?, miss: CodeMiss?): String? = when {
    miss == CodeMiss.NOT_IN_THIS_VERSION -> stringResource(R.string.code_missing)
    miss == CodeMiss.CANNOT_BE_REASONED -> stringResource(R.string.code_unplayable)
    fault == null -> null
    fault == CodeFault.BAD_CHARACTER -> stringResource(R.string.code_typo)
    fault == CodeFault.CORRUPT -> stringResource(R.string.code_cut_short)
    fault == CodeFault.TOO_NEW -> stringResource(R.string.code_too_new)
    fault == CodeFault.OUT_OF_RANGE -> stringResource(R.string.code_missing)
    else -> stringResource(R.string.code_unreadable)
}

/** Longer than the longest code there is, and short enough that a paste of a novel is refused. */
private const val MAX_CODE = 64
