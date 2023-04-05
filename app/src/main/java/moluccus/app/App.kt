package moluccus.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import moluccus.app.devops.MyGLSurfaceView
import moluccus.app.util.FirebaseUtils

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MyGLSurfaceView(this)
        FirebaseApp.initializeApp(this).let {
            Firebase.database.setPersistenceEnabled(true)
        }
    }
}
