package com.example.alasorto

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.alasorto.viewModels.AuthViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

class RegisterFragment : Fragment(R.layout.fragment_register) {
    private lateinit var mActivity: AuthActivity
    private lateinit var phoneNumTV: EditText
    private lateinit var sendCodeBtn: Button
    private lateinit var number: String
    private lateinit var id: String
    private lateinit var credential: PhoneAuthCredential
    private lateinit var builder: AlertDialog.Builder
    private lateinit var dialog: AlertDialog
    private lateinit var window: Window
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var viewModel: AuthViewModel

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.isClickable = true

        phoneNumTV = view.findViewById(R.id.tv_phone_num)
        sendCodeBtn = view.findViewById(R.id.btn_send_code)

        mActivity = (activity as AuthActivity)

        //Create Alert Dialogue
        builder = AlertDialog.Builder(context)
        builder.setCancelable(false)
        builder.setView(R.layout.progress_dialogue)
        dialog = builder.create()
        window = dialog.window!!
        window.setBackgroundDrawableResource(android.R.color.transparent)

        //Initialize View Model
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        sendCodeBtn.setOnClickListener(View.OnClickListener {
            number = phoneNumTV.text.toString()
            if (number.isNotEmpty() && number.length == 11) {
                if (number[0] != '+' && number[1] != '2') {
                    number = "+2$number"
                }
                sendVerificationCode(number)
                dialog.show()
            }
        })
    }

    private fun sendVerificationCode(number: String) {
        val options =
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(number) // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(requireActivity()) // Activity (for callback binding)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                        credential = p0
                    }

                    override fun onVerificationFailed(p0: FirebaseException) {
                    }

                    override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
                        super.onCodeSent(p0, p1)
                        id = p0
                        resendToken = p1
                        dialog.dismiss()
                        goToFragment()
                    }
                })          // OnVerificationStateChangedCallbacks

        PhoneAuthProvider.verifyPhoneNumber(options.build())
    }

    fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(AuthActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("AUTH_VIEW_MODEL", "signInWithCredential:success")
                } else {
                    Log.d("AUTH_VIEW_MODEL", "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        Log.d("AUTH_VIEW_MODEL", "signInWithPhoneAuthCredential: ${task.exception}")
                    }
                    // Update UI
                }
            }
    }

    private fun goToFragment() {
        val fragment = VerifyPhoneFragment()
        val manager = (activity as AuthActivity).supportFragmentManager
        val transaction = manager.beginTransaction()
        val bundle = Bundle()
        bundle.putString("ID", id)
        bundle.putString("Number", number)
        bundle.putParcelable("TOKEN", resendToken)
        fragment.arguments = bundle
        transaction.replace(R.id.auth_frame, fragment)
        transaction.commit()
    }
}