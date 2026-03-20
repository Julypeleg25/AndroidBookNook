package com.booknook.app.ui.posts

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.booknook.app.R
import com.booknook.app.databinding.FragmentPostsBinding
import com.booknook.app.model.Model
import kotlinx.coroutines.launch

class PostsFragment : Fragment(R.layout.fragment_posts) {

    private lateinit var binding: FragmentPostsBinding
    private val adapter = PostsAdapter { postId ->
        val action = PostsFragmentDirections.actionPostsToDetails(postId)
        findNavController().navigate(action)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentPostsBinding.bind(view)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        Model.observePosts().observe(viewLifecycleOwner) { list ->
            adapter.submit(list)
            binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        // refresh remote into cache (async)
        viewLifecycleOwner.lifecycleScope.launch {
            try { Model.refreshPosts() } catch (_: Exception) {}
        }
    }
}
