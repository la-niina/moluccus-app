package moluccus.app.route

data class FollowsData(
    val followers_uid : followersInfo? = null,
)

data class followersInfo(
    val followers_uid: String = "",
    val timestamp: String = "",
)
