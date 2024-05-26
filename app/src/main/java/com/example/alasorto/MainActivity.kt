package com.example.alasorto

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.etebarian.meowbottomnavigation.MeowBottomNavigation
import com.example.alasorto.adapters.MainFragmentsAdapter
import com.example.alasorto.dataClass.Group
import com.example.alasorto.dataClass.MediaData
import com.example.alasorto.dataClass.UserData
import com.example.alasorto.notification.NotificationModel
import com.example.alasorto.notification.NotificationViewModel
import com.example.alasorto.offlineUserDatabase.OfflineUserData
import com.example.alasorto.offlineUserDatabase.OfflineUserViewModel
import com.example.alasorto.pendingAttendanceDatabase.PendingAttendance
import com.example.alasorto.pendingAttendanceDatabase.PendingAttendanceViewModel
import com.example.alasorto.pendingMessagesDatabase.PendingMessage
import com.example.alasorto.pendingMessagesDatabase.PendingMessageViewModel
import com.example.alasorto.utils.InternetCheck
import com.example.alasorto.utils.LocaleHelper
import com.example.alasorto.utils.ThemeHelper
import com.example.alasorto.viewModels.AppViewModel
import com.example.alasorto.viewModels.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val pendingMessageViewModel: PendingMessageViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()
    private val appViewModel: AppViewModel by viewModels()
    private val offlineUserViewModel: OfflineUserViewModel by viewModels()
    private val localeHelper = LocaleHelper()
    private val themeHelper = ThemeHelper()
    private val pendingMessagesList = ArrayList<PendingMessage>()
    private val notificationArgsMap = HashMap<String, String>()
    private val currentUserPhone = FirebaseAuth.getInstance().currentUser!!.phoneNumber
    private val uploadedMediaMap = HashMap<String, ArrayList<MediaData>>()

    private var currentUser: UserData? = null
    private var usersList = ArrayList<UserData>()
    private var hasConnection = false
    private var isCurrentUserLoaded = false //Checks if current user is loaded from database
    private var offlineUser: UserData? = null
    private var currentUserId = ""

    //Checks if fragment is loaded so app doesn't open fragment again
    private var isFragmentLoaded = false
    private var currentUploadingAttendance: PendingAttendance? = null

    private lateinit var internetCheck: InternetCheck
    private lateinit var connectionTV: TextView
    private lateinit var fragmentContainer: FragmentContainerView
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: MeowBottomNavigation
    private lateinit var pendingAttendanceViewModel: PendingAttendanceViewModel

    private lateinit var dialog: Dialog
    private lateinit var dialogTV: TextView
    private lateinit var builder: android.app.AlertDialog.Builder
    private lateinit var window: Window

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pendingAttendanceViewModel = ViewModelProvider(this)[PendingAttendanceViewModel::class.java]

        connectionTV = findViewById(R.id.tv_connection)
        bottomNavigation = findViewById(R.id.bottom_nav)
        viewPager = findViewById(R.id.main_view_pager)
        fragmentContainer = findViewById(R.id.main_frame)

        supportFragmentManager.addFragmentOnAttachListener { _, _ ->
            if (supportFragmentManager.backStackEntryCount > 0) {
                fragmentContainer.visibility = VISIBLE
            } else {
                fragmentContainer.visibility = GONE
            }
        }

        //Check internet Connection
        internetCheck = InternetCheck(application)
        internetCheck.observe(this) {
            hasConnection = it
            if (it) {
                connectionTV.visibility = GONE
                appViewModel.getCurrentUser()
                appViewModel.getAllUsers()
                appViewModel.userToken()
                appViewModel.setOnline()

                pendingMessageViewModel.readAllData.observe(this, Observer { it1 ->
                    if (it1 != null) {
                        pendingMessagesList.clear()
                        pendingMessagesList.addAll(it1.sortedBy { it3 -> it3.message.date })
                        if (pendingMessagesList.isNotEmpty()) {
                            for (message in pendingMessagesList) {
                                sendPendingMessages(message)
                            }
                        }
                    }
                })

            } else {
                connectionTV.text = getString(R.string.disconnected)
                connectionTV.visibility = VISIBLE
            }
        }

        //Create loading Dialogue
        dialog = Dialog(this)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.progress_dialogue)
        dialog.window!!.setBackgroundDrawable(
            InsetDrawable(ColorDrawable(Color.TRANSPARENT), 40)
        )
        dialogTV = dialog.findViewById(R.id.tv_loading_dialog)

        offlineUserViewModel.readAllData.observe(this, Observer
        { it1 ->
            if (it1.isNotEmpty()) {
                for (offlineUserData in it1) {

                    if (usersList.any { a -> a.userId == offlineUserData.user.userId }) {
                        usersList.removeAll { a -> a.userId == offlineUserData.user.userId }
                    }

                    usersList.add(offlineUserData.user)

                    if (offlineUserData.user.phone == currentUserPhone) {
                        currentUser = offlineUserData.user
                        currentUserId = currentUser!!.userId
                        if (!isFragmentLoaded) {
                            initViewPager()
                            setBottomNavigationItems()
                            isFragmentLoaded = true
                        }
                        appViewModel.currentUserMLD.value = currentUser
                        if (currentUser != null && !currentUser!!.verified) {
                            forceLogout()
                        }
                    }
                }
            }
        })

        appViewModel.removedAttendanceUser.observe(this, Observer {
            if (currentUploadingAttendance != null) {
                currentUploadingAttendance!!.startedUpload = true
                currentUploadingAttendance!!.allUsers.remove(it)
                pendingAttendanceViewModel.addAttendance(currentUploadingAttendance!!)
                pendingAttendanceViewModel.getAttendanceById(currentUploadingAttendance!!.databaseId)
            }
        })

        appViewModel.deletedUserIdMLD.observe(this, Observer {
            offlineUserViewModel.deleteUserById(it)
        })

        pendingAttendanceViewModel.attendanceById.observe(this, Observer {
            if (it != null) {
                if (currentUploadingAttendance == null) {
                    currentUploadingAttendance = it
                    val attAllUsersList = currentUploadingAttendance!!.allUsers
                    val attendanceId = currentUploadingAttendance!!.databaseId

                    if (currentUploadingAttendance!!.isNewAttendance) {
                        /*Somehow isEmpty returns false cuz it has "" element when empty*/
                        for (user in attAllUsersList) {
                            appViewModel.addUserAttendanceData(
                                user,
                                attendanceId,
                                currentUploadingAttendance!!.attendedUsers.contains(user)
                            )
                        }
                    } else { //Editing
                            for (user in attAllUsersList) {
                                appViewModel.editUserAttendanceData(
                                    user,
                                    attendanceId,
                                    currentUploadingAttendance!!.attendedUsers.contains(user)
                                )
                            }
                    }
                    if (attAllUsersList.isEmpty() || attAllUsersList[0] != "") {
                        if (currentUploadingAttendance!!.isNewAttendance) {
                            appViewModel.createAttendanceData(currentUploadingAttendance!!)
                        } else {
                            appViewModel.updateAttendanceData(currentUploadingAttendance!!)
                        }
                    }
                } else {
                    val attAllUsersList = currentUploadingAttendance!!.allUsers
                    if (attAllUsersList.isEmpty() || attAllUsersList[0] != "") {
                        if (currentUploadingAttendance!!.isNewAttendance) {
                            appViewModel.createAttendanceData(currentUploadingAttendance!!)
                        } else {
                            appViewModel.updateAttendanceData(currentUploadingAttendance!!)
                        }
                    }
                }
            }
        })

        pendingAttendanceViewModel.readAllData.observe(this, Observer {
            if (it.any { a -> a.isDeleting }) {
                if (currentUploadingAttendance == null) {
                    currentUploadingAttendance = it.first { a -> a.isDeleting }
                    val attAllUsersList = currentUploadingAttendance!!.allUsers
                    val attendanceId = currentUploadingAttendance!!.databaseId

                    for (user in attAllUsersList) {
                        appViewModel.removeUserAttendanceData(
                            user,
                            attendanceId
                        )
                    }

                    if (attAllUsersList.isEmpty() || attAllUsersList[0] != "") {
                        appViewModel.deleteAttendanceData(currentUploadingAttendance!!)
                    }
                } else {
                    val attAllUsersList = currentUploadingAttendance!!.allUsers
                    if (attAllUsersList.isEmpty() || attAllUsersList[0] != "") {
                        appViewModel.deleteAttendanceData(currentUploadingAttendance!!)
                    }
                }
            }
        })


        appViewModel.finishedAttendanceMLD.observe(this, Observer
        {
            if (currentUploadingAttendance != null) {
                val id = currentUploadingAttendance!!.databaseId
                currentUploadingAttendance = null
                pendingAttendanceViewModel.deleteAttendanceById(id)

                val date = it.keys.first()
                val case = if (it[it.keys.first()] == "NEW_ATT") {
                    getString(R.string.creating)
                } else if (it[it.keys.first()] == "EDIT_ATT") {
                    getString(R.string.editing)
                } else {
                    getString(R.string.deleting)
                }

                Toast.makeText(
                    this,
                    "$case ${getString(R.string.attendance_of_date)} $date ${getString(R.string.was_successful)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        chatViewModel.messageSentMLD.observe(this, Observer
        {

            val sentMessage = pendingMessagesList.firstOrNull { it1 -> it1.message.messageId == it }

            if (sentMessage != null) {
                chatViewModel.getMessageById(
                    sentMessage.collectionPath,
                    sentMessage.chatId,
                    it
                )
                pendingMessageViewModel.deleteMessageItem(sentMessage)
            }
        })

/* //NotificationViewModel
 notificationViewModel.connectionError.observe(this)
 {
     when (it) {
         "sending" -> {
             Toast.makeText(this, "sending notification", Toast.LENGTH_SHORT).show()
         }
         "sent" -> {
             Toast.makeText(this, "notification sent", Toast.LENGTH_SHORT).show()
         }
         "error while sending" -> {
             Toast.makeText(this, "error while sending", Toast.LENGTH_SHORT).show()
         }
     }
 }*/

        notificationViewModel.response.observe(this)
        {
            if (it.isNotEmpty())
                Log.d(
                    "NOTIFICATION_TEST",
                    "Notification in Kotlin: $it "
                )
        }

        appViewModel.currentUserMLD.observe(
            this, Observer
            {
                if (it != null) {

                    if (currentUser != null) {
                        if (it != currentUser) {
                            val oldOfflineUserData = OfflineUserData(it.userId, currentUser!!)
                            offlineUserViewModel.deleteUserItem(oldOfflineUserData)
                        }

                        if (currentUserId.isNotEmpty() && currentUser != null && !isFragmentLoaded) {
                            setBottomNavigationItems()
                            initViewPager()
                            isFragmentLoaded = true
                        }
                    } else {
                        currentUser = it
                        currentUserId = it.userId
                    }

                    val newOfflineUserData = OfflineUserData(it.userId, it)
                    offlineUserViewModel.addUserItem(newOfflineUserData)

                    if (currentUser != null) {
                        if (!currentUser!!.verified) {
                            forceLogout()
                        }
                    }
                }
            })

        bottomNavigation.setOnClickMenuListener {
            viewPager.setCurrentItem(it.id, true)
        }

        //Check changes in posts
        Firebase.firestore.collection("Users").addSnapshotListener()
        { _, _ ->
            if (hasConnection) {
                appViewModel.getAllUsers()
                appViewModel.getCurrentUser()
            }
        }

        appViewModel.usersMLD.observe(this, Observer
        {
            for (user in it) {
                val offlineUserData = OfflineUserData(user.userId, user)
                offlineUserViewModel.addUserItem(offlineUserData)
                if (usersList.any { a -> a.userId == user.userId }) {
                    usersList.removeAll { a -> a.userId == user.userId }
                }
                usersList.add(user)
            }
        })

        appViewModel.groupByIdMLD.observe(this, Observer
        {
            if (it != null) {
                goToGroupChat(it)
                viewPager.setCurrentItem(2, true)
            }
        })

        chatViewModel.uploadMediaDataMLD.observe(this, Observer
        {
            val key = it.keys.first()
            val mediaData = it[key]
            if (mediaData != null) {
                if (uploadedMediaMap.containsKey(key) && uploadedMediaMap[key] != null) {
                    if (!uploadedMediaMap[key]!!.contains(mediaData)) {
                        uploadedMediaMap[key]!!.add(mediaData)
                    }
                } else {
                    uploadedMediaMap[key] = arrayListOf(mediaData)
                }

                if (uploadedMediaMap.containsKey(key)) {
                    if (pendingMessagesList.any { it1 -> it1.chatId == key }) {
                        val pendingMessage =
                            pendingMessagesList.firstOrNull { it1 -> it1.chatId == key }
                        if (pendingMessage != null) {
                            val mediaDataList = pendingMessage.message.mediaData

                            if (mediaDataList.isNotEmpty() && mediaDataList.size == uploadedMediaMap[key]!!.size) {
                                pendingMessage.message.mediaData = uploadedMediaMap[key]!!
                                chatViewModel.sendChatMessage(pendingMessage)
                            }
                        }
                    }
                }
            }
        })
    }

    fun viewPagerScrollState(canScroll: Boolean) {
        viewPager.isUserInputEnabled = canScroll
    }

    private fun initViewPager() {
        val adapter = MainFragmentsAdapter(supportFragmentManager, lifecycle)
        if (intent.extras != null) {

            var argsString = intent.extras!!.getString("DATA_MAP")
            if (argsString != null) {
                argsString = argsString.replace("}", "")
                argsString = argsString.replace("{", "")
                argsString = argsString.replace("\"", "")

                val argsMap = argsString.split(",").associateTo(kotlin.collections.HashMap()) {
                    val (left, right) = it.split(":")
                    left to right
                }

                if (argsMap.containsKey("case")) {
                    if (argsMap["case"] == "Chat") {
                        //ToDo: Personal chat too
                        if (argsMap["id"] != null && argsMap["id"]!!.isNotEmpty()) {
                            appViewModel.getGroupById(argsMap["id"]!!)

                        }
                    }
                }
            }
            adapter.setArgs(intent.extras)
        }

        viewPager.isUserInputEnabled = true

        viewPager.adapter = adapter

        viewPager.setCurrentItem(0, true)

        viewPager.offscreenPageLimit = 4

        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    bottomNavigation.show(position, true)
                }
            })
    }

    private fun setBottomNavigationItems() {
        bottomNavigation.add(MeowBottomNavigation.Model(0, R.drawable.ic_home))
        bottomNavigation.add(MeowBottomNavigation.Model(1, R.drawable.ic_event))
        bottomNavigation.add(MeowBottomNavigation.Model(2, R.drawable.ic_group))
        bottomNavigation.add(MeowBottomNavigation.Model(3, R.drawable.ic_gallery))
        bottomNavigation.add(MeowBottomNavigation.Model(4, R.drawable.ic_settings))
    }

    override fun onResume() {
        super.onResume()
        this.onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewPager.currentItem != 0) {
                    viewPager.currentItem = 0
                }
            }
        })

        //Observes Changes in user
        Firebase.firestore.collection("Users").document(currentUserPhone!!)
            .addSnapshotListener { _, _ ->
                appViewModel.getCurrentUser()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        appViewModel.setOffline()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(localeHelper.setLocale(newBase!!))
        themeHelper.setTheme(newBase)
    }

    fun createNotification(notificationModel: NotificationModel) {
        notificationViewModel.sendNotification(notificationModel)
    }

    fun getCurrentUserData(): UserData? = currentUser

    fun getCurrentUserId(): String = currentUserId

    fun getAllUsers(): ArrayList<UserData> {
        return usersList
    }

    fun showLoadingDialog(text: String? = null) {
        dialog.show()
        if (!text.isNullOrEmpty()) {
            dialogTV.text = text
        }
    }

    fun dismissLoadingDialog() {
        dialog.dismiss()
    }

    private fun sendPendingMessages(pendingMessage: PendingMessage) {
        when (pendingMessage.message.messageType) {
            "Text" -> {
                val messageMediaData = pendingMessage.message.mediaData.sortedBy { it.index }
                if (pendingMessage.message.mediaData.isNotEmpty()) {
                    for (item in messageMediaData) {

                        if (item.type == "Image") {
                            chatViewModel.uploadImage(
                                item,
                                pendingMessage.chatId,
                                pendingMessage.message.messageId,
                                contentResolver
                            )
                        } else {
                            chatViewModel.uploadVideo(
                                item,
                                pendingMessage.chatId,
                                pendingMessage.message.messageId
                            )
                        }
                    }
                } else {
                    chatViewModel.sendChatMessage(pendingMessage)
                }
                uploadedMediaMap.clear()
            }
            "File" -> {
                chatViewModel.sendChatFile(pendingMessage)
            }
            else -> {
                chatViewModel.sendChatVoiceNote(pendingMessage)
            }
        }
    }

    private fun goToGroupChat(group: Group) {
        val fragment = ChatFragment()
        val manager = supportFragmentManager
        val transaction = manager.beginTransaction()

        val bundle = Bundle()
        bundle.putString("CHAT_ID", group.groupId)
        bundle.putString("COLLECTION_PATH", "GroupChat")
        bundle.putBoolean("IS_GROUP_CHAT", group.admins.isEmpty())
        bundle.putParcelable("GROUP", group)
        fragment.arguments = bundle

        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("CHAT_FRAGMENT")
        transaction.commit()
    }

    fun goToEnlargeMediaFragment(args: Bundle?) {
        val fragment = EnlargedMediaFragment()
        val manager = supportFragmentManager
        val transaction = manager.beginTransaction()
        if (args != null) {
            fragment.arguments = args
        }
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack("ENLARGED_MEDIA_FRAGMENT")
        transaction.commit()
    }

    private fun goToCreateUserDataFragment() {
        val fragment = CreateUserDataFragment()
        val manager = supportFragmentManager
        val transaction = manager.beginTransaction()
        val args = Bundle()
        args.putBoolean("IS_POINTS_ET_ENABLED", false)
        args.putString("PHONE_NUM", currentUserPhone)
        args.putBoolean("isNewUser", true)
        fragment.arguments = args
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    //Move to profile
    private fun goToSafeSpaceFragment() {

    }

    fun logout() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(true)
        builder.setTitle(R.string.logout)
        builder.setMessage(R.string.logout_confirm)
        builder.setPositiveButton(R.string.yes) { _, _ ->
            val sharedPreferences =
                this.getSharedPreferences("KeepLoggedIn", Context.MODE_PRIVATE)
            val intent = Intent(this, AuthActivity()::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            this.finish()
            val editor = sharedPreferences!!.edit()
            editor.putBoolean("IsLoggedIn", false)
            editor.apply()
            FirebaseAuth.getInstance().signOut()
        }
        builder.setNegativeButton(R.string.no) { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        if (!isFinishing) {
            dialog.show()
        }
    }

    //Logout without asking for user's permission in case of change in verification
    private fun forceLogout() {
        val sharedPreferences =
            this.getSharedPreferences("KeepLoggedIn", Context.MODE_PRIVATE)
        val intent = Intent(this, AuthActivity()::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        this.finish()
        val editor = sharedPreferences!!.edit()
        editor.putBoolean("IsLoggedIn", false)
        editor.apply()
        FirebaseAuth.getInstance().signOut()
    }
}