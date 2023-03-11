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
import com.google.firebase.firestore.ktx.toObject
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
    var videoPostsMLD = MutableLiveData<ArrayList<VideoPost>>()
    var pollsMLD = MutableLiveData<ArrayList<PollPost>>()
    var profilePostsMLD = MutableLiveData<ArrayList<Posts>>()
    var remindersMLD = MutableLiveData<ArrayList<Reminder>>()
    var groupsMLD = MutableLiveData<ArrayList<Group>>()
    var pollImagesUrlMLD = MutableLiveData<String>()
    var galleryItemsMLD = MutableLiveData<ArrayList<GalleryItem>>()
    var galleryRequestsMLD = MutableLiveData<ArrayList<GalleryRequest>>()
    var reactMLD = MutableLiveData<ArrayList<PostReact>>()
    var videoUploadPercent = MutableLiveData<Double>()

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
                val retrievedUser = it.result.toObject(Users::class.java)
                userMap["AttendedTimes"] = retrievedUser?.AttendedTimes
                userMap["AttendedPercent"] = retrievedUser?.AttendedPercent
                userMap["Access"] = retrievedUser?.Access
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
        reference.update(
            "Name",
            user.Name,
            "Phone", user.Phone,
            "Address", user.Address,
            "Location", user.Location,
            "ConfessionPriest", user.ConfessionPriest,
            "Points", user.Points,
            "BirthDay", user.BirthDay,
            "BirthMonth", user.BirthMonth,
            "BirthYear", user.BirthYear,
            "College", user.College,
            "University", user.University,
            "Status", user.Status,
            "StatusYear", user.StatusYear
        ).addOnCompleteListener(OnCompleteListener {
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
        })
    }

    fun editUserPoints(pointsNum: Int, userId: String) {
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

    //For Text and image post
    fun createPost(post: Posts, image: Uri?, contentResolver: ContentResolver) {
        val id = db.collection("Posts").document().id
        val postMap = hashMapOf(
            "title" to post.title,
            "description" to post.description,
            "id" to id,
            "ownerID" to post.ownerID,
            "postDate" to Date(),
            "day" to post.day,
            "month" to post.month,
            "year" to post.year,
            "imageLink" to ""
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

    fun createVideoPost(post: VideoPost, video: Uri) {
        val id = db.collection("VideoPosts").document().id
        val postMap = hashMapOf(
            "title" to post.title,
            "description" to post.description,
            "id" to id,
            "ownerID" to post.ownerID,
            "postDate" to Date(),
            "videoLink" to "",
            "thumbLink" to "",
            "videoHeight" to post.videoHeight,
        )

        db.collection("VideoPosts").document(id).set(postMap)
            .addOnSuccessListener {
                uploadVideo(video, id)
            }.addOnFailureListener(OnFailureListener {
                Log.d("VIDEO_POST", it.toString())
            })
    }

    fun editPost(
        post: Posts,
        image: Uri?,
        contentResolver: ContentResolver
    ) {
        val reference = db.collection("Posts").document(post.id!!)
        reference.update(
            "title", post.title,
            "description", post.description,
            "day", post.day,
            "month", post.month,
            "year", post.year,
            "imageLink", post.imageLink
        ).addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                if (image != null) {
                    uploadImage(image, post.id, "Posts", contentResolver)
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
        db.collection("Posts").orderBy("postDate").get()
            .addOnCompleteListener(OnCompleteListener {
                if (it.isSuccessful) {
                    posts.addAll(it.result.toObjects(Posts::class.java))
                    postsMLD.value = posts
                }
            })
    }

    fun getVideoPost() {
        val posts: ArrayList<VideoPost> = ArrayList()
        db.collection("VideoPosts").orderBy("postDate").get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    posts.addAll(it.result.toObjects(VideoPost::class.java))
                    videoPostsMLD.value = posts
                }
            }
    }

    fun deletePost(postId: String) {
        //Remove post
        val postReference = db.collection("Posts").document(postId)
        postReference.delete().addOnCompleteListener(OnCompleteListener {
            clearFragmentMLD.value = true
        })
        //Remove Comments
        val commentReference = db.collection("Comments").whereEqualTo("postID", postId)
        commentReference.get().addOnCompleteListener {
            if (it.isSuccessful && it.result.documents.size > 0) {
                for (comment in it.result.documents) {
                    comment.reference.delete()
                }
            }
        }
        //Remove reacts
        val reactsReference = db.collection("PostReacts").whereEqualTo("postId", postId)
        reactsReference.get().addOnCompleteListener {
            if (it.isSuccessful && it.result.documents.size > 0) {
                for (react in it.result.documents) {
                    react.reference.delete()
                }
            }
        }
        //Remove post image
        val storageReference = FirebaseStorage.getInstance()
            .getReference("Posts").child(postId)
        storageReference.delete()
    }

    fun deleteVideoPost(postId: String) {
        //Remove post
        val postReference = db.collection("VideoPosts").document(postId)
        postReference.delete().addOnCompleteListener(OnCompleteListener {
            clearFragmentMLD.value = true
        })
        //Remove Comments
        val commentReference = db.collection("Comments").whereEqualTo("postID", postId)
        commentReference.get().addOnCompleteListener {
            if (it.isSuccessful && it.result.documents.size > 0) {
                for (comment in it.result.documents) {
                    comment.reference.delete()
                }
            }
        }
        //Remove reacts
        val reactsReference = db.collection("PostReacts").whereEqualTo("postId", postId)
        reactsReference.get().addOnCompleteListener {
            if (it.isSuccessful && it.result.documents.size > 0) {
                for (react in it.result.documents) {
                    react.reference.delete()
                }
            }
        }
        //Remove post image
        val storageReference = FirebaseStorage.getInstance()
            .getReference("VideoPosts").child(postId)
        storageReference.delete()
    }

    fun createPollPost(
        desc: String,
        ownerId: String,
        pollId: String,
        multiCheck: Boolean,
        pollItems: ArrayList<Poll>
    ) {
        val postMap = hashMapOf(
            "description" to desc,
            "pollItems" to pollItems,
            "multiCheck" to multiCheck,
            "id" to pollId,
            "ownerID" to ownerId,
            "postDate" to Date()
        )
        db.collection("Polls").document(pollId).set(postMap)
            .addOnCompleteListener(OnCompleteListener {
                clearFragmentMLD.value = it.isSuccessful
            })
    }

    fun editPollChoice(oldPollPost: PollPost, selectedPoll: Poll, currentUserId: String) {
        //Create poll post reference
        val reference = db.collection("Polls").document(oldPollPost.id.toString())
        //Get new poll post
        reference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                val pollItemsList = ArrayList<Poll>()
                val allUsers = ArrayList<String>()
                val newPollPost = it.result.toObject<PollPost>() //Poll post we got from database
                //Check if new poll post is not null and remove old images
                val pollItems =
                    newPollPost!!.pollItems //Poll Items from the post we got from database
                if (pollItems != null) {
                    for (pollItem in pollItems) {
                        for (user in pollItem.users!!) {
                            if (!allUsers.contains(user)) {
                                allUsers.add(user)
                            }
                        }

                        if (!allUsers.contains(currentUserId)) {
                            allUsers.add(currentUserId)
                        }
                        if (oldPollPost.multiCheck == false) {
                            //Check if the loop item is the same selected item to do edits
                            if (pollItem == selectedPoll) {
                                /*
                                For its single selection if the user id is in any of the choices
                                we remove it from that choice and add it to the new one
                                 */
                                if (!pollItem.users.contains(currentUserId)) {
                                    pollItem.users.add(currentUserId)
                                }
                                //Updates percentage
                                pollItem.percentage =
                                    getPercentage(pollItem.users.size, allUsers.size)
                                pollItemsList.add(pollItem)
                            } else {
                                //If its not the same item then we remove the user from this one
                                if (pollItem.users.contains(currentUserId)) {
                                    pollItem.users.remove(currentUserId)
                                }
                                pollItem.percentage =
                                    getPercentage(pollItem.users.size, allUsers.size)
                                pollItemsList.add(pollItem)
                            }
                        } else {
                            /*If multi check is allowed we just add the user to the choice
                            while leaving the old one and removes user while clicking on the
                            same choice again
                            */
                            if (pollItem == selectedPoll) {
                                if (!pollItem.users.contains(currentUserId)) {
                                    pollItem.users.add(currentUserId)
                                } else {
                                    pollItem.users.remove(currentUserId)
                                }
                            }
                            pollItem.percentage =
                                getPercentage(pollItem.users.size, allUsers.size)
                            pollItemsList.add(pollItem)
                        }
                    }
                }
                reference.update("pollItems", pollItemsList)
            }
        })
    }

    //Used to remove unnecessary old poll images from storage
    private fun deleteOldPollImages(pollId: String, pollItems: ArrayList<Poll>) {
        val oldImagesIds = ArrayList<String>()
        val newPollItems = ArrayList<String>()
        val storageReference = FirebaseStorage.getInstance()
            .getReference("Polls").child(pollId)
        storageReference.listAll().addOnCompleteListener {
            val storageChildrenList = it.result.items
            for (item in storageChildrenList) {
                oldImagesIds.add(item.name)
            }
            for (newItem in pollItems) {
                newPollItems.add(newItem.pollItemId.toString())
            }
            for (oldItem in oldImagesIds) {
                if (!newPollItems.contains(oldItem)) {
                    storageReference.child(oldItem).delete()
                }
            }
        }
    }

    fun editPollPostData(
        desc: String,
        pollId: String,
        multiCheck: Boolean,
        pollItems: ArrayList<Poll>
    ) {
        val reference = db.collection("Polls").document(pollId)
        reference.update(
            "description", desc,
            "multiCheck", multiCheck,
            "pollItems", pollItems
        )
            .addOnCompleteListener(OnCompleteListener {
                deleteOldPollImages(pollId, pollItems)
                clearFragmentMLD.value = it.isSuccessful
            })
    }

    private fun getPercentage(usersPerItem: Int, totalUsers: Int): Float {
        return (usersPerItem * 100 / totalUsers).toFloat()
    }

    fun getPolls() {
        val polls: ArrayList<PollPost> = ArrayList()
        val postReference = db.collection("Polls").orderBy("postDate")
        postReference.get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                polls.addAll(it.result.toObjects(PollPost::class.java))
                pollsMLD.value = polls
            }
        })
    }

    fun deletePollPost(pollId: String) {
        FirebaseStorage.getInstance().getReference("Polls").child(pollId).listAll()
            .addOnCompleteListener {
                val storageChildrenList = it.result.items
                for (image in storageChildrenList) {
                    image.delete()
                }
            }
        db.collection("Polls").document(pollId).delete()

        //Remove Comments
        val commentReference = db.collection("Comments").whereEqualTo("postID", pollId)
        commentReference.get().addOnCompleteListener {
            if (it.isSuccessful && it.result.documents.size > 0) {
                for (comment in it.result.documents) {
                    comment.reference.delete()
                }
            }
        }
        //Remove reacts
        val reactsReference = db.collection("PostReacts").whereEqualTo("postId", pollId)
        reactsReference.get().addOnCompleteListener {
            if (it.isSuccessful && it.result.documents.size > 0) {
                for (react in it.result.documents) {
                    react.reference.delete()
                }
            }
        }
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
            "postID" to postId,
            "comment" to comment,
            "ownerID" to commentOwnerId,
            "commentID" to commentId,
            "date" to date
        )
        reference.document(commentId).set(commentMap)
    }

    fun getPostComments(postId: String) {
        val reference = db.collection("Comments").whereEqualTo("postID", postId)
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
        reference.update("comment", newComment)
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
            "message" to message,
            "ownerID" to messageOwnerId,
            "messageID" to messageID,
            "imageLink" to "",
            "messageType" to messageType,
            "date" to Date()
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
        val reference = db.collection("GroupChat").orderBy("date")
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
            "title" to reminder.title,
            "desc" to reminder.desc,
            "type" to reminder.type,
            "reminderID" to id,
            "ownerID" to reminder.ownerID,
            "date" to reminder.date,
        )
        reference.document(id).set(map).addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                clearFragmentMLD.value = true
            }
        })
    }


    fun editReminder(reminderId: String, reminder: Reminder) {
        val reference = db.collection("Reminders").document(reminderId)
        reference.update(
            "title", reminder.title,
            "desc", reminder.desc,
            "type", reminder.type,
            "date", reminder.date
        ).addOnCompleteListener {
            clearFragmentMLD.value = true
        }
    }

    fun getReminders(myId: String) {
        val remindersList = ArrayList<Reminder>()
        val reference = db.collection("Reminders").whereEqualTo("ownerID", myId)
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
        val map = hashMapOf("name" to groupName, "groupID" to id, "members" to membersList)
        reference.document(id).set(map).addOnSuccessListener {
            clearFragmentMLD.value = true
        }
    }

    fun editGroup(groupId: String, groupName: String, membersList: ArrayList<String>) {
        val reference = db.collection("groups").document(groupId)
        reference.update(
            "members", membersList,
            "mame", groupName
        ).addOnCompleteListener {
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
                clearFragmentMLD.value = true
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
                uploadImage(
                    image,
                    "Gallery",
                    galleryItem.itemId,
                    contentResolver
                )
            } else {
                //Else notify that upload is done
                clearFragmentMLD.value = true
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
                clearFragmentMLD.value = true
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
            clearFragmentMLD.value = true
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

    fun createReact(postId: String, userId: String, react: String) {
        val reference = db.collection("PostReacts")
        reference.whereEqualTo("reactOwner", userId)
            .whereEqualTo("postId", postId).get().addOnCompleteListener(OnCompleteListener {
                if (it.isSuccessful && it.result.documents.size > 0) {
                    for (document in it.result.documents) {
                        val oldReact = document.toObject(PostReact::class.java)
                        if (oldReact!!.react == react) {
                            reference.document(document.id).delete()
                        } else {
                            reference.document(document.id).update("react", react)
                        }
                    }
                } else {
                    val id = reference.document().id
                    val reactMap =
                        hashMapOf(
                            "react" to react,
                            "reactOwner" to userId,
                            "postId" to postId,
                            "reactId" to id
                        )
                    reference.document(id).set(reactMap)
                }
            })

    }

    fun deleteReact(userId: String, postId: String) {
        db.collection("PostReacts").whereEqualTo("reactOwner", userId)
            .whereEqualTo("postId", postId).get().addOnCompleteListener(OnCompleteListener {
                if (it.isSuccessful) {
                    for (document in it.result.documents) {
                        db.collection("PostReacts").document(document.id).delete()
                    }
                }
            })
    }

    fun getReacts(postId: String) {
        val reactList = ArrayList<PostReact>()
        val reference = db.collection("PostReacts")
        reference.whereEqualTo("postId", postId).get().addOnCompleteListener(OnCompleteListener {
            if (it.isSuccessful) {
                reactList.addAll(it.result.toObjects(PostReact::class.java))
                reactMLD.value = reactList
            } else {
                reactMLD.value = reactList
            }
        })
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
        uploadTask.addOnSuccessListener(OnSuccessListener {
            storageReference.downloadUrl
                .addOnSuccessListener {
                    val url = it.toString()
                    when (location) {
                        "Avatars" -> {
                            db.collection(location).document(child).update("ImageLink", url)
                        }
                        else -> {
                            db.collection(location).document(child).update("imageLink", url)
                        }
                    }
                    clearFragmentMLD.value = true
                }.addOnFailureListener(OnFailureListener {
                    clearFragmentMLD.value = false
                })
        })
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
                        clearFragmentMLD.value = true
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
                        .update("Token", token)
                    Log.d("GET_TOKEN", token)
                }
            }
        })
    }
}