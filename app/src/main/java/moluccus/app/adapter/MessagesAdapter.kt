package moluccus.app.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import moluccus.app.R
import moluccus.app.glide.GlideImageLoader
import moluccus.app.route.Message
import moluccus.app.util.FirebaseUtils


class MessagesAdapter(
    var messagesList: MutableList<Message>,
    private val currentUserUid: String,
    private val layoutManager: LinearLayoutManager
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_MESSAGE_SENT = 1
    private val VIEW_TYPE_MESSAGE_RECEIVED = 2

    private val visibleThreshold = 5
    private var loading = true

    fun setLoaded() {
        loading = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        return if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messagesList[position]

        when (holder.itemViewType) {
            VIEW_TYPE_MESSAGE_SENT -> {
                (holder as SentMessageViewHolder).bind(message)
            }
            VIEW_TYPE_MESSAGE_RECEIVED -> {
                (holder as ReceivedMessageViewHolder).bind(message)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = messagesList[position]
        return if (message.sender_uid == currentUserUid) {
            VIEW_TYPE_MESSAGE_SENT
        } else {
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    override fun getItemCount(): Int {
        return messagesList.size
    }

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.current_user_chat)
        private val timeText: TextView = itemView.findViewById(R.id.timestampViewReception)

        private val imageLayout: MaterialCardView = itemView.findViewById(R.id.imageLayout)
        private val image_holder: ShapeableImageView = itemView.findViewById(R.id.image_holder)
        private val photoProgressBar: CircularProgressIndicator = itemView.findViewById(R.id.photoProgressBar)

        fun bind(message: Message) {
            messageText.text = message.messages
            timeText.text = message.timestamp
            val imageSent = message.image_message
            if (imageSent.isNullOrEmpty()) {
                imageLayout.visibility = View.GONE
                image_holder.visibility = View.GONE
            } else {
                val options = RequestOptions()
                    .error(R.drawable.default_cover)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                imageLayout.visibility = View.VISIBLE
                image_holder.visibility = View.VISIBLE // layout
                //using custom glide image loader to indicate progress in time
                try {
                    GlideImageLoader(image_holder, photoProgressBar).load(imageSent, options)
                } catch (e: Exception) {
                    e.printStackTrace()
                    image_holder.setImageResource(R.drawable.default_cover)
                }
            }
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.other_user_chat)
        private val timeText: TextView = itemView.findViewById(R.id.timestampViewedReception)

        private val imageLayout: MaterialCardView = itemView.findViewById(R.id.imageLayout)
        private val image_holder: ShapeableImageView = itemView.findViewById(R.id.image_holder)
        private val photoProgressBar: CircularProgressIndicator = itemView.findViewById(R.id.photoProgressBar)

        fun bind(message: Message) {
            messageText.text = message.messages
            timeText.text = message.timestamp
            val imageSent = message.image_message
            if (imageSent.isNullOrEmpty()) {
                imageLayout.visibility = View.GONE
                image_holder.visibility = View.GONE
            } else {
                val options = RequestOptions()
                    .error(R.drawable.default_cover)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                imageLayout.visibility = View.VISIBLE
                image_holder.visibility = View.VISIBLE // layout
                //using custom glide image loader to indicate progress in time
                try {
                    GlideImageLoader(image_holder, photoProgressBar).load(imageSent, options)
                } catch (e: Exception) {
                    e.printStackTrace()
                    image_holder.setImageResource(R.drawable.default_cover)
                }
            }
        }
    }
}