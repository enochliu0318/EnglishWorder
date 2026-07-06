package com.englishworder.ui.navigation

object Routes {
    const val WORD_LISTS = "word_lists"
    const val WORD_LIST_DETAIL = "word_list_detail/{listId}"
    const val IMPORT = "import"
    const val IMPORT_TO_LIST = "import/{listId}"
    const val PROGRESS = "progress"
    const val LEARN_SESSION = "learn_session/{listId}/{mode}"
    const val REVIEW = "review"
    const val GAME_SETUP = "game_setup/{gameType}?defaultMode={defaultMode}"
    const val GAME_QUIZ = "game_quiz/{listId}/{mode}"
    const val GAME_LINK = "game_link/{listId}/{mode}"
    const val GAME_SPELLING = "game_spelling/{listId}/{mode}"
    const val GAME_LISTENING = "game_listening/{listId}/{mode}"

    fun wordListDetail(listId: Long) = "word_list_detail/$listId"
    fun import(listId: Long) = "import/$listId"
    fun importGlobal() = "import"
    fun learnSession(listId: Long, mode: String) = "learn_session/$listId/$mode"
    fun gameSetup(gameType: String, defaultMode: String = "free_practice") =
        "game_setup/$gameType?defaultMode=$defaultMode"
    fun gameQuiz(listId: Long, mode: String) = "game_quiz/$listId/$mode"
    fun gameLink(listId: Long, mode: String) = "game_link/$listId/$mode"
    fun gameSpelling(listId: Long, mode: String) = "game_spelling/$listId/$mode"
    fun gameListening(listId: Long, mode: String) = "game_listening/$listId/$mode"
}
