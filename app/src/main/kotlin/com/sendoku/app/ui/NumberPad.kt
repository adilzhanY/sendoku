package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuIcons

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
 *
 * The keys have no card behind them. Nine boxes in a row under a grid made of boxes was one
 * pattern too many, and a large digit on the background is unmistakably a key without having
 * to be drawn as one. The tap target is unchanged: it is the whole column, not the glyph.
 */
@Composable
public fun NumberPad(
    state: GameState,
    onDigit: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onScan: (Int) -> Unit = {},
) {
    val dimens = Sendoku.dimens
    // One to nine, left to right, in every language. The keys are Western numerals and they
    // sit under the board they fill in, so mirroring them would put the 1 under the ninth
    // column and leave the board and its keyboard reading in opposite directions.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                    scanning = state.scanning == digit,
                    onClick = { onDigit(digit) },
                    onLongClick = { onScan(digit) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** The tick under a finished key. Small: it is a full stop, not an announcement. */
private val DONE = 12.dp

@Composable
private fun PadKey(
    digit: Int,
    remaining: Int,
    exhausted: Boolean,
    pencilMode: Boolean,
    scanning: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    val spoken = when {
        exhausted -> stringResource(R.string.pad_all_placed, digit)
        remaining == 1 -> stringResource(R.string.pad_one_left, digit)
        else -> pluralStringResource(R.plurals.pad_left, remaining, digit, remaining)
    }
    val label = when {
        scanning -> stringResource(R.string.pad_scanning, spoken)
        pencilMode -> stringResource(R.string.pad_notes_mode, spoken)
        else -> spoken
    }

    Column(
        modifier = modifier
            // No fixed aspect ratio. Nine keys across a narrow phone gives each one about
            // thirty seven density pixels of width, and a ratio then caps the height below
            // what a digit plus its count needs, which clipped the count in half.
            .heightIn(min = dimens.minTouchTarget + dimens.spaceS)
            .clip(RoundedCornerShape(dimens.radiusM))
            // Still tappable when exhausted: tapping it clears that digit from the selected
            // cell, which is a real thing to want and costs nothing to allow.
            // Holding a key asks where the digit could still go, and lights up every cell
            // that could take it. The count says how many are left; this says where.
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
            // Ink normally, accent in pencil mode. Using the entry colour here was wrong in
            // every theme where an entry is already the accent, which is most of them: both
            // modes came out the same colour and the row said nothing.
            color = if (pencilMode || scanning) colors.accent else colors.given,
        )
        Box(Modifier.padding(top = 1.dp)) {
            // A tick rather than a blank once the last one is placed. An empty space under a
            // dimmed key says the key is broken; a tick says the digit is finished, which is
            // a small thing to be told and a pleasant one.
            if (exhausted) {
                Icon(
                    imageVector = SendokuIcons.Done,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(DONE),
                )
            } else {
                Text(
                    text = remaining.toString(),
                    style = Sendoku.type.padCount,
                    color = if (scanning) colors.accent else colors.muted,
                )
            }
        }
    }
}
