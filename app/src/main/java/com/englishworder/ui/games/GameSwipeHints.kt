package com.englishworder.ui.games

fun choiceGameSwipeHint(
    currentIndex: Int,
    total: Int,
    isRetryPhase: Boolean,
    hasWrongQuestions: Boolean
): String = when {
    isRetryPhase && currentIndex + 1 >= total -> "左滑查看结果"
    !isRetryPhase && currentIndex + 1 >= total && hasWrongQuestions -> "左滑开始错题重练"
    currentIndex + 1 >= total -> "左滑查看结果"
    else -> "左滑下一题"
}

fun spellingSwipeHint(currentIndex: Int, total: Int): String =
    if (currentIndex + 1 >= total) "左滑查看结果" else "左滑下一题"
