package pl.programtv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.programtv.app.ui.ChannelsScreen
import pl.programtv.app.ui.NowScreen
import pl.programtv.app.ui.ScheduleScreen
import pl.programtv.app.ui.SearchScreen
import pl.programtv.app.ui.formatShortDateTime

private enum class Tab(val title: String, val icon: ImageVector) {
    NOW("Teraz", Icons.Filled.LiveTv),
    SCHEDULE("Program", Icons.Filled.CalendarMonth),
    SEARCH("Szukaj", Icons.Filled.Search),
    CHANNELS("Kanały", Icons.Filled.Tune)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = colors) {
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
        topBar = {
            TopAppBar(
                title = { Text("Program TV") },
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
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (currentTab) {
            Tab.NOW -> NowScreen(viewModel, padding)
            Tab.SCHEDULE -> ScheduleScreen(viewModel, padding)
            Tab.SEARCH -> SearchScreen(viewModel, padding)
            Tab.CHANNELS -> ChannelsScreen(viewModel, padding)
        }
    }

    if (showSettings) {
        SettingsDialog(
            currentUrl = viewModel.epgUrl,
            lastUpdateMillis = refreshState.lastUpdateMillis,
            onSave = { url ->
                viewModel.setEpgUrl(url)
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
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Źródło programu TV") },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(
                    "Adres pliku XMLTV z programem TV. Domyślne źródło zawiera " +
                        "polskie kanały; możesz podać własny adres.",
                    style = MaterialTheme.typography.bodySmall
                )
                androidx.compose.foundation.layout.Spacer(
                    Modifier.padding(vertical = 6.dp)
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Adres URL (XMLTV)") },
                    singleLine = true
                )
                if (lastUpdateMillis > 0) {
                    androidx.compose.foundation.layout.Spacer(
                        Modifier.padding(vertical = 6.dp)
                    )
                    Text(
                        "Ostatnia aktualizacja: ${formatShortDateTime(lastUpdateMillis)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(url.trim()) },
                enabled = url.trim().startsWith("http")
            ) { Text("Zapisz i odśwież") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}
