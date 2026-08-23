package com.sendoku.app.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A sheet of every token, so a change can be looked at rather than imagined.
 *
 * Colour tokens are checked by arithmetic elsewhere, which catches contrast and nothing
 * else. Whether the muted grey actually reads as quiet next to the accent, or whether the
 * pencil marks disappear under a selection wash, is a question only an eye can answer.
 * This is where that eye looks.
 */
@Composable
private fun TokenSheet() {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    Column(
        modifier = Modifier
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        Text(
            text = if (colors.isDark) "DEEP FIELD DARK" else "DEEP FIELD LIGHT",
            style = Sendoku.type.overline,
            color = colors.muted,
        )

        Section("Surfaces") {
            Swatch("background", colors.background)
            Swatch("surface", colors.surface)
            Swatch("surfaceRaised", colors.surfaceRaised)
        }

        Section("Ink") {
            Swatch("given", colors.given)
            Swatch("entry", colors.entry)
            Swatch("pencil", colors.pencil)
            Swatch("muted", colors.muted)
            Swatch("conflict", colors.conflict)
        }

        Section("Lines") {
            Swatch("hairline", colors.hairline)
            Swatch("boxLine", colors.boxLine)
        }

        Section("Washes") {
            Swatch("selection", colors.selection)
            Swatch("peer", colors.peer)
            Swatch("match", colors.match)
            Swatch("hintLogic", colors.hintLogic)
            Swatch("conflictWash", colors.conflictWash)
        }

        Section("Type") {
            Text("58%", style = Sendoku.type.display, color = colors.given)
            Text("Severe", style = Sendoku.type.title, color = colors.given)
            Text(
                "Only two cells in row three can hold a two or a six, so they hold one each.",
                style = Sendoku.type.body,
                color = colors.muted,
            )
            Text("24:08", style = Sendoku.type.timer, color = colors.muted)
        }

        Section("Digits") {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
            ) {
                Text("5", style = Sendoku.type.gridGiven, color = colors.given)
                Text("3", style = Sendoku.type.gridEntry, color = colors.entry)
                Text("247", style = Sendoku.type.pencilMark, color = colors.pencil)
                Text("9", style = Sendoku.type.padDigit, color = colors.given)
            }
        }

        Section("Rules and rounding") {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
                Box(
                    Modifier
                        .size(dimens.minTouchTarget)
                        .background(colors.surface, RoundedCornerShape(dimens.boardRadius))
                        .border(dimens.gridBoxLine, colors.boxLine, RoundedCornerShape(dimens.boardRadius)),
                )
                Box(
                    Modifier
                        .size(dimens.minTouchTarget)
                        .background(colors.surface, RoundedCornerShape(dimens.radiusM))
                        .border(dimens.gridHairline, colors.hairline, RoundedCornerShape(dimens.radiusM)),
                )
                Box(
                    Modifier
                        .size(dimens.minTouchTarget)
                        .background(colors.accent, RoundedCornerShape(dimens.radiusM)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("7", style = Sendoku.type.padDigit, color = colors.onAccent)
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Sendoku.dimens.spaceXs)) {
        Text(title, style = Sendoku.type.overline, color = Sendoku.colors.muted)
        content()
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
    ) {
        // Painted over the surface, so a wash shows what it will actually look like.
        Box(
            Modifier
                .size(dimens.spaceL)
                .background(colors.surface, RoundedCornerShape(dimens.radiusS))
                .background(color, RoundedCornerShape(dimens.radiusS))
                .border(dimens.gridHairline, colors.hairline, RoundedCornerShape(dimens.radiusS)),
        )
        Text(name, style = Sendoku.type.label, color = colors.given, modifier = Modifier.width(160.dp))
    }
}

@Preview(name = "Tokens, dark", heightDp = 1400)
@Composable
private fun TokensDark() {
    SendokuTheme(dark = true) { TokenSheet() }
}

@Preview(name = "Tokens, light", heightDp = 1400)
@Composable
private fun TokensLight() {
    SendokuTheme(dark = false) { TokenSheet() }
}
