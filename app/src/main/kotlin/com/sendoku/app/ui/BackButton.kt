package com.sendoku.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuIcons

/**
 * The way out of a screen. One chevron, and the same one everywhere.
 *
 * Every screen used to spell the word BACK, which is the one label on a phone that nobody
 * needs: an arrow pointing the way you came is understood before it is read, in every
 * language, and it does not grow to three lines in German at a large font scale. The game
 * screen already had the arrow, so twelve screens were saying it two different ways.
 *
 * The word is still here, as the thing a screen reader announces. It also still turns around
 * in Arabic, because the chevron is auto-mirrored and the way back is the other way there.
 */
@Composable
internal fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier, tag: String = "back") {
    val label = stringResource(R.string.back)
    Box(
        modifier = modifier
            .size(Sendoku.dimens.minTouchTarget)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(tag)
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = SendokuIcons.Back,
            contentDescription = null,
            tint = Sendoku.colors.muted,
            modifier = Modifier.size(ARROW),
        )
    }
}

private val ARROW = 22.dp
