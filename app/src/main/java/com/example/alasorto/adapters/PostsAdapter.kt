package com.example.alasorto.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alasorto.R
import com.example.alasorto.dataClass.*
import com.example.alasorto.utils.*
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class PostsAdapter(
    private val postsList: ArrayList<Post>,
    private var showComments: (String) -> Unit,
    private var showPostControls: (Post) -> Unit,
    private var pollItemSelect: (Post, String) -> Unit,
    private var enlargeMedia: (ArrayList<MediaData>?, Int) -> Unit,
    private var enlargeImage: (String) -> Unit,
    private var createReact: (UserSelection, String) -> Unit,
    private var deleteReact: (UserSelection, String) -> Unit

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser!!.phoneNumber!!
    private val sdf = SimpleDateFormat("EEE, d, MMM, HH:mm:ss", Locale.ENGLISH)
    private val handleReacts = HandleReacts()
    private val usersList = ArrayList<UserData>()
    private val reactsList = ArrayList<PostReact>()
    private val pollDataList = ArrayList<PollItemData>()

    companion object {
        private const val ITEM_POST = 0
        private const val ITEM_POLL = 1
    }

    fun updateReacts(newReactsList: PostReact) {
        val currentReactsListReplica = reactsList
        reactsList.clear()
        reactsList.addAll(
            handleReacts.handleReactsList(
                currentReactsListReplica,
                newReactsList
            )
        )
        notifyItemRangeChanged(0, itemCount)
    }

    fun updateUsersList(userData: UserData) {
        if (usersList.any { it.phone == userData.phone }) {
            usersList.remove(usersList.first { it.phone == userData.phone })
        }

        usersList.add(userData)

        val ownersPostsList = postsList.filter { it.ownerID == userData.phone }
        for (post in ownersPostsList) {
            notifyItemChanged(postsList.indexOf(post))
        }
    }

    fun updatePollData(newPollData: PollItemData) {
        val index = pollDataList.indexOfFirst { it1 -> it1.postId == newPollData.postId }
        if (pollDataList.any { it2 -> it2.postId == newPollData.postId }) {
            pollDataList[index] = newPollData
            notifyItemRangeChanged(0, itemCount)
        } else {
            pollDataList.add(newPollData)
            notifyItemRangeChanged(0, itemCount)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (postsList[position].postType == "Post") {
            ITEM_POST
        } else {
            ITEM_POLL
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_POST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_post, parent, false)
                PostViewHolder(view, this)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_poll_post, parent, false)
                PollViewHolder(view, this)
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val post = postsList[position]

        val react = if (reactsList.any { it.postId == post.postId }) {
            val postReacts = reactsList.first { it.postId == post.postId }
            postReacts.reacts.firstOrNull { it.userId == currentUserId }
        } else {
            null
        }

        if (getItemViewType(position) == ITEM_POST) {
            (holder as PostViewHolder).bind(post, react)
        } else {
            (holder as PollViewHolder).bind(post, react)
        }
    }

    override fun getItemCount(): Int {
        return postsList.size
    }

    fun getPostOwner(ownerID: String): UserData? {
        return usersList.firstOrNull { it.phone == ownerID }
    }

    @SuppressLint("ClickableViewAccessibility")
    class PostViewHolder(
        itemView: View,
        adapter: PostsAdapter
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val mAdapter = adapter

        private val mediaLayout: ConstraintLayout = itemView.findViewById(R.id.layout_media)
        private val descTV: TextView = itemView.findViewById(R.id.tv_post_desc)
        private val dateTV: TextView = itemView.findViewById(R.id.tv_post_date)
        private val nameTV: TextView = itemView.findViewById(R.id.tv_post_owner)
        private val ownerIV: ImageView = itemView.findViewById(R.id.iv_post_owner)
        private val commentTV: TextView = itemView.findViewById(R.id.tv_post_comment)
        private val reactsTV: ReactsButton = itemView.findViewById(R.id.tv_post_react)
        private val postMenuIV: ImageView = itemView.findViewById(R.id.iv_post_menu)
        private val postMediaLayout = PostMediaLayout(itemView.context)

        private var currentPost: Post? = null

        init {
            commentTV.setOnClickListener(this)
            postMenuIV.setOnClickListener(this)
        }

        fun bind(post: Post, react: UserSelection?) {
            if (post.ownerID != mAdapter.currentUserId) {
                postMenuIV.visibility = GONE
            } else {
                postMenuIV.visibility = VISIBLE
            }

            if (post.mediaList != null && post.mediaList!!.size > 0) {
                mediaLayout.visibility = VISIBLE
                postMediaLayout.setView(post.mediaList!!, mediaLayout, mAdapter.enlargeMedia)
            } else {
                mediaLayout.visibility = GONE
            }

            if (post.description != null && post.description.isNotEmpty()) {
                descTV.visibility = VISIBLE
                descTV.text = post.description
            } else {
                descTV.visibility = GONE
            }

            reactsTV.setArgs(
                post.postId,
                mAdapter.createReact,
                mAdapter.deleteReact
            )

            if (react != null) {
                reactsTV.setReact(react.userChoice)
            }

            val date = post.postDate
            dateTV.text = mAdapter.sdf.format(date!!)

            val owner: UserData? = mAdapter.getPostOwner(post.ownerID)
            if (owner != null) {
                nameTV.text = owner.name

                if (owner.imageLink.isNotEmpty()) {
                    Glide.with(ownerIV).load(owner.imageLink).into(ownerIV)
                }
            }
        }

        override fun onClick(p0: View?) {
            if (currentPost != null) {
                when (p0) {
                    //When user click on comments TV interface sends post ID and opens comments fragment from user fragment
                    commentTV -> {
                        mAdapter.showComments(currentPost!!.postId)
                    }

                    //When user click on menu IV interface sends post item and opens settings from user fragment
                    postMenuIV -> {
                        mAdapter.showPostControls(currentPost!!)
                    }
                }
            }
        }
    }

    class PollViewHolder(
        itemView: View,
        adapter: PostsAdapter,
    ) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {

        private val pollRV: PollRecyclerView = itemView.findViewById(R.id.rv_poll_list)
        private val descTV: TextView = itemView.findViewById(R.id.tv_post_desc)
        private val dateTV: TextView = itemView.findViewById(R.id.tv_post_date)
        private val nameTV: TextView = itemView.findViewById(R.id.tv_post_owner)
        private val ownerIV: ImageView = itemView.findViewById(R.id.iv_post_owner)
        private val postMenuIV: ImageView = itemView.findViewById(R.id.iv_post_menu)
        private val reactsTV: ReactsButton = itemView.findViewById(R.id.tv_post_react)
        private val commentTV: TextView = itemView.findViewById(R.id.tv_post_comment)
        private val postIV: ImageView = itemView.findViewById(R.id.iv_post_image)

        private val mAdapter = adapter //Posts Adapter

        private lateinit var pollItemsAdapter: PollPostAdapter
        private var currentPost: Post? = null

        init {
            commentTV.setOnClickListener(this)
            postMenuIV.setOnClickListener(this)
        }

        fun bind(post: Post, react: UserSelection?) {
            if (currentPost == null || currentPost != post) {
                currentPost = post

                if (post.ownerID != mAdapter.currentUserId) {
                    postMenuIV.visibility = GONE
                } else {
                    postMenuIV.visibility = VISIBLE
                }

                reactsTV.setArgs(
                    post.postId,
                    mAdapter.createReact,
                    mAdapter.deleteReact
                )

                val pollDataList =
                    mAdapter.pollDataList.firstOrNull { it.postId == post.postId }?.pollItemData

                pollRV.setAdapter(
                    mAdapter.usersList,
                    pollDataList,
                    post,
                    mAdapter.pollItemSelect,
                    mAdapter.enlargeImage
                )

                if (react != null) {
                    reactsTV.text = react.userChoice
                }
                descTV.text = post.description
                val date = post.postDate
                dateTV.text = mAdapter.sdf.format(date!!)
                val owner: UserData? = mAdapter.getPostOwner(post.ownerID)
                if (owner != null) {
                    nameTV.text = owner.name

                    if (owner.imageLink.isNotEmpty()) {
                        Glide.with(ownerIV).load(owner.imageLink).into(ownerIV)
                    }
                }
            }
        }

        //View.OnClick for
        override fun onClick(p0: View?) {
            if (currentPost != null) {
                when (p0) {
                    //When user click on comments TV interface sends post ID and opens comments fragment from user fragment
                    commentTV -> {
                        mAdapter.showComments(currentPost!!.postId)
                    }

                    //When user click on menu IV interface sends post item and opens settings from user fragment
                    postMenuIV -> {
                        mAdapter.showPostControls(currentPost!!)
                    }
                }
            }
        }
    }
}