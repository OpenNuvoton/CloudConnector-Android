package com.nuvoton.cloudconnector.model

import com.example.aiotcore.AiotMqttClient
import com.example.aiotcore.AiotMqttException
import com.google.gson.Gson
import com.nuvoton.cloudconnector.fromJsonString
import io.reactivex.subjects.PublishSubject

class AliyunRepo : RepositoryCommon() {
    private val mqttClient = AiotMqttClient()

    private val productKey = "a1Ll7sjheeL"
    private val deviceName = "PfrfuKsWweTxOnuG8wo4"
    private val deviceSecret = "EBsP2YuU486ybGOXiNcGlrtKQWMQs48H"
    private val readTopic = "/$productKey/$deviceName/get"
    private val writeTopic = "/$productKey/$deviceName/data"

    private val hostname = "$productKey.iot-as-mqtt.cn-shanghai.aliyuncs.com"
    private val port = 1883
    private val qos = 0
    private val gson = Gson()

    val aliyunSubject: PublishSubject<HashMap<String, Any?>> = PublishSubject.create()

    init {
        mqttClient.setHostName(hostname)
        mqttClient.setPort(port)
        mqttClient.setProductKey(productKey)
        mqttClient.setDeviceName(deviceName)
        mqttClient.setDeviceSecret(deviceSecret)
        try {
            startNotifyTimer()
            mqttClient.connect()
            mqttClient.subscribe(readTopic, qos) { topic, payload ->
                // remove last 2 bytes to bypass the issue
                val payloadString = String(payload.copyOfRange(0, payload.size - 2))
                // change to hashmap in order to map all data sources
//                val aliyunDataClass = gson.fromJson(payloadString, AliyunDataClass::class.java)
                notifyRepoIsAlive()
                aliyunSubject.onNext(gson.fromJsonString(payloadString))
            }
        } catch (e: AiotMqttException) {
            e.printStackTrace()
        }
    }

    fun destroy() {
        mqttClient.disconnect()
    }
}