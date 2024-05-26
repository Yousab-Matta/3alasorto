package com.example.alasorto.dataClass

import androidx.room.Embedded
import java.util.*
import kotlin.collections.ArrayList

data class Message(
    var message: String? = "",
    var textWithTags: String? = "",
    val ownerId: String = "",
    val messageId: String = "",
    val messageType: String = "",
    val repliedMessageId: String = "",
    var date: Date? = null,
    var status: String = "",
    var groupChat: Boolean = false,
    val seenBy: ArrayList<SeenBy> = ArrayList(),
    val mentions: ArrayList<String> = ArrayList(),
    var mediaData: ArrayList<MediaData> = ArrayList()
)