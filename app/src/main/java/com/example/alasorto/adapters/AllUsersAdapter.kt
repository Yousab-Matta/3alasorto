package com.example.alasorto.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alasorto.R
import com.example.alasorto.dataClass.UserData

class AllUsersAdapter(
    usersList: ArrayList<UserData>,
    private val access: ArrayList<String>,
    private val context: Context,
    private val goToUserProfile: (UserData) -> Unit,
    private val setSelectedUserCounterText: () -> Unit
) : RecyclerView.Adapter<AllUsersAdapter.ViewHolder>() {

    companion object {
        private const val ITEM_TYPE_USER_ITEM = 0
        private const val ITEM_TYPE_ADMIN_ITEM = 1
    }

    private val selectedUsersList = ArrayList<UserData>()

    private var filteredUsersList: ArrayList<UserData> = usersList
    private var isSelectionEnabled = false

    fun selectAllUsers() {
        selectedUsersList.clear()
        selectedUsersList.addAll(filteredUsersList)
        setSelectedUserCounterText()

        notifyItemRangeChanged(0, itemCount)
    }

    fun clearAllUsersSelection() {
        selectedUsersList.clear()
        notifyItemRangeChanged(0, itemCount)
        setSelectedUserCounterText()
        isSelectionEnabled = false
    }

    fun getSelectedUsersCount(): Int = selectedUsersList.size

    fun getSelectedUsers(): ArrayList<UserData> = selectedUsersList

    fun selectUser(userId: String) {
        isSelectionEnabled = true

        if (!selectedUsersList.any { it.phone == userId }) {
            selectedUsersList.add(filteredUsersList.first { it.phone == userId })
        } else {
            selectedUsersList.remove(filteredUsersList.first { it.phone == userId })
        }

        if (filteredUsersList.any { it.phone == userId }) {
            notifyItemChanged(filteredUsersList.indexOfFirst { it.phone == userId })
        }

        setSelectedUserCounterText()

        if (selectedUsersList.isEmpty()) {
            isSelectionEnabled = false
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (!access.contains("HANDLE_USERS")) {
            0
        } else {
            1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == ITEM_TYPE_USER_ITEM) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user, parent, false)
            ViewHolder(view, this)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_admin, parent, false)
            ViewHolder(view, this)
        }
    }

    @SuppressLint("UseCompatLoadingForColorStateLists")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = filteredUsersList[position]

        if (selectedUsersList.contains(user)) {
            holder.userDataRl.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.green)
        } else {
            holder.userDataRl.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.primary_color)
        }

        holder.nameTV.text = user.name

        val attendancePercent = if (user.attendedTimes != 0 && user.attendanceDue != 0) {
            ((user.attendedTimes.toFloat() / user.attendanceDue.toFloat()) * 100)
        } else {
            0f
        }

        "$attendancePercent %".also { holder.attendanceTV.text = it }
        "${user.points} Points".also { holder.pointsTV.text = it }

        if (user.imageLink.isNotEmpty()) {
            Glide.with(holder.userIV).load(user.imageLink)
                .into(holder.userIV)
        } else {
            Glide.with(holder.userIV).load(R.drawable.image_logo)
                .into(holder.userIV)
        }
    }

    override fun getItemCount(): Int {
        return filteredUsersList.size
    }

    fun getFilteredList(): ArrayList<UserData> {
        return filteredUsersList
    }

    class ViewHolder(
        itemView: View,
        private val adapter: AllUsersAdapter,
    ) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        val userDataRl: ConstraintLayout = itemView.findViewById(R.id.cl_user_item)
        val nameTV: TextView = itemView.findViewById(R.id.tv_user_item_name)
        val attendanceTV: TextView = itemView.findViewById(R.id.tv_user_percent)
        val pointsTV: TextView = itemView.findViewById(R.id.tv_user_points)
        val userIV: ImageView = itemView.findViewById(R.id.iv_user_item)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(p0: View?) {
            val selectedUser = adapter.getFilteredList()[layoutPosition]

            if (adapter.isSelectionEnabled) {
                adapter.selectUser(selectedUser.phone)
            } else {
                adapter.goToUserProfile(selectedUser)
            }
        }

        override fun onLongClick(p0: View?): Boolean {
            val selectedUser = adapter.getFilteredList()[layoutPosition]
            adapter.selectUser(selectedUser.phone)
            return true
        }
    }
}