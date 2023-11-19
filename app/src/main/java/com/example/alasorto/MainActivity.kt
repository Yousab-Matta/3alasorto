package com.example.alasorto

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View.*
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import androidx.lifecycle.Observer
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.viewpager2.widget.ViewPager2
import com.etebarian.meowbottomnavigation.MeowBottomNavigation
import com.example.alasorto.adapters.MainFragmentsAdapter
import com.example.alasorto.dataClass.UserData
import com.example.alasorto.notification.Data
import com.example.alasorto.notification.NotificationModel
import com.example.alasorto.notification.NotificationViewModel
import com.example.alasorto.offlineUserDatabase.OfflineUserData
import com.example.alasorto.offlineUserDatabase.OfflineUserViewModel
import com.example.alasorto.pendingMessagesDatabase.PendingMessage
import com.example.alasorto.pendingMessagesDatabase.PendingMessageViewModel
import com.example.alasorto.utils.InternetCheck
import com.example.alasorto.utils.LocaleHelper
import com.example.alasorto.utils.SortUsers
import com.example.alasorto.utils.ThemeHelper
import com.example.alasorto.viewModels.AppViewModel
import com.example.alasorto.viewModels.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.simform.custombottomnavigation.Model
import com.simform.custombottomnavigation.SSCustomBottomNavigation
import dagger.hilt.android.AndroidEntryPoint
import np.com.susanthapa.curved_bottom_navigation.CbnMenuItem
import np.com.susanthapa.curved_bottom_navigation.CurvedBottomNavigationView
import java.util.*
import kotlin.collections.ArrayList

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val pendingMessageViewModel: PendingMessageViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()
    private val appViewModel: AppViewModel by viewModels()
    private val offlineUserViewModel: OfflineUserViewModel by viewModels()
    private val phoneNum = Firebase.auth.currentUser?.phoneNumber
    private val localeHelper = LocaleHelper()
    private val themeHelper = ThemeHelper()
    private val pendingMessagesList = ArrayList<PendingMessage>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser!!.phoneNumber

    private lateinit var internetCheck: InternetCheck
    private lateinit var connectionTV: TextView
    private lateinit var fragmentContainer: FragmentContainerView
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: MeowBottomNavigation

    private lateinit var dialog: Dialog
    private lateinit var builder: android.app.AlertDialog.Builder
    private lateinit var window: Window

    private var currentUser: UserData? = null
    private var usersList = ArrayList<UserData>()
    private var hasConnection = false
    private var isCurrentUserLoaded = false //Checks if current user is loaded from database
    private var offlineUser: UserData? = null
    private var sendingMessage: PendingMessage? = null

    //Checks if fragment is loaded so app doesn't open fragment again
    private var isFragmentLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

                pendingMessageViewModel.readAllData.observe(this, Observer
                { it1 ->
                    if (it1 != null && it1.isNotEmpty()) {
                        sendingMessage = it1.sortedBy { it2 -> it2.message.date }[0]
                        if (sendingMessage != null) {
                            sendPendingMessages(sendingMessage!!)
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

        offlineUserViewModel.readAllData.observe(this, Observer { it1 ->
            if (it1.isNotEmpty()) {
                for (offlineUserData in it1) {
                    SortUsers().sortUser(usersList, offlineUserData.user, offlineUserViewModel)

                    if (offlineUserData.user.phone == currentUserId) {
                        currentUser = offlineUserData.user
                        appViewModel.currentUserMLD.value = currentUser

                        if (currentUser != null && !currentUser!!.verified) {
                            forceLogout()
                        }
                    }
                }
            }
        })

        chatViewModel.messageSentMLD.observe(this, Observer {
            if (sendingMessage != null) {
                chatViewModel.getMessageById(
                    sendingMessage!!.collectionPath,
                    sendingMessage!!.chatId,
                    it
                )
                pendingMessageViewModel.deleteMessageItem(sendingMessage!!)
            }
        })

        //NotificationViewModel
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
        }

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
                            val oldOfflineUserData = OfflineUserData(currentUserId!!, currentUser!!)
                            offlineUserViewModel.deleteUserItem(oldOfflineUserData)
                        }
                    } else {
                        currentUser = it
                    }

                    val newOfflineUserData = OfflineUserData(it.phone, it)
                    offlineUserViewModel.updateUserItem(newOfflineUserData)

                    if (currentUser != null) {
                        if (!currentUser!!.verified) {
                            forceLogout()
                        }
                    }
                }
            })

        setBottomNavigationItems()
        initViewPager()

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
            if (it != null) {
                val mUsersListReplica = usersList

                val mNewList = SortUsers().sortUsers(mUsersListReplica, it, offlineUserViewModel)
                usersList = mNewList
            }
        })
    }

    private fun initViewPager() {
        viewPager.isUserInputEnabled = true

        viewPager.adapter = MainFragmentsAdapter(supportFragmentManager, lifecycle)

        viewPager.setCurrentItem(0, true)

        viewPager.offscreenPageLimit = 4

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
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
            }
        })

        //Observes Changes in user
        Firebase.firestore.collection("Users")
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

    fun createNotification(token: String, title: String, message: String) {
        notificationViewModel
            .sendNotification(
                NotificationModel(
                    token,
                    Data(title, message)
                )
            )
    }

    fun getCurrentUser(): UserData? {
        return if (currentUser != null) {
            currentUser!!
        } else {
            null
        }
    }

    fun getAllUsers(): ArrayList<UserData> {
        return usersList
    }

    fun showLoadingDialog() {
        dialog.show()
    }

    fun dismissLoadingDialog() {
        dialog.dismiss()
    }

    private fun sendPendingMessages(pendingMessage: PendingMessage) {
        when (pendingMessage.message.messageType) {
            "Text" -> {
                chatViewModel.sendChatMessage(pendingMessage)
            }
            "Image" -> {
                chatViewModel.sendChatImage(pendingMessage, contentResolver)
            }
            "Video" -> {
                chatViewModel.sendChatVideo(pendingMessage)
            }
            "File" -> {
                chatViewModel.sendChatFile(pendingMessage)
            }
            else -> {
                chatViewModel.sendChatVoiceNote(pendingMessage, this)
            }
        }
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
        args.putString("PHONE_NUM", phoneNum)
        args.putBoolean("isNewUser", true)
        fragment.arguments = args
        transaction.add(R.id.main_frame, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    //Move to profile
    private fun goToSafeSpaceFragment() {

    }

    private fun logout() {
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