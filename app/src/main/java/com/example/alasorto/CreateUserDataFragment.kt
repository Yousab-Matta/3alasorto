package com.example.alasorto

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.alasorto.dataClass.UserData
import com.example.alasorto.utils.TextWatcherCreateUser
import com.example.alasorto.viewModels.AppViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import kotlin.collections.ArrayList

@Suppress("DEPRECATION")
class CreateUserDataFragment : Fragment(R.layout.fragment_create_user_data), AdapterView.OnItemSelectedListener {

    //Widgets
    private lateinit var nameET: EditText
    private lateinit var phoneET: EditText
    private lateinit var addressET: EditText
    private lateinit var locationET: EditText
    private lateinit var cpET: EditText
    private lateinit var pointsET: EditText
    private lateinit var birthDayET: EditText
    private lateinit var birthMonthET: EditText
    private lateinit var birthYearET: EditText
    private lateinit var collegeET: EditText
    private lateinit var uniET: EditText
    private lateinit var statusYearET: EditText
    private lateinit var titleTV: TextView
    private lateinit var saveUserBtn: ImageButton
    private lateinit var statusSpinner: Spinner
    private lateinit var viewModel: AppViewModel
    private lateinit var dialog: Dialog
    private lateinit var builder: android.app.AlertDialog.Builder
    private lateinit var window: Window

    private var user: UserData? = null
    private var isNewUser: Boolean = true
    private var status: String = "Student"
    private var phoneNum: String = ""
    private var isCurrentUser = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        nameET = view.findViewById(R.id.et_user_name)
        phoneET = view.findViewById(R.id.et_phone_num)
        addressET = view.findViewById(R.id.et_address)
        locationET = view.findViewById(R.id.et_location)
        cpET = view.findViewById(R.id.et_confession_priest)
        pointsET = view.findViewById(R.id.et_points)
        birthDayET = view.findViewById(R.id.et_birth_day)
        birthMonthET = view.findViewById(R.id.et_birth_month)
        birthYearET = view.findViewById(R.id.et_birth_year)
        collegeET = view.findViewById(R.id.et_college)
        uniET = view.findViewById(R.id.et_uni)
        statusYearET = view.findViewById(R.id.et_status_year)
        titleTV = view.findViewById(R.id.tv_create_user_title)
        saveUserBtn = view.findViewById(R.id.btn_save_user)
        statusSpinner = view.findViewById(R.id.spinner_status)

        val args = this.arguments
        if (args != null) {
            isNewUser = args.getBoolean("isNewUser")
            if (!isNewUser) {
                user = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    args.getParcelable("Editing_User", UserData::class.java)
                } else {
                    args.getParcelable("Editing_User")
                }
            }
            phoneNum = args.getString("PHONE_NUM", "")

