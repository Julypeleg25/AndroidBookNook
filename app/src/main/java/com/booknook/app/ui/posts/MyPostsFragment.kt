package com.booknook.app.ui.posts

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.booknook.app.R
import com.booknook.app.databinding.FragmentMyPostsBinding
import com.google.android.material.snackbar.Snackbar

class MyPostsFragment : Fragment(R.layout.fragment_my_posts) {

    private var _binding: FragmentMyPostsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyPostsViewModel by viewModels()

    private val adapter = PostsAdapter(
        onClick = { postId ->
            val action = MyPostsFragmentDirections.actionMyPostsToDetails(postId)
            findNavController().navigate(action)
        },
        onEdit = { postId ->
            val action = MyPostsFragmentDirections.actionMyPostsToEdit(postId)
            findNavController().navigate(action)
        },
        onDelete = { postId ->
            viewModel.deletePost(postId)
            Snackbar.make(binding.root, "Post deleted", Snackbar.LENGTH_LONG).show()
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyPostsBinding.bind(view)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        observeViewModel()
        viewModel.refreshPosts()
    }

    private fun observeViewModel() {
        viewModel.observeMyPosts().observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.isVisible = list.isEmpty()
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.pbLoading.isVisible = isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
