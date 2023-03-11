package com.example.alasorto.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alasorto.R
import com.example.alasorto.dataClass.Users

class AttAllUsersAdapter(
    usersList: ArrayList<Users>,
    private val attendedUsersList: ArrayList<Users>,
    private val onClickListener: OnClickListener,
    private val context: Context
) : RecyclerView.Adapter<AttAllUsersAdapter.ViewHolder>() {
    private var filteredUsersList = ArrayList<Users>(usersList)
    private lateinit var mViewHolder: ViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        val viewHolder = ViewHolder(view, this, onClickListener)
        mViewHolder = viewHolder
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d("ARRAY_SIZE", filteredUsersList.size.toString())
        val user = filteredUsersList[position]

        if (attendedUsersList.contains(user)) {
            holder.userDataRl.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.green)
        } else {
            holder.userDataRl.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.primary_color)
        }

        holder.nameTV.text = user.Name
        Glide.with(holder.userIV).load(user.ImageLink).into(holder.userIV)
    }

    override fun getItemCount(): Int {
        return filteredUsersList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filterList(filterList: ArrayList<Users>) {
        filteredUsersList = filterList
        notifyDataSetChanged()
    }

    fun getFilteredList(): ArrayList<Users> {
        return filteredUsersList
    }

    fun getAttendedList(): ArrayList<Users> {
        return attendedUsersList
    }

    class ViewHolder(
        itemView: View,
        private val adapter: AttAllUsersAdapter,
        private val onClickListener: OnClickListener
    ) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val userDataRl: RelativeLayout = itemView.findViewById(R.id.rl_user_item)
        val nameTV: TextView = itemView.findViewById(R.id.tv_user_item_name)
        val userIV: ImageView = itemView.findViewById(R.id.iv_user_item)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            val attendedUsersList = adapter.getAttendedList()
            val selectedUser = adapter.getFilteredList()[layoutPosition]

            if (attendedUsersList.contains(selectedUser)) {
                onClickListener.onClick(
                    selectedUser,
                    false,
                    layoutPosition
                )
            } else {
                onClickListener.onClick(
                    selectedUser,
                    true,
                    layoutPosition
                )
            }
        }
    }

    interface OnClickListener {
        fun onClick(user: Users, attend: Boolean, position: Int)
    }
}