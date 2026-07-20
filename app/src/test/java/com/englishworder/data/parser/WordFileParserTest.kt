package com.englishworder.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun parseCsv_posColumn_supported() {
        val csv = "word,pos,meaning,example\nrun,v.,跑,He runs every day.\n"
        val result = WordFileParser.parseCsv(ByteArrayInputStream(csv.toByteArray()))
        assertEquals(1, result.size)
        assertEquals("run", result[0].text)
        assertEquals("v.", result[0].partOfSpeech)
        assertEquals("跑", result[0].meaning)
        assertEquals("He runs every day.", result[0].example)
    }

    @Test
    fun parseCsv_sampleExcelStyle_preservesFullMeaningAndSplitsPos() {
        val csv = """
            单词,音标,释义,例句原文,例句翻译
            counsel,/'kaʊnsl/,n. 建议，忠告；商议,"Blessed is the man who walks not in the counsel of the wicked...","不从恶人的计谋，不站罪人的道路，不坐亵慢人的座位，那便有福…"
            wicked,/'wɪkɪd/,adj. 邪恶的，恶劣的,"Blessed is the man who walks not in the counsel of the wicked...","不从恶人的计谋…"
            perish,/'perɪʃ/,v. 灭亡，毁灭，消亡,"...but the way of the wicked will perish.","……恶人的道路却必灭亡。"
            begotten,/'bɪˈgɒtn/,v. 生，产生 (beget的过去分词),"...today I have begotten you.","……今日我生你。"
        """.trimIndent()

        val result = WordFileParser.parseCsv(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))
        assertEquals(4, result.size)

        val counsel = result[0]
        assertEquals("counsel", counsel.text)
        assertEquals("/'kaʊnsl/", counsel.phonetic)
        assertEquals("n.", counsel.partOfSpeech)
        assertEquals("建议，忠告；商议", counsel.meaning)
        assertTrue(counsel.example!!.contains("Blessed is the man"))
        assertTrue(counsel.example!!.contains("不从恶人的计谋"))

        val wicked = result[1]
        assertEquals("adj.", wicked.partOfSpeech)
        assertEquals("邪恶的，恶劣的", wicked.meaning)

        val perish = result[2]
        assertEquals("v.", perish.partOfSpeech)
        assertEquals("灭亡，毁灭，消亡", perish.meaning)

        val begotten = result[3]
        assertEquals("v.", begotten.partOfSpeech)
        assertEquals("生，产生 (beget的过去分词)", begotten.meaning)
    }

    @Test
    fun splitEmbeddedPos_extractsCommonTags() {
        assertEquals("n." to "建议，忠告；商议", WordFileParser.splitEmbeddedPos("n. 建议，忠告；商议"))
        assertEquals("adj." to "邪恶的，恶劣的", WordFileParser.splitEmbeddedPos("adj. 邪恶的，恶劣的"))
        assertEquals(null to "纯中文释义", WordFileParser.splitEmbeddedPos("纯中文释义"))
        assertNull(WordFileParser.splitEmbeddedPos(null).first)
        assertNull(WordFileParser.splitEmbeddedPos(null).second)
    }

    @Test
    fun parseCsv_exampleOriginalHeader_matched() {
        val csv = "单词,例句原文\nfaith,Have faith.\n"
        val result = WordFileParser.parseCsv(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))
        assertEquals("Have faith.", result[0].example)
    }
}
