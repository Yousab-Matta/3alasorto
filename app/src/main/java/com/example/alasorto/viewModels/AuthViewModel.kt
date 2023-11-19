package com.example.alasorto.viewModels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.alasorto.AuthActivity
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class AuthViewModel : ViewModel() {

    companion object {
        private const val TAG = "AUTH_VIEW_MODEL"
    }

    var resendTokenMLD = MutableLiveData<PhoneAuthProvider.ForceResendingToken>()
    var authIdMLD = MutableLiveData<String>()
    var credentialMLD = MutableLiveData<PhoneAuthCredential>()
    var logInSuccessMLD = MutableLiveData<Boolean>()
    var verificationMLD = MutableLiveData<Boolean>()

    //private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private val auth: FirebaseAuth = Firebase.auth

    private val callbacks =
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                verificationMLD.value = true
                //signInWithPhoneAuthCredential(credential)
                Log.d(TAG, "onVerificationCompleted: ")
            }

            override fun onVerificationFailed(e: FirebaseException) {
                verificationMLD.value = false
                Log.d(TAG, "onVerificationFailed", e)
                if (e is FirebaseAuthInvalidCredentialsException) {
                    Log.d(TAG, "onVerificationFailed: $e")
                } else if (e is FirebaseTooManyRequestsException) {
                    Log.d(TAG, "onVerificationFailed: $e")
                }
            }

            override fun onCodeSent(
                verificationId: String, token: PhoneAuthProvider.ForceResendingToken
            ) {
                authIdMLD.value = verificationId
                resendTokenMLD.value = token
            }
        }

    fun sendVerificationCode(number: String) {
        val options =
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(number)       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(AuthActivity())                 // Activity (for callback binding)
                .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
                .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyPhoneNumberWithCode(verificationId: String?, code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        credentialMLD.value = credential
    }




}