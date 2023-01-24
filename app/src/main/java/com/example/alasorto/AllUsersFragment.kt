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
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alasorto.adapters.AllUsersAdapter
import com.example.alasorto.dataClass.Users
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AllUsersFragment : Fragment(), AllUsersAdapter.OnClickListener,
    AllUsersAdapter.OnLongClickListener {
    private val usersControlsDialog = UsersControlsDialog(this)

    private lateinit var usersRV: RecyclerView
    private lateinit var adapter: AllUsersAdapter
    private lateinit var viewModel: AppViewModel
    private lateinit var addUserBtn: ImageButton
    private lateinit var addPointsBtn: ImageButton
    private lateinit var searchUsersET: EditText
    private lateinit var selectedUser: Users
    private lateinit var currentUser: Users
    private lateinit var internetCheck: InternetCheck
    private lateinit var pointsDialog: Dialog
    private var hasConnection = false

    private val usersList = ArrayList<Users>()
    private val selectedUsersList = ArrayList<Users>()
    private val usersFilteredList = ArrayList<Users>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_all_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        //Initializing ViewModel
        viewModel = ViewModelProvider(requireActivity())[AppViewModel::class.java]

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
            if (it) {
                viewModel.getAllUsers()
            }
        }

        //Initialize Widgets
        usersRV = view.findViewById(R.id.rv_all_users)
        addUserBtn = view.findViewById(R.id.btn_add_user)
        addPointsBtn = view.findViewById(R.id.btn_add_points)
        searchUsersET = view.findViewById(R.id.et_user_search)

        currentUser = (activity as MainActivity).getCurrentUser()

        //Setting LL manager to RV
        usersRV.layoutManager = LinearLayoutManager(context)

        //Observing all users
        viewModel.usersMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                usersList.clear()
                usersFilteredList.clear()
                usersList.addAll(it)
                usersFilteredList.addAll(it)
                adapter =
                    AllUsersAdapter(
                        usersFilteredList,
                        selectedUsersList,
                        this,
                        this,
                        currentUser.Access.toString(),
                        requireContext()
                    )
                usersRV.adapter = adapter
            }
        })

        addUserBtn.setOnClickListener(View.OnClickListener {
            editUserFragment(true)
        })

        addPointsBtn.setOnClickListener(View.OnClickListener {
            showAddPointsDialog()
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
                for (user in usersList) {
                    if (user.Name.toString().lowercase().contains(searchText)) {
                        usersFilteredList.add(user)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })

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

        adapter.notifyItemRangeChanged(0, usersList.size)
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

    fun showAddPointsDialog() {
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

        confirmBtn.setOnClickListener(View.OnClickListener {
            if (hasConnection) {
                if (pointsET.text.toString().isNotEmpty()) {
                    if (hasConnection) {
                        //If list is empty -> adds points for single user
                        if (selectedUsersList.isEmpty()) {
                            viewModel.addUserPoints(
                                (selectedUser.Points!! + pointsET.text.toString()
                                    .toInt()), selectedUser.Phone.toString()
                            )
                            //Else adds for all users in the list
                        } else {
                            for (user in selectedUsersList) {
                                viewModel.addUserPoints(
                                    (user.Points!! + pointsET.text.toString()
                                        .toInt()), user.Phone.toString()
                                )
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
                            R.string.check_internet,
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
            val deleteUserLL: LinearLayout = view.findViewById(R.id.ll_delete_user)
            val giveAdminRightsLL: LinearLayout = view.findViewById(R.id.ll_give_admin_rights)

            editUserLL.setOnClickListener(View.OnClickListener {
                mFragment.editUserFragment(false)
                requireDialog().dismiss()
            })

            addPointsLL.setOnClickListener(View.OnClickListener {
                mFragment.showAddPointsDialog()
                requireDialog().dismiss()
            })

            viewUserProfileLL.setOnClickListener(View.OnClickListener {
                mFragment.viewUserFragment()
                requireDialog().dismiss()
            })

            giveAdminRightsLL.setOnClickListener(View.OnClickListener {
                /*val manager = requireActivity().supportFragmentManager
                val transaction = manager.beginTransaction()
                //ToDo*/
            })

            deleteUserLL.setOnClickListener(View.OnClickListener {
                mFragment.deleteUser()
                requireDialog().dismiss()
            })
        }
    }
}