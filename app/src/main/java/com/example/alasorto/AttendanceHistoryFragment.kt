package com.example.alasorto

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alasorto.adapters.AttHistoryAdapter
import com.example.alasorto.adapters.GroupOfUsersAdapter
import com.example.alasorto.dataClass.Attendance
import com.example.alasorto.dataClass.Users
import com.example.alasorto.utils.InternetCheck
import com.example.alasorto.utils.LinearSpacingItemDecorator
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat

class AttendanceHistoryFragment : Fragment(), AttHistoryAdapter.OnClickListener {
    private lateinit var attHistoryRV: RecyclerView
    private lateinit var attUsersRV: RecyclerView
    private lateinit var newAttBtn: ImageButton
    private lateinit var headerTV: TextView
    private lateinit var viewModel: AppViewModel
    private lateinit var attHistoryAdapter: AttHistoryAdapter
    private lateinit var groupOfUsersAdapter: GroupOfUsersAdapter
    private lateinit var internetCheck: InternetCheck
    private var hasConnection = false
    private var position: Int = -1

    @SuppressLint("SimpleDateFormat")
    private val sdf = SimpleDateFormat("EEE, d, MMM, HH:mm:ss")
    private val attendanceList = ArrayList<Attendance>()
    private val allUsersList = ArrayList<Users>()
    private val usersList = ArrayList<Users>()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        allUsersList.addAll((activity as MainActivity).getAllUsers())

        //Initialize View and Widgets
        val view = inflater.inflate(R.layout.fragment_attendance_history, container, false)
        view.isClickable = true

        attHistoryRV = view.findViewById(R.id.rv_attendance_history)
        attUsersRV = view.findViewById(R.id.rv_attendance_history_users)
        newAttBtn = view.findViewById(R.id.btn_create_data)
        headerTV = view.findViewById(R.id.tv_att_desc)

        //Set Linear Layout to RVs'
        attHistoryRV.layoutManager = LinearLayoutManager(context)
        attUsersRV.layoutManager = LinearLayoutManager(context)

        //Set RVs' spacings
        attHistoryRV.addItemDecoration(LinearSpacingItemDecorator(30))
        attUsersRV.addItemDecoration(LinearSpacingItemDecorator(30))

        //Set History Users RV adapter
        groupOfUsersAdapter = GroupOfUsersAdapter(usersList)
        attUsersRV.adapter = groupOfUsersAdapter

        //Initialize View Model
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        viewModel.attendanceListMLD.observe(this.viewLifecycleOwner, Observer {
            attendanceList.clear()
            attendanceList.addAll(it)
            attHistoryAdapter = AttHistoryAdapter(attendanceList, this)
            attHistoryRV.adapter = attHistoryAdapter
        })

        //Go To create new Attendance Fragment
        newAttBtn.setOnClickListener(View.OnClickListener {
            val manager = requireActivity().supportFragmentManager
            val transaction = manager.beginTransaction()
            transaction.add(R.id.main_frame, CreateAttFragment())
            transaction.addToBackStack("CREATE_NEW_ATT_FRAGMENT")
            transaction.commit()
        })

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

        //Check changes in Attendance
        Firebase.firestore.collection("Attendance").addSnapshotListener { _, _ ->
            if (hasConnection) {
                viewModel.getAllAttendance()
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
            if (it) {
                viewModel.getAllAttendance()
            }
        }
    }

    override fun onClick(position: Int) {
        this.position = position
        switchRV(true)
    }

    private fun switchRV(showingUsers: Boolean) {
        val fadeIn: Animation = AlphaAnimation(0f, 1f)
        fadeIn.interpolator = DecelerateInterpolator()
        fadeIn.duration = 200
        val fadeOut: Animation = AlphaAnimation(1f, 0f)
        fadeOut.interpolator = AccelerateInterpolator()
        fadeOut.duration = 200
        if (showingUsers) {
            usersList.clear()
            val idsList = attendanceList[position].UsersIDs?.let { ArrayList<String>(it) }
            for (user in allUsersList) {
                if (idsList != null) {
                    if (idsList.contains(user.Phone.toString())) {
                        usersList.add(user)
                    }
                }
            }
            groupOfUsersAdapter.notifyItemRangeInserted(0, usersList.size)
            attUsersRV.visibility = View.VISIBLE
            attHistoryRV.visibility = View.INVISIBLE
            attUsersRV.startAnimation(fadeIn)
            attHistoryRV.startAnimation(fadeOut)
            headerTV.text = sdf.format(attendanceList[position].Date!!)
        } else {
            val size = usersList.size
            usersList.clear()
            groupOfUsersAdapter.notifyItemRangeRemoved(0, size)
            attUsersRV.visibility = View.INVISIBLE
            attHistoryRV.visibility = View.VISIBLE
            attUsersRV.startAnimation(fadeOut)
            attHistoryRV.startAnimation(fadeIn)
            headerTV.setText(R.string.att_history)
        }
    }
}