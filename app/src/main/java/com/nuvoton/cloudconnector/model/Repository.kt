package com.nuvoton.cloudconnector.model

import android.content.Context
import com.nuvoton.cloudconnector.viewmodel.RepoOption
import com.nuvoton.cloudconnector.viewmodel.RepoOption.*
import com.nuvoton.cloudconnector.viewmodel.RepoStatus
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class Repository private constructor() {
    companion object {
        object Holder { val inst = Repository() }
        val shared : Repository by lazy { Holder.inst }
    }

    private val switchCloudsDisposable = CompositeDisposable()
    private val lifeCycleDisposable = CompositeDisposable()

    lateinit var awsRepo : AWSRepo
    val aliyunRepo = AliyunRepo()
    val pelionRepo = PelionRepo()
    var context: Context? = null
    set(value) {
        awsRepo = AWSRepo(value!!)
        bindStatusCallback(AWS, awsRepo)
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
                    dataUpdateSubject.onNext(it)
                }, {
                    it.printStackTrace()
                })
            }
            PELION -> {
                pelionRepo.pelionNotiSubject.subscribeOn(Schedulers.io()).subscribe({
                    dataUpdateSubject.onNext(it)
                }, {
                    it.printStackTrace()
                })
            }
            ALIYUN -> {
                aliyunRepo.aliyunSubject.subscribeOn(Schedulers.io()).subscribe({
                    dataUpdateSubject.onNext(it)
                }, {
                    it.printStackTrace()
                })
            }
        }
        switchCloudsDisposable.add(dis)
    }

    fun bindStatusCallback(option: RepoOption, repo: RepositoryCommon) {
        val dis = repo.getIsAlive().observable.subscribeOn(Schedulers.io()).subscribe { isAlive ->
            repoStatusSubject.onNext(RepoStatus(option, isAlive))
        }
        lifeCycleDisposable.add(dis)
    }

    fun clearDisposable() {
        switchCloudsDisposable.clear()
    }

    fun destroy() {
        switchCloudsDisposable.dispose()
        lifeCycleDisposable.dispose()
        awsRepo.destroy()
        aliyunRepo.destroy()
        pelionRepo.destroy()
    }
}