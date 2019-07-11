package com.nuvoton.cloudconnector.model

import com.google.gson.Gson
import com.nuvoton.cloudconnector.RxVar
import com.nuvoton.cloudconnector.fromJsonString
import com.nuvoton.cloudconnector.model.RxWebStatus.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

// websocket reference: https://www.pelion.com/docs/device-management/current/integrate-web-app/event-notification.html#websocket-interface

class PelionRepo : RepositoryCommon() {
    private val apiKey = "ak_1MDE1ZTViZDZjNzBjMDI0MjBhMDExNDA1MDAwMDAwMDA015f5da4433c02420a011b0800000000OaUDAhYmVjiD1WjBj6vG0kIamO6FvC6L"
    private val requstHostname = "api.us-east-1.mbedcloud.com"
    private val requestUrl = "https://$requstHostname"
    private val websocketUrl = "wss://$requstHostname/notification/websocket-connect/"
    val pelionNotiSubject : PublishSubject<HashMap<String, Any?>> = PublishSubject.create()
    val pelionRequestSubject : PublishSubject<HashMap<String, Any?>> = PublishSubject.create()

    private val gson = Gson()

    private val restApi = RxRestApi(requestUrl, apiKey)

    private val lifeCycleDisposable = CompositeDisposable()

    var isWebSocketConnected = RxVar(false)

    init {
        val websocketDisposable = RxWebSocket(websocketUrl, apiKey).notificationChannel.subscribeOn(Schedulers.io())
            .subscribe({
                when (it.status) {
                    Open -> isWebSocketConnected.value = true
                    Failure -> {
                        isWebSocketConnected.value = false
                        pelionNotiSubject.onError(it.throwable!!)
                    }
                    Closing -> {
                        isWebSocketConnected.value = false
                    }
                    Message -> if (isWebSocketConnected.value) {
                        val map : HashMap<String, Any?> = gson.fromJsonString(it.response?.body()?.string()!!)
                        pelionNotiSubject.onNext(map)
                    }
                    Closed -> isWebSocketConnected.value = false
                }
            }, {
                it.printStackTrace()
            })
        lifeCycleDisposable.add(websocketDisposable)

        val restDisposable = restApi.restSubject.subscribeOn(Schedulers.io())
            .subscribe({
                pelionRequestSubject.onNext(it)
            }, {
                pelionRequestSubject.onError(it)
            })
        lifeCycleDisposable.add(restDisposable)
    }

    fun get(subUrl: String) = restApi.get(subUrl)

    fun put(subUrl: String, json: String) = restApi.put(subUrl, json)

    fun put(subUrl: String, content: HashMap<String, Any?>) = restApi.put(subUrl, content)

    fun post(subUrl: String, json: String) = restApi.post(subUrl, json)

    fun post(subUrl: String, content: HashMap<String, Any?>) = restApi.post(subUrl, content)


    fun destroy() {
        lifeCycleDisposable.dispose()
    }
}