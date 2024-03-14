package com.example.alasorto.dataClass

import java.util.*
import kotlin.collections.ArrayList

data class Comments(
    var postId: String = "",
    var comment: String = "",
    var textWithTags: String = "",
    var ownerId: String = "",
    var commentId: String = "",
    var date: Date? = null,
    var media: MediaData? = null,
    var mentionsList: ArrayList<String> = ArrayList()
)
