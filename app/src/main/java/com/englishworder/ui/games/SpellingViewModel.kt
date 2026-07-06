package com.englishworder.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishworder.data.repository.WordRepository
import com.englishworder.domain.model.ReviewMode
import com.englishworder.domain.model.WordWithReview
import com.englishworder.domain.srs.EbbinghausScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpellingUiState(
    val words: List<WordWithReview> = emptyList(),
    val currentIndex: Int = 0,
    val input: String = "",
    val feedback: String? = null,
    val showAnswer: Boolean = false,
    val lastAnswerCorrect: Boolean = false,
    val score: Int = 0,
    val finished: Boolean = false,
    val isLoading: Boolean = true,
    val listId: Long = 0,
    val mode: ReviewMode = ReviewMode.FREE_PRACTICE
) {
    val current: WordWithReview? get() = words.getOrNull(currentIndex)
    val updateSrs: Boolean get() = mode == ReviewMode.SCHEDULED
}

@HiltViewModel
class SpellingViewModel @Inject constructor(
    private val repository: WordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SpellingUiState())
    val state: StateFlow<SpellingUiState> = _state.asStateFlow()
    private var startTime = 0L

    fun loadGame(listId: Long, mode: ReviewMode) {
        viewModelScope.launch {
            _state.value = SpellingUiState(isLoading = true, listId = listId, mode = mode)
            val words = repository.getWordsForGame(listId, mode, 10)
                .filter { it.word.meaning.isNotBlank() }
            _state.value = SpellingUiState(
                words = words,
                isLoading = false,
                finished = words.isEmpty(),
                listId = listId,
                mode = mode
            )
            startTime = System.currentTimeMillis()
        }
    }

    fun restart() {
        val s = _state.value
        loadGame(s.listId, s.mode)
    }

    fun updateInput(text: String) {
        if (_state.value.showAnswer || _state.value.finished) return
        _state.value = _state.value.copy(input = text)
    }

    fun submit() {
        val state = _state.value
        val current = state.current ?: return
        if (state.showAnswer) return

        val isCorrect = state.input.trim().equals(current.word.text, ignoreCase = true)
        val elapsed = System.currentTimeMillis() - startTime

        viewModelScope.launch {
            val quality = EbbinghausScheduler.qualityFromCorrect(isCorrect, elapsed)
            repository.recordReviewResult(current.word.id, quality, state.updateSrs)
        }

        _state.value = state.copy(
            feedback = if (isCorrect) "正确！" else "拼错了",
            showAnswer = true,
            lastAnswerCorrect = isCorrect,
            score = state.score + if (isCorrect) 1 else 0
        )
    }

    fun nextWord() {
        val state = _state.value
        if (!state.showAnswer || state.finished) return
        val next = state.currentIndex + 1
        if (next >= state.words.size) {
            _state.value = state.copy(finished = true)
        } else {
            _state.value = state.copy(
                currentIndex = next,
                input = "",
                feedback = null,
                showAnswer = false,
                lastAnswerCorrect = false
            )
            startTime = System.currentTimeMillis()
        }
    }
}
