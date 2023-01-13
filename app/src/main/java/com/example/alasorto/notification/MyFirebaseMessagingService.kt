package com.example.alasorto.notification

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
            val message = message.data["message"]

            mNotificationManager.textNotification(title, message)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}