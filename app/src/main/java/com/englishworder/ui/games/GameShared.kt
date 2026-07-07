package com.englishworder.ui.games

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
    val nav = if (total > 1) "左右滑动翻页" else ""
    val advance = when {
        isRetryPhase && currentIndex + 1 >= total && currentAnswered -> "左滑查看结果"
        !isRetryPhase && currentIndex + 1 >= total && hasWrongQuestions && currentAnswered -> "左滑开始错题重练"
        currentIndex + 1 >= total && currentAnswered -> "左滑查看结果"
        currentAnswered -> "左滑下一题"
        else -> ""
    }
    return listOf(nav, advance).filter { it.isNotBlank() }.joinToString(" · ")
}

fun spellingSwipeHint(currentIndex: Int, total: Int, showAnswer: Boolean): String {
    val nav = if (total > 1) "左右滑动翻页" else ""
    val advance = when {
        currentIndex + 1 >= total && showAnswer -> "左滑查看结果"
        showAnswer -> "左滑下一题"
        else -> ""
    }
    return listOf(nav, advance).filter { it.isNotBlank() }.joinToString(" · ")
}

fun linkRoundSwipeHint(currentPage: Int, total: Int, roundComplete: Boolean, hasRetry: Boolean, isRetryPhase: Boolean): String {
    val nav = if (total > 1) "左右滑动翻页" else ""
    val advance = when {
        isRetryPhase && currentPage + 1 >= total && roundComplete -> "左滑查看结果"
        !isRetryPhase && currentPage + 1 >= total && hasRetry && roundComplete -> "左滑开始错题重练"
        currentPage + 1 >= total && roundComplete -> "左滑查看结果"
        roundComplete -> "左滑下一组"
        else -> ""
    }
    return listOf(nav, advance).filter { it.isNotBlank() }.joinToString(" · ")
}
