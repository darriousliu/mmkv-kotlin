package com.ctrip.flight.mmkv

import kotlinx.atomicfu.atomic
import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.getValue


internal fun interface MMKVInternalLog {
    fun invoke(level: Int, tag: MemorySegment, message: MemorySegment): Int
}

internal object NativeMMKV {
    internal var global by atomic<Arena?>(null)
    internal var dll by atomic<SymbolLookup?>(null)
    internal var isInitialized by atomic(false)


    val MMKV_STRING_SET_RETURN_STRUCT: StructLayout = MemoryLayout.structLayout(
        ADDRESS.withName("items"),
        JAVA_LONG.withName("size"),
    )

    val initialize: (String, Int, (Int, String, String) -> Unit) -> Unit by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_initialize").orElseThrow(),
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS)
        )

        val adapter = MethodHandles.lookup().findVirtual(
            MMKVInternalLog::class.java,
            "invoke",
            MethodType.methodType(
                Int::class.java,
                Int::class.java,
                MemorySegment::class.java,
                MemorySegment::class.java
            )
        )

        return@lazy { path, logLevel, logFunc ->
            val loggerStub = Linker.nativeLinker().upcallStub(
                adapter.bindTo(
                    MMKVInternalLog { level, tag0, message0 ->
                        val tag = tag0.reinterpret(Long.MAX_VALUE).getString(0)
                        val msg = message0.reinterpret(Long.MAX_VALUE).getString(0)
                        logFunc(level, tag, msg)
                        1
                    }
                ),
                FunctionDescriptor.of(
                    JAVA_INT,
                    JAVA_INT,// int level
                    ADDRESS, // char* tag
                    ADDRESS  // char* message
                ),
                global,
            )
            useArena {
                val cPath = allocateFrom(path)
                funcHandle.invoke(cPath, logLevel, loggerStub)
            }
        }
    }

    val defaultMMKV: (Int, String?) -> MMKV_KMP by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_defaultMMKV").orElseThrow(),
            FunctionDescriptor.of(ADDRESS, JAVA_INT, ADDRESS)
        )

        return@lazy { mode, cryptKey ->
            val ptr = useArena {
                val cCryptKey = if (cryptKey != null) allocateFrom(cryptKey) else MemorySegment.NULL
                funcHandle.invoke(mode, cCryptKey) as? MemorySegment
                    ?: error("defaultMMKV return null")
            }
            MMKVImpl(ptr)
        }
    }

    val mmkvWithID: (String, Int, String?, String?) -> MMKV_KMP by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_mmkvWithID").orElseThrow(),
            FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS)
        )

        return@lazy { id, mode, cryptKey, rootPath ->
            val ptr = useArena {
                val cId = allocateFrom(id)
                val cCryptKey = if (cryptKey != null) allocateFrom(cryptKey) else MemorySegment.NULL
                val cRootPath = if (rootPath != null) allocateFrom(rootPath) else MemorySegment.NULL
                funcHandle.invoke(cId, mode, cCryptKey, cRootPath) as? MemorySegment
                    ?: error("mmkvWithID return null")
            }
            MMKVImpl(ptr)
        }
    }

    val getInt: (MemorySegment, String, Int) -> Int by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("getInt").orElseThrow(),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT)
        )

        return@lazy { mmkv, key, default ->
            useArena {
                val cKey = allocateFrom(key)
                funcHandle.invoke(mmkv, cKey, default) as Int? ?: default
            }
        }
    }
    val setInt: (MemorySegment, String, Int) -> Boolean by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("setInt").orElseThrow(),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, JAVA_INT)
        )

        return@lazy { mmkv, key, value ->
            val result = useArena {
                val cKey = allocateFrom(key)
                funcHandle.invoke(mmkv, cKey, value)
            }
            result as Boolean
        }
    }

    val getString: (MemorySegment, String, String) -> String by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("getString").orElseThrow(),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS)
        )

        return@lazy { mmkv, key, default ->
            val segment = useArena {
                val cKey = allocateFrom(key)
                val cDefault = allocateFrom(default)
                funcHandle.invoke(mmkv, cKey, cDefault) as? MemorySegment
                    ?: error("getString return null")
            }
            val str = segment.reinterpret(Long.MAX_VALUE).getString(0)
            free(segment)
            str
        }
    }
    val setString: (MemorySegment, String, String) -> Boolean by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("setString").orElseThrow(),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, ADDRESS)
        )

        return@lazy { mmkv, key, value ->
            val result = useArena {
                val cKey = allocateFrom(key)
                val cValue = allocateFrom(value)
                funcHandle.invoke(mmkv, cKey, cValue)
            }
            result as Boolean
        }
    }

    val getFloat: (MemorySegment, String, Float) -> Float by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("getFloat").orElseThrow(),
            FunctionDescriptor.of(JAVA_FLOAT, ADDRESS, ADDRESS, JAVA_FLOAT)
        )

        return@lazy { mmkv, key, default ->
            useArena {
                val cKey = allocateFrom(key)
                funcHandle.invoke(mmkv, cKey, default) as Float? ?: default
            }
        }
    }
    val setFloat: (MemorySegment, String, Float) -> Boolean by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("setFloat").orElseThrow(),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, JAVA_FLOAT)
        )

        return@lazy { mmkv, key, value ->
            val result = useArena {
                val cKey = allocateFrom(key)
                funcHandle.invoke(mmkv, cKey, value)
            }
            result as Boolean
        }
    }

    val getLong: (MemorySegment, String, Long) -> Long by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("getLong").orElseThrow(),
            FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS, JAVA_LONG)
        )

        return@lazy { mmkv, key, default ->
            useArena {
                val cKey = allocateFrom(key)
                funcHandle.invoke(mmkv, cKey, default) as Long? ?: default
            }
        }
    }
    val setLong: (MemorySegment, String, Long) -> Boolean by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("setLong").orElseThrow(),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, JAVA_LONG)
        )

        return@lazy { mmkv, key, value ->
            val result = useArena {
                val cKey = allocateFrom(key)
                funcHandle.invoke(mmkv, cKey, value)
            }
            result as Boolean
        }
    }

    val getDouble: (MemorySegment, String, Double) -> Double by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("getDouble").orElseThrow(),
            FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS, ADDRESS, JAVA_DOUBLE)
        )

        return@lazy { mmkv, key, default ->
            useArena {
                val cKey = allocateFrom(key)
                funcHandle.invoke(mmkv, cKey, default) as Double? ?: default
            }
        }
    }
    val setDouble: (MemorySegment, String, Double) -> Boolean by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("setDouble").orElseThrow(),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, JAVA_DOUBLE)
        )

        return@lazy { mmkv, key, value ->
            val result = useArena {
                val cKey = allocateFrom(key)
                funcHandle.invoke(mmkv, cKey, value)
            }
            result as Boolean
        }
    }

    val getBoolean: (MemorySegment, String, Boolean) -> Boolean by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("getBoolean").orElseThrow(),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, JAVA_BOOLEAN)
        )

        return@lazy { mmkv, key, default ->
            useArena {
                val cKey = allocateFrom(key)
                funcHandle.invoke(mmkv, cKey, default) as Boolean? ?: default
            }
        }
    }
    val setBoolean: (MemorySegment, String, Boolean) -> Boolean by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("setBoolean").orElseThrow(),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, JAVA_BOOLEAN)
        )

        return@lazy { mmkv, key, value ->
            val result = useArena {
                val cKey = allocateFrom(key)
                funcHandle.invoke(mmkv, cKey, value)
            }
            result as Boolean
        }
    }

    val getByteArray: (MemorySegment, String, ByteArray?) -> ByteArray? by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("getByteArray").orElseThrow(),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS)
        )
        return@lazy { mmkv, key, default ->
            useArena {
                val keyPtr = allocateFrom(key)
                val sizePtr = allocate(JAVA_LONG).reinterpret(JAVA_LONG.byteSize())
                val memoryPtr = funcHandle.invoke(mmkv, keyPtr, sizePtr) as MemorySegment

                if (memoryPtr == MemorySegment.NULL) {
                    return@useArena default
                } else {
                    val size = sizePtr.get(JAVA_LONG, 0)
                    val array = memoryPtr.reinterpret(size).toArray(JAVA_BYTE)
                    free(memoryPtr)
                    array
                }
            }
        }
    }
    val setByteArray: (MemorySegment, String, ByteArray) -> Boolean by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("setByteArray").orElseThrow(),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG)
        )

        return@lazy { mmkv, key, value ->
            val result = useArena {
                val keyPtr = allocateFrom(key)
                val valuePtr = allocate(value.size.toLong())
                valuePtr.copyFrom(MemorySegment.ofArray(value))
                funcHandle.invoke(mmkv, keyPtr, valuePtr, value.size.toLong())
            }
            result as Boolean
        }
    }

    val getUInt: (MemorySegment, String, UInt) -> UInt by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("getUInt").orElseThrow(),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT)
        )

        return@lazy { mmkv, key, default ->
            useArena {
                val cKey = allocateFrom(key)
                (funcHandle.invoke(mmkv, cKey, default.toInt()) as Int?)?.toUInt() ?: default
            }
        }
    }
    val setUInt: (MemorySegment, String, UInt) -> Boolean by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("setUInt").orElseThrow(),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, JAVA_INT)
        )

        return@lazy { mmkv, key, value ->
            val result = useArena {
                val cKey = allocateFrom(key)
                funcHandle.invoke(mmkv, cKey, value.toInt())
            }
            result as Boolean
        }
    }

    val getULong: (MemorySegment, String, ULong) -> ULong by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("getULong").orElseThrow(),
            FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS, JAVA_LONG)
        )

        return@lazy { mmkv, key, default ->
            useArena {
                val cKey = allocateFrom(key)
                (funcHandle.invoke(mmkv, cKey, default.toLong()) as Long?)?.toULong() ?: default
            }
        }
    }
    val setULong: (MemorySegment, String, ULong) -> Boolean by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("setULong").orElseThrow(),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, JAVA_LONG)
        )

        return@lazy { mmkv, key, value ->
            val result = useArena {
                val cKey = allocateFrom(key)
                funcHandle.invoke(mmkv, cKey, value.toLong())
            }
            result as Boolean
        }
    }

    val getStringSet: (MemorySegment, String, Set<String>?) -> Set<String>? by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("getStringSet").orElseThrow(),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS)
        )
        // 获取字段的偏移量
        val itemOffset =
            MMKV_STRING_SET_RETURN_STRUCT.byteOffset(MemoryLayout.PathElement.groupElement("items"))
        val lenOffset =
            MMKV_STRING_SET_RETURN_STRUCT.byteOffset(MemoryLayout.PathElement.groupElement("size"))

        return@lazy { mmkv, key, default ->
            val segment = useArena {
                val keyPtr = allocateFrom(key)
                (funcHandle.invoke(mmkv, keyPtr) as MemorySegment).reinterpret(
                    MMKV_STRING_SET_RETURN_STRUCT.byteSize()
                )
            }
            if (segment == MemorySegment.NULL) {
                return@lazy default
            } else {
                // 获取字符串数组指针和数组长度
                val len = segment.get(JAVA_LONG, lenOffset)
                val itemsPtr =
                    segment.get(ADDRESS, itemOffset).reinterpret(len * ADDRESS.byteSize())

                val result = mutableSetOf<String>()

                for (i in 0 until len) {
                    val itemPtr = itemsPtr.getAtIndex(ADDRESS, i)
                    result.add(itemPtr.reinterpret(Long.MAX_VALUE).getString(0))
                    free(itemPtr)
                }
                free(itemsPtr)
                free(segment)
                // 返回结果Set
                result
            }
        }
    }
    val setStringSet: (MemorySegment, String, Set<String>?) -> Boolean by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("setStringSet").orElseThrow(),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG)
        )

        return@lazy { mmkv, key, value ->
            if (value == null) {
                return@lazy useArena {
                    val keyPtr = allocateFrom(key)
                    funcHandle.invoke(mmkv, keyPtr, MemorySegment.NULL, 0L) as Boolean
                }
            }
            val result = useArena {
                val valueSegment = allocate(ADDRESS.byteSize() * value.size)
                value.map { allocateFrom(it) }.forEachIndexed { index, segment ->
                    valueSegment.setAtIndex(ADDRESS, index.toLong(), segment)
                }
                val keyPtr = allocateFrom(key)
                funcHandle.invoke(mmkv, keyPtr, valueSegment, value.size.toLong())
            }
            result as Boolean
        }
    }

    val removeValueForKey: (MemorySegment, String) -> Boolean by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_removeValueForKey").orElseThrow(),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS)
        )

        return@lazy { mmkv, key ->
            useArena {
                val keyPtr = allocateFrom(key)
                funcHandle.invoke(mmkv, keyPtr) as Boolean
            }
        }
    }

    val removeValuesForKeys: (MemorySegment, List<String>) -> Boolean by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_removeValuesForKeys").orElseThrow(),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, JAVA_LONG)
        )

        return@lazy { mmkv, keys ->
            useArena {
                if (keys.isEmpty()) return@useArena true
                val keysSegment = allocate(ADDRESS.byteSize() * keys.size)
                keys.map { allocateFrom(it) }.forEachIndexed { index, segment ->
                    keysSegment.setAtIndex(ADDRESS, index.toLong(), segment)
                }
                funcHandle.invoke(mmkv, keysSegment, keys.size.toLong()) as Boolean
            }
        }
    }

    val actualSize: (ptr: MemorySegment) -> Long by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_actualSize").orElseThrow(),
            FunctionDescriptor.of(JAVA_LONG, ADDRESS)
        )

        return@lazy { mmkv ->
            funcHandle.invoke(mmkv) as Long
        }
    }

    val count: (ptr: MemorySegment) -> Long by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_count").orElseThrow(),
            FunctionDescriptor.of(JAVA_LONG, ADDRESS)
        )

        return@lazy { mmkv ->
            funcHandle.invoke(mmkv) as Long
        }
    }

    val totalSize: (ptr: MemorySegment) -> Long by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_totalSize").orElseThrow(),
            FunctionDescriptor.of(JAVA_LONG, ADDRESS)
        )

        return@lazy { mmkv ->
            funcHandle.invoke(mmkv) as Long
        }
    }

    val clearMemoryCache: (ptr: MemorySegment) -> Unit by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_clearMemoryCache").orElseThrow(),
            FunctionDescriptor.ofVoid(ADDRESS)
        )

        return@lazy { mmkv ->
            funcHandle.invoke(mmkv)
        }
    }

    val clearAll: (ptr: MemorySegment) -> Unit by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_clearAll").orElseThrow(),
            FunctionDescriptor.ofVoid(ADDRESS)
        )

        return@lazy { mmkv ->
            funcHandle.invoke(mmkv)
        }
    }

    val close: (ptr: MemorySegment) -> Unit by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_close").orElseThrow(),
            FunctionDescriptor.ofVoid(ADDRESS)
        )

        return@lazy { mmkv ->
            funcHandle.invoke(mmkv)
        }
    }

    val allKeys: (ptr: MemorySegment) -> List<String> by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_allKeys").orElseThrow(),
            FunctionDescriptor.of(ADDRESS, ADDRESS)
        )
        val itemOffset =
            MMKV_STRING_SET_RETURN_STRUCT.byteOffset(MemoryLayout.PathElement.groupElement("items"))
        val lenOffset =
            MMKV_STRING_SET_RETURN_STRUCT.byteOffset(MemoryLayout.PathElement.groupElement("size"))


        return@lazy { mmkv ->
            val segment =
                (funcHandle.invoke(mmkv) as? MemorySegment)?.reinterpret(
                    MMKV_STRING_SET_RETURN_STRUCT.byteSize()
                ) ?: error("allKeys return null")

            // 获取字符串数组指针和数组长度
            val len = segment.get(JAVA_LONG, lenOffset)
            val itemsPtr = segment.get(ADDRESS, itemOffset).reinterpret(len * ADDRESS.byteSize())

            val result = mutableListOf<String>()

            for (i in 0 until len) {
                val itemPtr = itemsPtr.getAtIndex(ADDRESS, i)
                result.add(itemPtr.reinterpret(Long.MAX_VALUE).getString(0))
                free(itemPtr)
            }
            free(itemsPtr)
            free(segment)
            // 返回结果List
            result
        }
    }

    val containsKey: (ptr: MemorySegment, key: String) -> Boolean by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_containsKey").orElseThrow(),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS)
        )

        return@lazy { mmkv, key ->
            useArena {
                val keyPtr = allocateFrom(key)
                funcHandle.invoke(mmkv, keyPtr) as Boolean
            }
        }
    }

    val checkReSetCryptKey: (MemorySegment, String?) -> Unit by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_checkReSetCryptKey").orElseThrow(),
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS)
        )

        return@lazy { mmkv, key ->
            useArena {
                val keyPtr = if (key != null) allocateFrom(key) else MemorySegment.NULL
                funcHandle.invoke(mmkv, keyPtr)
            }
        }
    }

    val mmapID: (MemorySegment) -> String by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_mmapID").orElseThrow(),
            FunctionDescriptor.of(ADDRESS, ADDRESS)
        )

        return@lazy { mmkv ->
            useArena {
                val segment = funcHandle.invoke(mmkv) as MemorySegment
                val mmapId = segment.reinterpret(Long.MAX_VALUE).getString(0)
                free(segment)
                mmapId
            }
        }
    }

    val sync: (MemorySegment) -> Unit by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_sync").orElseThrow(),
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_BOOLEAN)
        )

        return@lazy { mmkv ->
            funcHandle.invoke(mmkv, true)
        }
    }

    val async: (MemorySegment) -> Unit by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_sync").orElseThrow(),
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_BOOLEAN)
        )

        return@lazy { mmkv ->
            funcHandle.invoke(mmkv, false)
        }
    }

    val trim: (MemorySegment) -> Unit by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_trim").orElseThrow(),
            FunctionDescriptor.ofVoid(ADDRESS)
        )

        return@lazy { mmkv ->
            funcHandle.invoke(mmkv)
        }
    }

    val backupOneToDirectory: (String, String, String?) -> Boolean by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_backupOneToDirectory").orElseThrow(),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, ADDRESS)
        )

        return@lazy { mmapID, dstDir, rootPath ->
            useArena {
                val cMmapID = allocateFrom(mmapID)
                val cDstDir = allocateFrom(dstDir)
                val cRootPath = if (rootPath != null) allocateFrom(rootPath) else MemorySegment.NULL
                funcHandle.invoke(cMmapID, cDstDir, cRootPath) as Boolean
            }
        }
    }

    val pageSize: () -> Long by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_pageSize").orElseThrow(),
            FunctionDescriptor.of(JAVA_LONG)
        )

        return@lazy {
            funcHandle.invoke() as Long
        }
    }

    val setLogLevel: (Int) -> Unit by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_setLogLevel").orElseThrow(),
            FunctionDescriptor.ofVoid(JAVA_INT)
        )

        return@lazy { logLevel ->
            funcHandle.invoke(logLevel)
        }
    }

    val version: () -> String by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_version").orElseThrow(),
            FunctionDescriptor.of(ADDRESS)
        )

        return@lazy {
            useArena {
                val segment = funcHandle.invoke() as MemorySegment
                val version = segment.reinterpret(Long.MAX_VALUE).getString(0)
                free(segment)
                version
            }
        }
    }

    val unregisterHandler: () -> Unit by lazy {
        val funcHandle = Linker.nativeLinker().downcallHandle(
            dll!!.find("mmkv_unregisterHandler").orElseThrow(),
            FunctionDescriptor.ofVoid()
        )

        return@lazy {
            funcHandle.invoke()
        }
    }

    private val free by lazy {
        val func = with(Linker.nativeLinker()) {
            downcallHandle(
                defaultLookup().find("free").orElseThrow(),
                FunctionDescriptor.ofVoid(ADDRESS)
            )
        }
        return@lazy { message: MemorySegment ->
            func.invoke(message)
        }
    }
}