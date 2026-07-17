package com.englishworder.ui.games

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val AUTO_ADVANCE_DELAY_MS = 800L

fun CoroutineScope.scheduleAutoPageTurn(
    atIndex: Int,
    delayMs: Long = AUTO_ADVANCE_DELAY_MS,
    canProceed: () -> Boolean,
    advanceToNext: () -> Unit,
    completeSession: () -> Unit,
    isLastPage: () -> Boolean
) {
    launch {
        delay(delayMs)
        if (!canProceed()) return@launch
        if (isLastPage()) completeSession() else advanceToNext()
    }
}

data class ChoiceAnswer(
    val selectedIndex: Int = -1,
    val answered: Boolean = false
)

data class SpellingAnswer(
    val input: String = "",
    val feedback: String? = null,
    val showAnswer: Boolean = false,
    val lastAnswerCorrect: Boolean = false
)

fun choiceGameSwipeHint(
    currentIndex: Int,
    total: Int,
    isRetryPhase: Boolean,
    hasWrongQuestions: Boolean,
    currentAnswered: Boolean
): String {
    val nav = if (total > 1) "左右滑动可手动翻页" else ""
    val advance = when {
        isRetryPhase && currentIndex + 1 >= total && currentAnswered -> "完成后自动查看结果"
        !isRetryPhase && currentIndex + 1 >= total && hasWrongQuestions && currentAnswered -> "完成后自动开始错题重练"
        currentIndex + 1 >= total && currentAnswered -> "完成后自动查看结果"
        currentAnswered -> "稍后自动下一题"
        else -> ""
    }
    return listOf(nav, advance).filter { it.isNotBlank() }.joinToString(" · ")
}

fun spellingSwipeHint(currentIndex: Int, total: Int, showAnswer: Boolean): String {
    val nav = if (total > 1) "左右滑动可手动翻页" else ""
    val advance = when {
        currentIndex + 1 >= total && showAnswer -> "完成后自动查看结果"
        showAnswer -> "稍后自动下一题"
        else -> ""
    }
    return listOf(nav, advance).filter { it.isNotBlank() }.joinToString(" · ")
}

fun linkRoundSwipeHint(currentPage: Int, total: Int, roundComplete: Boolean, hasRetry: Boolean, isRetryPhase: Boolean): String {
    val nav = if (total > 1) "左右滑动可手动翻页" else ""
    val advance = when {
        isRetryPhase && currentPage + 1 >= total && roundComplete -> "完成后自动查看结果"
        !isRetryPhase && currentPage + 1 >= total && hasRetry && roundComplete -> "完成后自动开始错题重练"
        currentPage + 1 >= total && roundComplete -> "完成后自动查看结果"
        roundComplete -> "稍后自动下一组"
        else -> ""
    }
    return listOf(nav, advance).filter { it.isNotBlank() }.joinToString(" · ")
}
