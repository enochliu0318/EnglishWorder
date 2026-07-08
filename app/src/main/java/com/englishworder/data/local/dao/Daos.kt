package com.englishworder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.englishworder.data.local.entity.ReviewRecordEntity
import com.englishworder.data.local.entity.WordEntity
import com.englishworder.data.local.entity.WordListEntity
import com.englishworder.data.local.entity.WordListWithCountEntity
import com.englishworder.data.local.entity.WordWithReviewEntity
import com.englishworder.domain.model.FetchStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface WordListDao {
    @Query("SELECT * FROM word_lists ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WordListEntity>>

    @Query(
        """
        SELECT wl.id, wl.name, wl.description, wl.packId, wl.createdAt, COUNT(w.id) AS wordCount
        FROM word_lists wl
        LEFT JOIN words w ON wl.id = w.listId
        GROUP BY wl.id
        ORDER BY wl.createdAt DESC
        """
    )
    fun observeAllWithCount(): Flow<List<WordListWithCountEntity>>

    @Query("SELECT * FROM word_lists WHERE id = :id")
    suspend fun getById(id: Long): WordListEntity?

    @Query("SELECT * FROM word_lists WHERE packId = :packId LIMIT 1")
    suspend fun getByPackId(packId: String): WordListEntity?

    @Query("SELECT packId FROM word_lists WHERE packId IS NOT NULL")
    fun observeInstalledPackIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WordListEntity): Long

    @Update
    suspend fun update(entity: WordListEntity)

    @Query("DELETE FROM word_lists WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE listId = :listId ORDER BY createdAt DESC")
    fun observeByListId(listId: Long): Flow<List<WordEntity>>

    @Query(
        """
        SELECT w.*, r.easeFactor, r.interval, r.repetitions, r.nextReviewAt, r.lastReviewAt, r.status
        FROM words w
        INNER JOIN review_records r ON w.id = r.wordId
        WHERE w.listId = :listId
        ORDER BY w.createdAt DESC
        """
    )
    fun observeWithReviewByListId(listId: Long): Flow<List<WordWithReviewEntity>>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getById(id: Long): WordEntity?

    @Query("SELECT * FROM words WHERE listId = :listId AND text = :text LIMIT 1")
    suspend fun getByText(listId: Long, text: String): WordEntity?

    @Query("SELECT COUNT(*) FROM words WHERE listId = :listId")
    fun observeCountByListId(listId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE listId = :listId")
    suspend fun countByListId(listId: Long): Int

    @Query("SELECT * FROM words WHERE fetchStatus = :status")
    suspend fun getByFetchStatus(status: FetchStatus): List<WordEntity>

    @Query("SELECT * FROM words WHERE listId = :listId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomFromList(listId: Long, limit: Int): List<WordEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: WordEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<WordEntity>): List<Long>

    @Update
    suspend fun update(entity: WordEntity)

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ReviewRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReviewRecordEntity)

    @Update
    suspend fun update(entity: ReviewRecordEntity)

    @Query("SELECT * FROM review_records WHERE wordId = :wordId")
    suspend fun getByWordId(wordId: Long): ReviewRecordEntity?

    @Query(
        """
        SELECT w.*, r.easeFactor, r.interval, r.repetitions, r.nextReviewAt, r.lastReviewAt, r.status
        FROM words w
        INNER JOIN review_records r ON w.id = r.wordId
        WHERE r.nextReviewAt <= :beforeTime AND r.status != 'MASTERED'
        ORDER BY r.nextReviewAt ASC
        """
    )
    fun observeDueReviews(beforeTime: Long): Flow<List<WordWithReviewEntity>>

    @Query(
        """
        SELECT w.*, r.easeFactor, r.interval, r.repetitions, r.nextReviewAt, r.lastReviewAt, r.status
        FROM words w
        INNER JOIN review_records r ON w.id = r.wordId
        WHERE r.nextReviewAt <= :beforeTime AND r.status != 'MASTERED'
        ORDER BY r.nextReviewAt ASC
        LIMIT :limit
        """
    )
    suspend fun getDueReviews(beforeTime: Long, limit: Int): List<WordWithReviewEntity>

    @Query(
        """
        SELECT w.*, r.easeFactor, r.interval, r.repetitions, r.nextReviewAt, r.lastReviewAt, r.status
        FROM words w
        INNER JOIN review_records r ON w.id = r.wordId
        WHERE w.listId = :listId
            AND r.nextReviewAt <= :beforeTime
            AND r.status != 'MASTERED'
        ORDER BY r.nextReviewAt ASC
        LIMIT :limit
        """
    )
    suspend fun getDueReviewsForList(listId: Long, beforeTime: Long, limit: Int): List<WordWithReviewEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM words w
        INNER JOIN review_records r ON w.id = r.wordId
        WHERE w.listId = :listId
            AND r.nextReviewAt <= :beforeTime
            AND r.status != 'MASTERED'
        """
    )
    suspend fun countDueForList(listId: Long, beforeTime: Long): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM words w
        INNER JOIN review_records r ON w.id = r.wordId
        WHERE w.listId = :listId AND r.status = 'NEW'
        """
    )
    suspend fun countNewForList(listId: Long): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM review_records
        WHERE nextReviewAt <= :beforeTime AND status != 'MASTERED'
        """
    )
    fun observeDueCount(beforeTime: Long): Flow<Int>

    @Query(
        """
        SELECT w.*, r.easeFactor, r.interval, r.repetitions, r.nextReviewAt, r.lastReviewAt, r.status
        FROM words w
        INNER JOIN review_records r ON w.id = r.wordId
        WHERE w.listId = :listId AND r.status = 'NEW'
        ORDER BY w.createdAt ASC
        LIMIT :limit
        """
    )
    suspend fun getNewWords(listId: Long, limit: Int): List<WordWithReviewEntity>

    @Query(
        """
        SELECT w.*, r.easeFactor, r.interval, r.repetitions, r.nextReviewAt, r.lastReviewAt, r.status
        FROM words w
        INNER JOIN review_records r ON w.id = r.wordId
        WHERE w.listId = :listId
        ORDER BY RANDOM()
        LIMIT :limit
        """
    )
    suspend fun getAllWordsWithReview(listId: Long, limit: Int): List<WordWithReviewEntity>

    @Query(
        """
        SELECT w.*, r.easeFactor, r.interval, r.repetitions, r.nextReviewAt, r.lastReviewAt, r.status
        FROM words w
        INNER JOIN review_records r ON w.id = r.wordId
        ORDER BY RANDOM()
        LIMIT :limit
        """
    )
    suspend fun getAllWordsWithReviewGlobal(limit: Int): List<WordWithReviewEntity>

    @Query("SELECT status, COUNT(*) AS count FROM review_records GROUP BY status")
    fun observeStatusCounts(): Flow<List<com.englishworder.data.local.entity.ReviewStatusCountEntity>>

    @Query(
        """
        SELECT w.listId AS listId, wl.name AS listName, COUNT(*) AS total,
            SUM(CASE WHEN r.status = 'NEW' THEN 1 ELSE 0 END) AS newCount,
            SUM(CASE WHEN r.status = 'LEARNING' THEN 1 ELSE 0 END) AS learningCount,
            SUM(CASE WHEN r.status = 'REVIEW' THEN 1 ELSE 0 END) AS reviewCount,
            SUM(CASE WHEN r.status = 'MASTERED' THEN 1 ELSE 0 END) AS masteredCount,
            SUM(CASE WHEN r.nextReviewAt <= :endOfDay AND r.status != 'MASTERED' THEN 1 ELSE 0 END) AS dueToday
        FROM words w
        INNER JOIN review_records r ON w.id = r.wordId
        INNER JOIN word_lists wl ON w.listId = wl.id
        GROUP BY w.listId, wl.name
        ORDER BY wl.createdAt DESC
        """
    )
    fun observeListProgress(endOfDay: Long): Flow<List<com.englishworder.data.local.entity.ListProgressEntity>>

    @Query(
        """
        SELECT CAST((r.nextReviewAt - :startOfToday) / 86400000 AS INTEGER) AS dayOffset, COUNT(*) AS count
        FROM review_records r
        WHERE r.status != 'MASTERED'
            AND r.nextReviewAt >= :startOfToday
            AND r.nextReviewAt < :weekEnd
        GROUP BY dayOffset
        ORDER BY dayOffset ASC
        """
    )
    fun observeUpcomingReviews(startOfToday: Long, weekEnd: Long): Flow<List<com.englishworder.data.local.entity.DayReviewCountEntity>>
}
