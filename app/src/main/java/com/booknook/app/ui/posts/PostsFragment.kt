package com.booknook.app.ui.posts

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.booknook.app.R
import com.booknook.app.databinding.FragmentPostsBinding
import com.booknook.app.model.Model

class PostsFragment : Fragment(R.layout.fragment_posts) {

    private var _binding: FragmentPostsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PostsViewModel by viewModels()
    
    private val adapter = PostsAdapter(
        onClick = { postId ->
            val action = PostsFragmentDirections.actionPostsToDetails(postId)
            findNavController().navigate(action)
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        if (Model.currentUserId() == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        _binding = FragmentPostsBinding.bind(view)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.emptyText.isVisible = list.isEmpty()
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // If there's a loading progress bar in fragment_posts.xml
            // binding.pbLoading.isVisible = isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
