package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.sendoku.app.R
import com.sendoku.app.learn.Curriculum
import com.sendoku.app.theme.Sendoku
import com.sendoku.engine.Grade
import com.sendoku.engine.technique.TechniqueId

/**
 * Every technique the app knows, easiest first.
 *
 * Reachable from a hint, so somebody who has just been told about an X-Wing can go and read
 * what one is without losing their place. It is also, quietly, the honest statement of what
 * this app can do that others cannot: twenty four techniques, ending somewhere most apps
 * never go.
 */
@Composable
public fun GlossaryScreen(
    onBack: () -> Unit,
    onLesson: (TechniqueId) -> Unit,
    modifier: Modifier = Modifier,
    /** Deals a puzzle that turns on this technique, when the batch has one. */
    onPlay: (TechniqueId) -> Unit = {},
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val ladder = TechniqueId.entries.sortedBy { it.cost }

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            BackButton(onClick = onBack)
            Text(stringResource(R.string.glossary_title), style = Sendoku.type.title, color = colors.given)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = dimens.spaceM),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = dimens.spaceXl),
        ) {
            items(ladder) { technique ->
                val lesson = Curriculum.teaching(technique)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimens.radiusM))
                        .background(colors.surface)
                        .then(
                            // The glossary says what a technique is. The lesson shows one on a
                            // board and asks you to find the next. A definition is where you
                            // start looking, not where you should have to stop.
                            if (lesson == null) Modifier else Modifier.clickable { onLesson(technique) },
                        )
                        .padding(dimens.spaceM),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(TechniqueCopy.nameOf(technique)),
                            style = Sendoku.type.label,
                            color = colors.given,
                        )
                        Text(
                            text = stringResource(gradeName(Grade.of(technique.cost))),
                            style = Sendoku.type.overline,
                            color = colors.accent,
                        )
                    }
                    if (lesson != null) {
                        Text(
                            text = stringResource(R.string.glossary_learn_this),
                            style = Sendoku.type.overline,
                            color = colors.accent,
                        )
                    }
                    // The other half of knowing a technique: finding one on a real board.
                    // The glossary is where somebody looks a rule up, which is exactly the
                    // moment they would like a puzzle to try it on.
                    Text(
                        text = stringResource(R.string.technique_pick_play),
                        style = Sendoku.type.overline,
                        color = colors.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(dimens.radiusS))
                            .clickable { onPlay(technique) }
                            .testTag("glossary:play:${'$'}{technique.name}")
                            .padding(vertical = dimens.spaceXs),
                    )
                    Text(
                        text = stringResource(TechniqueCopy.lookFor(technique)),
                        style = Sendoku.type.body,
                        color = colors.given,
                    )
                    Text(
                        text = stringResource(TechniqueCopy.because(technique)),
                        style = Sendoku.type.body,
                        color = colors.muted,
                    )
                }
            }
        }
    }
}
