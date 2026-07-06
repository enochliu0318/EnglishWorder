package com.englishworder.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishworder.data.repository.WordRepository
import com.englishworder.domain.model.ReviewMode
import com.englishworder.domain.srs.EbbinghausScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TileType { WORD, MEANING }

data class LinkTile(
    val id: Int,
    val pairId: Int,
    val type: TileType,
    val text: String,
    val matched: Boolean = false
)

data class LinkMatchUiState(
    val tiles: List<LinkTile> = emptyList(),
    val selectedId: Int? = null,
    val score: Int = 0,
    val moves: Int = 0,
    val finished: Boolean = false,
    val isLoading: Boolean = true,
    val message: String? = null,
    val listId: Long = 0,
    val mode: ReviewMode = ReviewMode.FREE_PRACTICE,
    val audioByPair: Map<Int, String> = emptyMap()
) {
    val updateSrs: Boolean get() = mode == ReviewMode.SCHEDULED
}

@HiltViewModel
class LinkMatchViewModel @Inject constructor(
    private val repository: WordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LinkMatchUiState())
    val state: StateFlow<LinkMatchUiState> = _state.asStateFlow()
    private var wordIdsByPair = mapOf<Int, Long>()

    fun loadGame(listId: Long, mode: ReviewMode) {
        viewModelScope.launch {
            _state.value = LinkMatchUiState(isLoading = true, listId = listId, mode = mode)
            val words = repository.getWordsForGame(listId, mode, 6)
                .filter { it.word.meaning.isNotBlank() }

            if (words.size < 3) {
                _state.value = LinkMatchUiState(
                    finished = true,
                    isLoading = false,
                    message = if (mode == ReviewMode.SCHEDULED) "暂无待复习单词，试试自由练习" else "单词不足",
                    listId = listId,
                    mode = mode
                )
                return@launch
            }

            val pairs = words.take(6)
            wordIdsByPair = pairs.mapIndexed { index, w -> index to w.word.id }.toMap()
            val audioByPair = pairs.mapIndexed { index, w -> index to w.word.audioUrl }.toMap()

            val tiles = pairs.flatMapIndexed { index, item ->
                listOf(
                    LinkTile(id = index * 2, pairId = index, type = TileType.WORD, text = item.word.text),
                    LinkTile(id = index * 2 + 1, pairId = index, type = TileType.MEANING, text = item.word.gameMeaning())
                )
            }.shuffled()

            _state.value = LinkMatchUiState(
                tiles = tiles,
                isLoading = false,
                listId = listId,
                mode = mode,
                audioByPair = audioByPair
            )
        }
    }

    fun restart() {
        val s = _state.value
        loadGame(s.listId, s.mode)
    }

    fun selectTile(tileId: Int) {
        val state = _state.value
        if (state.finished) return
        val tile = state.tiles.find { it.id == tileId } ?: return
        if (tile.matched) return

        val selectedId = state.selectedId
        if (selectedId == null) {
            _state.value = state.copy(selectedId = tileId)
            return
        }
        if (selectedId == tileId) {
            _state.value = state.copy(selectedId = null)
            return
        }

        val first = state.tiles.find { it.id == selectedId } ?: return
        val second = tile
        val isMatch = first.pairId == second.pairId && first.type != second.type

        if (isMatch) {
            val wordId = wordIdsByPair[first.pairId] ?: return
            viewModelScope.launch {
                repository.recordReviewResult(
                    wordId,
                    EbbinghausScheduler.qualityFromCorrect(true),
                    state.updateSrs
                )
            }
            val updated = state.tiles.map {
                if (it.pairId == first.pairId) it.copy(matched = true) else it
            }
            val finished = updated.all { it.matched }
            _state.value = state.copy(
                tiles = updated,
                selectedId = null,
                score = state.score + 10,
                moves = state.moves + 1,
                finished = finished
            )
        } else {
            if (state.updateSrs) {
                val wordId = wordIdsByPair[first.pairId] ?: return
                viewModelScope.launch {
                    repository.recordReviewResult(
                        wordId,
                        EbbinghausScheduler.qualityFromCorrect(false),
                        true
                    )
                }
            }
            _state.value = state.copy(selectedId = null, moves = state.moves + 1)
        }
    }
}
