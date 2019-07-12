package com.nuvoton.cloudconnector.model

import com.google.gson.Gson
import com.nuvoton.cloudconnector.RxCountDown
import com.nuvoton.cloudconnector.RxVar
import io.reactivex.disposables.CompositeDisposable
import java.util.*
import java.util.concurrent.TimeUnit

abstract class RepositoryCommon {
    private val isAlive = RxVar(false)
    private val refreshTimer = object : RxCountDown() {
        override fun onTick(long: Long) {

        }

        override fun onFinish() {
            isAlive.value = false
        }
    }

    val lifeCycleDisposable = CompositeDisposable()
    val gson = Gson()
    private val startDate = Date().time

    fun getTimeSecond() : Long {
        val date = Date().time
        return (date - startDate) / 1000
    }

    fun getIsAlive() = isAlive

    fun startNotifyTimer(start: Long = 20L, unit: TimeUnit = TimeUnit.SECONDS) = refreshTimer.start(start, unit)

    fun notifyRepoIsAlive() {
        isAlive.value = true
        refreshTimer.refresh()
    }

    fun stopTimer() = refreshTimer.stop()

    abstract fun start()
    abstract fun pause()
    abstract fun destroy()
}