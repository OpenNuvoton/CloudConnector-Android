package com.nuvoton.cloudconnector.model

import com.google.gson.Gson
import com.nuvoton.cloudconnector.fromJsonString
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.*

class RxRestApi(val hostUrl: String, val apiKey: String) {
    private val client = OkHttpClient()
    private val gson = Gson()
    val restSubject: PublishSubject<HashMap<String, Any?>> = PublishSubject.create()

    private val compositeDisposable = CompositeDisposable()

    fun get(subUrl: String) {
        restAction(RESTAction.GET, subUrl)
    }

    fun put(subUrl: String, json: String) {
        restAction(RESTAction.PUT, subUrl, json)
    }

    fun put(subUrl: String, content: HashMap<String, Any?>) {
        restAction(RESTAction.PUT, subUrl, content)
    }

    fun post(subUrl: String, json: String) {
        restAction(RESTAction.POST, subUrl, json)
    }

    fun post(subUrl: String, content: HashMap<String, Any?>) {
        restAction(RESTAction.POST, subUrl, content)
    }

    private fun restAction(action: RESTAction, subUrl: String, json: String) {
        val map : HashMap<String, Any?> = gson.fromJsonString(json)
        restAction(action, subUrl, map)
    }

    private fun restAction(action: RESTAction, subUrl: String, content: HashMap<String, Any?>? = null) {
        val dis = Observable.just("$hostUrl/$subUrl").subscribeOn(Schedulers.io())
            .map {
                val builder = FormBody.Builder()
                content?.forEach { entry ->
                    builder.add(entry.key, entry.value.toString())
                }
                val request = when (action) {
                    RESTAction.GET -> {
                        Request.Builder()
                            .addHeader("authorization", "Bearer $apiKey")
                            .url(it)
                            .get()
                            .build()
                    }
                    RESTAction.PUT -> {
                        Request.Builder()
                            .addHeader("authorization", "Bearer $apiKey")
                            .url(it)
                            .put(builder.build())
                            .build()}
                    RESTAction.POST -> {
                        Request.Builder()
                            .addHeader("authorization", "Bearer $apiKey")
                            .url(it)
                            .post(builder.build())
                            .build()
                    }
                }
                client.newCall(request).execute()
            }.subscribe({
                val bodyMap : HashMap<String, Any?> = gson.fromJsonString(it.body()!!.string())
                restSubject.onNext(bodyMap)
            }, {
                restSubject.onError(it)
            })
        compositeDisposable.add(dis)
    }

    fun destroy() {
        compositeDisposable.clear()
    }

    enum class RESTAction {
        GET,
        PUT,
        POST,
    }
}