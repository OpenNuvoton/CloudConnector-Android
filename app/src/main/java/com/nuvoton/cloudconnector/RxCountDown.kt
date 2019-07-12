package com.nuvoton.cloudconnector

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

abstract class RxCountDown {
    private var disposable: Disposable? = null
    private var mStart: Long = 0L
    private lateinit var mUnit: TimeUnit

    abstract fun onTick(long: Long)
    abstract fun onFinish()

    fun start(start: Long, unit: TimeUnit) {
        mStart = start
        mUnit = unit
        refresh()
    }

    fun refresh() {
        disposable?.dispose()
        disposable = Observable.zip(
            Observable.range(0, mStart.toInt()),
            Observable.interval(1, mUnit), BiFunction { int: Int, long: Long ->
                mStart - int
            }).subscribeOn(Schedulers.io())
            .subscribeBy (
                onNext = {
                    onTick(it)
                },
                onComplete = {
                    onFinish()
                })
    }

    fun stop() {
        disposable?.dispose()
    }
}