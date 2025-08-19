package com.ctrip.flight.mmkv

actual enum class MMKVMode {
    SINGLE_PROCESS {
        override val rawValue = 1
    },
    MULTI_PROCESS {
        override val rawValue = 2
    };

    abstract val rawValue: Int
}