package com.example.alasorto.viewModels

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.alasorto.dataClass.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

@Suppress("DEPRECATION")
class AppViewModel : ViewModel() {
    private val db = Firebase.firestore

    var usersMLD = MutableLiveData<ArrayList<UserData>>()
    var pendingVerifyUsersMLD = MutableLiveData<ArrayList<UserData>>()
    var currentUserMLD = MutableLiveData<UserData?>()
    var attendanceListMLD = MutableLiveData<ArrayList<Attendance>>()
    var attendanceItemMLD = MutableLiveData<Attendance>()
    var dismissFragmentMLD = MutableLiveData<Boolean>()
    var safeSpaceCreatedMLD = MutableLiveData<SafeSpace>()
    var userByIdMLD = MutableLiveData<UserData?>()
    var remindersMLD = MutableLiveData<ArrayList<Reminder>>()
    var groupsMLD = MutableLiveData<ArrayList<Group>>()
    var pollImagesUrlMLD = MutableLiveData<String>()
    var galleryItemsMLD = MutableLiveData<ArrayList<GalleryItem>>()
    var galleryRequestsMLD = MutableLiveData<ArrayList<GalleryRequest>>()
    var galleryWishListMLD = MutableLiveData<ArrayList<GalleryWishList>>()
    var videoUploadPercent = MutableLiveData<Double>()
    var imageUploadedMLD = MutableLiveData<Boolean>()
    var coverUploadedMLD = MutableLiveData<Boolean>()
    var safeSpaceList = MutableLiveData<ArrayList<SafeSpace>>()
    var safeSpaceItemMLD = MutableLiveData<SafeSpace?>()
    var editSafeSpaceDetailsMLD = MutableLiveData<String>()
    var deletedUserAttMLD = MutableLiveData<Boolean>()
    var attendanceExistsMLD = MutableLiveData<Boolean>()
    var issuesListMLD = MutableLiveData<ArrayList<IssueData>>()
    var spiritualNoteListMLD = MutableLiveData<ArrayList<SpiritualNote>>()

    //Used to get the number of success events in case of repeating a function in a loop
    var counterMLD = MutableLiveData<Boolean>()

    fun createUser(user: UserData) {
        val reference = db.collection("Users").document(user.phone)
        val userMap = hashMapOf(
            "name" to user.name,
            "access" to user.access,
            "bio" to user.bio,
            "phone" to user.phone,
            "attendedTimes" to user.attendedTimes,
            "attendanceDue" to user.attendanceDue,
            "address" to user.address,
            "location" to user.location,
            "confessionPriest" to user.confessionPriest,
            "points" to user.points,
            "birthDay" to user.birthDay,
            "birthMonth" to user.birthMonth,
            "birthYear" to user.birthYear,
            "college" to user.college,
            "university" to user.university,
            "collegeStatus" to user.collegeStatus,
            "visibilityStatus" to user.visibilityStatus,
            "statusYear" to user.statusYear,
            "imageLink" to "",
            "coverImageLink" to "",
            "creationDate" to user.creationDate,
            "verified" to false,
            "token" to ""
        )

        Log.d("UPLOAD_TEST", user.phone)
        reference.set(userMap).addOnCompleteListener(OnCompleteListener {
            dismissFragmentMLD.value = it.isSuccessful
        })
    }

    fun editUser(user: UserData) {
        val reference = db.collection("Users").document(user.phone)
        reference.update(
            "name", user.name,
            "phone", user.phone,
            "address", user.address,
            "bio", user.bio,
            "location", user.location,
            "confessionPriest", user.confessionPriest,
            "points", user.points,
            "birthDay", user.birthDay,
            "birthMonth", user.birthMonth,
            "birthYear", user.birthYear,
            "college", user.college,
            "university", user.university,
            "status", user.collegeStatus,
            "statusYear", user.statusYear
        ).addOnCompleteListener(OnCompleteListener {
            dismissFragmentMLD.value = true
        })
    }

