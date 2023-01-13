package com.example.alasorto

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
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

class HomeFragment : Fragment(), PostsAdapter.OnClickListener {
    private val postsList = ArrayList<Posts>()
    private val postsOwnersList = ArrayList<Users>()

    private lateinit var postsRV: RecyclerView
    private lateinit var addPostBtn: ImageButton
    private lateinit var userImageIV: ImageView
    private lateinit var postTypeSpinner: Spinner
    private lateinit var viewModel: AppViewModel
    private lateinit var postsAdapter: PostsAdapter
    private lateinit var internetCheck: InternetCheck
    private lateinit var post: Posts
    private var currentUser: Users? = null

    private var hasConnection = false

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        currentUser = (activity as MainActivity).getCurrentUser()

        val view = layoutInflater.inflate(R.layout.fragment_posts, container, false)
        postsRV = view.findViewById(R.id.rv_admin_posts)
        addPostBtn = view.findViewById(R.id.btn_add_admin_post)
        postTypeSpinner = view.findViewById(R.id.spinner_post_type)
        userImageIV = view.findViewById(R.id.iv_home_user)

        postsRV.layoutManager = LinearLayoutManager(context)

        //Initialize ViewModel
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        Glide.with(requireContext()).load(currentUser!!.ImageLink.toString())
            .into(userImageIV)

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
            if (it) {
                viewModel.getPosts()
            }
        }

        postsAdapter = PostsAdapter(postsList, postsOwnersList, this, this)
        postsRV.adapter = postsAdapter

        //Observe Posts List
        viewModel.postsMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                postsList.clear()
                postsList.addAll(it)
                for (post in postsList) {
                    if (hasConnection) {
                        viewModel.getUserById(post.OwnerID.toString())
                    }
                }
                postsAdapter.notifyDataSetChanged()
            }
        })

        viewModel.otherUserDataMLD.observe(this.viewLifecycleOwner, Observer {
            if (!postsOwnersList.contains(it)) {
                postsOwnersList.add(it)
                postsAdapter.notifyDataSetChanged()
            }
        })

        addPostBtn.setOnClickListener(View.OnClickListener {
            val fragment = CreatePostFragment()
            val manager = requireActivity().supportFragmentManager
            val transaction = manager.beginTransaction()
            val bundle = Bundle()
            bundle.putBoolean("IsNewPost", true)
            fragment.arguments = bundle
            transaction.add(R.id.main_frame, fragment)
            transaction.addToBackStack(null)
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

        return view
    }

    override fun onClick(post: Posts) {
        this.post = post
    }

    fun goToComments() {
        val fragment = CommentsFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        val bundle = Bundle()
        bundle.putString("PostKey", post.ID)
        fragment.arguments = bundle
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("Comments")
        transaction.commit()
    }

    fun showDialog() {
        if (currentUser!!.Phone.toString() == post.OwnerID
            || currentUser!!.Access != "User"
        ) {
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.bottom_sheet_posts)

            val editCommentLL: LinearLayout = dialog.findViewById(R.id.ll_edit_comment)
            val deleteCommentLL: LinearLayout = dialog.findViewById(R.id.ll_delete_comment)

            editCommentLL.setOnClickListener(View.OnClickListener {
                val fragment = CreatePostFragment()
                val manager = requireActivity().supportFragmentManager
                val transaction = manager.beginTransaction()
                val bundle = Bundle()
                bundle.putParcelable("Editing_Post", post)
                bundle.putBoolean("IsNewPost", false)
                fragment.arguments = bundle
                transaction.add(R.id.main_frame, fragment)
                transaction.addToBackStack(null)
                transaction.commit()
                dialog.dismiss()
            })

            deleteCommentLL.setOnClickListener(View.OnClickListener {
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
            })

            dialog.show()
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
            dialog.window?.setGravity(Gravity.BOTTOM)
        }
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
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }
}