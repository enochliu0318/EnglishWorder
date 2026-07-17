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

data class ListeningQuestion(
    val word: WordWithReview,
    val options: List<String>,
    val correctIndex: Int
)

data class ListeningUiState(
    val questions: List<ListeningQuestion> = emptyList(),
    val answers: List<ChoiceAnswer> = emptyList(),
    val currentIndex: Int = 0,
    val score: Int = 0,
    val finished: Boolean = false,
    val isLoading: Boolean = true,
    val listId: Long = 0,
    val phase: QuizPhase = QuizPhase.MAIN,
    val wrongQuestions: List<ListeningQuestion> = emptyList(),
    val firstPassTotal: Int = 0,
    val firstPassScore: Int = 0,
    val retryScore: Int = 0
) {
    val isRetryPhase: Boolean get() = phase == QuizPhase.RETRY
    val currentAnswer: ChoiceAnswer get() = answers.getOrNull(currentIndex) ?: ChoiceAnswer()
    val answered: Boolean get() = currentAnswer.answered
    val selectedIndex: Int get() = currentAnswer.selectedIndex
    val progressLabel: String
        get() = if (isRetryPhase) {
            "错题重练 ${currentIndex + 1}/${questions.size}"
        } else {
            "第 ${currentIndex + 1}/${questions.size} 题"
        }

    fun answerFor(index: Int): ChoiceAnswer = answers.getOrNull(index) ?: ChoiceAnswer()
}

@HiltViewModel
class ListeningViewModel @Inject constructor(
    private val repository: WordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ListeningUiState())
    val state: StateFlow<ListeningUiState> = _state.asStateFlow()

    fun loadGame(listId: Long) {
        viewModelScope.launch {
            _state.value = ListeningUiState(isLoading = true, listId = listId)
            val words = repository.getWordsForSession(listId, StudyFilter.ALL, 10)
                .filter { it.word.text.isNotBlank() && it.word.meaning.isNotBlank() }

            val questions = buildQuestions(words)
            _state.value = ListeningUiState(
                questions = questions,
                answers = List(questions.size) { ChoiceAnswer() },
                isLoading = false,
                finished = questions.isEmpty(),
                listId = listId,
                firstPassTotal = questions.size
            )
        }
    }

    private suspend fun buildQuestions(words: List<WordWithReview>): List<ListeningQuestion> {
        return words.mapNotNull { item ->
            val distractors = repository.getDistractorMeanings(
                item.word.listId,
                item.word.id,
                3
            )
            if (distractors.size < 3) return@mapNotNull null
            val correct = item.word.gameMeaning()
            val uniqueDistractors = distractors
                .filter { it != correct }
                .distinct()
            if (uniqueDistractors.size < 3) return@mapNotNull null
            val options = (uniqueDistractors.take(3) + correct).shuffled()
            val correctIndex = options.indexOf(correct)
            if (correctIndex < 0) return@mapNotNull null
            ListeningQuestion(
                word = item,
                options = options,
                correctIndex = correctIndex
            )
        }
    }

    fun restart() {
        loadGame(_state.value.listId)
    }

    fun goToPage(page: Int) {
        val state = _state.value
        if (page !in state.questions.indices) return
        _state.value = state.copy(currentIndex = page)
    }

    fun selectOption(index: Int) {
        val state = _state.value
        if (state.finished || state.answerFor(state.currentIndex).answered) return
        val question = state.questions.getOrNull(state.currentIndex) ?: return
        val isCorrect = index == question.correctIndex

        if (state.phase == QuizPhase.MAIN) {
            viewModelScope.launch {
                val quality = EbbinghausScheduler.qualityFromCorrect(isCorrect)
                repository.recordReviewResult(question.word.word.id, quality)
            }
        }

        val answeredIndex = state.currentIndex
        val newAnswers = state.answers.toMutableList()
        newAnswers[answeredIndex] = ChoiceAnswer(index, answered = true)

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
            wrongQuestions = if (state.phase == QuizPhase.MAIN && !isCorrect) {
                state.wrongQuestions + question
            } else {
                state.wrongQuestions
            }
        )

        viewModelScope.scheduleAutoPageTurn(
            atIndex = answeredIndex,
            canProceed = {
                val s = _state.value
                !s.finished && s.currentIndex == answeredIndex && s.answerFor(answeredIndex).answered
            },
            advanceToNext = { goToPage(_state.value.currentIndex + 1) },
            completeSession = { advanceFromLastPage() },
            isLastPage = { answeredIndex >= _state.value.questions.lastIndex }
        )
    }

    fun advanceFromLastPage() {
        val state = _state.value
        if (!state.answered || state.finished) return
        if (state.currentIndex != state.questions.lastIndex) return

        when (state.phase) {
            QuizPhase.MAIN -> {
                if (state.wrongQuestions.isNotEmpty()) {
                    _state.value = state.copy(
                        phase = QuizPhase.RETRY,
                        questions = state.wrongQuestions,
                        answers = List(state.wrongQuestions.size) { ChoiceAnswer() },
                        currentIndex = 0,
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
