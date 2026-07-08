package com.englishworder.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishworder.data.repository.WordRepository
import com.englishworder.domain.model.StudyFilter
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
    val answers: List<SpellingAnswer> = emptyList(),
    val currentIndex: Int = 0,
    val score: Int = 0,
    val finished: Boolean = false,
    val isLoading: Boolean = true,
    val listId: Long = 0,
    val phase: QuizPhase = QuizPhase.MAIN,
    val wrongWordIds: List<Long> = emptyList(),
    val firstPassTotal: Int = 0,
    val firstPassScore: Int = 0,
    val retryScore: Int = 0
) {
    val current: WordWithReview? get() = words.getOrNull(currentIndex)
    val isRetryPhase: Boolean get() = phase == QuizPhase.RETRY
    val currentAnswer: SpellingAnswer get() = answers.getOrNull(currentIndex) ?: SpellingAnswer()
    val input: String get() = currentAnswer.input
    val feedback: String? get() = currentAnswer.feedback
    val showAnswer: Boolean get() = currentAnswer.showAnswer
    val lastAnswerCorrect: Boolean get() = currentAnswer.lastAnswerCorrect

    fun answerFor(index: Int): SpellingAnswer = answers.getOrNull(index) ?: SpellingAnswer()
}

@HiltViewModel
class SpellingViewModel @Inject constructor(
    private val repository: WordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SpellingUiState())
    val state: StateFlow<SpellingUiState> = _state.asStateFlow()
    private var startTime = 0L

    fun loadGame(listId: Long) {
        viewModelScope.launch {
            _state.value = SpellingUiState(isLoading = true, listId = listId)
            val words = repository.getWordsForSession(listId, StudyFilter.ALL, 10)
                .filter { it.word.meaning.isNotBlank() }
            _state.value = SpellingUiState(
                words = words,
                answers = List(words.size) { SpellingAnswer() },
                isLoading = false,
                finished = words.isEmpty(),
                listId = listId,
                firstPassTotal = words.size
            )
            startTime = System.currentTimeMillis()
        }
    }

    fun restart() {
        loadGame(_state.value.listId)
    }

    fun goToPage(page: Int) {
        val state = _state.value
        if (page !in state.words.indices) return
        _state.value = state.copy(currentIndex = page)
        startTime = System.currentTimeMillis()
    }

    fun updateInput(text: String) {
        val state = _state.value
        if (state.answerFor(state.currentIndex).showAnswer || state.finished) return
        val newAnswers = state.answers.toMutableList()
        newAnswers[state.currentIndex] = state.currentAnswer.copy(input = text)
        _state.value = state.copy(answers = newAnswers)
    }

    fun submit() {
        val state = _state.value
        val current = state.current ?: return
        if (state.answerFor(state.currentIndex).showAnswer) return

        val isCorrect = state.input.trim().equals(current.word.text, ignoreCase = true)
        val elapsed = System.currentTimeMillis() - startTime

        if (state.phase == QuizPhase.MAIN) {
            viewModelScope.launch {
                val quality = EbbinghausScheduler.qualityFromCorrect(isCorrect, elapsed)
                repository.recordReviewResult(current.word.id, quality)
            }
        }

        val newAnswers = state.answers.toMutableList()
        newAnswers[state.currentIndex] = SpellingAnswer(
            input = state.input,
            feedback = if (isCorrect) "正确！" else "拼错了",
            showAnswer = true,
            lastAnswerCorrect = isCorrect
        )

        _state.value = state.copy(
            answers = newAnswers,
            score = when {
                state.phase == QuizPhase.RETRY -> state.score
                isCorrect -> state.score + 1
                else -> state.score
            },
            firstPassScore = if (state.phase == QuizPhase.MAIN && isCorrect) {
                state.firstPassScore + 1
            } else {
                state.firstPassScore
            },
            retryScore = if (state.phase == QuizPhase.RETRY && isCorrect) {
                state.retryScore + 1
            } else {
                state.retryScore
            },
            wrongWordIds = if (state.phase == QuizPhase.MAIN && !isCorrect) {
                state.wrongWordIds + current.word.id
            } else {
                state.wrongWordIds
            }
        )
    }

    fun advanceFromLastPage() {
        val state = _state.value
        if (!state.showAnswer || state.finished) return
        if (state.currentIndex != state.words.lastIndex) return

        when (state.phase) {
            QuizPhase.MAIN -> {
                val wrongWords = state.words.filter { it.word.id in state.wrongWordIds }
                if (wrongWords.isNotEmpty()) {
                    _state.value = state.copy(
                        phase = QuizPhase.RETRY,
                        words = wrongWords,
                        answers = List(wrongWords.size) { SpellingAnswer() },
                        currentIndex = 0,
                        score = state.firstPassScore,
                        retryScore = 0
                    )
                    startTime = System.currentTimeMillis()
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
            append("首轮 ${s.firstPassScore}/${s.firstPassTotal}")
            if (s.wrongWordIds.isNotEmpty()) {
                append(" · 错题重练 ${s.retryScore}/${s.wrongWordIds.size}")
            }
        }
    }
}
