/*
 * Copyright 2026 Nuvoton Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nuvoton.cloudconnector.alimqtt

import android.content.Context
import android.util.Log
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


public class AliMqttHandler(context: Context, val aliMqttOption: AiotMqttOption, val host: String) {
    private lateinit var mqttAndroidClient: MqttAndroidClient
//    = MqttAndroidClient(context, host, aliMqttOption.clientId)

    public val messageRx = Observable.create<AliMqttMessage> { observer ->
        mqttAndroidClient.setCallback(object: MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {

                if (topic != null && message != null) {
                    Log.i("AliMqttHandler","messageArrived    topic:"+topic  + "   message:"+message)
                    observer.onNext(AliMqttMessage(topic, message))
                }
            }

            override fun connectionLost(cause: Throwable?) {
                if (cause != null) {
                    observer.onError(cause)
                    cause.printStackTrace();
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })
    }

    public val connectRx = Observable.create<Boolean> { observer ->
        try {
            val mqttOption = MqttConnectOptions();
            mqttOption.userName = aliMqttOption.username
            mqttOption.password = aliMqttOption.password.toCharArray()

            Log.i("mqttOption" ,   "clientId:"+aliMqttOption.clientId+"  userName:"+mqttOption.userName +"   password:"+mqttOption.password  +"       "+aliMqttOption.password)

            mqttAndroidClient = MqttAndroidClient(context,"tcp://"+host+":1883",aliMqttOption.clientId)

            mqttAndroidClient.connect(mqttOption, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.i("AliMqttHandler" ,   "Ali mqttAndroidClient connect onSuccess")
                    observer.onNext(true)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.i("AliMqttHandler" ,   "Ali mqttAndroidClient connect onFailure")
                    observer.onNext(false)
                }
            })
        } catch (e: Exception) {
            observer.onError(e)
        }
    }

    public fun publishRx(message: String, qos: Int = 0, topic: String) : Observable<Boolean> {
        return Observable.create<Boolean> { observer ->
            try {
                if (!mqttAndroidClient.isConnected) {
                    mqttAndroidClient.connect()
                }

                val mqttMessage = MqttMessage();
                mqttMessage.payload = message.toByteArray()
                mqttMessage.qos = qos
                mqttAndroidClient.publish(topic, mqttMessage, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        observer.onNext(true)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        observer.onNext(true)
                    }

                })
            } catch (e: Exception) {
                observer.onError(e)
            }
        }
    }

    public fun subscribeRx(topic: String, qos: Int = 0) : Observable<Boolean> {
        return Observable.create<Boolean> { observer ->
            try {
                mqttAndroidClient.subscribe(topic, qos, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        observer.onNext(true)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        observer.onNext(false)
                    }

                })
            } catch (e: Exception) {
                observer.onError(e)
            }
        }
    }

    public fun disconnect() {
        mqttAndroidClient.disconnect()
    }
}

public data class AliMqttMessage(val topic: String, val message: MqttMessage)