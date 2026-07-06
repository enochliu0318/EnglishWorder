package com.englishworder.data.local

import com.englishworder.data.local.entity.ReviewRecordEntity
import com.englishworder.data.local.entity.WordEntity
import com.englishworder.data.local.entity.WordListEntity
import com.englishworder.data.local.entity.WordWithReviewEntity
import com.englishworder.domain.model.FetchStatus
import com.englishworder.domain.model.ReviewRecord
import com.englishworder.domain.model.ReviewStatus
import com.englishworder.domain.model.Word
import com.englishworder.domain.model.WordList
import com.englishworder.domain.model.WordWithReview
import com.englishworder.domain.util.MeaningFormatter

fun WordListEntity.toDomain(wordCount: Int = 0) = WordList(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt,
    wordCount = wordCount
)

fun WordEntity.toDomain() = Word(
    id = id,
    listId = listId,
    text = text,
    phonetic = phonetic,
    meaning = meaning,
    shortMeaning = shortMeaning,
    example = example,
    partOfSpeech = partOfSpeech,
    audioUrl = audioUrl,
    fetchStatus = fetchStatus,
    createdAt = createdAt
)

fun Word.toEntity() = WordEntity(
    id = id,
    listId = listId,
    text = text,
    phonetic = phonetic,
    meaning = meaning,
    shortMeaning = shortMeaning.ifBlank { MeaningFormatter.short(meaning) },
    example = example,
    partOfSpeech = partOfSpeech,
    audioUrl = audioUrl,
    fetchStatus = fetchStatus,
    createdAt = createdAt
)

fun ReviewRecordEntity.toDomain() = ReviewRecord(
    wordId = wordId,
    easeFactor = easeFactor,
    interval = interval,
    repetitions = repetitions,
    nextReviewAt = nextReviewAt,
    lastReviewAt = lastReviewAt,
    status = status
)

fun ReviewRecord.toEntity() = ReviewRecordEntity(
    wordId = wordId,
    easeFactor = easeFactor,
    interval = interval,
    repetitions = repetitions,
    nextReviewAt = nextReviewAt,
    lastReviewAt = lastReviewAt,
    status = status
)

fun WordWithReviewEntity.toDomain() = WordWithReview(
    word = Word(
        id = id,
        listId = listId,
        text = text,
        phonetic = phonetic,
        meaning = meaning,
        shortMeaning = shortMeaning,
        example = example,
        partOfSpeech = partOfSpeech,
        audioUrl = audioUrl,
        fetchStatus = fetchStatus,
        createdAt = createdAt
    ),
    review = ReviewRecord(
        wordId = id,
        easeFactor = easeFactor,
        interval = interval,
        repetitions = repetitions,
        nextReviewAt = nextReviewAt,
        lastReviewAt = lastReviewAt,
        status = status
    )
)
