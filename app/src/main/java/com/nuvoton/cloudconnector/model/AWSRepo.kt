package com.nuvoton.cloudconnector.model

import android.content.Context
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.iotdata.AWSIotDataClient
import com.amazonaws.services.iotdata.model.GetThingShadowRequest
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class AWSRepo(applicationContext: Context) {
    private var mAWSIoTdataClient: AWSIotDataClient? = null
    private var mCognitoIdentityPoolId = "us-east-1:9e41d4ca-03a7-4af0-a6ec-0bc5b5814781"
    private var mCredentialProvider: CognitoCachingCredentialsProvider? = null
    private var mIoTEndpoint = "a1fljoeglhtf61-ats.iot.us-east-1.amazonaws.com"
    private var mIoTThingName = "NuAccelMeter"
    private var mRegion = Regions.US_EAST_1
    private val compositeDisposable = CompositeDisposable()
    val awsSubject : PublishSubject<String> = PublishSubject.create()
    private val mIoTSubject : PublishSubject<String> = PublishSubject.create()

    init {
        // Initialize AWS Cognito Credential Provider
        mCredentialProvider =
            CognitoCachingCredentialsProvider(
            applicationContext,
            mCognitoIdentityPoolId,
                mRegion)

        // Initialize AWS IoT Data Client
        mAWSIoTdataClient = AWSIotDataClient(mCredentialProvider)
        mAWSIoTdataClient!!.endpoint = mIoTEndpoint


        val dis = mIoTSubject.subscribeOn(Schedulers.io())
            .subscribe({
                val req = GetThingShadowRequest().withThingName(it)
                print("req=$req")
                val result = mAWSIoTdataClient?.getThingShadow(req)
                print("result=$result")
                if (result != null) {
                    val bytes = ByteArray(result.payload.remaining())
                    result.payload.get(bytes)
                    awsSubject.onNext(String(bytes))
                }
            }, {
                it.printStackTrace()
            })
        compositeDisposable.add(dis)
    }

    fun getIoTLatestStatus() {
        mIoTSubject.onNext(mIoTThingName)
    }

    fun destroy() {
        compositeDisposable.clear()
    }
}