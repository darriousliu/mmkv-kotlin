package com.ctrip.flight.mmkv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

class MMKVImplTest {
    // $HOME/.cache
    val tempFolder = File(System.getProperty("user.home")).resolve(".cache")

    private lateinit var mmkvRootDir: File
    private lateinit var mmkv: MMKV_KMP

    @BeforeTest
    fun setup() {
        mmkvRootDir = tempFolder.resolve("mmkv-test")
        initialize(mmkvRootDir.absolutePath)
        mmkv = defaultMMKV()
    }

    @AfterTest
    fun tearDown() {
        mmkv.clearAll()
        mmkv.clearMemoryCache()
    }

    // 字符串值测试
    @Test
    fun testStringOperations() {
        // 设置和获取字符串
        val key = "stringKey"
        val value = "Hello MMKV"

        assertTrue(mmkv.set(key, value))
        assertEquals(value, mmkv.takeString(key, ""))

        // 默认值测试
        assertEquals("default", mmkv.takeString("nonExistentKey", "default"))

        // 更新值
        val newValue = "Updated MMKV String"
        assertTrue(mmkv.set(key, newValue))
        assertEquals(newValue, mmkv.takeString(key, ""))

        // 移除值
        mmkv.removeValueForKey(key)
        assertEquals("", mmkv.takeString(key, ""))
    }

    // 布尔值测试
    @Test
    fun testBooleanOperations() {
        val key = "boolKey"

        assertTrue(mmkv.set(key, true))
        assertTrue(mmkv.takeBoolean(key, false))

        assertTrue(mmkv.set(key, false))
        assertFalse(mmkv.takeBoolean(key, true))

        // 默认值测试
        assertTrue(mmkv.takeBoolean("nonExistentKey", true))
    }

    // 整数值测试
    @Test
    fun testIntOperations() {
        val key = "intKey"
        val value = 42

        assertTrue(mmkv.set(key, value))
        assertEquals(value, mmkv.takeInt(key, 0))

        // 边界值测试
        assertTrue(mmkv.set(key, Int.MAX_VALUE))
        assertEquals(Int.MAX_VALUE, mmkv.takeInt(key, 0))

        assertTrue(mmkv.set(key, Int.MIN_VALUE))
        assertEquals(Int.MIN_VALUE, mmkv.takeInt(key, 0))
    }

    // 长整型测试
    @Test
    fun testLongOperations() {
        val key = "longKey"
        val value = 1234567890L

        assertTrue(mmkv.set(key, value))
        assertEquals(value, mmkv.takeLong(key, 0))

        // 边界值测试
        assertTrue(mmkv.set(key, Long.MAX_VALUE))
        assertEquals(Long.MAX_VALUE, mmkv.takeLong(key, 0))

        assertTrue(mmkv.set(key, Long.MIN_VALUE))
        assertEquals(Long.MIN_VALUE, mmkv.takeLong(key, 0))
    }

    // 浮点数测试
    @Test
    fun testFloatOperations() {
        val key = "floatKey"
        val value = 3.14159f

        assertTrue(mmkv.set(key, value))
        assertEquals(value, mmkv.takeFloat(key, 0f))

        // 特殊值测试
        assertTrue(mmkv.set(key, Float.MAX_VALUE))
        assertEquals(Float.MAX_VALUE, mmkv.takeFloat(key, 0f))

        assertTrue(mmkv.set(key, Float.MIN_VALUE))
        assertEquals(Float.MIN_VALUE, mmkv.takeFloat(key, 0f))
    }

    // 双精度浮点数测试
    @Test
    fun testDoubleOperations() {
        val key = "doubleKey"
        val value = 2.718281828459045

        assertTrue(mmkv.set(key, value))
        assertEquals(value, mmkv.takeDouble(key, 0.0))

        // 特殊值测试
        assertTrue(mmkv.set(key, Double.MAX_VALUE))
        assertEquals(Double.MAX_VALUE, mmkv.takeDouble(key, 0.0))

        assertTrue(mmkv.set(key, Double.MIN_VALUE))
        assertEquals(Double.MIN_VALUE, mmkv.takeDouble(key, 0.0))
    }

