package com.nuvoton.cloudconnector.model

import com.nuvoton.cloudconnector.*
import io.reactivex.subjects.PublishSubject
import okhttp3.*
import okio.ByteString
import java.lang.Exception
import java.util.concurrent.TimeUnit

class RxWebSocket(websocketUrl: String, apiKey: String) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val request = Request.Builder()
        .addHeader("authorization", "Bearer $apiKey")
        .url(websocketUrl)
        .build()
    private var webSocket: WebSocket? = null

    val notificationChannel: PublishSubject<RxWebSocketInfo> = PublishSubject.create()
    init {
        client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                this@RxWebSocket.webSocket = webSocket
                val open = RxWebSocketOpen()
                open.response = response
                notificationChannel.onNext(open)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@RxWebSocket.webSocket?.cancel()
                this@RxWebSocket.webSocket = null
                val fail = RxWebSocketFailure()
                fail.throwable = t
                fail.response = response
                notificationChannel.onNext(fail)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                val closing = RxWebSocketClosing()
                closing.code = code
                closing.reason = reason
                notificationChannel.onNext(closing)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val message = RxWebSocketMessage()
                message.text = text
                notificationChannel.onNext(message)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val message = RxWebSocketMessage()
                message.bytes = bytes
                notificationChannel.onNext(message)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@RxWebSocket.webSocket = null
                val closed = RxWebSocketClosed()
                closed.code = code
                closed.reason = reason
                notificationChannel.onNext(closed)
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