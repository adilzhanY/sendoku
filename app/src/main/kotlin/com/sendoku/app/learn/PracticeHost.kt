package com.sendoku.app.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.sendoku.app.R
import com.sendoku.app.theme.Sendoku
import com.sendoku.engine.Dimensions
import com.sendoku.engine.catalog.RatedPuzzle
import com.sendoku.engine.technique.TechniqueId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Finds an exercise and hands it to the screen.
 *
 * The search walks real puzzles and can take a moment for the rarer techniques, so it happens
 * off the main thread and the screen says it is looking rather than freezing. Restarted by
 * bumping a counter, which is what "another" does.
 */
@Composable
public fun PracticeHost(
    technique: TechniqueId?,
    puzzles: () -> Sequence<RatedPuzzle>,
    onAnswer: (TechniqueId, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (technique == null) {
        // With a way out. An empty state that cannot be left is a trap, and this one is
        // reachable on the player's very first visit to the course.
        Column(modifier.fillMaxSize().background(Sendoku.colors.background)) {
            Text(
                text = stringResource(R.string.back),
                style = Sendoku.type.overline,
                color = Sendoku.colors.muted,
                modifier = Modifier
                    .padding(Sendoku.dimens.spaceM)
                    .clip(RoundedCornerShape(Sendoku.dimens.radiusS))
                    .clickable(onClick = onBack)
                    .padding(Sendoku.dimens.spaceS)
                    .testTag("practice:back"),
            )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.practice_none),
                    style = Sendoku.type.body,
                    color = Sendoku.colors.muted,
                    modifier = Modifier.padding(Sendoku.dimens.spaceXl),
                )
            }
        }
        return
    }

    var round by remember(technique) { mutableIntStateOf(0) }
    var exercise by remember(technique) { mutableStateOf<Exercise?>(null) }

    LaunchedEffect(technique, round) {
        exercise = null
        exercise = withContext(Dispatchers.Default) {
            // A different starting point each round, so "another" is another rather than the
            // same board with the answer already known.
            PracticePositions.find(technique, puzzles().drop(round * SKIP), Dimensions.CLASSIC)
        }
    }

    PracticeScreen(
        exercise = exercise,
        onAnswer = { correct -> onAnswer(technique, correct) },
        onNext = { round++ },
        onBack = onBack,
        modifier = modifier,
    )
}

/** How far along the catalog each new round starts. Enough to be a different grid. */
private const val SKIP = 3
