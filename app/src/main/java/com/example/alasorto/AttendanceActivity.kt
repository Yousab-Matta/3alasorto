package com.example.alasorto

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.alasorto.notification.Data
import com.example.alasorto.notification.NotificationModel
import com.example.alasorto.notification.NotificationViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AttendanceActivity : AppCompatActivity() {
    private val manager = supportFragmentManager
    private val transaction = manager.beginTransaction()
    private val notificationViewModel: NotificationViewModel by viewModels()

    //ToDo: Check internet connection b4 sending message

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        if (savedInstanceState == null) {
            transaction.add(
                R.id.att_frame,
                AttendanceHistoryFragment(),
                "ATT_HISTORY_FRAGMENT"
            )
            transaction.commit()
        }

        //NotificationViewModel
        /*notificationViewModel.connectionError.observe(this) {
            when (it) {
                "sending" -> {
                    Toast.makeText(this, "sending notification", Toast.LENGTH_SHORT).show()
                }
                "sent" -> {
                    Toast.makeText(this, "notification sent", Toast.LENGTH_SHORT).show()
                }
                "error while sending" -> {
                    Toast.makeText(this, "error while sending", Toast.LENGTH_SHORT).show()
                }
            }
        }

        notificationViewModel.response.observe(this) {
            if (it.isNotEmpty())
                Log.d("NOTIFICATION_TEST", "Notification in Kotlin: $it ")
        }*/
    }

    fun createNotification(token: String) {
        notificationViewModel
            .sendNotification(
                NotificationModel(
                    token,
                    Data("3ala Sorto", "5 Points were added because you attended today :)")
                )
            )
    }
}