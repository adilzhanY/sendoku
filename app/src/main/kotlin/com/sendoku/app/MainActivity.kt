package com.sendoku.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.room.Room
import com.sendoku.app.data.CatalogPuzzleSource
import com.sendoku.app.data.DataStoreSettings
import com.sendoku.app.data.RoomGameRepository
import com.sendoku.app.data.SendokuDatabase
import com.sendoku.app.game.GameViewModel
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuTheme
import kotlinx.coroutines.launch

private val Context.preferences: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * The one activity.
 *
 * Wiring is done by hand rather than by an injection framework. There are four things to
 * build and they are built once, so a container would be more code than it replaced. If
 * that stops being true, this is the single place that has to change.
 */
class MainActivity : ComponentActivity() {

    private lateinit var model: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // Holds the system splash until the first game is ready, so the app never shows an
        // empty board on the way in.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = Room.databaseBuilder(applicationContext, SendokuDatabase::class.java, SendokuDatabase.NAME)
            .build()
        val repository = RoomGameRepository(database.inProgress(), database.finished())
        val settings = DataStoreSettings(preferences)
        model = GameViewModel(
            repository = repository,
            settingsStore = settings,
            puzzles = CatalogPuzzleSource.fromResources(),
            scope = lifecycleScope,
        )

        splash.setKeepOnScreenCondition { false }

        setContent {
            SendokuTheme {
                Scaffold(
                    containerColor = Sendoku.colors.background,
                    // Draw behind the bars, but keep every control clear of them and of any
                    // camera cutout. A board with a corner under a cutout has an unreachable
                    // cell in it.
                    contentWindowInsets = WindowInsets.safeDrawing,
                    modifier = Modifier.fillMaxSize(),
                ) { insets ->
                    SendokuApp(
                        model = model,
                        settings = settings.settings,
                        onSettingsChange = { changed ->
                            lifecycleScope.launch { settings.update { changed } }
                        },
                        solvedByGrade = repository.solvedByGrade(),
                        scope = lifecycleScope,
                        modifier = Modifier.padding(insets),
                    )
                }
            }
        }

        // Stop the clock when the app goes away, and write the game down while we still can.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    kotlinx.coroutines.awaitCancellation()
                } finally {
                    model.pause()
                }
            }
        }
    }
}
