/*
 * Copyright 2026 Nuvoton Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nuvoton.cloudconnector

import okhttp3.Response
import okio.ByteString

enum class RxWebStatus {
    Open,
    Failure,
    Closing,
    Message,
    Closed
}

interface RxWebSocketInfo {
    var status: RxWebStatus
    var code: Int
}

class RxWebSocketOpen : RxWebSocketInfo {
    override var status: RxWebStatus = RxWebStatus.Open
    override var code: Int = 0
    lateinit var response: Response

    override fun toString(): String {
        return "$status/$code/$response"
    }
}

class RxWebSocketFailure : RxWebSocketInfo {
    override var status: RxWebStatus = RxWebStatus.Failure
    override var code: Int = 0
    var response: Response? = null
    lateinit var throwable: Throwable

    override fun toString(): String {
        return "$status/$code/$response/${throwable.message}"
    }
}

class RxWebSocketClosing : RxWebSocketInfo {
    override var status: RxWebStatus = RxWebStatus.Closing
    override var code: Int = 0
    lateinit var reason: String

    override fun toString(): String {
        return "$status/$code/$reason"
    }
}

class RxWebSocketMessage : RxWebSocketInfo {
    override var status: RxWebStatus = RxWebStatus.Message
    override var code: Int = 0
    var text: String? = null
    var bytes: ByteString? = null

    override fun toString(): String {
        return "$status/$code/$text/$bytes"
    }
}

class RxWebSocketClosed : RxWebSocketInfo {
    override var status: RxWebStatus = RxWebStatus.Closed
    override var code: Int = 0
    lateinit var reason: String

    override fun toString(): String {
        return "$status/$code/$reason"
    }
}