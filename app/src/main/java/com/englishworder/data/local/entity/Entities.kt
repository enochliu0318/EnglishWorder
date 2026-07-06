package com.englishworder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.englishworder.domain.model.FetchStatus
import com.englishworder.domain.model.ReviewStatus

@Entity(tableName = "word_lists")
data class WordListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class WordListWithCountEntity(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val wordCount: Int
)

@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = WordListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId"), Index(value = ["listId", "text"], unique = true)]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val text: String,
    val phonetic: String = "",
    val meaning: String = "",
    val shortMeaning: String = "",
    val example: String = "",
    val partOfSpeech: String = "",
    val audioUrl: String = "",
    val fetchStatus: FetchStatus = FetchStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "review_records",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("wordId", unique = true), Index("nextReviewAt")]
)
data class ReviewRecordEntity(
    @PrimaryKey val wordId: Long,
    val easeFactor: Double = 2.5,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val nextReviewAt: Long = 0,
    val lastReviewAt: Long = 0,
    val status: ReviewStatus = ReviewStatus.NEW
)

data class WordWithReviewEntity(
    val id: Long,
    val listId: Long,
    val text: String,
    val phonetic: String,
    val meaning: String,
    val shortMeaning: String,
    val example: String,
    val partOfSpeech: String,
    val audioUrl: String,
    val fetchStatus: FetchStatus,
    val createdAt: Long,
    val easeFactor: Double,
    val interval: Int,
    val repetitions: Int,
    val nextReviewAt: Long,
    val lastReviewAt: Long,
    val status: ReviewStatus
)

data class ReviewStatusCountEntity(
    val status: ReviewStatus,
    val count: Int
)

data class ListProgressEntity(
    val listId: Long,
    val listName: String,
    val total: Int,
    val newCount: Int,
    val learningCount: Int,
    val reviewCount: Int,
    val masteredCount: Int,
    val dueToday: Int
)

data class DayReviewCountEntity(
    val dayOffset: Int,
    val count: Int
)
