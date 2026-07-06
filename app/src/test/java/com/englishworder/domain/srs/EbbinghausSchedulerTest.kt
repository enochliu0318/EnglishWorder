package com.englishworder.domain.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.englishworder.domain.model.ReviewStatus

class EbbinghausSchedulerTest {

    @Test
    fun markAsLearned_setsLearningStatus() {
        val record = EbbinghausScheduler.createInitialRecord(1L)
        val learned = EbbinghausScheduler.markAsLearned(record, now = 1_000L)
        assertEquals(ReviewStatus.LEARNING, learned.status)
        assertTrue(learned.nextReviewAt > 1_000L)
    }

    @Test
    fun recordReview_success_increasesInterval() {
        val record = EbbinghausScheduler.createInitialRecord(1L).copy(
            status = ReviewStatus.LEARNING,
            repetitions = 0
        )
        val reviewed = EbbinghausScheduler.recordReview(record, quality = 5, now = 0L)
        assertEquals(1, reviewed.repetitions)
        assertEquals(ReviewStatus.REVIEW, reviewed.status)
        assertEquals(1, reviewed.interval)
    }

    @Test
    fun recordReview_failure_resetsProgress() {
        val record = EbbinghausScheduler.createInitialRecord(1L).copy(
            status = ReviewStatus.REVIEW,
            repetitions = 3,
            interval = 7
        )
        val reviewed = EbbinghausScheduler.recordReview(record, quality = 1, now = 0L)
        assertEquals(0, reviewed.repetitions)
        assertEquals(ReviewStatus.LEARNING, reviewed.status)
        assertEquals(0, reviewed.interval)
    }

    @Test
    fun qualityFromCorrect_mapsCorrectly() {
        assertEquals(1, EbbinghausScheduler.qualityFromCorrect(false))
        assertEquals(5, EbbinghausScheduler.qualityFromCorrect(true, 1000))
        assertEquals(4, EbbinghausScheduler.qualityFromCorrect(true, 6000))
        assertEquals(3, EbbinghausScheduler.qualityFromCorrect(true, 12000))
    }
}
