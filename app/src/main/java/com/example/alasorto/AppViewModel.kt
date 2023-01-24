package com.example.alasorto

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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

@Suppress("DEPRECATION")
class AppViewModel : ViewModel() {
    private val db = Firebase.firestore
    var usersMLD = MutableLiveData<ArrayList<Users>>()
    var currentUserMLD = MutableLiveData<Users?>()
    var attendanceListMLD = MutableLiveData<ArrayList<Attendance>>()
    var groupChatMLD = MutableLiveData<ArrayList<GroupChat>>()
    var totalAttNumberMLD = MutableLiveData<Int>()
    var clearFragmentMLD = MutableLiveData<Boolean>()
    var otherUserDataMLD = MutableLiveData<Users>()
    var commentsMLD = MutableLiveData<ArrayList<Comments>>()
    var postsMLD = MutableLiveData<ArrayList<Posts>>()
    var profilePostsMLD = MutableLiveData<ArrayList<Posts>>()
    var remindersMLD = MutableLiveData<ArrayList<Reminder>>()
    var groupsMLD = MutableLiveData<ArrayList<Group>>()

    fun createUser(user: Users, uri: Uri?, contentResolver: ContentResolver) {
        val reference = db.collection("Users").document(user.Phone.toString())
        reference.get().addOnCompleteListener(OnCompleteListener {
            val userMap = hashMapOf(
                "Name" to user.Name,
                "Phone" to user.Phone,
                "Address" to user.Address,
                "Location" to user.Location,
                "ConfessionPriest" to user.ConfessionPriest,
                "Points" to user.Points,
                "BirthDay" to user.BirthDay,
                "BirthMonth" to user.BirthMonth,
                "BirthYear" to user.BirthYear,
                "College" to user.College,
                "University" to user.University,
                "Status" to user.Status,
                "StatusYear" to user.StatusYear,
                "ImageLink" to "",
                "Token" to ""
            )
            if (it.isSuccessful && it.result.exists()) {
                val user = it.result.toObject(Users::class.java)
                userMap["AttendedTimes"] = user?.AttendedTimes
                userMap["AttendedPercent"] = user?.AttendedPercent
                userMap["Access"] = user?.Access
            } else {
                userMap["AttendedTimes"] = 0
                userMap["AttendedPercent"] = 0
                userMap["Access"] = "User"
            }
            reference.set(userMap)
                .addOnCompleteListener(OnCompleteListener { it1 ->
                    if (it1.isSuccessful) {
                        if (uri != null) {
                            uploadImage(uri, user.Phone.toString(), "Avatars", contentResolver)
                        } else {
                            clearFragmentMLD.value = true
                        }
                    } else {
                        clearFragmentMLD.value = false
                    }
                })
        })
    }

