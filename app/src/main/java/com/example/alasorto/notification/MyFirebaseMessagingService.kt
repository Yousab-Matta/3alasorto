package com.example.alasorto.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var mNotificationManager: MyNotificationManager

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        if (message.data.isNotEmpty()) {
            val title = message.data["title"]
            val desc = message.data["message"]
            val dataMap = message.data["dataMap"]

            mNotificationManager.textNotification(title, desc, dataMap)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}