package com.booknook.app.ui.posts

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.booknook.app.R
import com.booknook.app.databinding.FragmentPostsBinding
import com.booknook.app.model.Model
import com.google.android.material.snackbar.Snackbar

class PostsFragment : Fragment(R.layout.fragment_posts) {

    private var _binding: FragmentPostsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PostsViewModel by viewModels()

    private val adapter = PostsAdapter(
        currentUserId = Model.authRepository.currentUserId(),
        onClick = { postId ->
            val action = PostsFragmentDirections.actionPostsToDetails(postId)
            findNavController().navigate(action)
        },
        onLike = { postId ->
            viewModel.toggleLike(postId)
        },
        onEdit = { postId ->
            val action = PostsFragmentDirections.actionPostsToEdit(postId)
            findNavController().navigate(action)
        },
        onDelete = { postId ->
            showDeleteConfirmation(postId)
        }
    )

    private fun showDeleteConfirmation(postId: String) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_post_title)
            .setMessage(R.string.delete_post_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.delete_post_confirm) { _, _ ->
                viewModel.deletePost(postId)
                Snackbar.make(binding.root, R.string.post_deleted, Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPostsBinding.bind(view)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setColorSchemeResources(R.color.bn_orange)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }

        binding.searchInput.doAfterTextChanged { text ->
            viewModel.filterPosts(text?.toString()?.trim().orEmpty())
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.emptyText.isVisible = list.isEmpty()
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.pbLoading.isVisible = isLoading
            if (!isLoading) binding.swipeRefresh.isRefreshing = false
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, getString(it), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
