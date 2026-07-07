package com.englishworder.data.repository

import android.content.Context
import com.englishworder.data.parser.WordFileParser
import com.englishworder.domain.model.BuiltInWordPacks
import com.englishworder.domain.model.WordPack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordPackRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wordRepository: WordRepository
) {

    fun availablePacks(): List<WordPack> = BuiltInWordPacks.ALL

    fun observeInstalledPackIds(): Flow<Set<String>> = wordRepository.observeInstalledPackIds()

    suspend fun isInstalled(packId: String): Boolean =
        wordRepository.getWordListByPackId(packId) != null

    suspend fun installPack(packId: String): Result<Long> = runCatching {
        if (isInstalled(packId)) {
            error("该词库已添加")
        }
        val pack = BuiltInWordPacks.find(packId) ?: error("词库不存在")
        val entries = context.assets.open(pack.assetFile).use { stream ->
            WordFileParser.parseCsv(stream)
        }
        if (entries.isEmpty()) error("词库文件为空")

        val listId = wordRepository.createWordList(
            name = pack.name,
            description = pack.description,
            packId = pack.id
        )
        val result = wordRepository.importWords(listId, entries)
        if (result.imported == 0 && result.updated == 0) {
            wordRepository.deleteWordList(listId)
            error("导入失败，请重试")
        }
        listId
    }
}
