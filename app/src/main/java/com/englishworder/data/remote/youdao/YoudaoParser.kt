package com.englishworder.data.remote.youdao

import org.json.JSONObject

data class YoudaoParseResult(
    val meaning: String,
    val shortMeaning: String,
    val phonetic: String,
    val partOfSpeech: String,
    val audioUrl: String
)

/**
 * 从有道词典 JSON 提取简明中文释义（如 red → 红色），不用机器翻译。
 */
object YoudaoParser {

    private val POS_PREFIX = Regex("^(?:[a-z]+\\.|\\s)+", RegexOption.IGNORE_CASE)
    private val PURE_CHINESE = Regex("^[\\u4e00-\\u9fff]{1,8}$")

    fun parse(json: JSONObject, word: String): YoudaoParseResult? {
        val normalized = word.trim().lowercase()
        val fromWeb = parseWebTranslation(json, normalized)
        val fromEc = parseEcWord(json)
        val meaning = fromWeb ?: fromEc ?: return null

        val phonetic = parsePhonetic(json, normalized)
        val audioUrl = "https://dict.youdao.com/dictvoice?audio=$normalized&type=2"
        val partOfSpeech = parsePartOfSpeech(json)

        return YoudaoParseResult(
            meaning = meaning,
            shortMeaning = toShort(meaning),
            phonetic = phonetic,
            partOfSpeech = partOfSpeech,
            audioUrl = audioUrl
        )
    }

    /** web_trans 精确匹配，按热度取最常用中文 */
    private fun parseWebTranslation(json: JSONObject, word: String): String? {
        val array = json.optJSONObject("web_trans")
            ?.optJSONArray("web-translation") ?: return null

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val key = item.optString("key").trim().lowercase()
            if (key != word) continue

            val trans = item.optJSONArray("trans") ?: continue
            val ranked = mutableListOf<Pair<String, Int>>()
            for (j in 0 until trans.length()) {
                val entry = trans.getJSONObject(j)
                val value = entry.optString("value").trim()
                val support = entry.optInt("support", 0)
                if (PURE_CHINESE.matches(value)) {
                    ranked += value to support
                }
            }
            if (ranked.isEmpty()) continue
            ranked.sortByDescending { it.second }
            return pickPreferredChinese(ranked.map { it.first }) ?: ranked.first().first
        }
        return null
    }

    /** 备选：ec 字段第一个中文义项 */
    private fun parseEcWord(json: JSONObject): String? {
        val wordArr = json.optJSONObject("ec")?.optJSONArray("word") ?: return null
        if (wordArr.length() == 0) return null
        val trs = wordArr.getJSONObject(0).optJSONArray("trs") ?: return null
        if (trs.length() == 0) return null

        val raw = trs.getJSONObject(0)
            .optJSONArray("tr")?.getJSONObject(0)
            ?.optJSONObject("l")?.optJSONArray("i")
            ?.optString(0).orEmpty()

        return extractFirstChineseClause(raw)
    }

    private fun extractFirstChineseClause(raw: String): String? {
        if (raw.isBlank()) return null
        val noPos = raw.replace(POS_PREFIX, "").trim()
        val first = noPos.split('，', '；', ';', ',', '、').firstOrNull()?.trim().orEmpty()
        val cleaned = first.replace(Regex("[^\\u4e00-\\u9fff]"), "")
        return cleaned.takeIf { it.isNotBlank() }
    }

    /** 2~4 字优先（红色 优于 红），同分时保留热度更高的（列表靠前） */
    private fun pickPreferredChinese(candidates: List<String>): String? {
        if (candidates.isEmpty()) return null
        val bestScore = candidates.maxOf { chinesePreferenceScore(it) }
        return candidates.first { chinesePreferenceScore(it) == bestScore }
    }

    private fun chinesePreferenceScore(text: String): Int = when (text.length) {
        2, 3, 4 -> 100
        1 -> 80
        5, 6 -> 60
        else -> 0
    }

    private fun toShort(meaning: String): String {
        val cleaned = meaning.replace(Regex("[^\\u4e00-\\u9fff]"), "")
        return if (cleaned.length <= 5) cleaned else cleaned.take(5)
    }

    private fun parsePartOfSpeech(json: JSONObject): String {
        val trs = json.optJSONObject("ec")?.optJSONArray("word")
            ?.optJSONObject(0)?.optJSONArray("trs") ?: return ""
        if (trs.length() == 0) return ""
        val raw = trs.getJSONObject(0)
            .optJSONArray("tr")?.getJSONObject(0)
            ?.optJSONObject("l")?.optJSONArray("i")
            ?.optString(0).orEmpty()
        return extractPosPrefix(raw)
    }

    private fun extractPosPrefix(raw: String): String {
        val match = Regex("^(\\w+\\.)").find(raw.trim()) ?: return ""
        return match.groupValues[1]
    }

    private fun parsePhonetic(json: JSONObject, word: String): String {
        val ecWord = json.optJSONObject("ec")?.optJSONArray("word")
        if (ecWord != null && ecWord.length() > 0) {
            val entry = ecWord.getJSONObject(0)
            val uk = entry.optString("ukphone").trim()
            val us = entry.optString("usphone").trim()
            val phone = when {
                uk.isNotBlank() && !uk.equals(word, ignoreCase = true) -> uk
                us.isNotBlank() && !us.equals(word, ignoreCase = true) -> us
                uk.isNotBlank() -> uk
                else -> us
            }
            if (phone.isNotBlank() && !phone.equals(word, ignoreCase = true)) {
                return if (phone.startsWith("/")) phone else "/$phone/"
            }
        }
        return ""
    }
}
