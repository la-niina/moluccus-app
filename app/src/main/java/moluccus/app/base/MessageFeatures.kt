package moluccus.app.base

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import moluccus.app.R
import moluccus.app.adapter.MessagesAdapter
import moluccus.app.databinding.MessageFeatureBinding
import moluccus.app.mediaModels.RTCActivity
import moluccus.app.route.Message
import moluccus.app.util.Constract.isCallEnded
import moluccus.app.util.Constract.isIntiatedNow
import moluccus.app.util.Extensions.toast
import moluccus.app.util.FirebaseUtils
import moluccus.app.util.users
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class MessageFeatures : AppCompatActivity() {

    private lateinit var binding: MessageFeatureBinding
    private var other_user_uid = ""

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var adapter: MessagesAdapter
    private lateinit var messagesList: MutableList<Message>

    val currentUser: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser
    val db = Firebase.firestore

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MessageFeatureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        isIntiatedNow = true
        isCallEnded = true
        other_user_uid = intent.getStringExtra("user_uid").toString()
        FirebaseUtils.firebaseDatabase.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SimpleDateFormat")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val value =
                        snapshot.child("users").child(other_user_uid).getValue(users::class.java)
                    if (value != null) {
                        binding.toolbar.title = value.usr_information?.usr_name?.trim().toString()
                        binding.toolbar.subtitle = "typing a message..."
                    } else {
                        // Handle the case where the user data is missing or invalid
                        binding.toolbar.title = "Unknown User"
                        binding.toolbar.subtitle = ""
                    }
                } else {
                    // Handle the case where the data snapshot is incomplete or missing
                    binding.toolbar.title = "Error"
                    binding.toolbar.subtitle = "Data Snapshot Missing"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the case where the database read operation was cancelled or failed
                Log.w("TAG", "Failed to read value.", error.toException())
                binding.toolbar.title = "Error"
                binding.toolbar.subtitle = "Database Error"
            }
        })

        binding.contentBlogs.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // No action needed
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // Show or hide the "more features" button depending on the length of the text input
                binding.moreFeatures.visibility =
                    if ((p0?.length ?: 0) > 1) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(p0: Editable?) {
                // No action needed
            }
        })

        binding.sendMessagetoUser.setOnClickListener {
            val messageText = binding.contentBlogs.text?.toString()?.trim()
            if (!messageText.isNullOrEmpty()) {
                sendMessageFirebase(messageText)
            } else {
                // Handle the case where the message text is empty or null
                toast("Please enter a message")
            }
        }


        messagesList = mutableListOf()

        recyclerView = findViewById(R.id.recycler_view_messages)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        adapter = MessagesAdapter(messagesList, currentUser!!.uid, linearLayoutManager)
        recyclerView.layoutManager = linearLayoutManager
        linearLayoutManager.isSmoothScrollbarEnabled = true
        recyclerView.adapter = adapter

        loadMessagesFromFirebase()

        swipeRefreshLayout.setOnRefreshListener {
            messagesList.clear()
            loadMessagesFromFirebase()
        }
    }

    private fun loadMessagesFromFirebase() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val messagesRef = FirebaseUtils.firebaseDatabase.child("messenger")
        messagesRef.orderByKey().addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                val messagesList = mutableListOf<Message>()
                for (chatSnapshot in snapshot.children) {
                    for (messageSnapshot in chatSnapshot.child("messages").children.reversed()) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        if ((message != null) &&
                            (((message.sender_uid == currentUserUid) && chatSnapshot.key!!.contains(
                                other_user_uid
                            )) ||
                                    ((message.sender_uid == other_user_uid) && chatSnapshot.key!!.contains(
                                        currentUserUid!!
                                    )))
                        ) {
                            messagesList.add(message)
                        }
                    }
                }

                swipeRefreshLayout.isRefreshing = false
                adapter.messagesList = messagesList
                adapter.notifyDataSetChanged()
            }


            override fun onCancelled(error: DatabaseError) {
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(
                    this@MessageFeatures,
                    "Failed to load messages",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    @SuppressLint("SimpleDateFormat")
    private fun sendMessageFirebase(msg: String) {
        val dates_only: DateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val dateonly: String = dates_only.format(Calendar.getInstance().time).toString()

        val time_only: DateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeonly: String = time_only.format(Calendar.getInstance().time).toString()

        val currentUser: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser
        val chatKey = "${currentUser?.uid}_${other_user_uid}"

        // Get a reference to the messages node for the chat between the current user and the other user
        val chatRef = FirebaseUtils.firebaseDatabase.child("messenger").child(chatKey)
        val messagesRef = chatRef.child("messages")

        // Check if chat with the current user and other user exists
        chatRef.orderByChild("participants_uid/sender_uid")
            .equalTo(currentUser?.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var chatKeys = ""
                    snapshot.children.forEach { chatSnapshot ->
                        val receiverUid =
                            chatSnapshot.child("participants_uid/receiver_uid").value.toString()
                        if (receiverUid == other_user_uid) {
                            chatKeys = chatSnapshot.key.toString()
                        }
                    }

                    // If chat exists, add the message to the existing chat
                    if (chatKey.isNotEmpty()) {
                        // Create a message object
                        val newMessage = Message(
                            datestamp = dateonly,
                            messages = msg,
                            audio_message = "",
                            image_message = "",
                            video_message = "",
                            sent_to = other_user_uid,
                            sender_uid = currentUser?.uid ?: "",
                            timestamp = timeonly
                        )

                        // Append the new message to the existing messages list in Firebase
                        val messageKey = messagesRef.push().key!!
                        val childUpdates = HashMap<String, Any>()
                        childUpdates["/messages/$messageKey"] = newMessage
                        childUpdates["/participants_uid/${currentUser?.uid}"] = true // add current user to participants list
                        childUpdates["/participants_uid/${other_user_uid}"] = true // add current user to participants list

                        // Update the chat with the new message
                        chatRef.updateChildren(childUpdates)

                        binding.contentBlogs.text?.clear()
                        binding.moreFeatures.visibility = View.GONE
                    } else {
                        // Chat doesn't exist, create new chat with participants and message
                        val newMessages: HashMap<String, Any> = hashMapOf(
                            "datestamp" to dateonly,
                            "messages" to msg,
                            "audio_message" to "",
                            "image_message" to "",
                            "video_message" to "",
                            "sent_to" to other_user_uid,
                            "sender_uid" to currentUser!!.uid,
                            "timestamp" to timeonly
                        )

                        val messageKey = messagesRef.push().key!!
                        val newChat: HashMap<String, Any> = hashMapOf(
                            "participants_uid" to hashMapOf(
                                "sender_uid" to currentUser.uid,
                                "receiver_uid" to other_user_uid,
                                other_user_uid to true,
                                currentUser.uid to true
                            ),
                            "messages" to hashMapOf(
                                messageKey to newMessages
                            )
                        )

                        binding.contentBlogs.text?.clear()
                        binding.moreFeatures.visibility = View.GONE

                        // Generate a new chatKey and set newChat hashmap to messenger node in Firebase database
                        chatRef.setValue(newChat)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chats_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.chat_video_call -> {
                // Handle menu item 1 click
                true
            }
            R.id.chat_voice_call -> {
                intitialVoiceCall()
                true
            }
            R.id.app_bar_search -> {
                // Handle menu item 2 click
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun intitialVoiceCall() {
        val intent = Intent(this, RTCActivity::class.java)
        intent.putExtra("meetingID", other_user_uid)
        intent.putExtra("isJoin", false)
        startActivity(intent)
    }
}


/**
 *  messagesRef.addChildEventListener(object : ChildEventListener {
@SuppressLint("NotifyDataSetChanged")
override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
val message = snapshot.getValue(Message::class.java)
if (message != null) {
messagesList.add(message)
adapter.notifyDataSetChanged()

binding.contentBlogs.text?.clear()
binding.moreFeatures.visibility = View.GONE
loadMessagesFromFirebase()
}
}

override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
// Handle changed messages
}

override fun onChildRemoved(snapshot: DataSnapshot) {
// Handle deleted messages
}

override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
// Handle moved messages
}

override fun onCancelled(error: DatabaseError) {
// Handle errors
}
})
 */
