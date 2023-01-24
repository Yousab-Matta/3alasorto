package com.example.alasorto

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alasorto.adapters.PostsAdapter
import com.example.alasorto.dataClass.Posts
import com.example.alasorto.dataClass.Users
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeFragment : Fragment(), PostsAdapter.OnClickListener,
    PostsBottomSheet.OnPostSettingsItemClick {
    private val postsList = ArrayList<Posts>()
    private val allUsersList = ArrayList<Users>()
    private val dialog = PostsBottomSheet(this)
    private lateinit var postsRV: RecyclerView

    private lateinit var addPostBtn: ImageButton
    private lateinit var userImageIV: ImageView
    private lateinit var viewModel: AppViewModel
    private lateinit var postsAdapter: PostsAdapter
    private lateinit var internetCheck: InternetCheck
    private lateinit var post: Posts
    private var height: Int = 0
    private var currentUser: Users? = null
    private var hasConnection = false

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Inflate view
        return layoutInflater.inflate(R.layout.fragment_home, container, false)
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

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
            if (it) {
                viewModel.getPosts()
            }
        }

        //Get current user data
        currentUser = (activity as MainActivity).getCurrentUser()

        //Get all users
        allUsersList.addAll((activity as MainActivity).getAllUsers())

        //Initialize views
        postsRV = view.findViewById(R.id.rv_admin_posts)
        addPostBtn = view.findViewById(R.id.btn_add_admin_post)
        userImageIV = view.findViewById(R.id.iv_home_user)

        //Set linear layout manager to recyclerView
        postsRV.layoutManager = LinearLayoutManager(context)

        postsRV.addItemDecoration(SpacingItemDecorator(30))

        //Initialize ViewModel
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        //set user image to imageView
        Glide.with(requireContext()).load(currentUser!!.ImageLink.toString())
            .into(userImageIV)

        //Initialize adapter and set it to recyclerview
        postsAdapter = PostsAdapter(postsList, allUsersList, this, this)
        postsRV.adapter = postsAdapter

        //Observe Posts List
        viewModel.postsMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                postsList.clear()
                postsList.addAll(it)
                postsAdapter.notifyDataSetChanged()
            }
        })

        //OnClick Listeners
        addPostBtn.setOnClickListener(View.OnClickListener {
            val fragment = CreatePostFragment()
            val manager = requireActivity().supportFragmentManager
            val transaction = manager.beginTransaction()
            val bundle = Bundle()
            bundle.putBoolean("IsNewPost", true)
            fragment.arguments = bundle
            transaction.add(R.id.main_frame, fragment)
            transaction.addToBackStack("HANDLE_POSTS_FRAGMENT")
            transaction.commit()
        })

        userImageIV.setOnClickListener(View.OnClickListener {
            goToProfileFragment(currentUser!!)
        })

        //Check changes in posts
        Firebase.firestore.collection("Posts").addSnapshotListener { _, _ ->
            if (hasConnection) {
                viewModel.getPosts()
            }
        }
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

    fun goToProfileFragment(user: Users?) {
        if (user != null) {
            val profilePostsList = ArrayList<Posts>()
            for (post in postsList) {
                if (post.OwnerID == user.Phone) {
                    profilePostsList.add(post)
                }
            }

            val manager = requireActivity().supportFragmentManager
            val transaction = manager.beginTransaction()
            val fragment = ProfileFragment()
            val args = Bundle()
            args.putParcelable("Profile_User", user)
            args.putParcelableArrayList("Profile_Posts", profilePostsList)
            fragment.arguments = args
            transaction.add(R.id.main_frame, fragment)
            transaction.addToBackStack("PROFILE_FRAGMENT")
            transaction.commit()
        }
    }

    override fun onPostSettingsClick(case: String) {
        if (case == "Edit"){
            editPost()
        }else if (case == "Delete"){
            deletePost()
        }
    }
}