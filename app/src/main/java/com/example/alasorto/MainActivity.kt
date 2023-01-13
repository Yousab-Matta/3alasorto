package com.example.alasorto

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.alasorto.dataClass.Users
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private val auth = Firebase.auth
    private val authUser = auth.currentUser
    private val phoneNum = authUser?.phoneNumber
    private lateinit var viewModel: AppViewModel
    private lateinit var internetCheck: InternetCheck
    private lateinit var connectionTV: TextView
    private lateinit var currentUser: Users
    private var hasConnection = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectionTV = findViewById(R.id.tv_connection)

        internetCheck = InternetCheck(application)
        internetCheck.observe(this) {
            if (it) {
                if (!hasConnection) {
                    connectionTV.visibility = GONE
                    viewModel.getCurrentUser(phoneNum!!)
                    viewModel.userToken()
                }
            } else {
                connectionTV.text = getString(R.string.disconnected)
                connectionTV.visibility = VISIBLE
            }
            hasConnection = it
        }

        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        viewModel.currentUserMLD.observe(this, Observer {
            if (it != null) {
                currentUser = it
                val fragment = HomeFragment()
                val manager = supportFragmentManager
                val transaction = manager.beginTransaction()
                val bundle = Bundle()
                bundle.putParcelable("CURRENT_USER", it)
                fragment.arguments = bundle
                transaction.add(R.id.main_frame, fragment)
                transaction.commit()
            } else {
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
        })
    }

    fun getCurrentUser(): Users {
        return currentUser
    }
}