package com.tanghuidev.idea.plugin

import com.tanghuidev.idea.plugin.common.service.IconPatchersFactory
import com.tanghuidev.idea.plugin.common.utils.crypto.HybridDecryptor
import com.tanghuidev.idea.plugin.common.utils.crypto.HybridEncryptor
import com.tanghuidev.idea.plugin.common.utils.crypto.RSAKeyGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test


/**
 * @BelongsPackage: com.tanghuidev.idea.plugin
 * @Author: 唐煇
 * @CreateTime: 2025-11-28-10:35
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
class IconPatchersFactoryTest {
    @Test
    fun `test create returns parsed IconPathPatchers`() {
        val factory = IconPatchersFactory()
        val result = factory.create()
        val map: Map<String, String> = result.iconPatchers.associate { it.originalPath to it.expUIPath }
        // 基本断言
        assertNotNull(result)
        assertTrue(result.iconPatchers.isNotEmpty(), "patchers should not be empty")

        // 具体断言（根据你的 XML 调整）
        assertEquals(listOf("aaa", "bbb"), result.iconPatchers)
    }


    @Test
    fun `test` () {

        val data = "apexsoft@123"
        println(data)
        // 加密
        val encrypt = HybridEncryptor.encrypt(data)
        println(encrypt)
        // 解密
        val result =
            HybridDecryptor.decrypt(encrypt)
        println(result)
    }

    @Test
    fun `testDecrypt` () {
        val data = "eyJkYXRhIjoiVFJ1ci8waG5wNC9RakU1aFZzNzZFUkRadmVscGM0VlRueTUvMHc9PSIsIml2IjoiR1YvdDBicnBucFpXZWtoRyIsImtleSI6ImlOdUJ3NzI0L2F2N1FZbm9HQlRSeklKclpYdWFnWDFPRVhoVDdXVTBOZzZRSXMwZlo4UW84RnlpcjdJRThtY3FyWEF6aWtqSEoxT3RuVzIzS1FsU0h6KzIrb1g4ZVlKamZLMjgya2NXNnh3NkxXenMrcjhOTnEwTHgxMG5nL3FhUTc1c3djRGJrTnM2U1lQMG0vdzBkUDlSdWRNYzNIVUI1U01RTnQrSHJkTzBEb3RnTTRXMTFUM3NKQlVlYjF2bytNek03K1RDWWl1N294NTRJT2o1citjc1Z6VXZadHgySFVFODZPelpoa2JHZEltSm9lK2NseUI5MmVyUmpPem5pWFlNeEZtTDN6MENpTXp3Q2l5cCtLdjN4YkphM0ErTFJONmdTN2tjNk4xdkhIUXliVjAvcldpUm1ka0IrNXRJc0RzS0M5Nk9yZjNkWW44RHhLdlJPZz09In0="
        // 解密
        val result =
            HybridDecryptor.decrypt(data)
        println(result)
    }
}