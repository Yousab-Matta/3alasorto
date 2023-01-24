package com.example.alasorto

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alasorto.adapters.AttAllUsersAdapter
import com.example.alasorto.dataClass.Users
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class CreateAttFragment : Fragment(), AttAllUsersAdapter.OnClickListener {
    private val auth = Firebase.auth
    private val authUser = auth.currentUser
    private val phoneNum = authUser?.phoneNumber

    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private lateinit var saveAttendanceBtn: ImageButton
    private lateinit var usersRV: RecyclerView

    private lateinit var viewModel: AppViewModel

    //Array List of all users and each changes when user moves to or from att list
    private var allUsersList = ArrayList<Users>()
    private var allUsersIds = ArrayList<String>()

    //Array List of and each changes when user moves to or from users list
    private val attUsersList = ArrayList<Users>()

    private val attendedUsersIds = ArrayList<String>()
    private val attendedUsersNames = ArrayList<String>()

    //Progress Dialog stuff
    private lateinit var builder: AlertDialog.Builder
    private lateinit var dialog: AlertDialog
    private lateinit var window: Window

    //Internet connection
    private lateinit var internetCheck: InternetCheck
    private var hasConnection = false

    private lateinit var attAllUsersAdapter: AttAllUsersAdapter
    private var allUsersFilteredList = ArrayList<Users>()
    private var totalAtt = -1

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        //Initialize view
        val view = inflater.inflate(R.layout.fragment_create_att, container, false)
        view.isClickable = true

        //Initialize widgets
        searchView = view.findViewById(R.id.searchView_attendance)
        saveAttendanceBtn = view.findViewById(R.id.btn_save_attendance)
        usersRV = view.findViewById(R.id.rv_att_all_users)

        //Create Alert Dialogue
        builder = AlertDialog.Builder(context)
        builder.setCancelable(false)
        builder.setView(R.layout.progress_dialogue)
        dialog = builder.create()
        window = dialog.window!!
        window.setBackgroundDrawableResource(android.R.color.transparent)

        usersRV.layoutManager = LinearLayoutManager(context)

        //Initialize VM
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
            if (it) {
                viewModel.getAllUsers()
            }
        }

        allUsersList.addAll((activity as MainActivity).getAllUsers())
        allUsersList.sortBy { it.Name }
        for (user in allUsersList) {
            allUsersIds.add(user.Phone.toString())
        }
        attAllUsersAdapter =
            AttAllUsersAdapter(allUsersList, attUsersList, this, requireContext())
        usersRV.adapter = attAllUsersAdapter


        /*After Creating new Att data app will observe the total attendance times
        next app will update att % for each user*/
        viewModel.totalAttNumberMLD.observe(this.viewLifecycleOwner, Observer {
            totalAtt = it
            if (hasConnection) {
                viewModel.handleUserAtt(attendedUsersIds, allUsersIds, totalAtt)
            }
        })

        viewModel.clearFragmentMLD.observe(this.viewLifecycleOwner, Observer {
            if (it == true) {
                dialog.dismiss()
                Toast.makeText(context, "Attendance was uploaded successfully", Toast.LENGTH_SHORT)
                    .show()
                requireActivity().supportFragmentManager.popBackStack(
                    "CREATE_NEW_ATT_FRAGMENT", FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
            }
        })

        saveAttendanceBtn.setOnClickListener(View.OnClickListener
        {
            if (hasConnection) {
                dialog.show()
                attendedUsersIds.clear()
                for (user in attUsersList) {
                    attendedUsersIds.add(user.Phone!!)
                    //requireActivity().createNotification(user.Token!!)
                }
                viewModel.createAttendance(attendedUsersIds)
                viewModel.getCurrentUser(phoneNum!!)
                //requireActivity().createNotification(viewModel.currentUserMLD.value!!.Token.toString())
                saveAttendanceBtn.isClickable = false
            }
        })

        //Filter Users
        searchView.setOnQueryTextListener(
            object :
                androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(p0: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(p0: String?): Boolean {
                    if (p0 != null) {
                        filterAllUsers(p0)
                    }
                    return false
                }
            })

        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    activity!!.supportFragmentManager.popBackStack(
                        "CREATE_NEW_ATT_FRAGMENT",
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                    this.isEnabled = false
                }
            })

        return view
    }

    override fun onClick(user: Users, attend: Boolean, position: Int) {
        if (attend) {
            attend(user, position)
        } else {
            absent(user, position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun attend(user: Users, position: Int) {
        if (!attUsersList.contains(user)) {
            attUsersList.add(user)
            attAllUsersAdapter.notifyItemChanged(position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun absent(user: Users, position: Int) {
        if (attUsersList.contains(user)) {
            attUsersList.remove(user)
            attAllUsersAdapter.notifyItemChanged(position)
        }
    }

    private fun filterAllUsers(text: String) {
        allUsersFilteredList.clear()
        for (user in allUsersList) {
            if (user.Name!!.lowercase().contains(text.lowercase())) {
                allUsersFilteredList.add(user)
            }
        }
        attAllUsersAdapter.filterList(allUsersFilteredList)
    }
}