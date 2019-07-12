package com.nuvoton.cloudconnector.model

import android.util.Log
import com.nuvoton.cloudconnector.*
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.Response
import kotlin.concurrent.thread

// websocket reference: https://www.pelion.com/docs/device-management/current/integrate-web-app/event-notification.html#websocket-interface

class PelionRepo : RepositoryCommon() {
    private val apiKey = "ak_1MDE1ZTViZDZjNzBjMDI0MjBhMDExNDA1MDAwMDAwMDA015f5da4433c02420a011b0800000000OaUDAhYmVjiD1WjBj6vG0kIamO6FvC6L"
    private val requstHostname = "api.us-east-1.mbedcloud.com"
    private val requestUrl = "https://$requstHostname"
    private val websocketUrl = "wss://$requstHostname/v2/notification/websocket-connect"
    private val deviceId = "016c08b9c7b9000000000001001002ab"
    private val resource = "3303/0/5700"
    val pelionDataSubject : PublishSubject<RxWebSocketInfo> = PublishSubject.create()
    val pelionRequestSubject : PublishSubject<Response> = PublishSubject.create()

    private val restApi = RxRestApi(requestUrl, apiKey)

    var isWebSocketConnected = RxVar(false)

    fun openWebSocket() {
        val websocketDisposable = RxWebSocket(websocketUrl, apiKey).notificationChannel.subscribeOn(Schedulers.io())
            .subscribe({
                Log.d(this.javaClass.simpleName, "it.status=${it.status}")
                when (it.status) {
                    RxWebStatus.Open -> isWebSocketConnected.value = true
                    RxWebStatus.Failure -> {
                        if (it is RxWebSocketFailure) {
                            isWebSocketConnected.value = false
                            pelionDataSubject.onError(it.throwable)
                        }
                    }
                    RxWebStatus.Closing -> {
                        isWebSocketConnected.value = false
                        pelionDataSubject.onNext(it)
                    }
                    RxWebStatus.Message -> if (isWebSocketConnected.value) {
                        notifyRepoIsAlive()
                        pelionDataSubject.onNext(it)
                    }
                    RxWebStatus.Closed -> {
                        isWebSocketConnected.value = false
                        pelionDataSubject.onNext(it)
                    }
                }
            }, {
                pelionDataSubject.onError(it)
            })
        lifeCycleDisposable.add(websocketDisposable)
    }

    fun createNotificationChannel() : HashMap<String, Any?> = put("v2/notification/websocket")

    fun getNotificationChannelStatus() : HashMap<String, Any?> = get("v2/notification/websocket")

    fun subscribeToResource(deviceId: String? = null, resource: String? = null) : HashMap<String, Any?> {
        return if (resource != null && deviceId != null) {
            put("v2/subscriptions/$deviceId/$resource")
        }else {
            put("v2/subscriptions/${this.deviceId}/${this.resource}")
        }
    }

    fun get(subUrl: String) : HashMap<String, Any?> = restApi.syncGet(subUrl)

    fun put(subUrl: String, json: String? = null) : HashMap<String, Any?> = restApi.syncPut(subUrl, json)

    fun put(subUrl: String, content: HashMap<String, Any?>) : HashMap<String, Any?> = restApi.syncPut(subUrl, content)

    fun post(subUrl: String, json: String) : HashMap<String, Any?> = restApi.syncPost(subUrl, json)

    fun post(subUrl: String, content: HashMap<String, Any?>) : HashMap<String, Any?> = restApi.syncPost(subUrl, content)

    // Implement abstract functions
    override fun start() {
        startNotifyTimer()
        val dis = restApi.restSubject.subscribeOn(Schedulers.io())
            .subscribe({
                debug("pelion response = $it")
                pelionRequestSubject.onNext(it)
            }, {
                pelionRequestSubject.onError(it)
            })
        lifeCycleDisposable.add(dis)
        thread {
            //若OPEN失敗會有問題，要做個定時檢查
            val map = createNotificationChannel()
            if (map["code"] == 200) {
                val status = getNotificationChannelStatus()
                if (status["code"] == 200) {
                    openWebSocket()
                    val result = subscribeToResource()
                    debug("subs result = $result")
                }
            }
        }
    }

    override fun pause() {
        stopTimer()
        lifeCycleDisposable.clear()
    }

    override fun destroy() {
        lifeCycleDisposable.dispose()
    }
}