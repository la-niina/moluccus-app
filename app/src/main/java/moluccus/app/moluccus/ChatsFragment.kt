package moluccus.app.moluccus

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import moluccus.app.adapter.ChatListAdapter
import moluccus.app.databinding.FragmentChatBinding
import moluccus.app.route.ChatListAdapterData
import moluccus.app.route.Message
import moluccus.app.util.FirebaseUtils

class ChatsFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        val recyclerViewCommitPosts = mutableListOf<ChatListAdapterData>()
        val adapter = ChatListAdapter(requireContext(), recyclerViewCommitPosts)
        val recyclerView = binding.recyclerChatList
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        val databaseRef = FirebaseUtils.firebaseDatabase.child("messenger").orderByKey()
        databaseRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                recyclerViewCommitPosts.clear()
                for (chatSnapshot in snapshot.children.reversed()) {
                    for (messageSnapshot in chatSnapshot.child("messages").children) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        if (message != null && message.sent_to != currentUserUid) {
                            val chatListUid = ChatListAdapterData(
                                message.messages,
                                message.image_message,
                                message.video_message,
                                message.audio_message,
                                message.sender_uid,
                                message.datestamp,
                                message.timestamp,
                                message.sent_to
                            )

                            val index =
                                recyclerViewCommitPosts.indexOfFirst { it.otheruid == chatListUid.otheruid }
                            if (index == -1) { // if chatListUid does not exist in the list, add it at the beginning
                                recyclerViewCommitPosts.add(0, chatListUid)
                            } else { // if chatListUid already exists in the list, update it at the same position
                                recyclerViewCommitPosts[index] = chatListUid
                            }
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

}