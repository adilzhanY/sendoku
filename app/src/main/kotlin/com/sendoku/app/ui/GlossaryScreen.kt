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
    modifier: Modifier = Modifier,
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
            Text(
                text = "BACK",
                style = Sendoku.type.overline,
                color = colors.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.radiusS))
                    .clickable(onClick = onBack)
                    .padding(dimens.spaceS),
            )
            Text("Techniques", style = Sendoku.type.title, color = colors.given)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = dimens.spaceM),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = dimens.spaceXl),
        ) {
            items(ladder) { technique ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimens.radiusM))
                        .background(colors.surface)
                        .padding(dimens.spaceM),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(technique.displayName, style = Sendoku.type.label, color = colors.given)
                        Text(
                            text = Grade.of(technique.cost).displayName.uppercase(),
                            style = Sendoku.type.overline,
                            color = colors.accent,
                        )
                    }
                    Text(
                        text = TechniqueCopy.lookFor(technique),
                        style = Sendoku.type.body,
                        color = colors.given,
                    )
                    Text(
                        text = TechniqueCopy.because(technique),
                        style = Sendoku.type.body,
                        color = colors.muted,
                    )
                }
            }
        }
    }
}
