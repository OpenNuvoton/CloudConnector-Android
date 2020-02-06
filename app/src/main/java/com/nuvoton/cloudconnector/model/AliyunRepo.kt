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
    private var readTopic = "/$mProductKey/$mDeviceName/get"
    private var writeTopic = "/$mProductKey/$mDeviceName/data"

    private val hostname = "$mProductKey.iot-as-mqtt.cn-shanghai.aliyuncs.com"
    private val port = 1883
    private val qos = 0

    val aliyunDataSubject: PublishSubject<HashMap<String, Any?>> = PublishSubject.create()

    // Implement abstract functions
    override fun start() {
        mqttClient.setHost(hostname)
        mqttClient.setPort(port)
        mqttClient.setProductKey(mProductKey)
        mqttClient.setDeviceName(mDeviceName)
        mqttClient.setDeviceSecret(mDeviceSecret)
        try {
            startNotifyTimer()
            mqttClient.connect()
            mqttClient.subscribe(readTopic, qos) { topic, qos, payload ->
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
        return listOf("Host name:\n$hostname",
            "MQTTTopic:\n$readTopic")
    }

    override fun updateSetting(context: Context) {
        val productKey = context.getPrefString("pref_aliyun_product_key")
        if (productKey != null) {
            mProductKey = productKey.toString()
        }

        val deviceName = context.getPrefString("pref_aliyun_device_name")
        if (deviceName != null) {
            mDeviceName = deviceName.toString()
        }

        val deviceSecret = context.getPrefString("pref_aliyun_device_secret")
        if (deviceSecret != null) {
            mDeviceSecret = deviceSecret.toString()
        }
    }
}