package com.englishworder.data.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class WordFileParserTest {

    @Test
    fun parseCsv_withHeader_extractsWords() {
        val csv = """
            word,meaning
            hello,greeting
            world,earth
        """.trimIndent()
        val result = WordFileParser.parseCsv(ByteArrayInputStream(csv.toByteArray()))
        assertEquals(2, result.size)
        assertEquals("hello", result[0].text)
        assertEquals("greeting", result[0].meaning)
    }

    @Test
    fun parseCsv_withoutHeader_usesFirstColumn() {
        val csv = "apple\nbanana\n"
        val result = WordFileParser.parseCsv(ByteArrayInputStream(csv.toByteArray()))
        assertEquals(2, result.size)
        assertEquals("apple", result[0].text)
    }

    @Test
    fun parseCsv_chineseHeader_supported() {
        val csv = "单词,释义\nabandon,放弃\n"
        val result = WordFileParser.parseCsv(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))
        assertEquals(1, result.size)
        assertEquals("abandon", result[0].text)
        assertEquals("放弃", result[0].meaning)
    }
}
