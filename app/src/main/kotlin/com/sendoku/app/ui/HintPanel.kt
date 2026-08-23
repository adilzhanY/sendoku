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
import androidx.compose.ui.draw.clip
import com.sendoku.app.game.Hint
import com.sendoku.app.game.HintLevel
import com.sendoku.app.theme.Sendoku

/**
 * The hint, as much of it as has been asked for.
 *
 * There is no advertisement in front of this, no countdown, and no limit. A player who
 * needs three hints on their first Diabolical puzzle is exactly the player this app is
 * for, and charging them for it, in money or in attention, would be the whole point missed.
 */
@Composable
public fun HintPanel(
    hint: Hint,
    onMore: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    onGlossary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.radiusL))
            .background(colors.surfaceRaised)
            .padding(dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
    ) {
        when (hint) {
            is Hint.Step -> StepBody(hint, onGlossary)
            is Hint.Mistake -> {
                Text("SOMETHING IS WRONG", style = Sendoku.type.overline, color = colors.conflict)
                Text(
                    text = if (hint.cells.size == 1) {
                        "One of the digits you have placed cannot be right. It is highlighted."
                    } else {
                        "${hint.cells.size} of the digits you have placed cannot be right. " +
                            "They are highlighted."
                    },
                    style = Sendoku.type.body,
                    color = colors.muted,
                )
                Text(
                    text = "Nothing else can be worked out until they are fixed.",
                    style = Sendoku.type.body,
                    color = colors.muted,
                )
            }
            Hint.Solved -> {
                Text("DONE", style = Sendoku.type.overline, color = colors.accent)
                Text("The puzzle is finished.", style = Sendoku.type.body, color = colors.muted)
            }
            Hint.Stuck -> {
                Text("NOTHING TO SUGGEST", style = Sendoku.type.overline, color = colors.muted)
                Text(
                    text = "This board is legal, but none of the techniques Sendoku knows " +
                        "apply to it. That should not happen on a puzzle from the app.",
                    style = Sendoku.type.body,
                    color = colors.muted,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceXs),
            horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
        ) {
            HintButton("Close", accent = false, onClick = onDismiss, modifier = Modifier.weight(1f))
            if (hint is Hint.Step) {
                if (hint.level.hasMore) {
                    HintButton(
                        label = when (hint.level) {
                            HintLevel.NAME -> "Show me where"
                            else -> "Explain it"
                        },
                        accent = true,
                        onClick = onMore,
                        modifier = Modifier.weight(1.6f),
                    )
                } else {
                    HintButton("Do it", accent = true, onClick = onApply, modifier = Modifier.weight(1.6f))
                }
            }
        }
    }
}

@Composable
private fun StepBody(hint: Hint.Step, onGlossary: () -> Unit) {
    val colors = Sendoku.colors
    val technique = hint.deduction.technique
    val region = TechniqueCopy.where(hint.deduction)

    Row(
        horizontalArrangement = Arrangement.spacedBy(Sendoku.dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = technique.displayName.uppercase(),
            style = Sendoku.type.overline,
            color = colors.accent,
        )
        if (region != null && hint.level != HintLevel.NAME) {
            Text(region, style = Sendoku.type.body, color = colors.muted)
        }
    }

    Text(
        text = TechniqueCopy.lookFor(technique),
        style = Sendoku.type.body,
        color = colors.given,
    )

    if (hint.level != HintLevel.NAME) {
        Text(
            text = "WHAT IS THIS?",
            style = Sendoku.type.overline,
            color = colors.accent,
            modifier = Modifier
                .clip(RoundedCornerShape(Sendoku.dimens.radiusS))
                .clickable(onClick = onGlossary)
                .padding(vertical = Sendoku.dimens.spaceXs),
        )
    }

    if (hint.level == HintLevel.FULL) {
        Text(
            text = TechniqueCopy.because(technique),
            style = Sendoku.type.body,
            color = colors.muted,
        )
        Text(
            text = TechniqueCopy.outcome(hint.deduction),
            style = Sendoku.type.body,
            color = colors.accent,
        )
    }
}

@Composable
private fun HintButton(
    label: String,
    accent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Box(
        modifier = modifier
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(if (accent) colors.accent else colors.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = Sendoku.type.label,
            color = if (accent) colors.onAccent else colors.muted,
        )
    }
}
