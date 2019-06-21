package com.nuvoton.cloudconnector

import android.content.Context
import android.util.Log
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.nuvoton.cloudconnector.model.AWSRepo
import io.reactivex.schedulers.Schedulers

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import java.util.concurrent.CountDownLatch
import kotlin.math.sign

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
        aws.awsSubject.subscribeOn(Schedulers.io()).subscribe({
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
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.nuvoton.cloudconnector", appContext.packageName)
    }
}
