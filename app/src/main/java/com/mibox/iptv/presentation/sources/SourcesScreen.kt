package com.mibox.iptv.presentation.sources

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Text

private enum class SourceType { M3U, XTREAM }

/**
 * Ekran konfiguracji źródła (pierwsze uruchomienie). Sterowany D-Padem:
 * przełącznik typu (M3U / Xtream), pola tekstowe z klawiaturą ekranową,
 * przycisk zapisu uruchamiający synchronizację z paskiem postępu.
 */
@Composable
fun SourcesScreen(
    onSynced: () -> Unit,
    viewModel: SourcesViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var type by remember { mutableStateOf(SourceType.M3U) }

    var name by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Dodaj źródło", fontSizeSp = 28)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { type = SourceType.M3U }) { Text("M3U / M3U8") }
            Button(onClick = { type = SourceType.XTREAM }) { Text("Xtream Codes") }
        }
        Text(
            text = if (type == SourceType.M3U) "Wybrano: M3U" else "Wybrano: Xtream",
            color = Color(0xFF3DA9FC),
        )
        Spacer(Modifier.height(8.dp))

        LabeledField("Nazwa", name) { name = it }

        if (type == SourceType.M3U) {
            LabeledField("URL playlisty (http/https)", m3uUrl) { m3uUrl = it }
            LabeledField("URL EPG XMLTV (opcjonalnie)", epgUrl) { epgUrl = it }
        } else {
            LabeledField("Host (np. http://serwer:8080)", host) { host = it }
            LabeledField("Użytkownik", user) { user = it }
            LabeledField("Hasło", pass, password = true) { pass = it }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                when (type) {
                    SourceType.M3U -> viewModel.addM3u(name, m3uUrl, epgUrl)
                    SourceType.XTREAM -> viewModel.addXtream(name, host, user, pass)
                }
            },
        ) { Text(if (ui.syncing) "Synchronizacja…" else "Zapisz i synchronizuj") }

        if (ui.statusText.isNotBlank()) {
            Text(text = ui.statusText, color = Color(0xFFAAAAAA))
        }
        ui.error?.let { Text(text = "Błąd: $it", color = Color(0xFFFF6B6B)) }

        if (ui.lastSyncedCount != null && !ui.syncing) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onSynced) { Text("Przejdź do kanałów") }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    password: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(0.7f)) {
        Text(text = label, color = Color(0xFFCCCCCC), fontSizeSp = 14)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF3DA9FC)),
            visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF3DA9FC))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

/** Skrót na Text z rozmiarem w sp (tv-material Text nie ma domyślnego dużego rozmiaru). */
@Composable
private fun Text(text: String, color: Color = Color.White, fontSizeSp: Int) {
    androidx.tv.material3.Text(
        text = text,
        color = color,
        style = androidx.tv.material3.MaterialTheme.typography.bodyLarge.copy(fontSize = fontSizeSp.sp),
    )
}
