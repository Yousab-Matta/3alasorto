package com.example.alasorto.notification

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiInterface {

    companion object {
        const val BASE_URL = "https://fcm.googleapis.com/"
    }

    @Headers(
        "Authorization: key=AAAAXt_W-PU:APA91bHsTyky3uUHlYjuW377ephM-lRsA1aa7G1LdGCvxVwJzrRvqG5fLqQIMlVl8MzomflsYLl3XT2GPVzWN-DwnuQ0cmPTKx8A6PfnKxPwrZRLGXnl6A4txAlQUTnNPaRKPQt2N1hM",
        "Content-Type:application/json"
    )

    @POST("fcm/send")
    suspend fun sendNotification(@Body notificationModel: NotificationModel): retrofit2.Response<ResponseBody>

}