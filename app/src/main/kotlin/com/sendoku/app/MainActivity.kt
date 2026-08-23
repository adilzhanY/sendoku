package com.sendoku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.app.ui.GameEvent
import com.sendoku.app.ui.GameScreen
import com.sendoku.app.ui.reduce
import com.sendoku.engine.Grade
import com.sendoku.engine.catalog.CatalogReader
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One screen, for now.
 *
 * The game state lives in a `remember` rather than a ViewModel, and the puzzle is picked at
 * random on every launch. Both are temporary: saving a game, restoring it after the process
 * dies, and navigating between screens are their own pieces of work and are next. What is
 * here is enough to actually play a puzzle end to end.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SendokuTheme {
                androidx.compose.material3.Scaffold(
                    containerColor = Sendoku.colors.background,
                    // Draw behind the bars, but keep every control clear of them and of any
                    // camera cutout. safeDrawing is the one that covers both, and a board
                    // with a corner under a cutout is a board with an unreachable cell.
                    contentWindowInsets = WindowInsets.safeDrawing,
                    modifier = Modifier.fillMaxSize(),
                ) { insets ->
                    Game(modifier = Modifier.padding(insets))
                }
            }
        }
    }
}

@Composable
private fun Game(modifier: Modifier = Modifier) {
    var state by remember { mutableStateOf<GameState?>(null) }

    // Reading the batch inflates a hundred and fifty kilobytes, which is not something to do
    // on the frame that draws the first screen.
    LaunchedEffect(Unit) {
        state = withContext(Dispatchers.IO) { loadGame(Grade.STEADY) }
    }

    // A puzzle should not keep ticking in the app switcher.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) state = state?.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    val game = state
    if (game == null) {
        Box(
            modifier = modifier.fillMaxSize().background(Sendoku.colors.background),
            contentAlignment = Alignment.Center,
        ) {
            Text("Sendoku", style = Sendoku.type.title, color = Sendoku.colors.muted)
        }
        return
    }

    GameScreen(
        state = game,
        onEvent = { event -> state = game.reduce(event) },
        // Both of these get somewhere to go once there is more than one screen.
        onNextPuzzle = { state = loadGame(game.grade) },
        onHome = { state = loadGame(game.grade) },
        modifier = modifier,
    )
}

/** Picks a puzzle out of the batch that ships inside the app. */
private fun loadGame(grade: Grade): GameState {
    val reader = checkNotNull(GameState::class.java.getResourceAsStream("/catalog/classic.sdkb")) {
        "the puzzle batch is missing from the app"
    }.use { CatalogReader.from(it) }

    val indices = reader.indicesOf(grade)
    val index = indices[Random.nextInt(indices.size)]
    return GameState.start(reader.puzzleAt(index))
}
