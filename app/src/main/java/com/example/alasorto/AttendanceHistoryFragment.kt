package com.example.alasorto

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
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

class AttendanceHistoryFragment : Fragment(R.layout.fragment_attendance_history) {
    private val pendingAttViewModel: PendingAttendanceViewModel by viewModels()
    private val viewModel: AppViewModel by viewModels()
    private val attendanceList = ArrayList<Attendance>()
    private val allUsersList = ArrayList<UserData>()
    private val usersList = ArrayList<UserData>()
    private val pendingAttendanceList = ArrayList<Attendance>() //List of (Attendance)
    private val pendingList = ArrayList<PendingAttendance>() //List of PenindingAttendance

    private lateinit var attHistoryRV: RecyclerView
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

    private var hasConnection = false
    private var deletedUserAttCount = 0 //Count users when deleting an attendance file
    private var totalAttendanceUserAttCount = 0 //Count users of selected attendance file

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        allUsersList.addAll((activity as MainActivity).getAllUsers())

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
            AttHistoryAdapter(attendanceList, ::goToAttendanceUsers, ::deleteAttendance)
        attHistoryRV.adapter = attHistoryAdapter

        pendingAttViewModel.readAllData.observe(this.viewLifecycleOwner, Observer {
            pendingList.clear()
            pendingList.addAll(it)

            pendingAttendanceList.clear()
            for (pendingAttendance in it) {
                pendingAttendanceList.add(pendingAttendance.attendance)
            }
            pendingAttendanceList.sortByDescending { it1 -> it1.date }
        })

        viewModel.attendanceListMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null && it.size > 0) {
                attendanceList.clear()
                attendanceList.addAll(it)
                Toast.makeText(requireContext(), it.size.toString(), Toast.LENGTH_SHORT).show()
                attHistoryAdapter.setAttendanceList(it)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.no_attendance_record),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        viewModel.attendanceItemMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                val index = attendanceList.indexOfFirst { it1 -> it1.id == it.id }
                attendanceList[index] = it
                attHistoryAdapter.notifyItemChanged(index)
            }
        })

        viewModel.deletedUserAttMLD.observe(this.viewLifecycleOwner, Observer {
            if (it) {
                deletedUserAttCount += 1

                if (deletedUserAttCount == totalAttendanceUserAttCount) {
                    deletedUserAttCount = 0
                    (activity as MainActivity).dismissLoadingDialog()

                    if (attendanceList.contains(deletedAttendance)) {
                        val index = attendanceList.indexOf(deletedAttendance)
                        attendanceList.remove(deletedAttendance)
                        attHistoryAdapter.notifyItemRemoved(index)
                    }
                }
            }
        })

        viewModel.deletedUserAttMLD.observe(this.viewLifecycleOwner, Observer {
            //ToDo: check user selection (all att or att by date), and read att from database depends on his selection to update attendance list if its deleted
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
            showDatePickedDialog()
        }

        getAllAttendanceBtn.setOnClickListener {
            if (hasConnection) {
                viewModel.getAllAttendance()
            } else {
                Toast.makeText(requireContext(), R.string.check_connection, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        pendingAttendanceBtn.setOnClickListener {
            attHistoryAdapter.setAttendanceList(pendingAttendanceList)
        }

        editUsersBtn.setOnClickListener {
            switchRV(false)

            val fragment = CreateAttFragment()
            val manager = requireActivity().supportFragmentManager
            val transaction = manager.beginTransaction()
            val bundle = Bundle()
            bundle.putParcelable("EDITING_ATTENDANCE", currentAttendance)
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
                    if (attUsersRV.visibility == View.VISIBLE) {
                        switchRV(false)
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

    private fun switchRV(showingUsers: Boolean) {
        val fadeIn: Animation = AlphaAnimation(0f, 1f)
        fadeIn.interpolator = DecelerateInterpolator()
        fadeIn.duration = 200
        fadeIn.startOffset = 200
        val fadeOut: Animation = AlphaAnimation(1f, 0f)
        fadeOut.interpolator = AccelerateInterpolator()
        fadeOut.duration = 200
        if (showingUsers) {
            usersList.clear()
            val idsList = currentAttendance.usersIDs?.let { ArrayList(it) }
            for (user in allUsersList) {
                if (idsList != null) {
                    if (idsList.contains(user.phone)) {
                        usersList.add(user)
                    }
                }
            }
            groupOfUsersAdapter.notifyItemRangeInserted(0, usersList.size)
            searchByDateBtn.visibility = View.GONE
            getAllAttendanceBtn.visibility = View.GONE
            pendingAttendanceBtn.visibility = View.GONE
            attHistoryRV.visibility = View.GONE
            attUsersRV.visibility = View.VISIBLE
            editUsersBtn.visibility = View.VISIBLE
            attHistoryRV.startAnimation(fadeOut)
            searchByDateBtn.startAnimation(fadeOut)
            getAllAttendanceBtn.startAnimation(fadeOut)
            attUsersRV.startAnimation(fadeIn)
            editUsersBtn.startAnimation(fadeIn)
            "${currentAttendance.day}/${currentAttendance.month}/${currentAttendance.year}"
                .also {
                    headerTV.text = it
                }
        } else {
            val size = usersList.size
            usersList.clear()
            groupOfUsersAdapter.notifyItemRangeRemoved(0, size)
            searchByDateBtn.visibility = View.VISIBLE
            attHistoryRV.visibility = View.VISIBLE
            getAllAttendanceBtn.visibility = View.VISIBLE
            pendingAttendanceBtn.visibility = View.VISIBLE
            attUsersRV.visibility = View.GONE
            editUsersBtn.visibility = View.GONE
            searchByDateBtn.startAnimation(fadeIn)
            attHistoryRV.startAnimation(fadeIn)
            getAllAttendanceBtn.startAnimation(fadeIn)
            attUsersRV.startAnimation(fadeOut)
            editUsersBtn.startAnimation(fadeOut)
            headerTV.setText(R.string.att_history)
        }
    }

    private fun goToAttendanceUsers(attendance: Attendance) {
        currentAttendance = attendance
        switchRV(true)
    }

    private fun deleteAttendance(attendance: Attendance) {
        deletedAttendance = attendance
        totalAttendanceUserAttCount = attendance.usersIDs!!.size
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity as MainActivity)
        builder.setCancelable(true)
        builder.setTitle(R.string.delete_attendance)
        builder.setMessage(R.string.delete_attendance_confirmation)
        builder.setPositiveButton(R.string.yes) { _, _ ->
            (activity as MainActivity).showLoadingDialog()
            if (attendanceList.contains(attendance)) {//Check if attendance is from online or offline database
                // If online database -> remove from online database
                if (hasConnection) {
                    viewModel.deleteAttendance(
                        attendance.id,
                        attendance.date!!,
                        attendance.usersIDs!!
                    )
                    val deletedItemIndex = attendanceList.indexOf(attendance)
                    attendanceList.remove(attendance)
                    attHistoryAdapter.notifyItemRemoved(deletedItemIndex)
                } else {
                    Toast.makeText(
                        requireContext(),
                        R.string.check_connection,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            } else if (pendingAttendanceList.contains(attendance)) {// If offline database -> remove from offline database
                val pendingAttendance = pendingList.first { it.attendance == attendance }
                pendingAttViewModel.deleteAttendance(pendingAttendance)
                (activity as MainActivity).dismissLoadingDialog()
            }
        }
        builder.setNegativeButton(R.string.no) { _, _ -> }
        builder.show()
    }
}