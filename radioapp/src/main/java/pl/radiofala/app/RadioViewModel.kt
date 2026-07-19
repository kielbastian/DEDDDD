package pl.radiofala.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.radiofala.app.data.CategorySource
import pl.radiofala.app.data.FavoriteDao
import pl.radiofala.app.data.RADIO_CATEGORIES
import pl.radiofala.app.data.RadioBrowserApi
import pl.radiofala.app.data.RadioCategory
import pl.radiofala.app.data.RadioDatabase
import pl.radiofala.app.data.RadioStation
import pl.radiofala.app.data.toFavoriteEntity
import pl.radiofala.app.data.toStation
import pl.radiofala.app.playback.PlaybackUiState
import pl.radiofala.app.playback.PlayerConnection
import java.io.IOException

data class StationsUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val stations: List<RadioStation> = emptyList()
)

data class SearchUiState(
    val query: String = "",
    val searching: Boolean = false,
    val searched: Boolean = false,
    val results: List<RadioStation> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class RadioViewModel(app: Application) : AndroidViewModel(app) {

    private val api = RadioBrowserApi()
    private val favoriteDao: FavoriteDao = RadioDatabase.get(app).favoriteDao()
    private val player = PlayerConnection(app)

    val categories: List<RadioCategory> = RADIO_CATEGORIES

    private val _selectedCategory = MutableStateFlow(categories.first())
    val selectedCategory: StateFlow<RadioCategory> = _selectedCategory

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState

    private var searchJob: Job? = null

    val favoriteIds: StateFlow<Set<String>> = favoriteDao.observeFavorites()
        .map { list -> list.map { it.stationUuid }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val playback: StateFlow<PlaybackUiState> = player.state

    val stationsState: StateFlow<StationsUiState> = _selectedCategory
        .flatMapLatest { category ->
            when (val source = category.source) {
                is CategorySource.Favorites -> favoriteDao.observeFavorites()
                    .map { list -> StationsUiState(stations = list.map { it.toStation() }) }
                is CategorySource.Tag -> flow {
                    emit(StationsUiState(loading = true))
                    emit(StationsUiState(stations = api.byTag(source.tag)))
                }.catch { e -> emit(fetchError(e)) }
                is CategorySource.Country -> flow {
                    emit(StationsUiState(loading = true))
                    // Więcej niż domyślne 40 - polskich stacji w bazie jest sporo (RMF, VOX, Eska itd.).
                    emit(StationsUiState(stations = api.byCountry(source.code, limit = 150)))
                }.catch { e -> emit(fetchError(e)) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StationsUiState(loading = true))

    init {
        player.connect()
    }

    private fun fetchError(e: Throwable): StationsUiState = StationsUiState(
        error = if (e is IOException) "Nie udało się pobrać stacji: ${e.message ?: "błąd sieci"}"
        else "Błąd wczytywania stacji: ${e.message ?: e.javaClass.simpleName}"
    )

    fun selectCategory(category: RadioCategory) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChange(query: String) {
        _searchState.value = _searchState.value.copy(query = query)
    }

    fun search() {
        val query = _searchState.value.query.trim()
        if (query.length < 2) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searchState.value = _searchState.value.copy(searching = true)
            delay(150)
            try {
                val results = api.searchByName(query)
                _searchState.value = _searchState.value.copy(
                    searching = false, searched = true, results = results
                )
            } catch (_: Exception) {
                _searchState.value = _searchState.value.copy(
                    searching = false, searched = true, results = emptyList()
                )
            }
        }
    }

    fun toggleFavorite(station: RadioStation) {
        viewModelScope.launch {
            if (station.stationUuid in favoriteIds.value) {
                favoriteDao.deleteByUuid(station.stationUuid)
            } else {
                favoriteDao.insert(station.toFavoriteEntity())
            }
        }
    }

    /** Lista, z której odtwarzana jest bieżąca stacja – potrzebna do „poprzednia/następna”. */
    private val _queue = MutableStateFlow<List<RadioStation>>(emptyList())
    private val _queueIndex = MutableStateFlow(-1)

    /** Pełne dane odtwarzanej stacji (favicon, tagi) – niezależne od tego, jaka kategoria jest teraz widoczna. */
    private val _currentStation = MutableStateFlow<RadioStation?>(null)
    val currentStation: StateFlow<RadioStation?> = _currentStation

    val canSkip: StateFlow<Boolean> = _queue
        .map { it.size > 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Odtwarza stację z podanej listy (kategoria albo wyniki wyszukiwania) – zapamiętuje kolejkę. */
    fun play(station: RadioStation, queue: List<RadioStation> = listOf(station)) {
        val index = queue.indexOfFirst { it.stationUuid == station.stationUuid }.let { if (it < 0) 0 else it }
        _queue.value = queue
        _queueIndex.value = index
        playInternal(station)
    }

    fun playNext() {
        val list = _queue.value
        if (list.isEmpty()) return
        val next = (_queueIndex.value + 1).let { if (it >= list.size) 0 else it }
        _queueIndex.value = next
        playInternal(list[next])
    }

    fun playPrevious() {
        val list = _queue.value
        if (list.isEmpty()) return
        val prev = (_queueIndex.value - 1).let { if (it < 0) list.size - 1 else it }
        _queueIndex.value = prev
        playInternal(list[prev])
    }

    /** Niektóre stacje mają dziesiątki tagów w jednym stringu (np. "70s,80s,...,webradio") –
     * bierzemy tylko kilka pierwszych, żeby podtytuł odtwarzacza nie rozjeżdżał layoutu. */
    private fun formatSubtitle(tags: String): String {
        val cleaned = tags.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(3)
            .joinToString(" • ")
        return cleaned.ifBlank { "Radio Fala" }
    }

    private fun playInternal(station: RadioStation) {
        _currentStation.value = station
        viewModelScope.launch {
            val url = try {
                api.resolveStreamUrl(station)
            } catch (_: Exception) {
                station.streamUrl
            }
            player.play(
                mediaId = station.stationUuid,
                title = station.name,
                subtitle = formatSubtitle(station.tags),
                streamUrl = url,
                artworkUri = station.faviconUrl
            )
        }
    }

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes

    fun togglePlayPause() = player.togglePlayPause()

    fun stopPlayback() {
        player.stop()
        _currentStation.value = null
        _queue.value = emptyList()
        _queueIndex.value = -1
    }

    private var sleepTimerClearJob: Job? = null

    fun setSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        player.setSleepTimer(minutes)
        sleepTimerClearJob?.cancel()
        sleepTimerClearJob = viewModelScope.launch {
            delay(minutes * 60_000L)
            _sleepTimerMinutes.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerClearJob?.cancel()
        _sleepTimerMinutes.value = null
        player.cancelSleepTimer()
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
