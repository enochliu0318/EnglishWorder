package com.englishworder.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishworder.data.repository.WordRepository
import com.englishworder.domain.model.StudyFilter
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

data class LinkPairInfo(
    val pairId: Int,
    val wordId: Long,
    val wordText: String,
    val meaning: String,
    val audioUrl: String
)

data class LinkRoundState(
    val tiles: List<LinkTile>,
    val selectedId: Int? = null
) {
    val isComplete: Boolean get() = tiles.all { it.matched }
}

data class LinkMatchUiState(
    val rounds: List<LinkRoundState> = emptyList(),
    val currentPage: Int = 0,
    val pairInfos: List<LinkPairInfo> = emptyList(),
    val score: Int = 0,
    val moves: Int = 0,
    val finished: Boolean = false,
    val isLoading: Boolean = true,
    val message: String? = null,
    val listId: Long = 0,
    val phase: QuizPhase = QuizPhase.MAIN,
    val wrongPairIds: Set<Int> = emptySet(),
    val firstPassWrongCount: Int = 0,
    val retryScore: Int = 0
) {
    val isRetryPhase: Boolean get() = phase == QuizPhase.RETRY
    val currentRound: LinkRoundState? get() = rounds.getOrNull(currentPage)
    val allRoundsComplete: Boolean get() = rounds.isNotEmpty() && rounds.all { it.isComplete }
    val progressLabel: String
        get() = if (isRetryPhase) {
            "错题重练 ${currentPage + 1}/${rounds.size} 组"
        } else {
            "第 ${currentPage + 1}/${rounds.size} 组"
        }
}

