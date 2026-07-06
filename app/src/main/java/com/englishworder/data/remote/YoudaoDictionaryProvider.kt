package com.englishworder.data.remote

import com.englishworder.data.remote.youdao.YoudaoJsonApi
import com.englishworder.data.remote.youdao.YoudaoParser
import com.englishworder.domain.model.WordInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoudaoDictionaryProvider @Inject constructor(
    private val youdaoApi: YoudaoJsonApi,
    private val freeDictionaryApi: FreeDictionaryApi
) : DictionaryProvider {

    override suspend fun lookup(word: String): Result<WordInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val normalized = word.trim().lowercase()
            val body = youdaoApi.lookup(normalized).string()
            val parsed = YoudaoParser.parse(JSONObject(body), normalized)
                ?: error("未找到 '$word' 的中文释义")

            var phonetic = parsed.phonetic
            var audioUrl = parsed.audioUrl

            if (phonetic.isBlank()) {
                val fallback = fetchPhoneticAndAudio(normalized)
                phonetic = fallback.first.ifBlank { phonetic }
                if (audioUrl.isBlank()) audioUrl = fallback.second
            }

            WordInfo(
                phonetic = phonetic,
                meaning = parsed.meaning,
                shortMeaning = parsed.shortMeaning,
                example = "",
                partOfSpeech = parsed.partOfSpeech,
                audioUrl = audioUrl
            )
        }
    }

    private suspend fun fetchPhoneticAndAudio(word: String): Pair<String, String> {
        return runCatching {
            val response = freeDictionaryApi.lookup(word).firstOrNull() ?: return@runCatching "" to ""
            val phonetic = response.phonetic
                ?: response.phonetics?.firstOrNull { !it.text.isNullOrBlank() }?.text
                ?: ""
            val audioUrl = response.phonetics
                ?.firstOrNull { !it.audio.isNullOrBlank() }
                ?.audio
                ?: ""
            phonetic to audioUrl
        }.getOrDefault("" to "")
    }
}
