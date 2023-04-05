package moluccus.app.base

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.BuildConfig
import com.google.firebase.ktx.Firebase
import moluccus.app.databinding.AuthLayoutBinding
import moluccus.app.ui.MainActivity
import moluccus.app.util.Extensions.toast
import moluccus.app.util.FirebaseUtils
import moluccus.app.util.FirebaseUtils.firebaseAuth
import moluccus.app.util.FirebaseUtils.firebaseDatabase
import moluccus.app.util.FirebaseUtils.firebaseUser
import moluccus.app.util.FirebaseUtils.uid
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: AuthLayoutBinding

    lateinit var userEmail: String
    lateinit var userPassword: String
    lateinit var createAccountInputsArray: Array<EditText>

    lateinit var signInEmail: String
    lateinit var signInPassword: String
    lateinit var signInInputsArray: Array<EditText>
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = AuthLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initAuthLayouts()
        signInInputsArray = arrayOf(binding.emailHolder, binding.passwordHolder)
        createAccountInputsArray = arrayOf(
            binding.createAccountEmail,
            binding.createAccountPassword,
            binding.createAccountPasswordRepeat
        )
    }

    public override fun onStart() {
        super.onStart()
        val user: FirebaseUser? = firebaseAuth.currentUser
        when {
            user != null -> {
                startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                finish()
            }
            else -> {
                // No user is signed in
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun initAuthLayouts() {
        binding.createAccount.setOnClickListener {
            binding.loginAuthLayout.visibility = View.GONE
            binding.createAuthLayout.visibility = View.VISIBLE
            initCreateLayout()
        }

        val email_address = binding.emailHolder
        val password_password = binding.passwordHolder
        binding.loginButton.setOnClickListener {
            signInEmail = email_address.text.toString().trim()
            signInPassword = password_password.text.toString().trim()

            if (notEmptyLogin()) {
                firebaseAuth.signInWithEmailAndPassword(signInEmail, signInPassword)
                    .addOnCompleteListener { signIn ->
                        if (signIn.isSuccessful) {
                            startActivity(Intent(this, MainActivity::class.java))
                            toast("signed in successfully")
                            finish()
                        } else {
                            toast("sign in failed")
                        }
                    }
            } else {
                signInInputsArray.forEach { input ->
                    if (input.text.toString().trim().isEmpty()) {
                        input.error = "${input.hint} is required"
                    }
                }
            }
        }
    }

    private fun notEmptyLogin(): Boolean = signInEmail.isNotEmpty() && signInPassword.isNotEmpty()

    @SuppressLint("SimpleDateFormat")
    private fun initCreateLayout() {
        binding.backToLogin.setOnClickListener {
            binding.loginAuthLayout.visibility = View.VISIBLE
            binding.createAuthLayout.visibility = View.GONE
            initAuthLayouts()
        }

        val createEmailAddress = binding.createAccountEmail
        val createPassword = binding.createAccountPassword

        binding.createAccountButton.setOnClickListener {
            if (identicalPassword()) {
                userEmail = createEmailAddress.text.toString().trim()
                userPassword = createPassword.text.toString().trim()

                /*create a user*/
                firebaseAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = firebaseAuth.currentUser

                            val df: DateFormat = SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss")
                            val date: String = df.format(Calendar.getInstance().time).toString()
                            val map : HashMap<String, Any> = hashMapOf(
                                "email_address" to userEmail,
                                "password_holder" to userPassword,
                                "create_at" to date,
                            )
                            firebaseDatabase.child("users").child(user!!.uid).setValue(map)
                                .addOnCompleteListener { isSuccessful ->
                                    if (isSuccessful.isSuccessful) {
                                        toast("created account successfully !")
                                        startActivity(Intent(this, MainActivity::class.java))
                                        finish()
                                    } else {
                                        toast("an error saving your data...")
                                    }
                                }
                        } else {
                            toast("failed to Authenticate !")
                        }
                    }
            }
        }
    }

    private fun notEmpty(): Boolean =
        binding.createAccountEmail.text.toString().trim().isNotEmpty() &&
                binding.createAccountPassword.text.toString().trim().isNotEmpty() &&
                binding.createAccountPasswordRepeat.text.toString().trim().isNotEmpty()

    private fun identicalPassword(): Boolean {
        var identical = false
        if (notEmpty() &&
            binding.createAccountPassword.text.toString()
                .trim() == binding.createAccountPasswordRepeat.text.toString().trim()
        ) {
            identical = true
        } else if (!notEmpty()) {
            createAccountInputsArray.forEach { input ->
                if (input.text.toString().trim().isEmpty()) {
                    input.error = "${input.hint} is required"
                }
            }
        } else {
            toast("passwords don't not matching !")
        }
        return identical
    }
}