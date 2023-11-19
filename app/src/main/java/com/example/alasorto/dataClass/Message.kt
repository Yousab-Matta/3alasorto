package com.example.alasorto.dataClass

import androidx.room.Embedded
import java.util.*
import kotlin.collections.ArrayList

data class Message(
    var message: String? = "",
    val ownerId: String = "",
    val messageId: String = "",
    val messageType: String = "",
    val repliedMessageId: String = "",
    var date: Date? = null,
    var status: String = "",
    var groupChat: Boolean = false,
    //ToDo: if you want to make arraylist in a room DB do it must be not nullable
    val seenBy: ArrayList<String> = ArrayList(),
    @Embedded var mediaData: MediaData? = null
)