            pointsET.isEnabled = args.getBoolean("IS_POINTS_ET_ENABLED", true)
        }

        isCurrentUser = phoneNum == FirebaseAuth.getInstance().currentUser!!.phoneNumber

        if (isCurrentUser || !isNewUser) {
            if (phoneNum.isNotEmpty()) {
                phoneET.setText(phoneNum.drop(2))
                phoneET.isEnabled = false
            }
        }

        //Spinner Adapter
        ArrayAdapter.createFromResource(
            requireActivity(),
            R.array.status_spinner_array,
            R.layout.spinner_layout
        )
            .also { arrayAdapter ->
                arrayAdapter.setDropDownViewResource(R.layout.spinner_layout)
                statusSpinner.adapter = arrayAdapter
            }

        statusSpinner.onItemSelectedListener = this
        //Edit Text Text watchers
        nameET.addTextChangedListener(TextWatcherCreateUser(nameET, requireContext()))
        phoneET.addTextChangedListener(TextWatcherCreateUser(phoneET, requireContext()))
        addressET.addTextChangedListener(TextWatcherCreateUser(addressET, requireContext()))
        pointsET.addTextChangedListener(TextWatcherCreateUser(pointsET, requireContext()))
        birthDayET.addTextChangedListener(TextWatcherCreateUser(birthDayET, requireContext()))
        birthMonthET.addTextChangedListener(TextWatcherCreateUser(birthMonthET, requireContext()))
        birthYearET.addTextChangedListener(TextWatcherCreateUser(birthYearET, requireContext()))
        statusYearET.addTextChangedListener(TextWatcherCreateUser(statusYearET, requireContext()))

        //Create loading Dialogue
        dialog = Dialog(requireContext())
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.progress_dialogue)
        dialog.window!!.setBackgroundDrawable(
            InsetDrawable(ColorDrawable(Color.TRANSPARENT), 40)
        )

        //Initialize ViewModel
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        if (user != null) {
            nameET.setText(user!!.name)
            phoneET.setText(user!!.phone.drop(2))
            addressET.setText(user!!.address)
            locationET.setText(user!!.location)
            cpET.setText(user!!.confessionPriest)
            pointsET.setText(user!!.points.toString())
            collegeET.setText(user!!.college)
            uniET.setText(user!!.university)
            birthDayET.setText(user!!.birthDay.toString())
            birthMonthET.setText(user!!.birthMonth.toString())
            birthYearET.setText(user!!.birthYear.toString())
            statusYearET.setText(user!!.statusYear.toString())
            titleTV.setText(R.string.edit_user)
            if (user!!.collegeStatus == "Student") {
                statusSpinner.setSelection(0)
            } else {
                statusSpinner.setSelection(1)
            }
        }

        viewModel.dismissFragmentMLD.observe(this.viewLifecycleOwner, Observer {
            if (it && isNewUser && isCurrentUser) {
                goToPendingVerificationFragment()
            }
            resetFragment(it)
        })

        saveUserBtn.setOnClickListener(View.OnClickListener
        {
            createData()
        })
    }

    private fun goToPendingVerificationFragment() {
        val fragment = PendingVerificationFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.replace(R.id.auth_frame, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().supportFragmentManager.popBackStack(
                        "HANDLE_USERS_FRAGMENT",
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                    this.isEnabled = false
                }
            })
    }

    private fun createData() {

        val name = nameET.text.toString().trim()
        if (phoneNum.isEmpty()) {
            phoneNum = "+2${phoneET.text.toString().trim()}"
        }
        val address = addressET.text.toString().trim()
        val location = locationET.text.toString().trim()
        val cp = cpET.text.toString().trim()
        val points = if (pointsET.text.toString().trim().isNotEmpty()) {
            pointsET.text.toString().trim().toInt()
        } else {
            0
        }

        val day = birthDayET.text.toString().trim()
        val month = birthMonthET.text.toString().trim()
        val year = birthYearET.text.toString().trim()
        val college = collegeET.text.toString().trim()
        val uni = uniET.text.toString().trim()
        val statusYear = statusYearET.text.toString().trim()

        //List that contains the ETs that MUST be filled
        val viewsList = arrayListOf(
            nameET,
            phoneET,
            addressET,
            birthDayET,
            birthMonthET,
            birthYearET,
            statusYearET
        )
        for (et in viewsList) {
            if (et.text.isEmpty()) {
                et.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_error,
                    0
                )
                et.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.error_custom_input)
            }
        }
        if (name.isNotEmpty() && phoneNum.isNotEmpty() && address.isNotEmpty()
            && day.isNotEmpty() && month.isNotEmpty()
            && year.isNotEmpty() && statusYear.isNotEmpty()
        ) {
            dialog.show()
            val user = UserData(
                name, 0, 0,
                location, address, "", cp, phoneNum,
                day.toInt(), month.toInt(), year.toInt(),
                points, college,
                uni, status, "", statusYear.toInt(),
                "", "", "",
                Date(), false, ArrayList()
            )
            if (isNewUser) {
                viewModel.createUser(user)
            } else {
                viewModel.editUser(user)
            }
        } else {
            Toast.makeText(context, "Please fill required data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetFragment(isSuccess: Boolean) {
        dialog.dismiss()
        if (isSuccess) {
            Toast.makeText(context, R.string.user_data_created, Toast.LENGTH_SHORT).show()
            nameET.text.clear()
            phoneET.text.clear()
            phoneET.isEnabled = true
            addressET.text.clear()
            locationET.text.clear()
            cpET.text.clear()
            pointsET.text.clear()
            birthDayET.text.clear()
            birthMonthET.text.clear()
            birthYearET.text.clear()
            collegeET.text.clear()
            uniET.text.clear()
            statusYearET.text.clear()
        } else {
            Toast.makeText(context, getString(R.string.user_data_not_created), Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        status = if (p2 == 0) {
            "Student"
        } else {
            "Graduated"
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
    }
}