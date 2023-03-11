package com.example.alasorto

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import java.util.*

@SuppressLint("CustomSplashScreen")
class SplashScreenFragment : Fragment() {
    private var isLoggedIn = false
    private val timer = Timer()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_splash_screen, container, false)

        val sharedPreferences =
            requireActivity().getSharedPreferences("KeepLoggedIn", Context.MODE_PRIVATE)
        isLoggedIn = sharedPreferences.getBoolean("IsLoggedIn", false)

        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (isLoggedIn) {
                    val intent = Intent(requireActivity(), MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    val manager = requireActivity().supportFragmentManager
                    val transaction = manager.beginTransaction()
                    transaction.replace(R.id.auth_frame, RegisterFragment())
                    transaction.commit()
                }
            }
        }, 1000, 3000)
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }
}