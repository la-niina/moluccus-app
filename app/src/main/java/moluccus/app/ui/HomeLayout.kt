package moluccus.app.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import moluccus.app.R
import moluccus.app.adapter.FeedsAdapter
import moluccus.app.databinding.HomeLayoutBinding
import moluccus.app.route.CommitPost
import moluccus.app.util.FirebaseUtils.firebaseDatabase


class HomeLayout : Fragment() {
    private var _binding: HomeLayoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HomeLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        initRecyclerView()
        binding.fab.setOnClickListener { view ->
            findNavController().navigate(R.id.action_post)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onStart() {
        super.onStart()
        initRecyclerView()
    }

    private fun initRecyclerView() {
        val recyclerViewCommitPosts = mutableListOf<CommitPost>()
        val adapter = FeedsAdapter(requireContext(), recyclerViewCommitPosts)
        val recyclerView = binding.postRecyclerView
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        // Listen for changes in the Firebase Realtime Database
        firebaseDatabase.child("commits").orderByKey()
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(snapshot: DataSnapshot) {
                    recyclerViewCommitPosts.clear()
                    for (postSnapshot in snapshot.children) {
                        val post = postSnapshot.getValue(CommitPost::class.java)
                        post?.let {
                            recyclerViewCommitPosts.add(it)
                        }
                    }
                    // Shuffle the list randomly
                    recyclerViewCommitPosts.shuffle()
                    // Notify the adapter that the data has changed
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle the error
                }
            })

        // Set up swipe to refresh
        swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            // Refresh the data from the Firebase Realtime Database
            firebaseDatabase.child("commits").orderByKey()
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        recyclerViewCommitPosts.clear()
                        for (postSnapshot in snapshot.children) {
                            val post = postSnapshot.getValue(CommitPost::class.java)
                            post?.let {
                                recyclerViewCommitPosts.add(it)
                            }
                        }
                        // Shuffle the list randomly
                        recyclerViewCommitPosts.shuffle()
                        // Notify the adapter that the data has changed
                        adapter.notifyDataSetChanged()
                        // Hide the refreshing indicator
                        swipeRefreshLayout.isRefreshing = false
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle the error
                        swipeRefreshLayout.isRefreshing = false
                    }
                })
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && binding.fab.visibility == View.VISIBLE) {
                    binding.fab.hide()
                } else if (dy < 0 && binding.fab.visibility != View.VISIBLE) {
                    binding.fab.show()
                }
            }
        })
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        val notificationMenuItem = menu.findItem(R.id.ic_notification)
        notificationMenuItem.setActionView(R.layout.menu_item_notification)
        notificationMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
                return true
            }
            override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
                return true
            }
        })

        notificationMenuItem.actionView?.setOnClickListener {
            findNavController().navigate(R.id.action_notification)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    @SuppressLint("SetTextI18n")
    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.ic_profile -> {
                findNavController().navigate(R.id.action_profile)
            }
            R.id.ic_message -> {
                findNavController().navigate(R.id.action_message)
            }
            R.id.ic_notification -> {
                val badgeTextView = item.actionView?.findViewById<TextView>(R.id.notification_badge_count)
                badgeTextView?.text = (badgeTextView?.text.toString().toInt() + 1).toString()
                findNavController().navigate(R.id.action_notification)
            }
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Glide.with(this).pauseRequests()
    }
}