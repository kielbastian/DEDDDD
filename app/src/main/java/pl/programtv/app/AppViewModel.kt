package pl.programtv.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.programtv.app.data.ChannelEntity
import pl.programtv.app.data.EpgRepository
import pl.programtv.app.data.ProgrammeEntity
import pl.programtv.app.data.ProgrammeWithChannel
import pl.programtv.app.data.ReminderEntity
import pl.programtv.app.data.reminderKeyOf
import pl.programtv.app.reminders.ReminderScheduler
import java.io.IOException

data class RefreshUiState(
    val refreshing: Boolean = false,
    val error: String? = null,
    val lastUpdateMillis: Long = 0L
)

data class SearchUiState(
    val query: String = "",
    val searching: Boolean = false,
    val searched: Boolean = false,
    val results: List<ProgrammeWithChannel> = emptyList(),
    val suggestions: List<String> = emptyList()
)

data class MoviesUiState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val genreId: String = "all",
    val movies: List<ProgrammeWithChannel> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = EpgRepository(app)

    private val _refreshState = MutableStateFlow(
        RefreshUiState(lastUpdateMillis = repo.lastUpdateMillis)
    )
    val refreshState: StateFlow<RefreshUiState> = _refreshState

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState

    private val _moviesState = MutableStateFlow(MoviesUiState())
    val moviesState: StateFlow<MoviesUiState> = _moviesState

    private var suggestJob: Job? = null
    private var movieCache: List<ProgrammeWithChannel>? = null

    val epgUrl: String get() = repo.epgUrl
    val polishOnly: Boolean get() = repo.polishOnly

    /** Klucze przypomnień aktualnie ustawionych – do podświetlania gwiazdek. */
    val reminderKeys: StateFlow<Set<String>> = repo.reminderDao.observeReminders()
        .map { list -> list.map { it.key }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val channels: StateFlow<List<ChannelEntity>> = repo.dao.observeChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedChannels: StateFlow<List<ChannelEntity>> = repo.dao.observeSelectedChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Odświeżany co minutę znacznik czasu – napędza widok "Teraz w TV". */
    private val minuteTick = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    val nowPlaying: StateFlow<List<ProgrammeWithChannel>> = minuteTick
        .flatMapLatest { now -> repo.dao.observeNowPlaying(now) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Kanał wybrany na ekranie "Program". */
    val scheduleChannelId = MutableStateFlow<String?>(null)

    val schedule: StateFlow<List<ProgrammeWithChannel>> = scheduleChannelId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repo.dao.observeSchedule(id, System.currentTimeMillis())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Przy pierwszym uruchomieniu pobierz program TV automatycznie.
        viewModelScope.launch {
            if (!repo.hasData()) refresh()
        }
    }

    fun refresh() {
        if (_refreshState.value.refreshing) return
        viewModelScope.launch {
            _refreshState.update { it.copy(refreshing = true, error = null) }
            try {
                repo.refresh()
                _refreshState.update {
                    it.copy(refreshing = false, lastUpdateMillis = repo.lastUpdateMillis)
                }
                // Świeże dane – przelicz filmy ponownie przy następnym wejściu.
                movieCache = null
                if (_moviesState.value.loaded) {
                    _moviesState.update { it.copy(loaded = false, movies = emptyList()) }
                    ensureMoviesLoaded()
                }
            } catch (e: IOException) {
                _refreshState.update {
                    it.copy(
                        refreshing = false,
                        error = "Nie udało się pobrać programu TV: ${e.message ?: "błąd sieci"}"
                    )
                }
            } catch (e: Exception) {
                _refreshState.update {
                    it.copy(
                        refreshing = false,
                        error = "Błąd przetwarzania danych EPG: ${e.message ?: e.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    fun dismissError() {
        _refreshState.update { it.copy(error = null) }
    }

    fun setChannelSelected(channelId: String, selected: Boolean) {
        viewModelScope.launch { repo.dao.setChannelSelected(channelId, selected) }
    }

    /** Zaznacza/odznacza wiele kanałów naraz (np. „Zaznacz wszystkie”). */
    fun setChannelsSelected(channelIds: List<String>, selected: Boolean) {
        viewModelScope.launch {
            // SQLite ogranicza liczbę parametrów zapytania, stąd porcje.
            channelIds.chunked(800).forEach { repo.dao.setChannelsSelected(it, selected) }
        }
    }

    fun setEpgUrl(url: String) {
        repo.epgUrl = url
    }

    /** Czy dla tej pozycji da się jeszcze ustawić przypomnienie (musi być w przyszłości). */
    fun canRemind(programme: ProgrammeEntity): Boolean =
        programme.startMillis > System.currentTimeMillis()

    /** Włącza/wyłącza przypomnienie (gwiazdkę) dla danej pozycji programu. */
    fun toggleReminder(item: ProgrammeWithChannel) {
        val programme = item.programme
        val key = reminderKeyOf(programme.channelId, programme.startMillis)
        val app = getApplication<Application>()
        viewModelScope.launch {
            if (key in reminderKeys.value) {
                repo.reminderDao.deleteByKey(key)
                ReminderScheduler.cancel(app, key)
            } else {
                if (!canRemind(programme)) return@launch
                val reminder = ReminderEntity(
                    key = key,
                    channelId = programme.channelId,
                    channelName = item.channelName,
                    title = programme.title,
                    startMillis = programme.startMillis,
                    stopMillis = programme.stopMillis
                )
                repo.reminderDao.insert(reminder)
                ReminderScheduler.schedule(app, reminder)
            }
        }
    }

    fun setPolishOnly(value: Boolean) {
        repo.polishOnly = value
    }

    fun onSearchQueryChange(query: String) {
        _searchState.update { it.copy(query = query) }
        suggestJob?.cancel()
        val q = query.trim()
        if (q.length < 2) {
            _searchState.update { it.copy(suggestions = emptyList()) }
            return
        }
        suggestJob = viewModelScope.launch {
            delay(180) // krótki debounce, żeby nie odpytywać bazy przy każdej literze
            val titles = repo.dao.suggestTitles(q, System.currentTimeMillis())
            val ranked = titles
                .sortedWith(
                    compareBy(
                        { if (it.startsWith(q, ignoreCase = true)) 0 else 1 },
                        { it.lowercase() }
                    )
                )
                .take(8)
            _searchState.update { it.copy(suggestions = ranked) }
        }
    }

    /** Wybór podpowiedzi – wpisuje tytuł i od razu wyszukuje. */
    fun applySuggestion(title: String) {
        suggestJob?.cancel()
        _searchState.update { it.copy(query = title, suggestions = emptyList()) }
        search()
    }

    fun search() {
        val query = _searchState.value.query.trim()
        if (query.length < 2) return
        suggestJob?.cancel()
        viewModelScope.launch {
            _searchState.update { it.copy(searching = true, suggestions = emptyList()) }
            val results = repo.dao.searchByTitle(query, System.currentTimeMillis())
            _searchState.update {
                it.copy(searching = false, searched = true, results = results)
            }
        }
    }

    // --- Filmy ---

    /** Ładuje listę filmów (raz), jeśli jeszcze nie wczytana. */
    fun ensureMoviesLoaded() {
        if (movieCache != null || _moviesState.value.loading) return
        viewModelScope.launch {
            _moviesState.update { it.copy(loading = true) }
            val now = System.currentTimeMillis()
            val candidates = repo.dao.candidateMovies(now, MIN_MOVIE_DURATION_MILLIS)
            val movies = candidates.filter { isMovie(it.programme) }
            movieCache = movies
            _moviesState.update {
                it.copy(
                    loading = false,
                    loaded = true,
                    movies = filterMoviesByGenre(movies, it.genreId)
                )
            }
        }
    }

    fun selectMovieGenre(genreId: String) {
        val cache = movieCache
        _moviesState.update {
            it.copy(
                genreId = genreId,
                movies = if (cache != null) filterMoviesByGenre(cache, genreId) else it.movies
            )
        }
    }

    private fun filterMoviesByGenre(
        movies: List<ProgrammeWithChannel>,
        genreId: String
    ): List<ProgrammeWithChannel> {
        val genre = MOVIE_GENRES.firstOrNull { it.id == genreId } ?: MOVIE_GENRES.first()
        return movies.filter { matchesGenre(it.programme, genre) }
    }
}
