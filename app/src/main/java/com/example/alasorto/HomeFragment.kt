package com.example.alasorto

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.DisplayMetrics
import android.view.*
import android.view.View.OnClickListener
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.example.alasorto.adapters.PollPostAdapter
import com.example.alasorto.adapters.PostsAdapter
import com.example.alasorto.dataClass.*
import com.example.alasorto.utils.InternetCheck
import com.example.alasorto.utils.LinearSpacingItemDecorator
import com.example.alasorto.utils.UpdatePostsList
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Suppress("UNCHECKED_CAST")
class HomeFragment : Fragment(), PostsAdapter.OnPostClickListener,
    PollPostAdapter.OnPollItemClicked,
    PostsBottomSheet.OnPostSettingsItemClick {
    private var postsList = ArrayList<Any>()
    private val allUsersList = ArrayList<Users>()
    private val reactsList = ArrayList<PostReact>()
    private val dialog = PostsBottomSheet(this)
    private val viewModel: AppViewModel by viewModels()

    private val openEmojiAnim: Animation by lazy {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.emoji_in
        )
    }

    private val closeEmojiAnim: Animation by lazy {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.emoji_out
        )
    }

    private lateinit var postsRV: RecyclerView
    private lateinit var addPostBtn: ImageButton
    private lateinit var userImageIV: ImageView
    private lateinit var reactsLayout: CardView
    private lateinit var scrollView: NestedScrollView
    private lateinit var postsAdapter: PostsAdapter
    private lateinit var internetCheck: InternetCheck
    private lateinit var reactsDialog: Dialog

    //Reacts
    private lateinit var emoji1TV: TextView
    private lateinit var emoji2TV: TextView
    private lateinit var emoji3TV: TextView
    private lateinit var emoji4TV: TextView
    private lateinit var emoji5TV: TextView

    private var selectedPost: Any? = null
    private var viewHeight: Int = 0
    private var currentUser: Users? = null
    private var hasConnection = false
    private var loaded = false

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_home, container, false)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        //Get view height for comments bottom sheet fragment
        val displayMetrics = DisplayMetrics()
        viewHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().windowManager.maximumWindowMetrics.bounds.height()
            displayMetrics.heightPixels
        } else {
            @Suppress("DEPRECATION")
            requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.heightPixels
        }

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
            if (it) {
                //If there is connection get all posts
                viewModel.getPosts()
                viewModel.getVideoPost()
                viewModel.getPolls()
            }
        }

        //Get current user data
        currentUser = (activity as MainActivity).getCurrentUser()

        //Get all users
        allUsersList.addAll((activity as MainActivity).getAllUsers())

        initViews(view)
        initRecyclerView()

        //set user image to imageView
        Glide.with(requireContext()).load(currentUser!!.ImageLink.toString())
            .into(userImageIV)

        //Posts observer
        viewModel.postsMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                //For loop to get only reacts for current posts
                for (post in it) {
                    viewModel.getReacts(post.id!!)
                }
                //Fun used to sort all posts array
                val postsArray = UpdatePostsList(postsList, it, postsAdapter).handlePostsList()
                postsList = postsArray
            }
        })

        //Video Posts observer
        viewModel.videoPostsMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                for (post in it) {
                    viewModel.getReacts(post.id)
                }
                val postsArray = UpdatePostsList(postsList, it, postsAdapter).handleVideoPostsList()
                postsList = postsArray
            }
        })

        //Poll posts observer
        viewModel.pollsMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                for (post in it) {
                    viewModel.getReacts(post.id!!)
                }
                val postsArray = UpdatePostsList(postsList, it, postsAdapter).handlePollsList()
                postsList = postsArray
            }
        })

        //Reacts observer
        viewModel.reactMLD.observe(this.viewLifecycleOwner, Observer {
            for (react in it) {
                if (!reactsList.contains(react)) {
                    reactsList.add(react)
                    postsAdapter.notifyDataSetChanged()
                }
            }
        })

        //OnClick Listeners
        addPostBtn.setOnClickListener(View.OnClickListener
        {
            goToCreatePostFragment()
        })

        userImageIV.setOnClickListener(View.OnClickListener
        {
            goToProfileFragment(currentUser!!)
        })

        //Check changes in posts
        Firebase.firestore.collection("Posts").addSnapshotListener()
        { _, _ ->
            if (hasConnection) {
                viewModel.getPosts()
            }
        }

        //Check changes in video posts
        Firebase.firestore.collection("VideoPosts").addSnapshotListener()
        { _, _ ->
            if (hasConnection) {
                viewModel.getVideoPost()
            }
        }

        //Check changes in polls
        Firebase.firestore.collection("Polls").addSnapshotListener()
        { _, _ ->
            if (hasConnection) {
                viewModel.getPolls()
            }
        }

        //Check changes in reacts
        Firebase.firestore.collection("PostReacts").addSnapshotListener()
        { _, _ ->
            if (hasConnection) {
                for (post in postsList) {
                    when (post) {
                        is Posts -> {
                            viewModel.getReacts(post.id!!)
                        }
                        is PollPost -> {
                            viewModel.getReacts(post.id!!)
                        }
                        is VideoPost -> {
                            viewModel.getReacts(post.id)
                        }
                    }
                }
            }
        }
    }

    private fun goToCreatePostFragment() {
        val fragment = CreatePostFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val bundle = Bundle()
        bundle.putBoolean("IsNewPost", true)
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("HANDLE_POSTS_FRAGMENT")
        transaction.commit()
    }

    private fun initRecyclerView() {
        //Set linear layout manager to recyclerView
        postsRV.layoutManager = LinearLayoutManager(context)
        postsRV.addItemDecoration(LinearSpacingItemDecorator(20))
        //Disable animations
        (postsRV.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        //Initialize adapter and set it to recyclerview
        postsAdapter = PostsAdapter(
            postsList, allUsersList, reactsList, this,
            this, currentUser!!.Phone!!, requireContext()
        )
        postsRV.adapter = postsAdapter
    }

    override fun onPostClick(post: Any) {
        selectedPost = post
        dialog.show(requireActivity().supportFragmentManager, "POSTS_BOTTOM_SHEETS")
    }

    override fun onCommentClick(postID: String) {
        showComments(postID)
    }

    override fun onReactsClick(height: Int, post: Any, case: String) {
        selectedPost = post
        if (case == "REMOVE") {
            when (post) {
                is Posts -> {
                    viewModel.deleteReact(currentUser!!.Phone!!, post.id!!)
                }
                is PollPost -> {
                    viewModel.deleteReact(currentUser!!.Phone!!, post.id!!)
                }
                is VideoPost -> {
                    viewModel.deleteReact(currentUser!!.Phone!!, post.id)
                }
            }
        } else {
            showDialog()
        }
    }

    override fun onPostOwnerClick(postOwner: Users) {
        goToProfileFragment(postOwner)
    }

    override fun onPollItemClicked(pollPost: PollPost, pollItem: Poll) {
        viewModel.editPollChoice(
            pollPost,
            pollItem,
            currentUser!!.Phone.toString()
        )
    }

    override fun onPostSettingsClick(case: String) {
        if (case == "Edit") {
            when (selectedPost) {
                is Posts -> {
                    editPost()
                }
                is PollPost -> {
                    editPollPost()
                }
                is VideoPost -> {
                    editVideoPollPost()
                }
            }
        } else if (case == "Delete") {
            deletePost()
        }
    }

    private fun initViews(view: View) {
        //Initialize views
        scrollView = view.findViewById(R.id.sv_home)
        postsRV = view.findViewById(R.id.rv_admin_posts)
        userImageIV = view.findViewById(R.id.iv_home_user)
        addPostBtn = view.findViewById(R.id.btn_add_admin_post)
    }

    private fun showComments(postID: String) {
        val commentsFragment = CommentsBottomFragment(postID, viewHeight)
        commentsFragment.show(
            requireActivity().supportFragmentManager,
            "COMMENTS_BOTTOM_SHEETS"
        )
    }

    private fun deletePost() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity as MainActivity)
        builder.setCancelable(true)
        builder.setTitle(R.string.delete_post)
        builder.setMessage(R.string.delete_post_confirmation)
        builder.setPositiveButton(R.string.yes) { _, _ ->
            if (hasConnection) {
                when (selectedPost) {
                    is Posts -> {
                        viewModel.deletePost((selectedPost as Posts).id!!)
                    }
                    is PollPost -> {
                        viewModel.deletePollPost((selectedPost as PollPost).id!!)
                    }
                    is VideoPost -> {
                        viewModel.deleteVideoPost((selectedPost as VideoPost).id)
                    }
                }
            } else {
                Toast.makeText(requireContext(), R.string.check_connection, Toast.LENGTH_SHORT)
                    .show()
            }
        }
        builder.setNegativeButton(R.string.no) { _, _ -> }
        builder.show()
        dialog.dismiss()
    }

    private fun editPost() {
        val fragment = CreatePostFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val bundle = Bundle()
        bundle.putParcelable("EDITING_POST", selectedPost as Posts)
        bundle.putBoolean("IsNewPost", false)
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("HANDLE_POSTS_FRAGMENT")
        transaction.commit()
        dialog.dismiss()
    }

    private fun editPollPost() {
        val fragment = CreatePostFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val bundle = Bundle()
        bundle.putParcelable("EDITING_POLL_POST", selectedPost as PollPost)
        bundle.putBoolean("IsNewPost", false)
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("HANDLE_POSTS_FRAGMENT")
        transaction.commit()
        dialog.dismiss()
    }

    private fun editVideoPollPost() {
        val fragment = CreatePostFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val bundle = Bundle()
        bundle.putParcelable("EDITING_VIDEO_POST", selectedPost as VideoPost)
        bundle.putBoolean("IsNewPost", false)
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("HANDLE_POSTS_FRAGMENT")
        transaction.commit()
        dialog.dismiss()
    }

    private fun goToProfileFragment(user: Users?) {
        if (user != null) {
            val profilePostsList = ArrayList<Any>()
            for (post in postsList) {
                if (post is Posts) {
                    if (post.ownerID == user.Phone) {
                        profilePostsList.add(post)
                    }
                }
                if (post is PollPost) {
                    if (post.ownerID == user.Phone) {
                        profilePostsList.add(post)
                    }
                }
            }

            val manager = requireActivity().supportFragmentManager
            val transaction = manager.beginTransaction()
            val fragment = ProfileFragment()
            val args = Bundle()
            args.putParcelable("Profile_User", user)
            args.putParcelableArrayList(
                "Profile_Posts",
                profilePostsList as ArrayList<out Parcelable>
            )
            fragment.arguments = args
            transaction.add(R.id.main_frame, fragment)
            transaction.addToBackStack("PROFILE_FRAGMENT")
            transaction.commit()
        }
    }

    private fun showDialog() {
        //Create another dialogue to get number of points
        reactsDialog = Dialog(requireContext())
        reactsDialog.setCancelable(true)
        reactsDialog.setContentView(R.layout.layout_reacts)

        val onClickListener = OnClickListener(object : OnClickListener, (View) -> Unit {
            override fun onClick(p0: View?) {
                when (selectedPost) {
                    is Posts -> {
                        viewModel.createReact(
                            (selectedPost as Posts).id!!,
                            currentUser!!.Phone!!,
                            (p0 as TextView).text as String
                        )
                    }
                    is PollPost -> {
                        viewModel.createReact(
                            (selectedPost as PollPost).id!!,
                            currentUser!!.Phone!!,
                            (p0 as TextView).text as String
                        )
                    }
                    is VideoPost -> {
                        viewModel.createReact(
                            (selectedPost as VideoPost).id,
                            currentUser!!.Phone!!,
                            (p0 as TextView).text as String
                        )
                    }
                }
                reactsDialog.dismiss()
            }

            override fun invoke(p1: View) {
                when (selectedPost) {
                    is Posts -> {
                        viewModel.createReact(
                            (selectedPost as Posts).id!!,
                            currentUser!!.Phone!!,
                            (p1 as TextView).text as String
                        )
                    }
                    is PollPost -> {
                        viewModel.createReact(
                            (selectedPost as PollPost).id!!,
                            currentUser!!.Phone!!,
                            (p1 as TextView).text as String
                        )
                    }
                    is VideoPost -> {
                        viewModel.createReact(
                            (selectedPost as VideoPost).id,
                            currentUser!!.Phone!!,
                            (p1 as TextView).text as String
                        )
                    }
                }
                reactsDialog.dismiss()
            }
        })

        //Init emojis
        emoji1TV = reactsDialog.findViewById(R.id.tv_emoji_1)
        emoji2TV = reactsDialog.findViewById(R.id.tv_emoji_2)
        emoji3TV = reactsDialog.findViewById(R.id.tv_emoji_3)
        emoji4TV = reactsDialog.findViewById(R.id.tv_emoji_4)
        emoji5TV = reactsDialog.findViewById(R.id.tv_emoji_5)

        //Start emoji animation
        emoji1TV.startAnimation(openEmojiAnim)
        emoji2TV.startAnimation(openEmojiAnim)
        emoji3TV.startAnimation(openEmojiAnim)
        emoji4TV.startAnimation(openEmojiAnim)
        emoji5TV.startAnimation(openEmojiAnim)

        //SetEmoji onClickListeners
        emoji1TV.setOnClickListener(onClickListener)
        emoji2TV.setOnClickListener(onClickListener)
        emoji3TV.setOnClickListener(onClickListener)
        emoji4TV.setOnClickListener(onClickListener)
        emoji5TV.setOnClickListener(onClickListener)

        val window = reactsDialog.window
        window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        window.setBackgroundDrawable(
            InsetDrawable(
                ColorDrawable(Color.TRANSPARENT),
                30
            )
        )

        reactsDialog.show()
    }

}