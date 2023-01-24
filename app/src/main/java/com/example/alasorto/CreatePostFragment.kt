package com.example.alasorto

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.alasorto.dataClass.Posts
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity.*
import java.io.File

private const val RESULT_OK = -1
private const val RESULT_CANCEL = 0

@Suppress("DEPRECATION")
class CreatePostFragment : Fragment() {
    private lateinit var titleET: EditText
    private lateinit var descET: EditText
    private lateinit var dayET: EditText
    private lateinit var monthET: EditText
    private lateinit var yearET: EditText
    private lateinit var selectIV: ImageView
    private lateinit var postIV: ImageView
    private lateinit var removeImageBtn: ImageButton
    private lateinit var createPostBtn: ImageButton
    private lateinit var viewModel: AppViewModel
    private lateinit var dialog: AlertDialog
    private lateinit var builder: AlertDialog.Builder
    private lateinit var window: Window
    private lateinit var internetCheck: InternetCheck
    private lateinit var postImageLink: String
    private lateinit var postId: String
    private var hasConnection = false
    private var isNewPost = true
    private var postImage: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_create_post, container, false)
        view.isClickable = true

        //Initialize Widgets
        titleET = view.findViewById(R.id.et_create_post_title)
        descET = view.findViewById(R.id.et_create_post_desc)
        dayET = view.findViewById(R.id.et_post_day)
        monthET = view.findViewById(R.id.et_post_month)
        yearET = view.findViewById(R.id.et_post_year)
        selectIV = view.findViewById(R.id.iv_create_post_select)
        postIV = view.findViewById(R.id.iv_create_post_image)
        removeImageBtn = view.findViewById(R.id.ib_post_remove_image)
        createPostBtn = view.findViewById(R.id.ib_create_post)

        //Get edit post data
        val args = this.arguments
        if (args != null) {
            isNewPost = args.getBoolean("IsNewPost")
            if (!isNewPost) {
                val post = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    args.getParcelable("Editing_Post", Posts::class.java)
                } else {
                    args.getParcelable("Editing_Post")
                }
                if (post != null) {
                    titleET.setText(post.Title)
                    descET.setText(post.Description)
                    if (post.Day != null) {
                        dayET.setText(post.Day.toString())
                    }
                    if (post.Month != null) {
                        monthET.setText(post.Month.toString())
                    }
                    if (post.Year != null) {
                        yearET.setText(post.Year.toString())
                    }
                    if (post.ImageLink != null && post.ImageLink.isNotEmpty()) {
                        removeImageBtn.visibility = View.VISIBLE
                        postIV.visibility = View.VISIBLE
                        Glide.with(requireContext()).load(post.ImageLink).into(postIV)
                    }
                    postImageLink = post.ImageLink.toString()
                    postId = post.ID.toString()
                }
            }
        }

        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        internetCheck = InternetCheck(requireActivity().application)
        internetCheck.observe(this.viewLifecycleOwner) {
            hasConnection = it
        }

        //Create Alert Dialogue
        builder = AlertDialog.Builder(context)
        builder.setCancelable(false)
        builder.setView(R.layout.progress_dialogue)
        dialog = builder.create()
        window = dialog.window!!
        window.setBackgroundDrawableResource(android.R.color.transparent)

        viewModel.clearFragmentMLD.observe(this.viewLifecycleOwner, Observer {
            if (it) {
                dialog.dismiss()
                requireActivity().supportFragmentManager.popBackStack(
                    "HANDLE_POSTS_FRAGMENT", FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
            }
        })

        selectIV.setOnClickListener(View.OnClickListener {
            openGalleryForImage()
        })

        removeImageBtn.setOnClickListener(View.OnClickListener {
            removeImageBtn.visibility = View.GONE
            postIV.visibility = View.GONE
            postImage = null
            postImageLink = ""
        })

        createPostBtn.setOnClickListener(View.OnClickListener {
            //List that contains the ETs that MUST be filled
            val viewsList = arrayListOf(
                titleET,
                descET,
                dayET,
                monthET,
                yearET
            )
            //If any of ETs are not filled app will show error drawable and background
            for (et in viewsList) {
                if (et.text.isEmpty()) {
                    et.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_error,
                        0
                    )
                    et.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.error_custom_input)
                }
            }

            //Get TVs texts to String
            val title = titleET.text.toString().trim()
            val desc = descET.text.toString().trim()
            val day = dayET.text.toString().trim()
            val month = monthET.text.toString().trim()
            val year = yearET.text.toString().trim()

            //Check internet connection
            if (hasConnection) {
                if (title.isNotEmpty() && desc.isNotEmpty() && day.isNotEmpty()
                    && month.isNotEmpty() && year.isNotEmpty()
                ) {
                    dialog.show()
                    if (isNewPost) {
                        viewModel.createPost(
                            title,
                            desc,
                            Firebase.auth.currentUser?.phoneNumber.toString(),
                            day.toInt(),
                            month.toInt(),
                            year.toInt(),
                            postImage,
                            requireActivity().contentResolver
                        )
                    } else {
                        viewModel.editPost(
                            title, desc, postId, postImageLink, day.toInt(), month.toInt(),
                            year.toInt(), postImage, requireActivity().contentResolver
                        )
                    }
                } else {
                    Toast.makeText(context, R.string.missing_data, Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(context, R.string.check_internet, Toast.LENGTH_SHORT).show()
            }
        })

        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    activity!!.supportFragmentManager.popBackStack(
                        "HANDLE_POSTS_FRAGMENT",
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                    this.isEnabled = false
                }
            })

    }

    //Crop Image
    //Select Image Launchers
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent = result.data!!
                val sourceUri: Uri = data.data!!
                val destinationUri = Uri.fromFile(
                    File(
                        requireActivity().cacheDir,
                        queryName(requireActivity().contentResolver, sourceUri)
                    )
                )

                //UCrop options
                val options = UCrop.Options()
                options.setToolbarColor(ContextCompat.getColor(requireActivity(), R.color.black))
                options.setStatusBarColor(ContextCompat.getColor(requireActivity(), R.color.black))
                options.setToolbarWidgetColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.white
                    )
                )

                options.setFreeStyleCropEnabled(true)
                options.setAllowedGestures(SCALE, ALL, SCALE)

                val intent = UCrop.of(sourceUri, destinationUri)
                    .withOptions(options)
                    .getIntent(requireActivity())
                cropResult.launch(intent)
            }
        }

    //Crop Result Launcher
    private val cropResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            assert(result.data != null)
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                postIV.visibility = View.VISIBLE
                removeImageBtn.visibility = View.VISIBLE
                postIV.setImageURI(resultUri)
                postImage = resultUri
            }
        } else if (result.resultCode == RESULT_CANCEL) {
            Toast.makeText(context, "Image was not Uploaded", Toast.LENGTH_SHORT).show()
        }
    }

    //UCrop Sh!t
    private fun queryName(resolver: ContentResolver, uri: Uri): String {
        val returnCursor = resolver.query(uri, null, null, null, null)
        val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        returnCursor.close()
        return name
    }

    //Select Image
    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }
}