package com.example.alasorto

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alasorto.adapters.AttHistoryAdapter
import com.example.alasorto.adapters.GroupOfUsersAdapter
import com.example.alasorto.dataClass.Attendance
import com.example.alasorto.dataClass.UserData
import com.example.alasorto.pendingAttendanceDatabase.PendingAttendance
import com.example.alasorto.pendingAttendanceDatabase.PendingAttendanceViewModel
import com.example.alasorto.utils.InternetCheck
import com.example.alasorto.utils.LinearSpacingItemDecorator
import com.example.alasorto.viewModels.AppViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.collections.ArrayList

class AttendanceHistoryFragment : Fragment(R.layout.fragment_attendance_history) {
    private val viewModel: AppViewModel by viewModels()
    private val attendanceList = ArrayList<Attendance>()
    private val allUsersList = ArrayList<UserData>()
    private val usersList = ArrayList<UserData>()
    private val pendingAttendanceList = ArrayList<Attendance>() //List of (Attendance)
    private val pendingList = ArrayList<PendingAttendance>() //List of Pending Attendance
    private lateinit var attHistoryRV: RecyclerView

    private lateinit var pendingAttViewModel: PendingAttendanceViewModel
    private lateinit var attUsersRV: RecyclerView
    private lateinit var newAttBtn: ImageButton
    private lateinit var searchByDateBtn: ImageButton
    private lateinit var getAllAttendanceBtn: ImageButton
    private lateinit var pendingAttendanceBtn: ImageButton
    private lateinit var editUsersBtn: ImageButton
    private lateinit var headerTV: TextView
    private lateinit var internetCheck: InternetCheck
    private lateinit var currentAttendance: Attendance
    private lateinit var deletedAttendance: Attendance
    private lateinit var attHistoryAdapter: AttHistoryAdapter
    private lateinit var groupOfUsersAdapter: GroupOfUsersAdapter
    private lateinit var handleAttDialog: Dialog

    private var hasConnection = false
    private var showingPendingAtt = false
    private var isNewAttendance = true
    private var noAttendanceResultText = 0
    private var handleAttText = "" //Text that is shown in handling att

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        allUsersList.addAll((activity as MainActivity).getAllUsers())

        pendingAttViewModel =
            ViewModelProvider(requireActivity())[PendingAttendanceViewModel::class.java]

        attHistoryRV = view.findViewById(R.id.rv_attendance_history)
        attUsersRV = view.findViewById(R.id.rv_attendance_history_users)
        newAttBtn = view.findViewById(R.id.btn_create_data)
        editUsersBtn = view.findViewById(R.id.attendance_edit_users)
        searchByDateBtn = view.findViewById(R.id.attendance_date_search)
        getAllAttendanceBtn = view.findViewById(R.id.btn_get_all_att)
        pendingAttendanceBtn = view.findViewById(R.id.btn_get_pending_att)
        headerTV = view.findViewById(R.id.tv_att_desc)

        //Set Linear Layout to RVs'
        attHistoryRV.layoutManager = LinearLayoutManager(context)
        attUsersRV.layoutManager = LinearLayoutManager(context)

        //Set RVs' spacings
        attHistoryRV.addItemDecoration(LinearSpacingItemDecorator(30))
        attUsersRV.addItemDecoration(LinearSpacingItemDecorator(30))

        groupOfUsersAdapter = GroupOfUsersAdapter(usersList)
        attUsersRV.adapter = groupOfUsersAdapter

        //Set History Users RV adapter
        attHistoryAdapter =
            AttHistoryAdapter(attendanceList, ::goToAttendanceUsers, ::handleAttendance)
        attHistoryRV.adapter = attHistoryAdapter

        pendingAttViewModel.readAllData.observe(this.viewLifecycleOwner, Observer {

            pendingList.clear()
            pendingList.addAll(it)

            pendingAttendanceList.clear()
            for (pendingAttendance in it) {
                pendingAttendanceList.add(pendingAttendance.attendance)
            }
            pendingAttendanceList.sortByDescending { it1 -> it1.date }

            if (showingPendingAtt) {
                attHistoryAdapter.setAttendanceList(pendingAttendanceList)
            }
        })

