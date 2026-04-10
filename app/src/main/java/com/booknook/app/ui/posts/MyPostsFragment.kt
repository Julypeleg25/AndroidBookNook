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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyPostsFragment : Fragment(R.layout.fragment_my_posts) {

    private var _binding: FragmentMyPostsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyPostsViewModel by viewModels()

    private val adapter by lazy { PostsAdapter(
        currentUserId = viewModel.currentUserId,
        onClick = { postId ->
            val action = MyPostsFragmentDirections.actionMyPostsToDetails(postId)
            findNavController().navigate(action)
        },
        onLike = { postId ->
            viewModel.toggleLike(postId)
        },
        onEdit = { postId ->
            val action = MyPostsFragmentDirections.actionMyPostsToEdit(postId)
            findNavController().navigate(action)
        },
        onDelete = { postId ->
            showDeleteConfirmation(postId)
        }
    ) }

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
    }

    private fun showDeleteConfirmation(postId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_post_title)
            .setMessage(R.string.delete_post_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.delete_post_confirm) { _, _ ->
                viewModel.deletePost(postId)
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
                Snackbar.make(binding.root, getString(it), Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.deleteSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Snackbar.make(binding.root, R.string.post_deleted, Snackbar.LENGTH_SHORT).show()
                viewModel.resetDeleteSuccess()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
