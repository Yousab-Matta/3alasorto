package com.example.alasorto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alasorto.R
import com.example.alasorto.dataClass.Users

class AllUsersAdapter(
    private val usersList: ArrayList<Users>,
    private val onClickListener: OnClickListener,
) : RecyclerView.Adapter<AllUsersAdapter.ViewHolder>() {

    private var filteredUsersList: ArrayList<Users> = usersList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return ViewHolder(view, onClickListener, filteredUsersList)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = filteredUsersList[position]
        holder.nameTV.text = user.Name
        Glide.with(holder.userIV).load(user.ImageLink).into(holder.userIV)
    }

    override fun getItemCount(): Int {
        return filteredUsersList.size
    }

    class ViewHolder(
        itemView: View,
        private val listener: OnClickListener,
        usersList: ArrayList<Users>
    ) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val nameTV: TextView = itemView.findViewById(R.id.tv_user_name_item)
        val userIV: ImageView = itemView.findViewById(R.id.iv_user_item)
        private val mUsersList = usersList

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            listener.onClick(mUsersList[adapterPosition])
        }
    }

    interface OnClickListener {
        fun onClick(user: Users)
    }
}
