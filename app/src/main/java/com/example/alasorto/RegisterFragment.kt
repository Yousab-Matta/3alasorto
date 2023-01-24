package com.example.alasorto

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.PhoneAuthProvider

class RegisterFragment : Fragment() {
    private lateinit var mActivity: AuthActivity
    private lateinit var phoneNumTV: EditText
    private lateinit var sendCodeBtn: Button
    private lateinit var number: String
    private lateinit var id: String
    private lateinit var builder: AlertDialog.Builder
    private lateinit var dialog: AlertDialog
    private lateinit var window: Window
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var viewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_register, container, false)
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

        //Observers
        viewModel.authIdMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                id = it
            }
        })

        viewModel.resendTokenMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                resendToken = it
                dialog.dismiss()
                goToFragment()
            }
        })

        sendCodeBtn.setOnClickListener(View.OnClickListener {
            number = phoneNumTV.text.toString()
            if (number.isNotEmpty()) {
                if (number[0] != '+' && number[1] != '2') {
                    number = "+2$number"
                    phoneNumTV.setText(number)
                }
                viewModel.sendVerificationCode(number)
                dialog.show()
            }
        })

        return view
    }

    private fun goToFragment() {
        val fragment = VerifyPhoneFragment()
        val manager = (activity as AuthActivity).supportFragmentManager
        val transaction = manager.beginTransaction()
        val bundle = Bundle()
        bundle.putString("ID", id)
        bundle.putString("Number", number)
        bundle.putParcelable("Token", resendToken)
        fragment.arguments = bundle
        transaction.replace(R.id.auth_frame, fragment)
        transaction.commit()
    }
}