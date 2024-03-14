package com.example.alasorto

import android.Manifest.permission.*
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.*
import android.provider.OpenableColumns
import android.view.View
import android.view.View.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.Transformation
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.removeItemAt
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devlomi.record_view.OnRecordListener
import com.devlomi.record_view.RecordButton
import com.devlomi.record_view.RecordView
import com.example.alasorto.adapters.ChatAdapter
import com.example.alasorto.adapters.ChatSelectedMediaAdapter
import com.example.alasorto.chatHistoryDatabase.ChatHistory
import com.example.alasorto.chatHistoryDatabase.ChatHistoryViewModel
import com.example.alasorto.dataClass.*
import com.example.alasorto.dataClass.Message
import com.example.alasorto.notification.Data
import com.example.alasorto.notification.NotificationModel
import com.example.alasorto.pendingMessagesDatabase.PendingMessage
import com.example.alasorto.pendingMessagesDatabase.PendingMessageViewModel
import com.example.alasorto.utils.*
import com.example.alasorto.viewModels.AppViewModel
import com.example.alasorto.viewModels.ChatViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.*

class ChatFragment : Fragment(R.layout.fragment_chat) {

    companion object {
        private const val RESULT_OK = -1
        private const val RESULT_CANCEL = 0
    }

    private val openImageAnim: Animation by lazy {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.expand_image
        )
    }

    private val closeImageAnim: Animation by lazy {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.close_image
        )
    }

    private val usersList = ArrayList<UserData>()
    private val chatHistoryList = ArrayList<ChatHistory>()
    private val chatViewModel: ChatViewModel by viewModels()
    private val pendingMessagesVM: PendingMessageViewModel by viewModels()
    private val chatHistoryVM: ChatHistoryViewModel by viewModels()
    private val appViewModel: AppViewModel by viewModels()
    private val initDate = Date()
    private val currentUserId = Firebase.auth.currentUser?.phoneNumber.toString()
    private val pendingMediaList = ArrayList<MediaData>()
    private val selectedMessagesList = ArrayList<Message>()

    private lateinit var chatRV: RecyclerView
    private lateinit var selectedMediaRV: RecyclerView
    private lateinit var mentionsRV: MentionRecyclerView
    private lateinit var replyLayout: ConstraintLayout
    private lateinit var chatLayout: ConstraintLayout
    private lateinit var headerLayout: ConstraintLayout
    private lateinit var headerImage: ImageView
    private lateinit var headerTitle: TextView
    private lateinit var replyTextTV: TextView
    private lateinit var messageET: MentionEditText
    private lateinit var clearReplyBtn: ImageButton
    private lateinit var sendMessageBtn: ImageButton
    private lateinit var clearTextBtn: ImageButton
    private lateinit var uploadMediaBtn: ImageButton
    private lateinit var uploadFileBtn: ImageButton
    private lateinit var messageOptionsBtn: ImageButton
    private lateinit var recordBtn: RecordButton
    private lateinit var recordView: RecordView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var selectedMediaAdapter: ChatSelectedMediaAdapter
    private lateinit var internetCheck: InternetCheck
    private lateinit var voiceRecorder: VoiceRecorder
    private lateinit var controller: SwipeController
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var replyMessage: Message //The message that is a reply in clicked message

    private var chatId: String = ""
    private var collectionPath: String = ""
    private var replyMessageId: String = ""
    private var voiceNoteId: String = ""
    private var isAnonymous: Boolean = true
    private var getMoreChat: Boolean = true
    private var isGroupChat: Boolean = false
    private var group: Group? = null
    private var messagesList = ArrayList<Message>()

    private var audioPath: String = ""

    private var hasConnection = false

    //Check if snapshot listener is enabled to avoid repetitive messages
    private var listenToSnapShot = false

    //Check if user reaches top or bottom of the RV to get newer or older messages
    private var canScrollVertically = true

    //Doesn't load start of chat again when onResume or ..... are called
    private var isChatLoadedFirstTime = false

    //Checks if the message is a reply to anotherMessage
    private var isReply = false

    //Checks if there is no more OLDER chat
    private var chatReachedTop = false

    //Checks if there is no more NEWER chat
    private var chatReachedBottom = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        initializeWidgets(view)

        mentionsRV.setAdapterData(::selectMentionedUser)
        messageET.initCommonFunctions(::showRecyclerView, ::setRecyclerViewData)
        messageET.initChatFunctions(::editTextStatus)
        recordBtn.setRecordView(recordView)
        initializeSelectedMediaRecyclerview()

        getArgs()

        audioPath = requireContext().externalCacheDir!!.absolutePath

        initializeChatAdapter()

        //Create LayoutManager
        linearLayoutManager = object : LinearLayoutManager(requireContext()) {
            //Set can scroll vertically or no to disable scroll while changing vc progress
            override fun canScrollVertically(): Boolean {
                return canScrollVertically
            }
        }

        //linearLayoutManager.stackFromEnd = true
        linearLayoutManager.reverseLayout

        chatRV.layoutManager = linearLayoutManager
        chatRV.addItemDecoration(LinearSpacingItemDecorator(20))
        chatRV.adapter = chatAdapter

        chatRV.addOnScrollListener(activeScrollListener)

        //getChatHistory
        /*chatHistoryVM.readAllData.observe(this.viewLifecycleOwner, Observer {
            val messagesFromHistory = ArrayList<Message>()
            for (item in it) {
                messagesFromHistory.add(item.message)

                if (item.message.mediaData != null) {
                    val mediaLink = item.message.mediaData!!.mediaLink
                    if (mediaLink != null && mediaLink.isNotEmpty() && item.message.messageType == "VoiceNote") {
                        viewModel.getVoiceNote(
                            item.message.mediaData!!.mediaLink!!,
                            item.messageKey,
                            requireContext()
                        )
                    }
                }

                val altMessageList =
                    HandleMessages(messagesList, messagesFromHistory, adapter).handleMessagesList()
                messagesList = altMessageList

                chatHistoryList.add(item)
            }
        })*/

        //Check internet connection
        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
            if (it) {
                if (!isChatLoadedFirstTime) {
                    chatViewModel.getChat(collectionPath, chatId)
                    isChatLoadedFirstTime = true
                }
            }
        }

        chatViewModel.audioFileMLD.observe(this.viewLifecycleOwner, Observer {
            val key = it.name.removeSuffix(".3gp")
            val audioMessage = messagesList.firstOrNull { it1 -> it1.messageId == key }
            if (audioMessage?.mediaData != null) {
                messagesList.remove(audioMessage)
                audioMessage.mediaData[0].media = it.path
                messagesList.add(audioMessage)
                messagesList.sortBy { it2 -> it2.date }
                chatAdapter.notifyItemChanged(messagesList.indexOf(audioMessage))
            }
        })

        chatViewModel.fileMLD.observe(this.viewLifecycleOwner, Observer {
            val fileName = it.nameWithoutExtension
            val fileExtension = it.extension
            //Get all messages where media name == file name && media extension == file extension
            val filteredMessageList =
                messagesList.filter { it1 -> it1.message == fileName && it1.mediaData[0].type == fileExtension }
            if (filteredMessageList.isNotEmpty()) {
                for (message in filteredMessageList) {
                    //Change media link with media for each message in list so that the adapter indicates that media is already downloaded,  hence hide download button
                    message.mediaData[0].media = it.path
                    chatAdapter.notifyItemChanged(messagesList.indexOf(message))
                }
            }
        })

        //Observe chat
        chatViewModel.chatMLD.observe(this.viewLifecycleOwner, Observer {
            listenToSnapShot = true
            if (it != null) {

                val altMessageList =
                    HandleMessages(messagesList, it, chatAdapter).handleMessagesList()
                messagesList = altMessageList

                getVoiceNote()
                checkIfFileExists()

                chatRV.scrollToPosition(messagesList.size - 1)
                chatReachedTop = it.size < 10
                getMoreChat = true

                getUsers(messagesList)
            }
            chatViewModel.chatMLD.removeObservers(this.viewLifecycleOwner)
        })

        //Observe old chats when scrolled up
        chatViewModel.olderChatMld.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {

                val altMessageList =
                    HandleMessages(messagesList, it, chatAdapter).handleMessagesList()
                messagesList = altMessageList

                getVoiceNote()
                checkIfFileExists()

                getUsers(messagesList)

                chatReachedTop = it.size < 10
                getMoreChat = true
            }
        })

        //Observe new chats when scrolled down
        chatViewModel.newerChatMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                val altMessageList =
                    HandleMessages(messagesList, it, chatAdapter).handleMessagesList()
                messagesList = altMessageList

                getUsers(messagesList)

                getVoiceNote()
                checkIfFileExists()

                chatReachedBottom = it.size < 10
                getMoreChat = true
            }
        })

        //Observe newly added chats
        chatViewModel.newMessagesMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                val altMessageList =
                    HandleMessages(messagesList, it, chatAdapter).handleMessagesList()
                messagesList = altMessageList

                getUsers(messagesList)

                getVoiceNote()
                checkIfFileExists()

                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == messagesList.size - 2) {
                    chatRV.scrollToPosition(messagesList.size - 1)
                }
                chatReachedBottom = true
                getMoreChat = true
            }
        })

        //Observe newly added chats
        chatViewModel.updatedMessageMLD.observe(MainActivity(), Observer {
            if (it != null) {
                val altMessageList =
                    HandleMessages(messagesList, arrayListOf(it), chatAdapter).handleMessagesList()
                messagesList = altMessageList
            }
        })

        //Clear old chat list and create a new one
        chatViewModel.replyChatMLD.observe(this.viewLifecycleOwner, Observer {
            listenToSnapShot = true
            if (it != null) {
                chatAdapter.updateChatList(it)
                chatRV.scrollToPosition(messagesList.indexOf(replyMessage))
                getMoreChat = true
            }
        })

        //Get Reply for chat items that is a reply fo another message
        chatViewModel.chatRepliesMld.observe(this.viewLifecycleOwner, Observer {
            chatAdapter.updateRepliesList(it)
        })

        recordView.setOnRecordListener(object : OnRecordListener {
            override fun onStart() {
                uploadMediaBtn.visibility = GONE
                uploadFileBtn.visibility = GONE
                messageET.visibility = GONE

                val timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        voiceNoteId = Firebase.firestore.collection("Chats").document().id
                        voiceRecorder = VoiceRecorder(requireContext(), voiceNoteId)
                        voiceRecorder.startRecord()
                    }
                }, 350)
            }

            override fun onCancel() {
            }

            override fun onFinish(recordTime: Long, limitReached: Boolean) {
                voiceRecorder.stopRecord()
                sendVoiceNote(recordTime.toInt())

                //Get show original views again
                uploadMediaBtn.visibility = VISIBLE
                uploadFileBtn.visibility = VISIBLE
                messageET.visibility = VISIBLE
            }

            override fun onLessThanSecond() {
                //Get show original views again
                uploadMediaBtn.visibility = VISIBLE
                uploadFileBtn.visibility = VISIBLE
                messageET.visibility = VISIBLE
            }

            override fun onLock() {
            }

        })

        recordView.setOnBasketAnimationEndListener {
            //Get show original views again
            uploadMediaBtn.visibility = VISIBLE
            uploadFileBtn.visibility = VISIBLE
            messageET.visibility = VISIBLE
        }

        controller = SwipeController(requireContext(),
            object : ISwipeControllerActions {
                override fun onSwipePerformed(position: Int) {
                    showReplyLayout(position)
                }
            })

        clearReplyBtn.setOnClickListener {
            clearReply()
        }

        uploadMediaBtn.setOnClickListener {
            openGalleryForMedia()
        }

        uploadFileBtn.setOnClickListener {
            openGalleryForFile()
        }

        sendMessageBtn.setOnClickListener {
            sendTextMessage()
        }

        messageOptionsBtn.setOnClickListener {
            val popUpMenu = PopupMenu(requireActivity(), messageOptionsBtn)
            popUpMenu.menuInflater.inflate(R.menu.message_options, popUpMenu.menu)

            val selectedMessage = chatAdapter.getSelectedMessage()
            if (selectedMessage != null) {

                if (selectedMessage.message.isNullOrEmpty()) {
                    popUpMenu.menu.removeItemAt(0)
                }
            }

            popUpMenu.setOnMenuItemClickListener {
                if (selectedMessage != null) {

                    if (it.title == "Copy") {
                        val clipboardManager =
                            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as (ClipboardManager)
                        val clipData =
                            ClipData.newPlainText(selectedMessage.message, selectedMessage.message)
                        clipboardManager.setPrimaryClip(clipData)
                    } else {
                        showMessageDetails(selectedMessage.seenBy)
                    }
                }
                true
            }
            popUpMenu.show()
        }

        appViewModel.userByIdMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                updateUsersList(it)
            }
        })

        val itemTouchHelper = ItemTouchHelper(controller)
        itemTouchHelper.attachToRecyclerView(chatRV)

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun sendTextMessage() {
        if (messageET.text.toString().trim()
                .isNotEmpty() || pendingMediaList.isNotEmpty()
        ) {

            val mentionedUsersList = messageET.getMentionedUsers()

            val messageText = if (mentionedUsersList.size > 0) {
                messageET.getActualText().trim()
            } else {
                messageET.text!!.toString().trim()
            }

            val mentionsList = ArrayList<String>()
            if (mentionedUsersList.size > 0) {
                for (user in mentionedUsersList) {
                    mentionsList.add(user.phone)
                }
            }

            val messageId = Firebase.firestore.collection("Chats").document().id
            val message = Message(
                messageET.text!!.toString().trim(),
                messageText,
                currentUserId,
                messageId,
                "Text",
                replyMessageId,
                DateUtils().getTime(),
                "Sending",
                isGroupChat,
                ArrayList(),
                mentionsList,
                pendingMediaList
            )


            messagesList.add(message)
            val position = messagesList.indexOf(message)
            chatAdapter.notifyItemInserted(position)
            chatRV.scrollToPosition(position)
            val pendingMessage =
                PendingMessage(0, chatId, collectionPath, message)
            pendingMessagesVM.addMessageItem(pendingMessage)

            if (usersList.any { it1 -> it1.phone == currentUserId }) {
                val currentUserName = usersList.first { it1 -> it1.phone == currentUserId }.name

                for (user in mentionedUsersList) {
                    val notifMessage = "$currentUserName mentioned you in a message"
                    val dataMap = hashMapOf("case" to "Chat", "id" to chatId)
                    val date = DateUtils().getTime()

                    val notificationModel = NotificationModel(
                        user.token,
                        Data(
                            "3ala Sorto",
                            notifMessage,
                            dataMap
                        )
                    )
                    (activity as MainActivity).createNotification(notificationModel)

                    val inAppNotificationData =
                        InAppNotificationData(
                            currentUserId,
                            user.phone,
                            notifMessage,
                            "",
                            date,
                            dataMap
                        )

                    appViewModel.createInAppNotification(inAppNotificationData)
                }
            }
            messageET.text!!.clear()
            selectedMediaRV.visibility = GONE
            downscaleEditText()
            clearReply()
        }
    }

    private fun showMessageDetails(usersList: ArrayList<String>) {
        val fragment = SeenByFragment(usersList)
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("SEEN_BY_FRAGMENT")
        transaction.commit()
    }

    private fun initializeChatAdapter() {
        //Initialize adapter
        chatAdapter = ChatAdapter(
            messagesList,
            currentUserId,
            requireContext(),
            ::scrollAllowed,
            ::goToReply,
            ::pauseAll,
            ::enlargeMedia,
            ::downloadFile,
            ::changeMessageDetailsVisibility,
            isAnonymous
        )
    }

    private fun initializeSelectedMediaRecyclerview() {
        selectedMediaAdapter = ChatSelectedMediaAdapter(pendingMediaList, ::removeMediaItem)
        selectedMediaRV.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        selectedMediaRV.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                outRect.right = 30
            }
        })
        selectedMediaRV.adapter = selectedMediaAdapter
    }

    private fun removeMediaItem(mediaItem: MediaData) {
        val removedItemIndex = pendingMediaList.indexOf(mediaItem)
        pendingMediaList.remove(mediaItem)
        selectedMediaAdapter.notifyItemRemoved(removedItemIndex)

        pendingMediaList.sortBy { it.index }

        if (pendingMediaList.isNotEmpty()) {
            //Rearrange indices
            for (item in pendingMediaList) {
                if (item.index != pendingMediaList.indexOf(item)) {
                    item.index = pendingMediaList.indexOf(item)
                }
            }
        } else {
            //Restore views visibility if media array is empty
            selectedMediaRV.visibility = GONE
            uploadMediaBtn.visibility = VISIBLE
            recordBtn.visibility = VISIBLE
            uploadFileBtn.visibility = VISIBLE
            sendMessageBtn.visibility = GONE
        }
    }

    private fun changeMessageDetailsVisibility(isVisible: Boolean) {
        messageOptionsBtn.visibility = if (isVisible) {
            VISIBLE
        } else {
            GONE
        }
    }

    private fun getArgs() {
        val args = this.arguments
        if (args != null) {
            group = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                args.getParcelable("GROUP", Group::class.java)
            } else {
                @Suppress("DEPRECATION")
                args.getParcelable("GROUP")
            }

            if (group != null) {
                Glide.with(headerImage).load(group!!.groupImageLink).into(headerImage)
                headerTitle.text = group!!.name

                //Get group members and admins data
                for (member in group!!.members) {
                    appViewModel.getUserById(member)
                }

                for (admin in group!!.admins) {
                    appViewModel.getUserById(admin)
                }
            }

            isGroupChat = args.getBoolean("IS_GROUP_CHAT")
            chatId = args.getString("CHAT_ID")!!
            collectionPath = args.getString("COLLECTION_PATH")!!
            isAnonymous = args.getBoolean("IS_ANONYMOUS")

            if (!isGroupChat) {
                headerLayout.visibility = GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()

        //Check changes in chats
        Firebase.firestore.collection(collectionPath)
            .document(chatId).collection("Chats")
            .addSnapshotListener { _, _ ->
                if (hasConnection && listenToSnapShot) {
                    if (messagesList.size > 0) {
                        val lastSentMessage =
                            messagesList.lastOrNull { it.status == "Delivered" }
                        if (lastSentMessage != null) {
                            chatViewModel.getNewMessages(
                                collectionPath,
                                chatId,
                                lastSentMessage.date!!
                            )
                        } else {
                            chatViewModel.getNewMessages(collectionPath, chatId, initDate)
                        }
                    } else {
                        chatViewModel.getNewMessages(collectionPath, chatId, initDate)
                    }
                }
            }

        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    //If there are any selected messages clear all selected messages
                    if (chatAdapter.areMessagesSelected()) {
                        chatAdapter.removeSelectedMessage()
                    } else { //Else remove fragment
                        requireActivity().supportFragmentManager.popBackStackImmediate(
                            "CHAT_FRAGMENT",
                            FragmentManager.POP_BACK_STACK_INCLUSIVE
                        )//Go to prev fragment
                        this.isEnabled = false //Disable observer
                    }
                }
            })
    }

    private fun downloadFile(message: Message) {
        chatViewModel.downloadFile(message, requireContext())
    }

    private fun getVoiceNote() {
        if (messagesList.any { it1 -> it1.messageType == "VoiceNote" }) {
            val voiceNotesMessages =
                messagesList.filter { it1 ->
                    it1.messageType == "VoiceNote"
                }

            for (message in voiceNotesMessages) {

                if (!message.mediaData[0].mediaLink.isNullOrEmpty()) {
                    chatViewModel.getVoiceNote(
                        message.mediaData[0].mediaLink!!,
                        message.messageId
                    )
                }
            }
        }
    }

    private fun checkIfFileExists() {
        if (messagesList.any { it1 -> it1.messageType == "File" }) {
            val filteredMessagesList =
                messagesList.filter { it1 ->
                    it1.messageType == "File"
                }

            for (filteredMessage in filteredMessagesList) {

                if (!filteredMessage.mediaData[0].mediaLink.isNullOrEmpty()) {

                    chatViewModel.checkIfFileExists(filteredMessage)
                }
            }
        }
    }

    private fun getUsers(messagesList: ArrayList<Message>) {
        for (message in messagesList) {
            for (userId in message.mentions) {
                if (userId.isNotEmpty() && !usersList.any { it.phone == userId }) {
                    appViewModel.getUserById(userId)
                }
            }
            for (userId in message.seenBy) {
                if (userId.isNotEmpty() && !usersList.any { it.phone == userId }) {
                    appViewModel.getUserById(userId)
                }
            }
            if (message.ownerId.isNotEmpty() && !usersList.any { it.phone == message.ownerId }) {
                appViewModel.getUserById(message.ownerId)
            }
        }
    }

    private fun editTextStatus(isEditTextEmpty: Boolean) {
        if (!isEditTextEmpty) {
            expandEditText()
        } else {
            downscaleEditText()
        }
    }

    private fun scrollAllowed(isEnabled: Boolean) {
        canScrollVertically = isEnabled
        controller.setSwipeEnabled(isEnabled)
    }

    private fun goToReply(message: Message) {
        replyMessage = message
        if (messagesList.contains(message)) {
            chatRV.scrollToPosition(messagesList.indexOf(message))
        } else {
            if (hasConnection) {
                chatViewModel.goToReplyMessage(collectionPath, chatId, message.date!!)
                chatReachedBottom = false
            }
        }
    }

    //Crop Result Launcher
    private
    val cropResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            assert(result.data != null)
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                val messageId =
                    Firebase.firestore.collection("Chats").document().id

                requireContext().grantUriPermission(
                    requireActivity().packageName,
                    resultUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                val extension = if (resultUri.path != null) {
                    File(resultUri.path!!).extension
                } else {
                    ""
                }

                val mediaData = MediaData(
                    null,
                    resultUri.toString(),
                    null,
                    extension,
                    0,
                    0
                )

                val message = Message(
                    "",
                    "",
                    currentUserId,
                    messageId,
                    "Image",
                    replyMessageId,
                    DateUtils().getTime(),
                    "Sending",
                    isGroupChat,
                    ArrayList(),
                    ArrayList(),
                    arrayListOf(mediaData)
                )
                messagesList.add(message)
                chatAdapter.notifyItemInserted(messagesList.indexOf(message))
                chatRV.scrollToPosition(messagesList.indexOf(message))
                val pendingMessage =
                    PendingMessage(0, chatId, collectionPath, message)
                pendingMessagesVM.addMessageItem(pendingMessage)
            }
        } else if (result.resultCode == RESULT_CANCEL) {
            Toast.makeText(
                context,
                "Image was not Uploaded",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //Select Media
    private fun openGalleryForMedia() {
        val intent = Intent()
        intent.type = "image/* video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        pickMediaLauncher.launch(intent)
    }

    //Select Image Launchers
    @SuppressLint("NotifyDataSetChanged")
    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                pendingMediaList.clear()
                selectedMediaAdapter.notifyDataSetChanged()
                selectedMediaRV.visibility = VISIBLE
                expandEditText()
                val data: Intent = result.data!!

                if (data.clipData != null) { //multiple media items
                    val count = data.clipData!!.itemCount

                    for (i in 0 until count) {
                        val uri = data.clipData!!.getItemAt(i).uri
                        val mediaType = getMediaType(uri)

                        requireContext().grantUriPermission(
                            requireActivity().packageName,
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                        val mediaWidth = if (mediaType == "Video") {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(requireContext(), uri)
                            Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!)
                        } else {
                            0
                        }

                        val mediaHeight = if (mediaType == "Video") {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(requireContext(), uri)
                            Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!)
                        } else {
                            0
                        }

                        val mediaItem =
                            MediaData(
                                pendingMediaList.size,
                                uri.toString(),
                                null,
                                mediaType,
                                mediaWidth,
                                mediaHeight
                            )
                        pendingMediaList.add(mediaItem)
                        selectedMediaAdapter.notifyItemInserted(pendingMediaList.indexOf(mediaItem))
                    }
                } else {//one media items
                    val uri: Uri = data.data!!

                    requireContext().grantUriPermission(
                        requireActivity().packageName,
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    val mediaType = getMediaType(uri)

                    val mediaWidth = if (mediaType == "Video") {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(requireContext(), uri)
                        Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!)
                    } else {
                        0
                    }

                    val mediaHeight = if (mediaType == "Video") {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(requireContext(), uri)
                        Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!)
                    } else {
                        0
                    }

                    /*val filePathColum = arrayOf(MediaStore.Images.Media.DATA)
                    val cursor =
                        requireContext().contentResolver.query(uri, filePathColum, null, null, null)
                    cursor!!.moveToFirst()

                    val columnIndex = cursor.getColumnIndex(filePathColum[0])
                    val picturePath = cursor.getString(columnIndex)
                    cursor.close()

                    val bitmap = ThumbnailUtils.createVideoThumbnail(picturePath,MediaStore.Video.Thumbnails.FULL_SCREEN_KIND)*/

                    val mediaItem =
                        MediaData(
                            pendingMediaList.size,
                            uri.toString(),
                            null,
                            mediaType,
                            mediaWidth,
                            mediaHeight
                        )
                    pendingMediaList.add(mediaItem)
                    selectedMediaAdapter.notifyItemInserted(pendingMediaList.indexOf(mediaItem))
                }
            }
        }

    //Select file
    private fun openGalleryForFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        pickFileLauncher.launch(intent)
    }

    //Select file launcher
    private
    val pickFileLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent = result.data!!
                val sourceUri: Uri = data.data!!

                requireContext().grantUriPermission(
                    requireActivity().packageName,
                    sourceUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                val file = File(sourceUri.path!!)
                val fileName =
                    file.nameWithoutExtension.substringAfter(
                        ":"
                    ).trim()
                val fileExtension = file.extension

                val mediaData =
                    MediaData(
                        null,
                        sourceUri.toString(),
                        null,
                        fileExtension,
                        null,
                        null,
                        null
                    )

                val messageId =
                    Firebase.firestore.collection("Chats")
                        .document().id


                val message = Message(
                    fileName,
                    "",
                    currentUserId,
                    messageId,
                    "File",
                    replyMessageId,
                    DateUtils().getTime(),
                    "Sending",
                    isGroupChat,
                    ArrayList(),
                    ArrayList(),
                    arrayListOf(mediaData)
                )
                messagesList.add(message)
                chatAdapter.notifyItemInserted(
                    messagesList.indexOf(message)
                )
                chatRV.scrollToPosition(
                    messagesList.indexOf(
                        message
                    )
                )
                val pendingMessage =
                    PendingMessage(
                        0,
                        chatId,
                        collectionPath,
                        message
                    )
                pendingMessagesVM.addMessageItem(
                    pendingMessage
                )
            }
        }

    private fun initializeWidgets(view: View) {
        //Initialize Widgets
        chatRV = view.findViewById(R.id.rv_chat)
        selectedMediaRV = view.findViewById(R.id.rv_selected_media)
        replyLayout = view.findViewById(R.id.cl_reply_text)
        chatLayout = view.findViewById(R.id.cl_chat)
        replyTextTV = view.findViewById(R.id.tv_chat_reply)
        messageET = view.findViewById(R.id.et_chat_text)
        sendMessageBtn = view.findViewById(R.id.btn_gc_send)
        clearReplyBtn = view.findViewById(R.id.btn_clear_reply)
        recordBtn = view.findViewById(R.id.btn_record_voice)
        recordView = view.findViewById(R.id.record_view)
        uploadMediaBtn = view.findViewById(R.id.btn_gc_image)
        uploadFileBtn = view.findViewById(R.id.btn_gc_file)
        headerImage = view.findViewById(R.id.iv_chat)
        headerTitle = view.findViewById(R.id.tv_chat_name)
        mentionsRV = view.findViewById(R.id.rv_mentions)
        headerLayout = view.findViewById(R.id.layout_chat_header)
        messageOptionsBtn = view.findViewById(R.id.btn_chat_menu)
    }

    //UCrop Sh!t
    private fun queryName(
        resolver: ContentResolver,
        uri: Uri
    ): String {
        val returnCursor = resolver.query(
            uri,
            null,
            null,
            null,
            null
        )
        val nameIndex =
            returnCursor!!.getColumnIndex(
                OpenableColumns.DISPLAY_NAME
            )
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        returnCursor.close()
        return name
    }

    private fun sendVoiceNote(
        audioDuration: Int
    ) {
        val file =
            File("${requireContext().externalCacheDir!!.absolutePath}/$voiceNoteId.3gp")
        val uri = Uri.fromFile(file)

        requireContext().grantUriPermission(
            requireActivity().packageName,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        val mediaData = MediaData(
            null,
            uri.toString(),
            null,
            null,
            0,
            0,
            audioDuration
        )


        //Create message object
        val message = Message(
            "",
            "",
            currentUserId,
            voiceNoteId,
            "VoiceNote",
            replyMessageId,
            DateUtils().getTime(),
            "Sending",
            isGroupChat,
            ArrayList(),
            ArrayList(),
            arrayListOf(mediaData)
        )

        messagesList.add(message)
        chatAdapter.notifyItemInserted(
            messagesList.indexOf(
                message
            )
        )
        chatRV.scrollToPosition(
            messagesList.indexOf(
                message
            )
        )
        val pendingMessage =
            PendingMessage(
                0,
                chatId,
                collectionPath,
                message
            )
        pendingMessagesVM.addMessageItem(
            pendingMessage
        )
    }

    private fun showReplyLayout(position: Int) {
        val replyText =
            if (chatAdapter.getItemById(position).message!!.isNotEmpty()) {
                chatAdapter.getItemById(position).message
            } else {
                getString(R.string.attachment_reply)
            }
        replyMessageId =
            chatAdapter.getItemById(position).messageId
        //Set swiped message to reply TV
        replyTextTV.text = replyText

        if (!isReply) {
            animateTranslation(-(chatLayout.height))
            animateRV(true)
            isReply = true
        }
    }

    private fun animateTranslation(
        translation: Int
    ) {
        if (replyLayout.visibility == INVISIBLE) {
            replyLayout.visibility = VISIBLE
        } else if (replyLayout.visibility == VISIBLE) {
            replyLayout.visibility = INVISIBLE
        }

        val animation = ObjectAnimator.ofFloat(
            replyLayout,
            "translationY",
            translation.toFloat()
        )
        animation.duration = 200
        animation.start()
    }

    private fun animateRV(isShowing: Boolean) {
        val rvAnimation: Animation =
            object : Animation() {
                override fun applyTransformation(
                    interpolatedTime: Float,
                    t: Transformation?
                ) {
                    val params: ConstraintLayout.LayoutParams =
                        chatRV.layoutParams as ConstraintLayout.LayoutParams
                    if (isShowing) {
                        params.bottomMargin =
                            (replyLayout.height + 30)
                    } else {
                        params.bottomMargin = (0)
                    }
                    chatRV.layoutParams = params
                }
            }
        rvAnimation.duration = 200
        rvAnimation.fillAfter = true
        chatRV.startAnimation(rvAnimation)
    }

    private fun clearReply() {
        if (isReply) {
            animateTranslation(0)
            animateRV(false)
            isReply = false
        }
        replyMessageId = ""
        replyTextTV.text = ""
    }

    private fun expandEditText() {
        uploadMediaBtn.visibility = GONE
        uploadFileBtn.visibility = GONE
        recordBtn.visibility = GONE
        sendMessageBtn.visibility = VISIBLE
    }

    private fun downscaleEditText() {
        uploadMediaBtn.visibility = VISIBLE
        recordBtn.visibility = VISIBLE
        uploadFileBtn.visibility = VISIBLE
        sendMessageBtn.visibility = GONE
    }

    private fun getMoreChat(isNew: Boolean) {
        if (hasConnection) {
            if (!isNew) {
                //Get chat before date of first item
                chatViewModel.getOlderChat(
                    collectionPath,
                    chatId,
                    messagesList[0].date!!
                )
            } else {
                //Get chat after date of first item
                chatViewModel.getNewerChat(
                    collectionPath,
                    chatId,
                    messagesList.last().date!!
                )
            }
            getMoreChat = false
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun enlargeMedia(
        mediaList: ArrayList<MediaData>?,
        clickedItemIndex: Int,
        postID: String
    ) {
        val args = Bundle()
        args.putParcelableArrayList("MEDIA_LIST", mediaList)
        args.putInt("CLICKED_ITEM_INDEX", clickedItemIndex)
        args.putString("POST_ID", postID)
        (activity as MainActivity).goToEnlargeMediaFragment(args)
    }

    private fun pauseAll(position: Int) {
        for (i in 0 until chatAdapter.itemCount) {
            val viewHolder =
                chatRV.findViewHolderForAdapterPosition(
                    i
                )
            if (viewHolder is ChatAdapter.VoiceViewHolder && i != position) {
                viewHolder.playStateBtn.setImageDrawable(
                    (ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_play
                    ))
                )
            }
        }
    }

    private
    val activeScrollListener = object :
        RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(
            recyclerView: RecyclerView,
            newState: Int
        ) {
            super.onScrollStateChanged(
                recyclerView,
                newState
            )

            if (messagesList.size > 0 && getMoreChat) {
                if (linearLayoutManager.findFirstCompletelyVisibleItemPosition() == 0 && !chatReachedTop) {
                    getMoreChat(false)
                } else if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == messagesList.size - 1
                    && !chatReachedBottom
                ) {
                    getMoreChat(true)
                }
            }
        }
    }

    private fun getMediaType(uri: Uri): String {
        val type =
            requireActivity().contentResolver.getType(
                uri
            )
        return if (type != null && type.startsWith("image")) {
            "Image"
        } else if (type != null && type.startsWith("video")) {
            "Video"
        } else {
            ""
        }
    }

    private fun updateUsersList(userData: UserData) {
        if (usersList.any { it.phone == userData.phone }) {
            usersList.removeAll { it.phone == userData.phone }
        }
        usersList.add(userData)
        chatAdapter.updateUserList(usersList)
        updateMentionsUsersList()
    }

    //Functions for mentions
    private fun showRecyclerView(
        show: Boolean
    ) {
        if (show) {
            mentionsRV.visibility =
                VISIBLE
        } else {
            mentionsRV.visibility =
                GONE
        }
    }

    private fun setRecyclerViewData(
        usersList: ArrayList<UserData>
    ) {
        mentionsRV.setRecyclerView(
            usersList
        )
    }

    private fun updateMentionsUsersList() {
        messageET.editUsersList(
            ArrayList(usersList)
        )
    }

    private fun selectMentionedUser(
        userData: UserData
    ) {
        messageET.selectMentionedUser(
            userData
        )
    }
}