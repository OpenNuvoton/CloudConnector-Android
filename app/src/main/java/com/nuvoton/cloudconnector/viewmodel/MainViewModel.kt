package com.nuvoton.cloudconnector.viewmodel

import android.content.Context
import com.nuvoton.cloudconnector.model.Repository
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class MainViewModel(context: Context) {
    val mvDataUpdateSubject : PublishSubject<Map<String, Any?>> = PublishSubject.create()
    val mvRepoStatusSubject : PublishSubject<RepoStatus> = PublishSubject.create()
    val repo = Repository.shared

    private val lifeCycleDisposables = CompositeDisposable()
    private val switchDisposables = CompositeDisposable()

    init {
        repo.context = context.applicationContext
        val dis = repo.repoStatusSubject.subscribeOn(Schedulers.io()).subscribe {
            mvRepoStatusSubject.onNext(it)
        }
        lifeCycleDisposables.add(dis)

        val periodRequestAWSData = Observable.interval(5, TimeUnit.SECONDS, Schedulers.io()).subscribe({
            repo.awsRepo.getIoTLatestStatus()
        }, {
            it.printStackTrace()
        })
        lifeCycleDisposables.add(periodRequestAWSData)
    }

    fun setupCloud(option: RepoOption) {
        clearSwitchDisposables()
        repo.clearDisposable()
        repo.switchRepo(option)
        val dis = repo.dataUpdateSubject.subscribe({
            mvDataUpdateSubject.onNext(it)
        }, {
            mvDataUpdateSubject.onError(it)
        })

        switchDisposables.add(dis)
    }

    fun clearSwitchDisposables() {
        switchDisposables.clear()
    }


    fun destroy() {
        switchDisposables.dispose()
        lifeCycleDisposables.dispose()
        repo.destroy()
    }
}

enum class RepoOption {
    AWS,
    PELION,
    ALIYUN
}

class RepoStatus(val option: RepoOption, var isAlive: Boolean = false)