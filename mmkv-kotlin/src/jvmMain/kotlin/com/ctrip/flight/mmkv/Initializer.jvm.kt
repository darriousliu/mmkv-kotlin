package com.ctrip.flight.mmkv

import java.lang.foreign.Arena
import java.lang.foreign.SymbolLookup
import com.ctrip.flight.mmkv.JvmTarget.*
import java.io.File

fun initialize(rootDir: String, logLevel: MMKVLogLevel = MMKVLogLevel.LevelDebug) {
    NativeMMKV.global = Arena.ofShared()
    NativeMMKV.dll = SymbolLookup.libraryLookup(defaultLoader.load(), NativeMMKV.global)
    NativeMMKV.initializeMMKV(rootDir, logLevel.ordinal) { level, tag, message ->
        if (level != MMKVLogLevel.LevelNone.ordinal) {
            val logMessage = when (level) {
                MMKVLogLevel.LevelDebug.ordinal -> "DEBUG: $message"
                MMKVLogLevel.LevelInfo.ordinal -> "INFO: $message"
                MMKVLogLevel.LevelWarning.ordinal -> "WARNING: $message"
                MMKVLogLevel.LevelError.ordinal -> "ERROR: $message"
                else -> message
            }
            if (level < logLevel.ordinal) return@initializeMMKV
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
        val tmp = File(System.getProperty("user.home")).resolve(".cache").resolve("$name.$ext")

        if (tmp.exists()) {
            val sha256 = tmp.sha256()
            val resourceSha256 =
                MMKV_KMP::class.java.getResourceAsStream("/build-${jvmTarget.name.lowercase()}.hash")!!
                    .readAllBytes().decodeToString()
            if (sha256 != resourceSha256) {
                tmp.parentFile!!.mkdirs()
                val stream = MMKV_KMP::class.java.getResourceAsStream("/$name.$ext")!!
                tmp.createNewFile()
                stream.use {
                    tmp.writeBytes(it.readAllBytes())
                }
            }
        } else {
            tmp.parentFile!!.mkdirs()
            val stream = MMKV_KMP::class.java.getResourceAsStream("/$name.$ext")!!
            tmp.createNewFile()
            stream.use {
                tmp.writeBytes(it.readAllBytes())
            }
        }

        tmp.absolutePath
    }
}