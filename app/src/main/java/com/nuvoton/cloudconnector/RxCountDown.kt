/*
 * Copyright 2026 Nuvoton Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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