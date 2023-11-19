package com.example.alasorto

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.alasorto.dataClass.*
import com.example.alasorto.utils.InternetCheck
import com.example.alasorto.viewModels.AppViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ramijemli.percentagechartview.PercentageChartView
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import java.io.File
import java.util.*

@Suppress("DEPRECATION")
class UserProfileFragment : Fragment(R.layout.fragment_user_profile) {

    companion object {
        private const val RESULT_OK = -1
        private const val RESULT_CANCEL = 0
    }

    private val viewModel: AppViewModel by viewModels()
    private val currentUserId = FirebaseAuth.getInstance().currentUser!!.phoneNumber!!

    private var currentProfileUser: UserData? = null
    private var userImageUri: Uri? = null
    private var coverImageUri: Uri? = null
    private var hasConnection = false
    private var isBioChanged = false
    private var isImageChanged = false

    //Widgets
    private lateinit var userIV: ImageView
    private lateinit var coverIV: ImageView
    private lateinit var bioET: EditText
    private lateinit var bioTV: TextView
    private lateinit var nameTV: TextView
    private lateinit var pointsTV: TextView
    private lateinit var birthdayTV: TextView
    private lateinit var internetCheck: InternetCheck
    private lateinit var progressView: PercentageChartView
    private lateinit var saveProfileChangesBtn: ImageButton
    private lateinit var editProfileDataBtn: ImageButton
    private lateinit var chatWithFatherBtn: Button
    private lateinit var spiritualNotesBtn: Button
    private lateinit var mActivity: MainActivity

    //Admin buttons
    private lateinit var handleUsersButton: Button
    private lateinit var attendanceBtn: Button
    private lateinit var sendCustomNotificationBtn: Button
    private lateinit var verifyUsersBtn: Button

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mActivity = (activity as MainActivity)

