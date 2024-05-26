package com.example.alasorto.dataClass

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import java.util.*
import kotlin.collections.ArrayList

data class Attendance(
    val day: Int = 0,
    val month: Int = 0,
    val year: Int = 0,
    val attendancePercentage: Float = 0f,
    val id: String = "",
    val date: Date? = null,
    var usersIDs: ArrayList<String> = ArrayList()
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readFloat(),
        parcel.readString()!!,
        parcel.readValue(Date::class.java.classLoader) as? Date,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayListOf<String>().apply {
                parcel.readList(this, String::class.java.classLoader, String::class.java)
            }
        } else {
            arrayListOf<String>().apply {
                parcel.readList(this, String::class.java.classLoader)
            }
        })

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(day)
        parcel.writeInt(month)
        parcel.writeInt(year)
        parcel.writeFloat(attendancePercentage)
        parcel.writeString(id)
        parcel.writeValue(date)
        parcel.writeList(usersIDs)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Attendance> {
        override fun createFromParcel(parcel: Parcel): Attendance {
            return Attendance(parcel)
        }

        override fun newArray(size: Int): Array<Attendance?> {
            return arrayOfNulls(size)
        }
    }
}