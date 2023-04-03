package moluccus.app.route

import com.google.errorprone.annotations.Keep
import com.google.firebase.database.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class CommitPost(
    val id: String = "",
    val content: String = "",
    val imageAvatarUrl: String= "",
    val user_handle: String = "",
    val user_name: String = "",
    val imageUrl: List<String>? = null,
    val videoUrl: String = "",
    val authorId: String = "",
    var timeStamp: String = "",
    var likesCount: Int = 0,
    var commentsCount: Int = 0,
    var currentUserNotificationReciceved: HashMap<String, Any> = hashMapOf(),
    val comments: HashMap<String, Comments> = hashMapOf(),
    var likedByCurrentUser: Map<String, Boolean> = emptyMap()
)