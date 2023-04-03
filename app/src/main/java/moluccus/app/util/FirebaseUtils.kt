package moluccus.app.util

import com.google.errorprone.annotations.Keep
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

@Keep
object FirebaseUtils {
    val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val firebaseUser: FirebaseUser? = firebaseAuth.currentUser
    val firebaseStorage : StorageReference = FirebaseStorage.getInstance().getReference("moluccus")
    val firebaseDatabase: DatabaseReference = FirebaseDatabase.getInstance().getReference("moluccus")
    val uid: String
        get() = firebaseUser!!.uid
}