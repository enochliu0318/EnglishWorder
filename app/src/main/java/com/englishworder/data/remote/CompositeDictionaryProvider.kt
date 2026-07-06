package com.englishworder.data.remote

import com.englishworder.domain.model.WordInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompositeDictionaryProvider @Inject constructor(
    private val youdao: YoudaoDictionaryProvider,
    private val fallback: FreeDictionaryPhoneticProvider
) : DictionaryProvider {

    override suspend fun lookup(word: String): Result<WordInfo> {
        return youdao.lookup(word).recoverCatching {
            fallback.lookup(word).getOrThrow()
        }
    }
}
