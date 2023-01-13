package com.example.alasorto

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alasorto.adapters.AllUsersAdapter
import com.example.alasorto.dataClass.Users
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AllUsersFragment : Fragment(), AllUsersAdapter.OnClickListener {
    private lateinit var usersRV: RecyclerView
    private lateinit var adapter: AllUsersAdapter
    private lateinit var viewModel: AppViewModel
    private lateinit var addUserBtn: ImageButton
    private lateinit var searchUsersET: EditText
    private lateinit var user: Users
    private lateinit var internetCheck: InternetCheck
    private var hasConnection = false

    private val usersList = ArrayList<Users>()
    private val usersFilteredList = ArrayList<Users>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_all_users, container, false)

        //Initialize Widgets
        usersRV = view.findViewById(R.id.rv_all_users)
        addUserBtn = view.findViewById(R.id.btn_add_user)
        searchUsersET = view.findViewById(R.id.et_user_search)

        //Setting LL manager to RV
        usersRV.layoutManager = LinearLayoutManager(context)

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

        //Observing all users
        viewModel.usersMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                usersList.clear()
                usersFilteredList.clear()
                usersList.addAll(it)
                usersFilteredList.addAll(it)
                adapter = AllUsersAdapter(usersFilteredList, this)
                usersRV.adapter = adapter
            }
        })

        addUserBtn.setOnClickListener(View.OnClickListener {
            editUserFragment(true)
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

        return view
    }

    override fun onClick(user: Users) {
        this.user = user
        if (viewModel.currentUserMLD.value!!.Access != "User") {
            showDialog()
        } else {
            viewUserFragment()
        }
    }

    private fun editUserFragment(isNewUser: Boolean) {
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val fragment = CreateUserDataFragment()
        if (!isNewUser) {
            val bundle = Bundle()
            bundle.putParcelable("Editing_User", user)
            bundle.putBoolean("isNewUser", false)
            fragment.arguments = bundle
        }
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun viewUserFragment() {
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val fragment = ProfileFragment()
        val bundle = Bundle()
        bundle.putParcelable("Viewing_User", user)
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun showDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_sheet_users)

        val editUserLL: LinearLayout = dialog.findViewById(R.id.ll_edit_user)
        val viewUserProfileLL: LinearLayout = dialog.findViewById(R.id.ll_view_user)
        val deleteUserLL: LinearLayout = dialog.findViewById(R.id.ll_delete_user)

        editUserLL.setOnClickListener(View.OnClickListener {
            editUserFragment(false)
            dialog.dismiss()
        })

        viewUserProfileLL.setOnClickListener(View.OnClickListener {
            viewUserFragment()
            dialog.dismiss()
        })

        deleteUserLL.setOnClickListener(View.OnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(activity as MainActivity)
            builder.setCancelable(true)
            builder.setTitle("Delete user?")
            builder.setMessage("Are you sure you want to delete this user?")
            builder.setPositiveButton("Yes") { _, _ ->
                if (hasConnection) {
                    viewModel.deleteUser(user.Phone.toString())
                }
            }
            builder.setNegativeButton("No") { _, _ -> }
            builder.show()
            dialog.dismiss()
        })

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
    }
}