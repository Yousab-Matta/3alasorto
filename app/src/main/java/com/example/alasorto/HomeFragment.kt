package com.example.alasorto

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.view.View.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide
import com.example.alasorto.adapters.PostMediaAdapter
import com.example.alasorto.adapters.PostsAdapter
import com.example.alasorto.dataClass.*
import com.example.alasorto.offlineUserDatabase.OfflineUserViewModel
import com.example.alasorto.utils.*
import com.example.alasorto.viewModels.AppViewModel
import com.example.alasorto.viewModels.PostsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.concurrent.thread

@Suppress("UNCHECKED_CAST")
class HomeFragment : Fragment(R.layout.fragment_home),
    PostsBottomSheet.OnPostSettingsItemClick {
    private val currentUserId = FirebaseAuth.getInstance().currentUser!!.phoneNumber!!
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

    private lateinit var mActivity: MainActivity
    private lateinit var recyclerView: RecyclerView
    private lateinit var addPostBtn: ImageButton
    private lateinit var userImageIV: ImageView
    private lateinit var postsAdapter: PostsAdapter
    private lateinit var postsMediaAdapter: PostMediaAdapter
    private lateinit var internetCheck: InternetCheck

    @SuppressLint("NotifyDataSetChanged", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
            if (it && postsList.size == 0) {
                postsViewModel.getPosts()
            }
        }

        initViews(view)
        initRecyclerView()

        mActivity = activity as MainActivity

        offlineUserViewModel.getUserById(currentUserId)
        offlineUserViewModel.userById.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                currentUser = it.user
                setUserData()
            } else {
                currentUser = mActivity.getCurrentUser()
                if (currentUser != null) {
                    setUserData()
                } else {
                    appViewModel.getCurrentUser()
                }
            }
        })

        appViewModel.currentUserMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                if (currentUser != null) {
                    currentUser = it
                    setUserData()
                }
            }
        })

        //User by Id Observer
        appViewModel.userByIdMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                postsAdapter.updateUsersList(it)
                usersIdsList.add(it.phone)

                if (it.phone == currentUserId && currentUser == null) {
                    currentUser = it
                    setUserData()
                }
            }
        })

        //Posts observer
        postsViewModel.postMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                for (post in it) {
                    if (!postsList.any { it1 -> it1.postId == post.postId }) {
                        postsList.add(post)
                        postsList.sortByDescending { it1 -> it1.postDate }
                        postsAdapter.notifyItemInserted(postsList.size - 1)
                    }

                    thread {
                        if (!usersIdsList.contains(post.ownerID)) {
                            appViewModel.getUserById(post.ownerID)
                        }
                    }
                    thread {
                        postsViewModel.getReacts(post.postId)
                    }

                    thread {
                        postsViewModel.getPollData(post.postId)
                    }
                }
            }
        })

        postsViewModel.pollDataMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                postsAdapter.updatePollData(it)

                if (it.pollItemData != null) {
                    for (pollItem in it.pollItemData!!) {
                        appViewModel.getUserById(pollItem.userId!!)
                    }
                }
            }
        })

        //Reacts observer
        postsViewModel.reactMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                postsAdapter.updateReacts(it)
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

        userImageIV.setOnClickListener(View.OnClickListener
        {
            goToProfileFragment()
        })

        currentUser = mActivity.getCurrentUser()
        setUserData()
    }

    override fun onResume() {
        super.onResume()
        //Check changes in posts
        Firebase.firestore.collection("Posts").addSnapshotListener()
        { _, _ ->
            if (hasConnection) {
                postsViewModel.getPosts()
            }
        }

        //Check changes in reacts
        Firebase.firestore.collection("PostReacts").addSnapshotListener()
        { _, _ ->
            if (hasConnection) {
                for (post in postsList) {
                    postsViewModel.getReacts(post.postId)
                }
            }
        }

        //Check changes in pollData
        Firebase.firestore.collection("PollItemData").addSnapshotListener()
        { _, _ ->
            if (hasConnection) {
                for (post in postsList) {
                    postsViewModel.getPollData(post.postId)
                }
            }
        }
    }

    private fun showPostControls(post: Post) {
        selectedPost = post
        dialog.show(requireActivity().supportFragmentManager, "POSTS_BOTTOM_SHEETS")
    }

    private fun showComments(postID: String) {
        val commentsFragment = CommentsBottomFragment(postID)
        commentsFragment.show(
            requireActivity().supportFragmentManager,
            "COMMENTS_BOTTOM_SHEETS"
        )
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
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.addItemDecoration(LinearSpacingItemDecorator(30))

        //Initialize adapter and set it to recyclerview
        postsAdapter = PostsAdapter(
            postsList,
            ::showComments,
            ::showPostControls,
            ::onPollItemClicked,
            ::expandMedia,
            ::expandImage,
            ::createReact,
            ::deleteReact
        )

        postsAdapter.setHasStableIds(true)
        recyclerView.adapter = postsAdapter
    }

    private fun onPollItemClicked(pollPost: Post, pollItemId: String) {
        if (Firebase.auth.currentUser != null && Firebase.auth.currentUser!!.phoneNumber != null) {
            val userSelection =
                UserSelection(Firebase.auth.currentUser!!.phoneNumber, pollItemId)
            postsViewModel.editPollChoice(
                userSelection,
                pollPost,
            )
        }
    }

    override fun onPostSettingsClick(case: String) {
        if (case == "Edit") {
            editPost()
        } else if (case == "Delete") {
            deletePost()
        }
    }

    private fun initViews(view: View) {
        //Initialize views
        userImageIV = view.findViewById(R.id.iv_home_user)
        recyclerView = view.findViewById(R.id.rv_admin_posts)
        addPostBtn = view.findViewById(R.id.btn_add_admin_post)
    }

    private fun deletePost() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity as MainActivity)
        builder.setCancelable(true)
        builder.setTitle(R.string.delete_post)
        builder.setMessage(R.string.delete_post_confirmation)
        builder.setPositiveButton(R.string.yes) { _, _ ->
            if (hasConnection) {
                postsViewModel.deletePost(selectedPost!!.postId)
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
        bundle.putParcelable("EDITING_POST", selectedPost)
        bundle.putBoolean("IsNewPost", false)
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("HANDLE_POSTS_FRAGMENT")
        transaction.commit()
        dialog.dismiss()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun expandMedia(mediaList: ArrayList<MediaData>?, clickedItemIndex: Int) {
        val args = Bundle()
        args.putParcelableArrayList("MEDIA_LIST", mediaList)
        args.putInt("CLICKED_ITEM_INDEX", clickedItemIndex)
        mActivity.goToEnlargeMediaFragment(args)
    }

    private fun expandImage(imageLink: String) {
        val args = Bundle()
        args.putString("IMAGE_LINK", imageLink)
        mActivity.goToEnlargeMediaFragment(args)
    }

    private fun expandVideo(videoLink: String) {
        val args = Bundle()
        args.putString("VIDEO_LINK", videoLink)
        mActivity.goToEnlargeMediaFragment(args)
    }

    private fun createReact(userSelection: UserSelection, postID: String) {
        postsViewModel.createReact(userSelection, postID)
    }

    private fun deleteReact(userSelection: UserSelection, postID: String) {
        postsViewModel.deleteReact(userSelection, postID)
    }

    private fun setUserData() {
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