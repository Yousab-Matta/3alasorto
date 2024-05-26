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
    private val currentUserId: String,
    private val goToVotesFragment: (String, ArrayList<UserSelection>?) -> Unit,
    private var showComments: (String) -> Unit,
    private var showPostControls: (Post) -> Unit,
    private var pollItemSelect: (Post, String) -> Unit,
    private var enlargeMedia: (ArrayList<MediaData>?, Int, String) -> Unit,
    private var createReact: (UserSelection, String) -> Unit,
    private var deleteReact: (UserSelection, String) -> Unit,
    private var goToPostPage: (String) -> Unit,
    private var recyclerViewScrollState: (Boolean) -> Unit,

    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val currentUserPhone = FirebaseAuth.getInstance().currentUser!!.phoneNumber!!
    private val sdf = SimpleDateFormat("EEE, d, MMM, HH:mm:ss", Locale.ENGLISH)
    private val usersList = ArrayList<UserData>()
    private val reactsList = ArrayList<PostReact>()
    private val pollDataList = ArrayList<PollItemData>()

    companion object {
        private const val ITEM_POST = 0
        private const val ITEM_POLL = 1
    }

    fun addReactToList(newReact: PostReact) {
        if (reactsList.any { it.postId == newReact.postId }) {
            reactsList.removeAll { it.postId == newReact.postId }
        }
        reactsList.add(newReact)
    }

    fun removeReactFromList(newReact: PostReact) {
        reactsList.remove(newReact)
    }

    fun updateUsersList(userData: UserData) {
        if (!usersList.any { it.userId == userData.userId }) {
            usersList.add(userData)
        }

        notifyItemRangeChanged(0, itemCount)
    }

    fun updatePollData(newPollData: PollItemData) {
        if (pollDataList.any { it.postId == newPollData.postId }) {
            pollDataList.removeAll { it.postId == newPollData.postId }
        }
        pollDataList.add(newPollData)

        notifyItemChanged(postsList.indexOfFirst { it.postId == newPollData.postId })
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

        if (getItemViewType(position) == ITEM_POST) {
            (holder as PostViewHolder).bind(post)
        } else {
            (holder as PollViewHolder).bind(post)
        }
    }

    override fun getItemCount(): Int {
        return postsList.size
    }

    fun getPostOwner(ownerID: String): UserData? {
        return usersList.firstOrNull { it.userId == ownerID }
    }

    @SuppressLint("ClickableViewAccessibility")
    class PostViewHolder(
        itemView: View,
        adapter: PostsAdapter
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val mAdapter = adapter

        private val mediaLayout: ConstraintLayout = itemView.findViewById(R.id.layout_media)
        private val descTV: MentionTextView = itemView.findViewById(R.id.tv_post_desc)
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
            dateTV.setOnClickListener(this)
        }

        fun bind(post: Post) {

            currentPost = post

            if (post.mediaList != null && post.mediaList!!.size > 0) {
                mediaLayout.visibility = VISIBLE

                postMediaLayout.setView(
                    post.postId,
                    post.mediaList!!,
                    mediaLayout,
                    mAdapter.enlargeMedia
                )
            } else {
                mediaLayout.visibility = GONE
            }


            //Create a list of user data to use in mention text view
            val mentionedUsersDataList = ArrayList<UserData>()

            if (post.mentionsList.isNotEmpty()) {
                for (userId in post.mentionsList) {
                    if (mAdapter.usersList.any { it.userId == userId } && !mentionedUsersDataList.any { it.userId == userId }) {
                        mentionedUsersDataList.add(mAdapter.usersList.first { it.userId == userId })
                    }
                }
            }

            if (post.description.isNotEmpty()) {
                descTV.visibility = VISIBLE
                descTV.setDescription(
                    post.description, post.textWithTags,
                    post.mentionsList, mentionedUsersDataList
                )
            } else {
                descTV.visibility = GONE
            }

            reactsTV.setArgs(
                post.postId,
                mAdapter.currentUserId,
                mAdapter.createReact,
                mAdapter.deleteReact,
                mAdapter.recyclerViewScrollState
            )

            if (post.ownerID != mAdapter.currentUserId) {
                postMenuIV.visibility = GONE
            } else {
                postMenuIV.visibility = VISIBLE
            }

            val date = post.postDate
            dateTV.text = mAdapter.sdf.format(date!!)

            val owner: UserData? = mAdapter.getPostOwner(post.ownerID)
            if (owner != null) {
                nameTV.text = owner.name

                Glide.with(ownerIV).load(owner.imageLink).placeholder(R.drawable.image_logo)
                    .into(ownerIV)
            }

            updateReact()
        }

        private fun updateReact() {
            if (currentPost != null) {
                val react = if (mAdapter.reactsList.any { it.postId == currentPost!!.postId }) {
                    val postReacts =
                        mAdapter.reactsList.first { it.postId == currentPost!!.postId }
                    postReacts.reacts.firstOrNull { it.userId == mAdapter.currentUserId }
                } else {
                    null
                }

                if (react != null) {
                    reactsTV.setReact(react.userChoice)
                } else {
                    reactsTV.setReact("")
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

                    dateTV -> {
                        mAdapter.goToPostPage(currentPost!!.postId)
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
        private val descTV: MentionTextView = itemView.findViewById(R.id.tv_post_desc)
        private val dateTV: TextView = itemView.findViewById(R.id.tv_post_date)
        private val nameTV: TextView = itemView.findViewById(R.id.tv_post_owner)
        private val ownerIV: ImageView = itemView.findViewById(R.id.iv_post_owner)
        private val postMenuIV: ImageView = itemView.findViewById(R.id.iv_post_menu)
        private val reactsTV: ReactsButton = itemView.findViewById(R.id.tv_post_react)
        private val commentTV: TextView = itemView.findViewById(R.id.tv_post_comment)
        private val postIV: ImageView = itemView.findViewById(R.id.iv_post_image)

        private val mAdapter = adapter //Posts Adapter

        private var currentPost: Post? = null

        init {
            commentTV.setOnClickListener(this)
            postMenuIV.setOnClickListener(this)
            dateTV.setOnClickListener(this)
        }

        fun bind(post: Post) {

            currentPost = post

            val pollDataList =
                mAdapter.pollDataList.firstOrNull { it.postId == post.postId }?.pollItemData

            pollRV.setAdapter(
                mAdapter.usersList,
                mAdapter.goToVotesFragment,
                pollDataList,
                post,
                mAdapter.pollItemSelect,
                mAdapter.enlargeMedia
            )

            reactsTV.setArgs(
                post.postId,
                mAdapter.currentUserId,
                mAdapter.createReact,
                mAdapter.deleteReact,
                mAdapter.recyclerViewScrollState
            )

            //Create a list of user data to use in mention text view
            val mentionedUsersDataList = ArrayList<UserData>()

            if (!post.mediaList.isNullOrEmpty() && !post.mediaList!![0].mediaLink.isNullOrEmpty()) {
                Glide.with(postIV).load(post.mediaList!![0].mediaLink).into(postIV)
            }

            if (post.mentionsList.isNotEmpty()) {
                for (userId in post.mentionsList) {
                    if (mAdapter.usersList.any { it.userId == userId } && !mentionedUsersDataList.any { it.userId == userId }) {
                        mentionedUsersDataList.add(mAdapter.usersList.first { it.userId == userId })
                    }
                }
            }

            if (post.description.isNotEmpty()) {
                descTV.visibility = VISIBLE
                if (post.mentionsList.isNotEmpty()) {
                    descTV.setDescription(
                        post.description,
                        post.textWithTags,
                        post.mentionsList,
                        mentionedUsersDataList
                    )
                } else {
                    descTV.text = post.description
                }
            } else {
                descTV.visibility = GONE
            }

            if (post.ownerID != mAdapter.currentUserId) {
                postMenuIV.visibility = GONE
            } else {
                postMenuIV.visibility = VISIBLE
            }

            val date = post.postDate
            dateTV.text = mAdapter.sdf.format(date!!)

            val owner: UserData? = mAdapter.getPostOwner(post.ownerID)
            if (owner != null) {
                nameTV.text = owner.name

                Glide.with(ownerIV).load(owner.imageLink).placeholder(R.drawable.image_logo)
                    .into(ownerIV)
            }
            updateReact()

        }

        private fun updateReact() {
            if (currentPost != null) {
                val react = if (mAdapter.reactsList.any { it.postId == currentPost!!.postId }) {
                    val postReacts =
                        mAdapter.reactsList.first { it.postId == currentPost!!.postId }
                    postReacts.reacts.firstOrNull { it.userId == mAdapter.currentUserId }
                } else {
                    null
                }

                if (react != null) {
                    reactsTV.setReact(react.userChoice)
                } else {
                    reactsTV.setReact("")
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
                    dateTV -> {
                        mAdapter.goToPostPage(currentPost!!.postId)
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