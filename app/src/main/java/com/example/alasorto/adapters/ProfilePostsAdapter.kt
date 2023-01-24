package com.example.alasorto.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alasorto.ProfileFragment
import com.example.alasorto.R
import com.example.alasorto.dataClass.Posts
import com.example.alasorto.dataClass.Users
import java.text.SimpleDateFormat

class ProfilePostsAdapter(
    private val postsList: ArrayList<Posts>,
    private val postOwner: Users,
    private val onClickListener: OnClickListener,
    private val profileFragment: ProfileFragment
) : RecyclerView.Adapter<ProfilePostsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return ViewHolder(view, postsList, postOwner, onClickListener, profileFragment)
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = postsList[position]
        holder.titleTV.text = post.Title
        holder.descTV.text = post.Description
        val sdf = SimpleDateFormat("EEE, d, MMM, HH:mm:ss")
        val date = post.PostDate
        holder.dateTV.text = sdf.format(date!!)
        Glide.with(holder.postIV).load(post.ImageLink).into(holder.postIV)
        holder.nameTV.text = postOwner.Name
        Glide.with(holder.ownerIV).load(postOwner.ImageLink).into(holder.ownerIV)
    }

    override fun getItemCount(): Int {
        return postsList.size
    }

    class ViewHolder(
        itemView: View,
        postsList: ArrayList<Posts>,
        postOwner: Users,
        listener: OnClickListener,
        profileFragment: ProfileFragment
    ) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val titleTV: TextView = itemView.findViewById(R.id.tv_post_title)
        val descTV: TextView = itemView.findViewById(R.id.tv_post_desc)
        val dateTV: TextView = itemView.findViewById(R.id.tv_post_date)
        val nameTV: TextView = itemView.findViewById(R.id.tv_post_owner)
        val postIV: ImageView = itemView.findViewById(R.id.iv_post_image)
        val ownerIV: ImageView = itemView.findViewById(R.id.iv_post_owner)
        private val postMenuIV: ImageView = itemView.findViewById(R.id.iv_post_menu)
        private val commentTV: TextView = itemView.findViewById(R.id.tv_post_comment)

        private val mListener = listener
        private val mPostsList = postsList
        private val mPostsOwnersList = postOwner
        private val mFragment = profileFragment

        init {
            commentTV.setOnClickListener(this)
            postMenuIV.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            if (p0 == commentTV) {
                mListener.onClick(mPostsList[adapterPosition])
                mFragment.showComments(mPostsList[adapterPosition].ID.toString())
            } else if (p0 == postMenuIV) {
                mListener.onClick(mPostsList[adapterPosition])
                mFragment.showSettingsDialog()
            }
        }
    }

    interface OnClickListener {
        fun onClick(post: Posts)
    }
}

