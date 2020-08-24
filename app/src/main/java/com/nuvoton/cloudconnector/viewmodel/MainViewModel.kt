package com.nuvoton.cloudconnector.viewmodel

import android.content.Context
import android.util.Base64
import android.util.Log
import com.nuvoton.cloudconnector.*
import com.nuvoton.cloudconnector.model.AWSRepo
import com.nuvoton.cloudconnector.model.AliyunRepo
import com.nuvoton.cloudconnector.model.NuAliyunRepo
import com.nuvoton.cloudconnector.model.PelionRepo
import com.nuvoton.cloudconnector.viewmodel.RepoOption.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.nio.charset.Charset
import kotlin.concurrent.thread

class MainViewModel(context: Context) {
    val mvAWSSubject : PublishSubject<Map<String, Any?>> = PublishSubject.create()
    val mvAliyunSubject : PublishSubject<Map<String, Any?>> = PublishSubject.create()
    val mvPelionSubject : PublishSubject<Map<String, Any?>> = PublishSubject.create()

    val mvAWSStatusSubject : PublishSubject<Boolean> = PublishSubject.create()
    val mvAliyunStatusSubject : PublishSubject<Boolean> = PublishSubject.create()
    val mvPelionStatusSubject : PublishSubject<Boolean> = PublishSubject.create()

    val awsRepo = AWSRepo(context)
    val aliyunRepo = NuAliyunRepo(context)
    val pelionRepo = PelionRepo()

    private val lifeCycleDisposables = CompositeDisposable()

    private fun bindStatusSubjects() {
        awsRepo.getIsAlive().observable
            .subscribeOn(Schedulers.io())
            .subscribe({
                mvAWSStatusSubject.onNext(it)
            }, {
                it.printStackTrace()
            }).disposeBy(lifeCycleDisposables)

        aliyunRepo.getIsAlive().observable
            .subscribeOn(Schedulers.io())
            .subscribe({
                mvAliyunStatusSubject.onNext(it)
            }, {
                it.printStackTrace()
            }).disposeBy(lifeCycleDisposables)

        pelionRepo.getIsAlive().observable
            .subscribeOn(Schedulers.io())
            .subscribe({
                mvPelionStatusSubject.onNext(it)
            }, {
                it.printStackTrace()
            }).disposeBy(lifeCycleDisposables)
    }

    private fun bindDataSubjects() {
        awsRepo.awsDataSubject
            .subscribeOn(Schedulers.io())
            .subscribe({
                mvAWSSubject.onNext(it)
            }, {
                it.printStackTrace()
            }).disposeBy(lifeCycleDisposables)

        aliyunRepo.aliyunDataSubject
            .subscribeOn(Schedulers.io())
            .subscribe({
                mvAliyunSubject.onNext(it)
            }, {
                it.printStackTrace()
            }).disposeBy(lifeCycleDisposables)

        pelionRepo.pelionDataSubject
            .subscribeOn(Schedulers.io())
            .subscribe({
                if (it is RxWebSocketMessage) {
                    val json : String =
                        if (it.text != null)
                            it.text!!
                        else
                            it.bytes!!.string(Charset.defaultCharset())
                    val notiMap : HashMap<String, Any?> = pelionRepo.gson.fromJsonString(json)
                    val notifications = notiMap["notifications"]
                    val payload = if (notifications is List<*> && notifications.size > 0) {
                        val noti = notifications[0] as Map<String, Any?>
                        noti["payload"]
                    }else
                        null
                    if (payload != null) {
                        val value = Base64.decode(payload as String, Base64.NO_WRAP)
                        val string = String(value)
                        val map = hashMapOf<String, Any?>("temperature" to string.toFloat())
                        map["timestamp"] = pelionRepo.getTimeSecond()
                        mvPelionSubject.onNext(map)
                    }
                }
            }, {
                it.printStackTrace()
            }).disposeBy(lifeCycleDisposables)
    }

    fun updateSettings(context: Context) {
        awsRepo.updateSetting(context)
        aliyunRepo.updateSetting(context)
        pelionRepo.updateSetting(context)
    }

    fun start() {
        bindDataSubjects()
        bindStatusSubjects()
        awsRepo.start()
        thread {
            aliyunRepo.start()
        }
        pelionRepo.start()
    }

    fun pause() {
        lifeCycleDisposables.clear()
        awsRepo.pause()
        aliyunRepo.pause()
        pelionRepo.pause()
    }

    fun destroy() {
        lifeCycleDisposables.dispose()
        awsRepo.destroy()
        aliyunRepo.destroy()
        pelionRepo.destroy()
    }

    fun getCloudSetting(option: RepoOption) : List<String> =
        when (option) {
            AWS -> awsRepo.getCloudSetting()
            PELION -> pelionRepo.getCloudSetting()
            ALIYUN -> aliyunRepo.getCloudSetting()
        }
}

enum class RepoOption {
    AWS,
    PELION,
    ALIYUN
}

class RepoStatus(val option: RepoOption, var isAlive: Boolean = false)