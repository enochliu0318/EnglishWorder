package com.englishworder.ui.navigation

object Routes {
    const val WORD_LISTS = "word_lists"
    const val WORD_LIST_DETAIL = "word_list_detail/{listId}"
    const val IMPORT = "import"
    const val IMPORT_TO_LIST = "import/{listId}"
    const val PROGRESS = "progress"
    const val LEARN_SESSION = "learn_session/{listId}?filter={filter}"
    const val REVIEW = "review"
    const val GAME_LIST_PICK = "game_list/{gameType}"
    const val GAME_QUIZ = "game_quiz/{listId}"
    const val GAME_LINK = "game_link/{listId}"
    const val GAME_SPELLING = "game_spelling/{listId}"
    const val GAME_LISTENING = "game_listening/{listId}"

    fun wordListDetail(listId: Long) = "word_list_detail/$listId"
    fun import(listId: Long) = "import/$listId"
    fun importGlobal() = "import"
    fun learnSession(listId: Long, filter: String? = null) =
        if (filter != null) "learn_session/$listId?filter=$filter" else "learn_session/$listId"
    fun gameListPick(gameType: String) = "game_list/$gameType"
    fun gameQuiz(listId: Long) = "game_quiz/$listId"
    fun gameLink(listId: Long) = "game_link/$listId"
    fun gameSpelling(listId: Long) = "game_spelling/$listId"
    fun gameListening(listId: Long) = "game_listening/$listId"
}
