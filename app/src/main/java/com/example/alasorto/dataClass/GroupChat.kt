package com.example.alasorto.dataClass

import java.util.*

data class GroupChat(
    val message: String? = null,
    val ownerID: String? = null,
    val messageID: String? = null,
    val imageLink: String? = null,
    val messageType: String? = null,
    val date: Date? = null,
)
