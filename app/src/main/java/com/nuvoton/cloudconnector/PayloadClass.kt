package com.nuvoton.cloudconnector

import com.google.gson.annotations.SerializedName

data class AliyunDataClass(@SerializedName("TS") var timestamp: String,
                           @SerializedName("X") var x: String,
                           @SerializedName("Y") var y: String,
                           @SerializedName("Z") var z: String) {
    override fun toString(): String {
        return "timestamp=$timestamp, \nx=$x, \ny=$y, \nx=$z"
    }
}

data class AWSDataClass(var timestamp: String,
                        var x: String,
                        var y: String,
                        var z: String)