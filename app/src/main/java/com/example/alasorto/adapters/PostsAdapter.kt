package com.example.alasorto.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.example.alasorto.R
import com.example.alasorto.dataClass.*
import com.example.alasorto.utils.LinearSpacingItemDecorator
import com.example.alasorto.utils.VideoPlayer
import java.text.SimpleDateFormat
import java.util.*

class PostsAdapter(
    private val postsList: ArrayList<Any>,
    private val postsOwnersList: ArrayList<Users>,
    private var reactsList: ArrayList<PostReact>,
    private val onPostClickListener: OnPostClickListener,   //On post click (menu - user IV - user TV) instance from home fragment
    private val onPollItemClicked: PollPostAdapter.OnPollItemClicked, //On poll item click instance from home fragment
    private val currentUserId: String,
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val ITEM_POST = 0
        private const val ITEM_VIDEO_POST = 1
        private const val ITEM_POLL = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (postsList[position]) {
            is Posts -> {
                ITEM_POST
            }
            is VideoPost -> {
                ITEM_VIDEO_POST
            }
            else -> {
                ITEM_POLL
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_POST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_post, parent, false)
                PostViewHolder(view, onPostClickListener, this, context)
            }
            ITEM_VIDEO_POST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_video_post, parent, false)
                VideoPostViewHolder(view, onPostClickListener, this, context)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_poll_post, parent, false)
                PollViewHolder(view, this, onPostClickListener, onPollItemClicked, context)
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val post = postsList[position]
        val currentPostReacts = ArrayList<PostReact>()

        for (react in reactsList) {
            if (post is Posts) {
                if (react.postId.equals(post.id) && !currentPostReacts.contains(react)) {
                    currentPostReacts.add(react)
                }
            } else if (post is VideoPost) {
                if (react.postId.equals(post.id) && !currentPostReacts.contains(react)) {
                    currentPostReacts.add(react)
                }
            } else if (post is PollPost) {
                if (react.postId.equals(post.id) && !currentPostReacts.contains(react)) {
                    currentPostReacts.add(react)
                }
            }
        }

        if (getItemViewType(position) == ITEM_POST) {
            (holder as PostViewHolder).bind(
                post as Posts,
                currentPostReacts,
                currentUserId
            )
        } else if (getItemViewType(position) == ITEM_VIDEO_POST) {
            (holder as VideoPostViewHolder).bind(
                post as VideoPost,
                currentPostReacts,
                currentUserId
            )
        } else {
            (holder as PollViewHolder).bind(post as PollPost)
        }
    }

    override fun getItemCount(): Int {
        return postsList.size
    }

    fun getPostByPosition(position: Int): Any {
        return postsList[position]
    }

    fun getOwnerByPosition(position: Int): Users {
        return postsOwnersList[position]
    }

    fun getCertainPostOwner(ownerID: String): Users? {
        var user: Users? = null
        for (owner in postsOwnersList) {
            if (owner.Phone == ownerID) {
                user = owner
                break
            }
        }
        return user
    }

    fun stopPlayback() {

    }

    @SuppressLint("ClickableViewAccessibility")
    class PostViewHolder(
        itemView: View,
        listener: OnPostClickListener,
        adapter: PostsAdapter,
        context: Context
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        private val titleTV: TextView = itemView.findViewById(R.id.tv_post_title)
        private val postIV: ImageView = itemView.findViewById(R.id.iv_post_image)
        private val descTV: TextView = itemView.findViewById(R.id.tv_post_desc)
        private val dateTV: TextView = itemView.findViewById(R.id.tv_post_date)
        private val nameTV: TextView = itemView.findViewById(R.id.tv_post_owner)
        private val ownerIV: ImageView = itemView.findViewById(R.id.iv_post_owner)
        private val commentTV: TextView = itemView.findViewById(R.id.tv_post_comment)
        private val reactsTV: TextView = itemView.findViewById(R.id.tv_post_react)
        private val postMenuIV: ImageView = itemView.findViewById(R.id.iv_post_menu)

        private val mListener = listener
        private val mAdapter = adapter
        private val mContext = context
        private var ownerHasEmoji = false

        init {
            commentTV.setOnClickListener(this)
            reactsTV.setOnClickListener(this)
            postMenuIV.setOnClickListener(this)
            nameTV.setOnClickListener(this)
            ownerIV.setOnClickListener(this)

            reactsTV.text = mContext.getString(R.string.react)
            reactsTV.setTextColor(
                ContextCompat.getColor(mContext, R.color.text_color)
            )
        }

        fun bind(
            post: Posts,
            reactsList: ArrayList<PostReact>,
            currentUserId: String
        ) {
            for (react in reactsList) {
                if (react.reactOwner == currentUserId && !ownerHasEmoji) {
                    when (react.react) {
                        mContext.getString(R.string.emoji_1) -> {
                            reactsTV.text = mContext.getString(R.string.haha)
                            reactsTV.setTextColor(ContextCompat.getColor(mContext, R.color.yellow))
                        }
                        mContext.getString(R.string.emoji_2) -> {
                            reactsTV.text = mContext.getString(R.string.sad)
                            reactsTV.setTextColor(ContextCompat.getColor(mContext, R.color.yellow))
                        }
                        mContext.getString(R.string.emoji_3) -> {
                            reactsTV.text = mContext.getString(R.string.angry)
                            reactsTV.setTextColor(ContextCompat.getColor(mContext, R.color.orange))
                        }
                        mContext.getString(R.string.emoji_4) -> {
                            reactsTV.text = mContext.getString(R.string.wow)
                            reactsTV.setTextColor(ContextCompat.getColor(mContext, R.color.yellow))
                        }
                        mContext.getString(R.string.emoji_5) -> {
                            reactsTV.text = mContext.getString(R.string.love)
                            reactsTV.setTextColor(ContextCompat.getColor(mContext, R.color.red))
                        }
                    }
                    //Set owner has emoji = true
                    ownerHasEmoji = true
                } else {
                    break
                }
            }

            titleTV.text = post.title
            descTV.text = post.description
            val sdf = SimpleDateFormat("EEE, d, MMM, HH:mm:ss", Locale.ENGLISH)
            val date = post.postDate
            dateTV.text = sdf.format(date!!)
            Glide.with(postIV).load(post.imageLink).into(postIV)
            val owner: Users? = mAdapter.getCertainPostOwner(post.ownerID)
            if (owner != null) {
                nameTV.text = owner.Name
                Glide.with(ownerIV).load(owner.ImageLink).into(ownerIV)
            }
        }

        override fun onClick(p0: View?) {
            val post = mAdapter.getPostByPosition(adapterPosition)
            if (post is Posts) {
                when (p0) {
                    //When user click on comments TV interface sends post ID and opens comments fragment from user fragment
                    commentTV -> {
                        mListener.onCommentClick(post.id!!)
                    }
                    //When user click on menu IV interface sends post item and opens settings from user fragment
                    postMenuIV -> {
                        mListener.onPostClick(post)
                    }
                    //When user click on post owner IV or TV interface sends owner id and goes to that user profile
                    nameTV, ownerIV -> {
                        val owner: Users? = mAdapter.getCertainPostOwner(post.ownerID)
                        mListener.onPostOwnerClick(owner!!)
                    }
                    reactsTV -> {
                        val height = itemView.bottom - reactsTV.height
                        if (ownerHasEmoji) {
                            //Call fragment fun to remove react
                            mListener.onReactsClick(height, post, "REMOVE")
                            //Set reacts TV to its original color and state
                            reactsTV.text = mContext.getString(R.string.react)
                            reactsTV.setTextColor(
                                ContextCompat.getColor(mContext, R.color.text_color)
                            )
                            //Set owner has emoji to false
                            ownerHasEmoji = false
                        } else {
                            mListener.onReactsClick(height, post, "000")
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    class VideoPostViewHolder(
        itemView: View,
        listener: OnPostClickListener,
        adapter: PostsAdapter,
        context: Context
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        private val titleTV: TextView = itemView.findViewById(R.id.tv_post_title)
        private val postVideo: ConstraintLayout = itemView.findViewById(R.id.video_view_post)
        private val descTV: TextView = itemView.findViewById(R.id.tv_post_desc)
        private val dateTV: TextView = itemView.findViewById(R.id.tv_post_date)
        private val nameTV: TextView = itemView.findViewById(R.id.tv_post_owner)
        private val ownerIV: ImageView = itemView.findViewById(R.id.iv_post_owner)
        private val commentTV: TextView = itemView.findViewById(R.id.tv_post_comment)
        private val reactsTV: TextView = itemView.findViewById(R.id.tv_post_react)
        private val postMenuIV: ImageView = itemView.findViewById(R.id.iv_post_menu)

        private val mListener = listener
        private val mAdapter = adapter
        private val mContext = context
        private var ownerHasEmoji = false
        private val videoPlayer = VideoPlayer(itemView.context, postVideo)

        init {
            commentTV.setOnClickListener(this)
            reactsTV.setOnClickListener(this)
            postMenuIV.setOnClickListener(this)
            nameTV.setOnClickListener(this)
            ownerIV.setOnClickListener(this)

            reactsTV.text = mContext.getString(R.string.react)
            reactsTV.setTextColor(
                ContextCompat.getColor(mContext, R.color.text_color)
            )
        }

        fun bind(
            post: VideoPost,
            reactsList: ArrayList<PostReact>,
            currentUserId: String
        ) {
            for (react in reactsList) {
                if (react.reactOwner == currentUserId && !ownerHasEmoji) {
                    when (react.react) {
                        mContext.getString(R.string.emoji_1) -> {
                            reactsTV.text = mContext.getString(R.string.haha)
                            reactsTV.setTextColor(ContextCompat.getColor(mContext, R.color.yellow))
                        }
                        mContext.getString(R.string.emoji_2) -> {
                            reactsTV.text = mContext.getString(R.string.sad)
                            reactsTV.setTextColor(ContextCompat.getColor(mContext, R.color.yellow))
                        }
                        mContext.getString(R.string.emoji_3) -> {
                            reactsTV.text = mContext.getString(R.string.angry)
                            reactsTV.setTextColor(ContextCompat.getColor(mContext, R.color.orange))
                        }
                        mContext.getString(R.string.emoji_4) -> {
                            reactsTV.text = mContext.getString(R.string.wow)
                            reactsTV.setTextColor(ContextCompat.getColor(mContext, R.color.yellow))
                        }
                        mContext.getString(R.string.emoji_5) -> {
                            reactsTV.text = mContext.getString(R.string.love)
                            reactsTV.setTextColor(ContextCompat.getColor(mContext, R.color.red))
                        }
                    }
                    //Set owner has emoji = true
                    ownerHasEmoji = true
                } else {
                    break
                }
            }

            titleTV.text = post.title
            descTV.text = post.description
            val sdf = SimpleDateFormat("EEE, d, MMM, HH:mm:ss", Locale.ENGLISH)
            val date = post.postDate
            dateTV.text = sdf.format(date!!)
            videoPlayer.setVideoLink(post.videoLink)
            videoPlayer.setVideoParams(post.videoHeight)
            val owner: Users? = mAdapter.getCertainPostOwner(post.ownerID)
            if (owner != null) {
                nameTV.text = owner.Name
                Glide.with(ownerIV).load(owner.ImageLink).into(ownerIV)
            }
        }

        override fun onClick(p0: View?) {
            val post = mAdapter.getPostByPosition(adapterPosition)
            if (post is VideoPost) {
                when (p0) {
                    //When user click on comments TV interface sends post ID and opens comments fragment from user fragment
                    commentTV -> {
                        mListener.onCommentClick(post.id)
                    }
                    //When user click on menu IV interface sends post item and opens settings from user fragment
                    postMenuIV -> {
                        mListener.onPostClick(post)
                    }
                    //When user click on post owner IV or TV interface sends owner id and goes to that user profile
                    nameTV, ownerIV -> {
                        val owner: Users? = mAdapter.getCertainPostOwner(post.ownerID)
                        mListener.onPostOwnerClick(owner!!)
                    }
                    reactsTV -> {
                        val height = itemView.bottom - reactsTV.height
                        if (ownerHasEmoji) {
                            //Call fragment fun to remove react
                            mListener.onReactsClick(height, post, "REMOVE")
                            //Set reacts TV to its original color and state
                            reactsTV.text = mContext.getString(R.string.react)
                            reactsTV.setTextColor(
                                ContextCompat.getColor(mContext, R.color.text_color)
                            )
                            //Set owner has emoji to false
                            ownerHasEmoji = false
                        } else {
                            mListener.onReactsClick(height, post, "000")
                        }
                    }
                }
            }
        }
    }

    class PollViewHolder(
        itemView: View,
        adapter: PostsAdapter,
        listener: OnPostClickListener,
        onPollItemClicked: PollPostAdapter.OnPollItemClicked,
        context: Context
    ) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {

        private val pollRV: RecyclerView = itemView.findViewById(R.id.rv_poll_list)
        private val descTV: TextView = itemView.findViewById(R.id.tv_post_desc)
        private val dateTV: TextView = itemView.findViewById(R.id.tv_post_date)
        private val nameTV: TextView = itemView.findViewById(R.id.tv_post_owner)
        private val ownerIV: ImageView = itemView.findViewById(R.id.iv_post_owner)
        private val postMenuIV: ImageView = itemView.findViewById(R.id.iv_post_menu)
        private val reactsTV: TextView = itemView.findViewById(R.id.tv_post_react)

        private val mListener = listener //PostsAdapter OnClickListener
        private val mAdapter = adapter //Posts Adapter
        private val mPollItemListener = onPollItemClicked //PollItemsAdapter OnClickListener
        private val mContext = context
        private val mReactsList = adapter.reactsList
        private val mCurrentUserId = adapter.currentUserId
        private val commentTV: TextView = itemView.findViewById(R.id.tv_post_comment)

        private var selectedItem: Poll? = null

        private lateinit var pollItemsAdapter: PollPostAdapter
        private lateinit var post: PollPost
        private var ownerHasEmoji = false

        init {
            pollRV.addItemDecoration(LinearSpacingItemDecorator(20))
            pollRV.layoutManager = LinearLayoutManager(mContext)
            (pollRV.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            reactsTV.setOnClickListener(this)
            commentTV.setOnClickListener(this)
            postMenuIV.setOnClickListener(this)
            nameTV.setOnClickListener(this)
            ownerIV.setOnClickListener(this)

            reactsTV.text = mContext.getString(R.string.react)
            reactsTV.setTextColor(
                ContextCompat.getColor(mContext, R.color.text_color)
            )
        }

        fun bind(post: PollPost) {
            reactsTV.text = mContext.getText(R.string.react)

            for (react in mReactsList) {
                if (react.reactOwner == mCurrentUserId && !ownerHasEmoji) {
                    when (react.react) {
                        mContext.getString(R.string.emoji_1) -> {
                            reactsTV.text = mContext.getString(R.string.haha)
                            reactsTV.setTextColor(ContextCompat.getColor(mContext, R.color.yellow))
                        }
                        mContext.getString(R.string.emoji_2) -> {
                            reactsTV.text = mContext.getString(R.string.sad)
                            reactsTV.setTextColor(ContextCompat.getColor(mContext, R.color.yellow))
                        }
                        mContext.getString(R.string.emoji_3) -> {
                            reactsTV.text = mContext.getString(R.string.angry)
                            reactsTV.setTextColor(ContextCompat.getColor(mContext, R.color.orange))
                        }
                        mContext.getString(R.string.emoji_4) -> {
                            reactsTV.text = mContext.getString(R.string.wow)
                            reactsTV.setTextColor(ContextCompat.getColor(mContext, R.color.yellow))
                        }
                        mContext.getString(R.string.emoji_5) -> {
                            reactsTV.text = mContext.getString(R.string.love)
                            reactsTV.setTextColor(ContextCompat.getColor(mContext, R.color.red))
                        }
                    }
                    //Set owner has emoji = true
                    ownerHasEmoji = true
                } else {
                    break
                }
            }

            this.post = post
            pollItemsAdapter =
                PollPostAdapter(post.pollItems!!, mCurrentUserId, post, mPollItemListener)
            pollRV.adapter = pollItemsAdapter

            descTV.text = post.description
            val sdf = SimpleDateFormat("EEE, d, MMM, HH:mm:ss", Locale.ENGLISH)
            val date = post.postDate
            dateTV.text = sdf.format(date!!)
            val owner: Users? = mAdapter.getCertainPostOwner(post.ownerID!!)
            if (owner != null) {
                nameTV.text = owner.Name
                Glide.with(ownerIV).load(owner.ImageLink).into(ownerIV)
            }
        }

        //View.OnClick for
        override fun onClick(p0: View?) {
            val post = mAdapter.getPostByPosition(adapterPosition)
            if (post is PollPost) {
                when (p0) {
                    //When user click on comments TV interface sends post ID and opens comments fragment from user fragment
                    commentTV -> {
                        mListener.onCommentClick(post.id!!)
                    }
                    //When user click on menu IV interface sends post item and opens settings from user fragment
                    postMenuIV -> {
                        mListener.onPostClick(post)
                    }
                    //When user click on post owner IV or TV interface sends owner id and goes to that user profile
                    nameTV, ownerIV -> {
                        val owner: Users? = mAdapter.getCertainPostOwner(post.ownerID!!)
                        mListener.onPostOwnerClick(owner!!)
                    }
                    reactsTV -> {
                        val height = itemView.bottom - reactsTV.height
                        if (ownerHasEmoji) {
                            //Call fragment fun to remove react
                            mListener.onReactsClick(height, post, "REMOVE")
                            //Set reacts TV to its original color and state
                            reactsTV.text = mContext.getString(R.string.react)
                            reactsTV.setTextColor(
                                ContextCompat.getColor(mContext, R.color.text_color)
                            )
                            //Set owner has emoji to false
                            ownerHasEmoji = false
                        } else {
                            mListener.onReactsClick(height, post, "000")
                        }
                    }
                }
            }
        }
    }

    interface OnPostClickListener {
        fun onPostClick(post: Any)

        fun onCommentClick(postID: String)

        fun onReactsClick(height: Int, post: Any, case: String)

        fun onPostOwnerClick(postOwner: Users)
    }
}