    fun setOnline() {
        val userId = Firebase.auth.currentUser!!.phoneNumber!!
        db.collection("Users").document(userId).update("visibilityStatus", "Online")
            .addOnCompleteListener {
                getCurrentUser()
            }
    }

    fun setOffline() {
        val userId = Firebase.auth.currentUser!!.phoneNumber!!
        db.collection("Users").document(userId).update("visibilityStatus", "Offline")
            .addOnCompleteListener {
                getCurrentUser()
            }
    }

    fun editUserImage(
        image: Uri?,
        cover: Uri?,
        contentResolver: ContentResolver,
        userId: String
    ) {
        uploadGroupImages(
            image,
            cover,
            "UserImage",
            "UserCover",
            "Users",
            userId,
            "imageLink",
            "coverImageLink",
            contentResolver
        )
    }

    fun getAllUsers() {
        val users: ArrayList<UserData> = ArrayList()
        val reference = db.collection("Users")
        reference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                users.addAll(it.result.toObjects(UserData::class.java))
                usersMLD.value = users
            }
        })
    }

    fun getUserById(id: String) {
        db.collection("Users").document(id).get()
            .addOnCompleteListener(OnCompleteListener {
                if (it.isSuccessful) {
                    if (it.result.exists()) {
                        userByIdMLD.value = it.result.toObject(UserData::class.java)
                    } else {
                        userByIdMLD.value = null
                    }
                }
            })
    }

    fun getCurrentUser() {
        if (Firebase.auth.currentUser != null) {
            val phoneNum = Firebase.auth.currentUser!!.phoneNumber
            val reference = db.collection("Users").document(phoneNum!!)
            reference.get().addOnCompleteListener(OnCompleteListener {
                if (it.isSuccessful) {
                    if (it.result.exists()) {
                        currentUserMLD.value = it.result.toObject(UserData::class.java)
                    } else {
                        currentUserMLD.value = null
                    }
                }
            })
        }
    }

    fun editUserPoints(pointsNum: Long, userId: String) {
        val reference = db.collection("Users").document(userId)
            .update("points", FieldValue.increment(pointsNum)).addOnSuccessListener {
                counterMLD.value = true
            }
    }

    fun updateUserBio(bio: String, userId: String) {
        val reference = db.collection("Users").document(userId)
        reference.update("bio", bio).addOnCompleteListener {
            dismissFragmentMLD.value = true
        }
    }

    fun editUserPermission(userId: String, permissions: ArrayList<String>) {
        val reference = db.collection("Users").document(userId)
        reference.update("access", permissions).addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                dismissFragmentMLD.value = true
            }
        })
    }

    fun verifyUser(userId: String, isVerified: Boolean) {
        val reference = db.collection("Users").document(userId)
        if (isVerified) {
            reference.update("verified", true)
        } else {
            //reference.delete()
        }
    }

    fun deleteUser(id: String) {
        db.collection("Users").document(id).delete().addOnSuccessListener {
            FirebaseStorage.getInstance().getReference("Users").child(id).delete()
                .addOnSuccessListener {
                    counterMLD.value = true
                }
        }
    }

    fun getPendingVerifyUsers() {
        db.collection("Users").whereEqualTo("verified", false)
            .get().addOnCompleteListener {
                pendingVerifyUsersMLD.value = ArrayList(it.result.toObjects(UserData::class.java))
            }
    }

    fun reportIssue(issue: IssueData) {
        val issueMap = hashMapOf(
            "ownerId" to issue.ownerId,
            "issueId" to issue.issueId,
            "issueText" to issue.issueText,
            "solved" to issue.solved,
            "date" to issue.date
        )
        db.collection("Issues").document(issue.issueId).set(issueMap).addOnCompleteListener {
            dismissFragmentMLD.value = true
        }
    }

    fun getIssues() {
        db.collection("Issues").get().addOnCompleteListener {
            if (it.isSuccessful) {
                issuesListMLD.value = ArrayList(it.result.toObjects(IssueData::class.java))
            }
        }
    }

    fun createAttendance(
        attendance: Attendance, allUsersIdsList: ArrayList<String>
    ) {
        db.collection("Attendance")
            .whereEqualTo("day", attendance.day)
            .whereEqualTo("month", attendance.month)
            .whereEqualTo("year", attendance.year).get().addOnCompleteListener {
                if (it.isSuccessful) {
                    if (it.result.documents.size >= 1) {
                        attendanceExistsMLD.value = true
                    } else {
                        //Create new Attendance data on Firebase
                        val reference = db.collection("Attendance")
                        val id = reference.document().id
                        //Get current Date

                        val attMap = hashMapOf(
                            "day" to attendance.day,
                            "month" to attendance.month,
                            "year" to attendance.year,
                            "id" to id,
                            "date" to Date(),
                            "usersIDs" to attendance.usersIDs
                        )
                        reference.document(id).set(attMap).addOnSuccessListener {
                            handleCreateUserAttendanceUser(attendance.usersIDs!!, allUsersIdsList)
                        }
                    }
                }
            }
    }

    fun editAttendance(
        addedUsersList: ArrayList<String>,
        removedUsersList: ArrayList<String>,
        updatedUsersList: ArrayList<String>,
        attendanceId: String
    ) {
        db.collection("Attendance").document(attendanceId)
            .update("usersIDs", updatedUsersList).addOnSuccessListener {
                handleEditAttendanceUsers(addedUsersList, removedUsersList)
            }
    }

    private fun handleCreateUserAttendanceUser(
        attendedUsersIds: ArrayList<String>,
        allUsersIds: ArrayList<String>
    ) {//Fun used to change attendance due and attended times for users
        val userReference = db.collection("Users").document(allUsersIds[0])


        userReference.update("attendanceDue", (FieldValue.increment(1))).addOnSuccessListener {
            //ToDo:clean if else code cuz im lazy rn
            if (attendedUsersIds.contains(allUsersIds[0])) {
                userReference.update("attendedTimes", (FieldValue.increment(1)))
                    .addOnSuccessListener {
                        allUsersIds.removeAt(0)
                        if (allUsersIds.size > 0) {
                            handleCreateUserAttendanceUser(attendedUsersIds, allUsersIds)
                        } else {
                            dismissFragmentMLD.value = true
                        }
                    }
            } else {
                allUsersIds.removeAt(0)
                if (allUsersIds.size > 0) {
                    handleCreateUserAttendanceUser(attendedUsersIds, allUsersIds)
                } else {
                    dismissFragmentMLD.value = true
                }
            }
        }
    }

    private fun handleEditAttendanceUsers(
        addedUsersIdsList: ArrayList<String>,
        removedUsersIdsList: ArrayList<String>
    ) {
        if (addedUsersIdsList.size > 0) {
            db.collection("Users").document(addedUsersIdsList[0])
                .update("attendedTimes", (FieldValue.increment(1)))
                .addOnSuccessListener {
                    addedUsersIdsList.removeAt(0)
                    handleEditAttendanceUsers(addedUsersIdsList, removedUsersIdsList)
                }
        } else {
            if (removedUsersIdsList.size > 0) {
                db.collection("Users").document(removedUsersIdsList[0])
                    .update("attendedTimes", (FieldValue.increment(-1)))
                    .addOnSuccessListener {
                        removedUsersIdsList.removeAt(0)
                        handleEditAttendanceUsers(addedUsersIdsList, removedUsersIdsList)
                    }
            } else {
                dismissFragmentMLD.value = true
            }
        }
    }

    fun getAllAttendance() {
        val attList = ArrayList<Attendance>()
        val reference = db.collection("Attendance").orderBy("date")
        reference.get().addOnSuccessListener {
            attList.addAll(it.toObjects(Attendance::class.java))
            attendanceListMLD.value = attList
        }
    }

    fun getAttendanceByDate(day: Int, month: Int, year: Int) {
        val attList = ArrayList<Attendance>()
        val reference =
            db.collection("Attendance").whereEqualTo("day", day).whereEqualTo("month", month)
                .whereEqualTo("year", year)
        reference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                attList.addAll(it.result.toObjects(Attendance::class.java))
                attendanceListMLD.value = attList
            }
        })
    }

    fun getAttendanceById(id: String) {
        val reference =
            db.collection("Attendance").document(id)
        reference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                attendanceItemMLD.value = it.result.toObject(Attendance::class.java)
            }
        })
    }

    fun deleteAttendance(attendanceId: String, date: Date, usersIds: ArrayList<String>) {
        val reference = db.collection("Users").whereLessThanOrEqualTo("creationDate", date)
        reference.get().addOnCompleteListener {
            if (it.isSuccessful) {
                for (document in it.result) {
                    val userData = document.toObject(UserData::class.java)
                    if (userData.attendanceDue >= 1) {
                        document.reference.update("attendanceDue", userData.attendanceDue - 1)
                            .addOnCompleteListener {
                                if (usersIds.contains(userData.phone)) {
                                    document.reference.update(
                                        "attendedTimes",
                                        userData.attendedTimes - 1
                                    )
                                        .addOnCompleteListener {
                                            deletedUserAttMLD.value = true
                                        }
                                }
                            }
                    }
                }
                db.collection("Attendance").document(attendanceId).delete()
                    .addOnSuccessListener {
                        deletedUserAttMLD.value = true
                    }
            }
        }
    }

    fun createReminder(reminder: Reminder) {
        val reference = db.collection("Reminders")
        val id = reference.document().id
        val map = hashMapOf(
            "title" to reminder.title,
            "desc" to reminder.desc,
            "type" to reminder.type,
            "reminderID" to id,
            "ownerID" to reminder.ownerID,
            "date" to reminder.date,
            "reminderUsers" to reminder.reminderUsers
        )
        reference.document(id).set(map).addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                dismissFragmentMLD.value = true
            }
        })
    }


    fun editReminder(reminderId: String, reminder: Reminder) {
        val reference = db.collection("Reminders").document(reminderId)
        reference.update(
            "title", reminder.title,
            "desc", reminder.desc,
            "type", reminder.type,
            "date", reminder.date,
            "reminderUsers", reminder.reminderUsers
        ).addOnCompleteListener {
            dismissFragmentMLD.value = true
        }
    }

    fun getReminders(myId: String) {
        val myRemindersList = ArrayList<Reminder>() //List for reminders i created

        val includingMeList =
            ArrayList<Reminder>() //List of reminders who others created that include me

        //Reference for reminders i created
        val myRemindersReference = db.collection("Reminders").whereEqualTo("ownerID", myId)

        //Reference for reminders who others created that include me
        val reminderIncludingMeReference =
            db.collection("Reminders").whereArrayContains("reminderUsers", myId)

        myRemindersReference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                myRemindersList.addAll(it.result.toObjects(Reminder::class.java))
                reminderIncludingMeReference.get().addOnCompleteListener { it1 ->
                    if (it1.isSuccessful) {
                        includingMeList.addAll(it1.result.toObjects(Reminder::class.java))
                        for (reminder in includingMeList) {
                            if (!myRemindersList.contains(reminder)) {
                                myRemindersList.add(reminder)
                            }
                        }
                        remindersMLD.value = myRemindersList
                    }
                }
            }
        })
    }

    fun deleteReminder(reminderId: String) {
        val reference = db.collection("Reminders").document(reminderId)
        reference.delete()
    }

    fun createGroup(
        groupName: String,
        membersList: ArrayList<String>,
        adminsList: ArrayList<String>,
        groupImage: Uri?,
        groupCover: Uri?,
        contentResolver: ContentResolver,
    ) {
        val reference = db.collection("Groups")
        val id = reference.document().id
        val map = hashMapOf(
            "name" to groupName,
            "groupID" to id,
            "members" to membersList,
            "admins" to adminsList,
            "groupImageLink" to "",
            "groupCoverImageLink" to "",
            "groupPoints" to 0
        )
        reference.document(id).set(map).addOnSuccessListener {
            uploadGroupImages(
                groupImage, groupCover,
                "GroupImages",
                "GroupCovers",
                "Groups", id, "groupImageLink",
                "groupCoverImageLink", contentResolver
            )
        }
    }

    fun editGroup(
        groupId: String,
        groupName: String,
        membersList: ArrayList<String>,
        adminsList: ArrayList<String>,
        groupImage: Uri?,
        groupCover: Uri?,
        contentResolver: ContentResolver,
    ) {
        val reference = db.collection("Groups").document(groupId)
        reference.update(
            "members", membersList,
            "admins", adminsList,
            "name", groupName
        ).addOnCompleteListener {
            uploadGroupImages(
                groupImage,
                groupCover,
                "GroupImages",
                "GroupCovers",
                "Groups",
                groupId,
                "groupImageLink",
                "groupCoverImageLink",
                contentResolver
            )
        }
    }

    fun getGroups() {
        val reference = db.collection("Groups")
        reference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                val groupsList = ArrayList(it.result.toObjects(Group::class.java))
                groupsMLD.value = groupsList
            }
        })
    }

    fun getUserGroups(userId: String) {
        val reference = db.collection("Groups")
        reference.whereArrayContains("members", userId).get()
            .addOnCompleteListener(OnCompleteListener {
                if (it.isSuccessful) {
                    val groupsList = ArrayList(it.result.toObjects(Group::class.java))
                    groupsMLD.value = groupsList
                }
            })
    }

    fun deleteGroup(groupId: String) {
        val reference = db.collection("Groups").document(groupId)
        val chatReference = db.collection("GroupChat").document(groupId)
        val storageReference = FirebaseStorage.getInstance().getReference("Groups")
        //ToDo: delete folder
        val groupChatReference =
            FirebaseStorage.getInstance().getReference("GroupChat").child(groupId)

        reference.delete().addOnCompleteListener {
            chatReference.delete().addOnCompleteListener {
                storageReference.child("$groupId Image").delete().addOnCompleteListener {
                    storageReference.child("$groupId Cover").delete().addOnCompleteListener {
                        groupChatReference.delete().addOnCompleteListener {
                            dismissFragmentMLD.value = true
                        }
                    }
                }
            }
        }
    }

    fun addGalleryItem(galleryItem: GalleryItem, uri: Uri?, contentResolver: ContentResolver) {
        val reference = db.collection("Gallery")
        val id = reference.document().id
        val galleryMap = hashMapOf(
            "itemName" to galleryItem.itemName,
            "itemsRemaining" to galleryItem.itemsRemaining,
            "price" to galleryItem.price,
            "imageLink" to galleryItem.imageLink,
            "itemId" to id,
        )
        reference.document(id).set(galleryMap).addOnCompleteListener(OnCompleteListener {
            if (uri != null) {
                uploadImage(uri, id, "Gallery", contentResolver)
            } else {
                dismissFragmentMLD.value = true
            }
        })
    }

    fun editGalleryItem(
        galleryItem: GalleryItem,
        image: Uri?,
        contentResolver: ContentResolver
    ) {
        //Create Reference
        val reference = db.collection("Gallery").document(galleryItem.itemId)
        reference.update(
            "itemName", galleryItem.itemName,
            "itemsRemaining", galleryItem.itemsRemaining,
            "price", galleryItem.price,
            "imageLink", galleryItem.imageLink
        ).addOnCompleteListener {
            //If image != null upload and update image link
            if (image != null) {
                uploadImage(image, "Gallery", galleryItem.itemId, contentResolver)
            } else {
                //Else notify that upload is done
                dismissFragmentMLD.value = true
            }
        }
    }


    fun editGalleryItemCount(itemId: String, newItemCount: Int) {
        val reference = db.collection("Gallery").document(itemId)
        reference.update("itemsRemaining", newItemCount)
    }

    fun getGalleryItems() {
        val galleryItemsList = ArrayList<GalleryItem>()
        val reference = db.collection("Gallery")
        reference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                galleryItemsList.addAll(it.result.toObjects(GalleryItem::class.java))
                galleryItemsMLD.value = galleryItemsList
            }
        })
    }

    fun deleteGalleryItem(itemId: String) {
        db.collection("Gallery").document(itemId).delete()
            .addOnCompleteListener(OnCompleteListener {
                dismissFragmentMLD.value = true
            })
    }

    fun createGalleryRequest(
        galleryRequest: GalleryRequest
    ) {
        val reference = db.collection("GalleryRequests")
        val id = reference.document().id
        val requestMap = hashMapOf(
            "itemName" to galleryRequest.itemName,
            "requestOwner" to galleryRequest.requestOwner,
            "requestId" to id,
            "requestStatus" to galleryRequest.requestStatus,
            "requestImageLink" to galleryRequest.requestImageLink
        )
        reference.document(id).set(requestMap).addOnCompleteListener {
            dismissFragmentMLD.value = true
        }
    }

    fun editGalleryRequest(requestId: String, status: String) {
        val reference = db.collection("GalleryRequests").document(requestId)
        reference.get().addOnCompleteListener {
            if (it.isSuccessful) {
                reference.update("requestStatus", status)
            }
        }
    }

    fun getGalleryRequests() {
        val galleryRequestList = ArrayList<GalleryRequest>()

        db.collection("GalleryRequests").get().addOnCompleteListener {
            if (it.isSuccessful) {
                galleryRequestList.addAll(it.result.toObjects(GalleryRequest::class.java))
                galleryRequestsMLD.value = galleryRequestList
            }
        }
    }

    fun createGalleryWishList(
        galleryWishList: GalleryWishList,
        itemImage: Uri?,
        contentResolver: ContentResolver
    ) {
        val reference = db.collection("GalleryWishList")
        val id = reference.document().id
        val itemMap = hashMapOf(
            "itemId" to galleryWishList.itemId,
            "status" to galleryWishList.status,
            "ownerId" to galleryWishList.ownerId,
            "itemName" to galleryWishList.itemName,
            "imageLink" to galleryWishList.imageLink
        )
        reference.document(id).set(itemMap).addOnCompleteListener {
            if (itemImage == null) {
                dismissFragmentMLD.value = true
            } else {
                uploadImage(itemImage, id, "GalleryWishList", contentResolver)
            }
        }
    }

    fun getGalleryWishList() {
        db.collection("GalleryWishList").get().addOnCompleteListener {
            galleryWishListMLD.value =
                ArrayList(it.result.toObjects(GalleryWishList::class.java))
        }
    }

    fun getSpiritualNote(userId: String) {
        db.collection("SpiritualNotes").whereEqualTo("ownerId", userId).get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    spiritualNoteListMLD.value =
                        ArrayList(it.result.toObjects(SpiritualNote::class.java))
                }
            }
    }

    fun createSpiritualNote(spiritualNote: SpiritualNote) {
        db.collection("SpiritualNotes").whereEqualTo("ownerId", spiritualNote.ownerId)
            .whereEqualTo("day", spiritualNote.day).whereEqualTo("month", spiritualNote.month)
            .whereEqualTo("year", spiritualNote.year).get().addOnCompleteListener {
                if (it.isSuccessful && it.result.documents.size > 0) {
                    editSpiritualNote(spiritualNote)
                } else {
                    val map = hashMapOf(
                        "firstHourPrayer" to spiritualNote.firstHourPrayer,
                        "thirdHourPrayer" to spiritualNote.thirdHourPrayer,
                        "sixthHourPrayer" to spiritualNote.sixthHourPrayer,
                        "ninthHourPrayer" to spiritualNote.ninthHourPrayer,
                        "eleventhHourPrayer" to spiritualNote.eleventhHourPrayer,
                        "twelfthHourPrayer" to spiritualNote.twelfthHourPrayer,
                        "attendingMass" to spiritualNote.attendingMass,
                        "eucharist" to spiritualNote.eucharist,
                        "confession" to spiritualNote.confession,
                        "fasting" to spiritualNote.fasting,
                        "bibleVerses" to spiritualNote.bibleVerses,
                        "date" to spiritualNote.date,
                        "day" to spiritualNote.day,
                        "month" to spiritualNote.month,
                        "year" to spiritualNote.year,
                        "id" to spiritualNote.id,
                        "ownerId" to spiritualNote.ownerId
                    )
                    db.collection("SpiritualNotes").document(spiritualNote.id).set(map)
                        .addOnCompleteListener {
                            dismissFragmentMLD.value = true
                        }
                }
            }
    }

    fun editSpiritualNote(spiritualNote: SpiritualNote) {
        val reference = db.collection("SpiritualNotes").document(spiritualNote.id)
        reference.get().addOnCompleteListener {
            if (it.isSuccessful && it.result.exists()) {
                reference.update(
                    "firstHourPrayer", spiritualNote.firstHourPrayer,
                    "thirdHourPrayer", spiritualNote.thirdHourPrayer,
                    "sixthHourPrayer", spiritualNote.sixthHourPrayer,
                    "ninthHourPrayer", spiritualNote.ninthHourPrayer,
                    "eleventhHourPrayer", spiritualNote.eleventhHourPrayer,
                    "twelfthHourPrayer", spiritualNote.twelfthHourPrayer,
                    "attendingMass", spiritualNote.attendingMass,
                    "eucharist", spiritualNote.eucharist,
                    "confession", spiritualNote.confession,
                    "fasting", spiritualNote.fasting,
                    "bibleVerses", spiritualNote.bibleVerses,
                ).addOnSuccessListener {
                    dismissFragmentMLD.value = true
                }
            }
        }
    }

    private fun uploadImage(
        uri: Uri,
        child: String,
        location: String,
        contentResolver: ContentResolver
    ) {
        val data = uriToByteArray(uri, contentResolver)

        val storageReference = FirebaseStorage.getInstance()
            .getReference(location).child(child)
        val uploadTask = storageReference.putBytes(data)
        uploadTask.addOnCompleteListener(OnCompleteListener {
            storageReference.downloadUrl
                .addOnSuccessListener {
                    val url = it.toString()
                    db.collection(location).document(child).update("imageLink", url)
                    dismissFragmentMLD.value = true
                }.addOnFailureListener(OnFailureListener {
                    dismissFragmentMLD.value = false
                })
        })
    }

    fun createSafeSpace(safeSpace: SafeSpace) {
        val reference = db.collection("ChatWithFather")

        val itemMap = hashMapOf(
            "ownerId" to safeSpace.ownerId,
            "details" to safeSpace.details,
            "itemName" to safeSpace.itemName,
            "date" to Date(),
            "hiddenUser" to safeSpace.hiddenUser,
        )

        reference.document(safeSpace.ownerId).set(itemMap).addOnCompleteListener {
            reference.document(safeSpace.ownerId).get().addOnCompleteListener {
                safeSpaceCreatedMLD.value = it.result.toObject(SafeSpace::class.java)
            }
        }
    }

    fun getAllSafeSpace() {
        val reference = db.collection("ChatWithFather")
        reference.get().addOnCompleteListener {
            safeSpaceList.value = ArrayList(it.result.toObjects(SafeSpace::class.java))
        }
    }

    fun getSafeSpaceById(userId: String) {
        db.collection("ChatWithFather").document(userId).get().addOnCompleteListener {
            if (it.isSuccessful && it.result.exists()) {
                safeSpaceItemMLD.value = it.result.toObject(SafeSpace::class.java)
            } else {
                safeSpaceItemMLD.value = null
            }
        }
    }

    fun editSafeSpaceDetails(details: String, userId: String) {
        db.collection("ChatWithFather").document(userId).update("details", details)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    getSafeSpaceById(userId)
                }
            }
    }

    private fun uploadGroupImages(
        image: Uri?,
        cover: Uri?,
        imageLocation: String,
        coverLocation: String,
        collection: String,
        child: String,
        imageField: String,
        coverField: String,
        contentResolver: ContentResolver
    ) {
        if (image != null) {
            val imageData = uriToByteArray(image, contentResolver)
            val storageReference = FirebaseStorage.getInstance()
                .getReference(imageLocation).child(child)
            val uploadTask = storageReference.putBytes(imageData)
            uploadTask.addOnSuccessListener(OnSuccessListener {
                storageReference.downloadUrl
                    .addOnSuccessListener {
                        val url = it.toString()
                        db.collection(collection).document(child).update(imageField, url)
                        imageUploadedMLD.value = true
                    }.addOnFailureListener(OnFailureListener {
                        imageUploadedMLD.value = false
                    })
            })
        } else {
            imageUploadedMLD.value = true
        }

        if (cover != null) {
            val coverData = uriToByteArray(cover, contentResolver)
            val storageReference = FirebaseStorage.getInstance()
                .getReference(coverLocation).child(child)
            val uploadTask = storageReference.putBytes(coverData)
            uploadTask.addOnSuccessListener(OnSuccessListener {
                storageReference.downloadUrl
                    .addOnSuccessListener {
                        val url = it.toString()
                        db.collection(collection).document(child)
                            .update(coverField, url)
                        coverUploadedMLD.value = true
                    }.addOnFailureListener(OnFailureListener {
                        coverUploadedMLD.value = false
                    })
            })
        } else {
            coverUploadedMLD.value = true
        }
    }

    fun uploadAlbum(
        uri: Uri?,
        location: String,
        postId: String,
        imageCode: String,
        contentResolver: ContentResolver
    ) {
        if (uri != null) {
            val data = uriToByteArray(uri, contentResolver)
            val storageReference = FirebaseStorage.getInstance()
                .getReference(location).child(postId).child(imageCode)
            val uploadTask = storageReference.putBytes(data)
            uploadTask.addOnSuccessListener(OnSuccessListener {
                storageReference.downloadUrl.addOnSuccessListener {
                    val url = it.toString()
                    pollImagesUrlMLD.value = url
                }
            })
        } else {
            pollImagesUrlMLD.value = ""
        }
    }

    private fun uriToByteArray(uri: Uri, contentResolver: ContentResolver): ByteArray {
        var bitmap: Bitmap? = null
        try {
            bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(Application().contentResolver, uri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val baos = ByteArrayOutputStream()
        bitmap!!.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        return baos.toByteArray()
    }

    private fun uploadVideo(videoUri: Uri, postId: String) {
        val reference = FirebaseStorage.getInstance()
            .getReference("VideoPosts").child(postId)

        reference.putFile(videoUri).addOnSuccessListener {
            reference.downloadUrl.addOnSuccessListener {
                db.collection("VideoPosts").document(postId).update("videoLink", it.toString())
                    .addOnSuccessListener {
                        dismissFragmentMLD.value = true
                    }
            }
        }.addOnProgressListener {
            val progressPercentage = it.bytesTransferred / it.totalByteCount * 100.0
            videoUploadPercent.value = progressPercentage
        }
    }

    //Get Device token
    fun userToken() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                val token = it.result
                if (firebaseUser != null) {
                    db.collection("Users").document(firebaseUser.phoneNumber.toString())
                        .update("token", token)
                }
            }
        })
    }
}