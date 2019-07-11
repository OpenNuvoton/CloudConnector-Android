package com.nuvoton.cloudconnector.model

import android.content.Context
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.iotdata.AWSIotDataClient
import com.amazonaws.services.iotdata.model.GetThingShadowRequest
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.nuvoton.cloudconnector.debug
import com.nuvoton.cloudconnector.fromJsonString
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

// icon credits: <div>Icons made by <a href="https://www.freepik.com/?__hstc=57440181.bc7bb437dc895a5e02ce4844a8fedf46.1561102499736.1561367496128.1562656684616.5&__hssc=57440181.7.1562656684616&__hsfp=2168691457" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/"                 title="Flaticon">www.flaticon.com</a> is licensed by <a href="http://creativecommons.org/licenses/by/3.0/"                 title="Creative Commons BY 3.0" target="_blank">CC 3.0 BY</a></div>

class AWSRepo(applicationContext: Context) : RepositoryCommon() {
    private var mAWSIoTdataClient: AWSIotDataClient? = null
    private var mCognitoIdentityPoolId = "us-east-1:9e41d4ca-03a7-4af0-a6ec-0bc5b5814781"
    private var mCredentialProvider: CognitoCachingCredentialsProvider? = null
    private var mIoTEndpoint = "a1fljoeglhtf61-ats.iot.us-east-1.amazonaws.com"
    private var mIoTThingName = "NuAccelMeter"
    private var mRegion = Regions.US_EAST_1
    private val lifeCycleDisposable = CompositeDisposable()
    val awsDataSubject : PublishSubject<Map<String, Any?>> = PublishSubject.create()
    private val iotRequestSubject : PublishSubject<String> = PublishSubject.create()

    private val gson = Gson()
    private var test = 0

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
        startNotifyTimer()
        val dis = iotRequestSubject.subscribeOn(Schedulers.io()).map { iotThingName ->
            val req = GetThingShadowRequest().withThingName(iotThingName)
            debug("req ${test++}=$req")
            mAWSIoTdataClient?.getThingShadow(req)
        }.subscribe({ result ->
                if (result != null) {
                    notifyRepoIsAlive()
                    val bytes = ByteArray(result.payload.remaining())
                    result.payload.get(bytes)
                    val json = String(bytes)
                    val map: Map<String, Any?> = gson.fromJsonString(json)
                    val state = map["state"] as Map<String, Any?>
                    val reported = state["reported"] as Map<String, Any?>
                    awsDataSubject.onNext(reported)
                }
            }, {
                it.printStackTrace()
            })
        lifeCycleDisposable.add(dis)
    }

    fun getIoTLatestStatus() {
        iotRequestSubject.onNext(mIoTThingName)
    }

    fun destroy() {
        lifeCycleDisposable.dispose()
    }
}