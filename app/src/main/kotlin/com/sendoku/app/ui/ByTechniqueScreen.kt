package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.sendoku.app.R
import com.sendoku.app.theme.Sendoku
import com.sendoku.engine.technique.TechniqueId

/**
 * Ask for a puzzle that needs a particular technique.
 *
 * This is the one thing this app can do that no other sudoku app can, and it costs almost
 * nothing to offer: every puzzle in the batch was rated by the technique solver on the way
 * in, so the hardest rule each one turns on is already written down next to it. Asking for
 * every puzzle that needs an X-Wing is a filter over a column, not a search.
 *
 * It is also what turns the course from reading into practice. Somebody who has just been
 * taught the X-Wing can go and find one on a real board, which is a different skill and the
 * one that actually makes a puzzle solvable.
 *
 * Ordered by the ladder rather than by how many there are, because the ladder is the order
 * the course teaches them in and the order they get harder in, and a list sorted by count
 * would open with whatever the batch happened to produce most of.
 */
@Composable
public fun ByTechniqueScreen(
    supply: List<TechniqueSupply>,
    onPlay: (TechniqueId) -> Unit,
    onLearn: (TechniqueId) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            BackButton(onClick = onBack, tag = "technique:back")
            Text(
                text = stringResource(R.string.technique_pick_title),
                style = Sendoku.type.title,
                color = colors.given,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = stringResource(R.string.technique_pick_body),
            style = Sendoku.type.body,
            color = colors.muted,
            modifier = Modifier.padding(horizontal = dimens.spaceM, vertical = dimens.spaceXs),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = dimens.spaceM).testTag("technique:list"),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = dimens.spaceS,
                bottom = dimens.spaceXl,
            ),
        ) {
            items(items = supply, key = { it.technique.name }) { entry ->
                TechniqueRow(entry, onPlay, onLearn)
            }
        }
    }
}

@Composable
private fun TechniqueRow(entry: TechniqueSupply, onPlay: (TechniqueId) -> Unit, onLearn: (TechniqueId) -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val name = stringResource(TechniqueCopy.nameOf(entry.technique))
    val empty = entry.count == 0
    val detail = if (empty) {
        stringResource(R.string.technique_pick_none)
    } else {
        pluralStringResource(R.plurals.technique_pick_count, entry.count, entry.count)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.minTouchTarget)
            .testTag("technique:${entry.technique.name}")
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(colors.surface)
            .clickable(enabled = !empty) { onPlay(entry.technique) }
            // Shown and disabled rather than left out. A technique missing from the list is
            // one a player wonders whether the app knows about; a technique that is here and
            // says why it cannot be played answers the question instead of raising it.
            .alpha(if (empty) 0.55f else 1f)
            .padding(horizontal = dimens.spaceM, vertical = dimens.spaceS)
            .semantics(mergeDescendants = true) {
                if (empty) disabled()
                contentDescription = "$name, $detail"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = Sendoku.type.label, color = colors.given)
            Text(detail, style = Sendoku.type.body, color = colors.muted)
        }
        // The lesson, for anybody who wants to be told what it is before being handed a
        // puzzle that turns on it.
        if (entry.hasLesson) {
            Text(
                text = stringResource(R.string.glossary_learn_this),
                style = Sendoku.type.overline,
                color = colors.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.radiusS))
                    .clickable { onLearn(entry.technique) }
                    .testTag("technique:learn:${entry.technique.name}")
                    .padding(dimens.spaceS),
            )
        }
    }
}

/**
 * One technique in the batch, and how many puzzles turn on it.
 *
 * [count] is how many puzzles need this technique and nothing harder. Zero means the batch
 * has none, which happens at the very bottom of the ladder, where a puzzle needing nothing
 * but naked singles is rare, and at the very top.
 */
public data class TechniqueSupply(val technique: TechniqueId, val count: Int, val hasLesson: Boolean)
