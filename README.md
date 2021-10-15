# NuCloudConnector-Android

* NuCloudConnector is designed to connect different IoT cloud server which monitors IoT devices' status.
For those who are interested in connecting to IoT cloud servers can apply support form for more information.
[Support Form](https://www.nuvoton.com/support/technical-support/form/)

## Build Environment
* IDE: Android Studio 4.0
* Build System: gradle-5.4.1-all.zip

## How to Build
* Open the project file with Android Studio
* Wait for Gradle system to finish syncing
* Click tab "Build" and click "Make Project"
* That's it!!

## How to add cloud settings
### AWS:
* 4 parameters are required to receive IoT data
    * mIoTThingName: The IoTThing's name
    * mIoTEndPoint: The IoTThing's endpoint
    * mCognitoIdentityPoolId: The cognito pool ID for those who has the permission to get the device status
    * mRegion: The region the IoT device registered. E.g.: AWSRegionType.USEast1

### Pelion:
* 4 parameters are required to receive IoT data
    * mApiKey: The API key that has the permission to received IoT values
    * mRequestHostname: The device's setup region. E.g. "api.us-east-1.mbedcloud.com"
    * mDeviceId: The device's ID string.
    * mResource: The resource - such as a thermometer - string that the IoT device registered. E.g. "3303/0/5700"

### Aliyun:
* 3 parameters are required to send/receive IoT data
    * mProductKey: The product key of the IoT device
    * mDeviceName: The device name of the IoT device
    * mDeviceSecret: The device secret of the IoT device
