package moluccus.app.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import moluccus.app.R
import moluccus.app.glide.GlideImageLoader
import moluccus.app.route.Comments
import moluccus.app.util.FirebaseUtils
import moluccus.app.util.users

class CommentListAdapter(var context: Context, var comments: List<Comments>) :
    RecyclerView.Adapter<CommentListAdapter.CommentViewHolder>() {
    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoProgressBar: CircularProgressIndicator =
            itemView.findViewById(R.id.photoProgressBar)
        val imageProfileComments: ShapeableImageView =
            itemView.findViewById(R.id.imageProfileComments)

        val usernameComments: TextView = itemView.findViewById(R.id.usernameComments)
        val commentUserHandle: TextView = itemView.findViewById(R.id.commentUserHandle)
        val commentContent: TextView = itemView.findViewById(R.id.commentContent)

        val commentLikedCount: TextView = itemView.findViewById(R.id.commentLikedCount)
        val commentCommentCount: TextView = itemView.findViewById(R.id.commentCommentCount)

        val likeCountButton: ImageView = itemView.findViewById(R.id.likeCountButton)

        val replyingToUserHandle: TextView = itemView.findViewById(R.id.replyingToUserHandle)
        val statusTimeStamp: TextView = itemView.findViewById(R.id.statusTimeStamp)
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.comment_layout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return CommentViewHolder(view)
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser
        var commentings = comments[position]

        holder.commentContent.text = commentings.content_comment?.trim().toString()
        holder.statusTimeStamp.text = commentings.timestamp?.trim().toString()

        holder.commentLikedCount.text = commentings.likesCount?.toString()
        holder.commentCommentCount.text = commentings.commentsCount?.toString()

        // Check if the current user has liked the blog post and set the "liked" status accordingly
        if (commentings.likedByCurrentUser.containsKey(user!!.uid)) {
            holder.likeCountButton.setImageResource(R.drawable.round_favorite)
        } else {
            holder.likeCountButton.setImageResource(R.drawable.round_like)
        }

        println("current user : ${commentings.id}")
        holder.likeCountButton.setOnClickListener {
            if (user!!.uid != null) {
                val postRef = FirebaseUtils.firebaseDatabase.child("commits").child(commentings.commitId!!)
                    .child("comments/${commentings.id!!}")
                val currentLikes = commentings.likedByCurrentUser
                if (currentLikes.containsKey(user.uid)) {
                    // User has already liked the post, so unlike it
                    val updatedLikes = currentLikes - user.uid
                    commentings = commentings.copy(
                        likedByCurrentUser = updatedLikes,
                        likesCount = commentings.likesCount?.minus(1)
                    )
                    holder.commentLikedCount.text = commentings.likesCount.toString()
                    holder.likeCountButton.setImageResource(R.drawable.round_like)
                    postRef.updateChildren(
                        mapOf(
                            "likedByCurrentUser" to updatedLikes,
                            "likesCount" to commentings.likesCount
                        )
                    )
                } else {
                    // User has not yet liked the post, so like it
                    val updatedLikes = currentLikes + (user.uid to true)
                    commentings = commentings.copy(
                        likedByCurrentUser = updatedLikes,
                        likesCount = commentings.likesCount?.plus(1)
                    )
                    holder.commentLikedCount.text = commentings.likesCount.toString()
                    holder.likeCountButton.setImageResource(R.drawable.round_favorite)
                    postRef.updateChildren(
                        mapOf(
                            "likedByCurrentUser" to updatedLikes,
                            "likesCount" to commentings.likesCount
                        )
                    )
                }
            }
        }


        FirebaseUtils.firebaseDatabase.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SimpleDateFormat")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val value =
                        snapshot.child("users").child(commentings.authorId!!)
                            .getValue(users::class.java)?.usr_information
                    if (value != null) {
                        holder.replyingToUserHandle.text = value.usr_handle?.trim().toString()
                        val twitterBlue = Color.parseColor("#1DA1F2")
                        val colorStateList = ColorStateList.valueOf(twitterBlue)
                        holder.commentUserHandle.setTextColor(colorStateList)

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

        FirebaseUtils.firebaseDatabase.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SimpleDateFormat")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val value =
                        snapshot.child("users").child(commentings.userId!!)
                            .getValue(users::class.java)?.usr_information
                    if (value != null) {
                        holder.usernameComments.text = value.usr_name?.trim().toString()
                        holder.commentUserHandle.text = value.usr_handle?.trim().toString()
                        val twitterBlue = Color.parseColor("#1DA1F2")
                        val colorStateList = ColorStateList.valueOf(twitterBlue)
                        holder.commentUserHandle.setTextColor(colorStateList)

                        val photo = value.usr_avatar
                        if (photo == null) {
                            Toast.makeText(context, "empty", Toast.LENGTH_SHORT).show()
                        } else {
                            val options = RequestOptions()
                                .error(R.drawable.default_cover)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                            holder.photoProgressBar.visibility = View.VISIBLE
                            try {
                                GlideImageLoader(
                                    holder.imageProfileComments,
                                    holder.photoProgressBar
                                ).load(photo, options)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                holder.imageProfileComments.setImageResource(R.drawable.default_cover)
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
    }
}

