package com.englishworder.data.parser

import com.englishworder.domain.model.ParsedWordEntry
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

object WordFileParser {

    private val WORD_HEADERS = setOf("word", "单词", "english", "词汇")
    private val PHONETIC_HEADERS = setOf("phonetic", "音标", "ipa")
    private val MEANING_HEADERS = setOf("meaning", "释义", "翻译", "中文", "definition")
    private val EXAMPLE_HEADERS = setOf("example", "例句", "sentence")

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
                for (cellIndex in 0 until row.lastCellNum) {
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

    private fun parseRows(rows: List<List<String>>): List<ParsedWordEntry> {
        if (rows.isEmpty()) return emptyList()

        val firstRow = rows.first().map { it.trim().lowercase() }
        val hasHeader = firstRow.any { it in WORD_HEADERS }

        val dataRows = if (hasHeader) rows.drop(1) else rows
        val wordIndex = if (hasHeader) findColumnIndex(rows.first(), WORD_HEADERS) else 0
        val phoneticIndex = if (hasHeader) findColumnIndex(rows.first(), PHONETIC_HEADERS) else -1
        val meaningIndex = if (hasHeader) findColumnIndex(rows.first(), MEANING_HEADERS) else -1
        val exampleIndex = if (hasHeader) findColumnIndex(rows.first(), EXAMPLE_HEADERS) else -1

        return dataRows.mapNotNull { row ->
            val text = row.getOrNull(wordIndex)?.trim().orEmpty()
            if (text.isBlank()) return@mapNotNull null
            ParsedWordEntry(
                text = text,
                phonetic = row.getOrNull(phoneticIndex)?.trim()?.takeIf { it.isNotBlank() },
                meaning = row.getOrNull(meaningIndex)?.trim()?.takeIf { it.isNotBlank() },
                example = row.getOrNull(exampleIndex)?.trim()?.takeIf { it.isNotBlank() }
            )
        }.distinctBy { it.text.lowercase() }
    }

    private fun findColumnIndex(headers: List<String>, candidates: Set<String>): Int {
        headers.forEachIndexed { index, header ->
            if (header.trim().lowercase() in candidates) return index
        }
        return -1
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
