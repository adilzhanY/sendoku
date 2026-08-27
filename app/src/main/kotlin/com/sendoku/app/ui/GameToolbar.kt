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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuIcons

/**
 * Undo, redo, erase, notes, hint.
 *
 * A drawn mark above a word, on the background rather than on a card. The cards were honest
 * about being tappable and made the bottom of the screen look like a form, and five words in
 * five boxes is a lot of furniture underneath a grid that is itself a box full of boxes.
 *
 * The word stays under every icon. An icon alone is a guess, and pencil mode in particular is
 * a guess nobody gets right: no glyph anywhere means "the digits I type are notes".
 *
 * Notes is the only one that is a mode, so it is the only one that stays lit, and it says so
 * twice: the mark and the word turn to the accent colour, and a bar appears under it. Colour
 * alone would leave the state invisible to anybody who cannot see the difference.
 */
@Composable
public fun GameToolbar(
    state: GameState,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onErase: () -> Unit,
    onTogglePencil: () -> Unit,
    onFillNotes: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ToolButton(
            icon = SendokuIcons.Undo,
            label = stringResource(R.string.tool_undo),
            enabled = state.canUndo,
            onClick = onUndo,
            modifier = Modifier.weight(1f),
        )
        ToolButton(
            icon = SendokuIcons.Redo,
            label = stringResource(R.string.tool_redo),
            enabled = state.canRedo,
            onClick = onRedo,
            modifier = Modifier.weight(1f),
        )
        ToolButton(
            icon = SendokuIcons.Erase,
            label = stringResource(R.string.tool_erase),
            enabled = state.canErase,
            onClick = onErase,
            modifier = Modifier.weight(1f),
        )
        ToolButton(
            icon = SendokuIcons.Notes,
            label = stringResource(R.string.tool_notes),
            enabled = true,
            active = state.pencilMode,
            onClick = onTogglePencil,
            // Holding the notes key pencils the whole board in. It is the one piece of help
            // that makes the hardest levels playable at all, and it is hidden behind a long
            // press rather than a sixth key because it is used once a game, not once a move.
            onLongClick = onFillNotes,
            modifier = Modifier.weight(1f),
        )
        ToolButton(
            icon = SendokuIcons.Hint,
            label = stringResource(R.string.tool_hint),
            enabled = !state.isOver,
            accent = true,
            onClick = onHint,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    accent: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val ink = when {
        active || accent -> colors.accent
        else -> colors.muted
    }

    Column(
        modifier = modifier
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick)
            .alpha(if (enabled) 1f else 0.3f)
            .padding(vertical = dimens.spaceS, horizontal = dimens.spaceXs)
            .testTag("tool:$label")
            .semantics(mergeDescendants = true) {
                contentDescription = label
                role = Role.Button
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = ink, modifier = Modifier.size(ICON))
        // One line, shrunk to fit rather than wrapped. Five words share the width of the
        // screen, and at a large font scale a German label is wider than its fifth of it.
        OneLine(label, Sendoku.type.toolLabel, ink)
        // The mode marker. Two pixels of accent under one of five words is enough to find
        // without being enough to notice while playing.
        Box(
            Modifier
                .padding(top = dimens.spaceXs)
                .size(width = UNDERLINE, height = dimens.gridBoxLine)
                .clip(CircleShape)
                .background(if (active) colors.accent else androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

private val ICON = 26.dp
private val UNDERLINE = 18.dp
