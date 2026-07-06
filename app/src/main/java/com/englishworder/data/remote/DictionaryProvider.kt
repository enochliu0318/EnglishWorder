package com.englishworder.data.remote

import com.englishworder.domain.model.WordInfo

interface DictionaryProvider {
    suspend fun lookup(word: String): Result<WordInfo>
}
