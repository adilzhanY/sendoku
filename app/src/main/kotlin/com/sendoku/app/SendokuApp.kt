package com.sendoku.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameViewModel
import com.sendoku.app.nav.Destination
import com.sendoku.app.nav.Navigator
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.ui.GameScreen
import com.sendoku.app.data.Statistics
import com.sendoku.app.ui.GlossaryScreen
import com.sendoku.app.ui.StatsScreen
import com.sendoku.app.ui.HomeScreen
import com.sendoku.app.ui.HomeState
import com.sendoku.app.ui.InProgressSummary
import com.sendoku.app.ui.SettingsScreen
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
    savedGame: Flow<InProgressSummary?>,
    statistics: Flow<Statistics>,
    onResetStats: () -> Unit,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val navigator = rememberSaveable(saver = Navigator.Saver) { Navigator() }
    val loading by model.loading.collectAsState()
    val counts by solvedByGrade.collectAsState(initial = emptyMap())
    val saved by savedGame.collectAsState(initial = null)
    val stats by statistics.collectAsState(initial = Statistics.of(emptyList()))
    val currentSettings by settings.collectAsState(initial = GameSettings())

    // Back from anywhere except home goes back a screen. Home itself lets the system take it,
    // because the way out of a home screen is out of the app.
    BackHandler(enabled = navigator.canGoBack) { navigator.back() }

    when (val destination = navigator.current) {
        Destination.Home -> {
            HomeScreen(
                state = HomeState(
                    solvedByGrade = counts,
                    // Read from storage, not from the live game. On a cold start there is no
                    // live game yet, and the home screen would say there is nothing to resume.
                    inProgress = saved,
                ),
                // Starting a game is an effect of navigating, not of drawing. Doing it in a
                // LaunchedEffect on the play screen meant that coming back from the glossary
                // recomposed it and silently dealt a brand new puzzle.
                onPlay = { grade ->
                    model.startNew(grade)
                    navigator.go(Destination.Play(grade))
                },
                onResume = {
                    model.resumeOrStart()
                    navigator.go(Destination.Resume)
                },
                onDaily = {
                    model.startDaily(todayEpochDay())
                    navigator.go(Destination.Daily(todayEpochDay()))
                },
                onSettings = { navigator.go(Destination.Settings) },
                onStats = { navigator.go(Destination.Stats) },
                modifier = modifier,
            )
        }

        is Destination.Play, Destination.Resume, is Destination.Daily ->
            PlayHost(model, loading, navigator, scope, modifier)

        Destination.Stats -> {
            StatsScreen(
                statistics = stats,
                onBack = { navigator.back() },
                onReset = onResetStats,
                modifier = modifier,
            )
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

/**
 * Today, as a count of days since 1970.
 *
 * Local rather than UTC on purpose. Somebody in Auckland should get tomorrow's puzzle when
 * it is tomorrow where they are, not when it is tomorrow in London.
 */
internal fun todayEpochDay(): Long = java.time.LocalDate.now().toEpochDay()
