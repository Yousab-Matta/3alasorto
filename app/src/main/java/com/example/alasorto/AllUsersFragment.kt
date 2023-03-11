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
import android.view.View.OnClickListener
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alasorto.adapters.AllUsersAdapter
import com.example.alasorto.dataClass.Users
import com.example.alasorto.utils.InternetCheck
import com.example.alasorto.utils.LinearSpacingItemDecorator
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AllUsersFragment : Fragment(), AllUsersAdapter.OnClickListener,
    AllUsersAdapter.OnLongClickListener {
    private val usersControlsDialog = UsersControlsDialog(this)
    private val viewModel: AppViewModel by viewModels()

    private lateinit var usersRV: RecyclerView
    private lateinit var adapter: AllUsersAdapter
    private lateinit var addUserBtn: ImageButton
    private lateinit var addPointsBtn: ImageButton
    private lateinit var searchUsersET: EditText
    private lateinit var sortBtn: ImageView
    private lateinit var selectedUser: Users
    private lateinit var currentUser: Users
    private lateinit var internetCheck: InternetCheck
    private lateinit var pointsDialog: Dialog
    private lateinit var monthsDialog: Dialog
    private lateinit var sortDialog: Dialog
    private var hasConnection = false

    private var month: Int = 0

    private val allUsersList = ArrayList<Users>()
    private val selectedUsersList = ArrayList<Users>()
    private val usersFilteredList = ArrayList<Users>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_all_users, container, false)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
        }

        //Initialize Widgets
        usersRV = view.findViewById(R.id.rv_all_users)
        sortBtn = view.findViewById(R.id.iv_filter_users)
        addUserBtn = view.findViewById(R.id.btn_add_user)
        addPointsBtn = view.findViewById(R.id.btn_add_points)
        searchUsersET = view.findViewById(R.id.et_user_search)

        allUsersList.addAll((activity as MainActivity).getAllUsers())
        usersFilteredList.addAll(allUsersList)

        currentUser = (activity as MainActivity).getCurrentUser()

        adapter =
            AllUsersAdapter(
                usersFilteredList,
                selectedUsersList,
                this,
                this,
                currentUser.Access.toString(),
                requireContext()
            )

        //Setting LL manager to RV
        usersRV.layoutManager = LinearLayoutManager(context)

        usersRV.adapter = adapter

        usersRV.addItemDecoration(LinearSpacingItemDecorator(30))

        //Observing all users
        viewModel.usersMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                allUsersList.clear()
                allUsersList.addAll(it)
                usersFilteredList.clear()
                usersFilteredList.addAll(it)
                adapter.notifyDataSetChanged()
            }
        })

        addUserBtn.setOnClickListener(OnClickListener {
            editUserFragment(true)
        })

        addPointsBtn.setOnClickListener(OnClickListener {
            showAddPointsDialog("ADD")
        })

        sortBtn.setOnClickListener(OnClickListener {
            showSortDialog()
        })

        //Check changes in firestore database
        Firebase.firestore.collection("Users").addSnapshotListener { _, _ ->
            if (hasConnection) {
                viewModel.getAllUsers()
            }
        }

        searchUsersET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                usersFilteredList.clear()
                val searchText = p0.toString().lowercase()
                for (user in allUsersList) {
                    if (user.Name.toString().lowercase().contains(searchText)) {
                        usersFilteredList.add(user)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })

        //Notify changes in Comments database
        Firebase.firestore.collection("Users").addSnapshotListener { _, _ ->
            if (hasConnection) {
                viewModel.getAllUsers()
            } else {
                Toast.makeText(context, R.string.check_connection, Toast.LENGTH_SHORT).show()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().supportFragmentManager.popBackStack(
                        "ALL_USERS_FRAGMENT",
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                    this.isEnabled = false
                }
            })
    }

    override fun onClick(user: Users) {
        selectedUser = user
        if (selectedUsersList.contains(selectedUser)) {
            selectedUsersList.remove(user)
        } else {
            selectedUsersList.add(user)
        }
        if (selectedUsersList.isEmpty()) {
            addPointsBtn.visibility = View.GONE
            addUserBtn.visibility = View.VISIBLE
        } else {
            addPointsBtn.visibility = View.VISIBLE
            addUserBtn.visibility = View.GONE
        }

        adapter.notifyItemRangeChanged(0, allUsersList.size)
    }

    override fun onLongClick(user: Users) {
        selectedUser = user
        //ToDo: Check user access first
        if (currentUser.Access != "User") {
            usersControlsDialog.show(
                requireActivity().supportFragmentManager,
                "USERS_CONTROLS_DIALOG"
            )
        } else {
            viewUserFragment()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showSortDialog() {
        sortDialog = Dialog(requireContext())
        sortDialog.setCancelable(true)
        sortDialog.setContentView(R.layout.layout_sort_user)

        val sortByNameTV: TextView = sortDialog.findViewById(R.id.tv_sort_by_name)
        val sortByMonthTV: TextView = sortDialog.findViewById(R.id.tv_sort_by_month)

        sortByNameTV.setOnClickListener(OnClickListener {
            usersFilteredList.clear()
            usersFilteredList.addAll(allUsersList)
            adapter.notifyDataSetChanged()
            sortDialog.dismiss()
        })

        sortByMonthTV.setOnClickListener(OnClickListener {
            showMonthsDialog()
            sortDialog.dismiss()
        })

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

    fun showAddPointsDialog(case: String) {
        //Create another dialogue to get number of points
        pointsDialog = Dialog(requireContext())
        pointsDialog.setCancelable(true)
        pointsDialog.setContentView(R.layout.layout_one_et)

        val pointsET: EditText = pointsDialog.findViewById(R.id.et_create_group_name)
        val confirmBtn: Button = pointsDialog.findViewById(R.id.btn_create_group)

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
                    if (hasConnection) {
                        //If list is empty -> adds points for single user
                        if (selectedUsersList.isEmpty()) {
                            if (case == "ADD") {
                                viewModel.editUserPoints(
                                    (selectedUser.Points!! + pointsET.text.toString()
                                        .toInt()), selectedUser.Phone.toString()
                                )
                            } else {
                                viewModel.editUserPoints(
                                    (selectedUser.Points!! - pointsET.text.toString()
                                        .toInt()), selectedUser.Phone.toString()
                                )
                            }
                            //Else adds for all users in the list
                        } else {
                            for (user in selectedUsersList) {
                                if (case == "ADD") {
                                    viewModel.editUserPoints(
                                        (selectedUser.Points!! + pointsET.text.toString()
                                            .toInt()), selectedUser.Phone.toString()
                                    )
                                } else {
                                    viewModel.editUserPoints(
                                        (selectedUser.Points!! - pointsET.text.toString()
                                            .toInt()), selectedUser.Phone.toString()
                                    )
                                }
                            }
                            selectedUsersList.clear()
                            adapter.notifyItemRangeChanged(0, selectedUsersList.size)
                            addPointsBtn.visibility = View.GONE
                            addUserBtn.visibility = View.VISIBLE
                        }
                        pointsDialog.dismiss()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            R.string.check_connection,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Please enter data", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please check your internet connection",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        pointsDialog.show()
    }

    fun deleteUser() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity as MainActivity)
        builder.setCancelable(true)
        builder.setTitle("Delete user?")
        builder.setMessage("Are you sure you want to delete this user?")
        builder.setPositiveButton("Yes") { _, _ ->
            if (hasConnection) {
                viewModel.deleteUser(selectedUser.Phone.toString())
            }
        }
        builder.setNegativeButton("No") { _, _ -> }
        builder.show()
    }

    private fun editUserFragment(isNewUser: Boolean) {
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val fragment = CreateUserDataFragment()
        if (!isNewUser) {
            val bundle = Bundle()
            bundle.putParcelable("Editing_User", selectedUser)
            bundle.putBoolean("isNewUser", false)
            fragment.arguments = bundle
        }
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("HANDLE_USERS_FRAGMENT")
        transaction.commit()
    }

    private fun viewUserFragment() {
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val fragment = ProfileFragment()
        val bundle = Bundle()
        bundle.putParcelable("Profile_User", selectedUser)
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("PROFILE_FRAGMENT")
        transaction.commit()
    }

    class UsersControlsDialog(fragment: AllUsersFragment) : BottomSheetDialogFragment() {
        private val mFragment = fragment

        override fun getTheme(): Int {
            return R.style.AppBottomSheetDialogTheme
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.bottom_sheet_users, container, false)
            view.isClickable = true
            return view
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val viewUserProfileLL: LinearLayout = view.findViewById(R.id.ll_view_user)
            val editUserLL: LinearLayout = view.findViewById(R.id.ll_edit_user)
            val addPointsLL: LinearLayout = view.findViewById(R.id.ll_add_points)
            val removePointsLL: LinearLayout = view.findViewById(R.id.ll_remove_points)
            val deleteUserLL: LinearLayout = view.findViewById(R.id.ll_delete_user)
            val giveAdminRightsLL: LinearLayout = view.findViewById(R.id.ll_give_admin_rights)

            editUserLL.setOnClickListener(OnClickListener {
                mFragment.editUserFragment(false)
                requireDialog().dismiss()
            })

            addPointsLL.setOnClickListener(OnClickListener {
                mFragment.showAddPointsDialog("ADD")
                requireDialog().dismiss()
            })

            removePointsLL.setOnClickListener(OnClickListener {
                mFragment.showAddPointsDialog("REMOVE")
                requireDialog().dismiss()
            })

            viewUserProfileLL.setOnClickListener(OnClickListener {
                mFragment.viewUserFragment()
                requireDialog().dismiss()
            })

            giveAdminRightsLL.setOnClickListener(OnClickListener {
                /*val manager = requireActivity().supportFragmentManager
                val transaction = manager.beginTransaction()
                //ToDo*/
            })

            deleteUserLL.setOnClickListener(OnClickListener {
                mFragment.deleteUser()
                requireDialog().dismiss()
            })
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun sortUsersByMonth(month: Int) {
        usersFilteredList.clear()
        for (user in allUsersList) {
            if (user.BirthMonth == month) {
                usersFilteredList.add(user)
            }
            adapter.notifyDataSetChanged()
            monthsDialog.dismiss()
        }
    }
}