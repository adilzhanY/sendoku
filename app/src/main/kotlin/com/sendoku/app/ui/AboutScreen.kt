package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.sendoku.app.theme.Sendoku

/**
 * What this app is, and what it will not do.
 *
 * The promises are written down here because they are the reason the app exists, and
 * because a promise nobody can find is not much of a promise.
 */
@Composable
public fun AboutScreen(
    version: String,
    onBack: () -> Unit,
    onLicences: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            Text(
                text = "BACK",
                style = Sendoku.type.overline,
                color = colors.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.radiusS))
                    .clickable(onClick = onBack)
                    .padding(dimens.spaceS),
            )
            Text("About", style = Sendoku.type.title, color = colors.given)
        }

        Text("Sendoku $version", style = Sendoku.type.display, color = colors.given)

        Text(
            text = "A sudoku app for people who have run out of hard sudoku.",
            style = Sendoku.type.body,
            color = colors.given,
        )

        Promise("No advertisements", "Not between puzzles, not for hints, not ever.")
        Promise("No tracking", "Nothing is measured and nothing is sent. There is no server.")
        Promise("No purchases", "Every puzzle and every hint is here already.")
        Promise("Works offline", "The whole batch ships inside the app.")
        Promise(
            "Hints that teach",
            "A hint names the technique and explains the reasoning. It never just fills in " +
                "a digit, and it is never held back.",
        )

        Text(
            text = "Difficulty is rated by the hardest technique a puzzle actually needs, " +
                "worked out by solving it the way a person would. That is why a Severe here " +
                "means the same thing as the last Severe.",
            style = Sendoku.type.body,
            color = colors.muted,
        )

        Text(
            text = "OPEN SOURCE LICENCES",
            style = Sendoku.type.overline,
            color = colors.accent,
            modifier = Modifier
                .padding(top = dimens.spaceL)
                .clip(RoundedCornerShape(dimens.radiusS))
                .clickable(onClick = onLicences)
                .padding(dimens.spaceS),
        )
    }
}

@Composable
private fun Promise(title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Sendoku.dimens.spaceXs)) {
        Text(title, style = Sendoku.type.label, color = Sendoku.colors.accent)
        Text(detail, style = Sendoku.type.body, color = Sendoku.colors.muted)
    }
}
