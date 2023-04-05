package moluccus.app.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import moluccus.app.R
import moluccus.app.base.MessageFeatures
import moluccus.app.glide.GlideImageLoader
import moluccus.app.route.ChatListAdapterData
import moluccus.app.ui.ProfileUserLayout
import moluccus.app.util.FirebaseUtils
import moluccus.app.util.users

class ChatListAdapter(var context: Context, var chatList: MutableList<ChatListAdapterData>) :
    RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        // chatListLayout when clicked will send user with intent extra of other users ui to chat messages ui
        val chatListLayout: MaterialCardView = itemView.findViewById(R.id.chatListLayout)

        val photoProgressBar: CircularProgressIndicator =
            itemView.findViewById(R.id.photoProgressBar)
        val chatUsersProfile: ShapeableImageView = itemView.findViewById(R.id.chatUsersProfile)

        val userNameHolder: TextView = itemView.findViewById(R.id.userNameHolder)
        val usersHandle: TextView = itemView.findViewById(R.id.usersHandle)
        val online_status: TextView = itemView.findViewById(R.id.online_status)

        // will show a a length of 13 texts of the current message in the chat room
        val currentMessageInChat: TextView = itemView.findViewById(R.id.currentMessageInChat)

        // time stamp of the current message in the chat room
        val timestamps: TextView = itemView.findViewById(R.id.timestamps)

        // will show number of unread messages in the chat room or else if no messages are available hide the view
        val unreadMessages: Chip = itemView.findViewById(R.id.unreadMessages)

        // all above data will be retrieved by using the other users uid and displaying there information under chat list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.message_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser
        val chatListUsers = chatList[position]

        FirebaseUtils.firebaseDatabase.child("users").child(chatListUsers.otheruid)
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userData = snapshot.getValue(users::class.java)
                    holder.userNameHolder.text = userData?.usr_information?.usr_name?.trim() ?: ""
                    holder.usersHandle.text = userData?.usr_information?.usr_handle?.trim() ?: ""

                    holder.currentMessageInChat.text = chatListUsers.messages
                    holder.timestamps.text = chatListUsers.timestamp

                    println("Current : ${chatListUsers.timestamp}")

                    holder.chatListLayout.setOnClickListener {
                        if (chatListUsers.otheruid == user!!.uid) {
                            // Navigate to ProfileLayout fragment
                            it.findNavController().navigate(R.id.action_profile)
                        } else {
                            // Start ProfileUserLayout Activity
                            context.startActivity(
                                Intent(context, MessageFeatures::class.java)
                                    .putExtra("user_uid", chatListUsers.otheruid)
                            )
                        }
                    }

                    val commentpp = userData?.usr_information?.usr_avatar.toString()
                    if (commentpp != null) {
                        val options = RequestOptions()
                            .error(R.drawable.default_cover)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                        holder.photoProgressBar.visibility = View.VISIBLE
                        try {
                            GlideImageLoader(holder.chatUsersProfile, holder.photoProgressBar).load(
                                commentpp,
                                options
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            holder.chatUsersProfile.setImageResource(R.drawable.default_cover)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}