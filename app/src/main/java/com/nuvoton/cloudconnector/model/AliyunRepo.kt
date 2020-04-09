package com.nuvoton.cloudconnector.model

import android.content.Context
import com.linkkit.aiotcore.AiotMqttClient
import com.linkkit.aiotcore.AiotMqttException
import com.nuvoton.cloudconnector.fromJsonString
import com.nuvoton.cloudconnector.getPrefString
import io.reactivex.subjects.PublishSubject

class AliyunRepo : RepositoryCommon() {
    private val mqttClient = AiotMqttClient()

    private var mProductKey = "a1Ll7sjheeL"
    private var mDeviceName = "PfrfuKsWweTxOnuG8wo4"
    private var mDeviceSecret = "EBsP2YuU486ybGOXiNcGlrtKQWMQs48H"
    private var mReadTopic = "/$mProductKey/$mDeviceName/get"
    private var mWriteTopic = "/$mProductKey/$mDeviceName/data"

    private var mHostname = "$mProductKey.iot-as-mqtt.cn-shanghai.aliyuncs.com"
    private val port = 1883
    private val qos = 0

    val aliyunDataSubject: PublishSubject<HashMap<String, Any?>> = PublishSubject.create()

    // Implement abstract functions
    override fun start() {
        mqttClient.setHost(mHostname)
        mqttClient.setPort(port)
        mqttClient.setProductKey(mProductKey)
        mqttClient.setDeviceName(mDeviceName)
        mqttClient.setDeviceSecret(mDeviceSecret)
        try {
            startNotifyTimer()
            mqttClient.connect()
            mqttClient.subscribe(mReadTopic, qos) { topic, qos, payload ->
                // remove last 2 bytes to bypass the issue
                val payloadString = String(payload.copyOfRange(0, payload.size - 2))
                // change to hashmap in order to map all data sources
//                val aliyunDataClass = gson.fromJson(payloadString, AliyunDataClass::class.java)
                notifyRepoIsAlive()
                val map : HashMap<String, Any?> = gson.fromJsonString(payloadString)
                map["timestamp"] = getTimeSecond()
                aliyunDataSubject.onNext(map)
            }
        } catch (e: AiotMqttException) {
            e.printStackTrace()
        }
    }

    override fun pause() {
        stopTimer()
        mqttClient.disconnect()
    }

    override fun destroy() {
        mqttClient.finalize()
    }

    override fun getCloudSetting(): List<String> {
        return listOf("Host name:\n$mHostname",
            "MQTTTopic:\n$mReadTopic")
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