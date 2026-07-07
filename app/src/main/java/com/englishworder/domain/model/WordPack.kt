package com.englishworder.domain.model

data class WordPack(
    val id: String,
    val name: String,
    val description: String,
    val assetFile: String,
    val wordCount: Int
)

object BuiltInWordPacks {
    val ALL = listOf(
        WordPack(
            id = "sat_core",
            name = "SAT 核心词汇",
            description = "SAT 考试高频核心词，含释义与例句",
            assetFile = "wordpacks/sat_core.csv",
            wordCount = 86
        ),
        WordPack(
            id = "ielts",
            name = "雅思词汇",
            description = "雅思考试常见学术与生活词汇",
            assetFile = "wordpacks/ielts.csv",
            wordCount = 86
        ),
        WordPack(
            id = "toefl",
            name = "托福词汇",
            description = "托福考试高频学术词汇精选",
            assetFile = "wordpacks/toefl.csv",
            wordCount = 101
        )
    )

    fun find(id: String): WordPack? = ALL.find { it.id == id }
}
