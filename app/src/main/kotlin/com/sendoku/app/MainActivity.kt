package com.sendoku.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.room.Room
import com.sendoku.app.data.Appearance
import com.sendoku.app.data.CatalogPuzzleSource
import com.sendoku.app.data.DataStoreSettings
import com.sendoku.app.data.RoomGameRepository
import com.sendoku.app.data.SendokuDatabase
import com.sendoku.app.data.ThemeMode
import com.sendoku.app.game.GameViewModel
import com.sendoku.app.learn.RoomLearningRepository
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.app.ui.InProgressSummary
import kotlinx.coroutines.flow.map
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

    private val model: GameViewModel by viewModels {
        // Built once and retained across a rotation, which is the point of a view model and
        // is what stops turning the phone from losing the game.
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GameViewModel(repository, settings, CatalogPuzzleSource.fromResources()) as T
        }
    }

    private val database by lazy {
        Room.databaseBuilder(applicationContext, SendokuDatabase::class.java, SendokuDatabase.NAME)
            .addMigrations(*SendokuDatabase.MIGRATIONS)
            .build()
    }
    private val repository by lazy { RoomGameRepository(database.inProgress(), database.finished()) }
    private val learning by lazy { RoomLearningRepository(database.lessonProgress(), database.mastery()) }
    private val settings by lazy { DataStoreSettings(preferences) }

    /**
     * Built once, not per frame.
     *
     * Mapping the flow inside the composable rebuilt it on every recomposition, which
     * restarts the collection and throws away whatever it had.
     */
    private val savedSummary by lazy {
        repository.watchInProgress().map { saved ->
            saved?.let {
                InProgressSummary(
                    grade = it.grade,
                    placed = it.placed,
                    total = it.total,
                    elapsed = it.elapsed,
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Holds the system splash until the first game is ready, so the app never shows an
        // empty board on the way in.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splash.setKeepOnScreenCondition { false }

        setContent {
            val look by settings.appearance.collectAsState(initial = Appearance())
            SendokuTheme(
                themeId = look.theme,
                dark = when (look.mode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
            ) {
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
                        statistics = repository.statistics(),
                        dailyDays = repository.dailyDays(),
                        course = learning.progress(),
                        onLessonStep = { lesson, step, finished ->
                            lifecycleScope.launch {
                                learning.record(lesson, step, finished, System.currentTimeMillis())
                            }
                        },
                        appearance = settings.appearance,
                        onAppearanceChange = { changed ->
                            lifecycleScope.launch { settings.updateAppearance { changed } }
                        },
                        onResetStats = { lifecycleScope.launch { repository.clearHistory() } },
                        version = BuildConfig.VERSION_NAME,
                        savedGame = savedSummary,
                        scope = lifecycleScope,
                        // Test tags become view ids, which is the only way UI Automator, and
                        // therefore the benchmark module, can find a Compose node reliably.
                        // Matching on the accessibility text instead would break the moment
                        // somebody runs the app in Russian.
                        modifier = Modifier
                            .padding(insets)
                            .semantics { testTagsAsResourceId = true },
                    )
                }
            }
        }

        // Stop the clock when the app goes away, and write the game down while we still can.
        //
        // A rotation stops and restarts the activity too, and a player who turns the phone
        // sideways has not walked away from the puzzle. So the clock only stops if it was
        // running, and it starts again by itself when the screen comes back. A pause the
        // player asked for, by tapping the clock, is left alone.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.onForeground()
                try {
                    kotlinx.coroutines.awaitCancellation()
                } finally {
                    model.onBackground()
                }
            }
        }
    }
}
