package com.example.alasorto.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alasorto.R
import com.example.alasorto.dataClass.Users

class AttAllUsersAdapter(
    private val usersList: ArrayList<Users>
) : RecyclerView.Adapter<AttAllUsersAdapter.ViewHolder>() {
    var filteredUsersList: ArrayList<Users> = usersList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = filteredUsersList[position]
        holder.nameTV.text = user.Name
        Glide.with(holder.userIV).load(user.ImageLink).into(holder.userIV)
    }

    override fun getItemCount(): Int {
        return filteredUsersList.size
    }

    fun getUser(position: Int): Users {
        return filteredUsersList[position]
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filterList(filterList: ArrayList<Users>) {
        filteredUsersList = filterList
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTV: TextView = itemView.findViewById(R.id.tv_user_name_item)
        val userIV: ImageView = itemView.findViewById(R.id.iv_user_item)
    }
}