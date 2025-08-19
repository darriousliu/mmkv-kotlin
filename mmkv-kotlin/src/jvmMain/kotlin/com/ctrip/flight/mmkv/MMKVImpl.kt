package com.ctrip.flight.mmkv

import java.lang.foreign.MemorySegment

class MMKVImpl(
    private val ptr: MemorySegment
) : MMKV_KMP {
    override fun set(key: String, value: String): Boolean {
        return NativeMMKV.setString(ptr, key, value)
    }

    override fun set(key: String, value: Boolean): Boolean {
        return NativeMMKV.setBoolean(ptr, key, value)
    }

    override fun set(key: String, value: Int): Boolean {
        return NativeMMKV.setInt(ptr, key, value)
    }

    override fun set(key: String, value: Long): Boolean {
        return NativeMMKV.setLong(ptr, key, value)
    }

    override fun set(key: String, value: Float): Boolean {
        return NativeMMKV.setFloat(ptr, key, value)
    }

    override fun set(key: String, value: Double): Boolean {
        return NativeMMKV.setDouble(ptr, key, value)
    }

    override fun set(key: String, value: ByteArray): Boolean {
        return NativeMMKV.setByteArray(ptr, key, value)
    }

    override fun set(key: String, value: UInt): Boolean {
        return NativeMMKV.setUInt(ptr, key, value)
    }

    override fun set(key: String, value: ULong): Boolean {
        return NativeMMKV.setULong(ptr, key, value)
    }

    override fun set(
        key: String,
        value: Set<String>?
    ): Boolean {
        return NativeMMKV.setStringSet(ptr, key, value)
    }

    override fun takeString(key: String, default: String): String {
        return NativeMMKV.getString(ptr, key, default)
    }

    override fun takeBoolean(key: String, default: Boolean): Boolean {
        return NativeMMKV.getBoolean(ptr, key, default)
    }

    override fun takeInt(key: String, default: Int): Int {
        return NativeMMKV.getInt(ptr, key, default)
    }

    override fun takeLong(key: String, default: Long): Long {
        return NativeMMKV.getLong(ptr, key, default)
    }

    override fun takeFloat(key: String, default: Float): Float {
        return NativeMMKV.getFloat(ptr, key, default)
    }

    override fun takeDouble(key: String, default: Double): Double {
        return NativeMMKV.getDouble(ptr, key, default)
    }

    override fun takeByteArray(key: String, default: ByteArray?): ByteArray? {
        return NativeMMKV.getByteArray(ptr, key, default)
    }

    override fun takeUInt(key: String, default: UInt): UInt {
        return NativeMMKV.getUInt(ptr, key, default)
    }

    override fun takeULong(key: String, default: ULong): ULong {
        return NativeMMKV.getULong(ptr, key, default)
    }

    override fun takeStringSet(
        key: String,
        default: Set<String>?
    ): Set<String>? {
        return NativeMMKV.getStringSet(ptr, key, default)
    }

    override fun removeValueForKey(key: String) {
        NativeMMKV.removeValueForKey(ptr, key)
    }

    override fun removeValuesForKeys(keys: List<String>) {
        NativeMMKV.removeValuesForKeys(ptr, keys)
    }

    override val actualSize: Long
        get() = NativeMMKV.actualSize(ptr)
    override val count: Long
        get() = NativeMMKV.count(ptr)
    override val totalSize: Long
        get() = NativeMMKV.totalSize(ptr)

    override fun clearMemoryCache() {
        NativeMMKV.clearMemoryCache(ptr)
    }

    override fun clearAll() {
        NativeMMKV.clearAll(ptr)
    }

    override fun close() {
        NativeMMKV.close(ptr)
    }

    override fun allKeys(): List<String> {
        return NativeMMKV.allKeys(ptr)
    }

    override fun containsKey(key: String): Boolean {
        return NativeMMKV.containsKey(ptr, key)
    }

    override fun checkReSetCryptKey(key: String?) {
        NativeMMKV.checkReSetCryptKey(ptr, key)
    }

    override fun mmapID(): String {
        return NativeMMKV.mmapID(ptr)
    }

    override fun async() {
        NativeMMKV.async(ptr)
    }

    override fun sync() {
        NativeMMKV.sync(ptr)
    }

    override fun trim() {
        NativeMMKV.trim(ptr)
    }
}