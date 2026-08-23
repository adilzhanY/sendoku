package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.sendoku.app.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.sendoku.app.theme.Sendoku

/**
 * What the player sees while the game is paused.
 *
 * The board is covered rather than blurred. A blur would be prettier and would need
 * RenderEffect, which arrives at API 31 while Sendoku supports 26, so half the supported
 * devices would get the plain version anyway. Covering it works everywhere and does the one
 * thing that actually matters, which is that a paused clock cannot be cheated by reading
 * the grid.
 */
@Composable
public fun PauseOverlay(
    elapsed: String,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(dimens.boardRadius))
            .background(colors.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onResume,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
        ) {
            Text(stringResource(R.string.paused), style = Sendoku.type.overline, color = colors.muted)
            Text(elapsed, style = Sendoku.type.display, color = colors.given)
            Text(
                stringResource(R.string.paused_tap),
                style = Sendoku.type.body,
                color = colors.muted,
                modifier = Modifier.padding(top = dimens.spaceS),
            )
        }
    }
}
