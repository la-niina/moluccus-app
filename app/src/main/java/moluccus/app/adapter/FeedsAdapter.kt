package moluccus.app.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
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
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import moluccus.app.R
import moluccus.app.glide.GlideImageLoader
import moluccus.app.route.Comments
import moluccus.app.route.CommitPost
import moluccus.app.route.Imagedecoderers
import moluccus.app.ui.ProfileUserLayout
import moluccus.app.util.Constract
import moluccus.app.util.FirebaseUtils
import moluccus.app.util.FirebaseUtils.firebaseDatabase
import moluccus.app.util.users
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

        val exoPlayerView: PlayerView = itemView.findViewById(R.id.exo_player_view)
        val videoLayout: LinearLayout = itemView.findViewById(R.id.videoLayout)


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
        firebaseDatabase.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SimpleDateFormat")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val value =
                        snapshot.child("users").child(commitsBlogs.authorId).getValue(users::class.java)?.usr_information
                    if (value != null) {
                        holder.username_holder.text = value.usr_name?.trim().toString()
                        holder.user_handle.text = value.usr_handle?.trim().toString()
                        val twitterBlue = Color.parseColor("#1DA1F2")
                        val colorStateList = ColorStateList.valueOf(twitterBlue)
                        holder.user_handle.setTextColor(colorStateList)

                        // Set the user avatar image for the commit post using Glide library
                        val photo = value.usr_avatar
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
                    } else {
                        // Handle the case where the user data is missing or invalid
                    }
                } else {
                    // Handle the case where the data snapshot is incomplete or missing
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Handle the case where the database read operation was cancelled or failed
            }
        })

        // Set the number of likes and comments for the commit post
        holder.like_count.text = commitsBlogs.likesCount.toString()
        holder.comment_count.text = commitsBlogs.comments.size.toString()

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
            val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter
        } else {
            holder.layout_image_post.visibility = View.GONE
        }

      /**  if (commitsBlogs.videoUrl != null && commitsBlogs.imageUrl == null) {
            holder.videoLayout.visibility = View.VISIBLE

            holder.videoLayout.setOnClickListener {
                val player = holder.exoPlayerView.player ?: SimpleExoPlayer.Builder(context).build()
                holder.exoPlayerView.player = player

                val mediaItem = MediaItem.fromUri(Uri.parse(commitsBlogs.videoUrl))
                val mediaList = listOf(mediaItem)

                player.setMediaItems(mediaList)
                player.prepare()
                player.playWhenReady = true
            }

            // Hide the video layout until the user clicks to play the video
            holder.videoLayout.visibility = View.VISIBLE
            holder.videoLayout.layoutParams.height = Constract.dpToPx(context, 250)
        } else {
            holder.videoLayout.visibility = View.GONE
        } **/

        holder.comment_holder.setOnClickListener {
            MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                customView(R.layout.comments_layout)

                val current_user_name = findViewById<TextView>(R.id.username_holder)
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

                firebaseDatabase.addValueEventListener(object : ValueEventListener {
                    @SuppressLint("SimpleDateFormat")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val value =
                                snapshot.child("users").child(commitsBlogs.authorId).getValue(users::class.java)?.usr_information
                            if (value != null) {
                                current_user_name.text = value.usr_name?.trim().toString()
                                current_user_handle.text = value.usr_handle?.trim().toString()
                                current_user_post_timestamp.text = commitsBlogs.timeStamp.trim()

                                val commentpp = value.usr_avatar
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
                            } else {
                                // Handle the case where the user data is missing or invalid
                            }
                        } else {
                            // Handle the case where the data snapshot is incomplete or missing
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        // Handle the case where the database read operation was cancelled or failed
                    }
                })

                val df: DateFormat = SimpleDateFormat("d MMM yyyy, HH:mm")
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
                                commitId= commitsBlogs.id,
                                userId = user.uid,
                                usr_reply_gif = "",
                                likesCount = 0,
                                authorId = commitsBlogs.authorId,
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
    }
    override fun getItemCount() = commitPosts.size
}