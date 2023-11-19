package com.example.alasorto

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.alasorto.dataClass.UserData
import com.example.alasorto.utils.InternetCheck
import com.example.alasorto.utils.OTPView
import com.example.alasorto.viewModels.AppViewModel
import com.example.alasorto.viewModels.AuthViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import java.util.concurrent.TimeUnit

class VerifyPhoneFragment : Fragment() {

    private val appViewModel: AppViewModel by viewModels()

    private lateinit var builder: AlertDialog.Builder
    private lateinit var dialog: AlertDialog
    private lateinit var window: Window
    private lateinit var timerTV: TextView
    private lateinit var mActivity: AuthActivity
    private lateinit var otpLayout: ConstraintLayout
    private lateinit var id: String
    private lateinit var currentUserNumber: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var keepLoggedInCb: CheckBox
    private lateinit var verifyBtn: Button
    private lateinit var verifyCode: String
    private lateinit var otpView: OTPView
    private lateinit var internetCheck: InternetCheck

    private var hasConnection = false
    private var currentUser: UserData? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_verify_phone, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        val args = this.arguments
        if (args != null) {
            id = args.getString("ID")!!
            currentUserNumber = args.getString("Number")!!
            resendToken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                args.getParcelable("TOKEN", PhoneAuthProvider.ForceResendingToken::class.java)!!
            } else {
                args.getParcelable("TOKEN")!!
            }
        }

        verifyBtn = view.findViewById(R.id.btn_verify)
        otpLayout = view.findViewById(R.id.layout_otp)
        timerTV = view.findViewById(R.id.tv_verification_timer)
        keepLoggedInCb = view.findViewById(R.id.cb_keep_logged_in)

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
        }

        //Create Alert dialogue
        builder = AlertDialog.Builder(requireContext())
        builder.setCancelable(false)
        builder.setView(R.layout.progress_dialogue)
        dialog = builder.create()
        window = dialog.window!!
        window.setBackgroundDrawableResource(android.R.color.transparent)

        //Initialize Activity
        mActivity = activity as AuthActivity

        otpView = OTPView(otpLayout, mActivity)

        appViewModel.userByIdMLD.observe(this.viewLifecycleOwner, Observer {
            dialog.dismiss()
            if (it != null) {
                currentUser = it
                if (it.verified) {
                    goToMainActivity()
                } else {
                    goToPendingVerificationFragment()
                }
            } else {
                goToCreateUserDataFragment()
            }
        })


        timerTV.setOnClickListener(View.OnClickListener {
            resendVerificationCode(currentUserNumber, resendToken)
            verifyBtn.isClickable = false
            timerTV.isClickable = false
        })

        verifyBtn.setOnClickListener(View.OnClickListener
        {
            verifyCode = otpView.getCode()

            if (verifyCode.length == 6) {
                verifyPhoneNumberWithCode(verifyCode)
                dialog.show()
            }
        })
    }

    override fun onResume() {
        super.onResume()

        //Observes Changes in user
        Firebase.firestore.collection("Users")
            .addSnapshotListener { _, _ ->
                if (currentUser != null) {
                    appViewModel.getUserById(currentUserNumber)
                }
            }
    }

    private fun verifyPhoneNumberWithCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(id, code)

        Firebase.auth.signInWithCredential(credential)
            .addOnCompleteListener(AuthActivity()) { task ->
                if (task.isSuccessful) {
                    //Set Keep Logged in SharedPreferences
                    if (keepLoggedInCb.isChecked) {
                        val sharedPreferences =
                            requireContext().getSharedPreferences(
                                "KeepLoggedIn",
                                Context.MODE_PRIVATE
                            )
                        val editor = sharedPreferences!!.edit()
                        editor.putBoolean("IsLoggedIn", true)
                        editor.apply()
                    }
                    appViewModel.getUserById(currentUserNumber)
                } else {
                    dialog.dismiss()

                    //Disable Verify Btn to avoid multiple callbacks
                    verifyBtn.isClickable = false

                    timerTV.visibility = View.VISIBLE
                    startTimer()
                }
            }
    }

    private fun goToMainActivity() {
        //Go to MainActivity
        val intent = Intent(context, MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun resendVerificationCode(
        phoneNumber: String,
        token: PhoneAuthProvider.ForceResendingToken?
    ) {
        val optionsBuilder = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(AuthActivity())                 // Activity (for callback binding)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                }

                override fun onVerificationFailed(p0: FirebaseException) {
                }
            })           // OnVerificationStateChangedCallbacks
        if (token != null) {
            optionsBuilder.setForceResendingToken(token) // callback's ForceResendingToken
        }
        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    private fun goToCreateUserDataFragment() {
        val fragment = CreateUserDataFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val args = Bundle()
        args.putBoolean("IS_POINTS_ET_ENABLED", false)
        args.putString("PHONE_NUM", currentUserNumber)
        args.putBoolean("isNewUser", true)
        fragment.arguments = args
        transaction.replace(R.id.auth_frame, fragment)
        transaction.commit()
    }

    private fun goToPendingVerificationFragment() {
        val fragment = PendingVerificationFragment()
        val manager = requireActivity().supportFragmentManager
        val args = Bundle()
        args.putBoolean("KEEP_LOGGED_IN", keepLoggedInCb.isChecked)
        fragment.arguments = args
        val transaction = manager.beginTransaction()
        transaction.replace(R.id.auth_frame, fragment)
        transaction.commit()
    }

    private fun startTimer() {
        timerTV.isClickable = false
        //Create a 1 min Counter before sending new Code
        object : CountDownTimer(60000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(p0: Long) {
                timerTV.text = "Resending new code in ${p0 / 1000}"
            }

            override fun onFinish() {
                verifyBtn.isClickable = true
                timerTV.isClickable = true
                timerTV.text = getString(R.string.resend_code)
            }
        }.start()
    }
}