        view.isClickable = true

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
        }

        bioET = view.findViewById(R.id.et_profile_bio)
        bioTV = view.findViewById(R.id.tv_profile_bio)
        nameTV = view.findViewById(R.id.tv_profile_name)
        userIV = view.findViewById(R.id.iv_profile_image)
        coverIV = view.findViewById(R.id.iv_profile_cover)
        pointsTV = view.findViewById(R.id.tv_profile_points)
        birthdayTV = view.findViewById(R.id.tv_profile_birthday)
        progressView = view.findViewById(R.id.profile_progress_view)
        saveProfileChangesBtn = view.findViewById(R.id.btn_save_profile)
        editProfileDataBtn = view.findViewById(R.id.btn_edit_profile)
        chatWithFatherBtn = view.findViewById(R.id.btn_chat_with_father)
        spiritualNotesBtn = view.findViewById(R.id.btn_spiritual_notes)

        handleUsersButton = view.findViewById(R.id.btn_users)
        attendanceBtn = view.findViewById(R.id.btn_go_to_attendance)
        sendCustomNotificationBtn = view.findViewById(R.id.btn_send_custom_notification)
        verifyUsersBtn = view.findViewById(R.id.btn_verify_users)

        val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }

        if (currentLocale == Locale.ENGLISH) {
            birthdayTV.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_cake, 0, 0, 0
            )
            pointsTV.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_star, 0, 0, 0
            )
        } else {
            birthdayTV.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, R.drawable.ic_cake, 0
            )

            pointsTV.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, R.drawable.ic_star, 0
            )
        }

        //Initialize user
        currentProfileUser = mActivity.getCurrentUser()

        //Set user data to views
        setUserData()

        //Observe changes in current user's data
        viewModel.currentUserMLD.observe(this.viewLifecycleOwner, Observer {
            currentProfileUser = it
            setUserData()
            mActivity.dismissLoadingDialog()
        })

        viewModel.safeSpaceItemMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                if (it.ownerId == currentUserId) {
                    mActivity.dismissLoadingDialog()
                    goToChatFragment(it)
                }
            } else {
                viewModel.getAllSafeSpace()
            }
        })

        viewModel.safeSpaceList.observe(this.viewLifecycleOwner, Observer {
            val safeSpace =
                SafeSpace(currentUserId, "Case ${it.size + 1}", "", null, true)
            viewModel.createSafeSpace(safeSpace)
        })

        viewModel.dismissFragmentMLD.observe(this.viewLifecycleOwner, Observer {
            mActivity.dismissLoadingDialog()
        })

        //Observe when safe space document is created for the first time then goes to chat fragment
        viewModel.safeSpaceCreatedMLD.observe(this.viewLifecycleOwner, Observer {
            mActivity.dismissLoadingDialog()
            goToChatFragment(it)
        })

        //Set click listener for user profile image
        userIV.setOnClickListener {
            ProfileImageControlsDialog("IMAGE", ::showImage, ::openGalleryForImage).show(
                requireActivity().supportFragmentManager,
                "PROFILE_CONTROLS_FRAGMENT"
            )
        }

        //Set click listener for user profile cover
        coverIV.setOnClickListener {
            ProfileImageControlsDialog("COVER", ::showImage, ::openGalleryForImage).show(
                requireActivity().supportFragmentManager,
                "PROFILE_CONTROLS_FRAGMENT"
            )
        }

        //Set Listener for the button that saves data when anything is changed
        saveProfileChangesBtn.setOnClickListener {
            saveProfileEdits()
        }

        editProfileDataBtn.setOnClickListener {
            if (currentProfileUser != null) {
                goToEditUserFragment()
            }
        }

        chatWithFatherBtn.setOnClickListener {
            mActivity.showLoadingDialog()
            viewModel.getSafeSpaceById(currentUserId)
        }

        spiritualNotesBtn.setOnClickListener {
            goToSpiritualNotesFragment()
        }

        handleUsersButton.setOnClickListener {
            goToAllUsersFragment()
        }

        attendanceBtn.setOnClickListener {
            goToAttendanceFragment()
        }

        sendCustomNotificationBtn.setOnClickListener {
            goToNotificationsFragment()
        }

        verifyUsersBtn.setOnClickListener {
            goToVerifyUsersFragment()
        }

        bioTV.setOnLongClickListener {
            if (currentProfileUser != null) {
                //Hide bio TextView and show bio EditText
                bioTV.visibility = INVISIBLE
                bioET.visibility = VISIBLE
                //Disable bioTV so it doesn't do on long click
                bioTV.isEnabled = false
                bioET.setText(currentProfileUser!!.bio)
            }
            false
        }

        bioET.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    isBioChanged = true
                    saveProfileChangesBtn.visibility = VISIBLE
                }

                override fun afterTextChanged(s: Editable?) {
                }

            })

        //Observes Changes un profile
        Firebase.firestore.collection("Users")
            .document(currentUserId)
            .addSnapshotListener { _, _ ->
                if (hasConnection) {
                    viewModel.getCurrentUser()
                }
            }
    }

    private fun saveProfileEdits() {
        if (hasConnection) {

            mActivity.showLoadingDialog()

            //Upload user and cover images
            viewModel.editUserImage(
                userImageUri,
                coverImageUri,
                requireActivity().contentResolver,
                currentUserId
            )

            //Update Bio
            if (isBioChanged) {
                viewModel.updateUserBio(bioET.text.toString(), currentUserId)

                saveProfileChangesBtn.visibility = GONE
                userImageUri = null
                coverImageUri = null
            }

        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.check_connection),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showImage(case: String) {
        val args = Bundle()

        if (currentProfileUser != null) {
            if (case == "IMAGE") {
                args.putString("IMAGE_LINK", currentProfileUser!!.imageLink)
            } else {
                args.putString("IMAGE_LINK", currentProfileUser!!.coverImageLink)
            }
            mActivity.goToEnlargeMediaFragment(args)
        }
    }

    //Select Image
    private fun openGalleryForImage(case: String) {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        if (case == "IMAGE") {
            profileImageLauncher.launch(intent)
        } else if (case == "COVER") {
            coverImageLauncher.launch(intent)
        }
    }

    //Crop Image
//Select Image Launcher for group image
    private val profileImageLauncher =
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

                val intent = UCrop.of(sourceUri, destinationUri)
                    .withOptions(options)
                    .getIntent(requireActivity())
                userImageCropResult.launch(intent)
            }
        }

    //Crop image Result
    private val userImageCropResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            assert(result.data != null)
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                userIV.setImageURI(resultUri)
                userImageUri = resultUri
                saveProfileChangesBtn.visibility = VISIBLE
                isImageChanged = true
            }
        } else if (result.resultCode == RESULT_CANCEL) {
            Toast.makeText(context, "Image was not Uploaded", Toast.LENGTH_SHORT).show()
        }
    }

    //Crop Image
