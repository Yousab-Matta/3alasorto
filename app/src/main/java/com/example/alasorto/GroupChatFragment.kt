package com.example.alasorto

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alasorto.adapters.GroupChatAdapter
import com.example.alasorto.dataClass.GroupChat
import com.example.alasorto.dataClass.Users
import com.example.alasorto.utils.ISwipeControllerActions
import com.example.alasorto.utils.InternetCheck
import com.example.alasorto.utils.SwipeController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class GroupChatFragment : Fragment() {
    private val groupChatList = ArrayList<GroupChat>()
    private val chatOwnersList = ArrayList<Users>()

    private lateinit var viewModel: AppViewModel
    private lateinit var groupChatRV: RecyclerView
    private lateinit var messageET: EditText
    private lateinit var sendMessageBtn: ImageButton
    private lateinit var clearTextBtn: ImageButton
    private lateinit var adapter: GroupChatAdapter
    private lateinit var internetCheck: InternetCheck
    private var hasConnection = false
    private var currentUser: Users? = null

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_chat, container, false)
        view.isClickable = true

        currentUser = (activity as MainActivity).getCurrentUser()

        //Initialize Widgets
        groupChatRV = view.findViewById(R.id.rv_group_chat)
        messageET = view.findViewById(R.id.et_group_chat)
        sendMessageBtn = view.findViewById(R.id.btn_gc_send)
        clearTextBtn = view.findViewById(R.id.btn_gc_clear_text)

        //Initialize adapter
        adapter = GroupChatAdapter(groupChatList, chatOwnersList, currentUser?.Phone.toString())

        //setLayoutManager & adapter to RV
        groupChatRV.layoutManager = LinearLayoutManager(requireContext())
        groupChatRV.adapter = adapter

        //Initialize VM
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
            if (it) {
                viewModel.getGroupChat()
            }
        }

        //Observe group chat
        viewModel.groupChatMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                for (chat in it) {
                    viewModel.getUserById(chat.ownerID.toString())
                    if (!groupChatList.contains(chat)) {
                        groupChatList.add(chat)
                        adapter.notifyItemInserted(groupChatList.size - 1)
                    }
                }
            }
        })

        viewModel.otherUserDataMLD.observe(this.viewLifecycleOwner, Observer {
            if (!chatOwnersList.contains(it)) {
                chatOwnersList.add(it)
                adapter.notifyItemRangeChanged(0, groupChatList.size)
            }
        })

        sendMessageBtn.setOnClickListener(View.OnClickListener {
            if (hasConnection && currentUser != null && messageET.text.toString().isNotEmpty()) {
                viewModel.sendGroupChatMessage(
                    currentUser!!.Phone.toString(),
                    messageET.text.toString().trim(),
                    "Message", requireActivity().contentResolver, null
                )
                messageET.text.clear()
            }
        })

        val controller = SwipeController(requireContext(), object : ISwipeControllerActions {
            override fun onSwipePerformed(position: Int) {
                messageET.setText(adapter.getItemById(position).message)
            }
        })

        val itemTouchHelper = ItemTouchHelper(controller)
        itemTouchHelper.attachToRecyclerView(groupChatRV)

        //Check changes in posts
        Firebase.firestore.collection("GroupChat").addSnapshotListener { _, _ ->
            if (hasConnection) {
                viewModel.getGroupChat()
            }
        }

        return view
    }
}