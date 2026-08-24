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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.nav.Destination
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuIcons

/**
 * The three places the app has.
 *
 * A drawn mark over a word, the same shape as the toolbar under the board, so the app has one
 * way of drawing a thing you tap rather than two. The selected tab says so three times: the
 * mark and the word take the accent colour, a bar appears above it, and the semantics mark it
 * selected, which is what a screen reader reads. Colour alone would leave the state invisible
 * to anybody who cannot see the difference.
 *
 * It shows only on the three roots. A puzzle, a lesson and a practice board are places you are
 * in the middle of something, and a bar offering to leave is furniture in the way.
 */
@Composable
public fun BottomBar(current: Destination, onSelect: (Destination) -> Unit, modifier: Modifier = Modifier) {
    val colors = Sendoku.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .testTag("tabs"),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Tab(
            icon = SendokuIcons.Home,
            label = stringResource(R.string.tab_home),
            selected = current == Destination.Home,
            onClick = { onSelect(Destination.Home) },
            tag = "tab:home",
            modifier = Modifier.weight(1f),
        )
        Tab(
            icon = SendokuIcons.Learn,
            label = stringResource(R.string.tab_learn),
            selected = current == Destination.Course,
            onClick = { onSelect(Destination.Course) },
            tag = "tab:learn",
            modifier = Modifier.weight(1f),
        )
        Tab(
            icon = SendokuIcons.Account,
            label = stringResource(R.string.tab_account),
            selected = current == Destination.Account,
            onClick = { onSelect(Destination.Account) },
            tag = "tab:account",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Tab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val ink = if (selected) colors.accent else colors.muted

    Column(
        modifier = modifier
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .clickable(onClick = onClick)
            .padding(vertical = dimens.spaceS)
            .testTag(tag)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                role = Role.Tab
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Box(
            Modifier
                .size(width = MARKER, height = dimens.gridBoxLine)
                .clip(CircleShape)
                .background(if (selected) colors.accent else Color.Transparent),
        )
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(ICON))
        Text(label, style = Sendoku.type.toolLabel, color = ink)
    }
}

private val ICON = 24.dp
private val MARKER = 20.dp
