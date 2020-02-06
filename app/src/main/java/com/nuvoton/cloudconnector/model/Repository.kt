package com.nuvoton.cloudconnector.model

import android.content.Context
import android.util.Base64
import com.nuvoton.cloudconnector.RxWebSocketMessage
import com.nuvoton.cloudconnector.fromJsonString
import com.nuvoton.cloudconnector.viewmodel.RepoOption
import com.nuvoton.cloudconnector.viewmodel.RepoOption.*
import com.nuvoton.cloudconnector.viewmodel.RepoStatus
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.nio.charset.Charset
import kotlin.collections.HashMap

class Repository private constructor() : RepositoryCommon() {
    companion object {
        object Holder { val inst = Repository() }
        val shared : Repository by lazy { Holder.inst }
    }

    private val switchCloudsDisposable = CompositeDisposable()

    lateinit var awsRepo : AWSRepo
    val aliyunRepo = AliyunRepo()
    val pelionRepo = PelionRepo()
    var context: Context? = null
    set(value) {
        awsRepo = AWSRepo(value!!)
        bindStatusCallback(AWS, awsRepo)
//        awsRepo.requestIoTData()
        field = value
    }

    val dataUpdateSubject : PublishSubject<Map<String, Any?>> = PublishSubject.create()
    val repoStatusSubject : PublishSubject<RepoStatus> = PublishSubject.create()

    init {
        bindStatusCallback(PELION, pelionRepo)
        bindStatusCallback(ALIYUN, aliyunRepo)
    }

    fun switchRepo(option: RepoOption) {
        val dis = when (option) {
            AWS -> {
                awsRepo.awsDataSubject.subscribeOn(Schedulers.io()).subscribe({
                    val reported = it["reported"]
                    val value = if (reported is Map<*, *>) {
                        val temp = reported["temperature"]
                        temp.toString().toFloat()
                    }else
                        null
                    dataUpdateSubject.onNext(hashMapOf("temperature" to value))
                }, {
                    it.printStackTrace()
                })
            }
            PELION -> {
                pelionRepo.pelionDataSubject.subscribeOn(Schedulers.io()).subscribe({
                    if (it is RxWebSocketMessage) {
                        val json : String =
                            if (it.text != null)
                            it.text!!
                        else
                            it.bytes!!.string(Charset.defaultCharset())
                        val notiMap : HashMap<String, Any?> = gson.fromJsonString(json)
                        val notifications = notiMap["notifications"]
                        val payload = if (notifications is List<*> && notifications.size > 0) {
                            val noti = notifications[0] as Map<String, Any?>
                            noti["payload"]
                        }else
                            null
                        if (payload != null) {
                            val value = Base64.decode(payload as String, Base64.NO_WRAP)
                            val string = String(value)
                            dataUpdateSubject.onNext(hashMapOf("temperature" to string.toFloat()))
                        }
                    }
                }, {
                    it.printStackTrace()
                })
            }
            ALIYUN -> {
                aliyunRepo.aliyunDataSubject.subscribeOn(Schedulers.io()).subscribe({
                    dataUpdateSubject.onNext(it)
                }, {
                    it.printStackTrace()
                })
            }
        }
        switchCloudsDisposable.add(dis)
    }

    private fun bindStatusCallback(option: RepoOption, repo: RepositoryCommon) {
        val dis = repo.getIsAlive().observable.subscribeOn(Schedulers.io()).subscribe { isAlive ->
            repoStatusSubject.onNext(RepoStatus(option, isAlive))
        }
        lifeCycleDisposable.add(dis)
    }

    fun clearSwitchDisposable() {
        switchCloudsDisposable.clear()
    }

    // Implement abstract function
    override fun start() {
        awsRepo.start()
        aliyunRepo.start()
        pelionRepo.start()
    }

    override fun pause() {
        switchCloudsDisposable.clear()
        lifeCycleDisposable.clear()

        awsRepo.pause()
        aliyunRepo.pause()
        pelionRepo.pause()
    }

    override fun destroy() {
        switchCloudsDisposable.dispose()
        lifeCycleDisposable.dispose()

        awsRepo.destroy()
        aliyunRepo.destroy()
        pelionRepo.destroy()
    }

    override fun getCloudSetting(): List<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateSetting(context: Context) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}