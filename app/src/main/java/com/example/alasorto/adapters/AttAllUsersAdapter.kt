package com.example.alasorto.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alasorto.R
import com.example.alasorto.dataClass.UserData

class AttAllUsersAdapter(
    usersList: ArrayList<UserData>,
    private val attendedUsersList: ArrayList<UserData>,
    private val onClickListener: OnClickListener,
    private val context: Context
) : RecyclerView.Adapter<AttAllUsersAdapter.ViewHolder>() {
    private var filteredUsersList = ArrayList<UserData>(usersList)
    private lateinit var mViewHolder: ViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return ViewHolder(view, this)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = filteredUsersList[position]

        if (attendedUsersList.contains(user)) {
            holder.userDataRl.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.green)
        } else {
            holder.userDataRl.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.primary_color)
        }

        holder.nameTV.text = user.name

        if (user.imageLink.isNotEmpty()) {
            Glide.with(holder.userIV).load(user.imageLink).into(holder.userIV)
        } else {
            Glide.with(holder.userIV).load(R.drawable.image_logo)
                .into(holder.userIV)
        }

    }

    override fun getItemCount(): Int {
        return filteredUsersList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filterList(filterList: ArrayList<UserData>) {
        filteredUsersList = filterList
        notifyDataSetChanged()
    }

    fun getFilteredList(): ArrayList<UserData> {
        return filteredUsersList
    }

    fun getAttendedList(): ArrayList<UserData> {
        return attendedUsersList
    }

    class ViewHolder(
        itemView: View,
        private val adapter: AttAllUsersAdapter
    ) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val userDataRl: ConstraintLayout = itemView.findViewById(R.id.cl_user_item)
        val nameTV: TextView = itemView.findViewById(R.id.tv_user_item_name)
        val userIV: ImageView = itemView.findViewById(R.id.iv_user_item)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            val attendedUsersList = adapter.getAttendedList()
            val selectedUser = adapter.getFilteredList()[layoutPosition]

            if (attendedUsersList.contains(selectedUser)) {
                adapter.onClickListener.onClick(
                    selectedUser,
                    layoutPosition
                )
            } else {
                adapter.onClickListener.onClick(
                    selectedUser,
                    layoutPosition
                )
            }
        }
    }

    interface OnClickListener {
        fun onClick(user: UserData, position: Int)
    }
}