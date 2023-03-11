package com.example.alasorto

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ramijemli.percentagechartview.PercentageChartView

@Suppress("DEPRECATION")
class ProfileFragment : Fragment(), PostsAdapter.OnPostClickListener,
    PostsBottomSheet.OnPostSettingsItemClick, PollPostAdapter.OnPollItemClicked,
    View.OnClickListener, View.OnLongClickListener {
    private var postsList = ArrayList<Any>()
    private val reactsList = ArrayList<PostReact>()
    private val dialog = PostsBottomSheet(this)
    private val viewModel: AppViewModel by viewModels()

    //Init fab animations
    private val rotateOpen: Animation by lazy {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.rotate_open_anim
        )
    }
    private val rotateClose: Animation by lazy {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.rotate_close_anim
        )
    }
    private val fromButton: Animation by lazy {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.from_bottom_anim
        )
    }
    private val toButton: Animation by lazy {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.to_button_anim
        )
    }

    private lateinit var openControlsBtn: FloatingActionButton
    private lateinit var handleUsersBtn: FloatingActionButton
    private lateinit var handleAttBtn: FloatingActionButton
    private lateinit var handleGroupsBtn: FloatingActionButton
    private lateinit var handleRemindersBtn: FloatingActionButton
    private lateinit var notificationBtn: FloatingActionButton
    private lateinit var galleryBtn: FloatingActionButton
    private lateinit var userIV: ImageView
    private lateinit var progressView: PercentageChartView
    private lateinit var postsRV: RecyclerView
    private lateinit var nameTV: TextView
    private lateinit var pointsTV: TextView
    private lateinit var internetCheck: InternetCheck
    private lateinit var adapter: PostsAdapter

    private var selectedPost: Posts? = null
    private var selectedPollPost: PollPost? = null

    private var height: Int = 0
    private var currentProfileUser: Users? = null
    private var hasConnection = false
    private var settingsBtnClicked = false

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        //Get View height for comments fragment
        val displayMetrics = DisplayMetrics()
        height = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().windowManager.maximumWindowMetrics.bounds.height()
            displayMetrics.heightPixels
        } else {
            @Suppress("DEPRECATION")
            requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.heightPixels
        }

        //Init widgets
        openControlsBtn = view.findViewById(R.id.btn_admin_controls)
        handleUsersBtn = view.findViewById(R.id.btn_handle_users)
        handleGroupsBtn = view.findViewById(R.id.btn_handle_groups)
        handleRemindersBtn = view.findViewById(R.id.btn_handle_reminders)
        handleAttBtn = view.findViewById(R.id.btn_handle_att)
        notificationBtn = view.findViewById(R.id.btn_custom_notification)
        galleryBtn = view.findViewById(R.id.btn_handle_gallery)
        nameTV = view.findViewById(R.id.tv_profile_name)
        pointsTV = view.findViewById(R.id.tv_profile_points)
        userIV = view.findViewById(R.id.iv_profile_image)
        progressView = view.findViewById(R.id.profile_progress_view)
        postsRV = view.findViewById(R.id.rv_profile_posts)

        //Get arguments
        val args = this.arguments
        //Initialize user
        if (args != null) {
            /*
            If user data is received from arguments then profile is set to this uses
            else user is current user
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                currentProfileUser = args.getParcelable("Profile_User", Users::class.java)
                val postsArray =
                    args.getParcelableArrayList("Profile_Posts", Posts::class.java)
                if (postsArray != null) {
                    postsList.addAll(postsArray)
                }
            } else {
                currentProfileUser = args.getParcelable("Profile_User")
                val postsArray =
                    args.getParcelableArrayList<Posts>("Profile_Posts")
                if (postsArray != null) {
                    postsList.addAll(postsArray)
                }
            }
        } else {
            currentProfileUser = (activity as MainActivity).getCurrentUser()
        }

        setUserData()

        //Check account access type and hide control buttons if not admin
        //ToDo: Access
        if (currentProfileUser!!.Access == "User") {
            openControlsBtn.visibility = View.GONE
        }

        val ownersList = ArrayList<Users>()
        ownersList.add(currentProfileUser!!)

        //Init adapter and RV layout
        adapter =
            PostsAdapter(
                postsList, ownersList, reactsList, this,
                this, currentProfileUser!!.Phone!!, requireContext()
            )
        postsRV.adapter = adapter
        postsRV.addItemDecoration(LinearSpacingItemDecorator(20))
        postsRV.layoutManager = LinearLayoutManager(requireContext())
        (postsRV.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
            if (it) {
                if (args == null) {
                    viewModel.getProfilePost(currentProfileUser!!.Phone!!)
                }
            }
        }

        //Observe Posts List
        viewModel.postsMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                val postsArray = UpdatePostsList(postsList, it, adapter).handlePostsList()
                postsList = postsArray
            }
        })

        viewModel.pollsMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                val postsArray = UpdatePostsList(postsList, it, adapter).handlePollsList()
                postsList = postsArray
            }
        })

        openControlsBtn.setOnClickListener(this)
        handleUsersBtn.setOnClickListener(this)
        handleGroupsBtn.setOnClickListener(this)
        handleRemindersBtn.setOnClickListener(this)
        handleAttBtn.setOnClickListener(this)
        notificationBtn.setOnClickListener(this)
        galleryBtn.setOnClickListener(this)

        openControlsBtn.setOnLongClickListener(this)
        handleUsersBtn.setOnLongClickListener(this)
        handleGroupsBtn.setOnLongClickListener(this)
        handleRemindersBtn.setOnLongClickListener(this)
        handleAttBtn.setOnLongClickListener(this)
        notificationBtn.setOnLongClickListener(this)
        galleryBtn.setOnLongClickListener(this)


        //Check changes in posts
        Firebase.firestore.collection("Posts").addSnapshotListener()
        { _, _ ->
            if (hasConnection) {
                viewModel.getPosts()
            }
        }

        //Check changes in posts
        Firebase.firestore.collection("Polls").addSnapshotListener()
        { _, _ ->
            if (hasConnection) {
                viewModel.getPolls()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().supportFragmentManager.popBackStack(
                        "PROFILE_FRAGMENT",
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                    this.isEnabled = false
                }
            })
    }

    override fun onPostClick(post: Any) {
        if (post is Posts) {
            this.selectedPost = post
            this.selectedPollPost = null
        } else if (post is PollPost) {
            this.selectedPollPost = post
            this.selectedPost = null
        }
        showSettingsDialog()
    }

    override fun onCommentClick(postID: String) {
        showComments(postID)
    }

    override fun onReactsClick(height: Int, post: Any, case: String) {
    }

    override fun onPostOwnerClick(postOwner: Users) {
    }

    override fun onPollItemClicked(pollPost: PollPost, pollItem: Poll) {
        viewModel.editPollChoice(
            pollPost,
            pollItem,
            currentProfileUser!!.Phone!!
        )
    }

    override fun onClick(p0: View?) {
        when (p0) {
            openControlsBtn -> {
                onSettingsButtonClicked()
            }
            handleUsersBtn -> {
                goToAllUsersFragment()
            }
            handleGroupsBtn -> {
                goToGroupsFragment()
            }
            handleAttBtn -> {
                goToAttendanceFragment()
            }
            handleRemindersBtn -> {
                goToRemindersFragment()
            }
            notificationBtn -> {
                goToNotificationsFragment()
            }
            galleryBtn -> {
                goToGalleryFragment()
            }
        }
    }

    override fun onLongClick(p0: View?): Boolean {
        when (p0) {
            handleUsersBtn -> {
                Toast.makeText(requireContext(), R.string.handle_users, Toast.LENGTH_SHORT).show()
            }
            handleGroupsBtn -> {
                Toast.makeText(requireContext(), R.string.handle_groups, Toast.LENGTH_SHORT).show()
            }
            handleAttBtn -> {
                Toast.makeText(requireContext(), R.string.handle_attendance, Toast.LENGTH_SHORT)
                    .show()
            }
            handleRemindersBtn -> {
                Toast.makeText(requireContext(), R.string.handle_reminders, Toast.LENGTH_SHORT)
                    .show()
            }
            notificationBtn -> {
                Toast.makeText(
                    requireContext(),
                    R.string.send_custom_notification,
                    Toast.LENGTH_SHORT
                ).show()
            }
            galleryBtn -> {
                Toast.makeText(requireContext(), R.string.handle_gallery, Toast.LENGTH_SHORT).show()
            }
        }
        return true
    }

    private fun showComments(postKey: String) {
        val commentsFragment = CommentsBottomFragment(postKey, height)
        commentsFragment.show(requireActivity().supportFragmentManager, "COMMENTS_BOTTOM_SHEETS")
    }

    private fun showSettingsDialog() {
        dialog.show(requireActivity().supportFragmentManager, "POSTS_BOTTOM_SHEETS")
    }

    private fun goToGalleryFragment() {
        val fragment = GalleryFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("GALLERY_FRAGMENT")
        transaction.commit()
    }

    private fun goToNotificationsFragment() {
        val fragment = SendNotificationFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("CUSTOM_NOTIF_FRAGMENT")
        transaction.commit()
    }

    private fun goToRemindersFragment() {
        val fragment = RemindersFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("REMINDERS_FRAGMENT")
        transaction.commit()
    }

    private fun goToAllUsersFragment() {
        val fragment = AllUsersFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("ALL_USERS_FRAGMENT")
        transaction.commit()
    }

    private fun goToGroupsFragment() {
        val fragment = GroupsFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val bundle = Bundle()
        bundle.putBoolean("SelectionEnabled", false)
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("GROUPS_FRAGMENT")
        transaction.commit()
    }

    private fun goToAttendanceFragment() {
        val fragment = AttendanceHistoryFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("ATTENDANCE_FRAGMENT")
        transaction.commit()
    }

    private fun setUserData() {
        nameTV.text = currentProfileUser!!.Name
        "${currentProfileUser!!.Points!!} Points".also { pointsTV.text = it }

        Glide.with(userIV).load(currentProfileUser!!.ImageLink).into(userIV)

        val attPercent = currentProfileUser!!.AttendedPercent!!
        if (attPercent in 0.0..100.0) {
            progressView.setProgress(currentProfileUser!!.AttendedPercent!!, true)
        }
    }

    private fun onSettingsButtonClicked() {
        setVisibility(settingsBtnClicked)
        setAnimation(settingsBtnClicked)
        settingsBtnClicked = !settingsBtnClicked
    }

    private fun setVisibility(isClicked: Boolean) {
        if (!isClicked) {
            //Set Visible
            handleUsersBtn.visibility = View.VISIBLE
            handleGroupsBtn.visibility = View.VISIBLE
            handleRemindersBtn.visibility = View.VISIBLE
            handleAttBtn.visibility = View.VISIBLE
            notificationBtn.visibility = View.VISIBLE
            galleryBtn.visibility = View.VISIBLE
        } else {
            handleUsersBtn.visibility = View.GONE
            handleGroupsBtn.visibility = View.GONE
            handleRemindersBtn.visibility = View.GONE
            handleAttBtn.visibility = View.GONE
            notificationBtn.visibility = View.GONE
            galleryBtn.visibility = View.GONE
        }
    }

    private fun setAnimation(isClicked: Boolean) {
        if (!isClicked) {
            openControlsBtn.startAnimation(rotateOpen)
            handleUsersBtn.startAnimation(fromButton)
            handleGroupsBtn.startAnimation(fromButton)
            handleRemindersBtn.startAnimation(fromButton)
            handleAttBtn.startAnimation(fromButton)
            notificationBtn.startAnimation(fromButton)
            galleryBtn.startAnimation(fromButton)
        } else {
            openControlsBtn.startAnimation(rotateClose)
            handleUsersBtn.startAnimation(toButton)
            handleGroupsBtn.startAnimation(toButton)
            handleRemindersBtn.startAnimation(toButton)
            handleAttBtn.startAnimation(toButton)
            notificationBtn.startAnimation(toButton)
            galleryBtn.startAnimation(toButton)

        }
    }

    override fun onPause() {
        super.onPause()
        handleUsersBtn.visibility = View.GONE
        handleGroupsBtn.visibility = View.GONE
        handleRemindersBtn.visibility = View.GONE
        handleAttBtn.visibility = View.GONE
        notificationBtn.visibility = View.GONE
        galleryBtn.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        handleUsersBtn.visibility = View.GONE
        handleGroupsBtn.visibility = View.GONE
        handleRemindersBtn.visibility = View.GONE
        handleAttBtn.visibility = View.GONE
        notificationBtn.visibility = View.GONE
        galleryBtn.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handleUsersBtn.visibility = View.GONE
        handleGroupsBtn.visibility = View.GONE
        handleRemindersBtn.visibility = View.GONE
        handleAttBtn.visibility = View.GONE
        notificationBtn.visibility = View.GONE
        galleryBtn.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        handleUsersBtn.visibility = View.GONE
        handleGroupsBtn.visibility = View.GONE
        handleRemindersBtn.visibility = View.GONE
        handleAttBtn.visibility = View.GONE
        notificationBtn.visibility = View.GONE
        galleryBtn.visibility = View.GONE
    }

    override fun onDetach() {
        super.onDetach()
        handleUsersBtn.visibility = View.GONE
        handleGroupsBtn.visibility = View.GONE
        handleRemindersBtn.visibility = View.GONE
        handleAttBtn.visibility = View.GONE
        notificationBtn.visibility = View.GONE
        galleryBtn.visibility = View.GONE
    }

    private fun editPost() {
        val fragment = CreatePostFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val bundle = Bundle()
        bundle.putParcelable("Editing_Post", selectedPost)
        bundle.putBoolean("IsNewPost", false)
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("HANDLE_POSTS_FRAGMENT")
        transaction.commit()
        dialog.dismiss()
    }

    private fun deletePost() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity as MainActivity)
        builder.setCancelable(true)
        builder.setTitle(R.string.delete_post)
        builder.setMessage(R.string.delete_post_confirmation)
        builder.setPositiveButton(R.string.yes) { _, _ ->
            if (hasConnection) {
                if (selectedPost != null) {
                    viewModel.deletePost(selectedPost!!.id.toString())
                } else if (selectedPollPost != null) {
                    viewModel.deletePollPost(selectedPollPost!!.id.toString())
                }
            }
        }
        builder.setNegativeButton(R.string.no) { _, _ -> }
        builder.show()
        dialog.dismiss()
    }

    override fun onPostSettingsClick(case: String) {
        if (case == "Edit") {
            editPost()
        } else if (case == "Delete") {
            deletePost()
        }
    }
}