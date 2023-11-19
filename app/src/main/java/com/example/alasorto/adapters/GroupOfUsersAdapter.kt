package com.example.alasorto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alasorto.R
import com.example.alasorto.dataClass.UserData

class GroupOfUsersAdapter(
    private val usersList: ArrayList<UserData>
) : RecyclerView.Adapter<GroupOfUsersAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = usersList[position]
        holder.nameTV.text = user.name
        Glide.with(holder.userIV).load(user.imageLink).into(holder.userIV)
        /* val formatter = DecimalFormat("#.##")
         val percent = formatter.format(user.AttendedPercent)
         Log.d(TAG, "onBindViewHolder: ${user.AttendedPercent}")
         holder.percentTV.text = percent.toString()*/
    }

    override fun getItemCount(): Int {
        return usersList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTV: TextView = itemView.findViewById(R.id.tv_user_item_name)
        val userIV: ImageView = itemView.findViewById(R.id.iv_user_item)
    }
}
