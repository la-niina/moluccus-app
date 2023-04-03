package moluccus.app.route

data class Message(
    val messages: String = "",
    val image_message: String = "",
    val video_message: String = "",
    val audio_message: String = "",
    val sender_uid: String = "",
    val sent_to: String = "",
    val datestamp: String = "",
    val timestamp: String = ""
)