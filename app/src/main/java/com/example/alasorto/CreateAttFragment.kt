package com.example.alasorto

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alasorto.adapters.AttAllUsersAdapter
import com.example.alasorto.dataClass.Attendance
import com.example.alasorto.dataClass.UserData
import com.example.alasorto.pendingAttendanceDatabase.PendingAttendance
import com.example.alasorto.pendingAttendanceDatabase.PendingAttendanceViewModel
import com.example.alasorto.utils.DateUtils
import com.example.alasorto.utils.InternetCheck
import com.example.alasorto.utils.LinearSpacingItemDecorator
import com.example.alasorto.viewModels.AppViewModel
import java.util.*
import kotlin.collections.ArrayList

class CreateAttFragment : Fragment(R.layout.fragment_create_att),
    AttAllUsersAdapter.OnClickListener {
    //Array List of and each changes when user moves to or from users list
    private val attUsersList = ArrayList<UserData>()
    private val attendedUsersIds = ArrayList<String>()

    private val viewModel: AppViewModel by viewModels()
    private val pendingAttendanceViewModel: PendingAttendanceViewModel by viewModels()

    //Array List of all users and each changes when user moves to or from att list
    private var allUsersList = ArrayList<UserData>()
    private var allUsersIds = ArrayList<String>()
    private var hasConnection = false
    private var editingAttendance: Attendance? = null //Attendance that is being edit
    private var allUsersFilteredList = ArrayList<UserData>()

    //Array lists for editing attendance only
    private val oldUsersList = ArrayList<String>()

    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private lateinit var attAllUsersAdapter: AttAllUsersAdapter
    private lateinit var saveAttendanceBtn: ImageButton
    private lateinit var usersRV: RecyclerView
    private lateinit var pointsDialog: Dialog

    //Internet connection
    private lateinit var internetCheck: InternetCheck

    @SuppressLint("NotifyDataSetChanged")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        //Initialize widgets
        searchView = view.findViewById(R.id.searchView_attendance)
        saveAttendanceBtn = view.findViewById(R.id.btn_save_attendance)
        usersRV = view.findViewById(R.id.rv_att_all_users)

        allUsersList.addAll((activity as MainActivity).getAllUsers())
        allUsersList.sortBy { it.name }
        for (user in allUsersList) {
            if (editingAttendance == null) { //If null add all users to the list
                allUsersIds.add(user.phone)
            } else { //If not null add only members whose account creation date is before the creation of attendance
                if (user.creationDate.before(editingAttendance!!.date)) {
                    allUsersIds.add(user.phone)
                }
            }
        }

        val args = this.arguments
        if (args != null) {
            editingAttendance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                args.getParcelable("EDITING_ATTENDANCE", Attendance::class.java)
            } else {
                @Suppress("DEPRECATION")
                args.getParcelable("EDITING_ATTENDANCE")
            }
            oldUsersList.addAll(editingAttendance!!.usersIDs!!)

            for (userId in oldUsersList) {
                val user = allUsersList.first { it.phone.contains(userId) }
                attUsersList.add(user)
            }
        }

        //Set RV layout and spacing
        usersRV.layoutManager = LinearLayoutManager(context)
        usersRV.addItemDecoration(LinearSpacingItemDecorator(30))

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
            if (it) {
                viewModel.getAllUsers()
            }
        }

        attAllUsersAdapter =
            AttAllUsersAdapter(allUsersList, attUsersList, this, requireContext())
        usersRV.adapter = attAllUsersAdapter

        viewModel.dismissFragmentMLD.observe(this.viewLifecycleOwner, Observer {
            if (it == true) {
                (activity as MainActivity).dismissLoadingDialog()
                Toast.makeText(context, getString(R.string.attendance_uploaded), Toast.LENGTH_SHORT)
                    .show()
                requireActivity().supportFragmentManager.popBackStack(
                    "CREATE_NEW_ATT_FRAGMENT", FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
            }
        })

        viewModel.attendanceExistsMLD.observe(this.viewLifecycleOwner, Observer {
            if (it) {
                (activity as MainActivity).dismissLoadingDialog()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.attendance_exists), Toast.LENGTH_SHORT
                ).show()
            }
        })

        saveAttendanceBtn.setOnClickListener(View.OnClickListener
        {
            attendedUsersIds.clear()
            for (user in attUsersList) {
                attendedUsersIds.add(user.phone)
                //requireActivity().createNotification(user.Token!!)
            }

            val dateUtils = DateUtils()

            val day = dateUtils.getDay()
            val month = dateUtils.getMonth() + 1
            val year = dateUtils.getYear()

            val attendance = Attendance(
                day,
                month,
                year,
                "",
                dateUtils.getTime(),
                attendedUsersIds
            )

            if (hasConnection) {
                (activity as MainActivity).showLoadingDialog()
                if (editingAttendance == null) {
                    viewModel.createAttendance(attendance, allUsersIds)
                } else {
                    val removedUsersList = ArrayList<String>()
                    for (userId in oldUsersList) {
                        if (!attendedUsersIds.contains(userId)) {
                            removedUsersList.add(userId)
                        }
                    }

                    val addedUsersList = ArrayList<String>()
                    for (userId in attendedUsersIds) {
                        if (!oldUsersList.contains(userId)) {
                            addedUsersList.add(userId)
                        }
                    }
                    viewModel.editAttendance(
                        addedUsersList,
                        removedUsersList,
                        attendedUsersIds,
                        editingAttendance!!.id
                    )
                }
                viewModel.getCurrentUser()
                //requireActivity().createNotification(viewModel.currentUserMLD.value!!.Token.toString())
                saveAttendanceBtn.isClickable = false

            } else {
                val pendingAttendanceId = "$day/$month/$year"
                val pendingAttendance = PendingAttendance(pendingAttendanceId, attendance)
                pendingAttendanceViewModel.addAttendance(pendingAttendance)
                Toast.makeText(
                    requireContext(),
                    R.string.pending_attendance,
                    Toast.LENGTH_SHORT
                ).show()
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
    }

    override fun onResume() {
        super.onResume()
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
    }

    override fun onClick(user: UserData, attend: Boolean, position: Int) {
        if (attend) {
            attend(user, position)
        } else {
            absent(user, position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun attend(user: UserData, position: Int) {
        if (!attUsersList.contains(user)) {
            attUsersList.add(user)
            attAllUsersAdapter.notifyItemChanged(position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun absent(user: UserData, position: Int) {
        if (attUsersList.contains(user)) {
            attUsersList.remove(user)
            attAllUsersAdapter.notifyItemChanged(position)
        }
    }

    private fun filterAllUsers(text: String) {
        allUsersFilteredList.clear()
        for (user in allUsersList) {
            if (user.name.lowercase().contains(text.lowercase())) {
                allUsersFilteredList.add(user)
            }
        }
        attAllUsersAdapter.filterList(allUsersFilteredList)
    }
}