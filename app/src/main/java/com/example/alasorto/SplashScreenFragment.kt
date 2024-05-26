package com.example.alasorto

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.alasorto.viewModels.AppViewModel
import java.util.*

@SuppressLint("CustomSplashScreen")
class SplashScreenFragment : Fragment() {

    private val appViewModel: AppViewModel by viewModels()
    private val timer = Timer()

    private var isLoggedIn = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isLoggedInPreferences =
            requireActivity().getSharedPreferences("KeepLoggedIn", Context.MODE_PRIVATE)
        isLoggedIn = isLoggedInPreferences.getBoolean("IsLoggedIn", false)

        if (Build.VERSION.SDK_INT >= 33) {
            startReadImagePermissionRequest()
        } else {
            startRecordPermissionRequest()
        }

        appViewModel.currentUserMLD.observe(this.viewLifecycleOwner, androidx.lifecycle.Observer {
            if (it != null) {
                timer.cancel()
                if (it.verified) {
                    goToMainActivity()
                } else {
                    goToPendingVerificationFragment()
                }
            }
        })
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (Build.VERSION.SDK_INT >= 33) {
                if (!hasPermissions(requireContext(), READ_MEDIA_VIDEO)) {
                    startReadVideoPermissionRequest()
                } else {
                    if (!hasPermissions(requireContext(), READ_MEDIA_AUDIO)) {
                        startReadAudioPermissionRequest()
                    } else {
                        goToRegisterFragment()
                    }
                }
            } else {
                if (!hasPermissions(requireContext(), WRITE_EXTERNAL_STORAGE)) {
                    startWriteStoragePermissionRequest()
                } else {
                    goToRegisterFragment()
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= 33) {
                startReadImagePermissionRequest()
            } else {
                startRecordPermissionRequest()
            }
        }
    }

    private fun goToRegisterFragment() {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (isLoggedIn) {
                    appViewModel.getCurrentUser()
                } else {
                    val manager = requireActivity().supportFragmentManager
                    val transaction = manager.beginTransaction()
                    transaction.replace(R.id.auth_frame, RegisterFragment())
                    transaction.commit()
                }
                timer.cancel()
            }
        }, 1000, 3000)
    }

    private fun startRecordPermissionRequest() {
        requestPermissionLauncher.launch(RECORD_AUDIO)
    }

    private fun startWriteStoragePermissionRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")

            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    uri
                )
            )
        }
        requestPermissionLauncher.launch(WRITE_EXTERNAL_STORAGE)
    }

    private fun startReadImagePermissionRequest() {
        requestPermissionLauncher.launch(READ_MEDIA_IMAGES)
    }

    private fun startReadVideoPermissionRequest() {
        requestPermissionLauncher.launch(READ_MEDIA_VIDEO)
    }

    private fun startReadAudioPermissionRequest() {
        requestPermissionLauncher.launch(READ_MEDIA_AUDIO)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean =
        permissions.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun goToMainActivity() {
        val intent = Intent(requireActivity(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        if (this.arguments != null) {
            intent.putExtras(this.arguments!!)
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun goToPendingVerificationFragment() {
        val fragment = PendingVerificationFragment()
        val manager = requireActivity().supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.replace(R.id.auth_frame, fragment)
        transaction.commit()
    }
}