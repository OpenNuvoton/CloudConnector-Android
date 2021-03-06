package com.nuvoton.cloudconnector.alimqtt;


import android.util.Log;

import java.math.BigInteger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AiotMqttOption {
    private String username = "";
    private String password = "";
    private String clientId = "";

    public String getUsername() { return this.username;}
    public String getPassword() { return this.password;}
    public String getClientId() { return this.clientId;}

    /**
     * @param productKey
     * @param deviceName
     * @param deviceSecret
     * @return AiotMqttOption
     */
    public AiotMqttOption getMqttOption(String productKey, String deviceName, String deviceSecret) {
        if (productKey == null || deviceName == null || deviceSecret == null) {
            return null;
        }

        try {
            String timestamp = Long.toString(System.currentTimeMillis());

            // clientId
            this.clientId = productKey + "." + deviceName + "|timestamp=" + timestamp +
                    ",_v=paho-android-1.0.0,securemode=2,signmethod=hmacsha256|";

            // userName
            this.username = deviceName + "&" + productKey;

            // password
            String macSrc = "clientId" + productKey + "." + deviceName + "deviceName" +
                    deviceName + "productKey" + productKey + "timestamp" + timestamp;

            Log.i("mqttOption" ,   "password:"+"clientId" + productKey + "." + deviceName + "deviceName" +
                    deviceName + "productKey" + productKey + "timestamp123456789") ;

            String algorithm = "HmacSHA256";
            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec secretKeySpec = new SecretKeySpec(deviceSecret.getBytes(), algorithm);
            mac.init(secretKeySpec);
            byte[] macRes = mac.doFinal(macSrc.getBytes());
            this.password = String.format("%064x", new BigInteger(1, macRes));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return this;
    }
}