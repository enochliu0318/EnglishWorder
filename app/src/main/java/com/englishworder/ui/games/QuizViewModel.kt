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

enum class QuizPhase { MAIN, RETRY, DONE }

data class QuizQuestion(
    val word: WordWithReview,
    val options: List<String>,
    val correctIndex: Int
)

data class QuizUiState(
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val score: Int = 0,
    val answered: Boolean = false,
    val selectedIndex: Int = -1,
    val finished: Boolean = false,
    val isLoading: Boolean = true,
    val listId: Long = 0,
    val mode: ReviewMode = ReviewMode.FREE_PRACTICE,
    val phase: QuizPhase = QuizPhase.MAIN,
    val wrongQuestions: List<QuizQuestion> = emptyList(),
    val firstPassTotal: Int = 0,
    val firstPassScore: Int = 0,
    val retryScore: Int = 0
) {
    val updateSrs: Boolean get() = mode == ReviewMode.SCHEDULED
    val isRetryPhase: Boolean get() = phase == QuizPhase.RETRY
    val progressLabel: String
        get() = if (isRetryPhase) {
            "错题重练 ${currentIndex + 1}/${questions.size}"
        } else {
            "第 ${currentIndex + 1}/${questions.size} 题"
        }
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val repository: WordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    fun loadQuiz(listId: Long, mode: ReviewMode) {
        viewModelScope.launch {
            _state.value = QuizUiState(isLoading = true, listId = listId, mode = mode)
            val words = repository.getWordsForGame(listId, mode, 10)
                .filter { it.word.meaning.isNotBlank() }

            val questions = buildQuestions(words)
            _state.value = QuizUiState(
                questions = questions,
                isLoading = false,
                finished = questions.isEmpty(),
                listId = listId,
                mode = mode,
                firstPassTotal = questions.size
            )
        }
    }

    private suspend fun buildQuestions(words: List<WordWithReview>): List<QuizQuestion> {
        return words.mapNotNull { word ->
            val distractors = repository.getDistractorMeanings(
                word.word.listId,
                word.word.id,
                3
            )
            if (distractors.size < 3) return@mapNotNull null
            val correct = word.word.gameMeaning()
            val uniqueDistractors = distractors.filter { it != correct }.distinct()
            if (uniqueDistractors.size < 3) return@mapNotNull null
            val options = (uniqueDistractors.take(3) + correct).shuffled()
            val correctIndex = options.indexOf(correct)
            if (correctIndex < 0) return@mapNotNull null
            QuizQuestion(
                word = word,
                options = options,
                correctIndex = correctIndex
            )
        }
    }

    fun restart() {
        val s = _state.value
        loadQuiz(s.listId, s.mode)
    }

    fun selectOption(index: Int) {
        val state = _state.value
        if (state.answered || state.finished) return
        val question = state.questions.getOrNull(state.currentIndex) ?: return
        val isCorrect = index == question.correctIndex

        if (state.phase == QuizPhase.MAIN) {
            viewModelScope.launch {
                val quality = EbbinghausScheduler.qualityFromCorrect(isCorrect)
                repository.recordReviewResult(question.word.word.id, quality, state.updateSrs)
            }
        }

        _state.value = state.copy(
            answered = true,
            selectedIndex = index,
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
            wrongQuestions = if (state.phase == QuizPhase.MAIN && !isCorrect) {
                state.wrongQuestions + question
            } else {
                state.wrongQuestions
            }
        )
    }

    fun nextQuestion() {
        val state = _state.value
        if (!state.answered || state.finished) return
        val next = state.currentIndex + 1

        if (next >= state.questions.size) {
            when (state.phase) {
                QuizPhase.MAIN -> {
                    if (state.wrongQuestions.isNotEmpty()) {
                        _state.value = state.copy(
                            phase = QuizPhase.RETRY,
                            questions = state.wrongQuestions,
                            currentIndex = 0,
                            answered = false,
                            selectedIndex = -1,
                            score = state.firstPassScore,
                            retryScore = 0
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
        } else {
            _state.value = state.copy(
                currentIndex = next,
                answered = false,
                selectedIndex = -1
            )
        }
    }

    fun finishSummary(): String {
        val s = _state.value
        return buildString {
            append("首轮 ${s.firstPassScore}/${s.firstPassTotal}")
            if (s.wrongQuestions.isNotEmpty()) {
                append(" · 错题重练 ${s.retryScore}/${s.wrongQuestions.size}")
            }
        }
    }
}
