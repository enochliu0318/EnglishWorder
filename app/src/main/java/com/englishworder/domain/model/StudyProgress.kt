package com.englishworder.domain.model

data class StudyProgressOverview(
    val totalWords: Int = 0,
    val newCount: Int = 0,
    val learningCount: Int = 0,
    val reviewCount: Int = 0,
    val masteredCount: Int = 0,
    val dueTodayCount: Int = 0,
    val masteryPercent: Int = 0,
    val listProgress: List<ListStudyProgress> = emptyList(),
    val upcomingDays: List<DayReviewPlan> = emptyList()
)

data class ListStudyProgress(
    val listId: Long,
    val listName: String,
    val total: Int,
    val newCount: Int,
    val learningCount: Int,
    val reviewCount: Int,
    val masteredCount: Int,
    val dueToday: Int,
    val masteryPercent: Int
)

data class DayReviewPlan(
    val dayOffset: Int,
    val label: String,
    val count: Int
)
