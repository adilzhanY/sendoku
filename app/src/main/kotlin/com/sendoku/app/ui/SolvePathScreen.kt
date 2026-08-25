package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.game.SolvePath
import com.sendoku.app.theme.Sendoku

/**
 * How the puzzle was solved, from the first digit to the last.
 *
 * Only ever shown after a game is over, so it can hold nothing back. Every step the engine
 * took, in order, named and placed. It is the most direct answer to the question a beaten
 * player is actually asking, which is not "what was the answer" but "what was I supposed to
 * have seen".
 *
 * The steps are the engine's own path, not a reconstruction. That matters: it is the same
 * path that rated the puzzle, so the hard step in the middle of it is exactly the step the
 * grade on the home screen is named after.
 */
@Composable
public fun SolvePathScreen(path: SolvePath, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            Text(
                text = stringResource(R.string.back),
                style = Sendoku.type.overline,
                color = colors.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.radiusS))
                    .clickable(onClick = onBack)
                    .padding(dimens.spaceS)
                    .testTag("path:back"),
            )
            Text(
                text = stringResource(R.string.path_title),
                style = Sendoku.type.title,
                color = colors.given,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = pluralStringResource(R.plurals.path_summary, path.steps.size, path.steps.size),
            style = Sendoku.type.body,
            color = colors.muted,
            modifier = Modifier.padding(horizontal = dimens.spaceM, vertical = dimens.spaceXs),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = dimens.spaceM)
                .testTag("path:list"),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = dimens.spaceS),
        ) {
            items(path.steps) { step -> PathRow(step) }
        }
    }
}

@Composable
private fun PathRow(step: SolvePath.Step) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    // The hard steps are the ones worth finding again, so they carry the same red the
    // advanced levels use. A list of two hundred lines with nothing marked is a wall.
    val ink = if (step.advanced) colors.conflict else colors.muted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(colors.surface)
            .padding(horizontal = dimens.spaceM, vertical = dimens.spaceS),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = step.number.toString(),
            style = Sendoku.type.statLabel,
            color = colors.muted,
            modifier = Modifier.width(28.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(stringResource(TechniqueCopy.nameOf(step.technique)), style = Sendoku.type.label, color = ink)
            Text(outcomeOf(step), style = Sendoku.type.body, color = colors.muted)
        }
    }
}

@Composable
private fun outcomeOf(step: SolvePath.Step): String {
    val place = step.placement
    return if (place != null) {
        stringResource(R.string.path_placed, place.cell / step.size + 1, place.cell % step.size + 1, place.digit)
    } else {
        pluralStringResource(R.plurals.path_struck, step.struck, step.struck)
    }
}
