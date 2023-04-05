package moluccus.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import moluccus.app.base.AuthActivity
import moluccus.app.databinding.SettingsLayoutBinding
import moluccus.app.util.FirebaseUtils

class SettingsLayout : Fragment() {
    private var _binding: SettingsLayoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.logoutButton.setOnClickListener {
            FirebaseUtils.firebaseAuth.signOut()
            val intent = Intent(requireContext(), AuthActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        initTotalStorageUsed()
        initTotalDevicesLoggedIn()
    }

    private fun initTotalDevicesLoggedIn() {

    }

    private fun initTotalStorageUsed() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val storageRef = FirebaseUtils.firebaseStorage
            .child("galleries").child(userId!!)
        var totalSize = 0L

        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                for (item in listResult.items) {
                    item.metadata.addOnSuccessListener { metadata ->
                        val size = metadata.sizeBytes
                        totalSize += size
                        val sizeString = when {
                            totalSize >= 1024 * 1024 * 1024 -> "%.2f GB".format(totalSize.toFloat() / (1024 * 1024 * 1024))
                            totalSize >= 1024 * 1024 -> "%.2f MB".format(totalSize.toFloat() / (1024 * 1024))
                            totalSize >= 1024 -> "%.2f KB".format(totalSize.toFloat() / 1024)
                            else -> "$totalSize B"
                        }
                        binding.totalstorageUsed.text = sizeString
                    }
                }
            }
            .addOnFailureListener {
                // Handle any errors
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}