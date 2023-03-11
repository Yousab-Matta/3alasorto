package com.example.alasorto.dataClass

import android.os.Parcel
import android.os.Parcelable
import java.util.*

data class Posts(
    val title: String? = null,
    val description: String? = null,
    val imageLink: String? = null,
    val id: String? = null,
    val ownerID: String = "",
    val postDate: Date? = null,
    val day: Int? = null,
    val month: Int? = null,
    val year: Int? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readValue(Date::class.java.classLoader) as? Date,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(p0: Parcel, p1: Int) {
        p0.writeString(title)
        p0.writeString(description)
        p0.writeString(imageLink)
        p0.writeString(id)
        p0.writeString(ownerID)
        p0.writeValue(postDate)
        if (day != null) {
            p0.writeInt(day)
        }
        if (month != null) {
            p0.writeInt(month)
        }
        if (year != null) {
            p0.writeInt(year)
        }
    }

    companion object CREATOR : Parcelable.Creator<Posts> {
        override fun createFromParcel(parcel: Parcel): Posts {
            return Posts(parcel)
        }

        override fun newArray(size: Int): Array<Posts?> {
            return arrayOfNulls(size)
        }
    }
}