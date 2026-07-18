package pl.programtv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.programtv.app.ui.ChannelsScreen
import pl.programtv.app.ui.MoviesScreen
import pl.programtv.app.ui.NowScreen
import pl.programtv.app.ui.ProgramTvTheme
import pl.programtv.app.ui.ScheduleScreen
import pl.programtv.app.ui.SearchScreen
import pl.programtv.app.ui.formatShortDateTime

private enum class Tab(val title: String, val icon: ImageVector) {
    NOW("Teraz", Icons.Filled.LiveTv),
    SCHEDULE("Program", Icons.Filled.CalendarMonth),
    MOVIES("Filmy", Icons.Filled.Movie),
    SEARCH("Szukaj", Icons.Filled.Search),
    CHANNELS("Kanały", Icons.Filled.Tune)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ProgramTvTheme {
                ProgramTvApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramTvApp(viewModel: AppViewModel = viewModel()) {
    var currentTab by rememberSaveable { mutableStateOf(Tab.NOW) }
    var showSettings by remember { mutableStateOf(false) }
    val refreshState by viewModel.refreshState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(refreshState.error) {
        refreshState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Tv,
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(6.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text(
                                "Program TV",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                currentTab.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    if (refreshState.refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(horizontal = 12.dp).size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Odśwież program TV")
                        }
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ustawienia")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (currentTab) {
            Tab.NOW -> NowScreen(viewModel, padding)
            Tab.SCHEDULE -> ScheduleScreen(viewModel, padding)
            Tab.MOVIES -> MoviesScreen(viewModel, padding)
            Tab.SEARCH -> SearchScreen(viewModel, padding)
            Tab.CHANNELS -> ChannelsScreen(viewModel, padding)
        }
    }

    if (showSettings) {
        SettingsDialog(
            currentUrl = viewModel.epgUrl,
            lastUpdateMillis = refreshState.lastUpdateMillis,
            polishOnly = viewModel.polishOnly,
            onSave = { url, polishOnly ->
                viewModel.setEpgUrl(url)
                viewModel.setPolishOnly(polishOnly)
                showSettings = false
                viewModel.refresh()
            },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun SettingsDialog(
    currentUrl: String,
    lastUpdateMillis: Long,
    polishOnly: Boolean,
    onSave: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }
    var polishOnlyChecked by remember { mutableStateOf(polishOnly) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Źródło programu TV") },
        text = {
            Column {
                Text(
                    "Adres pliku XMLTV z programem TV. Domyślne źródło zawiera " +
                        "polskie kanały; możesz podać własny adres.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.padding(vertical = 6.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Adres URL (XMLTV)") },
                    singleLine = true
                )
                Spacer(Modifier.padding(vertical = 8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Tylko polskie kanały",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Ukrywa stacje radiowe oraz kanały niepolskojęzyczne.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = polishOnlyChecked,
                        onCheckedChange = { polishOnlyChecked = it }
                    )
                }
                if (lastUpdateMillis > 0) {
                    Spacer(Modifier.padding(vertical = 6.dp))
                    Text(
                        "Ostatnia aktualizacja: ${formatShortDateTime(lastUpdateMillis)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(url.trim(), polishOnlyChecked) },
                enabled = url.trim().startsWith("http")
            ) { Text("Zapisz i odśwież") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}
