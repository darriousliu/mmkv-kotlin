package com.ctrip.flight.mmkv

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

actual fun backupOneToDirectory(
    mmapID: String,
    dstDir: String,
    rootPath: String?
): Boolean {
    return NativeMMKV.lib.mmkv_backupOneToDirectory(mmapID, dstDir, rootPath)
}

actual fun pageSize(): Long {
    return NativeMMKV.lib.mmkv_pageSize()
}

actual fun setLogLevel(logLevel: MMKVLogLevel) {
    NativeMMKV.lib.mmkv_setLogLevel(logLevel.ordinal)
}

actual fun version(): String {
    return NativeMMKV.lib.mmkv_version()
}

actual fun unregisterHandler() {
    NativeMMKV.lib.mmkv_unregisterHandler()
}

internal val jvmTarget by lazy {
    val osName = System.getProperty("os.name")
    when {
        osName == "Mac OS X" -> JvmTarget.MACOS
        osName.startsWith("Win") -> JvmTarget.WINDOWS
        osName.startsWith("Linux") -> JvmTarget.LINUX
        else -> error("Unsupported OS: $osName")
    }
}

internal enum class JvmTarget {
    MACOS,
    WINDOWS,
    LINUX;
}

internal fun File.sha256(): String {
    val buffer = ByteArray(8192)
    val digest = MessageDigest.getInstance("SHA-256")

    FileInputStream(this).use { inputStream ->
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }

    return digest.digest().joinToString("") { "%02x".format(it) }
}