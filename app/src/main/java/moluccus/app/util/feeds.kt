package moluccus.app.util

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class feeds(
    val user_name: String? = null,
    val user_handle: String? = null,
    val timestamp: String? = null,

    val user_avatar: String? = null,
    val user_id: String? = null,
    val content : String? = null,
)