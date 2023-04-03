package moluccus.app.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.messaging.ktx.remoteMessage
import moluccus.app.R
import moluccus.app.glide.GlideImageLoader
import moluccus.app.route.Comments
import moluccus.app.route.CommitPost
import moluccus.app.route.Imagedecoderers
import moluccus.app.service.PostFirebaseMessagingService
import moluccus.app.ui.ProfileUserLayout
import moluccus.app.util.FirebaseUtils
import moluccus.app.util.FirebaseUtils.firebaseDatabase
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class FeedsAdapter(var context: Context, var commitPosts: MutableList<CommitPost>) :
    RecyclerView.Adapter<FeedsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.content_layout, parent, false)
        return ViewHolder(view)
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val username_holder: TextView = itemView.findViewById(R.id.username_holder)
        val timestamps: TextView = itemView.findViewById(R.id.timestamps)
        val content_message: TextView = itemView.findViewById(R.id.content_message)
        val like_count: TextView = itemView.findViewById(R.id.like_count)
        val comment_count: TextView = itemView.findViewById(R.id.comment_count)

        val comment_holder: ImageView = itemView.findViewById(R.id.comment_holder)

        val user_handle: TextView = itemView.findViewById(R.id.userhandle_holder)
        val layout_image_post: LinearLayout = itemView.findViewById(R.id.layout_image_post)
        val imagePosts: RecyclerView = itemView.findViewById(R.id.imagePosts)

        val avatarHolder: ShapeableImageView = itemView.findViewById(R.id.profile_holder)
        val photoProgressBar: CircularProgressIndicator =
            itemView.findViewById(R.id.photoProgressBar)
        val like_holder: ImageView = itemView.findViewById(R.id.like_holder)
    }

    @SuppressLint("CutPasteId", "SimpleDateFormat")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser
        var commitsBlogs = commitPosts[position]

        // Set the username and user handle for the commit post
        holder.username_holder.text = commitsBlogs.user_name.trim()
        holder.user_handle.text = commitsBlogs.user_handle.trim()
        val twitterBlue = Color.parseColor("#1DA1F2")
        val colorStateList = ColorStateList.valueOf(twitterBlue)
        holder.user_handle.setTextColor(colorStateList)

        // Set the number of likes and comments for the commit post
        holder.like_count.text = commitsBlogs.likesCount.toString()
        holder.comment_count.text = commitsBlogs.comments.size.toString()

        // Get a reference to the 'currentUserNotificationReciceved' map for the user in the Firebase database
        val postRef = firebaseDatabase.child("commits").child(commitsBlogs.id)
            .child("currentUserNotificationReciceved")
        // Check if the user has already received a notification for this post
        if (!commitsBlogs.currentUserNotificationReciceved.containsKey(user!!.uid)) {
            val fm = Firebase.messaging
            fm.send(remoteMessage("870256442359@fcm.googleapis.com") {
                messageId = messageId
                addData("current_uid", commitsBlogs.authorId)
                addData("user_profile", commitsBlogs.imageAvatarUrl)
                addData("user_handle", commitsBlogs.user_handle)
                addData("user_name", commitsBlogs.user_name)
                addData("message_content", "${commitsBlogs.user_handle} ${commitsBlogs.timeStamp}")

                postRef.updateChildren(mapOf(user.uid to true))
                PostFirebaseMessagingService()
            })
        } else {
            // If the user has already received a notification for this post, do nothing
            Log.i("TAG", "User has already received notification for post ${commitsBlogs.id}")
        }

        // Check if the current user has liked the blog post and set the "liked" status accordingly
        if (commitsBlogs.likedByCurrentUser.containsKey(user!!.uid)) {
            holder.like_holder.setImageResource(R.drawable.round_favorite)
        } else {
            holder.like_holder.setImageResource(R.drawable.round_like)
        }
        // Set the title, timestamp, and content message for the commit post
        holder.timestamps.text = commitsBlogs.timeStamp
        holder.content_message.text = commitsBlogs.content
        val imageList = mutableListOf<Imagedecoderers>()
        if (commitsBlogs.imageUrl != null){
            holder.layout_image_post.visibility = View.VISIBLE

            imageList.addAll(commitsBlogs.imageUrl!!.map { Imagedecoderers(it.toUri()) })

            val adapter = ImagePostAdapter(context, imageList)
            val recyclerView = holder.imagePosts
            val layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter
        } else {
            holder.layout_image_post.visibility = View.GONE
        }

        holder.comment_holder.setOnClickListener {
            MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                customView(R.layout.comments_layout)

                val current_user_name = findViewById<TextView>(R.id.userhandle_holder)
                val current_user_handle = findViewById<TextView>(R.id.userhandle_holder)
                val current_user_post_timestamp = findViewById<TextView>(R.id.timestamps)
                val current_user_avatar = findViewById<ImageView>(R.id.profile_holder)
                val current_user_progressindicator =
                    findViewById<CircularProgressIndicator>(R.id.photoProgressBar)

                val twitterBlue = Color.parseColor("#1DA1F2")
                val colorStateList = ColorStateList.valueOf(twitterBlue)
                current_user_handle.setTextColor(colorStateList)

                val recyclerViewCommenting = findViewById<RecyclerView>(R.id.commentsRecyclerView)
                val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)

                val recyclerViewCommitPosts = mutableListOf<Comments>()
                val adaptering = CommentListAdapter(context, recyclerViewCommitPosts)
                val layoutManager = LinearLayoutManager(context)
                recyclerViewCommenting.layoutManager = layoutManager
                recyclerViewCommenting.adapter = adaptering

                // Listen for changes in the Firebase Realtime Database
                firebaseDatabase.child("commits").child(commitsBlogs.id).child("comments")
                    .orderByKey()
                    .addValueEventListener(object : ValueEventListener {
                        @SuppressLint("NotifyDataSetChanged")
                        override fun onDataChange(snapshot: DataSnapshot) {
                            recyclerViewCommitPosts.clear()
                            for (postSnapshot in snapshot.children) {
                                val post = postSnapshot.getValue(Comments::class.java)
                                if (post != null) {
                                    recyclerViewCommitPosts.add(post)
                                }
                            }
                            // Notify the adapter that the data has changed
                            adaptering.notifyDataSetChanged()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // handle the error
                        }
                    })

                // Set up swipe to refresh
                swipeRefreshLayout.setOnRefreshListener {
                    // Refresh the data from the Firebase Realtime Database
                    firebaseDatabase.child("commits").child(commitsBlogs.id)
                        .child("comments")
                        .orderByKey()
                        .addValueEventListener(object : ValueEventListener {
                            @SuppressLint("NotifyDataSetChanged")
                            override fun onDataChange(snapshot: DataSnapshot) {
                                recyclerViewCommitPosts.clear()
                                for (postSnapshot in snapshot.children) {
                                    val post = postSnapshot.getValue(Comments::class.java)
                                    if (post != null) {
                                        recyclerViewCommitPosts.add(post)
                                    }
                                }
                                // Notify the adapter that the data has changed
                                adaptering.notifyDataSetChanged()
                                // Hide the refreshing indicator
                                swipeRefreshLayout.isRefreshing = false
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Handle the error
                                swipeRefreshLayout.isRefreshing = false
                            }
                        })
                }

                val commentEditText = findViewById<TextView>(R.id.commentEditText)
                val commentEditTextButton = findViewById<MaterialButton>(R.id.commentEditTextButton)

                current_user_name.text = commitsBlogs.user_name.trim()
                current_user_handle.text = commitsBlogs.user_handle.trim()
                current_user_post_timestamp.text = commitsBlogs.timeStamp.trim()

                var bindingUsername = ""
                var bindingUserHandle = ""
                var bindingUserAvatar = ""
                firebaseDatabase.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val value =
                                snapshot.child("users").child(user.uid).child("usr_information")
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

                val df: DateFormat = SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss")
                val date: String = df.format(Calendar.getInstance().time).toString()

                fun addComment(commitPost: CommitPost, comment: Comments) {
                    val postRef = firebaseDatabase.child("commits").child(commitsBlogs.id)
                    commitPost.comments[comment.id!!] = comment
                    commitPost.commentsCount += 1
                    commentEditTextButton.visibility = View.GONE
                    commentEditText.text = ""
                    val childUpdates: HashMap<String, Any> = hashMapOf(
                        "comments/${comment.id}" to comment,
                        "commentsCount" to commitPost.commentsCount
                    )
                    println("comment id value: $childUpdates")
                    postRef.updateChildren(childUpdates)
                }

                commentEditText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        if (p0.isNullOrEmpty()) {
                            commentEditTextButton.visibility = View.GONE
                        } else {
                            commentEditTextButton.visibility = View.VISIBLE
                        }
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        if (p0.isNullOrEmpty()) {
                            commentEditTextButton.visibility = View.GONE
                        } else {
                            commentEditTextButton.visibility = View.VISIBLE
                            val commenting = Comments(
                                id = firebaseDatabase.child("commits").push().key ?: "",
                                userId = user.uid,
                                usr_name = bindingUsername,
                                usr_handle = bindingUserHandle,
                                usr_avatar = bindingUserAvatar,
                                usr_reply_gif = "",
                                likesCount = 0,
                                reply_to_usr_handle = commitsBlogs.user_handle,
                                content_comment = p0.trim().toString(),
                                timestamp = date.trim(),
                                commentsCount = 0,
                            )

                            commentEditTextButton.setOnClickListener {
                                println("Edit comment ${p0.trim()}")
                                if (p0.isNullOrEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "please write a comment",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    addComment(commitsBlogs, commenting)
                                }
                            }
                        }
                    }

                    override fun afterTextChanged(p0: Editable?) {
                        if (p0.isNullOrEmpty()) {
                            commentEditTextButton.visibility = View.GONE
                        } else {
                            commentEditTextButton.visibility = View.VISIBLE
                        }
                    }

                })

                val commentpp = commitsBlogs.imageAvatarUrl
                if (commentpp == null) {
                    Toast.makeText(context, "empty", Toast.LENGTH_SHORT).show()
                } else {
                    val options = RequestOptions()
                        .error(R.drawable.default_cover)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                    holder.photoProgressBar.visibility = View.VISIBLE
                    try {
                        GlideImageLoader(current_user_avatar, current_user_progressindicator).load(commentpp, options)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        current_user_avatar.setImageResource(R.drawable.default_cover)
                    }
                }
            }
        }
        holder.like_holder.setOnClickListener {
            if (user.uid != null) {
                val postRef = firebaseDatabase.child("commits").child(commitsBlogs.id)
                val currentLikes = commitsBlogs.likedByCurrentUser
                if (currentLikes.containsKey(user.uid)) {
                    // User has already liked the post, so unlike it
                    val updatedLikes = currentLikes - user.uid
                    commitsBlogs = commitsBlogs.copy(
                        likedByCurrentUser = updatedLikes,
                        likesCount = commitsBlogs.likesCount - 1
                    )
                    holder.like_count.text = commitsBlogs.likesCount.toString()
                    holder.like_holder.setImageResource(R.drawable.round_like)
                    postRef.updateChildren(
                        mapOf(
                            "likedByCurrentUser" to updatedLikes,
                            "likesCount" to commitsBlogs.likesCount
                        )
                    )
                } else {
                    // User has not yet liked the post, so like it
                    val updatedLikes = currentLikes + (user.uid to true)
                    commitsBlogs = commitsBlogs.copy(
                        likedByCurrentUser = updatedLikes,
                        likesCount = commitsBlogs.likesCount + 1
                    )
                    holder.like_count.text = commitsBlogs.likesCount.toString()
                    holder.like_holder.setImageResource(R.drawable.round_favorite)
                    postRef.updateChildren(
                        mapOf(
                            "likedByCurrentUser" to updatedLikes,
                            "likesCount" to commitsBlogs.likesCount
                        )
                    )
                }
            }
        }

        holder.avatarHolder.setOnClickListener {
            if (commitsBlogs.authorId == user.uid) {
                // Navigate to ProfileLayout fragment
                it.findNavController().navigate(R.id.action_profile)
            } else {
                // Start ProfileUserLayout Activity
                context.startActivity(
                    Intent(
                        context,
                        ProfileUserLayout::class.java
                    ).putExtra("user_uid", commitsBlogs.authorId)
                        .putExtra("user_key", commitsBlogs.id)
                )
            }
        }
        // Set the user avatar image for the commit post using Glide library
        val photo = commitsBlogs.imageAvatarUrl
        if (photo == null) {
            Toast.makeText(context, "empty", Toast.LENGTH_SHORT).show()
        } else {
            val options = RequestOptions()
                .error(R.drawable.default_cover)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

            holder.photoProgressBar.visibility = View.VISIBLE
            try {
                GlideImageLoader(holder.avatarHolder, holder.photoProgressBar).load(photo, options)
            } catch (e: Exception) {
                e.printStackTrace()
                holder.avatarHolder.setImageResource(R.drawable.default_cover)
            }
        }
    }

    override fun getItemCount() = commitPosts.size
}