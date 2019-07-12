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