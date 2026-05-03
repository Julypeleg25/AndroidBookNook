package com.booknook.app.ui.posts

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.booknook.app.R
import com.booknook.app.base.MyApplication
import com.booknook.app.databinding.FragmentPostsBinding
import com.google.android.material.snackbar.Snackbar

class PostsFragment : Fragment(R.layout.fragment_posts) {

    private var _binding: FragmentPostsBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as MyApplication
    private val viewModel: PostsViewModel by viewModels {
        PostsViewModel.factory(
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

    private fun showDeleteConfirmation(postId: String) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_post_title)
            .setMessage(R.string.delete_post_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.delete_post_confirm) { _, _ ->
                viewModel.onDeleteConfirmed(postId)
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

        val layoutManager = binding.recyclerView.layoutManager as LinearLayoutManager
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisible = layoutManager.findFirstVisibleItemPosition()
                    if ((visibleItemCount + firstVisible) >= totalItemCount && firstVisible >= 0) {
                        viewModel.onLoadMoreRequested()
                    }
                }
            }
        })

        binding.searchInput.doAfterTextChanged { text ->
            viewModel.onSearchQueryChanged(text?.toString().orEmpty())
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (binding.searchInput.text?.toString() != state.query) {
                binding.searchInput.setText(state.query)
                binding.searchInput.setSelection(state.query.length)
            }

            adapter.submitList(state.posts)
            binding.emptyText.isVisible = state.isEmpty
            binding.pbLoading.isVisible = state.isInitialLoading
            binding.swipeRefresh.isRefreshing = state.isRefreshing
            binding.pbLoadingMore.isVisible = state.isLoadingMore
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { action ->
                when (action) {
                    is PostsEvent.NavigateToDetails -> {
                        val direction = PostsFragmentDirections.actionPostsToDetails(action.postId)
                        findNavController().navigate(direction)
                    }

                    is PostsEvent.NavigateToEdit -> {
                        val direction = PostsFragmentDirections.actionPostsToEdit(action.postId)
                        findNavController().navigate(direction)
                    }

                    is PostsEvent.ShowMessage -> {
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
