package moluccus.app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.CircularProgressIndicator
import moluccus.app.R
import moluccus.app.glide.GlideImageLoader
import moluccus.app.route.Imagedecoderers

class ImageFeedsAdapter(var context: Context, var comments: List<Imagedecoderers>) :
    RecyclerView.Adapter<ImageFeedsAdapter.CommentViewHolder>() {

    // The ViewHolder for each comment view
    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image_profile : ShapeableImageView = itemView.findViewById(R.id.image_profile)
        val photoProgressBar : CircularProgressIndicator = itemView.findViewById(R.id.photoProgressBar)
    }

    // Return the view type for the comment view
    override fun getItemViewType(position: Int): Int {
        return R.layout.image_post
    }

    // Inflate the comment view layout and create a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return CommentViewHolder(view)
    }

    // Return the number of comments
    override fun getItemCount(): Int {
        return comments.size
    }

    // Bind the comment data to the ViewHolder
    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val commentings = comments[position]

        // Load the image into the ImageView using GlideImageLoader
        val photo = commentings.imageUri
        if (photo == null) {
            // If the image URI is null, show a toast and set the default cover image
            Toast.makeText(context, "empty", Toast.LENGTH_SHORT).show()
            holder.image_profile.setImageResource(R.drawable.default_cover)
        } else {
            // If the image URI is not null, load the image using GlideImageLoader
            val options = RequestOptions()
                .error(R.drawable.default_cover)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

            holder.photoProgressBar.visibility = View.VISIBLE
            GlideImageLoader(holder.image_profile, holder.photoProgressBar).load(photo.toString(), options)
        }
    }
}