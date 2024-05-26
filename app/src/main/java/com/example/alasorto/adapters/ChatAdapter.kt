package com.example.alasorto.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import java.util.ArrayList
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.alasorto.R
import com.example.alasorto.dataClass.MediaData
import com.example.alasorto.dataClass.Message
import com.example.alasorto.dataClass.UserData
import com.example.alasorto.utils.MentionTextView
import com.example.alasorto.utils.PostMediaLayout
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val messagesList: ArrayList<Message>,
    private val myId: String,
    private val context: Context,
    private val scrollAllowed: (Boolean) -> Unit,
    private val getReply: (Message) -> Unit,
    private val pauseMedia: (Int) -> Unit,
    private var enlargeMedia: (ArrayList<MediaData>?, Int, String) -> Unit,
    private val downloadMedia: (Message) -> Unit,
    private val changeMessageDetailsVisibility: (Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), OnPreparedListener,
    MediaPlayer.OnCompletionListener {

    private val chatRepliesList = ArrayList<Message>()

    private var selectedMessage: Message? = null
    private var audioHolder: VoiceViewHolder? = null
    private var currentAudioPath = ""

    private val mediaPlayer = MediaPlayer()
    val sdf = SimpleDateFormat("HH:mm", Locale.ENGLISH)

    companion object {
        private const val TEXT_ITEM_LEFT = 0
        private const val TEXT_ITEM_RIGHT = 1
        private const val VOICE_ITEM_LEFT = 2
        private const val VOICE_ITEM_RIGHT = 3
        private const val IMAGE_ITEM_LEFT = 4
        private const val IMAGE_ITEM_RIGHT = 5
        private const val VIDEO_ITEM_LEFT = 6
        private const val VIDEO_ITEM_RIGHT = 7
        private const val FILE_ITEM_LEFT = 8
        private const val FILE_ITEM_RIGHT = 9
    }

    private var usersList = ArrayList<UserData>()

    private val locale = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales.get(0)
    } else {
        @Suppress("DEPRECATION")
        context.resources.configuration.locale
    })!!

    private val myMessageDirection = if (locale == Locale.ENGLISH) {
        LAYOUT_DIRECTION_LTR
    } else {
        LAYOUT_DIRECTION_RTL
    }

    private val otherMessageDirection = if (locale == Locale.ENGLISH) {
        LAYOUT_DIRECTION_RTL
    } else {
        LAYOUT_DIRECTION_LTR
    }

    override fun getItemViewType(position: Int): Int {
        val message = messagesList[position]
        return if (message.ownerId == myId && message.messageType == "Text") {
            TEXT_ITEM_RIGHT
        } else if (message.ownerId != myId && message.messageType == "Text") {
            TEXT_ITEM_LEFT
        } else if (message.ownerId == myId && message.messageType == "VoiceNote") {
            VOICE_ITEM_RIGHT
        } else if (message.ownerId != myId && message.messageType == "VoiceNote") {
            VOICE_ITEM_LEFT
        } else if (message.ownerId == myId && message.messageType == "Image") {
            IMAGE_ITEM_RIGHT
        } else if (message.ownerId != myId && message.messageType == "Image") {
            IMAGE_ITEM_LEFT
        } else if (message.ownerId == myId && message.messageType == "Video") {
            VIDEO_ITEM_RIGHT
        } else if (message.ownerId != myId && message.messageType == "Video") {
            VIDEO_ITEM_LEFT
        } else if (message.ownerId == myId && message.messageType == "File") {
            FILE_ITEM_RIGHT
        } else {
            FILE_ITEM_LEFT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TEXT_ITEM_LEFT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_text, parent, false)
                view.layoutDirection = myMessageDirection
                TextViewHolder(view, this)
            }
            TEXT_ITEM_RIGHT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_text, parent, false)
                view.layoutDirection = otherMessageDirection
                TextViewHolder(view, this)
            }
            VOICE_ITEM_LEFT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_voice, parent, false)
                view.layoutDirection = myMessageDirection
                VoiceViewHolder(view, this)
            }
            VOICE_ITEM_RIGHT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_voice, parent, false)
                view.layoutDirection = otherMessageDirection
                VoiceViewHolder(view, this)
            }
            FILE_ITEM_LEFT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_file, parent, false)
                view.layoutDirection = myMessageDirection
                FileViewHolder(view, this)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_file, parent, false)
                view.layoutDirection = otherMessageDirection
                FileViewHolder(view, this)
            }
        }
    }

    fun selectMessage(messageId: String) {
        if (messagesList.any { it.messageId == messageId }) {
            changeMessageDetailsVisibility(true)
            selectedMessage = messagesList.first { it.messageId == messageId }
            notifyItemChanged(messagesList.indexOfFirst { it.messageId == messageId })
        }
    }

    fun areMessagesSelected(): Boolean = selectedMessage != null

    fun removeSelectedMessage() {
        changeMessageDetailsVisibility(false)
        if (selectedMessage != null) {
            val messageId = selectedMessage!!.messageId
            selectedMessage = null
            notifyItemChanged(messagesList.indexOfFirst { it.messageId == messageId })
        }
    }

    fun getSelectedMessage(): Message? = selectedMessage

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatItem = messagesList[position]

        if (getItemViewType(position) == TEXT_ITEM_LEFT || getItemViewType(position) == TEXT_ITEM_RIGHT) {
            (holder as TextViewHolder).bind(chatItem)
        }

        if (getItemViewType(position) == VOICE_ITEM_LEFT || getItemViewType(position) == VOICE_ITEM_RIGHT) {
            (holder as VoiceViewHolder).bind(chatItem)
        }

        if (getItemViewType(position) == FILE_ITEM_LEFT || getItemViewType(position) == FILE_ITEM_RIGHT) {
            (holder as FileViewHolder).bind(chatItem)
        }
    }

    override fun getItemCount(): Int {
        return messagesList.size
    }

    fun getItemById(position: Int): Message {
        return messagesList[position]
    }

    fun updatePlayingItem(path: String, messageId: String) {
        if (currentAudioPath != messageId) {
            mediaPlayer.reset()
            currentAudioPath = messageId
            if (File(path).exists()) {
                mediaPlayer.setDataSource(path)
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener(this)
                mediaPlayer.setOnCompletionListener(this)
            }
        }
    }

    fun updateRepliesList(newReply: Message) {
        if (chatRepliesList.any { it.messageId == newReply.messageId }) {
            val oldReply = chatRepliesList.first { it.messageId == newReply.messageId }
            if (oldReply != newReply) {
                val index = chatRepliesList.indexOf(oldReply)
                chatRepliesList.removeAt(index)
            }
        }
        chatRepliesList.add(newReply)

        if (messagesList.any { it.repliedMessageId == newReply.messageId }) {
            val messagesWithReply =
                messagesList.filter { it.repliedMessageId == newReply.messageId }
            for (message in messagesWithReply) {
                notifyItemRangeChanged(0, itemCount)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateChatList(messageLists: ArrayList<Message>) {
        messagesList.clear()
        messagesList.addAll(messageLists)
        notifyItemRangeChanged(0, itemCount)
    }

    fun updateUserList(newUsersList: ArrayList<UserData>) {
        usersList = newUsersList
        notifyItemRangeChanged(0, itemCount)
    }

    class TextViewHolder(itemView: View, adapter: ChatAdapter) :
        RecyclerView.ViewHolder(itemView), OnClickListener, OnLongClickListener {

        private var replyMessage: Message? = null
        private var currentMessageId: String = ""

        private val mAdapter = adapter

        private val parentLayout: ConstraintLayout = itemView.findViewById(R.id.layout_parent)
        private val layoutContainer: ConstraintLayout =
            itemView.findViewById(R.id.layout_message_container)
        private val chatOwnerTV: TextView = itemView.findViewById(R.id.tv_chat_name)
        private val chatReplyTV: MentionTextView = itemView.findViewById(R.id.tv_chat_reply)
        private val chatTextTV: MentionTextView = itemView.findViewById(R.id.tv_chat_message)
        private val timeTV: TextView = itemView.findViewById(R.id.tv_message_time)
        private val messageStatus: ImageView = itemView.findViewById(R.id.iv_message_status)
        private val mediaLayout: ConstraintLayout = itemView.findViewById(R.id.layout_media)
        private val postMediaLayout = PostMediaLayout(itemView.context)

        init {
            chatReplyTV.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            itemView.setOnClickListener(this)
        }

        @SuppressLint("UseCompatLoadingForColorStateLists")
        fun bind(message: Message) {

            currentMessageId = message.messageId

            if (message.ownerId != mAdapter.myId) {
                messageStatus.visibility = GONE
            } else {
                when (message.status) {
                    "Delivered" -> {
                        messageStatus.setImageDrawable(
                            AppCompatResources.getDrawable(
                                mAdapter.context,
                                R.drawable.ic_check
                            )
                        )
                    }
                    "Seen" -> {
                        messageStatus.setImageDrawable(
                            AppCompatResources.getDrawable(
                                mAdapter.context,
                                R.drawable.ic_seen
                            )
                        )
                    }
                    "Sending" -> {
                        messageStatus.setImageDrawable(
                            AppCompatResources.getDrawable(
                                mAdapter.context,
                                R.drawable.ic_sending
                            )
                        )
                    }
                }
            }

            if (mAdapter.selectedMessage != null && mAdapter.selectedMessage!!.messageId == message.messageId) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    parentLayout.backgroundTintList = mAdapter.context.resources.getColorStateList(
                        R.color.text_color,
                        mAdapter.context.theme
                    )
                } else {
                    @Suppress("DEPRECATION")
                    parentLayout.backgroundTintList =
                        mAdapter.context.resources.getColorStateList(R.color.text_color)
                }

                parentLayout.alpha = .3f
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    parentLayout.backgroundTintList = mAdapter.context.resources.getColorStateList(
                        android.R.color.transparent,
                        mAdapter.context.theme
                    )
                } else {
                    @Suppress("DEPRECATION")
                    parentLayout.backgroundTintList =
                        mAdapter.context.resources.getColorStateList(android.R.color.transparent)
                }

                parentLayout.alpha = 1f
            }

            val params = layoutContainer.layoutParams as ConstraintLayout.LayoutParams
            params.width = 0
            if (message.mediaData.isNotEmpty()) {
                mediaLayout.removeAllViews()

                mediaLayout.visibility = VISIBLE
                postMediaLayout.setView(
                    message.messageId,
                    message.mediaData,
                    mediaLayout,
                    mAdapter.enlargeMedia
                )
            } else {
                params.width = WRAP_CONTENT
                mediaLayout.visibility = GONE
            }
            layoutContainer.requestLayout()


            if (message.repliedMessageId.isNotEmpty()) {
                chatReplyTV.visibility = VISIBLE //Show reply TV

                //If Replied message is text show it in the TV
                if (mAdapter.chatRepliesList.any { it.messageId == message.repliedMessageId }) {
                    replyMessage =
                        mAdapter.chatRepliesList.first { it.messageId == message.repliedMessageId }
                    if (replyMessage != null && replyMessage!!.message!!.isNotEmpty()) {

                        //Create a list of user data to use in mention text view
                        val mentionedUsersDataList = ArrayList<UserData>()

                        if (message.mentions.isNotEmpty()) {
                            for (userId in message.mentions) {
                                if (mAdapter.usersList.any { it.userId == userId } && !mentionedUsersDataList.any { it.userId == userId }) {
                                    mentionedUsersDataList.add(mAdapter.usersList.first { it.userId == userId })
                                }
                            }
                        }

                        chatReplyTV.setDescription(
                            replyMessage!!.message!!,
                            replyMessage!!.textWithTags!!,
                            message.mentions,
                            mentionedUsersDataList
                        )

                        //Else show "Replying to attachment" text instead
                    } else if (replyMessage != null && replyMessage!!.message!!.isEmpty()) {
                        mAdapter.context.getText(R.string.attachment_reply)
                            .also { chatReplyTV.text = it }
                    }
                }
            } else {
                chatReplyTV.visibility = GONE
            }

            timeTV.text = mAdapter.sdf.format(message.date!!)

            //Create a list of user data to use in mention text view
            val mentionedUsersDataList = ArrayList<UserData>()

            if (message.mentions.isNotEmpty()) {
                for (userId in message.mentions) {
                    if (mAdapter.usersList.any { it.userId == userId } && !mentionedUsersDataList.any { it.userId == userId }) {
                        mentionedUsersDataList.add(mAdapter.usersList.first { it.userId == userId })
                    }
                }
            }

            if (!message.message.isNullOrEmpty()) {
                chatTextTV.visibility = VISIBLE
                chatTextTV.setDescription(
                    message.message!!,
                    message.textWithTags!!,
                    message.mentions,
                    mentionedUsersDataList
                )
            } else {
                chatTextTV.visibility = GONE
            }

            if (mAdapter.usersList.any { it.userId == message.ownerId }) {
                val messageOwnerName =
                    mAdapter.usersList.first { it.userId == message.ownerId }.name
                chatOwnerTV.text = messageOwnerName
            }
        }

        override fun onClick(v: View?) {
            if (mAdapter.selectedMessage == null) {
                if (v == chatReplyTV) {
                    if (replyMessage != null) {
                        mAdapter.getReply(replyMessage!!)
                    }
                }
            } else {
                mAdapter.removeSelectedMessage()
            }
        }

        override fun onLongClick(v: View?): Boolean {
            if (currentMessageId.isNotEmpty()) {
                mAdapter.removeSelectedMessage()
                mAdapter.selectMessage(currentMessageId)
            }
            return true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    class VoiceViewHolder(itemView: View, adapter: ChatAdapter) :
        RecyclerView.ViewHolder(itemView), OnClickListener, OnTouchListener, OnLongClickListener {

        private val mAdapter = adapter

        val seekBar: SeekBar = itemView.findViewById(R.id.sb_voice_chat)
        val playStateBtn: ImageView = itemView.findViewById(R.id.iv_play_state)
        val timerTV: TextView = itemView.findViewById(R.id.tv_seek_time)

        private val chatOwnerTV: TextView = itemView.findViewById(R.id.tv_chat_name)
        private val chatReplyTV: MentionTextView = itemView.findViewById(R.id.tv_chat_reply)
        private val timeTV: TextView = itemView.findViewById(R.id.tv_message_time)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
        private val messageStatus: ImageView = itemView.findViewById(R.id.iv_message_status)
        private val parentLayout: ConstraintLayout = itemView.findViewById(R.id.layout_parent)

        private var replyMessage: Message? = null
        private var currentMessageId: String = ""

        var audioDuration = 0
        var currentMessage: Message? = null

        init {
            //Set on Click Listeners for widgets
            playStateBtn.setOnClickListener(this)
            chatReplyTV.setOnClickListener(this)

            itemView.setOnLongClickListener(this)
            itemView.setOnClickListener(this)

            //Set on Click Listeners for widgets
            seekBar.setOnTouchListener(this)

            //If chat is anonymous hide names


            seekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (currentMessage != null) {
                        if (fromUser && mAdapter.isSameItemPlaying(currentMessage!!.messageId)) {
                            mAdapter.mediaPlayer.seekTo(seekBar!!.progress)
                        }
                    }

                    "${mAdapter.convertLongToTime(progress.toLong())} / ${
                        mAdapter.convertLongToTime(audioDuration.toLong())
                    }"
                        .also { timerTV.text = it }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            })
        }

        @SuppressLint("UseCompatLoadingForColorStateLists")
        fun bind(message: Message) {
            currentMessageId = message.messageId

            currentMessage = message

            if (message.ownerId != mAdapter.myId) {
                messageStatus.visibility = GONE
            } else {
                when (message.status) {
                    "Delivered" -> {
                        messageStatus.setImageDrawable(
                            AppCompatResources.getDrawable(
                                mAdapter.context,
                                R.drawable.ic_check
                            )
                        )
                    }
                    "Seen" -> {
                        messageStatus.setImageDrawable(
                            AppCompatResources.getDrawable(
                                mAdapter.context,
                                R.drawable.ic_seen
                            )
                        )
                    }
                    "Sending" -> {
                        messageStatus.setImageDrawable(
                            AppCompatResources.getDrawable(
                                mAdapter.context,
                                R.drawable.ic_sending
                            )
                        )
                    }
                }
            }

            if (mAdapter.selectedMessage != null && mAdapter.selectedMessage!!.messageId == message.messageId) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    parentLayout.backgroundTintList = mAdapter.context.resources.getColorStateList(
                        R.color.text_color,
                        mAdapter.context.theme
                    )
                } else {
                    @Suppress("DEPRECATION")
                    parentLayout.backgroundTintList =
                        mAdapter.context.resources.getColorStateList(R.color.text_color)
                }

                parentLayout.alpha = .3f
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    parentLayout.backgroundTintList = mAdapter.context.resources.getColorStateList(
                        android.R.color.transparent,
                        mAdapter.context.theme
                    )
                } else {
                    @Suppress("DEPRECATION")
                    parentLayout.backgroundTintList =
                        mAdapter.context.resources.getColorStateList(android.R.color.transparent)
                }

                parentLayout.alpha = 1f
            }

            //Show play button and hide progress if message is loaded
            if (message.mediaData.isNotEmpty() && !message.mediaData[0].media.isNullOrEmpty()) {
                progressBar.visibility = GONE
                playStateBtn.visibility = VISIBLE

                audioDuration = message.mediaData[0].duration!!
                seekBar.max = audioDuration - 500

                "00:00 / ${mAdapter.convertLongToTime(audioDuration.toLong())}"
                    .also { timerTV.text = it }
            }

            //Set message time TV
            timeTV.text = mAdapter.sdf.format(message.date!!)

            //Check if message has replies
            if (message.repliedMessageId.isNotEmpty()) {
                chatReplyTV.visibility = VISIBLE //Show reply TV
                //If Replied message is text show it in the TV
                if (mAdapter.chatRepliesList.any { it.messageId == message.repliedMessageId }) {
                    replyMessage =
                        mAdapter.chatRepliesList.first { it.messageId == message.repliedMessageId }
                    if (replyMessage != null && replyMessage!!.message!!.isNotEmpty()) {

                        //Create a list of user data to use in mention text view
                        val mentionedUsersDataList = ArrayList<UserData>()

                        if (message.mentions.isNotEmpty()) {
                            for (userId in message.mentions) {
                                if (mAdapter.usersList.any { it.userId == userId } && !mentionedUsersDataList.any { it.userId == userId }) {
                                    mentionedUsersDataList.add(mAdapter.usersList.first { it.userId == userId })
                                }
                            }
                        }

                        chatReplyTV.setDescription(
                            replyMessage!!.message!!,
                            replyMessage!!.textWithTags!!,
                            message.mentions,
                            mentionedUsersDataList
                        )

                        //Else show "Replying to attachment" text instead
                    } else if (replyMessage != null && replyMessage!!.message!!.isEmpty()) {
                        mAdapter.context.getText(R.string.attachment_reply)
                            .also { chatReplyTV.text = it }
                    }
                }
            }

            if (mAdapter.usersList.any { it.userId == message.ownerId }) {
                val messageOwnerName =
                    mAdapter.usersList.first { it.userId == message.ownerId }.name
                chatOwnerTV.text = messageOwnerName
            }
        }

        override fun onClick(p0: View?) {

            if (mAdapter.selectedMessage == null) {
                if (p0 == playStateBtn) {

                    if (currentMessage != null) {

                        if (currentMessage!!.mediaData.isNotEmpty() && !currentMessage!!.mediaData[0].media.isNullOrEmpty()
                            && !mAdapter.isSameItemPlaying(
                                currentMessage!!.messageId
                            )
                        ) {
                            mAdapter.audioHolder = this
                            mAdapter.updatePlayingItem(
                                currentMessage!!.mediaData[0].media!!,
                                currentMessage!!.messageId
                            )
                            mAdapter.pauseMedia(adapterPosition)
                        }
                    }

                    if (mAdapter.mediaPlayer.isPlaying) {
                        mAdapter.mediaPlayer.pause()

                        playStateBtn.setImageDrawable(
                            (ContextCompat.getDrawable(
                                mAdapter.context,
                                R.drawable.ic_play
                            ))
                        )
                    } else {
                        mAdapter.mediaPlayer.start()

                        playStateBtn.setImageDrawable(
                            (ContextCompat.getDrawable(
                                mAdapter.context,
                                R.drawable.ic_pause
                            ))
                        )
                    }

                    //mAdapter.pauseMedia(adapterPosition)
                } else if (p0 == chatReplyTV) {
                    if (replyMessage != null) {
                        mAdapter.getReply(replyMessage!!)
                    }
                }
            } else {
                mAdapter.removeSelectedMessage()
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
            if (p1!!.action == MotionEvent.ACTION_DOWN || p1.action == MotionEvent.ACTION_MOVE) {
                mAdapter.scrollAllowed(false)
            } else {
                mAdapter.scrollAllowed(true)
            }
            return false
        }

        override fun onLongClick(v: View?): Boolean {
            if (currentMessageId.isNotEmpty()) {
                mAdapter.removeSelectedMessage()
                mAdapter.selectMessage(currentMessageId)
            }
            return true
        }
    }

    class FileViewHolder(itemView: View, adapter: ChatAdapter) :
        RecyclerView.ViewHolder(itemView), OnClickListener, OnLongClickListener {

        private val mAdapter = adapter
        private val chatOwnerTV: TextView = itemView.findViewById(R.id.tv_chat_name)
        private val chatReplyTV: MentionTextView = itemView.findViewById(R.id.tv_chat_reply)
        private val fileNameTV: TextView = itemView.findViewById(R.id.tv_file_name)
        private val timeTV: TextView = itemView.findViewById(R.id.tv_message_time)
        private val messageStatus: ImageView = itemView.findViewById(R.id.iv_message_status)
        private val downloadBtn: ImageView = itemView.findViewById(R.id.iv_download)
        private val parentLayout: ConstraintLayout = itemView.findViewById(R.id.layout_parent)

        private var message: Message? = null
        private var currentMessageId: String = ""

        init {
            downloadBtn.setOnClickListener(this)
            fileNameTV.setOnClickListener(this)

            itemView.setOnLongClickListener(this)
            itemView.setOnClickListener(this)


        }

        @SuppressLint("UseCompatLoadingForColorStateLists")
        fun bind(message: Message) {
            currentMessageId = message.messageId

            this.message = message

            if (message.ownerId != mAdapter.myId) {
                messageStatus.visibility = GONE
            } else {
                when (message.status) {
                    "Delivered" -> {
                        messageStatus.setImageDrawable(
                            AppCompatResources.getDrawable(
                                mAdapter.context,
                                R.drawable.ic_check
                            )
                        )
                    }
                    "Seen" -> {
                        messageStatus.setImageDrawable(
                            AppCompatResources.getDrawable(
                                mAdapter.context,
                                R.drawable.ic_seen
                            )
                        )
                    }
                    "Sending" -> {
                        messageStatus.setImageDrawable(
                            AppCompatResources.getDrawable(
                                mAdapter.context,
                                R.drawable.ic_sending
                            )
                        )
                    }
                }
            }

            if (mAdapter.selectedMessage != null && mAdapter.selectedMessage!!.messageId == message.messageId) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    parentLayout.backgroundTintList = mAdapter.context.resources.getColorStateList(
                        R.color.text_color,
                        mAdapter.context.theme
                    )
                } else {
                    @Suppress("DEPRECATION")
                    parentLayout.backgroundTintList =
                        mAdapter.context.resources.getColorStateList(R.color.text_color)
                }

                parentLayout.alpha = .3f
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    parentLayout.backgroundTintList = mAdapter.context.resources.getColorStateList(
                        android.R.color.transparent,
                        mAdapter.context.theme
                    )
                } else {
                    @Suppress("DEPRECATION")
                    parentLayout.backgroundTintList =
                        mAdapter.context.resources.getColorStateList(android.R.color.transparent)
                }

                parentLayout.alpha = 1f
            }

            if (!message.mediaData[0].media.isNullOrEmpty()) {
                downloadBtn.visibility = GONE
            }

            "${message.message}.${message.mediaData[0].type}".also { fileNameTV.text = it }

            timeTV.text = mAdapter.sdf.format(message.date!!)


            //ToDo: Contains or == ?
            if (mAdapter.usersList.any { it.userId == message.ownerId }) {
                val messageOwnerName =
                    mAdapter.usersList.first { it.userId == message.ownerId }.name
                chatOwnerTV.text = messageOwnerName
            }
        }

        override fun onClick(v: View?) {
            if (mAdapter.selectedMessage == null) {
                if (v!! == downloadBtn && message != null) {
                    mAdapter.downloadMedia(message!!)
                } else if (v == fileNameTV && message != null && message!!.mediaData[0].media != null && message!!.mediaData[0].media!!.isNotEmpty()
                ) {
                    val file = File(message!!.mediaData[0].media!!)
                    val uri = Uri.fromFile(file)
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.setDataAndType(uri, message!!.mediaData[0].type!!)
                    try {
                        itemView.context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.d("OPEN_FILE_ERROR", e.toString())
                    }
                }
            } else {
                mAdapter.removeSelectedMessage()
            }
        }

        override fun onLongClick(v: View?): Boolean {
            if (currentMessageId.isNotEmpty()) {
                mAdapter.removeSelectedMessage()
                mAdapter.selectMessage(currentMessageId)
            }
            return true
        }

    }

    override fun onPrepared(mp: MediaPlayer?) {
        if (audioHolder != null) {

            mp!!.seekTo(audioHolder!!.seekBar.progress)

            audioHolder!!.playStateBtn.setImageDrawable(
                (ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_pause
                ))
            )

            //Timer to ser seekbar progress
            val seekBarTimer = Timer()
            seekBarTimer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    if (mp.currentPosition > 0) {
                        audioHolder!!.seekBar.progress = mp.currentPosition
                    }
                }
            }, 0, 50)
        }
        mp!!.start()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if (audioHolder != null) {
            //audioHolder!!.seekBar.progress = 0
            audioHolder!!.playStateBtn.setImageDrawable(
                (ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_play
                ))
            )
        }
    }

    fun convertLongToTime(time: Long): String {
        val date = Date(time)
        return if (time >= 3600000) {
            val format = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
            format.format(date)
        } else {
            val format = SimpleDateFormat("mm:ss", Locale.ENGLISH)
            format.format(date)
        }
    }

    fun isSameItemPlaying(messageID: String): Boolean {
        return currentAudioPath == messageID
    }
}