package moluccus.app.util

import android.content.Context
import java.util.regex.Pattern

object Constract {
    var isCallEnded: Boolean = false
    var isIntiatedNow : Boolean = true

    fun Context.checkEmail(email: String?): Boolean {
        val EMAIL_ADDRESS_PATTERN: Pattern = Pattern
            .compile(
                "[a-zA-Z0-9+._%-+]{1,256}" + "@"
                        + "[a-zA-Z0-9][a-zA-Z0-9-]{0,64}" + "(" + "."
                        + "[a-zA-Z0-9][a-zA-Z0-9-]{0,25}" + ")+"
            )
        return EMAIL_ADDRESS_PATTERN.matcher(email!!).matches()
    }

    fun isString(value: Any?): Boolean {
        return value is String
    }
}