package com.nuvoton.cloudconnector.model

import io.reactivex.subjects.PublishSubject
import okhttp3.*
import okio.ByteString
import java.lang.Exception

class RxWebSocket(websocketUrl: String, apiKey: String) {
    private val client = OkHttpClient()
    private val request = Request.Builder()
        .addHeader("Authorization", "Bearer $apiKey")
        .url(websocketUrl)
        .build()
    private var webSocket: WebSocket? = null

    val notificationChannel: PublishSubject<RxWebSocketInfo> = PublishSubject.create()

    init {
        client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                this@RxWebSocket.webSocket = webSocket
                notificationChannel.onNext(RxWebSocketInfo(status = RxWebStatus.Open, response = response))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@RxWebSocket.webSocket?.cancel()
                this@RxWebSocket.webSocket = null
                notificationChannel.onError(t)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                notificationChannel.onNext(RxWebSocketInfo(status = RxWebStatus.Closing, code = code, reason = reason))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                notificationChannel.onNext(RxWebSocketInfo(status = RxWebStatus.Message, text = text))
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                notificationChannel.onNext(RxWebSocketInfo(status = RxWebStatus.Message, byteString = bytes))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@RxWebSocket.webSocket = null
                notificationChannel.onNext(RxWebSocketInfo(status = RxWebStatus.Closed, code = code, reason = reason))
            }
        })
    }

    fun sendMessage(text: String? = null, bytes: ByteString? = null) : Boolean {
        return try {
            when {
                text != null -> webSocket?.send(text) ?: false
                bytes != null -> webSocket?.send(bytes) ?: false
                else -> false
            }
        }catch (e: Exception) {
            notificationChannel.onError(e)
            false
        }
    }

    fun closeConnection() {
        webSocket?.cancel()
        webSocket = null
    }
}

enum class RxWebStatus {
    Open,
    Failure,
    Closing,
    Message,
    Closed
}

open class RxWebSocketInfo(val status: RxWebStatus,
                      var code: Int? = null,
                      var reason: String? = null,
                      var response: Response? = null,
                      var text: String? = null,
                      var byteString: ByteString? = null,
                      var throwable: Throwable? = null)