    // 字节数组测试
    @Test
    fun testByteArrayOperations() {
        val key = "byteArrayKey"
        val value = byteArrayOf(-1, 2, -3, 4, -5)

        assertTrue(mmkv.set(key, value))
        assertContentEquals(value, mmkv.takeByteArray(key, null))

        // 空数组测试
        val emptyArray = byteArrayOf()
        assertTrue(mmkv.set(key, emptyArray))
        assertContentEquals(emptyArray, mmkv.takeByteArray(key, null))

        // 默认值测试
        val defaultArray = byteArrayOf(9, 8, 7)
        assertContentEquals(defaultArray, mmkv.takeByteArray("nonExistentKey", defaultArray))
    }

    // 无符号整型测试
    @Test
    fun testUIntOperations() {
        val key = "uintKey"
        val value = 42u

        assertTrue(mmkv.set(key, value))
        assertEquals(value, mmkv.takeUInt(key, 0u))

        // 边界值测试
        assertTrue(mmkv.set(key, UInt.MAX_VALUE))
        assertEquals(UInt.MAX_VALUE, mmkv.takeUInt(key, 0u))

        assertTrue(mmkv.set(key, UInt.MIN_VALUE))
        assertEquals(UInt.MIN_VALUE, mmkv.takeUInt(key, 0u))
    }

    // 无符号长整型测试
    @Test
    fun testULongOperations() {
        val key = "ulongKey"
        val value = 1234567890uL

        assertTrue(mmkv.set(key, value))
        assertEquals(value, mmkv.takeULong(key, 0uL))

        // 边界值测试
        assertTrue(mmkv.set(key, ULong.MAX_VALUE))
        assertEquals(ULong.MAX_VALUE, mmkv.takeULong(key, 0uL))

        assertTrue(mmkv.set(key, ULong.MIN_VALUE))
        assertEquals(ULong.MIN_VALUE, mmkv.takeULong(key, 0uL))
    }

    // 字符串集合测试
    @Test
    fun testStringSetOperations() {
        val key = "stringSetKey"
        val value = setOf("one", "two", "three", "one")

        assertTrue(mmkv.set(key, value))
        assertEquals(value, mmkv.takeStringSet(key, null))

        // 空集合测试
        val emptySet = emptySet<String>()
        assertTrue(mmkv.set(key, emptySet))
        assertEquals(emptySet, mmkv.takeStringSet(key, null))

        // 默认值测试
        val defaultSet = setOf("default")
        assertEquals(defaultSet, mmkv.takeStringSet("nonExistentKey", defaultSet))

        // null值测试
        assertTrue(mmkv.set(key, null as Set<String>?))
        assertNull(mmkv.takeStringSet(key, null))
    }

    // 移除多个键测试
    @Test
    fun testRemoveValuesForKeys() {
        // 设置多个键值对
        mmkv["key1"] = "value1"
        mmkv["key2"] = 2
        mmkv["key3"] = true

        // 验证设置成功
        assertEquals("value1", mmkv.takeString("key1", ""))
        assertEquals(2, mmkv.takeInt("key2", 0))
        assertTrue(mmkv.takeBoolean("key3", false))

        // 批量移除
        mmkv.removeValuesForKeys(listOf("key1", "key3"))

        // 验证被移除的键
        assertEquals("", mmkv.takeString("key1", ""))
        assertTrue(!mmkv.containsKey("key1"))
        assertEquals(2, mmkv.takeInt("key2", 0)) // 未移除
        assertFalse(mmkv.takeBoolean("key3", false))
        assertTrue(!mmkv.containsKey("key3"))
    }

    // 获取所有键测试
    @Test
    fun testAllKeys() {
        // 清除所有可能存在的键
        mmkv.clearAll()

        // 设置多个键值对
        mmkv["key1"] = "value1"
        mmkv["key2"] = 2
        mmkv["key3"] = true

        // 获取所有键并验证
        val keys = mmkv.allKeys()
        assertEquals(3, keys.size)
        assertTrue(keys.containsAll(listOf("key1", "key2", "key3")))

        // 移除一个键后再验证
        mmkv.removeValueForKey("key2")
        val updatedKeys = mmkv.allKeys()
        assertEquals(2, updatedKeys.size)
        assertTrue(updatedKeys.containsAll(listOf("key1", "key3")))
        assertFalse(updatedKeys.contains("key2"))
    }