    fun getAllUsers() {
        val users: ArrayList<Users> = ArrayList()
        val reference = db.collection("Users")
        reference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                users.addAll(it.result.toObjects(Users::class.java))
                usersMLD.value = users
            }
        })
    }

    fun getUserById(id: String) {
        val reference = db.collection("Users").document(id)
        reference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                otherUserDataMLD.value = it.result.toObject(Users::class.java)
            }
        })
    }

    fun getCurrentUser(phoneNum: String) {
        val reference = db.collection("Users").document(phoneNum)
        reference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                if (it.result.exists()) {
                    currentUserMLD.value = it.result.toObject(Users::class.java)
                } else {
                    currentUserMLD.value = null
                }
            } else {
                //currentUserMLD.value = null
            }
        })
    }

    fun editUser(user: Users, uri: Uri?, contentResolver: ContentResolver) {
        val reference = db.collection("Users").document(user.Phone.toString())
        reference.update("Name", user.Name)
        reference.update("Phone", user.Phone)
        reference.update("Address", user.Address)
        reference.update("Location", user.Location)
        reference.update("ConfessionPriest", user.ConfessionPriest)
        reference.update("Points", user.Points)
        reference.update("BirthDay", user.BirthDay)
        reference.update("BirthMonth", user.BirthMonth)
        reference.update("BirthYear", user.BirthYear)
        reference.update("College", user.College)
        reference.update("University", user.University)
        reference.update("Status", user.Status)
        reference.update("StatusYear", user.StatusYear)
        //If it has image link and no Uri file post old image link (no change)
        if (user.ImageLink.toString().isNotEmpty()) {
            reference.update("ImageLink", user.ImageLink)
            clearFragmentMLD.value = true
        } else {
            //If it has empty image link and has Uri file create image link (no change)
            if (uri != null) {
                uploadImage(uri, user.Phone.toString(), "Avatars", contentResolver)
            } else {
                reference.update("ImageLink", "")
                clearFragmentMLD.value = true
            }
        }
    }

    fun addUserPoints(pointsNum: Int, userId: String) {
        val reference = db.collection("Users").document(userId)
        reference.update("Points", pointsNum).addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                clearFragmentMLD.value = true
            }
        })
    }

    fun deleteUser(id: String) {
        db.collection("Users").document(id).delete()
        val storageReference = FirebaseStorage.getInstance()
            .getReference("Avatars").child(id)
        storageReference.delete()
    }

    fun createPost(
        title: String,
        desc: String,
        ownerId: String,
        day: Int,
        month: Int,
        year: Int,
        image: Uri?,
        contentResolver: ContentResolver
    ) {
        val id = db.collection("Posts").document().id
        val postMap = hashMapOf(
            "Title" to title,
            "Description" to desc,
            "Day" to day,
            "Month" to month,
            "Year" to year,
            "ID" to id,
            "OwnerID" to ownerId,
            "PostDate" to Date(),
            "ImageLink" to ""
        )
        db.collection("Posts").document(id).set(postMap)
            .addOnCompleteListener(OnCompleteListener {
                if (it.isSuccessful) {
                    if (image != null) {
                        uploadImage(image, id, "Posts", contentResolver)
                    } else {
                        clearFragmentMLD.value = true
                    }
                } else {
                    clearFragmentMLD.value = false
                }
            })
    }

    fun editPost(
        title: String,
        desc: String,
        id: String,
        imageLink: String,
        day: Int,
        month: Int,
        year: Int,
        image: Uri?,
        contentResolver: ContentResolver
    ) {
        val reference = db.collection("Posts").document(id)
        reference.update("Title", title)
        reference.update("Description", desc)
        reference.update("Day", day)
        reference.update("Month", month)
        reference.update("Year", year)
        reference.update("ImageLink", imageLink)
            .addOnCompleteListener(OnCompleteListener {
                if (it.isSuccessful) {
                    if (image != null) {
                        uploadImage(image, id, "Posts", contentResolver)
                    } else {
                        clearFragmentMLD.value = true
                    }
                } else {
                    //clearFragmentMLD.value = false
                }
            })
    }

    fun getPosts() {
        val posts: ArrayList<Posts> = ArrayList()
        val reference = db.collection("Posts").orderBy("PostDate")
        reference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                posts.addAll(it.result.toObjects(Posts::class.java))
                postsMLD.value = posts
            }
        })
    }

    fun getProfilePost(userId: String) {
        val posts: ArrayList<Posts> = ArrayList()
        val reference = db.collection("Posts").whereEqualTo("OwnerID", userId)
        reference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                posts.addAll(it.result.toObjects(Posts::class.java))
                profilePostsMLD.value = posts
            }
        })
    }

    fun deletePost(postId: String) {
        //Remove post
        val postReference = db.collection("Posts").document(postId)
        postReference.delete().addOnCompleteListener(OnCompleteListener {
            getPosts()
        })
        //Remove Comments
        val commentReference = db.collection("Comments").document(postId)
        commentReference.delete().addOnCompleteListener(OnCompleteListener {
            getPosts()
        })
        //Remove post image
        val storageReference = FirebaseStorage.getInstance()
            .getReference("Posts").child(postId)
        storageReference.delete()
    }

    fun createAttendance(usersIds: ArrayList<String>) {
        //Create new Attendance data on Firebase
        val reference = db.collection("Attendance")
        //Get current Date
        val date = Date()
        val attMap = hashMapOf("Date" to date, "UsersIDs" to usersIds)
        reference.add(attMap).addOnSuccessListener(OnSuccessListener {
            reference.get().addOnCompleteListener(OnCompleteListener {
                /*Gets Total Number for Attendance Records and trigger observer in fragment to
                update each user att % */
                val total = it.result.count()
                totalAttNumberMLD.value = total
                //For loop for each user to add 1 to attendance time and calculate percentage
            })
        })
    }

    fun handleUserAtt(
        attendedUsersIds: ArrayList<String>,
        allUsersIds: ArrayList<String>,
        total: Int
    ) {
        val userReference = db.collection("Users").document(allUsersIds[0])
        userReference.get()
            .addOnCompleteListener(OnCompleteListener {
                if (it.isSuccessful) {
                    //Gets User data
                    val user = it.result.toObject(Users::class.java)
                    if (attendedUsersIds.contains(allUsersIds[0])) {
                        Log.d("TESTING_ATT", "createAttendance: 1")
                        //Attended time for each user
                        val attendedTimes = user?.AttendedTimes!! + 1
                        //Attendance Percentage for each user
                        val attPercent = attendedTimes.toFloat() / total * 100
                        //Add Points for Attended users
                        val points = user.Points!!.toInt() + 5
                        //Update Attendance Percent
                        userReference.update(
                            "AttendedPercent", attPercent
                        )
                        //Update Attendance Times
                        userReference.update(
                            "AttendedTimes", attendedTimes
                        )
                        //Update Points
                        userReference.update(
                            "Points", points
                        )
                    } else {
                        Log.d("TESTING_ATT", "createAttendance: 2")
                        //Attended time for each user
                        val attendedTimes = user?.AttendedTimes!!
                        //Attendance Percentage for each user
                        val attPercent = attendedTimes.toFloat() / total * 100
                        //Update Attendance Percent
                        userReference.update(
                            "AttendedPercent", attPercent
                        )
                    }
                    allUsersIds.removeAt(0)
                    if (allUsersIds.size >= 1) {
                        handleUserAtt(attendedUsersIds, allUsersIds, total)
                    } else {
                        clearFragmentMLD.value = true
                    }
                }
            })
    }

    fun getAllAttendance() {
        val attList = ArrayList<Attendance>()
        val reference = db.collection("Attendance").orderBy("Date")
        reference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                attList.addAll(it.result.toObjects(Attendance::class.java))
                attendanceListMLD.value = attList
            }
        })
    }

    fun createPostComment(
        postId: String,
        comment: String,
        commentOwnerId: String,
    ) {
        val reference = db.collection("Comments")
        val commentId = reference.document().id
        val date = Date()
        val commentMap = hashMapOf(
            "PostID" to postId,
            "Comment" to comment,
            "OwnerID" to commentOwnerId,
            "CommentID" to commentId,
            "Date" to date
        )
        reference.document(commentId).set(commentMap).addOnCompleteListener(OnCompleteListener {
        })
    }

    fun getPostComments(postId: String) {
        val reference = db.collection("Comments").whereEqualTo("PostID", postId)
        reference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                val users = ArrayList<Comments>()
                users.addAll(it.result.toObjects(Comments::class.java))
                commentsMLD.value = users
            }
        })
    }

    fun deletePostComment(commentID: String) {
        val reference = db.collection("Comments").document(commentID)
        reference.delete().addOnCompleteListener(OnCompleteListener {
        })
    }

    fun editComment(commentID: String, newComment: String) {
        val reference = db.collection("Comments").document(commentID)
        reference.update("Comment", newComment)
    }

    fun sendGroupChatMessage(
        messageOwnerId: String,
        message: String,
        messageType: String,
        contentResolver: ContentResolver,
        uri: Uri?
    ) {
        val reference = db.collection("GroupChat")
        val messageID = reference.document().id
        val messageMap = hashMapOf(
            "Message" to message,
            "OwnerID" to messageOwnerId,
            "MessageID" to messageID,
            "ImageLink" to "",
            "MessageType" to messageType,
            "Date" to Date()
        )
        reference.document(messageID).set(messageMap)

        if (uri != null) {
            uploadImage(uri, messageID, "GroupChat", contentResolver)
        } else {
            clearFragmentMLD.value = true
        }
    }

    fun getGroupChat() {
        val chatList = ArrayList<GroupChat>()
        val reference = db.collection("GroupChat").orderBy("Date")
        reference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                chatList.addAll(it.result.toObjects(GroupChat::class.java))
                groupChatMLD.value = chatList
            }
        })
    }

    fun createReminder(reminder: Reminder) {
        val reference = db.collection("Reminders")
        val id = reference.document().id
        val map = hashMapOf(
            "Title" to reminder.Title,
            "Desc" to reminder.Desc,
            "Type" to reminder.Type,
            "ReminderID" to id,
            "OwnerID" to reminder.OwnerID,
            "Day" to reminder.Day,
            "Month" to reminder.Month,
            "Year" to reminder.Year
        )
        reference.document(id).set(map).addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                clearFragmentMLD.value = true
            }
        })
    }

    fun getReminders(myId: String) {
        val remindersList = ArrayList<Reminder>()
        val reference = db.collection("Reminders").whereEqualTo("OwnerID", myId)
        reference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                remindersList.addAll(it.result.toObjects(Reminder::class.java))
                remindersMLD.value = remindersList
            }
        })
    }

    fun deleteReminder(reminderId: String) {
        val reference = db.collection("Reminders").document(reminderId)
        reference.delete()
    }

    fun createGroup(groupName: String, membersList: ArrayList<String>) {
        val reference = db.collection("Groups")
        val id = reference.document().id
        val map = hashMapOf("Name" to groupName, "GroupID" to id, "Members" to membersList)
        reference.document(id).set(map).addOnSuccessListener {
            clearFragmentMLD.value = true
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

    fun deleteGroup(groupId: String) {
        val reference = db.collection("Groups").document(groupId)
        reference.delete()
    }

    private fun uploadImage(
        uri: Uri,
        child: String,
        location: String,
        contentResolver: ContentResolver
    ) {
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
        val data = baos.toByteArray()
        val storageReference = FirebaseStorage.getInstance()
            .getReference(location).child(child)
        val uploadTask = storageReference.putBytes(data)
        uploadTask.addOnSuccessListener(OnSuccessListener { getImageUrl(child, location) })
    }

    //GET STORAGE URL for user images///////////////////////////////////////////////////////////////
    private fun getImageUrl(child: String, location: String) {
        FirebaseStorage.getInstance()
            .getReference(location).child(child).downloadUrl
            .addOnSuccessListener {
                val url = it.toString()
                if (location == "Avatars") {
                    db.collection("Users").document(child).update("ImageLink", url)
                } else if (location == "Posts") {
                    db.collection("Posts").document(child).update("ImageLink", url)
                } else if (location == "GroupChat") {
                    db.collection("GroupChat").document(child).update("ImageLink", url)
                }
                clearFragmentMLD.value = true
            }.addOnFailureListener(OnFailureListener {
                clearFragmentMLD.value = false
            })
    }

    //Get Device token
    fun userToken() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                val token = it.result
                if (firebaseUser != null) {
                    db.collection("Users").document(firebaseUser.phoneNumber.toString())
                        .update("Token", token)
                    Log.d("GET_TOKEN", token)
                }
            }
        })
    }
}