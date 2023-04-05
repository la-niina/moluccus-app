package moluccus.app.base

import android.Manifest
import android.annotation.SuppressLint
import android.app.Fragment
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.datePicker
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import moluccus.app.R
import moluccus.app.databinding.ActivityInfoBinding
import moluccus.app.glide.GlideImageLoader
import moluccus.app.util.Extensions.toast
import moluccus.app.util.FirebaseUtils
import moluccus.app.util.FirebaseUtils.firebaseDatabase
import moluccus.app.util.FirebaseUtils.firebaseStorage
import moluccus.app.util.OpenDocumentContract
import moluccus.app.util.users
import java.util.*

class InfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInfoBinding
    private val LOCATION_PERMISSION_REQUEST_CODE = 123

    private val openDocument = registerForActivityResult(OpenDocumentContract()) { eri ->
        if (eri != null) {
            onImageSelected(eri)
        }
    }

    private val openDocumentAvatar = registerForActivityResult(OpenDocumentContract()) { uri ->
        if (uri != null) {
            onImageSelectedAvatar(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "Edit Profile"
        setSupportActionBar(binding.toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }
        initLayout()
        intialLocationTest()
    }

    override fun onStart() {
        super.onStart()
        intialLocationTest()
    }

    @Deprecated("Deprecated in Java")
    override fun onAttachFragment(fragment: Fragment?) {
        super.onAttachFragment(fragment)
        intialLocationTest()
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun initLayout() {
        val cover_holder = binding.coverHolder
        val avatar_holder = binding.avatarHolder

        val username_holder = binding.usernameHolder
        val userhandle_holder = binding.userhandleHolder
        val bio_holder = binding.bioHolder

        val location_holder = binding.locationHolder
        location_holder.isEnabled = false

        val website_holder = binding.websiteHolder
        val birthdate_holder = binding.birthdateHolder

        val update_profile_holder = binding.updateProfileButton

        val user = FirebaseUtils.firebaseAuth.currentUser
        val uid = user!!.uid

        cover_holder.setOnClickListener {
            openDocument.launch(arrayOf("image/*"))
        }

        avatar_holder.setOnClickListener {
            openDocumentAvatar.launch(arrayOf("image/*"))
        }

        birthdate_holder.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    MaterialDialog(this).show {
                        datePicker { dialog, date ->
                            val year = date.get(Calendar.YEAR)
                            val month = date.get(Calendar.MONTH)
                            val day = date.get(Calendar.DAY_OF_MONTH)
                            val dob = "$day/$month/$year"
                            birthdate_holder.setText(dob)
                        }
                    }
                }
                MotionEvent.ACTION_UP -> v.performClick()
                else -> {
                    // Todo: move
                }
            }
            true
        }

        firebaseDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val value =
                        snapshot.child("users").child(user.uid).getValue(
                            users::class.java
                        )
                    if (value != null) {
                        // important holders
                        username_holder.setText(value.usr_information!!.usr_name!!.trim())
                        userhandle_holder.setText(value.usr_information.usr_handle!!.trim())
                        bio_holder.setText(value.usr_information.usr_bio!!.trim())

                        // the other details
                        location_holder.setText(value.usr_information.usr_location!!.trim())
                        website_holder.setText(value.usr_information.usr_website.toString().trim())
                        birthdate_holder.setText(value.usr_information.usr_dob!!.trim())

                        val photo = value.usr_information.usr_cover.toString()
                        if (photo == null) {
                            binding.coverHolder.let {
                                Glide.with(this@InfoActivity).load(R.drawable.default_cover)
                                    .into(it)
                            }
                            binding.uploadCover.visibility = GONE
                        } else {
                            val options = RequestOptions()
                                .error(R.drawable.default_cover)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                            binding.uploadCover.visibility = VISIBLE
                            //using custom glide image loader to indicate progress in time
                            try {
                                GlideImageLoader(binding.coverHolder, binding.uploadCover).load(photo, options)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                binding.coverHolder.setImageResource(R.drawable.default_cover)
                            }
                        }

                        val pp = value.usr_information.usr_avatar.toString()
                        if (pp == null) {
                            binding.avatarHolder.let {
                                Glide.with(this@InfoActivity).load(R.drawable.default_cover)
                                    .into(it)
                            }
                            binding.uploadAvatar.visibility = GONE
                        } else {
                            val options = RequestOptions()
                                .error(R.drawable.default_cover)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                            binding.uploadAvatar.visibility = VISIBLE
                            //using custom glide image loader to indicate progress in time
                            try {
                                GlideImageLoader(binding.avatarHolder, binding.uploadAvatar).load(pp, options)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                binding.avatarHolder.setImageResource(R.drawable.default_cover)
                            }
                        }
                    } else {
                        // TODO: check
                    }
                } else {
                    toast("incomplete account")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("TAG", "Failed to read value.", error.toException())
            }
        })

        update_profile_holder.setOnClickListener {
            when {
                TextUtils.isEmpty(username_holder.text) -> {
                    binding.usernameLayout.error = "Please enter a username!!"
                }
                TextUtils.isEmpty(userhandle_holder.text) -> {
                    binding.userhandleLayout.error = "Please enter a userhandle!!"
                }
                TextUtils.isEmpty(bio_holder.text) -> {
                    binding.bioLayout.error = "Please provide a bio about you!!"
                }
                else -> {
                    // important holders
                    username_holder.text?.trim().toString()
                    bio_holder.text?.trim().toString()

                    userhandle_holder.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
                        source.toString().trim().toLowerCase(Locale.getDefault())
                    })

                    userhandle_holder.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                            // no op
                        }

                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            // no op
                        }

                        override fun afterTextChanged(s: Editable?) {
                            val input = s.toString().trim().toLowerCase(Locale.getDefault())
                            if (input != s.toString()) {
                                userhandle_holder.setText(input)
                                userhandle_holder.setSelection(input.length)
                            }
                        }
                    })

                    val handle_holder = userhandle_holder
                    val user_holder = userhandle_holder.text.toString()
                    if (!user_holder.contains("@")) {
                        // Add the "@" sign to the beginning of the text
                        handle_holder.setText("@$user_holder")
                        handle_holder.setSelection(1)
                    }
                    // the other details
                    location_holder.text?.trim().toString()
                    website_holder.text?.trim().toString()
                    birthdate_holder.text?.trim().toString()

                    uploadInitDataUpdate(
                        username_holder,
                        handle_holder,
                        bio_holder,
                        location_holder,
                        website_holder,
                        birthdate_holder
                    )
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun intialLocationTest() {
        // First, check if the user has granted location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Get the FusedLocationProviderClient instance
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            // Use the getLastLocation() method to get the current location of the user
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    // Use the Geocoder class to get the user's country and city
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses =
                        geocoder.getFromLocation(location.latitude, location.longitude, 1)

                    // Get the user's country and city from the address object
                    val country = addresses!![0].countryName
                    val city = addresses[0].locality

                    // Set the user's country and city in the EditText views
                    binding.locationHolder.setText("$country , $city").toString()
                }
                .addOnFailureListener { e ->
                    // Handle any errors that may occur while getting the user's location
                    Log.e(
                        getString(R.string.app_name),
                        "Error getting user's location: ${e.message}"
                    )
                }
        } else {
            // Request location permissions from the user
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun uploadInitDataUpdate(
        usernameHolder: TextInputEditText,
        userhandleHolder: TextInputEditText,
        bioHolder: TextInputEditText,
        locationHolder: TextInputEditText,
        websiteHolder: TextInputEditText,
        birthdateHolder: TextInputEditText
    ) {
        val mappingUpdateData: HashMap<String, Any> = hashMapOf(
            "usr_bio" to bioHolder.text.toString(),
            "usr_handle" to userhandleHolder.text.toString(),
            "usr_name" to usernameHolder.text.toString(),
            "usr_dob" to birthdateHolder.text.toString(),
            "usr_location" to locationHolder.text.toString(),
            "usr_website" to websiteHolder.text.toString(),
        )

        val user = FirebaseUtils.firebaseAuth.currentUser
        val uid = user!!.uid

        firebaseDatabase.child("users").child(user.uid).child("usr_information")
            .updateChildren(mappingUpdateData)
            .addOnCompleteListener {
                toast("successfully updated database")
                finish()
            }.addOnFailureListener {
                toast("${it.message} failed to update")
            }
    }

    private fun onImageSelected(eri: Uri) {
        val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser
        binding.uploadCover.visibility = VISIBLE
        val storageReference = firebaseStorage
            .child("galleries")
            .child(user!!.uid)
            .child("$user.uid-cover-photo")
        putImageInStorage(storageReference, eri)
    }

    private fun onImageSelectedAvatar(uri: Uri) {
        val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser
        binding.uploadCover.visibility = VISIBLE
        val storageReference = firebaseStorage
            .child("galleries")
            .child(user!!.uid)
            .child("$user!!.uid-profile-photo")
        putImageInStorageAvatar(storageReference, uri)
    }

    private fun putImageInStorageAvatar(storageReference: StorageReference, uri: Uri) {
        val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser

        storageReference.putFile(uri)
            .addOnSuccessListener(
                this@InfoActivity
            ) { taskSnapshot -> // After the image loads, get a public downloadUrl for the image
                // and add it to database
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { uri ->
                        val map: HashMap<String, Any> = HashMap()
                        map["usr_avatar"] = uri.toString()
                        firebaseDatabase.child("users").child(user!!.uid).child("usr_information")
                            .updateChildren(map)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    //get photo to show display updated photo
                                    getPhoto()
                                }
                            }
                    }
            }
    }


    private fun putImageInStorage(storageReference: StorageReference, eri: Uri) {
        val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser

        // First upload the image to Cloud Storage
        storageReference.putFile(eri)
            .addOnSuccessListener(
                this@InfoActivity
            ) { taskSnapshot -> // After the image loads, get a public downloadUrl for the image
                // and add it to database
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { urx ->
                        val map: HashMap<String, Any> = HashMap()
                        map["usr_cover"] = urx.toString()
                        firebaseDatabase.child("users").child(user!!.uid).child("usr_information")
                            .updateChildren(map)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    getPhoto()
                                }
                            }
                    }
            }
    }

    //get and load photo to view
    private fun getPhoto() {
        val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser

        firebaseDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val photo = snapshot.child("users").child(user!!.uid)
                    .getValue(users::class.java)?.usr_information!!.usr_cover
                if (photo == null) {
                    binding.coverHolder.let {
                        Glide.with(this@InfoActivity).load(R.drawable.default_cover)
                            .into(it)
                    }
                    binding.uploadCover.visibility = GONE
                } else {
                    val options = RequestOptions()
                        .error(R.drawable.default_cover)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                    binding.coverHolder.visibility = VISIBLE
                    //using custom glide image loader to indicate progress in time
                    try {
                        GlideImageLoader(binding.coverHolder, binding.uploadCover).load(photo, options)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        binding.coverHolder.setImageResource(R.drawable.default_cover)
                    }
                }

                val pp = snapshot.child("users").child(user!!.uid)
                    .getValue(users::class.java)?.usr_information!!.usr_avatar
                if (pp == null) {
                    binding.avatarHolder.let {
                        Glide.with(this@InfoActivity).load(R.drawable.default_cover)
                            .into(it)
                    }
                    binding.uploadAvatar.visibility = View.GONE
                } else {
                    val options = RequestOptions()
                        .error(R.drawable.default_cover)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                    binding.uploadAvatar.visibility = VISIBLE
                    //using custom glide image loader to indicate progress in time
                    try {
                        GlideImageLoader(binding.avatarHolder, binding.uploadAvatar).load(pp, options)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        binding.avatarHolder.setImageResource(R.drawable.default_cover)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // ToDo we should
            }

        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish() // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item)
    }
}