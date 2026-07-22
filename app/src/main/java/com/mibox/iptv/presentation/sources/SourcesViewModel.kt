package com.mibox.iptv.presentation.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibox.iptv.domain.model.PlaylistSource
import com.mibox.iptv.domain.repository.SyncStatus
import com.mibox.iptv.domain.usecase.AddSourceUseCase
import com.mibox.iptv.domain.usecase.ObserveSourcesUseCase
import com.mibox.iptv.domain.usecase.RemoveSourceUseCase
import com.mibox.iptv.domain.usecase.SyncPlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourcesUiState(
    val syncing: Boolean = false,
    val statusText: String = "",
    val error: String? = null,
    val lastSyncedCount: Int? = null,
)

@HiltViewModel
class SourcesViewModel @Inject constructor(
    observeSources: ObserveSourcesUseCase,
    private val addSource: AddSourceUseCase,
    private val removeSource: RemoveSourceUseCase,
    private val syncPlaylist: SyncPlaylistUseCase,
) : ViewModel() {

    val sources: StateFlow<List<PlaylistSource>> = observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(SourcesUiState())
    val ui: StateFlow<SourcesUiState> = _ui.asStateFlow()

    fun addM3u(name: String, url: String, epgUrl: String) {
        val source = PlaylistSource.M3u(
            id = 0,
            name = name.ifBlank { "Playlista M3U" },
            url = url.trim(),
            epgUrl = epgUrl.trim().ifBlank { null },
        )
        addAndSync(source)
    }

    fun addXtream(name: String, host: String, user: String, pass: String) {
        val source = PlaylistSource.Xtream(
            id = 0,
            name = name.ifBlank { "Xtream" },
            host = host.trim(),
            username = user.trim(),
            password = pass.trim(),
        )
        addAndSync(source)
    }

    fun remove(sourceId: Long) {
        viewModelScope.launch { removeSource(sourceId) }
    }

    private fun addAndSync(source: PlaylistSource) {
        viewModelScope.launch {
            _ui.value = SourcesUiState(syncing = true, statusText = "Zapisywanie źródła…")
            val id = addSource(source)
            syncPlaylist(id).collect { status ->
                _ui.value = when (status) {
                    is SyncStatus.Idle -> _ui.value.copy(syncing = true)
                    is SyncStatus.Running -> _ui.value.copy(
                        syncing = true,
                        statusText = "${status.label}… (${status.processed})",
                        error = null,
                    )
                    is SyncStatus.Done -> SourcesUiState(
                        syncing = false,
                        statusText = "Zaimportowano ${status.total} kanałów",
                        lastSyncedCount = status.total,
                    )
                    is SyncStatus.Error -> SourcesUiState(
                        syncing = false,
                        statusText = "",
                        error = status.message,
                    )
                }
            }
        }
    }
}
