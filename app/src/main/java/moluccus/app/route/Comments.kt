package moluccus.app.route

import com.google.errorprone.annotations.Keep
import com.google.firebase.database.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class Comments(
    val id: String? = null,
    val userId: String? = null,
    val usr_name: String? = null,
    val usr_handle: String? = null,
    val usr_avatar: String? = null,
    val usr_reply_gif: String? = null,
    var likesCount: Int? = 0,
    val reply_to_usr_handle: String? = null,
    val content_comment: String? = null,
    val timestamp: String? = null,
    val commentsCount: Int? = 0,
    val comments: HashMap<String, Comments> = hashMapOf(),
    var likedByCurrentUser: Map<String, Boolean> = emptyMap()
)