package com.colman.booknook.ui.posts

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.colman.booknook.R
import com.colman.booknook.databinding.FragmentPostsBinding
import com.colman.booknook.model.Model
import kotlinx.coroutines.launch

class PostsFragment : Fragment(R.layout.fragment_posts) {

    private lateinit var binding: FragmentPostsBinding
    private val adapter = PostsAdapter { postId ->
        findNavController().navigate(
            R.id.postDetailsFragment,
            bundleOf("postId" to postId)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentPostsBinding.bind(view)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.newPostBtn.setOnClickListener {
            findNavController().navigate(R.id.bookSearchFragment)
        }
        binding.searchBtn.setOnClickListener {
            findNavController().navigate(R.id.filtersFragment)
        }
        binding.wishlistBtn.setOnClickListener {
            findNavController().navigate(R.id.wishlistFragment)
        }
        binding.profileBtn.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        Model.observePosts().observe(viewLifecycleOwner) { list ->
            adapter.submit(list)
            binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try { Model.refreshPosts() } catch (_: Exception) {}
        }
    }
}
