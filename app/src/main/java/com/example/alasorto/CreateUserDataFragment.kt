package com.example.alasorto

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.alasorto.dataClass.Users
import com.example.alasorto.utils.TextWatcherCreateUser
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity.NONE
import com.yalantis.ucrop.UCropActivity.SCALE
import java.io.File

@Suppress("DEPRECATION")
class CreateUserDataFragment : Fragment(), AdapterView.OnItemSelectedListener {

    companion object {
        private const val RESULT_OK = -1
        private const val RESULT_CANCEL = 0
    }

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
    private lateinit var userIV: ImageView
    private lateinit var saveUserBtn: ImageButton
    private lateinit var statusSpinner: Spinner
    private lateinit var viewModel: AppViewModel
    private lateinit var builder: AlertDialog.Builder
    private lateinit var dialog: AlertDialog
    private lateinit var window: Window
    private var user: Users? = null
    private var finalUri: Uri? = null
    private var isNewUser: Boolean = true
    private var status: String = "Student"
    private var editImageLink: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_user_data, container, false)
        view.isClickable = true

        val args = this.arguments
        if (args != null) {
            isNewUser = args.getBoolean("isNewUser")
            if (!isNewUser) {
                user = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    args.getParcelable("Editing_User", Users::class.java)
                } else {
                    args.getParcelable("Editing_User")
                }
                if (user != null) {
                    editImageLink = user!!.ImageLink.toString()
                }
            }
        }

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
        userIV = view.findViewById(R.id.iv_create_user)

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

        //Create Alert Dialogue
        builder = AlertDialog.Builder(context)
        builder.setCancelable(false)
        builder.setView(R.layout.progress_dialogue)
        dialog = builder.create()
        window = dialog.window!!
        window.setBackgroundDrawableResource(android.R.color.transparent)

        //Initialize ViewModel
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        if (user != null) {
            nameET.setText(user!!.Name)
            phoneET.setText(user!!.Phone?.drop(2))
            addressET.setText(user!!.Address)
            locationET.setText(user!!.Location)
            cpET.setText(user!!.ConfessionPriest)
            pointsET.setText(user!!.Points.toString())
            collegeET.setText(user!!.College)
            uniET.setText(user!!.University)
            birthDayET.setText(user!!.BirthDay.toString())
            birthMonthET.setText(user!!.BirthMonth.toString())
            birthYearET.setText(user!!.BirthYear.toString())
            statusYearET.setText(user!!.StatusYear.toString())
            titleTV.setText(R.string.edit_user)
            if (user!!.ImageLink!!.isNotEmpty()) {
                Glide.with(userIV).load(user!!.ImageLink).into(userIV)
            }
            if (user!!.Status.equals("Student")) {
                statusSpinner.setSelection(0)
            } else {
                statusSpinner.setSelection(1)
            }
        }

        viewModel.clearFragmentMLD.observe(this.viewLifecycleOwner, Observer {
            resetFragment(it)
        })

        saveUserBtn.setOnClickListener(View.OnClickListener
        {
            createData()
        })

        userIV.setOnClickListener(View.OnClickListener
        {
            openGalleryForImage()
        })

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

        return view
    }

    private fun createData() {
        val name = nameET.text.toString().trim()
        val phone = phoneET.text.toString().trim()
        val address = addressET.text.toString().trim()
        val location = locationET.text.toString().trim()
        val cp = cpET.text.toString().trim()
        val points = pointsET.text.toString().trim()
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
            pointsET,
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
        if (name.isNotEmpty() && phone.isNotEmpty() && address.isNotEmpty()
            && points.isNotEmpty() && day.isNotEmpty() && month.isNotEmpty()
            && year.isNotEmpty() && statusYear.isNotEmpty()
        ) {
            dialog.show()
            val user = Users(
                name, 0, 0f,
                "User", location, address, cp, "+2${phone}",
                day.toInt(), month.toInt(), year.toInt(), points.toInt(), college,
                uni, status, "", statusYear.toInt(), editImageLink
            )
            if (isNewUser) {
                viewModel.createUser(user, finalUri, requireActivity().contentResolver)
            } else {
                viewModel.editUser(user, finalUri, requireActivity().contentResolver)
            }
        } else {
            Toast.makeText(context, "Please fill required data", Toast.LENGTH_SHORT).show()
        }
    }

    //Crop Image
//Select Image Launchers
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent = result.data!!
                val sourceUri: Uri = data.data!!
                val destinationUri = Uri.fromFile(
                    File(
                        requireActivity().cacheDir,
                        queryName(requireActivity().contentResolver, sourceUri)
                    )
                )

                //UCrop options
                val options = UCrop.Options()
                options.setToolbarColor(ContextCompat.getColor(requireActivity(), R.color.black))
                options.setStatusBarColor(ContextCompat.getColor(requireActivity(), R.color.black))
                options.setToolbarWidgetColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.white
                    )
                )
                options.setCircleDimmedLayer(true)
                options.setAllowedGestures(SCALE, NONE, NONE)

                val intent = UCrop.of(sourceUri, destinationUri)
                    .withOptions(options).withAspectRatio(1f, 1f)
                    .getIntent(requireActivity())
                cropResult.launch(intent)
            }
        }

    //Crop Result Launcher
    private val cropResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            assert(result.data != null)
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                finalUri = resultUri
                userIV.setImageURI(resultUri)
                editImageLink = ""
            }
        } else if (result.resultCode == RESULT_CANCEL) {
            Toast.makeText(context, "Image was not Uploaded", Toast.LENGTH_SHORT).show()
        }
    }

    //UCrop Sh!t
    private fun queryName(resolver: ContentResolver, uri: Uri): String {
        val returnCursor = resolver.query(uri, null, null, null, null)
        val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        returnCursor.close()
        return name
    }

    //Select Image
    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun resetFragment(isSuccess: Boolean) {
        dialog.dismiss()
        if (isSuccess) {
            Toast.makeText(context, "Data uploaded", Toast.LENGTH_SHORT).show()
            nameET.text.clear()
            phoneET.text.clear()
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
            userIV.setImageURI(null)
            requireActivity().supportFragmentManager.popBackStack(
                "CREATE_USER_DATA_FRAGMENT", FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        } else {
            Toast.makeText(context, "Data were not uploaded", Toast.LENGTH_SHORT).show()
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