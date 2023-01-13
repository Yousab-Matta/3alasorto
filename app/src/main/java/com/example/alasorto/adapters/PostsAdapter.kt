package com.example.alasorto.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alasorto.HomeFragment
import com.example.alasorto.R
import com.example.alasorto.dataClass.Posts
import com.example.alasorto.dataClass.Users
import java.text.SimpleDateFormat

class PostsAdapter(
    private val postsList: ArrayList<Posts>,
    private val postsOwnersList: ArrayList<Users>,
    private val onClickListener: OnClickListener,
    private val fragment: HomeFragment
) : RecyclerView.Adapter<PostsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return ViewHolder(view, postsList, postsOwnersList, onClickListener, fragment)
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
        for (owner in postsOwnersList) {
            if (owner.Phone.toString() == post.OwnerID.toString()) {
                holder.nameTV.text = owner.Name
                Glide.with(holder.ownerIV).load(owner.ImageLink).into(holder.ownerIV)
                break
            }
        }
    }

    override fun getItemCount(): Int {
        return postsList.size
    }

    class ViewHolder(
        itemView: View,
        postsList: ArrayList<Posts>,
        postsOwnersList: ArrayList<Users>,
        listener: OnClickListener,
        fragment: HomeFragment
    ) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val titleTV: TextView = itemView.findViewById(R.id.tv_post_title)
        val descTV: TextView = itemView.findViewById(R.id.tv_post_desc)
        val dateTV: TextView = itemView.findViewById(R.id.tv_post_date)
        val nameTV: TextView = itemView.findViewById(R.id.tv_post_owner)
        val postIV: ImageView = itemView.findViewById(R.id.iv_post_image)
        val ownerIV: ImageView = itemView.findViewById(R.id.iv_post_owner)
        private val postMenuIV: ImageView = itemView.findViewById(R.id.iv_post_menu)

        private val mFragment = fragment
        private val mListener = listener
        private val mPostsList = postsList
        private val mPostsOwnersList = postsOwnersList
        private val commentTV: TextView = itemView.findViewById(R.id.tv_post_comment)

        init {
            commentTV.setOnClickListener(this)
            postMenuIV.setOnClickListener(this)
            nameTV.setOnClickListener(this)
            ownerIV.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            if (p0 == commentTV) {
                mListener.onClick(mPostsList[adapterPosition])
                mFragment.goToComments()
            } else if (p0 == postMenuIV) {
                mListener.onClick(mPostsList[adapterPosition])
                mFragment.showDialog()
            } else if (p0 == nameTV || p0 == ownerIV) {
                for (owner in mPostsOwnersList) {
                    if (mPostsList[adapterPosition].OwnerID.toString() == owner.Phone.toString()) {
                        mFragment.goToProfileFragment(owner)
                        break
                    }
                }
            }
        }
    }

    interface OnClickListener {
        fun onClick(post: Posts)
    }
}