package com.nuvoton.cloudconnector.alimqtt

import android.content.Context
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


public class AliMqttHandler(context: Context, val aliMqttOption: AiotMqttOption, val host: String) {
    private var mqttAndroidClient: MqttAndroidClient =
        MqttAndroidClient(context, host, aliMqttOption.clientId)

    public val messageRx = Observable.create<AliMqttMessage> { observer ->
        mqttAndroidClient.setCallback(object: MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                if (topic != null && message != null) {
                    observer.onNext(AliMqttMessage(topic, message))
                }
            }

            override fun connectionLost(cause: Throwable?) {
                if (cause != null)
                    observer.onError(cause)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })
    }

    public val connectRx = Observable.create<String> { observer ->
        try {
            val mqttOption = MqttConnectOptions();
            mqttOption.userName = aliMqttOption.username
            mqttOption.password = aliMqttOption.password.toCharArray()

            mqttAndroidClient.connect(mqttOption, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    observer.onNext("Mqtt Connect Success")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    observer.onNext("Mqtt Connect Failed")
                }
            })
        } catch (e: Exception) {
            observer.onError(e)
        }
    }

    public fun publishRx(message: String, qos: Int = 0, topic: String) : Observable<String> {
        return Observable.create<String> { observer ->
            try {
                if (!mqttAndroidClient.isConnected) {
                    mqttAndroidClient.connect()
                }

                val mqttMessage = MqttMessage();
                mqttMessage.payload = message.toByteArray()
                mqttMessage.qos = qos
                mqttAndroidClient.publish(topic, mqttMessage, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        observer.onNext("Message Sent")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        observer.onNext("Message not Sent")
                    }

                })
            } catch (e: Exception) {
                observer.onError(e)
            }
        }
    }

    public fun subscribeRx(topic: String, qos: Int = 0) : Observable<String> {
        return Observable.create<String> { observer ->
            try {
                mqttAndroidClient.subscribe(topic, qos, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        observer.onNext("Subscribe success")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        observer.onNext("Subscribe failed")
                    }

                })
            } catch (e: Exception) {
                observer.onError(e)
            }
        }
    }
}

public data class AliMqttMessage(val topic: String, val message: MqttMessage)