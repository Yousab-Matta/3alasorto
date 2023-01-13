package com.example.alasorto

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import java.util.*

class VerifyPhoneFragment : Fragment() {

    private lateinit var otpTV1: EditText
    private lateinit var otpTV2: EditText
    private lateinit var otpTV3: EditText
    private lateinit var otpTV4: EditText
    private lateinit var otpTV5: EditText
    private lateinit var otpTV6: EditText
    private lateinit var builder: AlertDialog.Builder
    private lateinit var dialog: AlertDialog
    private lateinit var window: Window
    private lateinit var timer: TextView
    private lateinit var number: String
    private lateinit var mActivity: AuthActivity
    private lateinit var viewModel: AuthViewModel
    private lateinit var credential: PhoneAuthCredential
    private lateinit var id: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var keepLoggedInCb: CheckBox
    private lateinit var verifyBtn: Button
    private lateinit var verifyCode: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_verify_phone, container, false)

        val args = this.arguments
        id = args?.getString("ID")!!
        number = args.getString("Number")!!

        //Initialize EditTexts
        otpTV1 = view.findViewById(R.id.et_otp_1)
        otpTV2 = view.findViewById(R.id.et_otp_2)
        otpTV3 = view.findViewById(R.id.et_otp_3)
        otpTV4 = view.findViewById(R.id.et_otp_4)
        otpTV5 = view.findViewById(R.id.et_otp_5)
        otpTV6 = view.findViewById(R.id.et_otp_6)
        timer = view.findViewById(R.id.tv_verification_timer)
        keepLoggedInCb = view.findViewById(R.id.cb_keep_logged_in)
        verifyBtn = view.findViewById(R.id.btn_verify)

        //Create Alert dialogue
        builder = AlertDialog.Builder(context)
        builder.setCancelable(false)
        builder.setView(R.layout.progress_dialogue)
        dialog = builder.create()
        window = dialog.window!!
        window.setBackgroundDrawableResource(android.R.color.transparent)

        //Initialize Activity
        mActivity = activity as AuthActivity

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        viewModel.authIdMLD.observe(this.viewLifecycleOwner, Observer {
            id = it
        })

        viewModel.resendTokenMLD.observe(this.viewLifecycleOwner, Observer {
            resendToken = it
        })

        viewModel.logInSuccessMLD.observe(this.viewLifecycleOwner, Observer {
            if (it) {
                dialog.dismiss()
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
                //Go to MainActivity
                val intent = Intent(context, MainActivity::class.java)
                startActivity(intent)
                (mActivity).finish()
            } else {
                dialog.dismiss()
                //Disable Verify Btn to avoid multiple callbacks
                verifyBtn.isClickable = false

                //Clear All EditTexts
                otpTV2.text.clear()
                otpTV3.text.clear()
                otpTV4.text.clear()
                otpTV5.text.clear()
                otpTV6.text.clear()
                otpTV1.text.clear()

                timer.visibility = View.VISIBLE
                startTimer()
            }
        })

        timer.setOnClickListener(View.OnClickListener {
            viewModel.resendVerificationCode(number, resendToken)
            verifyBtn.isClickable = false
            timer.isClickable = false
        })

        //Credential Observer
        viewModel.credentialMLD.observe(this.viewLifecycleOwner, Observer
        {
            credential = it
        })

        verifyBtn.setOnClickListener(View.OnClickListener
        {
            verifyCode =
                otpTV1.text.toString() + otpTV2.text.toString() + otpTV3.text.toString() + otpTV4.text.toString() + otpTV5.text.toString() + otpTV6.text.toString()
            if (verifyCode.length == 6) {
                viewModel.verifyPhoneNumberWithCode(id, verifyCode)
                viewModel.signInWithPhoneAuthCredential(credential)
                dialog.show()
            }
        })

        //TextViews On Text Change Listener
        otpTV1.addTextChangedListener(TextWatcherVerify(null, otpTV2, mActivity))
        otpTV2.addTextChangedListener(TextWatcherVerify(otpTV1, otpTV3, mActivity))
        otpTV3.addTextChangedListener(TextWatcherVerify(otpTV2, otpTV4, mActivity))
        otpTV4.addTextChangedListener(TextWatcherVerify(otpTV3, otpTV5, mActivity))
        otpTV5.addTextChangedListener(TextWatcherVerify(otpTV4, otpTV6, mActivity))
        otpTV6.addTextChangedListener(TextWatcherVerify(otpTV5, null, mActivity))

        return view
    }

    private fun startTimer() {
        //Create a 1 min Counter before sending new Code
        object : CountDownTimer(60000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(p0: Long) {
                timer.isClickable = false
                timer.text = "Resending new code in ${p0 / 1000}"
            }

            override fun onFinish() {
                verifyBtn.isClickable = true
                timer.isClickable = true
                timer.text = getString(R.string.resend_code)
            }
        }.start()
    }
}