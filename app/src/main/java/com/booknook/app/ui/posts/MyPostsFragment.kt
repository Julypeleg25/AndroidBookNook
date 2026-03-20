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
import com.booknook.app.model.Model
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.booknook.app.util.toUserFriendlyMessage

class MyPostsFragment : Fragment(R.layout.fragment_my_posts) {

    private var _binding: FragmentMyPostsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyPostsViewModel by viewModels()

    private val adapter = PostsAdapter(
        currentUserId = Model.currentUserId(),
        onClick = { postId ->
            val action = MyPostsFragmentDirections.actionMyPostsToDetails(postId)
            findNavController().navigate(action)
        },
        showEngagement = false,
        onEdit = { postId ->
            val action = MyPostsFragmentDirections.actionMyPostsToEdit(postId)
            findNavController().navigate(action)
        },
        onDelete = { postId ->
            showDeleteConfirmation(postId)
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyPostsBinding.bind(view)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setColorSchemeResources(R.color.bn_orange)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }

        observeViewModel()
        viewModel.refreshPosts()
    }

    private fun showDeleteConfirmation(postId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePost(postId)
                Snackbar.make(binding.root, "Post deleted", Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun observeViewModel() {
        viewModel.observeMyPosts().observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.emptyState.isVisible = list.isEmpty()
            binding.recyclerView.isVisible = list.isNotEmpty()
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.pbLoading.isVisible = isLoading
            if (!isLoading) binding.swipeRefresh.isRefreshing = false
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it.toUserFriendlyMessage(), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
