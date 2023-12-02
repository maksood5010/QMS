package com.truelife.qms

import android.app.Application
import android.content.Context
import android.util.Log
import com.truelife.qms.utils.Constants.serverURI
import com.truelife.qms.utils.MQTTClient
import org.eclipse.paho.client.mqttv3.MqttClient

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        MyApplication.appContext = applicationContext
//        FirebaseApp.initializeApp(this)
//        val clientId = MQTT_CLIENT_ID MQTT_SERVER_URI
        MyDeviceId = MqttClient.generateClientId()

        Log.d("TAG", "onViewCreated: serverURI : $serverURI")
        Log.d("TAG", "onViewCreated: clientId : $MyDeviceId")

        mqttClient = MQTTClient(this, serverURI, MyDeviceId)

    }

    companion object {
        lateinit var appContext: Context
        lateinit var mqttClient: MQTTClient
        lateinit var MyDeviceId: String

    }
}