//Select Image Launcher for group image
    private val coverImageLauncher =
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

                options.withAspectRatio(2f, 1f)
                options.setAllowedGestures(
                    UCropActivity.SCALE,
                    UCropActivity.ALL,
                    UCropActivity.SCALE
                )

                val intent = UCrop.of(sourceUri, destinationUri)
                    .withOptions(options)
                    .getIntent(requireActivity())
                coverImageCropResult.launch(intent)
            }
        }

    //Group header crop Result
    private val coverImageCropResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            assert(result.data != null)
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                coverIV.setImageURI(resultUri)
                coverImageUri = resultUri
                saveProfileChangesBtn.visibility = VISIBLE
                isImageChanged = true
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

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isBioChanged || isImageChanged) {
                        discardChanges()
                    } else {
                        requireActivity().supportFragmentManager.popBackStack(
                            "USER_PROFILE_FRAGMENT",
                            FragmentManager.POP_BACK_STACK_INCLUSIVE
                        )
                        this.isEnabled = false
                    }
                }
            })
    }

    private fun discardChanges() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity as MainActivity)
        builder.setCancelable(true)
        builder.setTitle(R.string.discard_changes)
        builder.setMessage(R.string.discard_changes_message)
        builder.setPositiveButton(R.string.yes) { _, _ ->
            setUserData()
        }
        builder.setNegativeButton(R.string.no) { _, _ -> }
        builder.show()
    }

    private fun setUserData() {
        if (currentProfileUser != null) {
            bioET.visibility = GONE
            bioTV.visibility = VISIBLE
            bioTV.isEnabled = true

            saveProfileChangesBtn.visibility = GONE

            isBioChanged = false
            isImageChanged = false

            "${currentProfileUser!!.points} ${getString(R.string.points)}".also {
                pointsTV.text = it
            }

            nameTV.text = currentProfileUser!!.name

            bioTV.text = currentProfileUser!!.bio

            "${currentProfileUser!!.birthDay}/${currentProfileUser!!.birthMonth}/${currentProfileUser!!.birthYear}"
                .also { birthdayTV.text = it }

            if (currentProfileUser!!.imageLink.isNotEmpty()) {
                Glide.with(userIV).load(currentProfileUser!!.imageLink).into(userIV)
            }

            if (currentProfileUser!!.coverImageLink.isNotEmpty()) {
                Glide.with(coverIV).load(currentProfileUser!!.coverImageLink).into(coverIV)
            }

            val attendedTimes = currentProfileUser!!.attendedTimes
            val attendanceDue = currentProfileUser!!.attendanceDue

            val attendancePercent = if (attendedTimes != 0 && attendanceDue != 0) {
                ((attendedTimes.toFloat() / attendanceDue.toFloat()) * 100)
            } else {
                0f
            }

            if (attendancePercent in 0.0..100.0) {
                progressView.setProgress(attendancePercent, true)
            }

            showAdminButtons()
        }
    }

    private fun goToChatFragment(safeSpace: SafeSpace) {
        val fragment = ChatFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val bundle = Bundle()
        bundle.putString("CHAT_ID", currentUserId)
        bundle.putString("COLLECTION_PATH", "ChatWithFather")
        bundle.putBoolean("IS_ANONYMOUS", true)
        bundle.putParcelable("SAFE_SPACE_ITEM", safeSpace)
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("CHAT_FRAGMENT")
        transaction.commit()
    }

    private fun goToEditUserFragment() {
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val fragment = CreateUserDataFragment()
        val bundle = Bundle()
        bundle.putParcelable("Editing_User", currentProfileUser)
        bundle.putBoolean("isNewUser", false)
        bundle.putBoolean("IS_POINTS_ET_ENABLED", false)
        bundle.putString("PHONE_NUM", currentUserId)
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("HANDLE_USERS_FRAGMENT")
        transaction.commit()
    }

    class ProfileImageControlsDialog(
        private val imageType: String,
        private val showImage: (String) -> Unit,
        private val pickImage: (String) -> Unit
    ) :
        BottomSheetDialogFragment() {

        override fun getTheme(): Int {
            return R.style.AppBottomSheetDialogTheme
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.bottom_sheet_profile, container, false)
            view.isClickable = true
            return view
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val showImageTV: TextView = view.findViewById(R.id.tv_show_image)
            val changeImageTV: TextView = view.findViewById(R.id.tv_change_image)

            showImageTV.setOnClickListener(OnClickListener {
                showImage(imageType)
                requireDialog().dismiss()
            })

            changeImageTV.setOnClickListener(OnClickListener {
                pickImage(imageType)
                requireDialog().dismiss()
            })
        }
    }

    private fun showAdminButtons() {
        if (currentProfileUser != null) {
            if (currentProfileUser!!.access.contains("HANDLE_USERS")) {
                handleUsersButton.visibility = VISIBLE
            }
            if (currentProfileUser!!.access.contains("HANDLE_ATTENDANCE")) {
                attendanceBtn.visibility = VISIBLE
            }
            if (currentProfileUser!!.access.contains("SEND_CUSTOM_NOTIFICATION")) {
                sendCustomNotificationBtn.visibility = VISIBLE
            }
            if (currentProfileUser!!.access.contains("HANDLE_USERS_VERIFICATION")) {
                verifyUsersBtn.visibility = VISIBLE
            }
        }
    }

    private fun goToAllUsersFragment() {
        val fragment = AllUsersFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("ALL_USERS_FRAGMENT")
        transaction.commit()
    }

    private fun goToAttendanceFragment() {
        val fragment = AttendanceHistoryFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("ATTENDANCE_FRAGMENT")
        transaction.commit()
    }

    private fun goToNotificationsFragment() {
        val fragment = SendNotificationFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("CUSTOM_NOTIF_FRAGMENT")
        transaction.commit()
    }

    private fun goToVerifyUsersFragment() {
        val fragment = VerifyUsersFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("VERIFY_USERS_FRAGMENT")
        transaction.commit()
    }

    private fun goToSpiritualNotesFragment() {
        val fragment = SpiritualNoteFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("SPIRITUAL_NOTES_FRAGMENT")
        transaction.commit()
    }

    /*
    private fun goToGroupsFragment() {
        val fragment = GroupsFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val bundle = Bundle()
        bundle.putBoolean("SelectionEnabled", false)
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("GROUPS_FRAGMENT")
        transaction.commit()
    }
    private fun goToSettingsFragment() {
        val fragment = SettingsFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("SETTINGS_FRAGMENT")
        transaction.commit()
    }

*/
}