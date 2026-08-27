package com.sendoku.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
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
import com.sendoku.app.data.DataStoreHintLog
import com.sendoku.app.data.DataStoreSettings
import com.sendoku.app.data.RoomGameRepository
import com.sendoku.app.data.SendokuDatabase
import com.sendoku.app.data.ThemeMode
import com.sendoku.app.game.GameViewModel
import com.sendoku.app.learn.BackupResult
import com.sendoku.app.learn.BackupStore
import com.sendoku.app.learn.CourseBackup
import com.sendoku.app.learn.Problem
import com.sendoku.app.learn.RoomLearningRepository
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.app.ui.FirstRunLanguage
import com.sendoku.app.ui.InProgressSummary
import com.sendoku.app.ui.Languages
import com.sendoku.app.ui.languageAnswered
import com.sendoku.engine.catalog.CatalogReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val backups by lazy { BackupStore(database, BuildConfig.VERSION_NAME) }

    /**
     * What the last export or import did.
     *
     * Held here rather than in the screen, because the answer arrives from a file picker that
     * outlives the composition that started it.
     */
    private val dataMessage = MutableStateFlow<String?>(null)

    /*
     * The two file pickers.
     *
     * ACTION_CREATE_DOCUMENT and ACTION_OPEN_DOCUMENT, which hand back a single uri the app may
     * read or write once. No storage permission is involved, which is what keeps the promise
     * that this app asks for nothing: the player picks the file, and the system does the rest.
     */
    private val exportTo = registerForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch { writeBackup(uri) }
    }

    private val importFrom = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch { readBackup(uri) }
    }

    private suspend fun writeBackup(uri: android.net.Uri) {
        val backup = backups.export(System.currentTimeMillis())
        val written = runCatching {
            withContext(Dispatchers.IO) {
                contentResolver.openOutputStream(uri)?.use { it.write(CourseBackup.encode(backup).toByteArray()) }
                    ?: error("nothing to write to")
            }
        }
        dataMessage.value = if (written.isSuccess) {
            getString(R.string.settings_exported, backup.lessons.size, backup.games.size)
        } else {
            getString(R.string.settings_import_failed)
        }
    }

    private suspend fun readBackup(uri: android.net.Uri) {
        val text = runCatching {
            withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("no file")
            }
        }.getOrNull()

        if (text == null) {
            dataMessage.value = getString(R.string.settings_import_failed)
            return
        }

        dataMessage.value = when (val result = CourseBackup.decode(text)) {
            is BackupResult.Unreadable -> when (result.problem) {
                Problem.EMPTY -> getString(R.string.settings_import_empty)
                Problem.NOT_OURS -> getString(R.string.settings_import_not_ours)
                Problem.FROM_THE_FUTURE -> getString(R.string.settings_import_future)
            }

            is BackupResult.Read -> {
                val done = runCatching { backups.import(result.backup, System.currentTimeMillis()) }.getOrNull()
                when {
                    done == null -> getString(R.string.settings_import_failed)

                    done.ignored > 0 ->
                        getString(R.string.settings_imported_ignored, done.lessons, done.games, done.ignored)

                    else -> getString(R.string.settings_imported, done.lessons, done.games)
                }
            }
        }
    }
    private val settings by lazy { DataStoreSettings(preferences) }
    private val hints by lazy { DataStoreHintLog(preferences) }

    /**
     * Whether the language question has been answered, or does not need asking.
     *
     * The flag says whether this player answered it. The rest says whether they were ever
     * going to be asked: somebody with a saved game, a finished game or a lesson in progress
     * has been using the app since before this screen existed, and stopping them on an
     * update to ask a question they answered by playing is an update that feels broken.
     */
    private val firstRunAnswered by lazy {
        combine(
            settings.languageAsked,
            repository.watchInProgress(),
            repository.history(),
            learning.progress(),
        ) { asked, inProgress, finished, course ->
            languageAnswered(
                asked = asked == true,
                hasGame = inProgress != null,
                hasHistory = finished.isNotEmpty(),
                hasLessons = course.lessons.isNotEmpty(),
            )
        }
    }

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

    /**
     * The chosen language, applied before a single string is read.
     *
     * Below Android 13 this is the only place it can happen: there is no per app language in
     * the system, so the app carries its own and puts it on the context the activity is built
     * from. On 13 and later the system has already done it and this hands back what it was
     * given.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Languages.wrap(newBase))
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
                    // Ask which language, once, before anything else. Null means the
                    // answer is still being read off disk, and the first frame waits
                    // rather than showing the home screen and replacing it a moment
                    // later with a question.
                    val asked by firstRunAnswered.collectAsState(initial = null)
                    if (asked == null) {
                        Box(Modifier.fillMaxSize().background(Sendoku.colors.background))
                        return@Scaffold
                    }
                    if (asked == false) {
                        FirstRunLanguage(
                            onChoose = { language ->
                                // The flag first, and awaited: choosing a language restarts
                                // the activity, and a write left running in a scope that is
                                // about to be cancelled is a question asked twice.
                                lifecycleScope.launch {
                                    settings.markLanguageAsked()
                                    Languages.choose(this@MainActivity, language)
                                }
                            },
                            modifier = Modifier.fillMaxSize().padding(insets),
                        )
                        return@Scaffold
                    }
                    SendokuApp(
                        model = model,
                        settings = settings.settings,
                        onSettingsChange = { changed ->
                            lifecycleScope.launch { settings.update { changed } }
                        },
                        solvedByGrade = repository.solvedByGrade(),
                        statistics = repository.statistics(),
                        history = repository.history(),
                        dailyDays = repository.dailyDays(),
                        course = learning.progress(),
                        onExport = {
                            dataMessage.value = null
                            exportTo.launch(BACKUP_NAME)
                        },
                        onImport = {
                            dataMessage.value = null
                            importFrom.launch(arrayOf(BACKUP_MIME, "application/octet-stream", "text/plain"))
                        },
                        onResetCourse = {
                            lifecycleScope.launch {
                                learning.clear()
                                dataMessage.value = getString(R.string.settings_course_reset)
                            }
                        },
                        dataMessage = dataMessage.collectAsState().value,
                        onPractice = { technique, correct ->
                            lifecycleScope.launch {
                                learning.recordPractice(technique, correct, System.currentTimeMillis())
                            }
                        },
                        // Read fresh each time rather than held, since a practice search walks
                        // the batch and holding an inflated copy of it is a megabyte for nothing.
                        puzzles = {
                            val reader = CatalogReader.from(
                                checkNotNull(javaClass.getResourceAsStream("/catalog/classic.sdkb")),
                            )
                            (0 until reader.size).asSequence().map { reader.puzzleAt(it) }
                        },
                        onLessonStep = { lesson, step, finished ->
                            lifecycleScope.launch {
                                learning.record(lesson, step, finished, System.currentTimeMillis())
                            }
                        },
                        appearance = settings.appearance,
                        onAppearanceChange = { changed ->
                            lifecycleScope.launch { settings.updateAppearance { changed } }
                        },
                        onResetStats = {
                            lifecycleScope.launch {
                                repository.clearHistory()
                                hints.clear()
                            }
                        },
                        onSpendHint = { technique, level ->
                            lifecycleScope.launch { hints.record(technique, level) }
                        },
                        hintLog = hints.log,
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

/** What an exported file is called and what it is. */
private const val BACKUP_NAME = "sendoku-progress.json"
private const val BACKUP_MIME = "application/json"
