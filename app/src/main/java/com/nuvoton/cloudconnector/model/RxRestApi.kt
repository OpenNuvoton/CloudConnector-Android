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
    val restSubject: PublishSubject<Response> = PublishSubject.create()

    private val compositeDisposable = CompositeDisposable()

    fun syncGet(subUrl: String) : HashMap<String, Any?> {
        return syncRestAction(RESTAction.GET, subUrl)
    }

    fun syncPut(subUrl: String, json: String?) : HashMap<String, Any?> {
        return syncRestAction(RESTAction.PUT, subUrl, json)
    }

    fun syncPut(subUrl: String, content: HashMap<String, Any?>?) : HashMap<String, Any?> {
        return syncRestAction(RESTAction.PUT, subUrl, content)
    }

    fun syncPost(subUrl: String, json: String) : HashMap<String, Any?> {
        return syncRestAction(RESTAction.POST, subUrl, json)
    }

    fun syncPost(subUrl: String, content: HashMap<String, Any?>) : HashMap<String, Any?> {
        return syncRestAction(RESTAction.POST, subUrl, content)
    }

    private fun syncRestAction(action: RESTAction, subUrl: String, json: String?) : HashMap<String, Any?> {
        val map : HashMap<String, Any?> = gson.fromJsonString(json ?: "{}")
        return syncRestAction(action, subUrl, map)
    }


    private fun syncRestAction(action: RESTAction, subUrl: String, content: HashMap<String, Any?>? = null) : HashMap<String, Any?> {
        val url = "$hostUrl/$subUrl"
        val builder = FormBody.Builder()
        content?.forEach { entry ->
            builder.add(entry.key, entry.value.toString())
        }
        val request = when (action) {
            RESTAction.GET -> {
                Request.Builder()
                    .addHeader("authorization", "Bearer $apiKey")
                    .url(url)
                    .get()
                    .build()
            }
            RESTAction.PUT -> {
                Request.Builder()
                    .addHeader("authorization", "Bearer $apiKey")
                    .url(url)
                    .put(builder.build())
                    .build()}
            RESTAction.POST -> {
                Request.Builder()
                    .addHeader("authorization", "Bearer $apiKey")
                    .url(url)
                    .post(builder.build())
                    .build()
            }
        }
        val response = client.newCall(request).execute()
        return hashMapOf(
            "code" to response.code(),
            "message" to response.message(),
            "body" to response.body())
    }

    fun asyncGet(subUrl: String) {
        asyncRestAction(RESTAction.GET, subUrl)
    }

    fun asyncPut(subUrl: String, json: String?) {
        asyncRestAction(RESTAction.PUT, subUrl, json)
    }

    fun asyncPut(subUrl: String, content: HashMap<String, Any?>?) {
        asyncRestAction(RESTAction.PUT, subUrl, content)
    }

    fun asyncPost(subUrl: String, json: String) {
        asyncRestAction(RESTAction.POST, subUrl, json)
    }

    fun asyncPost(subUrl: String, content: HashMap<String, Any?>) {
        asyncRestAction(RESTAction.POST, subUrl, content)
    }

    private fun asyncRestAction(action: RESTAction, subUrl: String, json: String?) {
        val map : HashMap<String, Any?> = gson.fromJsonString(json ?: "{}")
        asyncRestAction(action, subUrl, map)
    }

    private fun asyncRestAction(action: RESTAction, subUrl: String, content: HashMap<String, Any?>? = null) {
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
                restSubject.onNext(it)
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