package com.englishworder.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishworder.data.repository.WordRepository
import com.englishworder.domain.model.StudyFilter
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
    val filter: StudyFilter = StudyFilter.PLAN,
    val listId: Long = 0,
    val cardFlipped: Boolean = false
) {
    val current: WordWithReview? get() = words.getOrNull(currentIndex)
    val progress: String get() = if (words.isEmpty()) "0/0" else "${currentIndex + 1}/${words.size}"
    val filterLabel: String get() = when (filter) {
        StudyFilter.PLAN -> "今日计划"
        StudyFilter.NEW -> "新词"
        StudyFilter.ALL -> "全部"
    }
}

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val repository: WordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LearnUiState())
    val state: StateFlow<LearnUiState> = _state.asStateFlow()

    fun loadWords(listId: Long, initialFilter: StudyFilter?) {
        viewModelScope.launch {
            val filter = initialFilter ?: repository.suggestDefaultFilter(listId)
            _state.value = LearnUiState(isLoading = true, filter = filter, listId = listId)
            val words = repository.getWordsForSession(listId, filter, 20)
            _state.value = LearnUiState(
                words = words,
                isLoading = false,
                finished = words.isEmpty(),
                filter = filter,
                listId = listId,
                cardFlipped = false
            )
        }
    }

    fun setFilter(filter: StudyFilter) {
        val listId = _state.value.listId
        if (listId == 0L) return
        loadWords(listId, filter)
    }

    fun restart() {
        val current = _state.value
        loadWords(current.listId, current.filter)
    }

    fun flipCard() {
        _state.value = _state.value.copy(cardFlipped = !_state.value.cardFlipped)
    }

    fun goToIndex(index: Int) {
        val state = _state.value
        if (index !in state.words.indices) return
        _state.value = state.copy(currentIndex = index, cardFlipped = false)
    }

    fun previousCard() {
        goToIndex(_state.value.currentIndex - 1)
    }

    fun nextCard() {
        goToIndex(_state.value.currentIndex + 1)
    }

    fun answer(known: Boolean) {
        val current = _state.value.current ?: return
        viewModelScope.launch {
            repository.markWordStudied(current.word.id, known)
            advance()
        }
    }

    private fun advance() {
        val state = _state.value
        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.words.size) {
            _state.value = state.copy(finished = true)
        } else {
            _state.value = state.copy(currentIndex = nextIndex, cardFlipped = false)
        }
    }
}
