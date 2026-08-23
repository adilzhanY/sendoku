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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.Sendoku

/**
 * The nine keys.
 *
 * Each one carries how many of that digit are still missing, which is the cheapest piece of
 * help a sudoku app can give and the one most often left out. Knowing there is a single
 * seven left changes where you look next, and counting them by eye across the grid is
 * exactly the kind of bookkeeping that makes a puzzle tiring rather than hard.
 *
 * A digit with none left is dimmed rather than removed. Taking the key away would shuffle
 * the other eight along under a thumb that already knows where they are.
 */
@Composable
public fun NumberPad(state: GameState, onDigit: (Int) -> Unit, modifier: Modifier = Modifier) {
    val dimens = Sendoku.dimens
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
    ) {
        for (digit in 1..state.size) {
            PadKey(
                digit = digit,
                remaining = state.remaining(digit),
                exhausted = state.isExhausted(digit),
                pencilMode = state.pencilMode,
                onClick = { onDigit(digit) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PadKey(
    digit: Int,
    remaining: Int,
    exhausted: Boolean,
    pencilMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    val spoken = when {
        exhausted -> stringResource(R.string.pad_all_placed, digit)
        remaining == 1 -> stringResource(R.string.pad_one_left, digit)
        else -> pluralStringResource(R.plurals.pad_left, remaining, digit, remaining)
    }
    val label = if (pencilMode) stringResource(R.string.pad_notes_mode, spoken) else spoken

    Column(
        modifier = modifier
            // No fixed aspect ratio. Nine keys across a narrow phone gives each one about
            // thirty seven density pixels of width, and a ratio then caps the height below
            // what a digit plus its count needs, which clipped the count in half.
            .heightIn(min = dimens.minTouchTarget + dimens.spaceS)
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(if (pencilMode) colors.surfaceRaised else colors.surface)
            // Still tappable when exhausted: tapping it clears that digit from the selected
            // cell, which is a real thing to want and costs nothing to allow.
            .clickable(onClick = onClick)
            .alpha(if (exhausted) 0.35f else 1f)
            // After the click, not before it. Put ahead of it, the label lands on a different
            // node from the one a screen reader focuses, and the focused one says nothing.
            .semantics(mergeDescendants = true) {
                contentDescription = label
                role = Role.Button
            }
            .testTag("pad:$digit")
            .padding(vertical = dimens.spaceS),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = digit.toString(),
            style = Sendoku.type.padDigit,
            color = if (pencilMode) colors.pencil else colors.given,
        )
        Box(Modifier.padding(top = 1.dp)) {
            Text(
                text = if (exhausted) "" else remaining.toString(),
                style = Sendoku.type.padCount,
                color = colors.muted,
            )
        }
    }
}
