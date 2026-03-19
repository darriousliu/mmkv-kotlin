package com.ctrip.flight.mmkv

import com.ctrip.flight.mmkv.JvmTarget.LINUX
import com.ctrip.flight.mmkv.JvmTarget.MACOS
import com.ctrip.flight.mmkv.JvmTarget.WINDOWS
import java.io.File
import java.lang.foreign.Arena
import java.lang.foreign.SymbolLookup
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

fun initialize(rootDir: String, logLevel: MMKVLogLevel = MMKVLogLevel.LevelDebug) {
    NativeMMKV.global = Arena.ofShared()
    NativeMMKV.dll = SymbolLookup.libraryLookup(defaultLoader.load(), NativeMMKV.global)
    NativeMMKV.initialize(rootDir, logLevel.ordinal) { level, tag, message ->
        if (level != MMKVLogLevel.LevelNone.ordinal) {
            val logMessage = when (level) {
                MMKVLogLevel.LevelDebug.ordinal -> "DEBUG: $message"
                MMKVLogLevel.LevelInfo.ordinal -> "INFO: $message"
                MMKVLogLevel.LevelWarning.ordinal -> "WARNING: $message"
                MMKVLogLevel.LevelError.ordinal -> "ERROR: $message"
                else -> message
            }
            if (level < logLevel.ordinal) return@initialize
            println("[$tag] $logMessage")
        }
    }
    NativeMMKV.isInitialized = true
}

/**
 * MMKV C库加载器接口
 */
fun interface MMKVCLibLoader {
    /**
     * 加载C库
     * @return 加载的库路径
     */
    fun load(): String
}

private val defaultLoader: MMKVCLibLoader by lazy {
    MMKVCLibLoader {
        val name = when (jvmTarget) {
            MACOS, LINUX -> "libmmkvc"
            WINDOWS -> "mmkvc"
        }
        val ext = when (jvmTarget) {
            MACOS -> "dylib"
            LINUX -> "so"
            WINDOWS -> "dll"
        }
        val resourceSha256 = loadBundledLibraryHash()
        val cacheDir = File(System.getProperty("user.home")).resolve(".cache").resolve("mmkv-kotlin")
        val cachedLib = cacheDir.resolve("$name-$resourceSha256.$ext")

        if (!cachedLib.isLibraryHashMatched(resourceSha256)) {
            val bundledLibrary = loadBundledLibraryBytes(name, ext)
            writeAtomically(cachedLib, bundledLibrary)
        }

        if (jvmTarget == WINDOWS) {
            val runtimeLib = cacheDir.resolve("$name-$resourceSha256-${UUID.randomUUID()}.$ext")
            cachedLib.copyTo(runtimeLib, overwrite = true)
            runtimeLib.deleteOnExit()
            cleanupStaleRuntimeLibraries(cacheDir, runtimeLib.name, ext)
            runtimeLib.absolutePath
        } else {
            cachedLib.absolutePath
        }
    }
}

private fun loadBundledLibraryBytes(name: String, ext: String): ByteArray {
    val stream = MMKV_KMP::class.java.getResourceAsStream("/$name.$ext")
        ?: error("Bundled native library /$name.$ext not found")
    return stream.use { it.readAllBytes() }
}

private fun loadBundledLibraryHash(): String {
    val hashResourcePath = "/build-${jvmTarget.name.lowercase()}.hash"
    val stream = MMKV_KMP::class.java.getResourceAsStream(hashResourcePath)
        ?: error("Bundled native hash file $hashResourcePath not found")
    return stream.use { it.readAllBytes().decodeToString().trim().lowercase() }
}

private fun File.isLibraryHashMatched(expectedSha256: String): Boolean {
    return exists() && runCatching { sha256().equals(expectedSha256, ignoreCase = true) }.getOrDefault(false)
}

private fun writeAtomically(target: File, bytes: ByteArray) {
    target.parentFile?.mkdirs()
    val temp = File.createTempFile("${target.nameWithoutExtension}-", ".tmp", target.parentFile)
    try {
        temp.writeBytes(bytes)
        try {
            Files.move(
                temp.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            temp.copyTo(target, overwrite = true)
        }
    } finally {
        if (temp.exists()) {
            temp.delete()
        }
    }
}

private fun cleanupStaleRuntimeLibraries(cacheDir: File, currentRuntimeName: String, ext: String) {
    val extension = ".$ext"
    cacheDir.listFiles()?.forEach { file ->
        if (file.name == currentRuntimeName || !file.name.endsWith(extension)) {
            return@forEach
        }
        val stem = file.name.removeSuffix(extension)
        // Keep the stable cache file: <name>-<sha256>.<ext>
        if (stem.count { it == '-' } < 2) {
            return@forEach
        }
        runCatching {
            file.delete()
        }
    }
}
