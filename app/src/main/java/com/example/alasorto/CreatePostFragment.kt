package com.example.alasorto

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.alasorto.adapters.CreatePostFragmentAdapter
import com.example.alasorto.dataClass.PollPost
import com.example.alasorto.dataClass.Posts
import com.example.alasorto.dataClass.VideoPost
import com.yalantis.ucrop.UCropActivity.*

@Suppress("DEPRECATION")
class CreatePostFragment : Fragment() {
    private lateinit var headerTV: TextView
    private lateinit var finishBtn: ImageButton
    private lateinit var createPostBtn: Button
    private lateinit var createPollBtn: Button
    private lateinit var postTypeLL: LinearLayout
    private lateinit var viewPager: ViewPager2
    private var isNewPost = true
    private var post: Posts? = null
    private var videoPost: VideoPost? = null
    private var pollPost: PollPost? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_create_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        headerTV = view.findViewById(R.id.tv_create_event_title)
        finishBtn = view.findViewById(R.id.ib_create_post)
        viewPager = view.findViewById(R.id.create_post_pager)
        createPostBtn = view.findViewById(R.id.btn_create_post)
        createPollBtn = view.findViewById(R.id.btn_create_poll)
        postTypeLL = view.findViewById(R.id.ll_post_type_select)

        //Disable swipe animation for viewPager
        viewPager.isUserInputEnabled = false

        //Set color of button representing current fragment
        createPostBtn.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.secondary_color)

        //Get edit post data
        val args = this.arguments
        if (args != null) {
            isNewPost = args.getBoolean("IsNewPost")
            if (!isNewPost) {
                headerTV.text = getString(R.string.edit_post)
                postTypeLL.visibility = View.GONE
                post = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    args.getParcelable("EDITING_POST", Posts::class.java)
                } else {
                    args.getParcelable("EDITING_POST")
                }
                pollPost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    args.getParcelable("EDITING_POLL_POST", PollPost::class.java)
                } else {
                    args.getParcelable("EDITING_POLL_POST")
                }
                videoPost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    args.getParcelable("EDITING_VIDEO_POST", VideoPost::class.java)
                } else {
                    args.getParcelable("EDITING_VIDEO_POST")
                }
            }
        }

        viewPager.adapter =
            CreatePostFragmentAdapter(
                childFragmentManager,
                lifecycle,
                isNewPost,
                post,
                pollPost,
                videoPost
            )

        if (!isNewPost) {
            if (post != null || videoPost != null) {
                viewPager.setCurrentItem(0, false)
            } else if (pollPost != null && post == null && videoPost == null) {
                viewPager.setCurrentItem(1, false)
            }
        }

        createPostBtn.setOnClickListener(View.OnClickListener {
            switchFragment(0)
        })

        createPollBtn.setOnClickListener(View.OnClickListener {
            switchFragment(1)
        })

        finishBtn.setOnClickListener(View.OnClickListener {
            Log.d("VIEW_PAGER", childFragmentManager.fragments.size.toString())
            if (childFragmentManager.fragments.size > 1) {
                val fragment = childFragmentManager.fragments[viewPager.currentItem]
                if (fragment is PostFragment) {
                    fragment.createPost()
                } else if (fragment is PollFragment) {
                    fragment.uploadPoll()
                }
            } else {
                val fragment = childFragmentManager.fragments[0]
                if (fragment is PostFragment) {
                    fragment.createPost()
                } else if (fragment is PollFragment) {
                    fragment.uploadPoll()
                }
            }
        })
    }

    private fun switchFragment(position: Int) {
        viewPager.setCurrentItem(position, true)
        if (viewPager.currentItem == 0) {
            createPostBtn.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.secondary_color)
            createPollBtn.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.btn_color)
        } else {
            createPostBtn.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.btn_color)
            createPollBtn.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.secondary_color)
        }
    }
}