package com.ctrip.flight.mmkv

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure

internal interface LibMMKV : Library {
    fun mmkv_initialize(path: String, level: Int, logger: Callback)

    fun mmkv_defaultMMKV(mode: Int, cryptKey: String?): Pointer
    fun mmkv_mmkvWithID(id: String, mode: Int, cryptKey: String?, rootPath: String?): Pointer

    fun getInt(handle: Pointer, key: String, default: Int): Int
    fun setInt(handle: Pointer, key: String, value: Int): Boolean

    fun getString(handle: Pointer, key: String, default: String): String
    fun setString(handle: Pointer, key: String, value: String): Boolean

    fun getFloat(handle: Pointer, key: String, default: Float): Float
    fun setFloat(handle: Pointer, key: String, value: Float): Boolean

    fun getLong(handle: Pointer, key: String, default: Long): Long
    fun setLong(handle: Pointer, key: String, value: Long): Boolean

    fun getDouble(handle: Pointer, key: String, default: Double): Double
    fun setDouble(handle: Pointer, key: String, value: Double): Boolean

    fun getBoolean(handle: Pointer, key: String, default: Boolean): Boolean
    fun setBoolean(handle: Pointer, key: String, value: Boolean): Boolean

    fun getByteArray(handle: Pointer, key: String, size: Pointer): Pointer?
    fun setByteArray(handle: Pointer, key: String, value: ByteArray, size: Long): Boolean

    fun getUInt(handle: Pointer, key: String, default: Int): Int
    fun setUInt(handle: Pointer, key: String, value: Int): Boolean

    fun getULong(handle: Pointer, key: String, default: Long): Long
    fun setULong(handle: Pointer, key: String, value: Long): Boolean

    fun getStringSet(handle: Pointer, key: String): StringListReturn?
    fun setStringSet(handle: Pointer, key: String, value: Array<String>?, size: Long): Boolean

    fun mmkv_removeValueForKey(handle: Pointer, key: String)
    fun mmkv_removeValuesForKeys(handle: Pointer, keys: Array<String>, size: Long)

    fun mmkv_actualSize(handle: Pointer): Long
    fun mmkv_count(handle: Pointer): Long
    fun mmkv_totalSize(handle: Pointer): Long

    fun mmkv_clearMemoryCache(handle: Pointer)
    fun mmkv_clearAll(handle: Pointer)
    fun mmkv_close(handle: Pointer)

    fun mmkv_allKeys(handle: Pointer): StringListReturn?
    fun mmkv_containsKey(handle: Pointer, key: String): Boolean

    fun mmkv_checkReSetCryptKey(handle: Pointer, key: String?)
    fun mmkv_mmapID(handle: Pointer): String

    fun mmkv_sync(handle: Pointer, flag: Boolean)
    fun mmkv_trim(handle: Pointer)

    fun mmkv_backupOneToDirectory(mmapID: String, dstDir: String, rootPath: String?): Boolean
    fun mmkv_pageSize(): Long
    fun mmkv_setLogLevel(logLevel: Int)
    fun mmkv_version(): String
    fun mmkv_unregisterHandler()
}

@Structure.FieldOrder("items", "size")
open class StringListReturn : Structure() {
    @JvmField
    var items: Pointer? = null
    @JvmField
    var size: Long = 0
}

internal object NativeMMKV {
    lateinit var lib: LibMMKV
    var isInitialized = false

    val defaultMMKV: (Int, String?) -> MMKV_KMP = { mode, cryptKey ->
        MMKVImpl(lib.mmkv_defaultMMKV(mode, cryptKey))
    }

    val mmkvWithID: (String, Int, String?, String?) -> MMKV_KMP = { id, mode, cryptKey, rootPath ->
        MMKVImpl(lib.mmkv_mmkvWithID(id, mode, cryptKey, rootPath))
    }

    fun initialize(path: String, logLevel: Int, logFunc: (Int, String, String) -> Unit) {
        // Initialization is handled in Initializer.jvm.kt where we load the library and assign 'lib'
        if (::lib.isInitialized) {
            val callback = object : Callback {
                fun invoke(level: Int, file: String, message: String) {
                    logFunc(level, file, message)
                }
            }
            // Keep a reference to prevent GC? JNA handles callbacks but for long-running it might be needed.
            // But here we just pass it to init.
            lib.mmkv_initialize(path, logLevel, callback)
        }
    }
    
    fun free(ptr: Pointer?) {
        if (ptr != null) {
            Native.free(Pointer.nativeValue(ptr))
        }
    }
}