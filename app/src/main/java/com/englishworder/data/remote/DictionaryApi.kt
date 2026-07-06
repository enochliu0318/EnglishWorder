package com.englishworder.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DictionaryResponse(
    val word: String? = null,
    val phonetic: String? = null,
    val phonetics: List<PhoneticDto>? = null,
    val meanings: List<MeaningDto>? = null
)

@JsonClass(generateAdapter = true)
data class PhoneticDto(
    val text: String? = null,
    val audio: String? = null
)

@JsonClass(generateAdapter = true)
data class MeaningDto(
    val partOfSpeech: String? = null,
    val definitions: List<DefinitionDto>? = null
)

@JsonClass(generateAdapter = true)
data class DefinitionDto(
    val definition: String? = null,
    val example: String? = null
)

interface FreeDictionaryApi {
    @retrofit2.http.GET("api/v2/entries/en/{word}")
    suspend fun lookup(@retrofit2.http.Path("word") word: String): List<DictionaryResponse>
}
