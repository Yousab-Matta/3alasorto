package com.example.alasorto.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alasorto.R
import com.example.alasorto.dataClass.Comments
import com.example.alasorto.dataClass.UserData
import com.example.alasorto.utils.CommentMediaLayout
import com.example.alasorto.utils.MentionTextView

class CommentsAdapter(
    private val commentsList: ArrayList<Comments>,
    private val usersList: ArrayList<UserData>,
    private val showControlsDialog: (Comments) -> Unit,
    private val enlargeMedia: (String) -> Unit,
    private val context: Context
) : RecyclerView.Adapter<CommentsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return ViewHolder(view, this)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = commentsList[position]
        if (comment.comment.isNotEmpty()) {

            //Create a list of user data to use in mention text view
            val mentionedUsersDataList = ArrayList<UserData>()

            if (comment.mentionsList.isNotEmpty()) {
                for (userId in comment.mentionsList) {
                    if (usersList.any { it.userId == userId } && !mentionedUsersDataList.any { it.userId == userId }) {
                        mentionedUsersDataList.add(usersList.first { it.userId == userId })
                    }
                }
            }

            holder.commentTV.setDescription(
                comment.comment,
                comment.textWithTags,
                comment.mentionsList,
                mentionedUsersDataList
            )

            holder.commentTV.visibility = VISIBLE
        }

        if (comment.media != null) {
            holder.mediaLayout.visibility = VISIBLE
            val params = holder.mediaLayout.layoutParams as ConstraintLayout.LayoutParams
            params.matchConstraintPercentWidth = .7f
            holder.mediaLayout.layoutParams = params
            holder.mediaLayout.requestLayout()
            holder.mediaLayoutController.setView(comment.media!!, holder.mediaLayout, enlargeMedia)
        }

        if (usersList.any { it.userId == comment.ownerId }) {
            val owner = usersList.first { it.userId == comment.ownerId }
            holder.userNameTV.text = owner.name
            if (owner.imageLink.isNotEmpty()) {
                Glide.with(holder.userImageIV).load(owner.imageLink).into(holder.userImageIV)
            }
        }
    }

    override fun getItemCount(): Int {
        return commentsList.size
    }

    class ViewHolder(itemView: View, adapter: CommentsAdapter) :
        RecyclerView.ViewHolder(itemView), View.OnLongClickListener {
        val userImageIV: ImageView = itemView.findViewById(R.id.iv_comment)
        val userNameTV: TextView = itemView.findViewById(R.id.tv_comment_owner)
        val commentTV: MentionTextView = itemView.findViewById(R.id.tv_comment)
        val mediaLayoutController = CommentMediaLayout(itemView.context)
        val mediaLayout: ConstraintLayout = itemView.findViewById(R.id.comment_media)

        private val mAdapter = adapter

        init {
            itemView.setOnLongClickListener(this)
        }

        override fun onLongClick(p0: View?): Boolean {
            mAdapter.showControlsDialog(mAdapter.commentsList[adapterPosition])
            return true
        }
    }
}