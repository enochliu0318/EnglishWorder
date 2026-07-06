package com.englishworder.data.repository

import com.englishworder.data.local.dao.ReviewRecordDao
import com.englishworder.data.local.dao.WordDao
import com.englishworder.data.local.dao.WordListDao
import com.englishworder.data.local.toDomain
import com.englishworder.data.local.toEntity
import com.englishworder.data.remote.DictionaryProvider
import com.englishworder.domain.model.FetchStatus
import com.englishworder.domain.util.MeaningFormatter
import com.englishworder.domain.model.ImportResult
import com.englishworder.domain.model.ParsedWordEntry
import com.englishworder.domain.model.ReviewRecord
import com.englishworder.domain.model.ReviewMode
import com.englishworder.domain.model.StudyMode
import com.englishworder.domain.model.StudyProgressOverview
import com.englishworder.domain.model.ListStudyProgress
import com.englishworder.domain.model.DayReviewPlan
import com.englishworder.domain.model.ReviewStatus
import com.englishworder.domain.model.Word
import com.englishworder.domain.model.WordList
import com.englishworder.domain.model.WordWithReview
import com.englishworder.domain.srs.EbbinghausScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordRepository @Inject constructor(
    private val wordListDao: WordListDao,
    private val wordDao: WordDao,
    private val reviewRecordDao: ReviewRecordDao,
    private val dictionaryProvider: DictionaryProvider
) {

    fun observeWordLists(): Flow<List<WordList>> {
        return wordListDao.observeAllWithCount().map { lists ->
            lists.map { entity ->
                WordList(
                    id = entity.id,
                    name = entity.name,
                    description = entity.description,
                    createdAt = entity.createdAt,
                    wordCount = entity.wordCount
                )
            }
        }
    }

    suspend fun getWordList(id: Long): WordList? {
        val entity = wordListDao.getById(id) ?: return null
        return entity.toDomain(wordDao.countByListId(id))
    }

    suspend fun createWordList(name: String, description: String = ""): Long {
        return wordListDao.insert(
            com.englishworder.data.local.entity.WordListEntity(
                name = name.trim(),
                description = description.trim()
            )
        )
    }

    suspend fun updateWordList(id: Long, name: String, description: String) {
        val existing = wordListDao.getById(id) ?: return
        wordListDao.update(
            existing.copy(
                name = name.trim(),
                description = description.trim()
            )
        )
    }

    suspend fun deleteWordList(id: Long) {
        wordListDao.deleteById(id)
    }

    fun observeWords(listId: Long): Flow<List<Word>> {
        return wordDao.observeByListId(listId).map { words ->
            words.map { it.toDomain() }
        }
    }

    fun observeWordsWithReview(listId: Long): Flow<List<WordWithReview>> {
        return wordDao.observeWithReviewByListId(listId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun addWord(
        listId: Long,
        text: String,
        phonetic: String = "",
        meaning: String = "",
        example: String = "",
        fetchIfNeeded: Boolean = true
    ): Long? {
        val normalized = text.trim()
        if (normalized.isBlank()) return null
        if (wordDao.getByText(listId, normalized) != null) return null

        val needsFetch = fetchIfNeeded && meaning.isBlank()
        val normalizedMeaning = MeaningFormatter.forLearning(meaning)
        val normalizedShort = MeaningFormatter.short(meaning.ifBlank { normalizedMeaning })
        val wordId = wordDao.insert(
            Word(
                listId = listId,
                text = normalized,
                phonetic = phonetic,
                meaning = normalizedMeaning,
                shortMeaning = normalizedShort,
                example = example,
                fetchStatus = if (needsFetch) FetchStatus.PENDING else FetchStatus.OK
            ).toEntity()
        )
        if (wordId == -1L) return null

        reviewRecordDao.insert(EbbinghausScheduler.createInitialRecord(wordId).toEntity())

        if (needsFetch) {
            fetchWordInfo(wordId)
        }

        return wordId
    }

    suspend fun importWords(listId: Long, entries: List<ParsedWordEntry>): ImportResult {
        var imported = 0
        var skipped = 0
        var pendingFetch = 0

        entries.forEach { entry ->
            val existing = wordDao.getByText(listId, entry.text)
            if (existing != null) {
                skipped++
                return@forEach
            }

            val hasFullInfo = !entry.meaning.isNullOrBlank()
            val fullMeaning = MeaningFormatter.forLearning(entry.meaning.orEmpty())
            val short = MeaningFormatter.short(entry.meaning.orEmpty())
            val wordId = wordDao.insert(
                Word(
                    listId = listId,
                    text = entry.text.trim(),
                    phonetic = entry.phonetic.orEmpty(),
                    meaning = fullMeaning,
                    shortMeaning = short,
                    example = entry.example.orEmpty(),
                    fetchStatus = if (hasFullInfo) FetchStatus.OK else FetchStatus.PENDING
                ).toEntity()
            )

            if (wordId == -1L) {
                skipped++
            } else {
                imported++
                reviewRecordDao.insert(EbbinghausScheduler.createInitialRecord(wordId).toEntity())
                if (!hasFullInfo) pendingFetch++
            }
        }

        return ImportResult(
            total = entries.size,
            imported = imported,
            skipped = skipped,
            pendingFetch = pendingFetch
        )
    }

    suspend fun fetchPendingWords(): Int {
        val pending = wordDao.getByFetchStatus(FetchStatus.PENDING)
        var success = 0
        pending.forEach { entity ->
            if (fetchWordInfo(entity.id)) success++
        }
        return success
    }

    suspend fun fetchWordInfo(wordId: Long): Boolean {
        val word = wordDao.getById(wordId) ?: return false
        return dictionaryProvider.lookup(word.text)
            .onSuccess { info ->
                wordDao.update(
                    word.copy(
                        phonetic = info.phonetic,
                        meaning = info.meaning,
                        shortMeaning = info.shortMeaning.ifBlank { MeaningFormatter.short(info.meaning) },
                        example = info.example,
                        audioUrl = info.audioUrl,
                        fetchStatus = FetchStatus.OK
                    )
                )
            }
            .onFailure {
                wordDao.update(word.copy(fetchStatus = FetchStatus.FAILED))
            }
            .isSuccess
    }

    suspend fun deleteWord(wordId: Long) {
        wordDao.deleteById(wordId)
    }

    fun observeDueReviews(): Flow<List<WordWithReview>> {
        val endOfDay = EbbinghausScheduler.endOfDay()
        return reviewRecordDao.observeDueReviews(endOfDay).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeDueCount(): Flow<Int> {
        return reviewRecordDao.observeDueCount(EbbinghausScheduler.endOfDay())
    }

    fun observeStudyProgress(): Flow<StudyProgressOverview> {
        val endOfDay = EbbinghausScheduler.endOfDay()
        val startOfToday = EbbinghausScheduler.startOfDay()
        val weekEnd = startOfToday + TimeUnit.DAYS.toMillis(7)

        return combine(
            reviewRecordDao.observeStatusCounts(),
            reviewRecordDao.observeListProgress(endOfDay),
            reviewRecordDao.observeUpcomingReviews(startOfToday, weekEnd),
            reviewRecordDao.observeDueCount(endOfDay)
        ) { statusCounts, listRows, upcoming, dueToday ->
            val counts = statusCounts.associate { it.status to it.count }
            val newCount = counts[ReviewStatus.NEW] ?: 0
            val learningCount = counts[ReviewStatus.LEARNING] ?: 0
            val reviewCount = counts[ReviewStatus.REVIEW] ?: 0
            val masteredCount = counts[ReviewStatus.MASTERED] ?: 0
            val total = newCount + learningCount + reviewCount + masteredCount
            val masteryPercent = if (total > 0) (masteredCount * 100 / total) else 0

            StudyProgressOverview(
                totalWords = total,
                newCount = newCount,
                learningCount = learningCount,
                reviewCount = reviewCount,
                masteredCount = masteredCount,
                dueTodayCount = dueToday,
                masteryPercent = masteryPercent,
                listProgress = listRows.map { row ->
                    ListStudyProgress(
                        listId = row.listId,
                        listName = row.listName,
                        total = row.total,
                        newCount = row.newCount,
                        learningCount = row.learningCount,
                        reviewCount = row.reviewCount,
                        masteredCount = row.masteredCount,
                        dueToday = row.dueToday,
                        masteryPercent = if (row.total > 0) row.masteredCount * 100 / row.total else 0
                    )
                },
                upcomingDays = upcoming.map { day ->
                    DayReviewPlan(
                        dayOffset = day.dayOffset,
                        label = dayLabel(day.dayOffset),
                        count = day.count
                    )
                }
            )
        }
    }

    private fun dayLabel(offset: Int): String = when (offset) {
        0 -> "今天"
        1 -> "明天"
        2 -> "后天"
        else -> "${offset}天后"
    }

    suspend fun getDueReviews(limit: Int = 20): List<WordWithReview> {
        return reviewRecordDao.getDueReviews(EbbinghausScheduler.endOfDay(), limit)
            .map { it.toDomain() }
    }

    suspend fun getNewWords(listId: Long, limit: Int = 20): List<WordWithReview> {
        return reviewRecordDao.getNewWords(listId, limit).map { it.toDomain() }
    }

    suspend fun getWordsForLearn(listId: Long, mode: StudyMode, limit: Int = 20): List<WordWithReview> {
        return when (mode) {
            StudyMode.NEW_WORDS -> getNewWords(listId, limit)
            StudyMode.FREE_PRACTICE -> reviewRecordDao.getAllWordsWithReview(listId, limit)
                .map { it.toDomain() }
        }
    }

    suspend fun getWordsForGame(
        listId: Long,
        mode: ReviewMode,
        limit: Int
    ): List<WordWithReview> {
        return when (mode) {
            ReviewMode.SCHEDULED -> {
                val due = reviewRecordDao.getDueReviews(EbbinghausScheduler.endOfDay(), limit)
                    .map { it.toDomain() }
                    .filter { it.word.listId == listId }
                due
            }
            ReviewMode.FREE_PRACTICE -> reviewRecordDao.getAllWordsWithReview(listId, limit)
                .map { it.toDomain() }
        }
    }

    suspend fun getRandomWordsForGame(listId: Long?, limit: Int): List<WordWithReview> {
        val due = getDueReviews(limit)
        if (due.size >= limit) return due.take(limit)

        val remaining = limit - due.size
        val fromList = if (listId != null) {
            reviewRecordDao.getAllWordsWithReview(listId, remaining)
        } else {
            reviewRecordDao.getAllWordsWithReviewGlobal(remaining)
        }

        return (due + fromList.map { it.toDomain() }).distinctBy { it.word.id }.take(limit)
    }

    suspend fun markWordStudied(wordId: Long, known: Boolean) {
        val record = reviewRecordDao.getByWordId(wordId)?.toDomain()
            ?: EbbinghausScheduler.createInitialRecord(wordId)
        val learned = if (record.status == ReviewStatus.NEW) {
            EbbinghausScheduler.markAsLearned(record)
        } else {
            record
        }
        val quality = if (known) 5 else 2
        reviewRecordDao.insert(EbbinghausScheduler.recordReview(learned, quality).toEntity())
    }

    suspend fun markWordLearned(wordId: Long) {
        markWordStudied(wordId, known = true)
    }

    suspend fun setWordMastered(wordId: Long, mastered: Boolean) {
        val record = reviewRecordDao.getByWordId(wordId)?.toDomain()
            ?: EbbinghausScheduler.createInitialRecord(wordId)
        val now = System.currentTimeMillis()
        val updated = if (mastered) {
            record.copy(
                status = ReviewStatus.MASTERED,
                repetitions = 6,
                interval = 30,
                easeFactor = 2.5,
                nextReviewAt = now + TimeUnit.DAYS.toMillis(90),
                lastReviewAt = now
            )
        } else {
            record.copy(
                status = ReviewStatus.REVIEW,
                repetitions = 2,
                interval = 4,
                easeFactor = 2.5,
                nextReviewAt = now,
                lastReviewAt = now
            )
        }
        reviewRecordDao.insert(updated.toEntity())
    }

    suspend fun recordReviewResult(wordId: Long, quality: Int, updateSrs: Boolean = true) {
        if (!updateSrs) return
        val existing = reviewRecordDao.getByWordId(wordId)?.toDomain()
            ?: EbbinghausScheduler.createInitialRecord(wordId)
        val updated = EbbinghausScheduler.recordReview(existing, quality)
        reviewRecordDao.update(updated.toEntity())
    }

    suspend fun getDistractorMeanings(listId: Long, excludeWordId: Long, count: Int): List<String> {
        return wordDao.getRandomFromList(listId, count + 5)
            .filter { it.id != excludeWordId && it.meaning.isNotBlank() }
            .map { it.toDomain().gameMeaning() }
            .distinct()
            .take(count)
    }

    suspend fun getDistractorWords(listId: Long, excludeWordId: Long, count: Int): List<String> {
        return wordDao.getRandomFromList(listId, count + 8)
            .filter { it.id != excludeWordId && it.text.isNotBlank() }
            .map { it.text.trim() }
            .distinctBy { it.lowercase() }
            .take(count)
    }
}
