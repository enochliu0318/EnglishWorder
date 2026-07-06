package com.englishworder.domain.model

import com.englishworder.domain.util.MeaningFormatter

enum class FetchStatus {
    PENDING,
    OK,
    FAILED
}

enum class ReviewStatus {
    NEW,
    LEARNING,
    REVIEW,
    MASTERED
}

data class WordList(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val wordCount: Int = 0
)

data class Word(
    val id: Long = 0,
    val listId: Long,
    val text: String,
    val phonetic: String = "",
    val meaning: String = "",
    val shortMeaning: String = "",
    val example: String = "",
    val audioUrl: String = "",
    val fetchStatus: FetchStatus = FetchStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** 游戏/消消乐显示的短释义（≤5字） */
    fun gameMeaning(): String = shortMeaning.ifBlank { MeaningFormatter.short(meaning) }
}

data class ReviewRecord(
    val wordId: Long,
    val easeFactor: Double = 2.5,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val nextReviewAt: Long = 0,
    val lastReviewAt: Long = 0,
    val status: ReviewStatus = ReviewStatus.NEW
)

data class WordWithReview(
    val word: Word,
    val review: ReviewRecord
)

data class ParsedWordEntry(
    val text: String,
    val phonetic: String? = null,
    val meaning: String? = null,
    val example: String? = null
)

data class WordInfo(
    val phonetic: String,
    val meaning: String,
    val shortMeaning: String = "",
    val example: String,
    val audioUrl: String
)

data class ImportResult(
    val total: Int,
    val imported: Int,
    val skipped: Int,
    val pendingFetch: Int
)

enum class GameType {
    QUIZ,
    LINK_MATCH,
    SPELLING,
    LISTENING
}

enum class StudyMode {
    NEW_WORDS,
    FREE_PRACTICE
}

enum class ReviewMode {
    SCHEDULED,
    FREE_PRACTICE
}