    // 键存在性测试
    @Test
    fun testContainsKey() {
        val key = "testKey"

        // 初始状态下键不存在
        assertFalse(mmkv.containsKey(key))

        // 设置值后键存在
        mmkv[key] = "value"
        assertTrue(mmkv.containsKey(key))

        // 移除后键不存在
        mmkv.removeValueForKey(key)
        assertFalse(mmkv.containsKey(key))
    }

    // 存储大小测试
    @Test
    fun testStorageSize() {
        // 清空存储
        mmkv.clearAll()

        // 初始状态
        assertEquals(0, mmkv.count)

        // 添加数据
        mmkv["key1"] = "value1"
        assertEquals(1, mmkv.count)

        mmkv["key2"] = "value2"
        assertEquals(2, mmkv.count)

        // 验证总大小和实际大小非零
        assertTrue(mmkv.totalSize > 0)
        assertTrue(mmkv.actualSize > 0)

        // 移除数据
        mmkv.removeValueForKey("key1")
        assertEquals(1, mmkv.count)

        // 清空所有
        mmkv.clearAll()
        assertEquals(0, mmkv.count)
    }

    // 清理内存缓存测试
    @Test
    fun testClearMemoryCache() {
        // 设置一些值
        mmkv["cacheKey1"] = "value1"
        mmkv["cacheKey2"] = 42

        // 清理内存缓存
        mmkv.clearMemoryCache()

        // 验证值仍然可以读取（从磁盘读取）
        assertEquals("value1", mmkv.takeString("cacheKey1", ""))
        assertEquals(42, mmkv.takeInt("cacheKey2", 0))
    }

    // 加密测试
    @Test
    fun testEncryption() {
        // 使用加密密钥创建MMKV实例
        val encryptedId = "encryptedMMKV"
        val cryptKey = "my-secret-key"
        val encryptedMMKV = mmkvWithID(encryptedId, MMKVMode.SINGLE_PROCESS, cryptKey)

        try {
            // 写入加密数据
            encryptedMMKV["secretKey"] = "secretValue"

            // 读取加密数据
            assertEquals("secretValue", encryptedMMKV.takeString("secretKey", ""))

            // 重设加密密钥
            encryptedMMKV.checkReSetCryptKey("new-secret-key")

            // 验证仍然可以访问之前的数据
            assertEquals("secretValue", encryptedMMKV.takeString("secretKey", ""))
        } finally {
            encryptedMMKV.close()
        }
    }

    // MMKV ID测试
    @Test
    fun testMMAPID() {
        val customId = "customMMKV"
        val customMMKV = mmkvWithID(customId)

        try {
            // 验证MMKV ID
            assertEquals(customId, customMMKV.mmapID())
        } finally {
            customMMKV.close()
        }
    }

    // 同步操作测试
    @Test
    fun testSync() {
        // 设置一些数据
        mmkv["syncKey"] = "syncValue"

        // 执行同步
        mmkv.sync()

        // 验证数据仍然可用
        assertEquals("syncValue", mmkv.takeString("syncKey", ""))
    }

    // 异步操作测试
    @Test
    fun testAsync() {
        // 设置一些数据
        mmkv["asyncKey"] = "asyncValue"

        // 执行异步操作
        mmkv.async()

        // 延迟一小段时间确保异步操作完成
        Thread.sleep(100)

        // 验证数据仍然可用
        assertEquals("asyncValue", mmkv.takeString("asyncKey", ""))
    }

    // 整理空间测试
    @Test
    fun testTrim() {
        // 添加然后删除一些数据以创建碎片
        for (i in 1..100) {
            mmkv["trimKey$i"] = "value$i"
        }

        for (i in 1..50) {
            mmkv.removeValueForKey("trimKey$i")
        }

        // 记录整理前大小
        val sizeBefore = mmkv.actualSize

        // 执行整理
        mmkv.trim()

        // 验证整理后的大小可能有所变化
        // 注意：实际大小可能不会立即变化，因为整理是对内部碎片的处理
        assertTrue(mmkv.actualSize == sizeBefore)
    }

