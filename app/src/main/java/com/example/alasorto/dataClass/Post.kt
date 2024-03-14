package com.example.alasorto.dataClass

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.room.Embedded
import java.util.Date
import kotlin.collections.ArrayList

data class Post(
    val description: String = "",
    val textWithTags: String = "",
    @Embedded(prefix = "media_items_")
    var mediaList: ArrayList<MediaData>? = ArrayList(),
    val postType: String = "",
    val postId: String = "",
    val ownerID: String = "",
    val postDate: Date? = null,
    val multiSelection: Boolean? = null,
    @Embedded(prefix = "poll_items_")
    var pollItems: ArrayList<Poll>? = ArrayList(),
    var mentionsList: ArrayList<String> = ArrayList()
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayListOf<MediaData>().apply {
                parcel.readList(this, MediaData::class.java.classLoader, MediaData::class.java)
            }
        } else {
            arrayListOf<MediaData>().apply {
                parcel.readList(this, MediaData::class.java.classLoader)
            }
        },
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readValue(Date::class.java.classLoader) as? Date,
        parcel.readValue(Boolean::class.java.classLoader) as Boolean,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayListOf<Poll>().apply {
                parcel.readList(this, Poll::class.java.classLoader, Poll::class.java)
            }
        } else {
            arrayListOf<Poll>().apply {
                parcel.readList(this, Poll::class.java.classLoader)
            }
        },
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayListOf<String>().apply {
                parcel.readList(this, Poll::class.java.classLoader, String::class.java)
            }
        } else {
            arrayListOf<String>().apply {
                parcel.readList(this, String::class.java.classLoader)
            }
        }
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(p0: Parcel, p1: Int) {
        p0.writeString(description)
        p0.writeString(textWithTags)
        p0.writeList(mediaList)
        p0.writeString(postType)
        p0.writeString(postId)
        p0.writeString(ownerID)
        p0.writeValue(postDate)
        p0.writeValue(multiSelection)
        p0.writeList(pollItems)
        p0.writeList(mentionsList)
    }

    companion object CREATOR : Parcelable.Creator<Post> {
        override fun createFromParcel(parcel: Parcel): Post {
            return Post(parcel)
        }

        override fun newArray(size: Int): Array<Post?> {
            return arrayOfNulls(size)
        }
    }
}