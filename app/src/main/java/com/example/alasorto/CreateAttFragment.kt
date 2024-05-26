package com.example.alasorto

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
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
    private val dateUtils = DateUtils()

    private val viewModel: AppViewModel by viewModels()
    private val pendingAttendanceViewModel: PendingAttendanceViewModel by viewModels()

    //Array List of all users and each changes when user moves to or from att list
    private var allUsersList = ArrayList<UserData>()
    private var allUsersIds = ArrayList<String>()
    private var hasConnection = false
    private var editingAttendance: Attendance? = null //Attendance that is being edit
    private var allUsersFilteredList = ArrayList<UserData>()
    private var isNewAttendance = true

    //Array lists for editing attendance only
    private val oldUsersList = ArrayList<String>()

    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private lateinit var attAllUsersAdapter: AttAllUsersAdapter
    private lateinit var saveAttendanceBtn: ImageButton
    private lateinit var clearUsersBtn: ImageButton
    private lateinit var usersRV: RecyclerView
    private lateinit var pointsDialog: Dialog
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    //Internet connection
    private lateinit var internetCheck: InternetCheck

    @SuppressLint("NotifyDataSetChanged")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        //Initialize widgets
        searchView = view.findViewById(R.id.searchView_attendance)
        saveAttendanceBtn = view.findViewById(R.id.btn_save_attendance)
        clearUsersBtn = view.findViewById(R.id.btn_clear_att)
        usersRV = view.findViewById(R.id.rv_att_all_users)

        sharedPreferences =
            requireContext().getSharedPreferences("ATTENDED_USERS", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        allUsersList.addAll(
            kotlin.collections.ArrayList(
                (activity as MainActivity).getAllUsers().filter { it.verified })
        )
        allUsersList.sortBy { it.name }
        for (user in allUsersList) {
            if (editingAttendance == null) { //If null add all users to the list
                allUsersIds.add(user.userId)
            } else { //If not null add only members whose account creation date is before the creation of attendance
                if (user.creationDate.before(editingAttendance!!.date)) {
                    allUsersIds.add(user.userId)
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
            oldUsersList.addAll(editingAttendance!!.usersIDs)

            isNewAttendance = args.getBoolean("IS_NEW_ATT", true)

            for (userId in oldUsersList) {
                val user = allUsersList.first { it.userId.contains(userId) }
                attUsersList.add(user)
            }
        } else {
            val attSet = sharedPreferences.getStringSet("ATT_SET", setOf())
            if (attSet != null) {
                for (user in attSet) {
                    if (allUsersList.any { it.userId == user }) {
                        attUsersList.add(allUsersList.first { it.userId == user })
                    }
                }
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

        saveAttendanceBtn.setOnClickListener {
            if (editingAttendance == null) {
                showDatePickedDialog()
            } else {
                if (editingAttendance != null) {
                    saveAttendance(
                        editingAttendance!!.day,
                        editingAttendance!!.month,
                        editingAttendance!!.year
                    )
                }
            }
        }

        clearUsersBtn.setOnClickListener {
            val builder: AlertDialog.Builder =
                AlertDialog.Builder(activity as MainActivity)
            builder.setCancelable(true)
            builder.setTitle(R.string.reset_users_list)
            builder.setMessage(R.string.reset_users_list_confirm)
            builder.setPositiveButton(R.string.yes) { _, _ ->
                clearAttList()
            }
            builder.setNegativeButton(R.string.no) { _, _ -> }
            builder.show()
        }

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

    private fun clearAttList() {
        attUsersList.clear()
        editor.putStringSet("ATT_SET", attUsersList.map { it.userId }.toSet())
        editor.apply()
        attAllUsersAdapter.notifyDataSetChanged()
    }

    private fun openCalendar() {

    }

    private fun saveAttendance(day: Int, month: Int, year: Int) {
        attendedUsersIds.clear()
        for (user in attUsersList) {
            attendedUsersIds.add(user.userId)
            //requireActivity().createNotification(user.Token!!)
        }

        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day)

        val documentIdFormatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SimpleDateFormat("dd-MM-yy", Locale.ENGLISH)
        } else {
            java.text.SimpleDateFormat("dd-MM-yy", Locale.ENGLISH)
        }

        val databaseIdFormatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SimpleDateFormat("dd/MM/yy", Locale.ENGLISH)
        } else {
            java.text.SimpleDateFormat("dd/MM/yy", Locale.ENGLISH)
        }

        val attendanceId = if (editingAttendance == null) {
            documentIdFormatter.format(cal.time)
        } else {
            editingAttendance!!.id
        }

        val allUsersIdsList = ArrayList(allUsersList.map { it.userId })

        val attendancePercentage = if (attendedUsersIds.size <= allUsersIdsList.size) {
            (attendedUsersIds.size.toFloat() / allUsersIdsList.size.toFloat()) * 100f
        } else {
            0f
        }

        val attendance = Attendance(
            day,
            month,
            year,
            attendancePercentage,
            attendanceId,
            dateUtils.getTime(),
            attendedUsersIds
        )

        val pendingAttendance =
            PendingAttendance(
                databaseIdFormatter.format(cal.time),
                attendance,
                isNewAttendance,
                isDeleting = false,
                startedUpload = false,
                allUsers = allUsersIdsList,
                attendedUsers = attendedUsersIds
            )

        //viewModel.resetAtt(allUsersIdsList)
        pendingAttendanceViewModel.addAttendance(pendingAttendance)

        dismissFragment()
        clearAttList()
    }

    override fun onPause() {
        super.onPause()
        editor.putStringSet("ATT_SET", attUsersList.map { it.userId }.toSet())
        editor.apply()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (attUsersList.isEmpty()) {
                        dismissFragment()
                    } else {
                        val builder: AlertDialog.Builder =
                            AlertDialog.Builder(activity as MainActivity)
                        builder.setCancelable(true)
                        builder.setTitle(R.string.discard_changes)
                        builder.setMessage(R.string.discard_changes_message)
                        builder.setPositiveButton(R.string.yes) { _, _ ->
                            dismissFragment()
                        }
                        builder.setNegativeButton(R.string.no) { _, _ -> }
                        builder.show()
                    }
                }
            })
    }

    private fun dismissFragment() {
        activity!!.supportFragmentManager.popBackStack(
            "CREATE_NEW_ATT_FRAGMENT",
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
    }

    override fun onClick(user: UserData, position: Int) {
        if (!attUsersList.contains(user)) {
            attUsersList.add(user)
        } else {
            attUsersList.remove(user)
        }
        attAllUsersAdapter.notifyItemChanged(position)
        editor.putStringSet("ATT_SET", attUsersList.map { it.userId }.toSet())
        editor.apply()
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

    private fun showDatePickedDialog() {

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            com.google.android.material.R.style.Widget_Material3_MaterialCalendar,
            DatePickerDialog.OnDateSetListener { _, y, m, d ->
                saveAttendance(d, m + 1, y)
            },
            dateUtils.getYear(),
            dateUtils.getMonth(),
            dateUtils.getDay()
        )

        val window = datePickerDialog.window
        window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        datePickerDialog.setCancelable(true)
        datePickerDialog.show()
    }
}