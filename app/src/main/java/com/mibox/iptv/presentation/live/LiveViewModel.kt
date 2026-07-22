package com.mibox.iptv.presentation.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibox.iptv.domain.model.Category
import com.mibox.iptv.domain.model.Channel
import com.mibox.iptv.domain.model.ContentKind
import com.mibox.iptv.domain.repository.PlaylistRepository
import com.mibox.iptv.domain.usecase.GetCategoriesUseCase
import com.mibox.iptv.domain.usecase.GetChannelsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LiveViewModel @Inject constructor(
    private val playlist: PlaylistRepository,
    getCategories: GetCategoriesUseCase,
    private val getChannels: GetChannelsUseCase,
) : ViewModel() {

    // Domyślnie pierwsze skonfigurowane źródło. W pełnej wersji: wybór w Ustawieniach.
    val sourceId: StateFlow<Long> = playlist.sources()
        .map { it.firstOrNull()?.id ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val selectedCategory = MutableStateFlow<String?>(null)

    val categories: StateFlow<List<Category>> = sourceId
        .flatMapLatest { id -> getCategories(id, ContentKind.LIVE) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val channels: StateFlow<List<Channel>> =
        combine(sourceId, selectedCategory) { id, cat -> id to cat }
            .flatMapLatest { (id, cat) -> getChannels(id, cat) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectCategory(categoryId: String?) {
        selectedCategory.value = categoryId
    }
}
