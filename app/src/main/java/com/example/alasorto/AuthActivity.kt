package com.example.alasorto

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.alasorto.utils.InternetCheck
import com.example.alasorto.utils.LocaleHelper
import com.example.alasorto.utils.ThemeHelper
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.firestore.ktx.memoryCacheSettings
import com.google.firebase.firestore.ktx.persistentCacheSettings
import com.google.firebase.ktx.Firebase

class AuthActivity : AppCompatActivity() {

    init {
       /* val settings = firestoreSettings {
            // Use memory cache
            setLocalCacheSettings(memoryCacheSettings {})
            // Use persistent disk cache (default)
            setLocalCacheSettings(persistentCacheSettings {})
        }
        Firebase.firestore.firestoreSettings = settings*/
    }

    private val localeHelper = LocaleHelper()
    private val themeHelper = ThemeHelper()

    private var hasConnection = false

    private lateinit var internetCheck: InternetCheck
    private lateinit var connectionTV: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        connectionTV = findViewById(R.id.tv_connection)

        //Check internet Connection
        internetCheck = InternetCheck(application)
        internetCheck.observe(this) {
            hasConnection = it
            if (it) {
                connectionTV.visibility = View.GONE
            } else {
                connectionTV.text = getString(R.string.disconnected)
                connectionTV.visibility = View.VISIBLE
            }
        }

        val args = if (intent != null) {
            intent.extras
        } else {
            null
        }

        if (savedInstanceState == null) {
            val fragment = SplashScreenFragment()

            if (args != null) {
                fragment.arguments = args
            }

            val manager = this.supportFragmentManager
            val transaction = manager.beginTransaction()

            transaction.add(R.id.auth_frame, fragment)
            transaction.commit()
        }
    }

    fun hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(localeHelper.setLocale(newBase!!))
        themeHelper.setTheme(newBase)
    }
}