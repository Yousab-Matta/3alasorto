package com.example.alasorto.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alasorto.R
import com.example.alasorto.dataClass.Users

private const val ITEM_TYPE_USER_ITEM = 0
private const val ITEM_TYPE_ADMIN_ITEM = 1

class AllUsersAdapter(
    usersList: ArrayList<Users>,
    private val selectedUsersList: ArrayList<Users>,
    private val onClickListener: OnClickListener,
    private val onLongClickListener: OnLongClickListener,
    private val access: String,
    private val context: Context,
) : RecyclerView.Adapter<AllUsersAdapter.ViewHolder>() {

    private var filteredUsersList: ArrayList<Users> = usersList

    override fun getItemViewType(position: Int): Int {
        return if (access == "User") {
            0
        } else {
            1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == ITEM_TYPE_USER_ITEM) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user, parent, false)
            ViewHolder(view, onClickListener, onLongClickListener, this)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_admin, parent, false)
            ViewHolder(view, onClickListener, onLongClickListener, this)
        }
    }

    @SuppressLint("UseCompatLoadingForColorStateLists")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = filteredUsersList[position]

        //context.resources.getColorStateList(R.color.green,context.theme)

        if (selectedUsersList.contains(user)) {
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.green)
            )
        } else {
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.grey5)
            )
        }

        holder.nameTV.text = user.Name
        "${user.AttendedPercent.toString()} %".also { holder.attendanceTV.text = it }
        "${user.Points.toString()} Points".also { holder.pointsTV.text = it }
        Glide.with(holder.userIV).load(user.ImageLink).into(holder.userIV)
    }

    override fun getItemCount(): Int {
        return filteredUsersList.size
    }

    fun getFilteredList(): ArrayList<Users> {
        return filteredUsersList
    }

    class ViewHolder(
        itemView: View,
        private val mClickListener: OnClickListener,
        private val mLongClickListener: OnLongClickListener,
        private val adapter: AllUsersAdapter,
    ) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        val cardView: CardView = itemView.findViewById(R.id.cv_user_item)
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
            mClickListener.onClick(selectedUser)
        }

        override fun onLongClick(p0: View?): Boolean {
            val selectedUser = adapter.getFilteredList()[layoutPosition]
            mLongClickListener.onLongClick(selectedUser)
            return true
        }
    }

    interface OnClickListener {
        fun onClick(user: Users)
    }

    interface OnLongClickListener {
        fun onLongClick(user: Users)
    }
}
