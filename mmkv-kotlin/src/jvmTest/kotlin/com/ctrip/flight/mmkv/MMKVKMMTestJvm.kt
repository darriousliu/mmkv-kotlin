/*
 * Copyright (C) 2022 Ctrip.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctrip.flight.mmkv


import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * iOS unit test
 * @author yaqiao
 */

class MMKVKotlinTestJvm {

    private lateinit var mmkvTest: MMKVKotlinTest

    @BeforeTest
    fun setUp() {
        initialize(File(System.getProperty("user.home")).resolve(".cache").absolutePath)
        mmkvTest = MMKVKotlinTest().apply {
            setUp()
        }
    }

    @AfterTest
    fun setDown() {
        mmkvTest.testDown()
    }

    @Test
    fun testCommon() = with(mmkvTest) {
        testBoolean()
        testInt()
        testLong()
        testFloat()
        testDouble()
        testString()
        testByteArray()
        testUInt()
        testULong()
        testStringSet()
        testRemove()
    }
}