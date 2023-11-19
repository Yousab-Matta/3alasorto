package com.example.alasorto.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.input.key.Key.Companion.F
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alasorto.R
import com.example.alasorto.dataClass.Message
import com.example.alasorto.dataClass.UserData
import com.google.android.material.imageview.ShapeableImageView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class ChatAdapter(
    private val chatList: ArrayList<Message>,
    private val chatOwnersList: ArrayList<UserData>,
    private val myId: String,
    private val context: Context,
    private val scrollAllowed: (Boolean) -> Unit,
    private val getReply: (Message) -> Unit,
    private val pauseMedia: (Int) -> Unit,
    private val enlargeImage: (String) -> Unit,
    private val enlargeVideo: (String?, Uri?) -> Unit,
    private val isAnonymous: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), OnPreparedListener,
    MediaPlayer.OnCompletionListener {

    private val chatRepliesList = HashMap<String, Message>()

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
        val message = chatList[position]
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
            IMAGE_ITEM_LEFT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_image, parent, false)
                view.layoutDirection = myMessageDirection
                ImageViewHolder(view, this)
            }
            IMAGE_ITEM_RIGHT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_image, parent, false)
                view.layoutDirection = otherMessageDirection
                ImageViewHolder(view, this)
            }
            VIDEO_ITEM_LEFT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_video, parent, false)
                view.layoutDirection = myMessageDirection
                VideoViewHolder(view, this)
            }
            VIDEO_ITEM_RIGHT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_video, parent, false)
                view.layoutDirection = otherMessageDirection
                VideoViewHolder(view, this)
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatItem = chatList[position]

        if (getItemViewType(position) == TEXT_ITEM_LEFT || getItemViewType(position) == TEXT_ITEM_RIGHT) {
            (holder as TextViewHolder).bind(chatItem)
        }

        if (getItemViewType(position) == VOICE_ITEM_LEFT || getItemViewType(position) == VOICE_ITEM_RIGHT) {
            (holder as VoiceViewHolder).bind(chatItem)
        }

        if (getItemViewType(position) == IMAGE_ITEM_LEFT || getItemViewType(position) == IMAGE_ITEM_RIGHT) {
            (holder as ImageViewHolder).bind(chatItem)
        }

        if (getItemViewType(position) == VIDEO_ITEM_LEFT || getItemViewType(position) == VIDEO_ITEM_RIGHT) {
            (holder as VideoViewHolder).bind(chatItem)
        }

        if (getItemViewType(position) == FILE_ITEM_LEFT || getItemViewType(position) == FILE_ITEM_RIGHT) {
            (holder as FileViewHolder).bind(chatItem)
        }
    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    fun getItemById(position: Int): Message {
        return chatList[position]
    }

    fun updatePlayingItem(path: String, messageId: String) {
        if (currentAudioPath != messageId) {
            mediaPlayer.reset()
            currentAudioPath = messageId
            mediaPlayer.setDataSource(path)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener(this)
            mediaPlayer.setOnCompletionListener(this)
        }
    }

    fun updateRepliesList(replyId: String, repliedMessage: Message) {
        if (!chatRepliesList.containsKey(replyId)) {
            chatRepliesList[replyId] = repliedMessage
            if (chatList.any { it.repliedMessageId.contains(replyId) }) {
                val updatedList =
                    chatList.filter { it.repliedMessageId.contains(it.messageId) }
                for (message in updatedList) {
                    notifyItemChanged(chatList.indexOf(message))
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(messageLists: ArrayList<Message>) {
        chatList.clear()
        chatList.addAll(messageLists)
        notifyItemRangeChanged(0, itemCount)
    }

    class TextViewHolder(itemView: View, adapter: ChatAdapter) :
        RecyclerView.ViewHolder(itemView), OnClickListener {

        private var replyMessage: Message? = null

        private val mAdapter = adapter

        private val chatOwnerTV: TextView = itemView.findViewById(R.id.tv_chat_name)
        private val chatReplyTV: TextView = itemView.findViewById(R.id.tv_chat_reply)
        private val chatMessage: TextView = itemView.findViewById(R.id.tv_chat_message)
        private val timeTV: TextView = itemView.findViewById(R.id.tv_message_time)
        private val messageStatus: ImageView = itemView.findViewById(R.id.iv_message_status)

        init {
            chatReplyTV.setOnClickListener(this)
            if (mAdapter.isAnonymous) {
                chatOwnerTV.visibility = GONE
            }
        }

        fun bind(message: Message) {
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

            if (message.repliedMessageId.isNotEmpty()) {
                chatReplyTV.visibility = VISIBLE //Show reply TV

                //If Replied message is text show it in the TV
                if (mAdapter.chatRepliesList.containsKey(message.messageId)) {
                    replyMessage = mAdapter.chatRepliesList[message.messageId]
                    if (replyMessage != null && replyMessage!!.message!!.isNotEmpty()) {
                        chatReplyTV.text = replyMessage!!.message

                        //Else show "Replying to attachment" text instead
                    } else if (replyMessage != null && replyMessage!!.message!!.isEmpty()) {
                        mAdapter.context.getText(R.string.attachment_reply)
                            .also { chatReplyTV.text = it }
                    }
                }
            }

            timeTV.text = mAdapter.sdf.format(message.date!!)

            chatMessage.text = message.message

            val owner = mAdapter.chatOwnersList.first { it.phone.contains(message.ownerId) }
            chatOwnerTV.text = owner.name
        }

        override fun onClick(v: View?) {
            if (v == chatReplyTV) {
                if (replyMessage != null) {
                    mAdapter.getReply(replyMessage!!)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    class VoiceViewHolder(itemView: View, adapter: ChatAdapter) :
        RecyclerView.ViewHolder(itemView), OnClickListener, OnTouchListener {

        private val mAdapter = adapter

        val seekBar: SeekBar = itemView.findViewById(R.id.sb_voice_chat)
        val playStateBtn: ImageView = itemView.findViewById(R.id.iv_play_state)
        val timerTV: TextView = itemView.findViewById(R.id.tv_seek_time)

        private val chatOwnerTV: TextView = itemView.findViewById(R.id.tv_chat_name)
        private val chatReplyTV: TextView = itemView.findViewById(R.id.tv_chat_reply)
        private val timeTV: TextView = itemView.findViewById(R.id.tv_message_time)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
        private val messageStatus: ImageView = itemView.findViewById(R.id.iv_message_status)

        private var replyMessage: Message? = null

        var audioDuration = 0
        var currentMessage: Message? = null

        init {
            //Set on Click Listeners for widgets
            playStateBtn.setOnClickListener(this)
            chatReplyTV.setOnClickListener(this)

            //Set on Click Listeners for widgets
            seekBar.setOnTouchListener(this)

            //If chat is anonymous hide names
            if (mAdapter.isAnonymous) {
                chatOwnerTV.visibility = GONE
            }

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

        fun bind(message: Message) {
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

            //Show play button and hide progress if message is loaded
            if (message.mediaData!!.media != null && message.mediaData!!.media!!.isNotEmpty()) {
                progressBar.visibility = GONE
                playStateBtn.visibility = VISIBLE

                audioDuration = message.mediaData!!.duration!!
                seekBar.max = audioDuration

                "00:00 / ${mAdapter.convertLongToTime(audioDuration.toLong())}"
                    .also { timerTV.text = it }
            }

            //Set message time TV
            timeTV.text = mAdapter.sdf.format(message.date!!)

            //Check if message has replies
            if (message.repliedMessageId.isNotEmpty()) {
                chatReplyTV.visibility = VISIBLE //Show reply TV
                //If Replied message is text show it in the TV
                if (mAdapter.chatRepliesList.containsKey(message.messageId))
                    replyMessage = mAdapter.chatRepliesList[message.messageId]
                if (replyMessage!!.message!!.isNotEmpty()) {
                    chatReplyTV.text = replyMessage!!.message
                } else { //Else show "Replying to attachment" text instead
                    mAdapter.context.getText(R.string.attachment_reply)
                        .also { chatReplyTV.text = it }
                }
            }

            val owner = mAdapter.chatOwnersList.first { it.phone.contains(message.ownerId) }
            chatOwnerTV.text = owner.name
        }

        override fun onClick(p0: View?) {
            if (p0 == playStateBtn) {

                if (currentMessage!!.mediaData!!.media != null && !mAdapter.isSameItemPlaying(
                        currentMessage!!.messageId
                    )
                ) {
                    mAdapter.audioHolder = this
                    mAdapter.updatePlayingItem(
                        currentMessage!!.mediaData!!.media!!,
                        currentMessage!!.messageId
                    )
                    mAdapter.pauseMedia(adapterPosition)
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
    }

    //ToDo: reply TV
    @SuppressLint("ClickableViewAccessibility")
    class ImageViewHolder(itemView: View, adapter: ChatAdapter) :
        RecyclerView.ViewHolder(itemView), OnClickListener {

        var maxWidth: Int = 0

        private val mAdapter = adapter
        private var message: Message? = null

        private val chatOwnerTV: TextView = itemView.findViewById(R.id.tv_chat_name)
        private val chatImageIV: ShapeableImageView = itemView.findViewById(R.id.iv_chat)
        private val timeTV: TextView = itemView.findViewById(R.id.tv_message_time)
        private val messageStatus: ImageView = itemView.findViewById(R.id.iv_message_status)

        init {
            if (mAdapter.isAnonymous) {
                chatOwnerTV.visibility = GONE
            }
            chatImageIV.setOnClickListener(this)
        }

        fun bind(message: Message) {
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

            timeTV.text = mAdapter.sdf.format(message.date!!)

            val owner = mAdapter.chatOwnersList.first { it.phone.contains(message.ownerId) }
            chatOwnerTV.text = owner.name

            if (message.mediaData != null) {

                val mediaLink: String? = message.mediaData!!.mediaLink


                if (mediaLink != null && mediaLink.isNotEmpty()) {
                    Glide.with(chatImageIV).load(mediaLink).into(chatImageIV)

                    val width = message.mediaData!!.width
                    val height = message.mediaData!!.height
                    (chatImageIV.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio =
                        "${width}:${height}"
                    chatImageIV.requestLayout()

                } else if (message.mediaData!!.media != null) {
                    chatImageIV.setImageURI(Uri.parse(message.mediaData!!.media))
                    chatImageIV.layoutParams.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                    chatImageIV.requestLayout()
                }
            }
        }

        override fun onClick(v: View?) {
            if (v == chatImageIV) {
                if (message != null && message!!.mediaData!!.mediaLink!!.isNotEmpty()) {
                    mAdapter.enlargeImage(message!!.mediaData!!.mediaLink!!)
                }
            }
        }
    }

    class VideoViewHolder(itemView: View, adapter: ChatAdapter) :
        RecyclerView.ViewHolder(itemView), OnClickListener {

        private val mAdapter = adapter
        private val chatOwnerTV: TextView = itemView.findViewById(R.id.tv_chat_name)
        private val videoView: ConstraintLayout = itemView.findViewById(R.id.chat_video)
        private val timeTV: TextView = itemView.findViewById(R.id.tv_message_time)
        private val messageStatus: ImageView = itemView.findViewById(R.id.iv_message_status)

        private var message: Message? = null

        init {
            if (mAdapter.isAnonymous) {
                chatOwnerTV.visibility = GONE
            }
            videoView.setOnClickListener(this)
        }

        fun bind(message: Message) {
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

            timeTV.text = mAdapter.sdf.format(message.date!!)

            val owner = mAdapter.chatOwnersList.first { it.phone.contains(message.ownerId) }
            chatOwnerTV.text = owner.name

            if (message.mediaData != null) {
                (videoView.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio =
                    "${message.mediaData!!.width}:${message.mediaData!!.height}"
                videoView.requestLayout()
            }
        }

        override fun onClick(v: View?) {
            if (v == videoView) {
                //ToDo
                if (message!!.mediaData!!.mediaLink != null && message!!.mediaData!!.mediaLink!!.isNotEmpty()) {
                    mAdapter.enlargeVideo(message!!.mediaData!!.mediaLink!!, null)
                } else if (message?.mediaData?.media != null && message!!.ownerId == mAdapter.myId) {
                    mAdapter.enlargeVideo(null, Uri.parse(message!!.mediaData!!.media))
                }
            }
        }
    }

    class FileViewHolder(itemView: View, adapter: ChatAdapter) :
        RecyclerView.ViewHolder(itemView), OnClickListener {

        private val mAdapter = adapter
        private val chatOwnerTV: TextView = itemView.findViewById(R.id.tv_chat_name)
        private val chatReplyTV: TextView = itemView.findViewById(R.id.tv_chat_reply)
        private val fileNameTV: TextView = itemView.findViewById(R.id.tv_file_name)
        private val timeTV: TextView = itemView.findViewById(R.id.tv_message_time)
        private val messageStatus: ImageView = itemView.findViewById(R.id.iv_message_status)
        private val downloadBtn: ImageView = itemView.findViewById(R.id.iv_download)

        private var message: Message? = null

        init {
            if (mAdapter.isAnonymous) {
                chatOwnerTV.visibility = GONE
            }
        }

        fun bind(message: Message) {
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

            fileNameTV.text = message.message

            timeTV.text = mAdapter.sdf.format(message.date!!)

            val owner = mAdapter.chatOwnersList.first { it.phone.contains(message.ownerId) }
            chatOwnerTV.text = owner.name
        }

        override fun onClick(v: View?) {

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