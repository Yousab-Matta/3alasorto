package com.example.alasorto

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color.TRANSPARENT
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.View.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alasorto.adapters.CommentsAdapter
import com.example.alasorto.adapters.OnCommentClick
import com.example.alasorto.dataClass.Comments
import com.example.alasorto.dataClass.Users
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CommentsFragment : Fragment(), OnCommentClick {
    private lateinit var commentsRV: RecyclerView
    private lateinit var commentET: EditText
    private lateinit var makeCommentBtn: ImageButton
    private lateinit var clearTextBtn: ImageButton
    private lateinit var cancelEditBtn: ImageButton
    private lateinit var editRL: RelativeLayout
    private lateinit var commentRL: RelativeLayout
    private lateinit var viewModel: AppViewModel
    private lateinit var postKey: String
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var internetCheck: InternetCheck
    private lateinit var comment: Comments
    private var isEditing = false
    private var hasConnection = false

    private val commentsList = ArrayList<Comments>()
    private val commentsOwnersList = ArrayList<Users>()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comments, container, false)

        val args = this.arguments
        if (args != null) {
            postKey = args.getString("PostKey").toString()
        }

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
            if (it) {
                viewModel.getPostComments(postKey)
            }
        }

        //Initialize widgets
        commentsRV = view.findViewById(R.id.rv_post_comments)
        commentET = view.findViewById(R.id.et_post_comment)
        makeCommentBtn = view.findViewById(R.id.btn_post_comment)
        clearTextBtn = view.findViewById(R.id.btn_clear_text)
        cancelEditBtn = view.findViewById(R.id.btn_cancel_edit)
        editRL = view.findViewById(R.id.rl_edit)
        commentRL = view.findViewById(R.id.comments_rl)

        //Set LinearLayout to RV
        commentsRV.layoutManager = LinearLayoutManager(context)

        commentsAdapter = CommentsAdapter(commentsList, commentsOwnersList, this)
        commentsRV.adapter = commentsAdapter

        //Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[AppViewModel::class.java]

        viewModel.commentsMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                commentsList.clear()
                commentsList.addAll(it)
                commentsList.sortBy { it1 -> it1.Date }
                commentsAdapter.notifyDataSetChanged()
                for (comment in commentsList) {
                    viewModel.getUserById(comment.OwnerID.toString())
                }
                commentsRV.smoothScrollToPosition(commentsList.size)
            }
        })

        viewModel.otherUserDataMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                if (!commentsOwnersList.contains(it)) {
                    commentsOwnersList.add(it)
                    commentsAdapter.notifyDataSetChanged()
                }
            }
        })

        commentET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0.toString().isNotEmpty()) {
                    clearTextBtn.visibility = VISIBLE
                } else {
                    clearTextBtn.visibility = INVISIBLE
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })

        //Button to cancel editing comment
        cancelEditBtn.setOnClickListener(OnClickListener {
            showEditLayout(false)
            isEditing = false
        })

        //Button to clear all text in ET
        clearTextBtn.setOnClickListener(OnClickListener {
            commentET.text.clear()
        })

        //Button to create comment
        makeCommentBtn.setOnClickListener(OnClickListener
        {
            val commentText = commentET.text.toString().trim()
            if (commentText.isNotEmpty() && viewModel.currentUserMLD.value != null) {
                if (hasConnection) {
                    if (!isEditing) {
                        viewModel.createPostComment(
                            postKey,
                            commentText,
                            viewModel.currentUserMLD.value?.Phone.toString()
                        )
                        viewModel.getPostComments(postKey)
                        commentET.text.clear()
                    } else {
                        viewModel.editComment(comment.CommentID.toString(), commentText)
                        isEditing = false
                        showEditLayout(false)
                        commentET.text.clear()
                    }
                }
            }
        })

        /*Firebase.firestore.collection("Comments").addSnapshotListener { _, _ ->
            if (hasConnection) {
                viewModel.getPostComments(postKey)
            }
        }*/

        requireActivity().onBackPressedDispatcher.addCallback(
            requireActivity(), object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    commentsList.clear()
                    commentsAdapter.notifyDataSetChanged()
                    requireActivity().supportFragmentManager.popBackStack(
                        "Comments",
                        0
                    )
                }
            })

        return view
    }

    private fun showDialog() {
        //Checks if its comment owner OR admin
        if (comment.OwnerID == viewModel.currentUserMLD.value!!.Phone
            || viewModel.currentUserMLD.value!!.Access != "User"
        ) {
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.bottom_sheet_posts)

            val editCommentLL: LinearLayout = dialog.findViewById(R.id.ll_edit_comment)
            val deleteCommentLL: LinearLayout = dialog.findViewById(R.id.ll_delete_comment)

            editCommentLL.setOnClickListener(OnClickListener {
                commentET.setText(comment.Comment)
                commentET.setSelection(commentET.length())//placing cursor at the end of the text
                showEditLayout(true)
                isEditing = true
                dialog.dismiss()
            })

            deleteCommentLL.setOnClickListener(OnClickListener {
                viewModel.deletePostComment(comment.CommentID.toString())
                dialog.dismiss()
            })

            dialog.show()
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.window?.setBackgroundDrawable(ColorDrawable(TRANSPARENT))
            dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
            dialog.window?.setGravity(Gravity.BOTTOM)
        }
    }

    override fun onClick(comment: Comments) {
        this.comment = comment
        if (comment.OwnerID.toString() == viewModel.currentUserMLD.value!!.Phone.toString())
            showDialog()
    }

    private fun showEditLayout(isShowing: Boolean) {
        val height = editRL.height
        if (isShowing) {
            val animator = ObjectAnimator.ofFloat(editRL, "translationY", -height.toFloat())
            animator.duration = 200
            animator.start()
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                commentRL.setBackgroundResource(R.color.grey2)
            }, 100)
        } else {
            val animator = ObjectAnimator.ofFloat(editRL, "translationY", height.toFloat())
            animator.duration = 200
            animator.start()
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                commentRL.setBackgroundResource(R.drawable.round_top_corners)
            }, 100)
        }
    }
}