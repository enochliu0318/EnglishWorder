package com.englishworder.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishworder.data.repository.WordRepository
import com.englishworder.domain.model.StudyMode
import com.englishworder.domain.model.WordWithReview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LearnUiState(
    val words: List<WordWithReview> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val finished: Boolean = false,
    val mode: StudyMode = StudyMode.NEW_WORDS,
    val listId: Long = 0,
    val cardFlipped: Boolean = false
) {
    val current: WordWithReview? get() = words.getOrNull(currentIndex)
    val progress: String get() = if (words.isEmpty()) "0/0" else "${currentIndex + 1}/${words.size}"
    val modeLabel: String get() = when (mode) {
        StudyMode.NEW_WORDS -> "学习新词"
        StudyMode.FREE_PRACTICE -> "自由练习"
    }
}

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val repository: WordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LearnUiState())
    val state: StateFlow<LearnUiState> = _state.asStateFlow()

    fun loadWords(listId: Long, mode: StudyMode) {
        viewModelScope.launch {
            _state.value = LearnUiState(isLoading = true, mode = mode, listId = listId)
            val words = repository.getWordsForLearn(listId, mode, 20)
            _state.value = LearnUiState(
                words = words,
                isLoading = false,
                finished = words.isEmpty(),
                mode = mode,
                listId = listId,
                cardFlipped = false
            )
        }
    }

    fun restart() {
        val current = _state.value
        loadWords(current.listId, current.mode)
    }

    fun flipCard() {
        _state.value = _state.value.copy(cardFlipped = !_state.value.cardFlipped)
    }

    fun answer(known: Boolean) {
        val current = _state.value.current ?: return
        viewModelScope.launch {
            if (_state.value.mode == StudyMode.NEW_WORDS) {
                repository.markWordStudied(current.word.id, known)
            }
            advance()
        }
    }

    private fun advance() {
        val nextIndex = _state.value.currentIndex + 1
        if (nextIndex >= _state.value.words.size) {
            _state.value = _state.value.copy(finished = true)
        } else {
            _state.value = _state.value.copy(currentIndex = nextIndex, cardFlipped = false)
        }
    }
}
