package com.ctrip.flight.mmkv

actual fun defaultMMKV(): MMKV_KMP {
    return NativeMMKV.defaultMMKV(MMKVMode.SINGLE_PROCESS.ordinal, null)
}

actual fun defaultMMKV(cryptKey: String): MMKV_KMP {
    return NativeMMKV.defaultMMKV(MMKVMode.SINGLE_PROCESS.ordinal, cryptKey)
}

actual fun mmkvWithID(
    mmapId: String,
    mode: MMKVMode,
    cryptKey: String?,
    rootPath: String?
): MMKV_KMP {
    return NativeMMKV.mmkvWithID(mmapId, mode.ordinal, cryptKey, rootPath)
}