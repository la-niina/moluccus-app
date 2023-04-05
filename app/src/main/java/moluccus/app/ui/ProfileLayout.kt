package moluccus.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import moluccus.app.R
import moluccus.app.adapter.FeedsAdapter
import moluccus.app.base.InfoActivity
import moluccus.app.databinding.ProfileLayoutBinding
import moluccus.app.glide.GlideImageLoader
import moluccus.app.route.CommitPost
import moluccus.app.util.Extensions.toast
import moluccus.app.util.FirebaseUtils
import moluccus.app.util.FirebaseUtils.firebaseDatabase
import moluccus.app.util.users
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sign

class ProfileLayout : Fragment() {
    private var binding: ProfileLayoutBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ProfileLayoutBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser

        val twitterBlue = Color.parseColor("#1DA1F2")
        val colorStateList = ColorStateList.valueOf(twitterBlue)
        binding?.userhandleView?.setTextColor(colorStateList)
        firebaseDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    //get the user data from the snapshot
                    val value = snapshot.child("users").child(user!!.uid)
                        .getValue(users::class.java)

                    if (binding != null && value != null) {
                        //set the user information to the corresponding views
                        binding?.emailProfile?.text = value.email_address?.trim().toString()
                        binding?.dateJoined?.text = value.create_at?.trim().toString()
                        binding?.usernameView?.text = value.usr_information!!.usr_name?.trim().toString()
                        binding?.userhandleView?.text = value.usr_information.usr_handle?.trim().toString()
                        binding?.bioView?.text = value.usr_information.usr_bio?.trim().toString()
                        binding?.locationView?.text = value.usr_information.usr_location?.trim().toString()
                        binding?.websiteView?.text = value.usr_information.usr_website?.trim().toString()

                        value.usr_impression?.following_count.toString().let {
                            if (it.isNullOrEmpty()){
                                binding?.profileFollowing?.text = "0"
                            } else {
                                binding?.profileFollowing?.text = it
                            }
                        }

                        value.usr_impression?.followers_count?.toString().let {
                            if (it.isNullOrEmpty()){
                                binding?.profileFollowers?.text = "0"
                            } else {
                                binding?.profileFollowers?.text = it
                            }
                        }

                        //load the user cover photo using Glide library
                        val photo = value.usr_information.usr_cover
                        if (photo == null) {
                            //set the default cover photo if the user doesn't have one
                            binding?.wallpaperCover?.let {
                                Glide.with(requireContext()).load(R.drawable.default_cover).into(it)
                            }
                            binding?.photoProgressBar?.visibility = View.GONE
                        } else {
                            val options = RequestOptions()
                                .error(R.drawable.default_cover)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                            //show the progress bar while the photo is being loaded
                            binding?.photoProgressBar?.visibility = View.VISIBLE
                            //using custom glide image loader to indicate progress in time
                            try {
                                GlideImageLoader(
                                    binding?.wallpaperCover,
                                    binding?.photoProgressBar
                                ).load(photo, options)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                binding?.wallpaperCover?.setImageResource(R.drawable.default_cover)
                            }
                        }

                        //load the user avatar using Glide library
                        val pp = value.usr_information.usr_avatar
                        if (pp == null) {
                            //set the default avatar photo if the user doesn't have one
                            binding?.avatarHolder?.let {
                                Glide.with(requireContext()).load(R.drawable.default_cover).into(it)
                            }
                            binding?.avatarHolder?.visibility = View.GONE
                        } else {
                            val options = RequestOptions()
                                .error(R.drawable.default_cover)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                            //show the avatar view and progress bar while the photo is being loaded
                            binding?.avatarHolder?.visibility = View.VISIBLE
                            //using custom glide image loader to indicate progress in time
                            try {
                                GlideImageLoader(binding?.avatarHolder, binding?.uploadAvatar).load(
                                    pp,
                                    options
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                                binding?.avatarHolder?.setImageResource(R.drawable.default_cover)
                            }
                        }
                    } else {
                        Log.e("ProfileLayout", "Binding or value is null!")
                    }
                } else {
                    //handle the case where the snapshot doesn't exist or is incomplete
                    requireActivity().toast("incomplete account")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("TAG", "Failed to read value.", error.toException())
            }
        })
        initRecyclerView()
    }

    private fun initRecyclerView() {
        val recyclerViewCommitPosts = mutableListOf<CommitPost>()
        val adapter = FeedsAdapter(requireContext(), recyclerViewCommitPosts)
        val recyclerView = binding?.UserPostRecyclerView
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView!!.layoutManager = layoutManager
        recyclerView.adapter = adapter
        val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser

        // Define a list of date format patterns
        val datePatterns = listOf(
            "EEE, d MMM yyyy, HH:mm",
            "EEE, d MMM yyyy, HH:mm:ss",
            "d MMM yyyy, HH:mm",
            "d MMM yyyy, HH:mm:ss",
        )

        // Listen for changes in the Firebase Realtime Database
        firebaseDatabase.child("commits").orderByChild("timeStamp")
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(snapshot: DataSnapshot) {
                    recyclerViewCommitPosts.clear()
                    for (postSnapshot in snapshot.children) {
                        val post = postSnapshot.getValue(CommitPost::class.java)
                        if (post?.authorId == user!!.uid) {
                            // Try parsing the date with each pattern
                            var date: Date? = null
                            for (pattern in datePatterns) {
                                try {
                                    date =
                                        SimpleDateFormat(pattern, Locale.US).parse(post.timeStamp)
                                    if (date != null) break
                                } catch (e: Exception) {
                                    // Ignore parse errors and try the next pattern
                                }
                            }

                            // Add the post with the parsed date
                            post.let {
                                if (date != null) it.timeStamp //= date.toString()
                                recyclerViewCommitPosts.add(it)
                            }
                        }
                    }
                    // Sort the posts by date in ascending order
                    recyclerViewCommitPosts.sortBy { it.timeStamp }
                    // Notify the adapter that the data has changed
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle the error
                }
            })
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.profile_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit_profile -> {
                startActivity(Intent(requireContext(), InfoActivity::class.java))
            }
            R.id.settings_holder -> {
                findNavController().navigate(R.id.action_settings)
            }
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        Glide.with(this).pauseRequests()
    }

}