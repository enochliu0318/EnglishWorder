package com.englishworder.data.remote

import com.englishworder.domain.model.WordInfo
import com.englishworder.domain.util.MeaningFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 仅作有道查词失败时的兜底：音标来自 Free Dictionary，释义仍尽量简短。
 */
@Singleton
class FreeDictionaryPhoneticProvider @Inject constructor(
    private val api: FreeDictionaryApi
) : DictionaryProvider {

    override suspend fun lookup(word: String): Result<WordInfo> = runCatching {
        val responses = api.lookup(word.trim().lowercase())
        val response = responses.firstOrNull()
            ?: error("No definition found for '$word'")

        val phonetic = response.phonetic
            ?: response.phonetics?.firstOrNull { !it.text.isNullOrBlank() }?.text
            ?: ""

        val audioUrl = response.phonetics
            ?.firstOrNull { !it.audio.isNullOrBlank() }
            ?.audio
            ?: ""

        val firstEnglish = response.meanings
            ?.firstOrNull()
            ?.definitions
            ?.firstOrNull()
            ?.definition
            ?.trim()
            .orEmpty()

        if (firstEnglish.isBlank()) error("No meaning found for '$word'")

        val meaning = MeaningFormatter.forLearning(firstEnglish)
        val shortMeaning = MeaningFormatter.short(firstEnglish)

        val exampleEn = extractExample(response)

        WordInfo(
            phonetic = phonetic,
            meaning = meaning,
            shortMeaning = shortMeaning,
            example = exampleEn,
            audioUrl = audioUrl
        )
    }

    private fun extractExample(response: DictionaryResponse): String {
        response.meanings?.forEach { meaning ->
            meaning.definitions?.forEach { def ->
                if (!def.example.isNullOrBlank()) return def.example
            }
        }
        return ""
    }
}
