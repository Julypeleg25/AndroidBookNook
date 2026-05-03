package com.booknook.app.ui.posts

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.booknook.app.R
import com.booknook.app.base.MyApplication
import com.booknook.app.databinding.FragmentMyPostsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MyPostsFragment : Fragment(R.layout.fragment_my_posts) {

    private var _binding: FragmentMyPostsBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as MyApplication
    private val viewModel: MyPostsViewModel by viewModels {
        MyPostsViewModel.factory(
            postsRepository = app.postsRepository,
            authRepository = app.authRepository
        )
    }

    private val adapter by lazy { PostsAdapter(
        currentUserId = viewModel.currentUserId,
        onClick = viewModel::onPostSelected,
        onLike = viewModel::onLikeClicked,
        onEdit = viewModel::onEditRequested,
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
                viewModel.onDeleteConfirmed(postId)
            }
            .show()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.posts)
            binding.emptyState.isVisible = state.isEmpty
            binding.recyclerView.isVisible = state.posts.isNotEmpty()
            binding.pbLoading.isVisible = state.isInitialLoading
            binding.swipeRefresh.isRefreshing = state.isRefreshing
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { action ->
                when (action) {
                    is MyPostsEvent.NavigateToDetails -> {
                        val direction = MyPostsFragmentDirections.actionMyPostsToDetails(action.postId)
                        findNavController().navigate(direction)
                    }

                    is MyPostsEvent.NavigateToEdit -> {
                        val direction = MyPostsFragmentDirections.actionMyPostsToEdit(action.postId)
                        findNavController().navigate(direction)
                    }

                    is MyPostsEvent.ShowMessage -> {
                        Snackbar.make(binding.root, getString(action.messageRes), Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.recyclerView.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
