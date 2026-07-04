package com.sleep.snore.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DataExporter.csv() 顶层 internal 函数的 CSV 注入防护单元测试。
 *
 * csv() 实现规则（来自源码）：
 * 1. 任意字段都以双引号包裹
 * 2. 字段内的双引号转义为 ""
 * 3. 以 = + - @ 开头的字段前缀 ' 防止 Excel 公式注入
 * 4. null 视为空字符串
 */
class DataExporterCsvTest {

    // ===== 用例 1：含逗号的字段被双引号包裹 =====
    @Test
    fun `含逗号的字段被双引号包裹`() {
        // 含逗号的字段若不包裹会破坏 CSV 列结构，应在双引号内安全输出
        val result = csv("a,b")
        assertEquals("\"a,b\"", result)
    }

    // ===== 用例 2：含双引号的字段被转义并包裹 =====
    @Test
    fun `含双引号的字段被转义并包裹`() {
        // 字段内的 " 必须转义为 ""，避免提前结束字段引号
        val result = csv("a\"b")
        assertEquals("\"a\"\"b\"", result)
    }

    // ===== 用例 3：以 = + - @ 开头的字段添加单引号前缀防注入 =====
    @Test
    fun `以等号开头的字段添加单引号前缀防注入`() {
        // =cmd|/c calc! 在 Excel 中会被解释为公式执行，前缀 ' 阻止公式解析
        val result = csv("=cmd|/c calc!")
        assertEquals("\"'=cmd|/c calc!\"", result)
    }

    @Test
    fun `以加号开头的字段添加单引号前缀防注入`() {
        val result = csv("+1+1")
        assertEquals("\"'+1+1\"", result)
    }

    @Test
    fun `以减号开头的字段添加单引号前缀防注入`() {
        val result = csv("-1-1")
        assertEquals("\"'-1-1\"", result)
    }

    @Test
    fun `以at符号开头的字段添加单引号前缀防注入`() {
        val result = csv("@SUM(A1:A2)")
        assertEquals("\"'@SUM(A1:A2)\"", result)
    }

    // ===== 用例 4：普通字段原样输出（实现上同样被双引号包裹）=====
    @Test
    fun `普通字段原样输出但被双引号包裹`() {
        // csv() 实现对所有字段都加双引号，普通字段也不例外
        val result = csv("normal")
        assertEquals("\"normal\"", result)
    }

    // ===== 补充用例：边界情况 =====
    @Test
    fun `null字段输出为空双引号`() {
        val result = csv(null)
        assertEquals("\"\"", result)
    }

    @Test
    fun `空字符串输出为空双引号`() {
        val result = csv("")
        assertEquals("\"\"", result)
    }

    @Test
    fun `含换行符的字段被双引号包裹`() {
        // 换行符在双引号内不会破坏 CSV 行结构
        val result = csv("a\nb")
        assertEquals("\"a\nb\"", result)
    }

    @Test
    fun `数字类型字段被转换为字符串`() {
        val result = csv(42)
        assertEquals("\"42\"", result)
    }

    @Test
    fun `同时含公式前缀与双引号的字段同时应用两层防护`() {
        // =a"b 既要前缀 ' 防注入，又要将 " 转义为 ""
        val result = csv("=a\"b")
        assertEquals("\"'=a\"\"b\"", result)
    }
}
