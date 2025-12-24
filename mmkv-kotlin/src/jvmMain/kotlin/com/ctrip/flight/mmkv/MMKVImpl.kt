package com.ctrip.flight.mmkv

import com.sun.jna.Memory
import com.sun.jna.Pointer

class MMKVImpl(
    private val handle: Pointer
) : MMKV_KMP {
    private val lib = NativeMMKV.lib

    override fun set(key: String, value: String): Boolean {
        return lib.setString(handle, key, value)
    }

    override fun set(key: String, value: Boolean): Boolean {
        return lib.setBoolean(handle, key, value)
    }

    override fun set(key: String, value: Int): Boolean {
        return lib.setInt(handle, key, value)
    }

    override fun set(key: String, value: Long): Boolean {
        return lib.setLong(handle, key, value)
    }

    override fun set(key: String, value: Float): Boolean {
        return lib.setFloat(handle, key, value)
    }

    override fun set(key: String, value: Double): Boolean {
        return lib.setDouble(handle, key, value)
    }

    override fun set(key: String, value: ByteArray): Boolean {
        return lib.setByteArray(handle, key, value, value.size.toLong())
    }

    override fun set(key: String, value: UInt): Boolean {
        return lib.setUInt(handle, key, value.toInt())
    }

    override fun set(key: String, value: ULong): Boolean {
        return lib.setULong(handle, key, value.toLong())
    }

    override fun set(
        key: String,
        value: Set<String>?
    ): Boolean {
        val arr = value?.toTypedArray()
        return lib.setStringSet(handle, key, arr, arr?.size?.toLong() ?: 0L)
    }

    @Deprecated(
        message = "Renamed to 'getString' for clarity, as the 'take' prefix could be confusing.",
        replaceWith = ReplaceWith("getString(key, default)")
    )
    override fun takeString(key: String, default: String): String =
        lib.getString(handle, key, default)

    @Deprecated(
        message = "Renamed to 'getBoolean' for clarity, as the 'take' prefix could be confusing.",
        replaceWith = ReplaceWith("getBoolean(key, default)")
    )
    override fun takeBoolean(key: String, default: Boolean): Boolean =
        lib.getBoolean(handle, key, default)

    @Deprecated(
        message = "Renamed to 'getInt' for clarity, as the 'take' prefix could be confusing.",
        replaceWith = ReplaceWith("getInt(key, default)")
    )
    override fun takeInt(key: String, default: Int): Int = lib.getInt(handle, key, default)

    @Deprecated(
        message = "Renamed to 'getLong' for clarity, as the 'take' prefix could be confusing.",
        replaceWith = ReplaceWith("getLong(key, default)")
    )
    override fun takeLong(key: String, default: Long): Long = lib.getLong(handle, key, default)

    @Deprecated(
        message = "Renamed to 'getFloat' for clarity, as the 'take' prefix could be confusing.",
        replaceWith = ReplaceWith("getFloat(key, default)")
    )
    override fun takeFloat(key: String, default: Float): Float =
        lib.getFloat(handle, key, default)

    @Deprecated(
        message = "Renamed to 'getDouble' for clarity, as the 'take' prefix could be confusing.",
        replaceWith = ReplaceWith("getDouble(key, default)")
    )
    override fun takeDouble(key: String, default: Double): Double =
        lib.getDouble(handle, key, default)

    @Deprecated(
        message = "Renamed to 'getByteArray' for clarity, as the 'take' prefix could be confusing.",
        replaceWith = ReplaceWith("getByteArray(key, default)")
    )
    override fun takeByteArray(key: String, default: ByteArray?): ByteArray? =
        getByteArray(key, default)

    @Deprecated(
        message = "Renamed to 'getUInt' for clarity, as the 'take' prefix could be confusing.",
        replaceWith = ReplaceWith("getUInt(key, default)")
    )
    override fun takeUInt(key: String, default: UInt): UInt =
        lib.getInt(handle, key, default.toInt()).toUInt()

    @Deprecated(
        message = "Renamed to 'getULong' for clarity, as the 'take' prefix could be confusing.",
        replaceWith = ReplaceWith("getULong(key, default)")
    )
    override fun takeULong(key: String, default: ULong): ULong =
        lib.getLong(handle, key, default.toLong()).toULong()

    @Deprecated(
        message = "Renamed to 'getStringSet' for clarity, as the 'take' prefix could be confusing.",
        replaceWith = ReplaceWith("getStringSet(key, default)")
    )
    override fun takeStringSet(key: String, default: Set<String>?): Set<String>? =
        getStringSet(key, default)

    override fun getString(key: String, default: String): String {
        return lib.getString(handle, key, default)
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return lib.getBoolean(handle, key, default)
    }

    override fun getInt(key: String, default: Int): Int {
        return lib.getInt(handle, key, default)
    }

    override fun getLong(key: String, default: Long): Long {
        return lib.getLong(handle, key, default)
    }

    override fun getFloat(key: String, default: Float): Float {
        return lib.getFloat(handle, key, default)
    }

    override fun getDouble(key: String, default: Double): Double {
        return lib.getDouble(handle, key, default)
    }

    override fun getByteArray(key: String, default: ByteArray?): ByteArray? {
        val sizePtr = Memory(8) // long
        val ptr = lib.getByteArray(handle, key, sizePtr)
        return if (ptr == null) {
            default
        } else {
            val size = sizePtr.getLong(0)
            val bytes = ptr.getByteArray(0, size.toInt())
            NativeMMKV.free(ptr)
            bytes
        }
    }

    override fun getUInt(key: String, default: UInt): UInt {
        return lib.getUInt(handle, key, default.toInt()).toUInt()
    }

    override fun getULong(key: String, default: ULong): ULong {
        return lib.getULong(handle, key, default.toLong()).toULong()
    }

    override fun getStringSet(
        key: String,
        default: Set<String>?
    ): Set<String>? {
        val ret = lib.getStringSet(handle, key) ?: return default
        val size = ret.size
        val itemsPtr = ret.items ?: return emptySet()
        val result = mutableSetOf<String>()
        val ptrArray = itemsPtr.getPointerArray(0, size.toInt())
        for (p in ptrArray) {
            result.add(p.getString(0))
            NativeMMKV.free(p)
        }
        NativeMMKV.free(ret.items)
        // ret is a Structure. If it was allocated by C++, we need to free it.
        // C++: return static_cast<StringListReturn *>(malloc(...));
        // So yes, we must free ret.
        NativeMMKV.free(ret.pointer)
        return result
    }

    override fun removeValueForKey(key: String) {
        lib.mmkv_removeValueForKey(handle, key)
    }

    override fun removeValuesForKeys(keys: List<String>) {
        val arr = keys.toTypedArray()
        lib.mmkv_removeValuesForKeys(handle, arr, arr.size.toLong())
    }

    override val actualSize: Long
        get() = lib.mmkv_actualSize(handle)
    override val count: Long
        get() = lib.mmkv_count(handle)
    override val totalSize: Long
        get() = lib.mmkv_totalSize(handle)

    override fun clearMemoryCache() {
        lib.mmkv_clearMemoryCache(handle)
    }

    override fun clearAll() {
        lib.mmkv_clearAll(handle)
    }

    override fun close() {
        lib.mmkv_close(handle)
    }

    override fun allKeys(): List<String> {
        val ret = lib.mmkv_allKeys(handle) ?: return emptyList()
        val size = ret.size
        val itemsPtr = ret.items ?: return emptyList()
        val result = mutableListOf<String>()
        val ptrArray = itemsPtr.getPointerArray(0, size.toInt())
        for (p in ptrArray) {
            result.add(p.getString(0))
            NativeMMKV.free(p)
        }
        NativeMMKV.free(ret.items)
        NativeMMKV.free(ret.pointer)
        return result
    }

    override fun containsKey(key: String): Boolean {
        return lib.mmkv_containsKey(handle, key)
    }

    override fun checkReSetCryptKey(key: String?) {
        lib.mmkv_checkReSetCryptKey(handle, key)
    }

    override fun mmapID(): String {
        return lib.mmkv_mmapID(handle)
    }

    override fun async() {
        lib.mmkv_sync(handle, false)
    }

    override fun sync() {
        lib.mmkv_sync(handle, true)
    }

    override fun trim() {
        lib.mmkv_trim(handle)
    }
}