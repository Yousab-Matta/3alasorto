package com.example.alasorto.notification

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.alasorto.AuthActivity
import com.example.alasorto.MainActivity
import com.example.alasorto.R
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

class MyNotificationManager @Inject constructor(private val mCtx: Application) {

    fun textNotification(title: String?, message: String?, map: String?) {
        val rand = Random()
        val idNotification = rand.nextInt(1000000000)

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationManager = mCtx.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(mCtx, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("DATA_MAP", map)

        val pendingIntent =
            PendingIntent.getActivity(mCtx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "Channel_id_default", "Channel_name_default", NotificationManager.IMPORTANCE_HIGH
            )
            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            notificationChannel.description = "Channel_description_default"
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.setSound(soundUri, attributes)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val notificationBuilder = NotificationCompat.Builder(mCtx, "Channel_id_default")


        notificationBuilder.setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setTicker(mCtx.resources.getString(R.string.app_name))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setContentTitle(title)
            .setContentText(message)

        notificationBuilder.setContentIntent(pendingIntent)

        notificationManager.notify(idNotification, notificationBuilder.build())
    }

}