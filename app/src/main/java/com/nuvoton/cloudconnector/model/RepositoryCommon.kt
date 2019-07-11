package com.nuvoton.cloudconnector.model

import com.nuvoton.cloudconnector.RxCountDown
import com.nuvoton.cloudconnector.RxVar
import java.util.concurrent.TimeUnit

open class RepositoryCommon {
    private val isAlive = RxVar(false)
    private val refreshTimer = object : RxCountDown() {
        override fun onTick(long: Long) {

        }

        override fun onFinish() {
            isAlive.value = false
        }
    }

    fun getIsAlive() = isAlive

    fun startNotifyTimer(start: Long = 10L, unit: TimeUnit = TimeUnit.SECONDS) = refreshTimer.start(start, unit)

    fun notifyRepoIsAlive() {
        isAlive.value = true
        refreshTimer.refresh()
    }

    fun stopTimer() = refreshTimer.cancel()
}