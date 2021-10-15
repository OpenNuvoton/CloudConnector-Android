package com.nuvoton.cloudconnector.model

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceManager
import com.nuvoton.cloudconnector.alimqtt.AiotMqttOption
import com.nuvoton.cloudconnector.alimqtt.AliMqttHandler
import com.nuvoton.cloudconnector.fromJsonString
import com.nuvoton.cloudconnector.getPrefString
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject


class NuAliyunRepo(val context: Context) : RepositoryCommon() {
    private var mProductKey = ""
    private var mDeviceName = ""
    private var mDeviceSecret = ""
    private var mReadTopic = "/$mProductKey/$mDeviceName/get"
    private var mWriteTopic = "/$mProductKey/$mDeviceName/data"

    private var mHostname = "tcp://$mProductKey.iot-as-mqtt.cn-shanghai.aliyuncs.com:443"
    private val port = 1883
    private val qos = 0
    private var mqttClient : AliMqttHandler? = null

    val aliyunDataSubject: PublishSubject<HashMap<String, Any?>> = PublishSubject.create()

    // Implement abstract functions
    override fun start() {

        if (mProductKey == "" || mDeviceName == "" || mDeviceSecret == "") return
        val mqttOption = AiotMqttOption().getMqttOption(mProductKey, mDeviceName, mDeviceSecret);

        mqttClient = AliMqttHandler(context, mqttOption, mHostname);
        startNotifyTimer()
        if (mqttClient != null) {
            val dis = mqttClient!!.connectRx.subscribeOn(Schedulers.io())
                .flatMap { connected ->
                    if (connected) {
                        mqttClient!!.subscribeRx(mReadTopic)
                    } else {
                        throw Exception("")
                    }
                }.flatMap { subscribed ->
                    if (subscribed) {
                        mqttClient!!.messageRx
                    } else {
                        throw Exception("")
                    }
                }.subscribe({ mqttMessage ->
                    notifyRepoIsAlive()
                    val map: HashMap<String, Any?> =
                        gson.fromJsonString(mqttMessage.message.toString())
                    map["timestamp"] = getTimeSecond()
                    aliyunDataSubject.onNext(map)
                }, { error ->
                    aliyunDataSubject.onError(error)
                })
            lifeCycleDisposable.add(dis)
        }
    }

    override fun pause() {
        stopTimer()
        if (mqttClient != null) mqttClient!!.disconnect()
        lifeCycleDisposable.clear()
    }

    override fun destroy() {

    }

    override fun getCloudSetting(): List<String> {
        return listOf(
            "Host name:\n$mHostname",
            "MQTTTopic:\n$mReadTopic"
        )
    }

    override fun updateSetting(context: Context) {

        val productKey = context.getPrefString("pref_aliyun_product_key")
        if (productKey != null) {
            mProductKey = productKey.toString()
            mHostname = "$mProductKey.iot-as-mqtt.cn-shanghai.aliyuncs.com"
        }

        val deviceName = context.getPrefString("pref_aliyun_device_name")
        if (deviceName != null) {
            mDeviceName = deviceName.toString()
            mReadTopic = "/$mProductKey/$mDeviceName/user/get"
            mWriteTopic = "/$mProductKey/$mDeviceName/user/data"
        }

        val deviceSecret = context.getPrefString("pref_aliyun_device_secret")
        if (deviceSecret != null) {
            mDeviceSecret = deviceSecret.toString()
        }

        val getTopic = context.getPrefString("pref_aliyun_topic_get")
        if (getTopic != null) {
            mReadTopic = "/$mProductKey/$mDeviceName/$getTopic"
        }

        val dataTopic = context.getPrefString("pref_aliyun_topic_data")
        if (dataTopic != null) {
            mWriteTopic = "/$mProductKey/$mDeviceName/$dataTopic"
        }
    }
}