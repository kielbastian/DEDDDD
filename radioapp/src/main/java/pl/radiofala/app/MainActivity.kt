package pl.radiofala.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.radiofala.app.ui.CategoryTabs
import pl.radiofala.app.ui.FullPlayerScreen
import pl.radiofala.app.ui.MiniPlayerBar
import pl.radiofala.app.ui.RadioFalaTheme
import pl.radiofala.app.ui.RadioSearchScreen
import pl.radiofala.app.ui.StationListScreen

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            RadioFalaTheme {
                RadioFalaApp()
            }
        }
    }

    // Cała aplikacja jest immersyjna – chowa pasek nawigacyjny i status bar telefonu.
    // System czasem przywraca paski (np. po powrocie z tła), więc chowamy je ponownie
    // za każdym razem, gdy okno odzyskuje fokus.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadioFalaApp(viewModel: RadioViewModel = viewModel()) {
    var searchMode by remember { mutableStateOf(false) }
    var playerExpanded by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = searchMode) { searchMode = false }

    val categories = viewModel.categories
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val stationsState by viewModel.stationsState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val playback by viewModel.playback.collectAsState()
    val sleepMinutes by viewModel.sleepTimerMinutes.collectAsState()
    val canSkip by viewModel.canSkip.collectAsState()
    val currentStation by viewModel.currentStation.collectAsState()

    // Mini-odtwarzacz zajmuje ok. 1/4 wysokości ekranu.
    val miniPlayerHeight = (LocalConfiguration.current.screenHeightDp / 4).dp

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Radio,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
                            Text("Radio Fala", style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    navigationIcon = {
                        if (searchMode) {
                            IconButton(onClick = { searchMode = false }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Wróć")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { searchMode = !searchMode }) {
                            Icon(Icons.Filled.Search, contentDescription = "Szukaj stacji")
                        }
                    }
                )
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = playback.currentMediaId != null && !playerExpanded,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    MiniPlayerBar(
                        playback = playback,
                        faviconUrl = currentStation?.faviconUrl,
                        canSkip = canSkip,
                        height = miniPlayerHeight,
                        onExpand = { playerExpanded = true },
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onPrevious = viewModel::playPrevious,
                        onNext = viewModel::playNext
                    )
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (searchMode) {
                    RadioSearchScreen(
                        viewModel = viewModel,
                        state = searchState,
                        favoriteIds = favoriteIds,
                        currentMediaId = playback.currentMediaId,
                        isPlaying = playback.isPlaying,
                        padding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    )
                } else {
                    CategoryTabs(
                        categories = categories,
                        selected = selectedCategory,
                        onSelect = viewModel::selectCategory
                    )
                    StationListScreen(
                        state = stationsState,
                        favoriteIds = favoriteIds,
                        currentMediaId = playback.currentMediaId,
                        isPlaying = playback.isPlaying,
                        onPlay = { station -> viewModel.play(station, stationsState.stations) },
                        onToggleFavorite = viewModel::toggleFavorite,
                        padding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = playerExpanded && playback.currentMediaId != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            FullPlayerScreen(
                playback = playback,
                faviconUrl = currentStation?.faviconUrl,
                isFavorite = currentStation?.stationUuid?.let { it in favoriteIds } ?: false,
                sleepMinutesActive = sleepMinutes,
                canSkip = canSkip,
                onToggleFavorite = {
                    currentStation?.let { viewModel.toggleFavorite(it) }
                },
                onTogglePlayPause = viewModel::togglePlayPause,
                onPrevious = viewModel::playPrevious,
                onNext = viewModel::playNext,
                onStop = {
                    viewModel.stopPlayback()
                    playerExpanded = false
                },
                onSetSleepTimer = viewModel::setSleepTimer,
                onCancelSleepTimer = viewModel::cancelSleepTimer,
                onCollapse = { playerExpanded = false }
            )
        }
    }
}
