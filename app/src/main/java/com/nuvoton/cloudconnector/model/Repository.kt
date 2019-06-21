package com.nuvoton.cloudconnector.model

import android.content.Context
import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class Repository private constructor() {
    companion object {
        object Holder { val inst = Repository() }
        val shared : Repository by lazy { Holder.inst }
    }

    private val compositeDisposable = CompositeDisposable()

    var awsRepo : AWSRepo? = null
    set(value) {
        val dis = value!!.awsSubject.subscribeOn(Schedulers.io()).subscribe({
            awsSubject.onNext(it)
        }, {
            Log.d(this.javaClass.simpleName, "Repository error")
            it.printStackTrace()
        })
        compositeDisposable.add(dis)
        field = value
    }
    var awsSubject : PublishSubject<String> = PublishSubject.create()

    fun destroy() {
        compositeDisposable.clear()
    }
}