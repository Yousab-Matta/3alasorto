package com.example.alasorto

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
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
class ProfileFragment : Fragment(), ProfilePostsAdapter.OnClickListener {
    private val postsList = ArrayList<Posts>()

    private lateinit var handleUsersIV: ImageView
    private lateinit var handleAttIV: ImageView
    private lateinit var handlePostsIV: ImageView
    private lateinit var groupChatIV: ImageView
    private lateinit var userIV: ImageView
    private lateinit var progressView: PercentageChartView
    private lateinit var controlLL: LinearLayout
    private lateinit var postsRV: RecyclerView
    private lateinit var viewModel: AppViewModel
    private lateinit var nameTV: TextView
    private lateinit var internetCheck: InternetCheck
    private lateinit var adapter: ProfilePostsAdapter
    private var user: Users? = null
    private var hasConnection = false

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        handleUsersIV = view.findViewById(R.id.iv_user)
        handlePostsIV = view.findViewById(R.id.iv_create_post)
        handleAttIV = view.findViewById(R.id.iv_create_att)
        groupChatIV = view.findViewById(R.id.iv_group_chat)
        nameTV = view.findViewById(R.id.tv_profile_name)
        userIV = view.findViewById(R.id.iv_profile_image)
        progressView = view.findViewById(R.id.profile_progress_view)
        controlLL = view.findViewById(R.id.ll_profile_control)
        postsRV = view.findViewById(R.id.rv_profile_posts)

        postsRV.layoutManager = LinearLayoutManager(requireContext())

        //Initialize view model (passing activity as context)
        viewModel = ViewModelProvider(requireActivity())[AppViewModel::class.java]

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
            user = viewModel.currentUserMLD.value
        }

        //Check account access type and hide control buttons if not admin
        if (user!!.Access == "User") {
            controlLL.visibility = View.GONE
        }

        adapter = ProfilePostsAdapter(postsList, user!!, this)
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
            transaction.addToBackStack(null)
            transaction.commit()
        })

        handleAttIV.setOnClickListener(View.OnClickListener {
            val fragment = AttendanceHistoryFragment()
            val manager = requireActivity().supportFragmentManager
            val transaction = manager.beginTransaction()
            transaction.add(R.id.main_frame, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        })

        handlePostsIV.setOnClickListener(View.OnClickListener {
            val fragment = HomeFragment()
            val manager = requireActivity().supportFragmentManager
            val transaction = manager.beginTransaction()
            transaction.add(R.id.main_frame, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        })

        groupChatIV.setOnClickListener(View.OnClickListener {
            val fragment = GroupChatFragment()
            val manager = requireActivity().supportFragmentManager
            val transaction = manager.beginTransaction()
            transaction.add(R.id.main_frame, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        })
        ////////////////////////////////////////////////////////////////////////

        //OnLongClickListener
        handleUsersIV.setOnLongClickListener(View.OnLongClickListener {
            Toast.makeText(context, "Handle Users", Toast.LENGTH_SHORT).show()
            return@OnLongClickListener true
        })
        handleAttIV.setOnLongClickListener(View.OnLongClickListener {
            Toast.makeText(context, "Handle Attendance", Toast.LENGTH_SHORT).show()
            return@OnLongClickListener true
        })
        handlePostsIV.setOnLongClickListener(View.OnLongClickListener {
            Toast.makeText(context, "Handle Posts", Toast.LENGTH_SHORT).show()
            return@OnLongClickListener true
        })

        return view
    }

    override fun onClick(post: Posts) {
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().supportFragmentManager.popBackStack()
    }
}