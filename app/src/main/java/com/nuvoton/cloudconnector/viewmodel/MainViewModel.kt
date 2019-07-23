package com.nuvoton.cloudconnector.viewmodel

import android.content.Context
import android.util.Base64
import android.util.Log
import com.nuvoton.cloudconnector.RxWebSocketInfo
import com.nuvoton.cloudconnector.RxWebSocketMessage
import com.nuvoton.cloudconnector.debug
import com.nuvoton.cloudconnector.fromJsonString
import com.nuvoton.cloudconnector.model.AWSRepo
import com.nuvoton.cloudconnector.model.AliyunRepo
import com.nuvoton.cloudconnector.model.PelionRepo
import com.nuvoton.cloudconnector.viewmodel.RepoOption.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.nio.charset.Charset

class MainViewModel(context: Context) {
    val mvAWSSubject : PublishSubject<Map<String, Any?>> = PublishSubject.create()
    val mvAliyunSubject : PublishSubject<Map<String, Any?>> = PublishSubject.create()
    val mvPelionSubject : PublishSubject<Map<String, Any?>> = PublishSubject.create()

    val mvAWSStatusSubject : PublishSubject<Boolean> = PublishSubject.create()
    val mvAliyunStatusSubject : PublishSubject<Boolean> = PublishSubject.create()
    val mvPelionStatusSubject : PublishSubject<Boolean> = PublishSubject.create()

    val awsRepo = AWSRepo(context)
    val aliyunRepo = AliyunRepo()
    val pelionRepo = PelionRepo()

    private val lifeCycleDisposables = CompositeDisposable()

    private fun bindStatusSubjects() {
        val aws = awsRepo.getIsAlive().observable
            .subscribeOn(Schedulers.io())
            .subscribe({
                mvAWSStatusSubject.onNext(it)
            }, {
                it.printStackTrace()
            })
        lifeCycleDisposables.add(aws)

        val aliyun = aliyunRepo.getIsAlive().observable
            .subscribeOn(Schedulers.io())
            .subscribe({
                mvAliyunStatusSubject.onNext(it)
            }, {
                it.printStackTrace()
            })
        lifeCycleDisposables.add(aliyun)

        val pelion = pelionRepo.getIsAlive().observable
            .subscribeOn(Schedulers.io())
            .subscribe({
                mvPelionStatusSubject.onNext(it)
            }, {
                it.printStackTrace()
            })
        lifeCycleDisposables.add(pelion)
    }

    private fun bindDataSubjects() {
        val aws = awsRepo.awsDataSubject
            .subscribeOn(Schedulers.io())
            .subscribe({
                mvAWSSubject.onNext(it)
            }, {
                it.printStackTrace()
            })
        lifeCycleDisposables.add(aws)

        val aliyun = aliyunRepo.aliyunDataSubject
            .subscribeOn(Schedulers.io())
            .subscribe({
                mvAliyunSubject.onNext(it)
            }, {
                it.printStackTrace()
            })
        lifeCycleDisposables.add(aliyun)

        val pelion = pelionRepo.pelionDataSubject
            .subscribeOn(Schedulers.io())
            .subscribe({
                //                debug("pelion data=$it")
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
            })
        lifeCycleDisposables.add(pelion)
    }

    fun start() {
        bindDataSubjects()
        bindStatusSubjects()
        awsRepo.start()
        aliyunRepo.start()
        pelionRepo.start()
    }

    fun pause() {
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