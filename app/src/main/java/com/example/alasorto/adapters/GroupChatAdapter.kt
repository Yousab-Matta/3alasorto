package com.example.alasorto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.alasorto.R
import com.example.alasorto.dataClass.GroupChat
import com.example.alasorto.dataClass.Users

class GroupChatAdapter(
    private val groupChatList: ArrayList<GroupChat>,
    private val chatOwnersList: ArrayList<Users>,
    private val myId: String
) : RecyclerView.Adapter<GroupChatAdapter.ViewHolder>() {

    companion object{
        private const val MSG_ITEM_LEFT = 0
        private const val MSG_ITEM_RIGHT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (groupChatList[position].ownerID == myId) {
            MSG_ITEM_RIGHT
        } else {
            MSG_ITEM_LEFT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == MSG_ITEM_LEFT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_group_chat_text, parent, false)
            ViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_group_chat_text, parent, false)
            view.layoutDirection = View.LAYOUT_DIRECTION_RTL

            ViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chatItem = groupChatList[position]
        holder.chatMessage.text = chatItem.message
        for (owner in chatOwnersList) {
            if (owner.Phone.toString() == chatItem.ownerID) {
                holder.chatOwner.text = owner.Name
            }
        }
    }

    override fun getItemCount(): Int {
        return groupChatList.size
    }

    fun getItemById(position: Int): GroupChat {
        return groupChatList[position]
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chatOwner: TextView = itemView.findViewById(R.id.tv_group_chat_name)
        val chatMessage: TextView = itemView.findViewById(R.id.tv_group_chat_message)
    }
}