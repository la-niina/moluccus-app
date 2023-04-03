package moluccus.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import moluccus.app.R
import moluccus.app.adapter.FeedsAdapter
import moluccus.app.base.MessageFeatures
import moluccus.app.databinding.ProfileUserLayoutBinding
import moluccus.app.glide.GlideImageLoader
import moluccus.app.route.CommitPost
import moluccus.app.util.Extensions.toast
import moluccus.app.util.FirebaseUtils
import moluccus.app.util.FirebaseUtils.firebaseDatabase
import moluccus.app.util.users
import java.text.SimpleDateFormat
import java.util.*

class ProfileUserLayout : AppCompatActivity() {
    private lateinit var binding: ProfileUserLayoutBinding

    private var user_uid = ""
    private var user_id = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProfileUserLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }
        initLayout()
    }

    @SuppressLint("SetTextI18n")
    private fun initLayout() {
        user_uid = intent.getStringExtra("user_uid").toString()
        user_id = intent.getStringExtra("user_key").toString()

        val wallpaper_cover = binding.wallpaperCover
        val photoProgressBar = binding.photoProgressBar

        val avatar_holder = binding.avatarHolder
        val upload_avatar = binding.uploadAvatar

        val username_view = binding.usernameView
        val userhandle_view = binding.userhandleView

        val follow_button = binding.followButton
        follow_button.text = "Following"
        val bio_view = binding.bioView
        val associated_holder = binding.associatedHolder

        val date_joined = binding.dateJoined
        val location_view = binding.locationView
        val website_view = binding.websiteView
        val email_profile = binding.emailProfile
        val followingHolder = binding.followingHolder
        val followersHolder = binding.followersHolder

        val UserPostRecyclerView = binding.UserPostRecyclerView

        initRecyclerView()

        firebaseDatabase.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SimpleDateFormat")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val value =
                        snapshot.child("users").child(user_uid).getValue(
                            users::class.java
                        )
                    if (value != null) {
                        binding.toolbar.title = value.usr_information?.usr_name?.trim().toString()
                        username_view.text = value.usr_information?.usr_name?.trim().toString()
                        userhandle_view.text = value.usr_information?.usr_handle?.trim().toString()
                        val twitterBlue = Color.parseColor("#1DA1F2")
                        val colorStateList = ColorStateList.valueOf(twitterBlue)
                        userhandle_view.setTextColor(colorStateList)

                        date_joined.text = value.create_at?.trim().toString()
                        location_view.text = value.usr_information?.usr_location?.trim().toString()
                        website_view.text = value.usr_information?.usr_website?.trim().toString()

                        bio_view.text = value.usr_information?.usr_bio?.trim().toString()

                        email_profile.text = value.email_address?.trim().toString()
                        followingHolder.text =
                            value.usr_impression?.following_count?.toString()
                        followersHolder.text =
                            value.usr_impression?.followers_count?.toString()

                        val photo = value.usr_information?.usr_avatar?.trim().toString()
                        if (photo == null) {
                            Toast.makeText(this@ProfileUserLayout, "empty", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            val options = RequestOptions()
                                .error(R.drawable.default_cover)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                            initLayoutFollowButton(value)
                            upload_avatar.visibility = View.VISIBLE
                            try {
                                GlideImageLoader(avatar_holder, upload_avatar).load(photo, options)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                avatar_holder.setImageResource(R.drawable.default_cover)
                            }
                        }

                        initLayoutFollowButton(value)

                        val pp = value.usr_information?.usr_cover?.trim().toString()
                        if (pp == null) {
                            Toast.makeText(this@ProfileUserLayout, "empty", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            val options = RequestOptions()
                                .error(R.drawable.default_cover)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                            initLayoutFollowButton(value)
                            photoProgressBar.visibility = View.VISIBLE
                            try {
                                GlideImageLoader(wallpaper_cover, photoProgressBar).load(pp, options)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                wallpaper_cover.setImageResource(R.drawable.default_cover)
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
    }

    @SuppressLint("SetTextI18n")
    private fun initLayoutFollowButton(value: users) {
        val userUid = intent.getStringExtra("user_uid").toString()
        val currentUser : FirebaseUser?= FirebaseUtils.firebaseAuth.currentUser
        val currentUserUid = currentUser?.uid ?: ""

        val otherUserFollowersRef = firebaseDatabase.child("users").child(userUid).child("usr_impression").child("followers")
        val currentUserFollowingsRef = firebaseDatabase.child("users").child(currentUserUid).child("usr_impression").child("followings")

        otherUserFollowersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(otherUserFollowersSnapshot: DataSnapshot) {
                val otherUserFollowers = otherUserFollowersSnapshot.value as? Map<String, String> ?: emptyMap()
                val otherUserFollowersCount = otherUserFollowers.size

                val otherUserFollowedByCurrentUser = otherUserFollowers.containsKey(currentUserUid)
                if (otherUserFollowedByCurrentUser) {
                    binding.followButton.text = "Following"
                    binding.messageFollongUser.visibility = View.VISIBLE
                    binding.messageFollongUser.setOnClickListener {
                        startActivity(Intent(this@ProfileUserLayout, MessageFeatures::class.java)
                            .putExtra("user_uid", user_uid))
                    }
                } else {
                    binding.followButton.text = "Follow"
                    binding.messageFollongUser.visibility = View.GONE
                }

                currentUserFollowingsRef.addValueEventListener(object : ValueEventListener {
                    @SuppressLint("SimpleDateFormat")
                    override fun onDataChange(currentUserFollowingsSnapshot: DataSnapshot) {
                        val currentUserFollowings = currentUserFollowingsSnapshot.value as? Map<String, String> ?: emptyMap()
                        val currentUserFollowingCount = currentUserFollowings.size

                        binding.followButton.setOnClickListener {
                            val postRef = firebaseDatabase.child("users").child(user_uid).child("usr_impression")
                            val currentPostRef = firebaseDatabase.child("users").child(currentUserUid).child("usr_impression")

                            if (otherUserFollowedByCurrentUser) {
                                // Unfollow the user
                                val newOtherUserFollowers = otherUserFollowers.filterKeys { it != currentUserUid }
                                val newCurrentUserFollowings = currentUserFollowings.filterKeys { it != user_uid }

                                postRef.updateChildren(
                                    mapOf(
                                        "followers" to newOtherUserFollowers,
                                        "followers_count" to newOtherUserFollowers.size
                                    )
                                )
                                currentPostRef.updateChildren(
                                    mapOf(
                                        "followings" to newCurrentUserFollowings,
                                        "following_count" to newCurrentUserFollowings.size
                                    )
                                )
                            } else {
                                // Follow the user
                                val currentDate = SimpleDateFormat("d MMM yyyy, HH:mm").format(Calendar.getInstance().time)
                                val newOtherUserFollowers = otherUserFollowers.plus(currentUserUid to currentDate)
                                val newCurrentUserFollowings = currentUserFollowings.plus(user_uid to currentDate)

                                newOtherUserFollowers.let {
                                    postRef.updateChildren(
                                        mapOf(
                                            "followers" to it,
                                            "followers_count" to it.size
                                        )
                                    )
                                }

                                newCurrentUserFollowings.let {
                                    currentPostRef.updateChildren(
                                        mapOf(
                                            "followings" to it,
                                            "following_count" to it.size
                                        )
                                    )
                                }
                            }
                        }

                            binding.followersHolder.text = otherUserFollowersCount.toString()
                        binding.followingHolder.text = currentUserFollowingCount.toString()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("TAG", "Failed to read user data", error.toException())
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TAG", "Failed to read user data", error.toException())
            }
        })
    }

    private fun initRecyclerView() {
        val recyclerViewCommitPosts = mutableListOf<CommitPost>()
        val adapter = FeedsAdapter(this, recyclerViewCommitPosts)
        val recyclerView = binding.UserPostRecyclerView
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser

        // Listen for changes in the Firebase Realtime Database
        firebaseDatabase.child("commits").orderByKey()
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(snapshot: DataSnapshot) {
                    recyclerViewCommitPosts.clear()
                    for (postSnapshot in snapshot.children) {
                        val post = postSnapshot.getValue(CommitPost::class.java)
                        if (post?.authorId == user_uid) {
                            post.let {
                                recyclerViewCommitPosts.add(it)
                            }
                        }
                    }
                    // Notify the adapter that the data has changed
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle the error
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