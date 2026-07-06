package com.englishworder.domain.srs

import com.englishworder.domain.model.ReviewRecord
import com.englishworder.domain.model.ReviewStatus
import java.util.concurrent.TimeUnit

object EbbinghausScheduler {

    private val DAY_MS = TimeUnit.DAYS.toMillis(1)
    private val INTERVALS = listOf(1, 2, 4, 7, 15, 30)

    fun createInitialRecord(wordId: Long, now: Long = System.currentTimeMillis()): ReviewRecord {
        return ReviewRecord(
            wordId = wordId,
            easeFactor = 2.5,
            interval = 0,
            repetitions = 0,
            nextReviewAt = now,
            lastReviewAt = 0,
            status = ReviewStatus.NEW
        )
    }

    fun markAsLearned(record: ReviewRecord, now: Long = System.currentTimeMillis()): ReviewRecord {
        return record.copy(
            status = ReviewStatus.LEARNING,
            repetitions = 0,
            interval = 0,
            nextReviewAt = now + DAY_MS,
            lastReviewAt = now
        )
    }

    fun recordReview(
        record: ReviewRecord,
        quality: Int,
        now: Long = System.currentTimeMillis()
    ): ReviewRecord {
        val q = quality.coerceIn(0, 5)
        var easeFactor = record.easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        easeFactor = easeFactor.coerceAtLeast(1.3)

        return if (q < 3) {
            record.copy(
                easeFactor = easeFactor,
                interval = 0,
                repetitions = 0,
                status = ReviewStatus.LEARNING,
                nextReviewAt = now + TimeUnit.HOURS.toMillis(12),
                lastReviewAt = now
            )
        } else {
            val newRepetitions = record.repetitions + 1
            val intervalDays = when {
                newRepetitions == 1 -> INTERVALS[0]
                newRepetitions == 2 -> INTERVALS[1]
                newRepetitions <= INTERVALS.size -> INTERVALS[newRepetitions - 1]
                else -> (record.interval * easeFactor).toInt().coerceAtMost(90)
            }

            val newStatus = when {
                newRepetitions >= 6 && easeFactor >= 2.3 -> ReviewStatus.MASTERED
                newRepetitions >= 1 -> ReviewStatus.REVIEW
                else -> ReviewStatus.LEARNING
            }

            record.copy(
                easeFactor = easeFactor,
                interval = intervalDays,
                repetitions = newRepetitions,
                status = newStatus,
                nextReviewAt = now + intervalDays * DAY_MS,
                lastReviewAt = now
            )
        }
    }

    fun qualityFromCorrect(isCorrect: Boolean, responseTimeMs: Long = 0): Int {
        return when {
            !isCorrect -> 1
            responseTimeMs > 10_000 -> 3
            responseTimeMs > 5_000 -> 4
            else -> 5
        }
    }

    fun endOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val dayMs = TimeUnit.DAYS.toMillis(1)
        return ((timestamp / dayMs) + 1) * dayMs - 1
    }

    fun startOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val dayMs = TimeUnit.DAYS.toMillis(1)
        return (timestamp / dayMs) * dayMs
    }
}
