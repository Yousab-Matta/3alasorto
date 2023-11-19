package com.example.alasorto

import android.Manifest.permission.*
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.*
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.alasorto.dataClass.Group
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devlomi.record_view.OnRecordListener
import com.devlomi.record_view.RecordButton
import com.devlomi.record_view.RecordView
import com.example.alasorto.adapters.ChatAdapter
import com.example.alasorto.chatHistoryDatabase.ChatHistory
import com.example.alasorto.chatHistoryDatabase.ChatHistoryViewModel
import com.example.alasorto.dataClass.MediaData
import com.example.alasorto.dataClass.Message
import com.example.alasorto.dataClass.UserData
import com.example.alasorto.pendingMessagesDatabase.PendingMessage
import com.example.alasorto.pendingMessagesDatabase.PendingMessageViewModel
import com.example.alasorto.utils.*
import com.example.alasorto.viewModels.ChatViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
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

    private val chatRepliesList = HashMap<String, Message>()
    private val allUsersList = ArrayList<UserData>()
    private val chatHistoryList = ArrayList<ChatHistory>()
    private val viewModel: ChatViewModel by viewModels()
    private val pendingMessagesVM: PendingMessageViewModel by viewModels()
    private val chatHistoryVM: ChatHistoryViewModel by viewModels()
    private val initDate = Date()

    private lateinit var chatRV: RecyclerView
    private lateinit var replyLayout: ConstraintLayout
    private lateinit var chatLayout: ConstraintLayout
    private lateinit var headerImage: ImageView
    private lateinit var headerTitle: TextView
    private lateinit var enlargedMediaLayout: ConstraintLayout
    private lateinit var replyTextTV: TextView
    private lateinit var messageET: EditText
    private lateinit var clearReplyBtn: ImageButton
    private lateinit var sendMessageBtn: ImageButton
    private lateinit var clearTextBtn: ImageButton
    private lateinit var uploadImageBtn: ImageButton
    private lateinit var uploadVideoBtn: ImageButton
    private lateinit var uploadFileBtn: ImageButton
    private lateinit var expandedImage: ImageView
    private lateinit var expandedVideo: ConstraintLayout
    private lateinit var videoPlayer: VideoPlayer
    private lateinit var recordBtn: RecordButton
    private lateinit var recordView: RecordView
    private lateinit var adapter: ChatAdapter
    private lateinit var internetCheck: InternetCheck
    private lateinit var voiceRecorder: VoiceRecorder
    private lateinit var controller: SwipeController
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var replyMessage: Message //The message that is a reply in clicked message

    private var chatId: String = ""
    private var collectionPath: String = ""
    private var replyMessageId: String = ""
    private var voiceNoteId: String = ""
    private var currentUser: UserData? = null
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

        //Get current user
        currentUser = (activity as MainActivity).getCurrentUser()

        //Get all users
        allUsersList.addAll((activity as MainActivity).getAllUsers())

        //Initialize Widgets
        chatRV = view.findViewById(R.id.rv_chat)
        replyLayout = view.findViewById(R.id.cl_reply_text)
        chatLayout = view.findViewById(R.id.cl_chat)
        enlargedMediaLayout = view.findViewById(R.id.cl_chat_expanded_media)
        expandedImage = view.findViewById(R.id.iv_expanded)
        expandedVideo = view.findViewById(R.id.expanded_video)
        replyTextTV = view.findViewById(R.id.tv_chat_reply)
        messageET = view.findViewById(R.id.et_chat_text)
        sendMessageBtn = view.findViewById(R.id.btn_gc_send)
        clearReplyBtn = view.findViewById(R.id.btn_clear_reply)
        recordBtn = view.findViewById(R.id.btn_record_voice)
        recordView = view.findViewById(R.id.record_view)
        uploadImageBtn = view.findViewById(R.id.btn_gc_image)
        uploadVideoBtn = view.findViewById(R.id.btn_gc_video)
        uploadFileBtn = view.findViewById(R.id.btn_gc_file)
        headerImage = view.findViewById(R.id.iv_chat)
        headerTitle = view.findViewById(R.id.tv_chat_name)

        recordBtn.setRecordView(recordView)

        videoPlayer = VideoPlayer(requireContext(), expandedVideo)

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
            }

            isGroupChat = args.getBoolean("IS_GROUP_CHAT")
            chatId = args.getString("CHAT_ID")!!
            collectionPath = args.getString("COLLECTION_PATH")!!
            isAnonymous = args.getBoolean("IS_ANONYMOUS")
        }

        audioPath = requireContext().externalCacheDir!!.absolutePath

        //Initialize adapter
        adapter = ChatAdapter(
            messagesList,
            allUsersList,
            currentUser!!.phone,
            requireContext(),
            ::scrollAllowed,
            ::goToReply,
            ::pauseAll,
            ::enlargeImage,
            ::enlargeVideo,
            isAnonymous
        )

        //Create LayoutManager
        linearLayoutManager = object : LinearLayoutManager(requireContext()) {
            //Set can scroll vertically or no to disable scroll while changing vc progress
            override fun canScrollVertically(): Boolean {
                return canScrollVertically
            }
        }

        linearLayoutManager.stackFromEnd = true

        chatRV.layoutManager = linearLayoutManager
        chatRV.addItemDecoration(LinearSpacingItemDecorator(20))
        chatRV.adapter = adapter

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
                    viewModel.getChat(collectionPath, chatId)
                    isChatLoadedFirstTime = true
                }
            }
        }

        viewModel.audioFileMLD.observe(this.viewLifecycleOwner, Observer {
            val key = it.name.removeSuffix(".3gp")
            val audioMessage = messagesList.firstOrNull { it1 -> it1.messageId == key }
            if (audioMessage?.mediaData != null) {
                messagesList.remove(audioMessage)
                audioMessage.mediaData!!.media = it.path
                messagesList.add(audioMessage)
                messagesList.sortBy { it2 -> it2.date }
                adapter.notifyItemChanged(messagesList.indexOf(audioMessage))
            }
        })

        viewModel.fileMLD.observe(this.viewLifecycleOwner, Observer {
            //ToDo
        })

        //Observe chat
        viewModel.chatMLD.observe(this.viewLifecycleOwner, Observer {
            listenToSnapShot = true
            if (it != null) {

                val altMessageList = HandleMessages(messagesList, it, adapter).handleMessagesList()
                messagesList = altMessageList

                getVoiceNote()

                chatRV.scrollToPosition(messagesList.size - 1)
                chatReachedTop = it.size < 10
                getMoreChat = true
            }
            viewModel.chatMLD.removeObservers(this.viewLifecycleOwner)
        })

        //Observe old chats when scrolled up
        viewModel.olderChatMld.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {

                val altMessageList = HandleMessages(messagesList, it, adapter).handleMessagesList()
                messagesList = altMessageList

                getVoiceNote()

                chatReachedTop = it.size < 10
                getMoreChat = true
            }
        })

        //Observe new chats when scrolled down
        viewModel.newerChatMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                val altMessageList = HandleMessages(messagesList, it, adapter).handleMessagesList()
                messagesList = altMessageList

                getVoiceNote()

                chatReachedBottom = it.size < 10
                getMoreChat = true
            }
        })

        //Observe newly added chats
        viewModel.newMessagesMLD.observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                val altMessageList = HandleMessages(messagesList, it, adapter).handleMessagesList()
                messagesList = altMessageList

                getVoiceNote()

                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == messagesList.size - 2) {
                    chatRV.scrollToPosition(messagesList.size - 1)
                }
                chatReachedBottom = true
                getMoreChat = true
            }
        })

        //Observe newly added chats
        viewModel.updatedMessageMLD.observe(MainActivity(), Observer {
            if (it != null) {
                val altMessageList =
                    HandleMessages(messagesList, arrayListOf(it), adapter).handleMessagesList()
                messagesList = altMessageList
            }
        })

        //Clear old chat list and create a new one
        viewModel.replyChatMLD.observe(this.viewLifecycleOwner, Observer {
            listenToSnapShot = true
            if (it != null) {
                adapter.updateList(it)
                chatRV.scrollToPosition(messagesList.indexOf(replyMessage))
                getMoreChat = true
            }
        })

        //Get Reply for chat items that is a reply fo another message
        viewModel.chatRepliesMld.observe(this.viewLifecycleOwner, Observer {
            if (messagesList.any { it1 -> it1.repliedMessageId.contains(it.messageId) }) {
                val messagesWithReplyList =
                    messagesList.filter { it1 -> it1.repliedMessageId.contains(it.messageId) }
                for (message in messagesWithReplyList) {
                    adapter.updateRepliesList(message.messageId, it)
                }
            }
        })

        /*viewModel.messageSentMLD.observe(this.viewLifecycleOwner, Observer {
            if (pendingMessagesList.any { it1 -> it1.message.messageID == it }) {
                pendingMessagesList.remove(pendingMessagesList.first { it1 -> it1.message.messageID == it })
            }
        })

        pendingMessagesVM.readAllData.observe(this.viewLifecycleOwner, Observer {
            for (item in it) {
                if (!pendingMessagesList.contains(item))
                    pendingMessagesList.add(item)
            }
        })*/

        recordView.setOnRecordListener(object : OnRecordListener {
            override fun onStart() {
                uploadImageBtn.visibility = GONE
                uploadVideoBtn.visibility = GONE
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
                sendVoiceNote(recordTime.toInt() - 350)

                //Get show original views again
                uploadImageBtn.visibility = VISIBLE
                uploadVideoBtn.visibility = VISIBLE
                messageET.visibility = VISIBLE
            }

            override fun onLessThanSecond() {
                //Get show original views again
                uploadImageBtn.visibility = VISIBLE
                uploadVideoBtn.visibility = VISIBLE
                uploadFileBtn.visibility = VISIBLE
                messageET.visibility = VISIBLE
            }

            override fun onLock() {
            }

        })

        recordView.setOnBasketAnimationEndListener {
            //Get show original views again
            uploadImageBtn.visibility = VISIBLE
            uploadVideoBtn.visibility = VISIBLE
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

        uploadImageBtn.setOnClickListener {
            openGalleryForImage()
        }

        uploadVideoBtn.setOnClickListener {
            openGalleryForVideo()
        }

        uploadFileBtn.setOnClickListener {
            openGalleryForFile()
        }

        messageET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s!!.toString().isNotEmpty()) {
                    expandEditText()
                } else {
                    downscaleEditText()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })

        sendMessageBtn.setOnClickListener(View.OnClickListener
        {
            val messageId = Firebase.firestore.collection("Chats").document().id
            val message = Message(
                messageET.text.toString().trim(),
                currentUser!!.phone,
                messageId,
                "Text",
                replyMessageId,
                DateUtils().getTime(),
                "Sending",
                isGroupChat,
                ArrayList(),
                null
            )
            if (messageET.text.toString().trim().isNotEmpty()) {
                messagesList.add(message)
                val position = messagesList.indexOf(message)
                adapter.notifyItemInserted(position)
                chatRV.scrollToPosition(position)
                val pendingMessage =
                    PendingMessage(0, chatId, collectionPath, message)
                pendingMessagesVM.addMessageItem(pendingMessage)
                messageET.text.clear()
                clearReply()
            }
        })

        val itemTouchHelper = ItemTouchHelper(controller)
        itemTouchHelper.attachToRecyclerView(chatRV)
    }

    override fun onResume() {
        super.onResume()

        //Check changes in chats
        Firebase.firestore.collection(collectionPath)
            .document(chatId).collection("Chats")
            .addSnapshotListener { _, _ ->
                if (hasConnection && listenToSnapShot) {
                    if (messagesList.size > 0) {
                        val lastSentMessage = messagesList.lastOrNull { it.status == "Delivered" }
                        if (lastSentMessage != null) {
                            viewModel.getNewMessages(collectionPath, chatId, lastSentMessage.date!!)
                        } else {
                            viewModel.getNewMessages(collectionPath, chatId, initDate)
                        }
                    } else {
                        viewModel.getNewMessages(collectionPath, chatId, initDate)
                    }
                }
            }

        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (expandedImage.visibility == VISIBLE) {
                        closeExpandedImage()
                    } else if (expandedVideo.visibility == VISIBLE) {
                        closeExpandedVideo()
                    } else {
                        requireActivity().supportFragmentManager.popBackStackImmediate(
                            "CHAT_FRAGMENT",
                            FragmentManager.POP_BACK_STACK_INCLUSIVE
                        )//Go to prev fragment
                        this.isEnabled = false //Disable observer
                    }
                }
            })
    }

    private fun downloadFile(url: String, messageId: String, extension: String) {
        viewModel.getFile(url, messageId, requireContext(), extension)
    }

    private fun getVoiceNote() {
        if (messagesList.any { it1 -> it1.messageType == "VoiceNote" }) {
            val voiceNotesMessages =
                messagesList.filter { it1 ->
                    it1.messageType == "VoiceNote" && it1.mediaData != null
                }

            for (message in voiceNotesMessages) {

                if (message.mediaData!!.mediaLink != null && message.mediaData!!.mediaLink!!.isNotEmpty()) {

                    viewModel.getVoiceNote(
                        message.mediaData!!.mediaLink!!,
                        message.messageId,
                        requireContext()
                    )
                }
            }
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
                viewModel.goToReplyMessage(collectionPath, chatId, message.date!!)
                chatReachedBottom = false
            }
        }
    }

    //Select Image
    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    //Crop Image
    //Select Image Launchers
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent = result.data!!
                val sourceUri: Uri = data.data!!
                val destinationUri = Uri.fromFile(
                    File(
                        requireActivity().cacheDir,
                        queryName(requireActivity().contentResolver, sourceUri)
                    )
                )

                //UCrop options
                val options = UCrop.Options()
                options.setToolbarColor(ContextCompat.getColor(requireActivity(), R.color.black))
                options.setStatusBarColor(ContextCompat.getColor(requireActivity(), R.color.black))
                options.setToolbarWidgetColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.white
                    )
                )

                options.setFreeStyleCropEnabled(true)
                options.setAllowedGestures(
                    UCropActivity.SCALE,
                    UCropActivity.ALL,
                    UCropActivity.SCALE
                )

                val intent = UCrop.of(sourceUri, destinationUri)
                    .withOptions(options)
                    .getIntent(requireActivity())
                cropResult.launch(intent)
            }
        }

    //Crop Result Launcher
    private val cropResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            assert(result.data != null)
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                val messageId = Firebase.firestore.collection("Chats").document().id

                requireContext().grantUriPermission(
                    requireActivity().packageName,
                    resultUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                val mediaData = MediaData(null, resultUri.toString(), null, null, 0, 0)

                val message = Message(
                    messageET.text.toString().trim(),
                    currentUser!!.phone,
                    messageId,
                    "Image",
                    replyMessageId,
                    DateUtils().getTime(),
                    "Sending",
                    isGroupChat,
                    ArrayList(),
                    mediaData
                )
                messagesList.add(message)
                adapter.notifyItemInserted(messagesList.indexOf(message))
                chatRV.scrollToPosition(messagesList.indexOf(message))
                val pendingMessage =
                    PendingMessage(0, chatId, collectionPath, message)
                pendingMessagesVM.addMessageItem(pendingMessage)
            }
        } else if (result.resultCode == RESULT_CANCEL) {
            Toast.makeText(context, "Image was not Uploaded", Toast.LENGTH_SHORT).show()
        }
    }

    //Select Video
    private fun openGalleryForVideo() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "video/*"
        pickVideoLauncher.launch(intent)
    }

    //Select video launcher
    private val pickVideoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent = result.data!!
                val sourceUri: Uri = data.data!!

                requireContext().grantUriPermission(
                    requireActivity().packageName,
                    sourceUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(requireContext(), sourceUri)
                val width =
                    Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!)
                val height =
                    Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!)
                val duration =
                    Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!)

                Toast.makeText(requireContext(), "$width // $height", Toast.LENGTH_SHORT).show()

                val mediaData =
                    MediaData(null, sourceUri.toString(), null, null, width, height, duration)

                val messageId = Firebase.firestore.collection("Chats").document().id


                val message = Message(
                    messageET.text.toString().trim(),
                    currentUser!!.phone,
                    messageId,
                    "Video",
                    replyMessageId,
                    DateUtils().getTime(),
                    "Sending",
                    isGroupChat,
                    ArrayList(),
                    mediaData
                )
                messagesList.add(message)
                adapter.notifyItemInserted(messagesList.indexOf(message))
                chatRV.scrollToPosition(messagesList.indexOf(message))
                val pendingMessage =
                    PendingMessage(
                        0,
                        chatId,
                        collectionPath,
                        message
                    )
                pendingMessagesVM.addMessageItem(pendingMessage)
            }
        }

    //Select file
    private fun openGalleryForFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        pickFileLauncher.launch(intent)
    }

    //Select file launcher
    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent = result.data!!
                val sourceUri: Uri = data.data!!

                requireContext().grantUriPermission(
                    requireActivity().packageName,
                    sourceUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                val file = File(sourceUri.path!!)
                val nameWithSource = file.name
                val fileName = nameWithSource.substringAfter(":").trim()
                val fileExtension = file.extension

                Toast.makeText(requireContext(), fileExtension, Toast.LENGTH_SHORT).show()

                val mediaData =
                    MediaData(null, sourceUri.toString(), null, fileExtension, null, null, null)

                val messageId = Firebase.firestore.collection("Chats").document().id


                val message = Message(
                    fileName,
                    currentUser!!.phone,
                    messageId,
                    "File",
                    replyMessageId,
                    DateUtils().getTime(),
                    "Sending",
                    isGroupChat,
                    ArrayList(),
                    mediaData
                )
                messagesList.add(message)
                adapter.notifyItemInserted(messagesList.indexOf(message))
                chatRV.scrollToPosition(messagesList.indexOf(message))
                val pendingMessage =
                    PendingMessage(
                        0,
                        chatId,
                        collectionPath,
                        message
                    )
                pendingMessagesVM.addMessageItem(pendingMessage)
            }
        }

    //UCrop Sh!t
    private fun queryName(resolver: ContentResolver, uri: Uri): String {
        val returnCursor = resolver.query(uri, null, null, null, null)
        val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        returnCursor.close()
        return name
    }

    private fun sendVoiceNote(audioDuration: Int) {
        val file = File("${requireContext().externalCacheDir!!.absolutePath}/$voiceNoteId.3gp")
        val uri = Uri.fromFile(file)

        requireContext().grantUriPermission(
            requireActivity().packageName,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        val mediaData = MediaData(null, uri.toString(), null, null, 0, 0, audioDuration)


        //Create message object
        val message = Message(
            messageET.text.toString().trim(),
            currentUser!!.phone,
            voiceNoteId,
            "VoiceNote",
            replyMessageId,
            DateUtils().getTime(),
            "Sending",
            isGroupChat,
            ArrayList(),
            mediaData
        )

        messagesList.add(message)
        adapter.notifyItemInserted(messagesList.indexOf(message))
        chatRV.scrollToPosition(messagesList.indexOf(message))
        val pendingMessage =
            PendingMessage(
                0,
                chatId,
                collectionPath,
                message
            )
        pendingMessagesVM.addMessageItem(pendingMessage)
    }

    private fun showReplyLayout(position: Int) {
        val replyText = if (adapter.getItemById(position).message!!.isNotEmpty()) {
            adapter.getItemById(position).message
        } else {
            getString(R.string.attachment_reply)
        }
        replyMessageId = adapter.getItemById(position).messageId
        //Set swiped message to reply TV
        replyTextTV.text = replyText

        if (!isReply) {
            animateTranslation(-(chatLayout.height))
            animateRV(true)
            isReply = true
        }
    }

    private fun animateTranslation(translation: Int) {
        if (replyLayout.visibility == INVISIBLE) {
            replyLayout.visibility = VISIBLE
        } else if (replyLayout.visibility == VISIBLE) {
            replyLayout.visibility = INVISIBLE
        }

        val animation = ObjectAnimator.ofFloat(replyLayout, "translationY", translation.toFloat())
        animation.duration = 200
        animation.start()
    }

    private fun animateRV(isShowing: Boolean) {
        val rvAnimation: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                val params: ConstraintLayout.LayoutParams =
                    chatRV.layoutParams as ConstraintLayout.LayoutParams
                if (isShowing) {
                    params.bottomMargin = (replyLayout.height + 30)
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
        uploadImageBtn.visibility = GONE
        uploadVideoBtn.visibility = GONE
        recordBtn.visibility = GONE
        sendMessageBtn.visibility = VISIBLE
    }

    private fun downscaleEditText() {
        uploadImageBtn.visibility = VISIBLE
        uploadVideoBtn.visibility = VISIBLE
        recordBtn.visibility = VISIBLE
        sendMessageBtn.visibility = GONE
    }

    private fun getMoreChat(isNew: Boolean) {
        if (hasConnection) {
            if (!isNew) {
                //Get chat before date of first item
                viewModel.getOlderChat(
                    collectionPath,
                    chatId,
                    messagesList[0].date!!
                )
            } else {
                //Get chat after date of first item
                viewModel.getNewerChat(
                    collectionPath,
                    chatId,
                    messagesList.last().date!!
                )
            }
            getMoreChat = false
        }
    }

    private fun enlargeImage(imageLink: String) {
        enlargedMediaLayout.visibility = VISIBLE
        expandedImage.visibility = VISIBLE
        Glide.with(expandedImage).load(imageLink).into(expandedImage)

        enlargedMediaLayout.startAnimation(openImageAnim)
        expandedImage.startAnimation(openImageAnim)
    }

    private fun closeExpandedImage() {
        enlargedMediaLayout.visibility = GONE
        expandedImage.visibility = GONE

        enlargedMediaLayout.startAnimation(closeImageAnim)
        expandedImage.startAnimation(closeImageAnim)
    }

    private fun enlargeVideo(videoLink: String?, videoPath: Uri?) {
        enlargedMediaLayout.visibility = VISIBLE
        expandedVideo.visibility = VISIBLE

        if (videoLink != null) {
            videoPlayer.setVideoLink(videoLink)
        } else if (videoPath != null) {
            videoPlayer.setVideoPath(videoPath)
        }

        enlargedMediaLayout.startAnimation(openImageAnim)
        expandedVideo.startAnimation(openImageAnim)
    }

    private fun closeExpandedVideo() {
        enlargedMediaLayout.visibility = GONE
        expandedVideo.visibility = GONE

        videoPlayer.stopMedia()

        enlargedMediaLayout.startAnimation(closeImageAnim)
        expandedVideo.startAnimation(closeImageAnim)
    }

    private fun pauseAll(position: Int) {
        for (i in 0 until adapter.itemCount) {
            val viewHolder = chatRV.findViewHolderForAdapterPosition(i)
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

    private val activeScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

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
}