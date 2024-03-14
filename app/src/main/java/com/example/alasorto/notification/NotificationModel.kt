package com.example.alasorto.notification

data class NotificationModel(val to: String, val data: Data)

data class Data(
    val title: String,
    val message: String,
    val dataMap: HashMap<String, String>? = null
)