package com.sendoku.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameViewModel
import com.sendoku.app.nav.Destination
import com.sendoku.app.nav.Navigator
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.ui.GameScreen
import com.sendoku.app.ui.GlossaryScreen
import com.sendoku.app.ui.HomeScreen
import com.sendoku.app.ui.HomeState
import com.sendoku.app.ui.InProgressSummary
import com.sendoku.app.ui.SettingsScreen
import com.sendoku.engine.Board
import com.sendoku.engine.Grade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * The whole app, one screen at a time.
 *
 * A single activity with a back stack of typed destinations. Home is always underneath, so
 * the system back gesture always has somewhere to go, and only the playing screen ever
 * intercepts it, to ask before throwing away a half finished puzzle.
 */
@Composable
public fun SendokuApp(
    model: GameViewModel,
    settings: Flow<GameSettings>,
    onSettingsChange: (GameSettings) -> Unit,
    solvedByGrade: Flow<Map<Grade, Int>>,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val navigator = remember { Navigator() }
    val state by model.state.collectAsState()
    val loading by model.loading.collectAsState()
    val counts by solvedByGrade.collectAsState(initial = emptyMap())
    val currentSettings by settings.collectAsState(initial = GameSettings())

    // Back from anywhere except home goes back a screen. Home itself lets the system take it,
    // because the way out of a home screen is out of the app.
    BackHandler(enabled = navigator.canGoBack) { navigator.back() }

    when (val destination = navigator.current) {
        Destination.Home -> {
            HomeScreen(
                state = HomeState(
                    solvedByGrade = counts,
                    inProgress = state?.takeIf { !it.isOver }?.summarise(),
                ),
                onPlay = { grade -> navigator.go(Destination.Play(grade)) },
                onResume = { navigator.go(Destination.Resume) },
                onDaily = { navigator.go(Destination.Daily(todayEpochDay())) },
                onSettings = { navigator.go(Destination.Settings) },
                modifier = modifier,
            )
        }

        is Destination.Play -> {
            LaunchedEffect(destination) { model.startNew(destination.grade) }
            PlayHost(model, loading, navigator, scope, modifier)
        }

        Destination.Resume -> {
            LaunchedEffect(destination) { model.resumeOrStart() }
            PlayHost(model, loading, navigator, scope, modifier)
        }

        is Destination.Daily -> {
            LaunchedEffect(destination) { model.startDaily(destination.epochDay) }
            PlayHost(model, loading, navigator, scope, modifier)
        }

        Destination.Glossary -> {
            GlossaryScreen(onBack = { navigator.back() }, modifier = modifier)
        }

        Destination.Settings -> {
            SettingsScreen(
                settings = currentSettings,
                onChange = onSettingsChange,
                onBack = { navigator.back() },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun PlayHost(
    model: GameViewModel,
    loading: Boolean,
    navigator: Navigator,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val state by model.state.collectAsState()
    val game = state
    if (loading || game == null) {
        Loading(modifier)
        return
    }
    GameScreen(
        state = game,
        onEvent = model::onEvent,
        onNextPuzzle = { scope.launch { model.startNew(game.grade) } },
        onHome = { navigator.home() },
        onGlossary = { navigator.go(Destination.Glossary) },
        modifier = modifier,
    )
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(Sendoku.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Text("Sendoku", style = Sendoku.type.title, color = Sendoku.colors.muted)
    }
}

private fun com.sendoku.app.game.GameState.summarise(): InProgressSummary = InProgressSummary(
    grade = grade,
    placed = cells.count { it.digit != Board.EMPTY },
    total = cellCount,
    elapsed = elapsed,
)

/**
 * Today, as a count of days since 1970.
 *
 * Local rather than UTC on purpose. Somebody in Auckland should get tomorrow's puzzle when
 * it is tomorrow where they are, not when it is tomorrow in London.
 */
internal fun todayEpochDay(): Long = java.time.LocalDate.now().toEpochDay()
