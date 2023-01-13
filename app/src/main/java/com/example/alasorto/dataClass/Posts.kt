package com.example.alasorto.dataClass

import android.os.Parcel
import android.os.Parcelable
import java.util.*

data class Posts(
    val Title: String? = null,
    val Description: String? = null,
    val ImageLink: String? = null,
    val ID: String? = null,
    val OwnerID: String? = null,
    val PostDate: Date? = null,
    val Day: Int? = null,
    val Month: Int? = null,
    val Year: Int? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Date::class.java.classLoader) as? Date,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(p0: Parcel, p1: Int) {
        p0.writeString(Title)
        p0.writeString(Description)
        p0.writeString(ImageLink)
        p0.writeString(ID)
        p0.writeString(OwnerID)
        p0.writeValue(PostDate)
        if (Day != null) {
            p0.writeInt(Day)
        }
        if (Month != null) {
            p0.writeInt(Month)
        }
        if (Year != null) {
            p0.writeInt(Year)
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