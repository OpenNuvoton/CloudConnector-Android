package com.nuvoton.cloudconnector.model

import android.util.Log
import com.nuvoton.cloudconnector.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.Response
import kotlin.Exception
import kotlin.concurrent.thread

// websocket reference: https://www.pelion.com/docs/device-management/current/integrate-web-app/event-notification.html#websocket-interface

class PelionRepo : RepositoryCommon() {
    // Nuvoton test
    private val apiKey = "ak_1MDE1ZTViZDZjNzBjMDI0MjBhMDExNDA1MDAwMDAwMDA015f5da4433c02420a011b0800000000OaUDAhYmVjiD1WjBj6vG0kIamO6FvC6L"
    var deviceId = "016e6e4416e5000000000001001201f5"
    private val resource = "3303/0/5700"
//    val resources = hashMapOf("tension" to "3200/0/5501", "current" to "3200/0/5502", "power" to "3200/0/5503")

    private val requstHostname = "api.us-east-1.mbedcloud.com"
    private val requestUrl = "https://$requstHostname"
    private val websocketUrl = "wss://$requstHostname/v2/notification/websocket-connect"
    val pelionDataSubject : PublishSubject<RxWebSocketInfo> = PublishSubject.create()
    val pelionRequestSubject : PublishSubject<Response> = PublishSubject.create()
    val pelionToastSubject : PublishSubject<String> = PublishSubject.create()

    private val restApi = RxRestApi(requestUrl, apiKey)

    var isWebSocketConnected = RxVar(false)

    fun getLatestDeviceList() : Observable<List<Map<String, Any?>>> {
        Log.d(this.javaClass.simpleName, "getLatestDeviceList")
        return Observable.just("")
            .map {
                get("v3/devices").run {
                    if (this["code"] != 200)
                        throw Exception("response code is not 200 but ${this["code"]}")

                    val body = this["body"] as String? ?: throw Exception("body is null")

                    val map: HashMap<String, Any?> = gson.fromJsonString(body)
                    var data = map["data"]
                    if (data !is List<*>)
                        throw Exception("data is not a list")

                    data as List<Map<String, Any?>>
                }
            }
    }


    fun openWebSocket() {
        val websocketDisposable = RxWebSocket(websocketUrl, apiKey)
            .notificationChannel.toFlowable(BackpressureStrategy.BUFFER)
            .subscribeOn(Schedulers.io())
            .subscribe({
                //                Log.d(this.javaClass.simpleName, "it.status=${it.status}")
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
//                        notifyRepoIsAlive()
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

    fun closeWebSocket() = delete("v2/notification/websocket")

    fun createNotificationChannel() : HashMap<String, Any?> = put("v2/notification/websocket")

    fun getNotificationChannelStatus() : HashMap<String, Any?> = get("v2/notification/websocket")

    fun subscribeToResource(deviceId: String? = null, resource: String? = null) : HashMap<String, Any?> {
        val id = deviceId ?: this.deviceId
        val res = resource ?: this.resource

        return put("v2/subscriptions/$id/$res")
    }

    fun get(subUrl: String) : HashMap<String, Any?> = restApi.syncGet(subUrl)

    fun put(subUrl: String, json: String? = null) : HashMap<String, Any?> = restApi.syncPut(subUrl, json)

    fun put(subUrl: String, content: HashMap<String, Any?>) : HashMap<String, Any?> = restApi.syncPut(subUrl, content)

    fun post(subUrl: String, json: String) : HashMap<String, Any?> = restApi.syncPost(subUrl, json)

    fun post(subUrl: String, content: HashMap<String, Any?>) : HashMap<String, Any?> = restApi.syncPost(subUrl, content)

    fun delete(subUrl: String) : HashMap<String, Any?> = restApi.syncDelete(subUrl)

    // Implement abstract functions
    override fun start() {
//        startNotifyTimer()
        val dis = restApi.restSubject.subscribeOn(Schedulers.io())
            .subscribe({
                debug("pelion response = $it")
                pelionRequestSubject.onNext(it)
            }, {
                pelionRequestSubject.onError(it)
            })
        lifeCycleDisposable.add(dis)


        val webDis = getLatestDeviceList().subscribeOn(Schedulers.io())
            .map { devices ->
                devices.forEach { device ->
                    if (device["name"] as? String == "pelion_multiCloud_demo") {
                        val id = device["id"]
                        if (id !is String)
                            throw Exception("id is not string")

                        deviceId = id
                        debug("deviceId=$deviceId")
                    }
                }
                createNotificationChannel()
            }
            .map {
                if (it["code"] != 200 && it["code"] != 201)
                    throw Exception("CreateNotificationChannel resp=${it["code"]}")

                getNotificationChannelStatus()
            }.map {
                if (it["code"] != 200)
                    throw Exception("GetNotificationChannelStatus resp=${it["code"]}")
                openWebSocket()
                subscribeToResource(resource = resource)
            }.subscribe({
                if (it["code"] != 200 && it["code"] != 202)
                    throw Exception("subscribe power failed, resp=${it["code"]}")
                notifyRepoIsAlive()
            }, {
                pelionToastSubject.onError(it)
            })

        lifeCycleDisposable.add(webDis)
    }

    override fun pause() {
        stopTimer()
        lifeCycleDisposable.clear()
    }

    override fun destroy() {
        thread { closeWebSocket() }
        lifeCycleDisposable.dispose()
    }

    override fun getCloudSetting(): List<String> {
        val splitKey = apiKey.subSequence(0, 8).toString()
        return listOf("ApiKey:\n$splitKey",
            "DeviceId:\n$deviceId",
            "Resource:\n$resource")
    }
}