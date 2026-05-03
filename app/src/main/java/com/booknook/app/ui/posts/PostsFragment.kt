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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        MaterialAlertDialogBuilder(requireContext())
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

        setupPostsList()
        setupSwipeRefresh()
        setupSearch()
        observeViewModel()
    }

    private fun setupPostsList() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(createLoadMoreScrollListener())
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.bn_orange)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }
    }

    private fun setupSearch() {
        binding.searchInput.doAfterTextChanged { text ->
            viewModel.onSearchQueryChanged(text?.toString().orEmpty())
        }
    }

    private fun createLoadMoreScrollListener(): RecyclerView.OnScrollListener {
        val layoutManager = binding.recyclerView.layoutManager as LinearLayoutManager
        return object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && layoutManager.isScrolledToEnd()) {
                    viewModel.onLoadMoreRequested()
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { action ->
                handleEvent(action)
            }
        }
    }

    private fun renderState(state: PostsUiState) {
        updateSearchText(state.query)
        adapter.submitList(state.posts)
        binding.emptyText.isVisible = state.isEmpty
        binding.pbLoading.isVisible = state.isInitialLoading
        binding.swipeRefresh.isRefreshing = state.isRefreshing
        binding.pbLoadingMore.isVisible = state.isLoadingMore
    }

    private fun updateSearchText(query: String) {
        if (binding.searchInput.text?.toString() == query) return
        binding.searchInput.setText(query)
        binding.searchInput.setSelection(query.length)
    }

    private fun handleEvent(event: PostsEvent) {
        when (event) {
            is PostsEvent.NavigateToDetails -> navigateToDetails(event.postId)
            is PostsEvent.NavigateToEdit -> navigateToEdit(event.postId)
            is PostsEvent.ShowMessage -> showMessage(event.messageRes)
        }
    }

    private fun navigateToDetails(postId: String) {
        val direction = PostsFragmentDirections.actionPostsToDetails(postId)
        findNavController().navigate(direction)
    }

    private fun navigateToEdit(postId: String) {
        val direction = PostsFragmentDirections.actionPostsToEdit(postId)
        findNavController().navigate(direction)
    }

    private fun showMessage(messageRes: Int) {
        Snackbar.make(binding.root, getString(messageRes), Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        binding.recyclerView.adapter = null
        super.onDestroyView()
        _binding = null
    }

    private fun LinearLayoutManager.isScrolledToEnd(): Boolean {
        val firstVisiblePosition = findFirstVisibleItemPosition()
        return firstVisiblePosition >= 0 && childCount + firstVisiblePosition >= itemCount
    }
}
