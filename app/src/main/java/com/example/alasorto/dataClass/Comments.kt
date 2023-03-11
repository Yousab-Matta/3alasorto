package com.example.alasorto.dataClass

import java.util.*

data class Comments(
    val postID: String? = null,
    val comment: String? = null,
    val ownerID: String? = null,
    val commentID: String? = null,
    val date: Date? = null
)
