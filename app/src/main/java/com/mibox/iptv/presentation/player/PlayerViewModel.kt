package com.mibox.iptv.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibox.iptv.domain.model.Channel
import com.mibox.iptv.domain.model.NowNext
import com.mibox.iptv.domain.usecase.GetAdjacentChannelUseCase
import com.mibox.iptv.domain.usecase.GetNowNextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val channel: Channel? = null,
    val index: Int = 0,
    val overlayVisible: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getAdjacent: GetAdjacentChannelUseCase,
    private val getNowNext: GetNowNextUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var sourceId: Long = 0
    private var categoryId: String? = null

    val nowNext: StateFlow<NowNext> = _state
        .flatMapLatest { s ->
            s.channel?.tvgId?.let { getNowNext(it) } ?: flowOf(NowNext(null, null))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowNext(null, null))

    fun start(sourceId: Long, categoryId: String?, index: Int) {
        this.sourceId = sourceId
        this.categoryId = categoryId
        loadAt(index)
    }

    /** Quick Zap: przełącz na kanał sąsiedni (delta = +1 / -1). */
    fun zap(delta: Int) {
        viewModelScope.launch {
            val current = _state.value.index
            val channel = getAdjacent(sourceId, categoryId, current, delta) ?: return@launch
            _state.value = _state.value.copy(
                channel = channel,
                index = current + delta,
            )
        }
    }

    fun toggleOverlay() {
        _state.value = _state.value.copy(overlayVisible = !_state.value.overlayVisible)
    }

    private fun loadAt(index: Int) {
        viewModelScope.launch {
            val channel = getAdjacent(sourceId, categoryId, index, 0) ?: return@launch
            _state.value = PlayerUiState(channel = channel, index = index)
        }
    }
}
