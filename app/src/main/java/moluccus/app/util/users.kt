package moluccus.app.util

import com.google.errorprone.annotations.Keep
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import moluccus.app.route.Comments
import moluccus.app.route.FollowsData

@Keep
@IgnoreExtraProperties
data class users(
    val email_address: String? = null,
    val password_holder: String? = null,
    val create_at: String? = null,

    val usr_information: usr_informationData? = null,
    val usr_impression: usr_impressionData? = null,
)

@Keep
@IgnoreExtraProperties
data class usr_informationData(
    val usr_bio: String? = null,
    val usr_telephone: String? = null,
    val usr_handle: String? = null,
    val usr_name: String? = null,
    val usr_dob: String? = null,
    val usr_category: String? = null,
    val usr_location: String? = null,
    val usr_website: String? = null,
    val usr_avatar: String? = null,
    val usr_cover: String? = null,
)

@Keep
@IgnoreExtraProperties
data class usr_impressionData(
    val followers_count: Int = 0,
    val following_count: Int = 0,
    val posts_count: String? = null,
    val live_count: String? = null,
    val followings: Map<String, String>? = emptyMap(),
    val followers: Map<String, String>? = emptyMap(),
)

@Keep
@IgnoreExtraProperties
data class followingsData(
    val followers_uid: String = "",
    val timestamp: String = "",
)

@Keep
@IgnoreExtraProperties
data class followersData(
    val followers_uid: String = "",
    val timestamp: String = "",
)