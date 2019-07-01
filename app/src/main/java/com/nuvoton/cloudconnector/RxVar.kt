package com.nuvoton.cloudconnector

import io.reactivex.subjects.BehaviorSubject

class RxVar<T> (private val defaultValue: T) {
    var value: T = defaultValue
        set(value) {
            field = value
            observable.onNext(value)
        }

    val observable = BehaviorSubject.createDefault(value)

    override fun toString(): String {
        return "RxVar(defaultValue=$defaultValue, value=$value)"
    }

}