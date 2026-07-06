package com.englishworder.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MeaningFormatterTest {

    @Test
    fun primary_takesFirstSegment() {
        assertEquals("放弃", MeaningFormatter.primary("放弃；抛弃；遗弃"))
    }

    @Test
    fun short_limitsToFiveChars() {
        assertEquals("放弃", MeaningFormatter.short("放弃 permanently"))
        assertEquals("你好世界", MeaningFormatter.short("你好世界啊"))
    }

    @Test
    fun short_stripsParentheses() {
        assertEquals("运行", MeaningFormatter.short("（动词）运行；操作"))
    }
}
