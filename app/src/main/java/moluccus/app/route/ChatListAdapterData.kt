package moluccus.app.route

data class ChatListAdapterData(
    val messages: String = "",
    val image_message: String = "",
    val video_message: String = "",
    val audio_message: String = "",
    val sender_uid: String = "",
    val datestamp: String = "",
    val timestamp: String = "",
    val otheruid: String = "",
)