package com.example.alasorto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alasorto.R
import com.example.alasorto.dataClass.Comments
import com.example.alasorto.dataClass.Users

class CommentsAdapter(
    private val commentsList: ArrayList<Comments>,
    private val commentsOwnersList: ArrayList<Users>,
    private val onCommentClick: OnCommentClick

) : RecyclerView.Adapter<CommentsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return ViewHolder(view, onCommentClick, commentsList)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = commentsList[position]
        holder.commentTV.text = comment.comment
        for (owner in commentsOwnersList) {
            if (comment.ownerID == owner.Phone)
                if (owner.ImageLink!!.isNotEmpty()) {
                    Glide.with(holder.userImageIV).load(owner.ImageLink!!).into(holder.userImageIV)
                    holder.userNameTV.text = owner.Name
                }
        }
    }

    override fun getItemCount(): Int {
        return commentsList.size
    }

    class ViewHolder(
        itemView: View,
        private val onCommentClick: OnCommentClick,
        private val commentsList: ArrayList<Comments>
    ) :
        RecyclerView.ViewHolder(itemView), View.OnLongClickListener {
        val userImageIV: ImageView = itemView.findViewById(R.id.iv_comment)
        val userNameTV: TextView = itemView.findViewById(R.id.tv_comment_owner)
        val commentTV: TextView = itemView.findViewById(R.id.tv_comment)

        init {
            itemView.setOnLongClickListener(this)
        }

        override fun onLongClick(p0: View?): Boolean {
            onCommentClick.onClick(commentsList[adapterPosition])
            return true
        }
    }
}

interface OnCommentClick {
    fun onClick(comment: Comments)
}