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
import com.etebarian.meowbottomnavigation.MeowBottomNavigation
import com.example.alasorto.adapters.CreatePostFragmentAdapter
import com.example.alasorto.dataClass.Post
import com.yalantis.ucrop.UCropActivity.*

@Suppress("DEPRECATION")
class CreatePostFragment : Fragment(R.layout.fragment_create_post) {
    private lateinit var headerTV: TextView
    private lateinit var finishBtn: ImageButton
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: MeowBottomNavigation
    private var isNewPost = true
    private var post: Post? = null

    private var groupId = ""
    private var collectionPath = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        headerTV = view.findViewById(R.id.tv_create_event_title)
        finishBtn = view.findViewById(R.id.ib_create_post)
        viewPager = view.findViewById(R.id.create_post_pager)
        bottomNavigation = view.findViewById(R.id.meowBottomNavigation)

        //Disable swipe animation for viewPager
        viewPager.isUserInputEnabled = false

        //Get edit post data
        val args = this.arguments
        if (args != null) {
            collectionPath = args.getString("COLLECTION_PATH", "")
            groupId = args.getString("GROUP_ID", "")

            isNewPost = args.getBoolean("IsNewPost")
            if (!isNewPost) {

                headerTV.text = getString(R.string.edit_post)
                post = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    args.getParcelable("EDITING_POST", Post::class.java)
                } else {
                    args.getParcelable("EDITING_POST")
                }

                if (post != null) {
                    if (post!!.postType == "Post") {
                        viewPager.setCurrentItem(0, true)
                    } else {
                        viewPager.setCurrentItem(1, true)
                    }
                }
            }
        }

        setBottomNavigationItems()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNavigation.show(position, true)
            }
        })

        viewPager.adapter =
            CreatePostFragmentAdapter(
                childFragmentManager,
                lifecycle,
                collectionPath,
                groupId,
                isNewPost,
                post
            )

        if (!isNewPost) {
            if (post != null) {
                if (post!!.postType == "Post") {
                    viewPager.setCurrentItem(0, false)
                } else {
                    viewPager.setCurrentItem(1, false)
                }
            }
        }

        bottomNavigation.setOnClickMenuListener {
            if (isNewPost) {
                viewPager.setCurrentItem(it.id, true)
            }
        }

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

    private fun setBottomNavigationItems() {
        bottomNavigation.add(MeowBottomNavigation.Model(0, R.drawable.ic_event))
        bottomNavigation.add(MeowBottomNavigation.Model(1, R.drawable.ic_poll))
    }
}