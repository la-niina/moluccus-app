package moluccus.app.ui

import android.annotation.SuppressLint
import android.app.Notification
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.messaging.ktx.remoteMessage
import com.google.firebase.storage.StorageReference
import moluccus.app.R
import moluccus.app.adapter.ImagePostAdapter
import moluccus.app.databinding.PostLayoutBinding
import moluccus.app.route.CommitPost
import moluccus.app.route.Imagedecoderers
import moluccus.app.service.PostFirebaseMessagingService
import moluccus.app.util.FirebaseUtils
import moluccus.app.util.FirebaseUtils.firebaseDatabase
import moluccus.app.util.OpenMultipleDocumentContract
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class PostLayout : Fragment() {
    private var _binding: PostLayoutBinding? = null
    private val binding get() = _binding!!

    private var bindingUsername = ""
    private var bindingUserHandle = ""
    private var bindingUserAvatar = ""

    private val openDocumentLauncher = registerForActivityResult(
        OpenMultipleDocumentContract()
    ) { uris ->
        onImageSelected(uris)
    }

    private val imageList = mutableListOf<Imagedecoderers>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PostLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun isYouTubeUrl(url: String): Boolean {
        return url.startsWith(".mp4")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser
        firebaseDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val value = snapshot.child("users").child(user!!.uid).child("usr_information")
                    if (value != null) {
                        bindingUsername = value.child("usr_name").value.toString()
                        bindingUserHandle = value.child("usr_handle").value.toString()
                        bindingUserAvatar = value.child("usr_avatar").value.toString()
                    } else {
                        // TODO: check
                    }
                } else {
                    // toast("incomplete account")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("TAG", "Failed to read value.", error.toException())
            }
        })
        when {
            TextUtils.isEmpty(binding.contentBlogs.text) -> {
                Toast.makeText(requireContext(), "empty post", Toast.LENGTH_SHORT).show()
                setHasOptionsMenu(false)
            }
            else -> {
            }
        }

        binding.videourl.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            @SuppressLint("SetTextI18n")
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0!!.isNotEmpty()){
                    binding.uploadResources.isEnabled = true
                    imageList.clear()
                }else if (p0.length >= 6){
                    val inputText = binding.videourl.text.toString()
                    if (isYouTubeUrl(inputText)) {
                        // Set the video URL in the view
                        binding.videourl.setText(inputText)
                    } else {
                        // Clear the input text
                        binding.videourl.setText("")
                        // Set an error message in the view
                        binding.videourl.setText("Only ending with .mp4 url video formats are supported.")
                    }
                } else if (p0.length <= 4){
                    binding.videourl.setText("paste url...")
                }
            }
            override fun afterTextChanged(p0: Editable?) {}
        })

        binding.uploadResources.setOnClickListener {
            // openDocument.launch(arrayOf("image/*"))
            val mimeTypes = arrayOf("image/*", "application/gallery")
            openDocumentLauncher.launch(mimeTypes)
        }
        setHasOptionsMenu(true)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.post_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.ic_commit -> {
                initPhotosAndVideos()
            }
        }
        return false
    }

    @SuppressLint("SimpleDateFormat")
    private fun initPhotosAndVideos() {
        if (binding.videourl.text!!.isEmpty()){
            if (imageList.isEmpty()) {
                val df: DateFormat = SimpleDateFormat("d MMM yyyy, HH:mm")
                val date: String = df.format(Calendar.getInstance().time).toString()
                val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser

                val commitPost = CommitPost(
                    id = firebaseDatabase.child("commits").push().key ?: "",
                    content = binding.contentBlogs.text?.trim().toString(),
                    imageUrl = listOf(), // Add empty list of image URLs
                    videoUrl = "",
                    authorId = user?.uid ?: "",
                    timeStamp = date.trim(),
                    likesCount = 0,
                    commentsCount = 0,
                )

                firebaseDatabase.child("commits").child(commitPost.id)
                    .setValue(commitPost)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            findNavController().navigate(R.id.action_home)
                        } else {
                            Toast.makeText(requireContext(), "not posted", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser
                val storageReference = FirebaseUtils.firebaseStorage
                    .child("galleries")
                    .child(user!!.uid)
                    .child("$user.uid-posts-photo")
                val uploadingTheImagesToStorage = imageList.toList()
                putImagesInStorage(
                    storageReference,
                    uploadingTheImagesToStorage
                ) // Call putImagesInStorage()
            }
        } else {
            val df: DateFormat = SimpleDateFormat("d MMM yyyy, HH:mm")
            val date: String = df.format(Calendar.getInstance().time).toString()
            val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser

            val commitPost = CommitPost(
                id = firebaseDatabase.child("commits").push().key ?: "",
                content = binding.contentBlogs.text?.trim().toString(),
                imageUrl = listOf(), // Add empty list of image URLs
                videoUrl = binding.videourl.text?.trim().toString(),
                authorId = user?.uid ?: "",
                timeStamp = date.trim(),
                likesCount = 0,
                commentsCount = 0,
            )

            firebaseDatabase.child("commits").child(commitPost.id)
                .setValue(commitPost)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        findNavController().navigate(R.id.action_home)
                    } else {
                        Toast.makeText(requireContext(), "not posted", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun putImagesInStorage(
        storageReference: StorageReference,
        uploadingTheImagesToStorage: List<Imagedecoderers>
    ) {
        val imagesUrls = mutableListOf<String>()
        // upload each image to Cloud Storage and add its URL to imagesUrls
        uploadingTheImagesToStorage.forEachIndexed { index, image ->
            val imageRef = storageReference.child("image_$index")
            // put image in storage
            imageRef.putFile(image.imageUri!!)
                .addOnSuccessListener {
                    // get download URL of uploaded image
                    imageRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            imagesUrls.add(uri.toString())

                            // check if all images have been uploaded
                            if (imagesUrls.size == uploadingTheImagesToStorage.size) {
                                // all images have been uploaded, now we can create the CommitPost object
                                val df: DateFormat = SimpleDateFormat("d MMM yyyy, HH:mm")
                                val date: String = df.format(Calendar.getInstance().time).toString()
                                val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser

                                val commitPost = CommitPost(
                                    id = firebaseDatabase.child("commits").push().key ?: "",
                                    content = binding.contentBlogs.text?.trim().toString(),
                                    imageUrl = imagesUrls,
                                    videoUrl = "",
                                    authorId = user?.uid ?: "",
                                    timeStamp = date.trim(),
                                    likesCount = 0,
                                    commentsCount = 0,
                                )

                                val fm = Firebase.messaging
                                fm.send(remoteMessage("870256442359@fcm.googleapis.com") {
                                    messageId = messageId
                                    addData("current_uid", commitPost.authorId)
                                    addData("user_profile", commitPost.imageAvatarUrl)
                                    addData("user_handle", commitPost.user_handle)
                                    addData("user_name", commitPost.user_name)
                                    addData("message_content", "${commitPost.user_handle} ${commitPost.timeStamp}")
                                })

                                // add CommitPost to database
                                firebaseDatabase.child("commits").child(commitPost.id)
                                    .setValue(commitPost)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            findNavController().navigate(R.id.action_home)
                                        } else {
                                            Toast.makeText(
                                                requireContext(),
                                                "not posted",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            }
                        }
                }
        }
    }

    private fun onImageSelected(uris: List<Uri>) {
        imageList.addAll(uris.map { Imagedecoderers(it) })

        val adapter = ImagePostAdapter(requireContext(), imageList)
        val recyclerView = binding.imagePosts
        val layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Glide.with(this).pauseRequests()
    }
}