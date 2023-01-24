package com.example.alasorto

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alasorto.adapters.ProfilePostsAdapter
import com.example.alasorto.dataClass.Posts
import com.example.alasorto.dataClass.Users
import com.ramijemli.percentagechartview.PercentageChartView

@Suppress("DEPRECATION")
class ProfileFragment : Fragment(), ProfilePostsAdapter.OnClickListener,
    PostsBottomSheet.OnPostSettingsItemClick {
    private val postsList = ArrayList<Posts>()
    private val dialog = PostsBottomSheet(this)

    private lateinit var handleUsersIV: ImageView
    private lateinit var handleAttIV: ImageView
    private lateinit var handleGroupsIV: ImageView
    private lateinit var handleRemindersIV: ImageView
    private lateinit var notificationIV: ImageView
    private lateinit var userIV: ImageView
    private lateinit var progressView: PercentageChartView
    private lateinit var controlLL: LinearLayout
    private lateinit var postsRV: RecyclerView
    private lateinit var viewModel: AppViewModel
    private lateinit var nameTV: TextView
    private lateinit var internetCheck: InternetCheck
    private lateinit var adapter: ProfilePostsAdapter
    private lateinit var post: Posts

    private var height: Int = 0
    private var user: Users? = null
    private var hasConnection = false

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

        val displayMetrics = DisplayMetrics()
        height = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().windowManager.maximumWindowMetrics.bounds.height()
            displayMetrics.heightPixels
        } else {
            @Suppress("DEPRECATION")
            requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.heightPixels
        }

        handleUsersIV = view.findViewById(R.id.iv_user)
        handleGroupsIV = view.findViewById(R.id.iv_handle_groups)
        handleRemindersIV = view.findViewById(R.id.iv_create_post)
        handleAttIV = view.findViewById(R.id.iv_create_att)
        notificationIV = view.findViewById(R.id.iv_group_chat)
        nameTV = view.findViewById(R.id.tv_profile_name)
        userIV = view.findViewById(R.id.iv_profile_image)
        progressView = view.findViewById(R.id.profile_progress_view)
        controlLL = view.findViewById(R.id.ll_profile_control)
        postsRV = view.findViewById(R.id.rv_profile_posts)

        postsRV.layoutManager = LinearLayoutManager(requireContext())

        //Initialize view model
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        //Get arguments
        val args = this.arguments
        //Initialize user
        if (args != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                user = args.getParcelable("Profile_User", Users::class.java)
                val postsArray =
                    args.getParcelableArrayList("Profile_Posts", Posts::class.java)
                if (postsArray != null) {
                    postsList.addAll(postsArray)
                }
            } else {
                user = args.getParcelable("Profile_User")
                val postsArray =
                    args.getParcelableArrayList<Posts>("Profile_Posts")
                if (postsArray != null) {
                    postsList.addAll(postsArray)
                }
            }
        } else {
            user = (activity as MainActivity).getCurrentUser()
        }

        //Check account access type and hide control buttons if not admin
        if (user!!.Access == "User") {
            controlLL.visibility = View.GONE
        }

        adapter = ProfilePostsAdapter(postsList, user!!, this, this)
        postsRV.adapter = adapter

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
            if (it) {
                if (args == null) {
                    viewModel.getProfilePost(user!!.Phone.toString())
                }
            }
        }

        viewModel.profilePostsMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                postsList.clear()
                postsList.addAll(it)
                adapter.notifyDataSetChanged()
            }
        })

        nameTV.text = user!!.Name

        Glide.with(userIV).load(user!!.ImageLink).into(userIV)

        val attPercent = user!!.AttendedPercent!!
        if (attPercent in 0.0..100.0) {
            progressView.setProgress(user!!.AttendedPercent!!, true)
        }

        //OnClickListeners
        handleUsersIV.setOnClickListener(View.OnClickListener {
            val fragment = AllUsersFragment()
            val manager = requireActivity().supportFragmentManager
            val transaction = manager.beginTransaction()
            transaction.add(R.id.main_frame, fragment)
            transaction.addToBackStack("ALL_USERS_FRAGMENT")
            transaction.commit()
        })

        handleGroupsIV.setOnClickListener(View.OnClickListener {
            val fragment = GroupsFragment()
            val manager = requireActivity().supportFragmentManager
            val transaction = manager.beginTransaction()
            val bundle = Bundle()
            bundle.putBoolean("SelectionEnabled", false)
            fragment.arguments = bundle
            transaction.add(R.id.main_frame, fragment)
            transaction.addToBackStack("GROUPS_FRAGMENT")
            transaction.commit()
        })

        handleAttIV.setOnClickListener(View.OnClickListener {
            val fragment = AttendanceHistoryFragment()
            val manager = requireActivity().supportFragmentManager
            val transaction = manager.beginTransaction()
            transaction.add(R.id.main_frame, fragment)
            transaction.addToBackStack("ATTENDANCE_FRAGMENT")
            transaction.commit()
        })

        handleRemindersIV.setOnClickListener(View.OnClickListener {
            val fragment = RemindersFragment()
            val manager = requireActivity().supportFragmentManager
            val transaction = manager.beginTransaction()
            transaction.add(R.id.main_frame, fragment)
            transaction.addToBackStack("REMINDERS_FRAGMENT")
            transaction.commit()
        })

        notificationIV.setOnClickListener(View.OnClickListener {
            val fragment = CreateNotificationFragment()
            val manager = requireActivity().supportFragmentManager
            val transaction = manager.beginTransaction()
            transaction.add(R.id.main_frame, fragment)
            transaction.addToBackStack("CUSTOM_NOTIF_FRAGMENT")
            transaction.commit()
        })

        ////////////////////////////////////////////////////////////////////////

        //OnLongClickListener
        handleUsersIV.setOnLongClickListener(View.OnLongClickListener {
            Toast.makeText(context, "Handle users", Toast.LENGTH_SHORT).show()
            return@OnLongClickListener true
        })
        handleAttIV.setOnLongClickListener(View.OnLongClickListener {
            Toast.makeText(context, "Handle attendance", Toast.LENGTH_SHORT).show()
            return@OnLongClickListener true
        })
        handleRemindersIV.setOnLongClickListener(View.OnLongClickListener {
            Toast.makeText(context, "Handle reminders", Toast.LENGTH_SHORT).show()
            return@OnLongClickListener true
        })
        notificationIV.setOnLongClickListener(View.OnLongClickListener {
            Toast.makeText(context, "Send custom notification", Toast.LENGTH_SHORT).show()
            return@OnLongClickListener true
        })

        handleGroupsIV.setOnLongClickListener(View.OnLongClickListener {
            Toast.makeText(context, "Handle groups", Toast.LENGTH_SHORT).show()
            return@OnLongClickListener true
        })

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

    override fun onClick(post: Posts) {
        this.post = post

    }

    fun showComments(postKey: String) {
        val commentsFragment = CommentsBottomFragment(postKey, height)
        commentsFragment.show(requireActivity().supportFragmentManager, "COMMENTS_BOTTOM_SHEETS")
    }

    fun showSettingsDialog() {
        dialog.show(requireActivity().supportFragmentManager, "POSTS_BOTTOM_SHEETS")
    }

    private fun editPost() {
        val fragment = CreatePostFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val bundle = Bundle()
        bundle.putParcelable("Editing_Post", post)
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
        builder.setTitle("Delete post?")
        builder.setMessage("Are you sure you want to delete this post?")
        builder.setPositiveButton("Yes") { _, _ ->
            if (hasConnection) {
                viewModel.deletePost(post.ID.toString())
            }
        }
        builder.setNegativeButton("No") { _, _ -> }
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