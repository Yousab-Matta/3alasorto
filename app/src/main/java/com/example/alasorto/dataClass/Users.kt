package com.example.alasorto.dataClass

import android.os.Parcel
import android.os.Parcelable

data class Users(
    var Name: String? = null,
    var AttendedTimes: Int? = null,
    var AttendedPercent: Float? = null,
    var Access: String? = null,
    var Location: String? = null,
    var Address: String? = null,
    var ConfessionPriest: String? = null,
    var Phone: String? = null,
    var BirthDay: Int? = null,
    var BirthMonth: Int? = null,
    var BirthYear: Int? = null,
    var Points: Int? = null,
    var College: String? = null,
    var University: String? = null,
    var Status: String? = null,
    var Token: String? = null,
    var StatusYear: Int? = null,
    var ImageLink: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(Name)
        parcel.writeValue(AttendedTimes)
        parcel.writeValue(AttendedPercent)
        parcel.writeString(Access)
        parcel.writeString(Location)
        parcel.writeString(Address)
        parcel.writeString(ConfessionPriest)
        parcel.writeString(Phone)
        parcel.writeValue(BirthDay)
        parcel.writeValue(BirthMonth)
        parcel.writeValue(BirthYear)
        parcel.writeValue(Points)
        parcel.writeString(College)
        parcel.writeString(University)
        parcel.writeString(Status)
        parcel.writeString(Token)
        parcel.writeValue(StatusYear)
        parcel.writeString(ImageLink)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Users> {
        override fun createFromParcel(parcel: Parcel): Users {
            return Users(parcel)
        }

        override fun newArray(size: Int): Array<Users?> {
            return arrayOfNulls(size)
        }
    }
}