        viewModel.attendanceListMLD.observe(this.viewLifecycleOwner, Observer {
            if (!it.isNullOrEmpty()) {
                attendanceList.clear()
                attendanceList.addAll(it)
                attHistoryAdapter.setAttendanceList(it)
            } else {
                Toast.makeText(requireContext(), noAttendanceResultText, Toast.LENGTH_SHORT)
                    .show()
            }
        })

        viewModel.attendanceItemMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                val index = attendanceList.indexOfFirst { it1 -> it1.id == it.id }
                if (index > -1) {
                    attendanceList[index] = it
                    attHistoryAdapter.notifyItemChanged(index)
                }
            }
        })

        viewModel.finishedAttendanceMLD.observe(this.viewLifecycleOwner, Observer {
            pendingAttViewModel.readAllData
        })

        //Go To create new Attendance Fragment
        newAttBtn.setOnClickListener(View.OnClickListener {

            val manager = requireActivity().supportFragmentManager
            val transaction = manager.beginTransaction()
            transaction.add(R.id.main_frame, CreateAttFragment())
            transaction.addToBackStack("CREATE_NEW_ATT_FRAGMENT")
            transaction.commit()
        })

        searchByDateBtn.setOnClickListener {
            showingPendingAtt = false
            noAttendanceResultText = R.string.no_attendance_record_for_date
            showDatePickedDialog()
        }

        getAllAttendanceBtn.setOnClickListener {
            showingPendingAtt = false
            noAttendanceResultText = R.string.no_attendance_records
            if (hasConnection) {
                viewModel.getAllAttendance()
            } else {
                Toast.makeText(requireContext(), R.string.check_connection, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        pendingAttendanceBtn.setOnClickListener {
            showingPendingAtt = true
            attendanceList.clear()
            attHistoryAdapter.setAttendanceList(pendingAttendanceList)
            if (pendingAttendanceList.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.no_pending_attendance),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        searchByDateBtn.setOnLongClickListener {
            Toast.makeText(requireContext(), R.string.search_att_by_date, Toast.LENGTH_SHORT)
                .show()
            true
        }

        getAllAttendanceBtn.setOnLongClickListener {
            Toast.makeText(requireContext(), R.string.get_all_att_records, Toast.LENGTH_SHORT)
                .show()
            true
        }

        pendingAttendanceBtn.setOnLongClickListener {
            Toast.makeText(requireContext(), R.string.get_pending_attendance, Toast.LENGTH_SHORT)
                .show()
            true
        }

        editUsersBtn.setOnClickListener {
            switchRV(false, showEditButton = false)

            if (pendingList.any { a -> a.attendance.id == currentAttendance.id }) {
                val pendingAtt =
                    pendingList.first { a -> a.attendance.id == currentAttendance.id }

                isNewAttendance = pendingAtt.isNewAttendance && !pendingAtt.startedUpload
            }

            val fragment = CreateAttFragment()
            val manager = requireActivity().supportFragmentManager
            val transaction = manager.beginTransaction()
            val bundle = Bundle()
            bundle.putParcelable("EDITING_ATTENDANCE", currentAttendance)
            bundle.putBoolean("IS_NEW_ATT", isNewAttendance)
            fragment.arguments = bundle
            transaction.add(R.id.main_frame, fragment)
            transaction.addToBackStack("CREATE_NEW_ATT_FRAGMENT")
            transaction.commit()
        }

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
        }
    }

    override fun onResume() {
        super.onResume()

        Firebase.firestore.collection("Attendance").addSnapshotListener()
        { _, _ ->
            if (hasConnection) {
                for (item in attendanceList) {
                    viewModel.getAttendanceById(item.id)
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (attUsersRV.visibility == VISIBLE) {
                        switchRV(false, showEditButton = false)
                    } else {
                        requireActivity().supportFragmentManager.popBackStack(
                            "ATTENDANCE_FRAGMENT",
                            FragmentManager.POP_BACK_STACK_INCLUSIVE
                        )
                        this.isEnabled = false
                    }
                }
            })
    }

    private fun showDatePickedDialog() {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            com.google.android.material.R.style.Widget_Material3_MaterialCalendar,
            DatePickerDialog.OnDateSetListener { _, y, m, d ->

                viewModel.getAttendanceByDate(d, m + 1, y)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        val window = datePickerDialog.window
        window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        datePickerDialog.setCancelable(true)
        datePickerDialog.show()
    }

    private fun switchRV(showingUsers: Boolean, showEditButton: Boolean) {
        val fadeIn: Animation = AlphaAnimation(0f, 1f)
        fadeIn.interpolator = DecelerateInterpolator()
        fadeIn.duration = 200
        fadeIn.startOffset = 200
        val fadeOut: Animation = AlphaAnimation(1f, 0f)
        fadeOut.interpolator = AccelerateInterpolator()
        fadeOut.duration = 200
        if (showingUsers) {
            usersList.clear()
            val idsList = ArrayList(currentAttendance.usersIDs)
            for (user in allUsersList) {
                if (idsList.contains(user.userId)) {
                    usersList.add(user)
                }
            }
            groupOfUsersAdapter.notifyItemRangeInserted(0, usersList.size)
            searchByDateBtn.visibility = GONE
            getAllAttendanceBtn.visibility = GONE
            pendingAttendanceBtn.visibility = GONE
            newAttBtn.visibility = GONE
            attHistoryRV.visibility = GONE
            attUsersRV.visibility = VISIBLE
            editUsersBtn.visibility = if (showEditButton) {
                editUsersBtn.startAnimation(fadeIn)
                VISIBLE
            } else {
                GONE
            }

            attHistoryRV.startAnimation(fadeOut)
            searchByDateBtn.startAnimation(fadeOut)
            getAllAttendanceBtn.startAnimation(fadeOut)
            pendingAttendanceBtn.startAnimation(fadeOut)
            newAttBtn.startAnimation(fadeOut)
            attUsersRV.startAnimation(fadeOut)

            "${currentAttendance.day}/${currentAttendance.month}/${currentAttendance.year}"
                .also {
                    headerTV.text = it
                }
        } else {
            val size = usersList.size
            usersList.clear()
            groupOfUsersAdapter.notifyItemRangeRemoved(0, size)
            searchByDateBtn.visibility = VISIBLE
            attHistoryRV.visibility = VISIBLE
            getAllAttendanceBtn.visibility = VISIBLE
            pendingAttendanceBtn.visibility = VISIBLE
            newAttBtn.visibility = VISIBLE
            attUsersRV.visibility = GONE
            editUsersBtn.visibility = GONE
            searchByDateBtn.startAnimation(fadeIn)
            attHistoryRV.startAnimation(fadeIn)
            getAllAttendanceBtn.startAnimation(fadeIn)
            pendingAttendanceBtn.startAnimation(fadeIn)
            newAttBtn.startAnimation(fadeIn)
            attUsersRV.startAnimation(fadeOut)
            editUsersBtn.startAnimation(fadeOut)
            headerTV.setText(R.string.att_history)
        }
    }

    private fun goToAttendanceUsers(attendance: Attendance) {

        currentAttendance = attendance

        val showEditBtn = if (pendingList.any { it.attendance.id == attendance.id }) {
            val pendingAttendance = pendingList.first { it.attendance.id == attendance.id }
            !pendingAttendance.startedUpload
        } else {
            true
        }
        switchRV(true, showEditBtn)
    }

    private fun handleAttendance(attendance: Attendance) {

        val pendingAttendance = pendingList.firstOrNull { it.attendance.id == attendance.id }

        if (!showingPendingAtt) {
            deleteAttendance(attendance)
        } else {

            handleAttDialog = Dialog(requireContext())
            handleAttDialog.setCancelable(true)
            handleAttDialog.setContentView(R.layout.layout_handle_att)

            //Init onclick
            val onClick = View.OnClickListener { p0 ->
                when (p0!!.id) {
                    R.id.tv_delete -> {
                        deleteAttendance(attendance)
                    }
                    R.id.tv_upload -> {
                        val builder: AlertDialog.Builder =
                            AlertDialog.Builder(activity as MainActivity)
                        builder.setCancelable(true)
                        builder.setTitle(R.string.upload_attendance)
                        builder.setMessage(R.string.upload_attendance_confirmation)
                        builder.setPositiveButton(R.string.yes) { _, _ ->

                            if (pendingAttendance != null) {
                                pendingAttViewModel.getAttendanceById(pendingAttendance.databaseId)

                                if (!hasConnection) {
                                    Toast.makeText(
                                        requireContext(),
                                        R.string.pending_attendance,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    if (pendingAttendance.isNewAttendance) {
                                        Toast.makeText(
                                            requireContext(),
                                            R.string.creating_new_att,
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            R.string.editing_att,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                        builder.setNegativeButton(R.string.no) { _, _ -> }
                        builder.show()
                    }
                }
                handleAttDialog.dismiss()
            }

            val deleteTV: TextView = handleAttDialog.findViewById(R.id.tv_delete)
            val uploadTV: TextView = handleAttDialog.findViewById(R.id.tv_upload)

            deleteTV.setOnClickListener(onClick)
            uploadTV.setOnClickListener(onClick)

            if (pendingAttendance != null && pendingAttendance.startedUpload) {
                deleteTV.visibility = GONE
            }

            val window = handleAttDialog.window
            window!!.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            window.setBackgroundDrawable(
                InsetDrawable(
                    ColorDrawable(Color.TRANSPARENT),
                    50
                )
            )
            handleAttDialog.show()
        }
    }

    private fun deleteAttendance(attendance: Attendance) {
        deletedAttendance = attendance

        val cal = Calendar.getInstance()
        cal.set(attendance.year, attendance.month - 1, attendance.day)
        val formatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SimpleDateFormat("dd/MM/yy", Locale.ENGLISH)
        } else {
            java.text.SimpleDateFormat("dd/MM/yy", Locale.ENGLISH)
        }
        val databaseId = formatter.format(cal.time)

        val startedUpload = if (pendingList.any { it.databaseId == databaseId }) {
            pendingList.first { it.databaseId == databaseId }.startedUpload
        } else {
            false
        }

        val pendingAttendance = PendingAttendance(
            databaseId,
            attendance,
            false,
            isDeleting = true,
            startedUpload = startedUpload,
            allUsers = ArrayList(allUsersList.map { a -> a.userId }),
            attendedUsers = ArrayList()
        )

        val builder: AlertDialog.Builder = AlertDialog.Builder(activity as MainActivity)
        builder.setCancelable(true)
        builder.setTitle(R.string.delete_attendance)
        builder.setMessage(R.string.delete_attendance_confirmation)
        builder.setPositiveButton(R.string.yes) { _, _ ->

            if (!startedUpload) {
                if (pendingList.any { !it.isDeleting }) {
                    pendingAttViewModel.deleteAttendanceById(databaseId)
                    Toast.makeText(
                        requireContext(), R.string.attendance_was_deleted, Toast.LENGTH_SHORT
                    ).show()
                } else {
                    pendingAttViewModel.addAttendance(pendingAttendance)
                    Toast.makeText(requireContext(), R.string.deleting_att, Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                pendingAttViewModel.addAttendance(pendingAttendance)
                Toast.makeText(requireContext(), R.string.deleting_att, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        builder.setNegativeButton(R.string.no) { _, _ -> }
        builder.show()
    }
}