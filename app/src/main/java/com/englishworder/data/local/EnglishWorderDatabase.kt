package com.englishworder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.englishworder.data.local.dao.ReviewRecordDao
import com.englishworder.data.local.dao.WordDao
import com.englishworder.data.local.dao.WordListDao
import com.englishworder.data.local.entity.ReviewRecordEntity
import com.englishworder.data.local.entity.WordEntity
import com.englishworder.data.local.entity.WordListEntity
import com.englishworder.domain.model.FetchStatus
import com.englishworder.domain.model.ReviewStatus

class Converters {
    @TypeConverter
    fun fromFetchStatus(value: FetchStatus): String = value.name

    @TypeConverter
    fun toFetchStatus(value: String): FetchStatus = FetchStatus.valueOf(value)

    @TypeConverter
    fun fromReviewStatus(value: ReviewStatus): String = value.name

    @TypeConverter
    fun toReviewStatus(value: String): ReviewStatus = ReviewStatus.valueOf(value)
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE words ADD COLUMN shortMeaning TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE words ADD COLUMN partOfSpeech TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [
        WordListEntity::class,
        WordEntity::class,
        ReviewRecordEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EnglishWorderDatabase : RoomDatabase() {
    abstract fun wordListDao(): WordListDao
    abstract fun wordDao(): WordDao
    abstract fun reviewRecordDao(): ReviewRecordDao
}
