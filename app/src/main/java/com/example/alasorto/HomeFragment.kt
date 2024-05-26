package com.example.alasorto

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.View.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.alasorto.adapters.PostsAdapter
import com.example.alasorto.dataClass.*
import com.example.alasorto.offlineUserDatabase.OfflineUserViewModel
import com.example.alasorto.utils.*
import com.example.alasorto.viewModels.AppViewModel
import com.example.alasorto.viewModels.PostsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

@Suppress("UNCHECKED_CAST")
class HomeFragment : Fragment(R.layout.fragment_home),
    PostsBottomSheet.OnPostSettingsItemClick {
    private val currentUserPhone = FirebaseAuth.getInstance().currentUser!!.phoneNumber!!
    private val dialog = PostsBottomSheet(this)
    private val postsViewModel: PostsViewModel by viewModels()
    private val appViewModel: AppViewModel by viewModels()
    private val offlineUserViewModel: OfflineUserViewModel by viewModels()

    //Used to check if this user is already updated from database so we don't fetch this user again
    private val usersIdsList = ArrayList<String>()
    private var postsList = ArrayList<Post>()
    private var selectedPost: Post? = null
    private var currentUser: UserData? = null
    private var hasConnection = false
    private var postsHaveReachedBottom = false
    private var canScrollVertically = true
    private var reference = Firebase.firestore.collection("Posts")

    private var groupId = ""
    private var collectionPath = ""
    private var currentUserId = ""

    private lateinit var mActivity: MainActivity
    private lateinit var recyclerView: RecyclerView
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var headerLayout: ConstraintLayout
    private lateinit var addPostBtn: TextView
    private lateinit var notificationsBtn: ImageButton
    private lateinit var userImageIV: ImageView
    private lateinit var postsAdapter: PostsAdapter
    private lateinit var internetCheck: InternetCheck

    @SuppressLint("NotifyDataSetChanged", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        mActivity = activity as MainActivity
        currentUser = mActivity.getCurrentUserData()
        currentUserId = mActivity.getCurrentUserId()

        val args = this.arguments
        if (args != null) {
            collectionPath = args.getString("COLLECTION_PATH", "")
            groupId = args.getString("GROUP_ID", "")

            if (collectionPath.isNotEmpty() && groupId.isNotEmpty()) {
                reference =
                    Firebase.firestore.collection(collectionPath).document(groupId)
                        .collection("Posts")
            }

            var argsString = args.getString("DATA_MAP")
            if (argsString != null) {
                argsString = argsString.replace("}", "")
                argsString = argsString.replace("{", "")
                argsString = argsString.replace("\"", "")

                val argsMap = argsString.split(",").associateTo(kotlin.collections.HashMap()) {
                    val (left, right) = it.split(":")
                    left to right
                }

                val collectionPath = if (argsMap.containsKey("collectionPath")) {
                    argsMap["collectionPath"]
                } else {
                    ""
                }!!

                val groupId = if (argsMap.containsKey("groupId")) {
                    argsMap["groupId"]
                } else {
                    ""
                }!!

                if (argsMap.containsKey("case")) {
                    if (argsMap["case"] == "Comment") {
                        if (argsMap["id"] != null && argsMap["id"]!!.isNotEmpty()) {
                            showComments(argsMap["id"]!!, collectionPath, groupId)
                        }
                    } else if (argsMap["case"] == "Post") {
                        if (argsMap["id"] != null && argsMap["id"]!!.isNotEmpty()) {
                            goToPostPage(argsMap["id"]!!, collectionPath, groupId)
                        }
                    } else if (argsMap["case"] == "Chat") {
                        if (argsMap["id"] != null && argsMap["id"]!!.isNotEmpty()) {
                            goToChatFragment(argsMap["id"]!!, collectionPath, groupId)
                        }
                    }
                }
            }
        }

        initViews(view)

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
            if (it) {
                if (postsList.size == 0) {
                    postsViewModel.getPosts(reference)
                    refreshLayout.isRefreshing = false
                } else {
                    if (!postsHaveReachedBottom) {
                        refreshLayout.isRefreshing = false
                        postsViewModel.getMorePosts(
                            reference,
                            postsList.last().postDate!!
                        )
                    }
                }
            }
        }

        initRecyclerView()
        setUserData()

        refreshLayout.setOnRefreshListener {
            if (hasConnection) {
                postsList.clear()
                postsViewModel.getPosts(reference)
                refreshLayout.isRefreshing = false
            }
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (postsList.size > 0 && linearLayoutManager.findLastCompletelyVisibleItemPosition() == 0 && !postsHaveReachedBottom) {
                    refreshLayout.isRefreshing = false
                    postsViewModel.getMorePosts(
                        reference,
                        postsList.last().postDate!!
                    )
                }
            }
        })

        //ToDo:offline user data

        //User by Id Observer
        appViewModel.userByIdMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                postsAdapter.updateUsersList(it)
                usersIdsList.add(it.userId)
            }
        })

        //Posts observer
        postsViewModel.postsListMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                postsAdapter.notifyDataSetChanged()

                postsHaveReachedBottom = it.size < 15

                if (refreshLayout.isRefreshing) {
                    refreshLayout.isRefreshing = false

                    postsList.clear()
                    postsList.addAll(it)
                    postsList.sortByDescending { it1 -> it1.postDate }
                    postsAdapter.notifyDataSetChanged()
                } else {
                    for (post in it) {
                        if (!postsList.any { it1 -> it1.postId == post.postId }) {
                            postsList.add(post)
                            postsList.sortByDescending { it1 -> it1.postDate }
                            postsAdapter.notifyItemInserted(postsList.size - 1)
                        }
                    }
                }
            }

            getPostsData()
        })

        postsViewModel.newPostsListMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                postsHaveReachedBottom = it.size < 15

                for (post in it) {
                    if (!postsList.any { it1 -> it1.postId == post.postId }) {
                        postsList.add(post)
                        postsList.sortByDescending { it1 -> it1.postDate }
                        postsAdapter.notifyItemInserted(postsList.size - 1)
                    }
                }

                getPostsData()
            }
        })

        postsViewModel.pollDataMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                postsAdapter.updatePollData(it)

                //Get poll users who voted
                if (it.pollItemData != null) {
                    for (pollItem in it.pollItemData!!) {
                        if (!usersIdsList.contains(pollItem.userId)) {
                            appViewModel.getUserById(pollItem.userId)
                        }
                    }
                }
            }
        })

        //Reacts observer
        postsViewModel.reactMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                postsAdapter.addReactToList(it)
            }
        })

        postsViewModel.failedEventMLD.observe(this.viewLifecycleOwner, Observer {
            if (it) {
                Toast.makeText(requireContext(), R.string.error_has_occured, Toast.LENGTH_SHORT)
                    .show()
            }
        })

        //OnClick Listeners
        addPostBtn.setOnClickListener(View.OnClickListener
        {
            goToCreatePostFragment()
        })

        notificationsBtn.setOnClickListener(View.OnClickListener
        {
            goToNotificationsFragment()
        })

        userImageIV.setOnClickListener(View.OnClickListener
        {
            goToProfileFragment()
        })
    }

    override fun onResume() {
        super.onResume()
        //Check changes in posts
        reference.addSnapshotListener()
        { _, _ ->
            if (hasConnection) {
                postsViewModel.getPosts(reference)
                refreshLayout.isRefreshing = false
            }
        }

        //Check changes in reacts
        for (post in postsList) {
            reference.document(post.postId).collection("PostReacts")
                .addSnapshotListener()
                { _, _ ->
                    if (hasConnection) {
                        postsViewModel.getReacts(reference, post.postId)
                    }
                }

            reference.document(post.postId).collection("PollItemData")
                .addSnapshotListener()
                { _, _ ->
                    if (hasConnection) {
                        postsViewModel.getPollData(reference, post.postId)
                    }
                }
        }
    }

    private fun getPostsData() {
        for (post in postsList) {

            thread {
                if (!usersIdsList.contains(post.ownerID)) {
                    appViewModel.getUserById(post.ownerID)
                }
            }
            thread {
                postsViewModel.getReacts(reference, post.postId)
            }

            thread {
                postsViewModel.getPollData(reference, post.postId)
            }

            thread {
                if (post.mentionsList.isNotEmpty()) {
                    for (user in post.mentionsList)
                        if (!usersIdsList.contains(user)) {
                            appViewModel.getUserById(user)
                        }
                }
            }
        }
    }

    private fun showPostControls(post: Post) {
        selectedPost = post
        dialog.show(requireActivity().supportFragmentManager, "POSTS_BOTTOM_SHEETS")
    }

    private fun showComments(postId: String) {
        val bottomFragment = BottomSheetFragment()
        val args = Bundle()
        args.putString("COLLECTION_PATH", collectionPath)
        args.putString("FRAGMENT", "COMMENTS")
        args.putString("POST_ID", postId)
        args.putString("GROUP_ID", groupId)
        bottomFragment.show(
            requireActivity().supportFragmentManager,
            "COMMENTS_BOTTOM_SHEETS"
        )

        val commentsFragment = CommentsFragment()
        commentsFragment.arguments = args
        bottomFragment.setFragment(commentsFragment)
    }

    private fun goToPeopleWhoVotedFragment(postID: String, usersList: ArrayList<UserSelection>?) {
        val fragment = PeopleWhoReactedFragment(postID)
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val args = Bundle()
        args.putString("COLLECTION_PATH", collectionPath)
        args.putString("GROUP_ID", groupId)
        args.putParcelableArrayList("USERS_WHO_VOTED", usersList)
        fragment.arguments = args
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("PEOPLE_WHO_REACTED_FRAGMENT")
        transaction.commit()
    }

    private fun goToCreatePostFragment() {
        val fragment = CreatePostFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val bundle = Bundle()
        bundle.putString("COLLECTION_PATH", collectionPath)
        bundle.putString("GROUP_ID", groupId)
        bundle.putBoolean("IsNewPost", true)
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("HANDLE_POSTS_FRAGMENT")
        transaction.commit()
    }

    private fun goToNotificationsFragment() {
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.add(R.id.main_frame, InAppNotificationFragment())
        transaction.addToBackStack("IN_APP_NOTIFICATION_FRAGMENT")
        transaction.commit()
    }

    private fun goToProfileFragment() {
        val fragment = UserProfileFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("USER_PROFILE_FRAGMENT")
        transaction.commit()
    }

    private fun initRecyclerView() {
        //Set linear layout manager to recyclerView
        linearLayoutManager = object : LinearLayoutManager(requireContext()) {
            //Set can scroll vertically or no to disable scroll while changing vc progress
            override fun canScrollVertically(): Boolean {
                return canScrollVertically
            }
        }
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.addItemDecoration(LinearSpacingItemDecorator(30))

        //Initialize adapter and set it to recyclerview
        postsAdapter = PostsAdapter(
            postsList,
            currentUserId,
            ::goToPeopleWhoVotedFragment,
            ::showComments,
            ::showPostControls,
            ::onPollItemClicked,
            ::enlargeMedia,
            ::createReact,
            ::deleteReact,
            ::goToPostPage,
            ::changeScrollState
        )

        postsAdapter.setHasStableIds(true)
        recyclerView.adapter = postsAdapter
    }

    private fun changeScrollState(canScroll: Boolean) {
        //Disable or Enable viewPager scroll
        mActivity.viewPagerScrollState(canScroll)
        //Disable or Enable posts recyclerview scroll
        canScrollVertically = canScroll
    }

    private fun onPollItemClicked(pollPost: Post, pollItemId: String) {
        val userSelection =
            UserSelection(currentUserId, pollItemId)
        postsViewModel.editPollChoice(
            userSelection,
            pollPost
        )
    }

    override fun onPostSettingsClick(case: String) {
        if (case == "Edit") {
            editPost()
        } else if (case == "Delete") {
            deletePost()
        }
    }

    private fun goToPostPage(postId: String) {
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.add(R.id.main_frame, SpecificPostFragment(postId))
        transaction.addToBackStack("SPECIFIC_POSTS_FRAGMENT")
        transaction.commit()
    }

    private fun goToPostPage(postId: String, collectionPath: String, groupId: String) {
        val fragment = SpecificPostFragment(postId)
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val args = Bundle()
        args.putString("COLLECTION_PATH", collectionPath)
        args.putString("GROUP_ID", groupId)
        fragment.arguments = args
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("SPECIFIC_POSTS_FRAGMENT")
        transaction.commit()
    }

    private fun goToChatFragment(messageId: String, collectionPath: String, groupId: String) {
        val fragment = ChatFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val args = Bundle()
        args.putString("COLLECTION_PATH", collectionPath)
        args.putString("CHAT_ID", groupId)
        fragment.arguments = args
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("CHAT_FRAGMENT")
        transaction.commit()
    }

    private fun showComments(postId: String, collectionPath: String, groupId: String) {
        val bottomFragment = BottomSheetFragment()
        val args = Bundle()
        args.putString("COLLECTION_PATH", collectionPath)
        args.putString("FRAGMENT", "COMMENTS")
        args.putString("POST_ID", postId)
        args.putString("GROUP_ID", groupId)
        bottomFragment.show(
            requireActivity().supportFragmentManager,
            "COMMENTS_BOTTOM_SHEETS"
        )

        val commentsFragment = CommentsFragment()
        commentsFragment.arguments = args
        bottomFragment.setFragment(commentsFragment)
    }

    private fun initViews(view: View) {
        //Initialize views
        userImageIV = view.findViewById(R.id.iv_home_user)
        headerLayout = view.findViewById(R.id.layout_home_header)
        recyclerView = view.findViewById(R.id.rv_admin_posts)
        addPostBtn = view.findViewById(R.id.tv_add_new_post)
        notificationsBtn = view.findViewById(R.id.btn_user_notification)
        refreshLayout = view.findViewById(R.id.swipe_refresh_layout)

        //If both are empty then header is not needed since it will be a nested fragment inside other fragment
        if (collectionPath.isEmpty() && groupId.isEmpty()) {
            headerLayout.visibility = VISIBLE
        } else {
            headerLayout.visibility = GONE
        }
    }

    private fun deletePost() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity as MainActivity)
        builder.setCancelable(true)
        builder.setTitle(R.string.delete_post)
        builder.setMessage(R.string.delete_post_confirmation)
        builder.setPositiveButton(R.string.yes) { _, _ ->
            if (hasConnection) {
                postsViewModel.deletePost(reference, selectedPost!!.postId)
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.check_connection,
                    Toast.LENGTH_SHORT
                )
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
        bundle.putString("COLLECTION_PATH", collectionPath)
        bundle.putString("GROUP_ID", groupId)
        bundle.putParcelable("EDITING_POST", selectedPost)
        bundle.putBoolean("IsNewPost", false)
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("HANDLE_POSTS_FRAGMENT")
        transaction.commit()
        dialog.dismiss()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun enlargeMedia(
        mediaList: ArrayList<MediaData>?,
        clickedItemIndex: Int,
        postID: String
    ) {
        val args = Bundle()
        args.putParcelableArrayList("MEDIA_LIST", mediaList)
        args.putInt("CLICKED_ITEM_INDEX", clickedItemIndex)
        args.putString("POST_ID", postID)
        mActivity.goToEnlargeMediaFragment(args)
    }

    private fun createReact(userSelection: UserSelection, postID: String) {
        postsViewModel.createReact(reference, userSelection, postID)
    }

    private fun deleteReact(userSelection: UserSelection, postID: String) {
        postsAdapter.removeReactFromList(PostReact(arrayListOf(userSelection), postID))
        postsViewModel.deleteReact(reference, userSelection, postID)
    }

    private fun setUserData() {
        Log.d("TEST_USER_DATA", "$currentUser")
        if (currentUser != null) {
            //set user image to imageView
            if (currentUser!!.imageLink.isNotEmpty()) {
                Glide.with(requireContext()).load(currentUser!!.imageLink)
                    .into(userImageIV)
            } else {
                Glide.with(requireContext()).load(R.drawable.image_logo)
                    .into(userImageIV)
            }
        }
    }
}