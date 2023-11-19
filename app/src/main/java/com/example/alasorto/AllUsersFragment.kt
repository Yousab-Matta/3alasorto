package com.example.alasorto

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.*
import android.view.View.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alasorto.adapters.AllUsersAdapter
import com.example.alasorto.dataClass.UserData
import com.example.alasorto.utils.InternetCheck
import com.example.alasorto.utils.LinearSpacingItemDecorator
import com.example.alasorto.viewModels.AppViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.concurrent.thread

class AllUsersFragment : Fragment(R.layout.fragment_all_users), AllUsersAdapter.OnClickListener,
    AllUsersAdapter.OnLongClickListener {
    private val viewModel: AppViewModel by viewModels()

    private val allUsersList = ArrayList<UserData>()
    private val selectedUsersList = ArrayList<UserData>()
    private val usersFilteredList = ArrayList<UserData>()

    private var currentUser: UserData? = null
    private var hasConnection = false
    private var month: Int = 0
    private var vMCounter: Int = 0
    private var selectingUsersEnabled = false

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AllUsersAdapter
    private lateinit var addUserBtn: ImageButton
    private lateinit var userSearchET: EditText
    private lateinit var selectedUserCounterTV: TextView
    private lateinit var sortBtn: ImageView
    private lateinit var selectAllUserscheckBox: CheckBox
    private lateinit var internetCheck: InternetCheck
    private lateinit var pointsDialog: Dialog
    private lateinit var monthsDialog: Dialog
    private lateinit var sortDialog: Dialog

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
            if (it) {
                viewModel.getAllUsers()
            }
        }

        //Initialize Widgets
        recyclerView = view.findViewById(R.id.rv_all_users)
        sortBtn = view.findViewById(R.id.iv_filter_users)
        addUserBtn = view.findViewById(R.id.btn_add_user)
        userSearchET = view.findViewById(R.id.et_user_search)
        selectAllUserscheckBox = view.findViewById(R.id.cb_select_all_users)
        selectedUserCounterTV = view.findViewById(R.id.tv_selected_user_counter)

        allUsersList.addAll((activity as MainActivity).getAllUsers().sortedBy { it.name })
        usersFilteredList.addAll(allUsersList)

        currentUser = (activity as MainActivity).getCurrentUser()
        setRecyclerView()

        viewModel.currentUserMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                currentUser = it
                setRecyclerView()
            }
        })

        viewModel.counterMLD.observe(this.viewLifecycleOwner, Observer {
            if (it) {
                vMCounter++
                if (vMCounter == selectedUsersList.size) {
                    vMCounter = 0
                    (activity as MainActivity).dismissLoadingDialog()
                }
            }
        })

        //Observing all users
        viewModel.usersMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                allUsersList.clear()
                allUsersList.addAll(it.filter { it1 -> it1.verified })
                usersFilteredList.clear()
                usersFilteredList.addAll(allUsersList.sortedBy { it1 -> it1.name })
                adapter.notifyDataSetChanged()
            }
        })

        addUserBtn.setOnClickListener(OnClickListener {
            goToEditUserFragment(true)
        })

        sortBtn.setOnClickListener(OnClickListener {
            showSortDialog()
        })

        selectAllUserscheckBox.setOnClickListener {
            if (selectAllUserscheckBox.isChecked) {
                selectingUsersEnabled = true
                selectedUsersList.clear()
                selectedUsersList.addAll(allUsersList)
            } else {
                selectedUsersList.clear()
            }
            "${selectedUsersList.size} Selected".also { selectedUserCounterTV.text = it }
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
        }

        userSearchET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                usersFilteredList.clear()
                val searchText = p0.toString().lowercase()
                for (user in allUsersList) {
                    if (user.name.lowercase().contains(searchText)) {
                        usersFilteredList.add(user)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })
    }

    private fun setRecyclerView() {
        if (currentUser != null) {
            adapter =
                AllUsersAdapter(
                    usersFilteredList,
                    selectedUsersList,
                    this,
                    this,
                    currentUser!!.access,
                    requireContext()
                )

            //Setting LL manager to RV
            recyclerView.layoutManager = LinearLayoutManager(context)

            recyclerView.adapter = adapter

            recyclerView.addItemDecoration(LinearSpacingItemDecorator(30))
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner, object : OnBackPressedCallback(true) {
                @SuppressLint("NotifyDataSetChanged")
                override fun handleOnBackPressed() {
                    if (selectingUsersEnabled) {
                        selectedUsersList.clear()
                        adapter.notifyDataSetChanged()
                        selectingUsersEnabled = false
                        "0 Selected".also { selectedUserCounterTV.text = it }
                        selectedUserCounterTV.visibility = GONE
                    } else {
                        requireActivity().supportFragmentManager.popBackStack(
                            "ALL_USERS_FRAGMENT",
                            FragmentManager.POP_BACK_STACK_INCLUSIVE
                        )
                        this.isEnabled = false
                    }
                }
            })

        //Notify changes in Comments database
        Firebase.firestore.collection("Users").addSnapshotListener { _, _ ->
            if (hasConnection) {
                viewModel.getAllUsers()
            } else {
                //Toast always shown check later
                //Toast.makeText(context, R.string.check_connection, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onClick(user: UserData) {
        if (selectingUsersEnabled) {
            selectUser(user)
        } else {
            viewUserProfile(user)
        }
    }

    override fun onLongClick(user: UserData) {
        selectingUsersEnabled = true
        selectedUserCounterTV.visibility = VISIBLE

        selectUser(user)
    }

    private fun showControlsMenu() {
        if (currentUser != null && currentUser!!.access.contains("HANDLE_USERS")) {

            val usersControlsDialog =
                UsersControlsDialog(
                    selectedUsersList,
                    ::goToGiveAdminRightsFragment,
                    currentUser!!.access.contains("HANDLE_USERS_PERMISSIONS"),
                    ::showEditPointsDialog,
                    ::goToEditUserFragment,
                    ::deleteUser
                )

            usersControlsDialog.show(
                requireActivity().supportFragmentManager,
                "USERS_CONTROLS_DIALOG"
            )
        }
    }

    private fun selectUser(user: UserData) {
        if (selectedUsersList.contains(user)) {
            selectedUsersList.remove(user)
        } else {
            selectedUsersList.add(user)
        }
        "${selectedUsersList.size} Selected".also { selectedUserCounterTV.text = it }
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showSortDialog() {
        sortDialog = Dialog(requireContext())
        sortDialog.setCancelable(true)
        sortDialog.setContentView(R.layout.layout_sort_user)

        val sortByNameTV: TextView = sortDialog.findViewById(R.id.tv_sort_by_name)
        val sortByMonthTV: TextView = sortDialog.findViewById(R.id.tv_sort_by_month)
        val sortByAttPercentTV: TextView =
            sortDialog.findViewById(R.id.tv_sort_by_attendance_percent)

        sortByNameTV.setOnClickListener(OnClickListener {
            usersFilteredList.clear()
            usersFilteredList.addAll(allUsersList.sortedBy { it.name })
            adapter.notifyDataSetChanged()
            sortDialog.dismiss()
        })

        sortByMonthTV.setOnClickListener(OnClickListener {
            showMonthsDialog()
            sortDialog.dismiss()
        })

        sortByAttPercentTV.setOnClickListener {
            usersFilteredList.clear()
            usersFilteredList.addAll(allUsersList.sortedByDescending { it1 -> it1.attendedTimes / it1.attendanceDue })
            adapter.notifyDataSetChanged()
            sortDialog.dismiss()
        }

        val window = sortDialog.window
        window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        window.setBackgroundDrawable(
            InsetDrawable(
                ColorDrawable(android.graphics.Color.TRANSPARENT),
                50
            )
        )
        sortDialog.show()
    }

    //Shows a dialog to select users where their birth month meets the month from the list
    private fun showMonthsDialog() {
        monthsDialog = Dialog(requireContext())
        monthsDialog.setCancelable(true)
        monthsDialog.setContentView(R.layout.layout_select_month)

        //Init onclick
        val onClick = OnClickListener { p0 ->
            when (p0!!.id) {
                R.id.tv_january -> month = 1
                R.id.tv_february -> month = 2
                R.id.tv_march -> month = 3
                R.id.tv_april -> month = 4
                R.id.tv_may -> month = 5
                R.id.tv_june -> month = 6
                R.id.tv_july -> month = 7
                R.id.tv_august -> month = 8
                R.id.tv_september -> month = 9
                R.id.tv_october -> month = 10
                R.id.tv_november -> month = 11
                R.id.tv_december -> month = 12
            }
            sortUsersByMonth(month)
        }

        //Init TVs
        val januaryTV: TextView = monthsDialog.findViewById(R.id.tv_january)
        val februaryTV: TextView = monthsDialog.findViewById(R.id.tv_february)
        val marchTV: TextView = monthsDialog.findViewById(R.id.tv_march)
        val aprilTV: TextView = monthsDialog.findViewById(R.id.tv_april)
        val mayTV: TextView = monthsDialog.findViewById(R.id.tv_may)
        val juneTV: TextView = monthsDialog.findViewById(R.id.tv_june)
        val julyTV: TextView = monthsDialog.findViewById(R.id.tv_july)
        val augustTV: TextView = monthsDialog.findViewById(R.id.tv_august)
        val septemberTV: TextView = monthsDialog.findViewById(R.id.tv_september)
        val octoberTV: TextView = monthsDialog.findViewById(R.id.tv_october)
        val novemberTV: TextView = monthsDialog.findViewById(R.id.tv_november)
        val decemberTV: TextView = monthsDialog.findViewById(R.id.tv_december)

        val window = monthsDialog.window
        window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        //Set onclick to TVs
        januaryTV.setOnClickListener(onClick)
        februaryTV.setOnClickListener(onClick)
        marchTV.setOnClickListener(onClick)
        aprilTV.setOnClickListener(onClick)
        mayTV.setOnClickListener(onClick)
        juneTV.setOnClickListener(onClick)
        julyTV.setOnClickListener(onClick)
        augustTV.setOnClickListener(onClick)
        septemberTV.setOnClickListener(onClick)
        octoberTV.setOnClickListener(onClick)
        novemberTV.setOnClickListener(onClick)
        decemberTV.setOnClickListener(onClick)


        window.setBackgroundDrawable(
            InsetDrawable(
                ColorDrawable(android.graphics.Color.TRANSPARENT),
                50
            )
        )
        monthsDialog.show()
    }

    private fun showEditPointsDialog(case: String) {
        //Create another dialogue to get number of points
        pointsDialog = Dialog(requireContext())
        pointsDialog.setCancelable(true)
        pointsDialog.setContentView(R.layout.layout_one_et)

        val pointsET: EditText = pointsDialog.findViewById(R.id.et_title)
        val confirmBtn: Button = pointsDialog.findViewById(R.id.btn_confirm_data)

        pointsET.setHint(R.string.points_number)
        pointsET.inputType = InputType.TYPE_CLASS_NUMBER
        confirmBtn.setText(R.string.confirm)

        val window = pointsDialog.window
        window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        window.setBackgroundDrawable(
            InsetDrawable(
                ColorDrawable(android.graphics.Color.TRANSPARENT),
                40
            )
        )

        confirmBtn.setOnClickListener(OnClickListener {
            if (hasConnection) {
                if (pointsET.text.toString().isNotEmpty()) {
                    if (selectedUsersList.isNotEmpty()) {
                        //If list is empty -> adds points for single user
                        //Else adds for all users in the list
                        thread {
                            for (user in selectedUsersList) {
                                if (case == "ADD") {
                                    viewModel.editUserPoints(
                                        (pointsET.text.toString().toLong()), user.phone
                                    )
                                } else {
                                    viewModel.editUserPoints(
                                        (-(pointsET.text.toString().toLong())), user.phone
                                    )
                                }
                            }
                        }
                        selectedUsersList.clear()
                        adapter.notifyItemRangeChanged(0, selectedUsersList.size)
                        pointsDialog.dismiss()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please enter required data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.check_connection,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        pointsDialog.show()
    }

    private fun deleteUser() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity as MainActivity)
        builder.setCancelable(true)
        builder.setTitle("Delete user?")
        builder.setMessage("Are you sure you want to delete this user?")
        builder.setPositiveButton("Yes") { _, _ ->
            if (hasConnection) {
                for (user in selectedUsersList) {
                    viewModel.deleteUser(user.phone)
                }
            }
        }
        builder.setNegativeButton("No") { _, _ -> }
        builder.show()
    }

    private fun goToEditUserFragment(isNewUser: Boolean) {
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val fragment = CreateUserDataFragment()
        if (!isNewUser) {
            val bundle = Bundle()
            bundle.putParcelable("Editing_User", selectedUsersList[0])
            bundle.putBoolean("isNewUser", false)
            bundle.putString("PHONE_NUM", "")
            fragment.arguments = bundle
        }
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("HANDLE_USERS_FRAGMENT")
        transaction.commit()
    }

    private fun viewUserProfile(userData: UserData) {
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val fragment = AdminProfileFragment()
        val bundle = Bundle()
        bundle.putParcelable("Profile_User", userData)
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("ADMIN_PROFILE_FRAGMENT")
        transaction.commit()
    }

    private fun goToGiveAdminRightsFragment() {
        val fragment = GiveAdminRightsFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val bundle = Bundle()
        bundle.putParcelable("EDITING_USER", selectedUsersList[0])
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("GIVE_ADMIN_RIGHTS_FRAGMENT")
        transaction.commit()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun sortUsersByMonth(month: Int) {
        usersFilteredList.clear()
        usersFilteredList.addAll(allUsersList.filter { it.birthMonth == month })
        adapter.notifyDataSetChanged()
        monthsDialog.dismiss()
    }

    class UsersControlsDialog(
        private val selectedUsersList: ArrayList<UserData>,
        private val goToGiveAdminRightsFragment: () -> Unit,
        private val currentUserHasPermission: Boolean,
        private val showPointsDialog: (String) -> Unit,
        private val goToEditUserFragment: (Boolean) -> Unit,
        private val deleteUser: () -> Unit,
    ) : BottomSheetDialogFragment(R.layout.bottom_sheet_users), OnClickListener {

        private lateinit var editUserTV: TextView
        private lateinit var addPointsTV: TextView
        private lateinit var removePointsTV: TextView
        private lateinit var deleteUserTV: TextView
        private lateinit var giveAdminRightsTV: TextView

        override fun getTheme(): Int {
            return R.style.AppBottomSheetDialogTheme
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            view.isClickable = true

            editUserTV = view.findViewById(R.id.tv_edit_user_data)
            addPointsTV = view.findViewById(R.id.tv_add_points)
            removePointsTV = view.findViewById(R.id.tv_remove_points)
            deleteUserTV = view.findViewById(R.id.tv_give_admin_rights)
            giveAdminRightsTV = view.findViewById(R.id.tv_delete_user)

            if (selectedUsersList.size == 1) {
                editUserTV.visibility = VISIBLE

                //Change visibility of give permission option for users who don't have this access
                if (currentUserHasPermission) {
                    giveAdminRightsTV.visibility = VISIBLE
                }
            }
        }

        override fun onClick(v: View?) {
            (activity as MainActivity).showLoadingDialog()
            requireDialog().dismiss()
            if (v!! == editUserTV) {
                goToEditUserFragment(false)
            } else if (v == addPointsTV) {
                showPointsDialog("ADD")
            } else if (v == removePointsTV) {
                showPointsDialog("REMOVE")
            } else if (v == giveAdminRightsTV) {
                goToGiveAdminRightsFragment
            } else if (v == deleteUserTV) {
                deleteUser()
            }
        }
    }
}