package com.englishworder.di

import android.content.Context
import androidx.room.Room
import com.englishworder.data.local.MIGRATION_1_2
import com.englishworder.data.local.MIGRATION_2_3
import com.englishworder.data.local.EnglishWorderDatabase
import com.englishworder.data.local.dao.ReviewRecordDao
import com.englishworder.data.local.dao.WordDao
import com.englishworder.data.local.dao.WordListDao
import com.englishworder.data.remote.CompositeDictionaryProvider
import com.englishworder.data.remote.DictionaryProvider
import com.englishworder.data.remote.FreeDictionaryApi
import com.englishworder.data.remote.youdao.YoudaoJsonApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideFreeDictionaryApi(
        moshi: Moshi,
        okHttpClient: OkHttpClient
    ): FreeDictionaryApi {
        return Retrofit.Builder()
            .baseUrl("https://api.dictionaryapi.dev/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FreeDictionaryApi::class.java)
    }

    @Provides
    @Singleton
    fun provideYoudaoJsonApi(okHttpClient: OkHttpClient): YoudaoJsonApi {
        return Retrofit.Builder()
            .baseUrl("https://dict.youdao.com/")
            .client(okHttpClient)
            .build()
            .create(YoudaoJsonApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EnglishWorderDatabase {
        return Room.databaseBuilder(
            context,
            EnglishWorderDatabase::class.java,
            "englishworder.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }

    @Provides
    fun provideWordListDao(db: EnglishWorderDatabase): WordListDao = db.wordListDao()

    @Provides
    fun provideWordDao(db: EnglishWorderDatabase): WordDao = db.wordDao()

    @Provides
    fun provideReviewRecordDao(db: EnglishWorderDatabase): ReviewRecordDao = db.reviewRecordDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    @Singleton
    abstract fun bindDictionaryProvider(
        impl: CompositeDictionaryProvider
    ): DictionaryProvider
}
