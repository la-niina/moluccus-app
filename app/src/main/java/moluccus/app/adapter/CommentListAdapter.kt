package moluccus.app.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.CircularProgressIndicator
import moluccus.app.R
import moluccus.app.glide.GlideImageLoader
import moluccus.app.route.Comments

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
        val commentings = comments[position]

        holder.usernameComments.text = commentings.usr_name?.trim()
        holder.commentUserHandle.text = commentings.usr_handle?.trim()
        val twitterBlue = Color.parseColor("#1DA1F2")
        val colorStateList = ColorStateList.valueOf(twitterBlue)
        holder.commentUserHandle.setTextColor(colorStateList)

        holder.commentContent.text = commentings.content_comment?.trim()
        holder.replyingToUserHandle.text = commentings.reply_to_usr_handle?.trim()
        holder.statusTimeStamp.text = commentings.timestamp?.trim()

        val photo = commentings.usr_avatar
        if (photo == null) {
            Toast.makeText(context, "empty", Toast.LENGTH_SHORT).show()
        } else {
            val options = RequestOptions()
                .error(R.drawable.default_cover)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

            holder.photoProgressBar.visibility = View.VISIBLE
            try {
                GlideImageLoader(holder.imageProfileComments, holder.photoProgressBar).load(photo, options)
            } catch (e: Exception) {
                e.printStackTrace()
                holder.imageProfileComments.setImageResource(R.drawable.default_cover)
            }
        }
    }
}