    // 并发访问测试
    @Test
    fun testConcurrentThreadAccess() {
        val threadCount = 10
        val operationsPerThread = 100
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        // 并发写入
        for (t in 0 until threadCount) {
            executor.submit {
                try {
                    for (i in 0 until operationsPerThread) {
                        val key = "concurrent_thread_${t}_$i"
                        mmkv[key] = "value_${t}_$i"
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        // 等待所有线程完成
        assertTrue(latch.await(30, TimeUnit.SECONDS))

        // 验证所有值都被正确写入
        for (t in 0 until threadCount) {
            for (i in 0 until operationsPerThread) {
                val key = "concurrent_thread_${t}_$i"
                assertEquals("value_${t}_$i", mmkv.takeString(key, ""))
            }
        }

        executor.shutdown()
    }

    @Test
    fun testConcurrentCoroutineAccess() = runBlocking {
        val coroutineCount = 10
        val operationsPerCoroutine = 100

        // 并发写入
        val jobs = List(coroutineCount) { t ->
            launch(Dispatchers.IO) {
                for (i in 0 until operationsPerCoroutine) {
                    val key = "concurrent_coroutine_${t}_$i"
                    mmkv[key] = "value_${t}_$i"
                }
            }
        }

        // 等待所有协程完成
        jobs.joinAll()

        // 验证所有值都被正确写入
        for (t in 0 until coroutineCount) {
            for (i in 0 until operationsPerCoroutine) {
                val key = "concurrent_coroutine_${t}_$i"
                assertEquals("value_${t}_$i", mmkv.takeString(key, ""))
            }
        }
    }


    // 使用自定义路径的MMKV测试
    @Test
    fun testCustomPathMMKV() {
        val customDir = tempFolder.resolve("custom-mmkv-dir")
        val customId = "customPathMMKV"

        val customMMKV = mmkvWithID(customId, MMKVMode.SINGLE_PROCESS, null, customDir.absolutePath)

        try {
            // 写入数据
            customMMKV["pathKey"] = "pathValue"

            // 读取并验证
            assertEquals("pathValue", customMMKV.takeString("pathKey", ""))

            // 验证文件确实在自定义路径创建
            val mmkvFiles = customDir.listFiles { _, name -> name.startsWith(customId) }
            assertNotNull(mmkvFiles)
            assertTrue(mmkvFiles.isNotEmpty())
        } finally {
            customMMKV.close()
        }
    }

    // 边缘情况测试 - 空键
    @Test
    fun testEmptyKey() {
        val emptyKey = ""

        // 设置空键
        assertFalse(mmkv.set(emptyKey, "emptyKeyValue"))

        // 验证不可以取回
        assertNotEquals("emptyKeyValue", mmkv.takeString(emptyKey, ""))

        // 验证存在性
        assertFalse(mmkv.containsKey(emptyKey))

        // 移除
        mmkv.removeValueForKey(emptyKey)
        assertFalse(mmkv.containsKey(emptyKey))
    }

    // 边缘情况测试 - 超长键和值
    @Test
    fun testLongKeyAndValue() {
        // 创建一个很长的键
        val longKey = "a".repeat(1000)
        // 创建一个很长的值
        val longValue = "b".repeat(10000)

        // 设置
        assertTrue(mmkv.set(longKey, longValue))

        // 验证
        assertEquals(longValue, mmkv.takeString(longKey, ""))
    }

    // 边缘情况测试 - 特殊字符
    @Test
    fun testSpecialCharacters() {
        val specialKey = "!@#$%^&*()_+{}|:<>?~`-=[];',./\""
        val specialValue = "特殊值：!@#$%^&*()_+{}|:<>?~`-=[];',./\""

        // 设置
        assertTrue(mmkv.set(specialKey, specialValue))

        // 验证
        assertEquals(specialValue, mmkv.takeString(specialKey, ""))
    }
}
