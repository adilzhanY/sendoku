package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.Sendoku

/**
 * Undo, redo, erase, notes, hint.
 *
 * Text labels rather than icons, for now. An icon set is a design job of its own and a
 * guessed-at glyph is worse than a word, particularly for pencil mode, which no icon
 * communicates reliably.
 *
 * Notes is the only one that stays lit, because it is the only one that is a mode. A player
 * who cannot tell which mode they are in will fill a cell they meant to annotate.
 *
 * It is "Notes" rather than "Pencil" for a dull reason that matters: at two hundred percent
 * font scale "Pencil" breaks across two lines and leaves a single letter dangling. Five
 * characters fit where six do not, and it is the word most sudoku apps use anyway.
 */
@Composable
public fun GameToolbar(
    state: GameState,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onErase: () -> Unit,
    onTogglePencil: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = Sendoku.dimens
    Row(
        // Every button as tall as the tallest. At two hundred percent font scale "Pencil"
        // wraps onto a second line, and without this its button alone grows and the row goes
        // ragged.
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
    ) {
        ToolButton("Undo", enabled = state.canUndo, onClick = onUndo, modifier = Modifier.weight(1f))
        ToolButton("Redo", enabled = state.canRedo, onClick = onRedo, modifier = Modifier.weight(1f))
        ToolButton("Erase", enabled = state.selected != null, onClick = onErase, modifier = Modifier.weight(1f))
        ToolButton(
            label = "Notes",
            enabled = true,
            active = state.pencilMode,
            onClick = onTogglePencil,
            modifier = Modifier.weight(1f),
        )
        ToolButton("Hint", enabled = !state.isOver, accent = true, onClick = onHint, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ToolButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    accent: Boolean = false,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    Column(
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(if (active) colors.selection else colors.surface)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.35f)
            .padding(vertical = dimens.spaceS),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            style = Sendoku.type.label,
            color = when {
                active || accent -> colors.accent
                else -> colors.muted
            },
        )
    }
}
