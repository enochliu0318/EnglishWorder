package com.englishworder.domain.util

/**
 * 提取最常用释义，并生成游戏用的短释义（默认 ≤5 个汉字）。
 */
object MeaningFormatter {

    private val SPLIT_REGEX = Regex("[；;，,、/|]")
    private val PAREN_REGEX = Regex("[（(][^）)]*[）)]")
    private val LATIN_REGEX = Regex("[a-zA-Z]+")

    /** 取第一个义项，去掉词性标注和英文残留 */
    fun primary(raw: String): String {
        if (raw.isBlank()) return raw
        return raw
            .split(SPLIT_REGEX)
            .firstOrNull()
            .orEmpty()
            .replace(PAREN_REGEX, "")
            .replace(LATIN_REGEX, "")
            .replace(Regex("\\s+"), "")
            .trim()
            .ifBlank { raw.trim() }
    }

    /** 游戏/消消乐用：最多 [maxChars] 个字符 */
    fun short(raw: String, maxChars: Int = 5): String {
        val cleaned = primary(raw)
        if (cleaned.length <= maxChars) return cleaned
        return cleaned.take(maxChars)
    }

    /** 学习卡片用：稍长但仍保持单义项，避免整段翻译 */
    fun forLearning(raw: String, maxChars: Int = 24): String {
        val cleaned = primary(raw)
        if (cleaned.length <= maxChars) return cleaned
        return cleaned.take(maxChars)
    }
}
