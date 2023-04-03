package moluccus.app.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class OpenMultipleDocumentContract : ActivityResultContract<Array<out String>, List<Uri>>() {
    override fun createIntent(context: Context, input: Array<out String>): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, input)
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        val uris = mutableListOf<Uri>()
        if (resultCode == Activity.RESULT_OK) {
            intent?.let { data ->
                if (data.clipData != null) {
                    for (i in 0 until data.clipData!!.itemCount) {
                        uris.add(data.clipData!!.getItemAt(i).uri)
                    }
                } else {
                    data.data?.let { uri ->
                        uris.add(uri)
                    }
                }
            }
        }
        return uris
    }
}
