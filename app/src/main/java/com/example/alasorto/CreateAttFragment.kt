package com.example.alasorto

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alasorto.adapters.AttAllUsersAdapter
import com.example.alasorto.adapters.AttendedUsersAdapter
import com.example.alasorto.dataClass.Users
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class CreateAttFragment : Fragment() {
    private val auth = Firebase.auth
    private val authUser = auth.currentUser
    private val phoneNum = authUser?.phoneNumber

    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private lateinit var switchRVBtn: ImageButton
    private lateinit var saveAttendanceBtn: ImageButton
    private lateinit var shownListTV: TextView

    //RV that shows all users
    private lateinit var usersRV: RecyclerView

    //RV that shows attended users
    private lateinit var attendedUsersRV: RecyclerView
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
    private var attUsersFilteredList = ArrayList<Users>()
    private var totalAtt = -1
    private val attUsersAdapter = AttendedUsersAdapter(attUsersList)
    private var isSearchViewEmpty = true

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        //Initialize view
        val view = inflater.inflate(R.layout.fragment_create_att, container, false)

        //Initialize widgets
        searchView = view.findViewById(R.id.searchView_attendance)
        switchRVBtn = view.findViewById(R.id.btn_switch_rv)
        saveAttendanceBtn = view.findViewById(R.id.btn_save_attendance)
        shownListTV = view.findViewById(R.id.tv_shown_rv)
        usersRV = view.findViewById(R.id.rv_att_all_users)
        attendedUsersRV = view.findViewById(R.id.rv_attended_users)

        switchRVBtn.isActivated = false

        //Create Alert Dialogue
        builder = AlertDialog.Builder(context)
        builder.setCancelable(false)
        builder.setView(R.layout.progress_dialogue)
        dialog = builder.create()
        window = dialog.window!!
        window.setBackgroundDrawableResource(android.R.color.transparent)

        usersRV.layoutManager = LinearLayoutManager(context)
        ItemTouchHelper(allUsersTouchHelper).attachToRecyclerView(usersRV)

        attendedUsersRV.layoutManager = LinearLayoutManager(context)
        ItemTouchHelper(attendedUsersTouchHelper).attachToRecyclerView(attendedUsersRV)
        attendedUsersRV.adapter = attUsersAdapter

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

        viewModel.usersMLD.observe(this.viewLifecycleOwner, Observer { users ->
            allUsersList = users
            allUsersList.sortBy { it.Name }
            for (user in users) {
                allUsersIds.add(user.Phone.toString())
            }
            attAllUsersAdapter = AttAllUsersAdapter(allUsersList)
            usersRV.adapter = attAllUsersAdapter
        })


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
                requireActivity().supportFragmentManager.popBackStack()
            }
        })

        switchRVBtn.setOnClickListener(
            View.OnClickListener
            {
                if (usersRV.visibility == View.VISIBLE) {
                    switchRV(false)
                } else {
                    switchRV(true)
                }
            })

        saveAttendanceBtn.setOnClickListener(View.OnClickListener
        {
            if (hasConnection) {
                dialog.show()
                attendedUsersIds.clear()
                for (user in attUsersList) {
                    attendedUsersIds.add(user.Phone!!)
                    attendedUsersNames.add(user.Name!!)
                    //requireActivity().createNotification(user.Token!!)
                }
                viewModel.createAttendance(attendedUsersIds, attendedUsersNames)
                viewModel.getCurrentUser(phoneNum!!)
                //requireActivity().createNotification(viewModel.currentUserMLD.value!!.Token.toString())
                saveAttendanceBtn.isClickable = false
            }
        })

        searchView.setOnQueryTextListener(
            object :
                androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(p0: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(p0: String?): Boolean {
                    isSearchViewEmpty = p0!!.isEmpty()
                    filterAllUsers(p0)
                    return false
                }
            })

        return view
    }

    /*Switch between (All users RV) and (Attended Users RV)
    and include fade in and fade out animations*/
    private fun switchRV(showingAllUsers: Boolean) {
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.interpolator = DecelerateInterpolator()
        fadeIn.startOffset = 200
        fadeIn.duration = 200

        val fadeOut: Animation = AlphaAnimation(1f, 0f)
        fadeOut.interpolator = AccelerateInterpolator()
        fadeOut.startOffset = 200
        fadeOut.duration = 200

        if (showingAllUsers) {
            saveAttendanceBtn.animation = fadeOut
            saveAttendanceBtn.visibility = View.INVISIBLE
            usersRV.visibility = View.VISIBLE
            usersRV.startAnimation(fadeIn)
            attendedUsersRV.visibility = View.INVISIBLE
            attendedUsersRV.startAnimation(fadeOut)
            shownListTV.startAnimation(fadeOut)
            shownListTV.setText(R.string.all_users)
            shownListTV.startAnimation(fadeIn)
            switchRVBtn.animate().rotation(0f).setDuration(200).start()
            switchRVBtn.isActivated = false
        } else {
            saveAttendanceBtn.animation = fadeIn
            saveAttendanceBtn.visibility = View.VISIBLE
            usersRV.visibility = View.INVISIBLE
            usersRV.startAnimation(fadeOut)
            attendedUsersRV.visibility = View.VISIBLE
            attendedUsersRV.startAnimation(fadeIn)
            shownListTV.startAnimation(fadeOut)
            shownListTV.setText(R.string.attended_users)
            shownListTV.startAnimation(fadeIn)
            switchRVBtn.animate().rotation(360f).setDuration(200).start()
            switchRVBtn.isActivated = true
        }
    }

    private val allUsersTouchHelper: ItemTouchHelper.SimpleCallback =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val user = attAllUsersAdapter.getUser(viewHolder.adapterPosition)
                attend(user)
            }
        }

    private val attendedUsersTouchHelper: ItemTouchHelper.SimpleCallback =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val user = attUsersAdapter.getUser(viewHolder.adapterPosition)
                absent(user)
            }
        }

    /*
    * The explanation here goes the same for the Absent function
    * I have created 2 ArrayList<User> one for all users filtered results and the other for attended users
    * so if search view is empty we do attend/absent as usual remove/add to/from allUsers/AttendedUsers lists
    * but in search case if the search view isn't empty the following will happen
    * in this case (attend) we check if the allUsers filtered list has the User item we pass
    * then we get its index and remove it from all users list and add to attended users ist
    * after this is done i remove it from all users filtered list and add to attended users filtered list :D
    */
    @SuppressLint("NotifyDataSetChanged")
    private fun attend(user: Users) {
        if (isSearchViewEmpty) {
            attUsersList.add(user)
            attUsersList.sortBy { it.Name }
            allUsersList.remove(user)
            allUsersList.sortBy { it.Name }
            attUsersAdapter.filterList(attUsersList)
            attAllUsersAdapter.filterList(allUsersList)
        } else {
            if (allUsersFilteredList.contains(user)) {
                attUsersFilteredList.add(user)
                attUsersFilteredList.sortBy { it.Name }
                allUsersFilteredList.remove(user)
                allUsersFilteredList.sortBy { it.Name }
                attUsersList.add(user)
                allUsersList.remove(user)
                attUsersAdapter.filterList(attUsersFilteredList)
                attAllUsersAdapter.filterList(allUsersFilteredList)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun absent(user: Users) {
        if (isSearchViewEmpty) {
            attUsersList.remove(user)
            attUsersList.sortBy { it.Name }
            allUsersList.add(user)
            allUsersList.sortBy { it.Name }
            attAllUsersAdapter.filterList(allUsersList)
            attUsersAdapter.filterList(attUsersList)
        } else {
            if (attUsersFilteredList.contains(user)) {
                attUsersFilteredList.remove(user)
                attUsersFilteredList.sortBy { it.Name }
                allUsersFilteredList.add(user)
                allUsersFilteredList.sortBy { it.Name }
                attUsersList.remove(user)
                allUsersList.add(user)
                attUsersAdapter.filterList(attUsersFilteredList)
                attAllUsersAdapter.filterList(allUsersFilteredList)
            }
        }
    }

    private fun filterAllUsers(text: String) {
        allUsersFilteredList.clear()
        attUsersFilteredList.clear()
        for (user in allUsersList) {
            if (user.Name!!.lowercase().contains(text.lowercase())) {
                allUsersFilteredList.add(user)
            }
        }
        attAllUsersAdapter.filterList(allUsersFilteredList)

        for (user in attUsersList) {
            if (user.Name!!.lowercase().contains(text.lowercase())) {
                attUsersFilteredList.add(user)
            }
        }
        attUsersAdapter.filterList(attUsersFilteredList)
    }

    /*private fun filterAttended(text: String) {
        allUsersFilteredList.clear()
        for (user in allUsersList) {
            if (user.Name!!.lowercase().contains(text.lowercase())) {
                attUsersFilteredList.add(user)
            }
        }
        if (attUsersFilteredList.isEmpty()) {
            attUsersAdapter.filterList(emptyList)
        } else {
            attUsersAdapter.filterList(allUsersFilteredList)
        }
    }*/
}