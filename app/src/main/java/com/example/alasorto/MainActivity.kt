package com.example.alasorto

import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.alasorto.dataClass.Users
import com.example.alasorto.notification.Data
import com.example.alasorto.notification.NotificationModel
import com.example.alasorto.notification.NotificationViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val auth = Firebase.auth
    private val authUser = auth.currentUser
    private val phoneNum = authUser?.phoneNumber
    private lateinit var viewModel: AppViewModel
    private lateinit var internetCheck: InternetCheck
    private lateinit var connectionTV: TextView
    private lateinit var currentUser: Users
    private var hasConnection = false

    private val allUsersList = ArrayList<Users>()

    private val notificationViewModel: NotificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectionTV = findViewById(R.id.tv_connection)

        //Initialize app VM
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        //Check internet Connection
        internetCheck = InternetCheck(application)
        internetCheck.observe(this) {
            if (it) {
                if (!hasConnection) {
                    connectionTV.visibility = GONE
                    viewModel.getCurrentUser(phoneNum!!)
                    viewModel.getAllUsers()
                    viewModel.userToken()
                }
            } else {
                connectionTV.text = getString(R.string.disconnected)
                connectionTV.visibility = VISIBLE
            }
            hasConnection = it
        }

        //NotificationViewModel
        notificationViewModel.connectionError.observe(this) {
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
        }

        //Go to some fragment when current user is loaded
        viewModel.currentUserMLD.observe(this, Observer {
            if (savedInstanceState == null) {
                if (it != null) {
                    currentUser = it //Sets value to current user var
                    //Go tp Home Fragment
                    val fragment = HomeFragment()
                    val manager = supportFragmentManager
                    val transaction = manager.beginTransaction()
                    val bundle = Bundle()
                    bundle.putParcelable("CURRENT_USER", it)
                    fragment.arguments = bundle
                    transaction.add(R.id.main_frame, fragment)
                    transaction.commit()
                    viewModel.currentUserMLD.removeObservers(this)
                } else {
                    //If no Data for User go to CreateData fragment
                    val manager = supportFragmentManager
                    val transaction = manager.beginTransaction()
                    val fragment = CreateUserDataFragment()
                    val bundle = Bundle()
                    bundle.putBoolean("isNewUser", true)
                    bundle.putString("ID", phoneNum)
                    fragment.arguments = bundle
                    transaction.add(R.id.main_frame, fragment)
                    transaction.commit()
                }
            }
        })

        viewModel.usersMLD.observe(this, Observer {
            if (it != null) {
                allUsersList.clear()
                allUsersList.addAll(it)
            }
        })

    }

    fun createNotification(token: String, title: String, message: String) {
        notificationViewModel
            .sendNotification(
                NotificationModel(
                    token,
                    Data(title, message)
                )
            )
    }

    fun getCurrentUser(): Users {
        return currentUser
    }

    fun getAllUsers(): ArrayList<Users> {
        return allUsersList
    }
}