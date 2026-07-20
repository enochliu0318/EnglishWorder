package com.englishworder.data.parser

import com.englishworder.domain.model.ParsedWordEntry
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

object WordFileParser {

    private val WORD_HEADERS = setOf("word", "单词", "english", "词汇", "英文")
    private val PHONETIC_HEADERS = setOf("phonetic", "音标", "ipa", "发音")
    private val MEANING_HEADERS = setOf("meaning", "释义", "中文释义", "定义", "definition")
    private val EXAMPLE_HEADERS = setOf("example", "例句", "例句原文", "sentence", "example sentence")
    private val EXAMPLE_TRANSLATION_HEADERS = setOf(
        "例句翻译", "翻译例句", "example translation", "sentence translation", "translation"
    )
    private val POS_HEADERS = setOf("pos", "part of speech", "partofspeech", "词性", "词类")

    /** 从释义开头拆出词性，如 "n. 建议，忠告" → ("n.", "建议，忠告") */
    private val EMBEDDED_POS_REGEX = Regex(
        """^(?i)(n|v|vt|vi|adj|adv|prep|conj|pron|num|art|int|aux|modal|phr\.?\s*v)\.?\s+"""
    )

    fun parseCsv(inputStream: InputStream): List<ParsedWordEntry> {
        val lines = inputStream.bufferedReader().readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) return emptyList()

        val delimiter = if (lines.first().contains('\t')) '\t' else ','
        val rows = lines.map { parseCsvLine(it, delimiter) }
        return parseRows(rows)
    }

    fun parseExcel(inputStream: InputStream): List<ParsedWordEntry> {
        inputStream.use { stream ->
            val workbook = WorkbookFactory.create(stream)
            val sheet = workbook.getSheetAt(0)
            val rows = mutableListOf<List<String>>()

            for (rowIndex in 0..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                val cells = mutableListOf<String>()
                for (cellIndex in 0 until row.lastCellNum.coerceAtLeast(0)) {
                    val cell = row.getCell(cellIndex)
                    cells += when {
                        cell == null -> ""
                        cell.cellType == CellType.STRING -> cell.stringCellValue.trim()
                        cell.cellType == CellType.NUMERIC -> cell.numericCellValue.toLong().toString()
                        cell.cellType == CellType.BOOLEAN -> cell.booleanCellValue.toString()
                        else -> cell.toString().trim()
                    }
                }
                if (cells.any { it.isNotBlank() }) {
                    rows += cells
                }
            }
            workbook.close()
            return parseRows(rows)
        }
    }

    fun parseRows(rows: List<List<String>>): List<ParsedWordEntry> {
        if (rows.isEmpty()) return emptyList()

        val headerRow = rows.first()
        val firstRowNormalized = headerRow.map { it.trim().lowercase() }
        val hasHeader = firstRowNormalized.any { header ->
            WORD_HEADERS.any { candidate -> headerMatches(header, candidate) }
        }

        val dataRows = if (hasHeader) rows.drop(1) else rows
        val wordIndex = if (hasHeader) findColumnIndex(headerRow, WORD_HEADERS) else 0
        val phoneticIndex = if (hasHeader) findColumnIndex(headerRow, PHONETIC_HEADERS) else -1
        val meaningIndex = if (hasHeader) findColumnIndex(headerRow, MEANING_HEADERS) else -1
        val exampleIndex = if (hasHeader) findColumnIndex(headerRow, EXAMPLE_HEADERS) else -1
        val exampleTranslationIndex =
            if (hasHeader) findColumnIndex(headerRow, EXAMPLE_TRANSLATION_HEADERS) else -1
        val posIndex = if (hasHeader) findColumnIndex(headerRow, POS_HEADERS) else -1

        return dataRows.mapNotNull { row ->
            val text = row.getOrNull(wordIndex)?.trim().orEmpty()
            if (text.isBlank()) return@mapNotNull null

            val rawMeaning = row.getOrNull(meaningIndex)?.trim()?.takeIf { it.isNotBlank() }
            val explicitPos = row.getOrNull(posIndex)?.trim()?.takeIf { it.isNotBlank() }
            val (embeddedPos, cleanedMeaning) = splitEmbeddedPos(rawMeaning)
            val exampleEn = row.getOrNull(exampleIndex)?.trim()?.takeIf { it.isNotBlank() }
            val exampleZh = if (exampleTranslationIndex >= 0 && exampleTranslationIndex != exampleIndex) {
                row.getOrNull(exampleTranslationIndex)?.trim()?.takeIf { it.isNotBlank() }
            } else {
                null
            }

            ParsedWordEntry(
                text = text,
                phonetic = row.getOrNull(phoneticIndex)?.trim()?.takeIf { it.isNotBlank() },
                meaning = cleanedMeaning,
                example = combineExample(exampleEn, exampleZh),
                partOfSpeech = explicitPos ?: embeddedPos
            )
        }.distinctBy { it.text.lowercase() }
    }

    /** 公开给测试：从释义拆词性 */
    fun splitEmbeddedPos(rawMeaning: String?): Pair<String?, String?> {
        if (rawMeaning.isNullOrBlank()) return null to null
        val match = EMBEDDED_POS_REGEX.find(rawMeaning.trim()) ?: return null to rawMeaning.trim()
        val tag = match.groupValues[1]
            .lowercase()
            .replace(Regex("""\s+"""), "")
            .let { if (it.endsWith(".")) it else "$it." }
        val rest = rawMeaning.trim().removeRange(match.range).trim()
        return tag to rest.ifBlank { null }
    }

    private fun combineExample(english: String?, chinese: String?): String? {
        return when {
            !english.isNullOrBlank() && !chinese.isNullOrBlank() -> "$english\n$chinese"
            !english.isNullOrBlank() -> english
            !chinese.isNullOrBlank() -> chinese
            else -> null
        }
    }

    private fun findColumnIndex(headers: List<String>, candidates: Set<String>): Int {
        // 优先精确匹配
        headers.forEachIndexed { index, header ->
            val h = header.trim().lowercase()
            if (candidates.any { it == h }) return index
        }
        // 其次：表头包含候选，或候选包含表头（取最长匹配，避免「翻译」误伤「例句翻译」）
        var bestIndex = -1
        var bestScore = -1
        headers.forEachIndexed { index, header ->
            val h = header.trim().lowercase()
            candidates.forEach { candidate ->
                if (headerMatches(h, candidate)) {
                    val score = minOf(h.length, candidate.length)
                    if (score > bestScore) {
                        bestScore = score
                        bestIndex = index
                    }
                }
            }
        }
        return bestIndex
    }

    private fun headerMatches(header: String, candidate: String): Boolean {
        if (header == candidate) return true
        // 避免短泛化词误匹配：候选至少 2 字，且整段包含
        if (candidate.length >= 2 && header.contains(candidate)) return true
        if (header.length >= 2 && candidate.contains(header)) return true
        return false
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        line.forEach { char ->
            when {
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> {
                    result += current.toString().trim()
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result += current.toString().trim()
        return result
    }
}
