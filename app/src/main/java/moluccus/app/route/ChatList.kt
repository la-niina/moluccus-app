package moluccus.app.route

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ChatList(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: String = "",
    val id: String = ""
)
