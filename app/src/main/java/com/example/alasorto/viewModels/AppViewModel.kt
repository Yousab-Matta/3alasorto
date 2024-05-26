package com.example.alasorto.viewModels

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.alasorto.dataClass.*
import com.example.alasorto.pendingAttendanceDatabase.PendingAttendance
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Suppress("DEPRECATION")
class AppViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val allUsersList = ArrayList<UserData>()

    var usersMLD = MutableLiveData<ArrayList<UserData>>()
    var pendingVerifyUsersMLD = MutableLiveData<ArrayList<UserData>>()
    var currentUserMLD = MutableLiveData<UserData?>()
    var attendanceListMLD = MutableLiveData<ArrayList<Attendance>>()
    var attendanceItemMLD = MutableLiveData<Attendance>()
    var dismissFragmentMLD = MutableLiveData<Boolean>()
    var safeSpaceCreatedMLD = MutableLiveData<SafeSpace>()
    var userByIdMLD = MutableLiveData<UserData?>()
    var deletedUserIdMLD = MutableLiveData<String>()
    var remindersMLD = MutableLiveData<ArrayList<Reminder>>()
    var groupsListMLD = MutableLiveData<ArrayList<Group>>()
    var groupByIdMLD = MutableLiveData<Group>()
    var pollImagesUrlMLD = MutableLiveData<String>()
    var galleryItemsMLD = MutableLiveData<ArrayList<GalleryItem>>()
    var galleryRequestsMLD = MutableLiveData<ArrayList<GalleryRequest>>()
    var galleryWishListMLD = MutableLiveData<ArrayList<GalleryWishList>>()
    var videoUploadPercent = MutableLiveData<Double>()
    var imageUploadedMLD = MutableLiveData<Boolean>()
    var coverUploadedMLD = MutableLiveData<Boolean>()
    var safeSpaceList = MutableLiveData<ArrayList<SafeSpace>>()
    var safeSpaceItemMLD = MutableLiveData<SafeSpace?>()
    var issuesListMLD = MutableLiveData<ArrayList<IssueData>>()
    var spiritualNoteListMLD = MutableLiveData<ArrayList<SpiritualNote>>()
    var inAppNotificationsMLD = MutableLiveData<ArrayList<InAppNotificationData>>()
    var groupWithActivity = MutableLiveData<String>()
    var finishedAttendanceMLD = MutableLiveData<HashMap<String, String>>()

    var removedAttendanceUser = MutableLiveData<String>()

    private var currentUserId: String = ""
    private var currentUserData: UserData? = null

    //Used to get the number of success events in case of repeating a function in a loop
    var counterMLD = MutableLiveData<Boolean>()

    fun updateUsersList(userData: UserData) {
        if (allUsersList.any { it.userId == userData.userId }) {
            allUsersList.removeAll { it.userId == userData.userId }
        }
        allUsersList.add(userData)
    }

    fun readAllUsers(): ArrayList<UserData> = allUsersList

    fun getCurrentUserId(): String = currentUserId

    fun getCurrentUserData(): UserData? = currentUserData

    fun createUser(user: UserData) {
        val reference = db.collection("Users").document(user.phone)
        val userMap = hashMapOf(
            "name" to user.name,
            "access" to user.access,
            "bio" to user.bio,
            "phone" to user.phone,
            "userId" to user.userId,
            "attendedTimes" to ArrayList<String>(),
            "attendanceDue" to ArrayList<String>(),
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
        val userPhone = Firebase.auth.currentUser!!.phoneNumber!!
        db.collection("Users").document(userPhone).update("visibilityStatus", "Online")
            .addOnCompleteListener {
                getCurrentUser()
            }
    }

    fun setOffline() {
        val userPhone = Firebase.auth.currentUser!!.phoneNumber!!
        db.collection("Users").document(userPhone).update("visibilityStatus", "Offline")
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
        val currentUserPhone = Firebase.auth.currentUser!!.phoneNumber!!

        uploadGroupImages(
            image,
            cover,
            "UserImage",
            "UserCover",
            "Users",
            currentUserPhone,
            userId,
            "imageLink",
            "coverImageLink",
            contentResolver,
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
        db.collection("Users").whereEqualTo("userId", id).get()
            .addOnSuccessListener {
                if (it.size() > 0) {
                    userByIdMLD.value = it.toObjects(UserData::class.java)[0]
                }
            }.addOnFailureListener { userByIdMLD.value = null }
    }

    fun getUserByPhone(phoneNum: String) {
        db.collection("Users").document(phoneNum).get()
            .addOnSuccessListener {
                userByIdMLD.value = it.toObject(UserData::class.java)
            }.addOnFailureListener { userByIdMLD.value = null }
    }

    fun getCurrentUser() {
        if (Firebase.auth.currentUser != null) {
            val userPhone = Firebase.auth.currentUser!!.phoneNumber!!
            val reference = db.collection("Users").document(userPhone)
            reference.get().addOnCompleteListener(OnCompleteListener {
                if (it.isSuccessful) {
                    if (it.result.exists()) {
                        currentUserData = it.result.toObject(UserData::class.java)
                        currentUserMLD.value = currentUserData
                        currentUserId = currentUserData!!.userId
                    } else {
                        currentUserMLD.value = null
                    }
                }
            })
        }
    }

    fun editUserPoints(pointsNum: Long, userId: String) {
        db.collection("Users").document(userId)
            .update("points", FieldValue.increment(pointsNum)).addOnSuccessListener {
                counterMLD.value = true
            }
    }

    fun updateUserBio(bio: String) {
        val currentUserPhone = Firebase.auth.currentUser!!.phoneNumber!!
        val reference = db.collection("Users").document(currentUserPhone)
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
        deleteUserInAppNotification(id)
    }

    private fun deleteUserInAppNotification(id: String) {
        val reference = db.collection("Users").document(id).collection("Notifications")

        reference.get().addOnSuccessListener {
            if (!it.isEmpty) {
                reference.document(it.first().id).delete().addOnCompleteListener {
                    deleteUserInAppNotification(id)
                }
            } else {
                deleteSpiritualNotes(id)
            }
        }
    }

    private fun deleteSpiritualNotes(id: String) {
        val reference = db.collection("Users").document(id).collection("SpiritualNotes")

        reference.get().addOnSuccessListener { it1 ->
            if (!it1.isEmpty) {
                reference.document(it1.first().id).delete().addOnCompleteListener {
                    deleteSpiritualNotes(id)
                }
            } else {
                db.collection("Users").document(id).get().addOnSuccessListener { it2 ->
                    val userData = it2.toObject(UserData::class.java)
                    val userId = userData!!.userId

                    val coverImageReference = FirebaseStorage.getInstance()
                        .getReference("UserCover").child(userId)

                    val userImageReference = FirebaseStorage.getInstance()
                        .getReference("UserImage").child(userId)

                    coverImageReference.delete().addOnCompleteListener {
                        userImageReference.delete().addOnCompleteListener {
                            db.collection("Users").document(id).delete()
                                .addOnSuccessListener {
                                    counterMLD.value = true
                                    deletedUserIdMLD.value = userId
                                }
                        }
                    }
                }
            }
        }
    }

    fun getPendingVerifyUsers() {
        db.collection("Users").whereEqualTo("verified", false)
            .get().addOnCompleteListener {
                pendingVerifyUsersMLD.value = ArrayList(it.result.toObjects(UserData::class.java))
            }
    }

    fun createInAppNotification(notification: InAppNotificationData, receiverPhone: String) {
        val reference =
            db.collection("Users").document(receiverPhone).collection("Notifications")

        val id = reference.document().id
        val dataMap = hashMapOf(
            "senderId" to notification.senderId,
            "receiverId" to notification.receiverId,
            "text" to notification.text,
            "notificationId" to id,
            "date" to notification.date,
            "notificationData" to notification.notificationData,
            "read" to notification.read
        )
        reference.document(id).set(dataMap)
    }

    fun getInAppNotification(currentUserId: String) {
        db.collection("Users").document(currentUserId).collection("Notifications").get()
            .addOnSuccessListener {
                inAppNotificationsMLD.value =
                    ArrayList(it.toObjects(InAppNotificationData::class.java))
            }
    }

    fun markNotificationAsRead(notificationId: String) {
        db.collection("Users").document(currentUserId).collection("Notifications")
            .document(notificationId).update("read", true)
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

    fun createAttendanceData(pendingAttendance: PendingAttendance) {
        val attendance = pendingAttendance.attendance

        //Create new Attendance data on Firebase
        val reference = db.collection("Attendance")
        //Get current Date

        val attMap = hashMapOf(
            "day" to attendance.day,
            "month" to attendance.month,
            "year" to attendance.year,
            "id" to attendance.id,
            "date" to attendance.date,
            "attendancePercentage" to attendance.attendancePercentage,
            "usersIDs" to attendance.usersIDs
        )
        reference.document(attendance.id).set(attMap).addOnSuccessListener {
            finishedAttendanceMLD.value =
                hashMapOf(pendingAttendance.databaseId to "NEW_ATT")
        }
    }

    fun updateAttendanceData(pendingAttendance: PendingAttendance) {
        val attendance = pendingAttendance.attendance
        val reference = db.collection("Attendance").document(attendance.id)
        reference.update(
            "usersIDs",
            attendance.usersIDs,
            "attendancePercentage",
            attendance.attendancePercentage
        ).addOnSuccessListener {
            finishedAttendanceMLD.value = hashMapOf(pendingAttendance.databaseId to "EDIT_ATT")
        }
    }

    fun editUserFields() {
        db.collection("Users").get().addOnSuccessListener {
            for (doc in it.documents) {
                db.collection("Users").document(doc.id).update(
                    "attendanceDue",
                    FieldValue.delete(),
                    "attendedTimes",
                    FieldValue.delete()
                )
            }
        }
    }

    fun addUserAttendanceData(userId: String, databaseId: String, addAttendedTimes: Boolean) {

        db.collection("Users").whereEqualTo("userId", userId).get()
            .addOnSuccessListener {
                if (it.size() > 0) {
                    val userPhone =
                        it.first().toObject(UserData::class.java).phone
                    val userReference =
                        db.collection("Users").document(userPhone)

                    val operation = if (addAttendedTimes) {
                        userReference.update(
                            "attendanceDue",
                            (FieldValue.arrayUnion(databaseId)),
                            "attendedTimes",
                            (FieldValue.arrayUnion(databaseId))
                        )
                    } else {
                        userReference.update(
                            "attendanceDue", (FieldValue.arrayUnion(databaseId)),
                            "attendedTimes",
                            (FieldValue.arrayRemove(databaseId))
                        )
                    }

                    operation.addOnSuccessListener {
                        removedAttendanceUser.value = userId
                    }

                }
            }
    }

    fun removeUserAttendanceData(userId: String, databaseId: String) {

        db.collection("Users").whereEqualTo("userId", userId).get()
            .addOnSuccessListener {
                if (it.size() > 0) {
                    val userPhone =
                        it.first().toObject(UserData::class.java).phone
                    val userReference =
                        db.collection("Users").document(userPhone)

                    userReference.update(
                        "attendanceDue",
                        (FieldValue.arrayRemove(databaseId)),
                        "attendedTimes",
                        (FieldValue.arrayRemove(databaseId))
                    ).addOnSuccessListener {
                        removedAttendanceUser.value = userId
                    }

                }
            }
    }

    fun editUserAttendanceData(userId: String, databaseId: String, addAtt: Boolean) {
        db.collection("Users").whereEqualTo("userId", userId).get()
            .addOnSuccessListener {
                if (it.size() > 0) {
                    val userPhone =
                        it.first().toObject(UserData::class.java).phone
                    val userReference =
                        db.collection("Users").document(userPhone)

                    val operation = if (addAtt) {
                        userReference.update(
                            "attendanceDue",
                            (FieldValue.arrayUnion(databaseId)),
                            "attendedTimes",
                            (FieldValue.arrayUnion(databaseId))
                        )
                    } else {
                        userReference.update(
                            "attendanceDue",
                            (FieldValue.arrayUnion(databaseId)),
                            "attendedTimes",
                            (FieldValue.arrayRemove(databaseId))
                        )
                    }

                    operation.addOnSuccessListener {
                        removedAttendanceUser.value = userId
                    }
                }
            }
    }

    fun resetAtt(allUsersList: ArrayList<String>) {
        for (user in allUsersList) {
            val userReference = db.collection("Users").document(user)
            userReference.update(
                "attendanceDue",
                ArrayList<String>(),
                "attendedTimes",
                ArrayList<String>()
            )
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
            db.collection("Attendance").whereEqualTo("day", day)
                .whereEqualTo("month", month)
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

    fun deleteAttendanceData(pendingAttendance: PendingAttendance) {
        db.collection("Attendance").document(pendingAttendance.attendance.id).delete()
            .addOnSuccessListener {
                finishedAttendanceMLD.value =
                    hashMapOf(pendingAttendance.databaseId to "DELETE_ATT")
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
            "groupId" to id,
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
                "Groups", id, "", "groupImageLink",
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
                "",
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
                groupsListMLD.value = groupsList
            }
        })
    }

    fun getGroupById(groupId: String) {
        val reference = db.collection("Groups").document(groupId)
        reference.get().addOnSuccessListener {
            groupByIdMLD.value = it.toObject(Group::class.java)
        }
    }

    fun getUserGroups(userId: String) {
        val reference = db.collection("Groups")
        reference.whereArrayContains("members", userId).get()
            .addOnCompleteListener(OnCompleteListener {
                if (it.isSuccessful) {
                    val groupsList = ArrayList(it.result.toObjects(Group::class.java))
                    groupsListMLD.value = groupsList
                }
            })
    }

    fun getAdminGroups(userId: String) {
        val reference = db.collection("Groups")
        reference.whereArrayContains("admins", userId).get()
            .addOnCompleteListener(OnCompleteListener {
                if (it.isSuccessful) {
                    val groupsList = ArrayList(it.result.toObjects(Group::class.java))
                    groupsListMLD.value = groupsList
                }
            })
    }

    fun deleteGroup(groupId: String) {
        deleteChatMedia(groupId)
    }

    private fun deleteChatMedia(groupId: String) {
        FirebaseStorage.getInstance().getReference("Chat").child(groupId).listAll()
            .addOnCompleteListener {
                if (it.result.prefixes.isNotEmpty()) {
                    deleteChatMessageFolder(it.result.prefixes.first(), 0, groupId)
                } else {
                    val groupReference = db.collection("Groups").document(groupId)

                    val groupCoverReference =
                        FirebaseStorage.getInstance().getReference("GroupCovers").child(groupId)

                    val groupImageReference =
                        FirebaseStorage.getInstance().getReference("GroupImages").child(groupId)

                    groupCoverReference.delete().addOnCompleteListener {
                        groupImageReference.delete().addOnCompleteListener {
                            deleteChatDocuments(groupId)
                        }
                    }
                }
            }
    }

    private fun deleteChatDocuments(groupId: String) {
        val groupReference = db.collection("Groups").document(groupId).collection("Chats")
        groupReference.get()
            .addOnSuccessListener {
                if (!it.isEmpty) {
                    groupReference.document(it.documents.first().id).delete()
                        .addOnSuccessListener {
                            deleteChatDocuments(groupId)
                        }
                } else {
                    db.collection("Groups").document(groupId).delete().addOnSuccessListener {
                        dismissFragmentMLD.value = true
                    }
                }
            }
    }

    private fun deleteChatMessageFolder(
        storageReference: StorageReference,
        index: Int,
        groupId: String
    ) {
        storageReference.child(index.toString()).delete().addOnSuccessListener {
            deleteChatMessageFolder(storageReference, index + 1, groupId)
        }.addOnFailureListener {
            deleteChatMedia(groupId)
        }
    }

    fun addGalleryItem(
        galleryItem: GalleryItem,
        uri: Uri?,
        contentResolver: ContentResolver
    ) {
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

        FirebaseStorage.getInstance().getReference("Gallery").child(itemId).delete()
            .addOnCompleteListener {
                db.collection("Gallery").document(itemId).delete()
                    .addOnCompleteListener {
                        dismissFragmentMLD.value = true
                    }
            }
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
            "price" to galleryRequest.price,
            "requestStatus" to galleryRequest.requestStatus,
            "requestImageLink" to galleryRequest.requestImageLink
        )
        reference.document(id).set(requestMap).addOnCompleteListener {
            dismissFragmentMLD.value = true
        }
    }

    fun editGalleryRequest(requestId: String, status: String) {
        val reference = db.collection("GalleryRequests").document(requestId)
        reference.get().addOnSuccessListener {
            reference.update("requestStatus", status)
        }
    }

    fun deleteGalleryRequest(galleryRequest: GalleryRequest) {

        FirebaseStorage.getInstance().getReference("GalleryRequests")
            .child(galleryRequest.requestId).delete()
            .addOnCompleteListener {
                db.collection("GalleryRequests").document(galleryRequest.requestId).delete()
                    .addOnCompleteListener {
                        if (galleryRequest.requestStatus != "Delivered") {
                            editUserPoints(
                                galleryRequest.price.toLong(),
                                galleryRequest.requestOwner
                            )
                        }
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

    fun deleteGalleryWishList(itemId: String) {
        val itemImageReference =
            FirebaseStorage.getInstance().getReference("GalleryWishList").child(itemId)

        db.collection("GalleryWishList").get().addOnSuccessListener {
            itemImageReference.delete().addOnSuccessListener {
                dismissFragmentMLD.value = true
            }
        }
    }

    fun checkForGroupNewActivity(collectionPath: String, groupId: String, userId: String) {
        db.collection(collectionPath).document(groupId)
            .collection("Chats").orderBy("date", Query.Direction.DESCENDING).limit(1)
            .get().addOnSuccessListener {
                if (it.size() > 0) {
                    val lastMessage = it.last().toObject(Message::class.java)
                    if (lastMessage.ownerId != userId && !lastMessage.seenBy.any { a -> a.userId == userId }) {
                        groupWithActivity.value = groupId
                    }
                }
            }
    }

    fun getSpiritualNote() {
        db.collection("Users")
            .document(FirebaseAuth.getInstance().currentUser!!.phoneNumber!!)
            .collection("SpiritualNotes").get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    spiritualNoteListMLD.value =
                        ArrayList(it.result.toObjects(SpiritualNote::class.java))
                }
            }
    }

    fun createSpiritualNote(spiritualNote: SpiritualNote) {
        db.collection("Users")
            .document(FirebaseAuth.getInstance().currentUser!!.phoneNumber!!)
            .collection("SpiritualNotes").whereEqualTo("ownerId", spiritualNote.ownerId)
            .whereEqualTo("day", spiritualNote.day)
            .whereEqualTo("month", spiritualNote.month)
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
                    db.collection("Users")
                        .document(FirebaseAuth.getInstance().currentUser!!.phoneNumber!!)
                        .collection("SpiritualNotes").document(spiritualNote.id).set(map)
                        .addOnCompleteListener {
                            dismissFragmentMLD.value = true
                        }
                }
            }
    }

    fun editSpiritualNote(spiritualNote: SpiritualNote) {
        val reference = db.collection("Users")
            .document(FirebaseAuth.getInstance().currentUser!!.phoneNumber!!)
            .collection("SpiritualNotes").document(spiritualNote.id)
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
            "itemName" to safeSpace.itemId,
            "date" to safeSpace.date
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

    private fun uploadGroupImages(
        image: Uri?,
        cover: Uri?,
        imageLocation: String,
        coverLocation: String,
        collection: String,
        documentId: String,
        userId: String,
        imageField: String,
        coverField: String,
        contentResolver: ContentResolver
    ) {

        if (image != null) {
            val imageData = uriToByteArray(image, contentResolver)
            val storageReference = FirebaseStorage.getInstance()
                .getReference(imageLocation).child(userId)
            val uploadTask = storageReference.putBytes(imageData)
            uploadTask.addOnSuccessListener(OnSuccessListener {
                storageReference.downloadUrl
                    .addOnSuccessListener {
                        val url = it.toString()
                        db.collection(collection).document(documentId).update(imageField, url)
                            .addOnSuccessListener {
                                imageUploadedMLD.value = true
                            }
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
                .getReference(coverLocation).child(documentId)
            val uploadTask = storageReference.putBytes(coverData)
            uploadTask.addOnSuccessListener(OnSuccessListener {
                storageReference.downloadUrl
                    .addOnSuccessListener {
                        val url = it.toString()
                        db.collection(collection).document(documentId)
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
                db.collection("VideoPosts").document(postId)
                    .update("videoLink", it.toString())
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
        val userPhone = FirebaseAuth.getInstance().currentUser!!.phoneNumber!!
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                val token = it.result
                if (userPhone != null) {
                    db.collection("Users").document(userPhone)
                        .update("token", token)
                }
            }
        })
    }
}