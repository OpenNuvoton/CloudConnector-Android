package com.nuvoton.cloudconnector

import android.content.Context
import android.util.Log
import androidx.test.InstrumentationRegistry
import com.nuvoton.cloudconnector.model.AWSRepo
import com.nuvoton.cloudconnector.model.AliyunRepo
import com.nuvoton.cloudconnector.model.PelionRepo
import io.reactivex.schedulers.Schedulers

import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import java.util.concurrent.CountDownLatch

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleInstrumentedTest
{
    lateinit var fakeContext: Context

    @Before
    fun setup() {
        fakeContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun testAWSRepo() {
        val signal = CountDownLatch(1)
        val aws = AWSRepo(fakeContext)
        aws.awsDataSubject.subscribeOn(Schedulers.io()).subscribe({
            Log.d(this.javaClass.simpleName, "message = $it")
            print("message = $it")
            signal.countDown()
        }, {
            it.printStackTrace()
            signal.countDown()
        })
        aws.getIoTLatestStatus()
        signal.await()
    }

    @Test
    fun testAliyunRepo() {
        val signal = CountDownLatch(1)
        val aliyun = AliyunRepo()
        aliyun.aliyunDataSubject.subscribeOn(Schedulers.io()).subscribe({
            Log.d(this.javaClass.simpleName, "message = $it")
            aliyun.destroy()
            signal.countDown()
        }, {
            it.printStackTrace()
            aliyun.destroy()
            signal.countDown()
        })
        signal.await()
    }

    @Test
    fun testPelionGet() {
        val signal = CountDownLatch(1)
        val pelionRepo = PelionRepo()
        pelionRepo.start()

        pelionRepo.isWebSocketConnected.observable.filter { isConnected ->
            Log.d(this.javaClass.simpleName, "isConnected = $isConnected")
            isConnected
        }.flatMap {
            pelionRepo.pelionDataSubject
        }.subscribeOn(Schedulers.io()).subscribe({
            Log.d(this.javaClass.simpleName, "notiSubject = $it")
        }, {
            it.printStackTrace()
        })

        val map = pelionRepo.createNotificationChannel()
        if (map["code"] == 200) {
            val status = pelionRepo.getNotificationChannelStatus()
            if (status["code"] == 200) {
                pelionRepo.openWebSocket()
                val result = pelionRepo.subscribeToResource()
                Log.d(this.javaClass.simpleName, "subs result = $result")
            }
        }

        signal.await()
    }

    @Test
    fun testPelionWebSocket() {
        val signal = CountDownLatch(1)
        val pelionRepo = PelionRepo()

        pelionRepo.pelionRequestSubject.subscribe {
            Log.d(this.javaClass.simpleName, "request response = $it")
        }

        pelionRepo.pelionDataSubject.subscribe({
            Log.d(this.javaClass.simpleName, "noti response = $it")
            signal.countDown()
        }, {
            it.printStackTrace()
            signal.countDown()
        })
        pelionRepo.put("v2/notification/websocket", "{}")
        signal.await()
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.nuvoton.cloudconnector", appContext.packageName)
    }
}
