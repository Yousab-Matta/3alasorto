package com.example.alasorto

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.widget.ViewPager2
import com.example.alasorto.adapters.CreatePostFragmentAdapter
import com.example.alasorto.dataClass.Post
import com.yalantis.ucrop.UCropActivity.*

@Suppress("DEPRECATION")
class CreatePostFragment : Fragment(R.layout.fragment_create_post) {
    private lateinit var headerTV: TextView
    private lateinit var finishBtn: ImageButton
    private lateinit var createPostBtn: Button
    private lateinit var createPollBtn: Button
    private lateinit var postTypeLL: LinearLayout
    private lateinit var viewPager: ViewPager2
    private var isNewPost = true
    private var post: Post? = null

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

        //Get edit post data
        val args = this.arguments
        if (args != null) {
            isNewPost = args.getBoolean("IsNewPost")
            if (!isNewPost) {
                headerTV.text = getString(R.string.edit_post)
                postTypeLL.visibility = View.GONE
                post = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    args.getParcelable("EDITING_POST", Post::class.java)
                } else {
                    args.getParcelable("EDITING_POST")
                }
            }
        }

        viewPager.adapter =
            CreatePostFragmentAdapter(
                childFragmentManager,
                lifecycle,
                isNewPost,
                post
            )

        if (!isNewPost) {
            if (post != null) {
                if (post!!.postType == "Post" || post!!.postType == "VideoPost") {
                    switchFragment(0, false)
                } else {
                    switchFragment(1, false)
                }
            }
        }

        createPostBtn.setOnClickListener(View.OnClickListener {
            switchFragment(0, true)
        })

        createPollBtn.setOnClickListener(View.OnClickListener {
            switchFragment(1, true)
        })

        finishBtn.setOnClickListener(View.OnClickListener {
            if (childFragmentManager.fragments.size > 1) {

                val fragment = childFragmentManager.fragments[viewPager.currentItem]
                if (fragment is PostFragment) {
                    fragment.uploadPostMedia()
                } else if (fragment is PollFragment) {
                    fragment.uploadPoll()
                }
            } else {
                val fragment = childFragmentManager.fragments[0]
                if (fragment is PostFragment) {
                    fragment.uploadPostMedia()
                } else if (fragment is PollFragment) {
                    fragment.uploadPoll()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().supportFragmentManager.popBackStack(
                        "HANDLE_POSTS_FRAGMENT",
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                    this.isEnabled = false
                }
            })
    }

    private fun switchFragment(position: Int, smoothScroll: Boolean) {
        if (position == 0) {
            createPostBtn.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.fragment_bg)
            createPollBtn.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.btn_color)
        } else {
            createPostBtn.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.btn_color)
            createPollBtn.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.fragment_bg)
        }
        createPostBtn.requestLayout()
        createPollBtn.requestLayout()
        viewPager.setCurrentItem(position, smoothScroll)
    }
}