@HiltViewModel
class LinkMatchViewModel @Inject constructor(
    private val repository: WordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LinkMatchUiState())
    val state: StateFlow<LinkMatchUiState> = _state.asStateFlow()

    companion object {
        private const val PAIRS_PER_ROUND = 3
    }

    fun loadGame(listId: Long) {
        viewModelScope.launch {
            _state.value = LinkMatchUiState(isLoading = true, listId = listId)
            val words = repository.getWordsForSession(listId, StudyFilter.ALL, 6)
                .filter { it.word.meaning.isNotBlank() }

            if (words.size < 3) {
                _state.value = LinkMatchUiState(
                    finished = true,
                    isLoading = false,
                    message = "词库单词不足",
                    listId = listId
                )
                return@launch
            }

            val pairInfos = words.take(6).mapIndexed { index, item ->
                LinkPairInfo(
                    pairId = index,
                    wordId = item.word.id,
                    wordText = item.word.text,
                    meaning = item.word.gameMeaning(),
                    audioUrl = item.word.audioUrl
                )
            }

            _state.value = LinkMatchUiState(
                rounds = buildRounds(pairInfos),
                pairInfos = pairInfos,
                isLoading = false,
                listId = listId
            )
        }
    }

    fun restart() {
        loadGame(_state.value.listId)
    }

    fun goToPage(page: Int) {
        val state = _state.value
        if (page !in state.rounds.indices) return
        _state.value = state.copy(currentPage = page)
    }

    fun selectTile(tileId: Int) {
        val state = _state.value
        if (state.finished) return
        val page = state.currentPage
        val round = state.currentRound ?: return

        val tile = round.tiles.find { it.id == tileId } ?: return
        if (tile.matched) return

        val selectedId = round.selectedId
        if (selectedId == null) {
            updateRound(page, round.copy(selectedId = tileId))
            return
        }
        if (selectedId == tileId) {
            updateRound(page, round.copy(selectedId = null))
            return
        }

        val first = round.tiles.find { it.id == selectedId } ?: return
        val second = tile
        val isMatch = first.pairId == second.pairId && first.type != second.type

        if (isMatch) {
            val pairInfo = state.pairInfos.find { it.pairId == first.pairId } ?: return
            if (state.phase == QuizPhase.MAIN) {
                viewModelScope.launch {
                    repository.recordReviewResult(
                        pairInfo.wordId,
                        EbbinghausScheduler.qualityFromCorrect(true)
                    )
                }
            }
            val updatedTiles = round.tiles.map {
                if (it.pairId == first.pairId) it.copy(matched = true) else it
            }
            val updatedRound = round.copy(tiles = updatedTiles, selectedId = null)
            val newRounds = state.rounds.toMutableList()
            newRounds[page] = updatedRound

            val retryBonus = if (state.phase == QuizPhase.RETRY) 10 else 0
            _state.value = state.copy(
                rounds = newRounds,
                score = state.score + 10,
                moves = state.moves + 1,
                retryScore = if (state.phase == QuizPhase.RETRY) state.retryScore + 1 else state.retryScore
            )
        } else {
            val newWrong = if (state.phase == QuizPhase.MAIN) {
                state.wrongPairIds + first.pairId + second.pairId
            } else {
                state.wrongPairIds
            }
            if (state.phase == QuizPhase.MAIN) {
                val pairInfo = state.pairInfos.find { it.pairId == first.pairId }
                if (pairInfo != null) {
                    viewModelScope.launch {
                        repository.recordReviewResult(
                            pairInfo.wordId,
                            EbbinghausScheduler.qualityFromCorrect(false)
                        )
                    }
                }
            }
            updateRound(page, round.copy(selectedId = null))
            _state.value = _state.value.copy(
                wrongPairIds = newWrong,
                moves = _state.value.moves + 1
            )
        }
    }

    fun advanceFromLastPage() {
        val state = _state.value
        if (!state.allRoundsComplete || state.finished) return
        if (state.currentPage != state.rounds.lastIndex) return

        when (state.phase) {
            QuizPhase.MAIN -> {
                val wrongIds = state.wrongPairIds.distinct()
                if (wrongIds.isNotEmpty()) {
                    val retryPairs = state.pairInfos.filter { it.pairId in wrongIds }
                    _state.value = state.copy(
                        phase = QuizPhase.RETRY,
                        pairInfos = retryPairs,
                        rounds = buildRounds(retryPairs),
                        currentPage = 0,
                        firstPassWrongCount = wrongIds.size,
                        retryScore = 0,
                        wrongPairIds = emptySet()
                    )
                } else {
                    _state.value = state.copy(finished = true, phase = QuizPhase.DONE)
                }
            }
            QuizPhase.RETRY -> {
                _state.value = state.copy(finished = true, phase = QuizPhase.DONE)
            }
            QuizPhase.DONE -> Unit
        }
    }

    fun finishSummary(): String {
        val s = _state.value
        return buildString {
            append("得分 ${s.score} · 步数 ${s.moves}")
            if (s.firstPassWrongCount > 0) {
                append(" · 错题重练 ${s.retryScore}/${s.firstPassWrongCount}")
            }
        }
    }

    fun audioUrlFor(pairId: Int): String =
        _state.value.pairInfos.find { it.pairId == pairId }?.audioUrl.orEmpty()

    private fun updateRound(page: Int, round: LinkRoundState) {
        val state = _state.value
        val newRounds = state.rounds.toMutableList()
        newRounds[page] = round
        _state.value = state.copy(rounds = newRounds)
    }

    private fun buildRounds(pairs: List<LinkPairInfo>): List<LinkRoundState> {
        return pairs.chunked(PAIRS_PER_ROUND).map { chunk ->
            val tiles = chunk.flatMapIndexed { chunkIndex, info ->
                val baseId = info.pairId * 2
                listOf(
                    LinkTile(id = baseId, pairId = info.pairId, type = TileType.WORD, text = info.wordText),
                    LinkTile(id = baseId + 1, pairId = info.pairId, type = TileType.MEANING, text = info.meaning)
                )
            }.shuffled()
            LinkRoundState(tiles = tiles)
        }
    }
}
